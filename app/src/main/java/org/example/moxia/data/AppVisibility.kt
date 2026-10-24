package org.example.moxia.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide "is any activity visible" signal.
 *
 * The view model is application-scoped now, so it outlives every activity and cannot rely on
 * ViewModel.onCleared() to release polling work. The activity lifecycle callbacks registered in
 * [org.example.moxia.MemosApplication] drive this counter instead.
 */
object AppVisibility {
    private var startedActivities = 0
    private val _foreground = MutableStateFlow(true)
    val foreground: StateFlow<Boolean> = _foreground.asStateFlow()

    /** Test hook: pretend one activity is visible. */
    @Synchronized
    internal fun resetForTest() {
        startedActivities = 1
        _foreground.value = true
    }

    @Synchronized
    fun onActivityStarted() {
        startedActivities++
        if (startedActivities == 1) _foreground.value = true
    }

    @Synchronized
    fun onActivityStopped() {
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
        if (startedActivities == 0) _foreground.value = false
    }
}
