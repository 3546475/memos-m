package org.example.memosm.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MemosBenchmarks {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupCold() = rule.measureRepeated(
        packageName = "org.example.memosm",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.DEFAULT,
        startupMode = StartupMode.COLD,
        iterations = 5,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
        device.waitForIdle()
    }

    /**
     * Scrolls the feed for frame timings. Needs the app signed in: the benchmark variant shares the
     * release package, so it sees whatever the installed release app last had.
     */
    @Test
    fun feedScroll() = rule.measureRepeated(
        packageName = "org.example.memosm",
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.None(),
        startupMode = StartupMode.WARM,
        iterations = 3,
        setupBlock = {
            startActivityAndWait()
            device.waitForIdle()
            Thread.sleep(3_000)
        },
    ) {
        repeat(3) {
            device.swipe(
                device.displayWidth / 2,
                (device.displayHeight * 0.72f).toInt(),
                device.displayWidth / 2,
                (device.displayHeight * 0.28f).toInt(),
                30
            )
            device.waitForIdle()
            Thread.sleep(300)
        }
    }

    /**
     * Starts a pull-to-refresh and scrolls while the network round-trip and the list merge are in
     * flight, so list updates land on top of scrolling instead of an idle feed.
     */
    @Test
    fun scrollDuringRefresh() = rule.measureRepeated(
        packageName = "org.example.memosm",
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.None(),
        startupMode = StartupMode.WARM,
        iterations = 3,
        setupBlock = {
            startActivityAndWait()
            device.waitForIdle()
            Thread.sleep(3_000)
        },
    ) {
        // Pull down far enough to arm the refresh.
        device.swipe(
            device.displayWidth / 2,
            (device.displayHeight * 0.28f).toInt(),
            device.displayWidth / 2,
            (device.displayHeight * 0.80f).toInt(),
            40
        )
        device.waitForIdle()
        // Scroll while it settles.
        repeat(2) {
            device.swipe(
                device.displayWidth / 2,
                (device.displayHeight * 0.72f).toInt(),
                device.displayWidth / 2,
                (device.displayHeight * 0.28f).toInt(),
                30
            )
            device.waitForIdle()
            Thread.sleep(200)
        }
    }
}
