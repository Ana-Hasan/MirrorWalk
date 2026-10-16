package com.mirrorwalk.data

/**
 * A route available for pace/path comparison. Future blocks can implement this
 * interface by loading a [RunEntity] from Room instead of using canned points.
 */
interface GhostRunSource {
    fun loadGhostRun(): GhostRun
}

data class GhostRun(
    val id: String,
    val label: String,
    val startedAt: Long,
    val points: List<RecordedPoint>
)

/**
 * Feature 2's temporary comparison route. The timestamps are real epoch values
 * from a fixed prior run and increase as the runner progresses around the loop.
 */
object HardcodedGhostRunSource : GhostRunSource {
    override fun loadGhostRun(): GhostRun = GhostRun(
        id = "demo-river-loop-01",
        label = "Your best: River Loop",
        startedAt = 1_771_251_200_000L,
        points = DEMO_POINTS
    )

    private val DEMO_POINTS = listOf(
        RecordedPoint(12.971900, 77.594700, 1_771_251_200_000L),
        RecordedPoint(12.972080, 77.594910, 1_771_251_230_000L),
        RecordedPoint(12.972300, 77.595190, 1_771_251_260_000L),
        RecordedPoint(12.972560, 77.595430, 1_771_251_290_000L),
        RecordedPoint(12.972850, 77.595590, 1_771_251_320_000L),
        RecordedPoint(12.973120, 77.595710, 1_771_251_350_000L),
        RecordedPoint(12.973400, 77.595640, 1_771_251_380_000L),
        RecordedPoint(12.973610, 77.595390, 1_771_251_410_000L),
        RecordedPoint(12.973690, 77.595080, 1_771_251_440_000L),
        RecordedPoint(12.973570, 77.594800, 1_771_251_470_000L),
        RecordedPoint(12.973310, 77.594650, 1_771_251_500_000L),
        RecordedPoint(12.973030, 77.594690, 1_771_251_530_000L),
        RecordedPoint(12.972760, 77.594860, 1_771_251_560_000L),
        RecordedPoint(12.972500, 77.594980, 1_771_251_590_000L),
        RecordedPoint(12.972240, 77.594910, 1_771_251_620_000L),
        RecordedPoint(12.972020, 77.594730, 1_771_251_650_000L),
        RecordedPoint(12.971900, 77.594700, 1_771_251_680_000L)
    )
}
