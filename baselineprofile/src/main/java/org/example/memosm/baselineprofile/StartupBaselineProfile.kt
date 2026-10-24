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
        // Give the first frame and the theme pass time to run, then scroll once so the
        // list-scrolling entry points are hot as well.
        Thread.sleep(1_500)
        repeat(2) {
            device.swipe(
                device.displayWidth / 2,
                device.displayHeight * 2 / 3,
                device.displayWidth / 2,
                device.displayHeight / 3,
                12
            )
            device.waitForIdle()
        }
        Thread.sleep(1_500)
    }
}
