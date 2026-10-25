// The runtime of an extension: the App global of the source format, made from the calls of the app, and the loop that runs the methods of the extension's Source class for the app. cheerio and CryptoJS are loaded before this file. Nothing else from a browser is given.
(function () {
  var g = globalThis;

  function call(name, payload) {
    return sendMessage(name, JSON.stringify(payload || {})).then(function (text) {
      var reply = JSON.parse(text);
      if (reply && reply.error) throw new Error(reply.error);
      return reply;
    });
  }

  // Base64 of the bytes of a text in UTF-8, and back, written out since atob and btoa are not given.
  var LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';

  function base64Encode(text) {
    var bytes = unescape(encodeURIComponent(String(text)));
    var out = '';
    for (var i = 0; i < bytes.length; i += 3) {
      var a = bytes.charCodeAt(i), b = bytes.charCodeAt(i + 1), c = bytes.charCodeAt(i + 2);
      var triple = (a << 16) | ((b || 0) << 8) | (c || 0);
      out += LETTERS[(triple >> 18) & 63] + LETTERS[(triple >> 12) & 63];
      out += i + 1 < bytes.length ? LETTERS[(triple >> 6) & 63] : '=';
      out += i + 2 < bytes.length ? LETTERS[triple & 63] : '=';
    }
    return out;
  }

  function base64Decode(text) {
    var clean = String(text).replace(/[^A-Za-z0-9+/]/g, '');
    var bytes = '';
    for (var i = 0; i < clean.length; i += 4) {
      var n = (LETTERS.indexOf(clean[i]) << 18) | (LETTERS.indexOf(clean[i + 1]) << 12) | ((LETTERS.indexOf(clean[i + 2]) & 63) << 6) | (LETTERS.indexOf(clean[i + 3]) & 63);
      bytes += String.fromCharCode((n >> 16) & 255);
      if (clean[i + 2] !== undefined) bytes += String.fromCharCode((n >> 8) & 255);
      if (clean[i + 3] !== undefined) bytes += String.fromCharCode(n & 255);
    }
    try {
      return decodeURIComponent(escape(bytes));
    } catch (error) {
      return bytes;
    }
  }

  // Shared by every request manager of the source, since extensions make a new manager for each request.
  var pace = { interval: 0, last: 0 };

  function paceFrom(options) {
    var limit = options && options.rateLimit;
    if (!limit) return;
    var gaps = [];
    if (limit.requestsPerSecond > 0) gaps.push(1000 / limit.requestsPerSecond);
    if (limit.requestsPerMinute > 0) gaps.push(60000 / limit.requestsPerMinute);
    if (gaps.length) pace.interval = Math.max.apply(null, gaps);
  }

  // The settings the reader chose, set by the app before every call, so App.getSourceSetting answers at once as the format needs.
  var settings = {};

  // The helpers the format puts in every page it runs an extension script in. __fetchJSON asks the site from inside the page with its cookies. __watchFetch collects the addresses of the page's requests that hold a piece of text, also those made before the script ran.
  var PageHelpers = [
    'window.__fetchJSON = window.__fetchJSON || function (url, init) {',
    '  init = init || {};',
    '  if (!init.credentials) init.credentials = "include";',
    '  return fetch(url, init).then(function (response) {',
    '    if (!response.ok) throw new Error("HTTP " + response.status + " from " + url);',
    '    return response.json();',
    '  });',
    '};',
    'window.__watchFetch = window.__watchFetch || function (part, settleMs, timeoutMs) {',
    '  return new Promise(function (resolve, reject) {',
    '    var seen = [];',
    '    var quiet = null;',
    '    function note(url) {',
    '      url = String(url || "");',
    '      if (!url || url.indexOf(part) < 0) return;',
    '      try { url = new URL(url, location.href).href; } catch (e) {}',
    '      if (seen.indexOf(url) < 0) seen.push(url);',
    '      clearTimeout(quiet);',
    '      quiet = setTimeout(function () { resolve(seen.slice()); }, settleMs || 1000);',
    '    }',
    '    try { performance.getEntriesByType("resource").forEach(function (entry) { note(entry.name); }); } catch (e) {}',
    '    try { new PerformanceObserver(function (list) { list.getEntries().forEach(function (entry) { note(entry.name); }); }).observe({ entryTypes: ["resource"] }); } catch (e) {}',
    '    var realFetch = window.fetch;',
    '    window.fetch = function (input) { note(typeof input === "string" ? input : input && input.url); return realFetch.apply(this, arguments); };',
    '    var realOpen = XMLHttpRequest.prototype.open;',
    '    XMLHttpRequest.prototype.open = function (method, url) { note(url); return realOpen.apply(this, arguments); };',
    '    setTimeout(function () { if (!seen.length) reject(new Error("no request with " + part + " was seen")); }, timeoutMs || 15000);',
    '  });',
    '};',
    '',
  ].join('\n');

  // Many sites ask with XMLHttpRequest while extension scripts watch fetch. For a script that runs once the page is parsed, each finished text request is also passed through fetch, answered from its own response, so a wrapper of fetch sees it. A script that runs before the page gets none, since the page could see these copies and ask again.
  var XhrMirror = [
    'if (!window.__sumizuriMirror) {',
    '  window.__sumizuriMirror = true;',
    '  var mirrorMark = "__sumizuriXhr";',
    '  var networkFetch = window.fetch;',
    '  window.fetch = function (input, init) {',
    '    if (init && init[mirrorMark]) return Promise.resolve(init[mirrorMark]());',
    '    return networkFetch.apply(this, arguments);',
    '  };',
    '  var xhrOpen = XMLHttpRequest.prototype.open;',
    '  var xhrSend = XMLHttpRequest.prototype.send;',
    '  XMLHttpRequest.prototype.open = function (method, url) { this.__sumizuriUrl = url; this.__sumizuriMethod = method; return xhrOpen.apply(this, arguments); };',
    '  XMLHttpRequest.prototype.send = function () {',
    '    var xhr = this;',
    '    xhr.addEventListener("loadend", function () {',
    '      var type = xhr.responseType;',
    '      if (type !== "" && type !== "text" && type !== "json") return;',
    '      try {',
    '        var init = { method: xhr.__sumizuriMethod || "GET" };',
    '        init[mirrorMark] = function () {',
    '          var body = type === "" || type === "text" ? xhr.responseText : type === "json" ? JSON.stringify(xhr.response) : "";',
    '          var status = xhr.status >= 200 && xhr.status <= 599 && [204, 205, 304].indexOf(xhr.status) < 0 ? xhr.status : 200;',
    '          return new Response(body, { status: status });',
    '        };',
    '        window.fetch(new URL(xhr.__sumizuriUrl, location.href).href, init);',
    '      } catch (e) {}',
    '    });',
    '    return xhrSend.apply(this, arguments);',
    '  };',
    '}',
    '',
  ].join('\n');

  g.App = {
    createRequest: function (options) {
      return { url: options.url, method: options.method || 'GET', headers: options.headers || {}, body: options.body };
    },
    createRequestManager: function (options) {
      paceFrom(options);
      return {
        schedule: async function (request) {
          if (pace.interval > 0) {
            var wait = pace.last + pace.interval - Date.now();
            if (wait > 0) await call('sleep', { ms: wait });
            pace.last = Date.now();
          }
          var sent = { method: request.method || 'GET', headers: request.headers || {} };
          if (request.body !== undefined && request.body !== null) sent.body = typeof request.body === 'string' ? request.body : JSON.stringify(request.body);
          var result = await call('fetchUrl', { url: request.url, options: sent });
          return { status: result.statusCode, headers: result.headers || {}, data: result.body === undefined ? '' : result.body };
        },
      };
    },
    getSourceSetting: function (key) {
      var value = settings[key];
      return value === undefined || value === null ? undefined : String(value);
    },
    createSourceStateManager: function () {
      return {
        store: async function (key, value) {
          await call('storageSet', { key: String(key), value: JSON.stringify(value === undefined ? null : value) });
        },
        retrieve: async function (key) {
          var reply = await call('storageGet', { key: String(key) });
          var text = reply && reply.value;
          if (text === undefined || text === null) return undefined;
          try {
            return JSON.parse(text);
          } catch (error) {
            return text;
          }
        },
      };
    },
    base64Encode: base64Encode,
    base64Decode: base64Decode,
    // The rules for putting scrambled pictures back together are not published. The answer says it cannot, which the extensions that call it take as a sign to use the picture as it is.
    descrambleImage: async function () {
      return { error: 'Putting scrambled pictures back together is not supported.' };
    },
    // Runs a script in a page of the hidden browser, before the page's own scripts or once it is parsed. The script reports back with __reportResult or __reportError.
    executeInWebView: async function (options) {
      var address = options.url || options.baseUrl;
      if (!address) throw new Error('executeInWebView needs a url or a baseUrl.');
      var script = PageHelpers + String(options.script || '');
      var atParsed = '(function () { var run = function () { (0, eval)(' + JSON.stringify(XhrMirror + script) + '); };'
        + ' if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", run, { once: true }); else run(); })();';
      var answer;
      try {
        answer = await call('render', { url: address, early: options.runBeforePageScripts ? script : atParsed, timeout: options.timeoutMs });
      } catch (error) {
        return { error: String((error && error.message) || error) };
      }
      var text = answer.result;
      if (typeof text === 'string' && text.indexOf('ERROR: ') === 0) return { error: text.slice(7) };
      try {
        return { value: text === null || text === undefined ? null : JSON.parse(text) };
      } catch (error) {
        return { value: text };
      }
    },
  };

  // console goes to the log of the app, tagged with the source.
  function write(level) {
    return function () {
      var text = Array.prototype.map.call(arguments, function (part) {
        if (typeof part === 'string') return part;
        try { return JSON.stringify(part); } catch (error) { return String(part); }
      }).join(' ');
      call('log', { level: level, text: text }).catch(function () {});
    };
  }
  g.console = { log: write('info'), info: write('info'), debug: write('debug'), warn: write('warn'), error: write('error') };

  var source = null;

  // Makes the Source the file exports, as the format does: the Source export, then one named after the source, then a global Source.
  g.__install = function (mod, id) {
    var exported = mod.exports || {};
    var Constructor = exported.Source || exported[id] || g.Source;
    if (typeof Constructor !== 'function') throw new Error('The file does not export a Source class.');
    source = new Constructor();
    var methods = [];
    for (var name in source) if (typeof source[name] === 'function') methods.push(name);
    var proto = Object.getPrototypeOf(source);
    while (proto && proto !== Object.prototype) {
      Object.getOwnPropertyNames(proto).forEach(function (name) {
        if (name !== 'constructor' && typeof source[name] === 'function' && methods.indexOf(name) < 0) methods.push(name);
      });
      proto = Object.getPrototypeOf(proto);
    }
    var feeds = [];
    if (typeof source.getSourceFeeds === 'function') {
      try { feeds = source.getSourceFeeds() || []; } catch (error) { feeds = []; }
    }
    var readsExcluded = false;
    try { readsExcluded = String(Constructor).indexOf('excludedTags') >= 0; } catch (error) { readsExcluded = false; }
    return JSON.stringify({ methods: methods, feeds: feeds, readsExcluded: readsExcluded });
  };

  // Waits for a call from the app, sets the settings it brings, runs the method without waiting for it, and goes back to waiting.
  g.__loop = async function () {
    function start(request) {
      (async function () {
        settings = request.settings || {};
        var fn = source && typeof source[request.method] === 'function' ? source[request.method] : null;
        if (!fn) throw new Error('__NOT_IMPLEMENTED__:' + request.method);
        var result = await fn.apply(source, request.args || []);
        return JSON.stringify(result === undefined ? null : result);
      })().then(
        function (text) { __complete(request.id, text, null); },
        function (error) { __complete(request.id, null, String((error && error.message) || error)); }
      );
    }
    while (true) start(JSON.parse(await __next()));
  };
})();
