package com.example.notesapp.ui

object SnapHelper {
    var gridEnabled = false
    var snapEnabled = false
    const val GRID_SIZE = 20f
    const val SNAP_THRESHOLD = 12f

    fun snapValue(value: Float): Float {
        if (!snapEnabled) return value
        val remainder = value % GRID_SIZE
        return if (remainder < SNAP_THRESHOLD) {
            value - remainder
        } else if (GRID_SIZE - remainder < SNAP_THRESHOLD) {
            value + (GRID_SIZE - remainder)
        } else {
            value
        }
    }

    fun snapPoint(x: Float, y: Float): Pair<Float, Float> {
        if (!snapEnabled) return Pair(x, y)
        return Pair(snapValue(x), snapValue(y))
    }
}
