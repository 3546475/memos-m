package org.example.memosm.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the startup path into a baseline profile: process init, the launch gates, the theme
 * and the first screens the app composes.
 *
 * The target is the non-minified release variant, which has its own data, so a fresh install
 * lands on the setup flow. That is still the path that matters here: the time from the tap to
 * the first frame is dominated by process start, not by the feed.
 */
@RunWith(AndroidJUnit4::class)
class StartupBaselineProfile {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() = rule.collect(packageName = "org.example.memosm") {
        pressHome()
        startActivityAndWait()
        device.waitForIdle()
        // A logged-in install lands on the feed: give the first page and its markdown time to
        // render, then scroll so the list, card and image paths are hot as well.
        Thread.sleep(3_000)
        device.waitForIdle()
        repeat(3) {
            device.swipe(
                device.displayWidth / 2,
                device.displayHeight * 2 / 3,
                device.displayWidth / 2,
                device.displayHeight / 3,
                12
            )
            device.waitForIdle()
        }
        // Open the first card so the detail page's markdown render is recorded too. The app must
        // stay in the foreground when the rule flushes the profile: HyperOS defers the
        // SAVE_PROFILE broadcast to a backgrounded package, and the rule then reports a missing
        // profileinstaller. Never finish the activity here.
        device.click(device.displayWidth / 2, (device.displayHeight * 0.33f).toInt())
        device.waitForIdle()
        Thread.sleep(2_000)
    }
}
