package com.example.papereyes.domain.telemetry

/** Local numeric timing only. No OCR text, query, identifiers or image paths. */
enum class ScanStage(val wire: String) {
    FRAME_TIME("frame_time"), PREVIEW_OCR_START("preview_ocr_start"), PREVIEW_OCR_END("preview_ocr_end"),
    CAPTURE_START("capture_start"), CAPTURE_END("capture_end"),
    OCR_START("ocr_start"), OCR_END("ocr_end"), LAYOUT_END("layout_end"),
    LOOKUP_START("lookup_start"), LOOKUP_END("lookup_end"), RESULT_DISPLAY("result_display")
}
data class ScanEvent(val stage: ScanStage, val nanos: Long)
class ScanTrace(private val clock: () -> Long = System::nanoTime) {
    private val events = mutableListOf<ScanEvent>()
    @Synchronized fun mark(stage: ScanStage) { if (events.size < 64) events += ScanEvent(stage, clock()) }
    @Synchronized fun snapshot(): List<ScanEvent> = events.toList()

    fun durationMillis(start: ScanStage, end: ScanStage): Double? {
        var from: Long? = null
        var total = 0L
        var pairs = 0
        for (event in snapshot()) {
            if (event.stage == start) from = event.nanos
            if (event.stage == end && from != null && event.nanos >= from) {
                total += event.nanos - from; from = null; pairs++
            }
        }
        return if (pairs == 0) null else total / 1_000_000.0
    }

    fun toNumericJson(): String {
        val copy = snapshot()
        val origin = copy.firstOrNull()?.nanos ?: 0L
        val stages = copy.joinToString(",") { "{\"stage\":\"${it.stage.wire}\",\"ms\":${(it.nanos-origin)/1_000_000.0}}" }
        return "{\"events\":[$stages],\"total_ms\":${durationMillis(ScanStage.FRAME_TIME,ScanStage.RESULT_DISPLAY)}," +
            "\"ocr_ms\":${durationMillis(ScanStage.OCR_START,ScanStage.OCR_END)}," +
            "\"preview_ocr_ms\":${durationMillis(ScanStage.PREVIEW_OCR_START,ScanStage.PREVIEW_OCR_END)}," +
            "\"layout_ms\":${durationMillis(ScanStage.OCR_END,ScanStage.LAYOUT_END)}," +
            "\"lookup_including_pacing_ms\":${durationMillis(ScanStage.LOOKUP_START,ScanStage.LOOKUP_END)}}"
    }
}
