package org.example.moxia.ui.component.setting

import org.example.moxia.ui.MainDestination

/**
 * Pure bookkeeping for the configurable bottom bar. Kept out of the composable so the ordering and
 * enable/disable rules can be unit tested — the UI previously conflated "enabled" with "listed",
 * which made every switch look on and resurrected a disabled destination on the next toggle.
 */

/**
 * The enabled destinations in their configured order. An empty stored list means "everything
 * enabled"; names that no longer exist (a build without map support) are dropped.
 */
internal fun enabledDestinations(
    stored: List<String>,
    available: List<MainDestination>
): List<MainDestination> {
    if (stored.isEmpty()) return available
    val enabled = stored.mapNotNull { name -> available.firstOrNull { it.name == name } }
    return enabled.ifEmpty { available }
}

/** Rows to show in the editor: enabled ones first, then the disabled ones so they can be re-added. */
internal fun destinationRows(
    enabled: List<MainDestination>,
    available: List<MainDestination>
): List<MainDestination> = enabled + available.filterNot { it in enabled }

/**
 * Turning a destination on or off. The last one cannot be turned off: there is no bottom bar
 * without a destination, and an empty stored list would be read back as "everything enabled".
 */
internal fun setDestinationEnabled(
    enabled: List<MainDestination>,
    destination: MainDestination,
    checked: Boolean
): List<MainDestination> {
    val current = enabled.toMutableList()
    if (checked) {
        if (destination !in current) current.add(destination)
    } else {
        current.remove(destination)
    }
    return if (current.isEmpty()) enabled else current
}

/** Reorders within the enabled set; positions are relative to that list, not to the editor rows. */
internal fun moveDestination(
    enabled: List<MainDestination>,
    position: Int,
    delta: Int
): List<MainDestination> {
    val target = position + delta
    if (position !in enabled.indices || target !in enabled.indices) return enabled
    val moved = enabled.toMutableList()
    val item = moved.removeAt(position)
    moved.add(target, item)
    return moved
}

