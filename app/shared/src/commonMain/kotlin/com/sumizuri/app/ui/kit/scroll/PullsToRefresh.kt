package com.sumizuri.app.ui.kit.scroll

// Whether pages are refreshed by pulling them down. A finger does that, and a mouse wheel on a desktop cannot let go of the pull, so the spinner stays until the wheel is turned back. A desktop has the refresh buttons instead.
expect val pullsToRefresh: Boolean
