package com.sumizuri.sync.core

// The few pieces of cryptography and packing that each system does with what it has: the Java classes and Bouncy Castle on Android and the desktop, and the libraries of Apple on an iPhone. Everything that is built from them is in common code, so every device does the same thing with the same bytes.

// Bytes that cannot be guessed.
internal expect fun secureRandomBytes(count: Int): ByteArray

// AES-256-GCM. The sealed bytes are the locked bytes followed by the 16 byte tag, and bound is data that is checked but not hidden.
internal expect fun aesGcmSeal(key: ByteArray, nonce: ByteArray, plain: ByteArray, bound: ByteArray): ByteArray

// The plain bytes, or nothing when the key is wrong or the bytes or the bound data were changed.
internal expect fun aesGcmOpen(key: ByteArray, nonce: ByteArray, sealed: ByteArray, bound: ByteArray): ByteArray?

internal expect fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray

// Argon2id, version 1.3, with a tag of 32 bytes.
internal expect fun argon2id(secret: ByteArray, salt: ByteArray, kdf: Kdf): ByteArray

// A zlib stream, packed as small as it goes.
internal expect fun zlibDeflate(data: ByteArray): ByteArray

// The bytes of a zlib stream. It throws when the stream ends early or is not valid, or when it holds more than max bytes.
internal expect fun zlibInflate(data: ByteArray, max: Int): ByteArray
