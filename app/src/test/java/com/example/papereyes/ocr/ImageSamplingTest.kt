package com.example.papereyes.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageSamplingTest {
    @Test fun smallImageIsNotDownsampled() =
        assertEquals(1, calculateInSampleSize(2000, 1500, 2400))

    @Test fun largeImageUsesPowerOfTwoSampling() =
        assertEquals(4, calculateInSampleSize(8000, 6000, 2400))

    @Test fun invalidDimensionsFailSafeToOne() =
        assertEquals(1, calculateInSampleSize(0, 6000, 2400))
    @Test fun ceilingDivisionAndLargeDimensionsAreBounded() {
        assertEquals(4, calculateInSampleSize(4801, 100, 2400))
        assertEquals(512, calculateInSampleSize(1000000, 10000, 2400))
        assertEquals(1 shl 30, calculateInSampleSize(Int.MAX_VALUE, 100, 2))
    }
}
