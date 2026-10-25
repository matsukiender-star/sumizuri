package com.sumizuri.app.source.browser

import com.sumizuri.app.source.browser.page.newWebView
import com.sumizuri.app.ui.topViewController
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.*

private class Closer(private val onClose: () -> Unit) : NSObject() {
    @ObjCAction
    fun close() = onClose()
}

private var holding: Closer? = null

// Opens the site over the app and waits until the person closes it, so they can pass the check of the site by hand.
@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
class IosChallengeSolver : ChallengeSolver {
    override suspend fun solve(url: String) {
        val done = CompletableDeferred<Unit>()
        withContext(Dispatchers.Main) {
            val top = topViewController()
            val address = NSURL.URLWithString(url)
            if (top == null || address == null) {
                done.complete(Unit)
                return@withContext
            }
            val web = newWebView(null)
            val page = UIViewController(nibName = null, bundle = null)
            page.view = web
            val screen = UINavigationController(rootViewController = page)
            screen.modalPresentationStyle = UIModalPresentationFullScreen
            val closer = Closer {
                screen.dismissViewControllerAnimated(true) { done.complete(Unit) }
            }
            page.navigationItem.rightBarButtonItem =
                UIBarButtonItem(
                    barButtonSystemItem = UIBarButtonSystemItem.UIBarButtonSystemItemDone,
                    target = closer,
                    action = NSSelectorFromString("close"),
                )
            web.loadRequest(NSURLRequest.requestWithURL(address))
            top.presentViewController(screen, animated = true, completion = null)
            // The button holds its target weakly, so the closer is kept here until the person is done.
            holding = closer
            done.invokeOnCompletion { holding = null }
        }
        done.await()
    }
}
