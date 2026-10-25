package com.sumizuri.app.ui

import platform.UIKit.*

private fun UIViewController.frontmost(): UIViewController = presentedViewController?.frontmost() ?: this

// The screen the person is looking at, so a chooser, a share sheet or a site opens over it.
internal fun topViewController(): UIViewController? = UIApplication.sharedApplication.keyWindow
    ?.rootViewController
    ?.frontmost()
