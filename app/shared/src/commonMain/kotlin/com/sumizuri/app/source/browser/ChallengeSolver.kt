package com.sumizuri.app.source.browser

// Opens the site in a window the reader can see, so a bot check the hidden browser could not pass can be passed by a person. It returns when the window is closed. The clearance stays in the browser, so the hidden browser has it from then on.
fun interface ChallengeSolver {
    suspend fun solve(url: String)
}
