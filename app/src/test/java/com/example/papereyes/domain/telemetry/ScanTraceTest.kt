package com.example.papereyes.domain.telemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
class ScanTraceTest {
 @Test fun sumsFallbackAttemptsAndKeepsTotalSeparate() {
  var clock=0L;val trace=ScanTrace{clock}
  trace.mark(ScanStage.FRAME_TIME)
  repeat(2){trace.mark(ScanStage.OCR_START);clock+=10000000;trace.mark(ScanStage.OCR_END);clock+=2000000;trace.mark(ScanStage.LAYOUT_END)}
  clock+=100000000;trace.mark(ScanStage.RESULT_DISPLAY)
  assertEquals(20.0,trace.durationMillis(ScanStage.OCR_START,ScanStage.OCR_END))
  assertEquals(4.0,trace.durationMillis(ScanStage.OCR_END,ScanStage.LAYOUT_END))
  assertEquals(124.0,trace.durationMillis(ScanStage.FRAME_TIME,ScanStage.RESULT_DISPLAY))
  assertTrue(trace.toNumericJson().contains("\"total_ms\":124.0"))
 }
 @Test fun unfinishedStagesAreNotFakedAndTraceIsBounded() {
  val trace=ScanTrace{0};trace.mark(ScanStage.OCR_START)
  assertEquals(null,trace.durationMillis(ScanStage.OCR_START,ScanStage.OCR_END))
  repeat(1000){trace.mark(ScanStage.FRAME_TIME)};assertEquals(64,trace.snapshot().size)
 }
}
