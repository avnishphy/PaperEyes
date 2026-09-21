package com.example.papereyes.ui.live

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveScanZoomTest {

    @Test
    fun zoomIsClampedToCameraLimits() {
        assertEquals(1f, clampZoomRatio(0.2f, 1f, 8f), 0f)
        assertEquals(4f, clampZoomRatio(4f, 1f, 8f), 0f)
        assertEquals(8f, clampZoomRatio(12f, 1f, 8f), 0f)
    }

    @Test
    fun pinchScaleBuildsOnCurrentZoom() {
        assertEquals(3f, scaleZoomRatio(2f, 1.5f, 1f, 8f), 0.0001f)
        assertEquals(1f, scaleZoomRatio(2f, 0.1f, 1f, 8f), 0.0001f)
    }

    @Test
    fun invalidCameraOrGestureValuesFailSafe() {
        assertEquals(1f, clampZoomRatio(Float.NaN, Float.NaN, Float.POSITIVE_INFINITY), 0f)
        assertEquals(2f, scaleZoomRatio(2f, Float.NaN, 1f, 8f), 0f)
        assertEquals(2f, clampZoomRatio(4f, 2f, 1f), 0f)
    }
}
