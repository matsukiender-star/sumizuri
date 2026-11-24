package com.sumizuri.app.data.homeserver

import okio.Path
import java.util.concurrent.ConcurrentHashMap

// One server for each folder of the app, whoever asks.
private val hosts = ConcurrentHashMap<String, HomeServer>()

actual fun homeHostOf(appFolder: Path): HomeHost? = hosts
    .getOrPut(appFolder.toString()) { HomeServer(appFolder.toFile().resolve(HomeFolder)) }
    .takeIf { it.available }
