package com.sumizuri.app.source.engine

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.asyncFunction
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.source.host.HostBridge
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

// What a loaded extension offers: the methods its Source has, its feeds and whether its search reads the tags to leave out.
class ExtensionShape(val methods: Set<String>, val feeds: JsonArray, val readsExcluded: Boolean)

// One extension in its own QuickJS instance. A script loop that never ends takes the calls, so calls on one source do not wait for each other.
class ExtensionRuntime private constructor(private val quickJs: QuickJs, private val calls: Calls, val shape: ExtensionShape) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun startLoop() {
        scope.launch {
            try {
                quickJs.evaluate<Any?>("__loop()", "loop.js")
            } catch (_: Throwable) {
                // Closing the instance ends the loop, which is how it is meant to stop.
            }
        }
    }

    // Calls a method of the Source with the settings the reader chose, and gives back what it resolved to.
    suspend fun call(method: String, args: List<JsonElement>, settings: Map<String, String>): JsonElement {
        require(method.all { it.isLetterOrDigit() }) { "Bad method name" }
        val done = CompletableDeferred<Outcome>()
        val id = calls.register(done)
        try {
            calls.requests.send(
                buildJsonObject {
                    put("id", id)
                    put("method", method)
                    put("args", JsonArray(args))
                    put("settings", JsonObject(settings.mapValues { JsonPrimitive(it.value) }))
                }.toString(),
            )
            val outcome = done.await()
            outcome.error?.let { throw ExtensionCallException(it) }
            return Json.parseToJsonElement(outcome.text ?: "null")
        } finally {
            // A call given up on may finish later, and nobody waits for it then.
            calls.forget(id)
        }
    }

    // The calls waiting on this source end at once. The instance itself is shut on a thread of its own, since shutting it waits for a call that is still running, such as a test left halfway, and would freeze the screen.
    fun close() {
        scope.cancel()
        calls.requests.close()
        CoroutineScope(Dispatchers.IO).launch {
            calls.failAll("the source was closed")
            runCatching { quickJs.close() }
        }
    }

    class Outcome(val text: String?, val error: String?)

    // The way calls travel to the script and answers travel back.
    class Calls {
        val requests = Channel<String>(Channel.UNLIMITED)
        private val lock = Mutex()
        private val pending = HashMap<Long, CompletableDeferred<Outcome>>()
        private var nextId = 1L

        suspend fun register(done: CompletableDeferred<Outcome>): Long = lock.withLock { (nextId++).also { pending[it] = done } }

        suspend fun forget(id: Long) = lock.withLock { pending.remove(id) }

        // Ends every call still waiting, for a source that is closed.
        suspend fun failAll(message: String) {
            val waiting = lock.withLock { pending.values.toList().also { pending.clear() } }
            waiting.forEach { it.complete(Outcome(null, message)) }
        }

        suspend fun complete(id: Long, text: String?, error: String?) {
            lock.withLock { pending[id] }?.complete(Outcome(text, error))
        }
    }

    companion object {
        // One extension may not use more than this much memory.
        private const val MemoryLimitBytes = 256L * 1024 * 1024

        // A stand in for the Node Buffer and an atob, which the cheerio bundle looks for while it loads. atob is taken away again, since the format gives none.
        private const val BeforeCheerio = """
            globalThis.Buffer = globalThis.Buffer || { isBuffer: function () { return false; } };
            if (typeof globalThis.atob !== 'function') {
              globalThis.__tempAtob = true;
              globalThis.atob = function (text) {
                var letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
                var clean = String(text).replace(/[^A-Za-z0-9+/]/g, '');
                var out = '';
                for (var i = 0; i < clean.length; i += 4) {
                  var n = (letters.indexOf(clean[i]) << 18) | (letters.indexOf(clean[i + 1]) << 12) | ((letters.indexOf(clean[i + 2]) & 63) << 6) | (letters.indexOf(clean[i + 3]) & 63);
                  out += String.fromCharCode((n >> 16) & 255);
                  if (clean[i + 2] !== undefined) out += String.fromCharCode((n >> 8) & 255);
                  if (clean[i + 3] !== undefined) out += String.fromCharCode(n & 255);
                }
                return out;
              };
            }
        """

        private const val AfterCheerio = "if (globalThis.__tempAtob) { delete globalThis.atob; delete globalThis.__tempAtob; }"

        suspend fun open(id: String, code: String, scripts: EngineScripts, bridge: HostBridge): ExtensionRuntime {
            val quickJs = QuickJs.create(Dispatchers.Default)
            quickJs.memoryLimit = MemoryLimitBytes
            val calls = Calls()
            try {
                quickJs.asyncFunction("sendMessage") { args -> bridge.call(args[0] as String, args[1] as String) }
                quickJs.asyncFunction("__next") { calls.requests.receive() }
                quickJs.asyncFunction("__complete") { args ->
                    calls.complete((args[0] as Number).toLong(), args[1] as String?, args[2] as String?)
                    null
                }
                quickJs.evaluate<Any?>(BeforeCheerio, "before.js")
                quickJs.evaluate<Any?>(scripts.cheerio)
                quickJs.evaluate<Any?>(AfterCheerio, "after.js")
                quickJs.evaluate<Any?>(scripts.cryptoJs)
                quickJs.evaluate<Any?>(scripts.runtime)
                // The file runs inside a function with its own module, so it can end with module.exports as the format expects.
                val shapeText = quickJs.evaluate<String>(
                    "(function () {\nvar module = { exports: {} };\nvar exports = module.exports;\n$code\n;return __install(module, ${JsonPrimitive(id)});\n})()",
                    "$id.js",
                )
                val shape = Json.parseToJsonElement(shapeText).jsonObject
                val methods = (shape["methods"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.content }.toSet()
                val required = listOf("getSearchResults", "getMangaDetails", "getChapters", "getChapterDetails").filter { it !in methods }
                if (required.isNotEmpty()) throw ExtensionDefinitionException("The Source class is missing ${required.joinToString(", ")}.")
                val loaded = ExtensionShape(methods, shape["feeds"] as? JsonArray ?: JsonArray(emptyList()), (shape["readsExcluded"] as? JsonPrimitive)?.content == "true")
                return ExtensionRuntime(quickJs, calls, loaded).also { it.startLoop() }
            } catch (error: Throwable) {
                quickJs.close()
                throw error
            }
        }
    }
}

// A method of the Source threw, and this holds its text.
class ExtensionCallException(message: String) : Exception(message)

// An extension that cannot run, such as one whose Source lacks a method the format requires.
class ExtensionDefinitionException(message: String) : Exception(message)

// The scripts every extension runs on top of: the slim build of cheerio and CryptoJS, which the format gives as globals, and the runtime with App and the loop. They are turned into bytecode once while the app runs, so each extension that opens loads them in about half the time and never parses them again.
class EngineScripts(val cheerio: ByteArray, val cryptoJs: ByteArray, val runtime: ByteArray) {
    companion object {
        suspend fun load(): EngineScripts {
            val compiler = QuickJs.create(Dispatchers.Default)
            try {
                suspend fun compiled(path: String, name: String): ByteArray = compiler.compile(Res.readBytes(path).decodeToString(), name)
                return EngineScripts(
                    cheerio = compiled("files/engine/cheerio.slim.js", "cheerio.js"),
                    cryptoJs = compiled("files/engine/crypto-js.bundle.js", "crypto-js.js"),
                    runtime = compiled("files/engine/runtime.js", "runtime.js"),
                )
            } finally {
                compiler.close()
            }
        }
    }
}
