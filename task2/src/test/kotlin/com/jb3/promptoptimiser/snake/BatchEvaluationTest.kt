package com.jb3.promptoptimiser.snake

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BatchEvaluationTest {

    // ── runBatch on empty input ───────────────────────────────────────────────

    @Test
    fun `empty batch produces zero totals`() {
        val summary = runBatch(SweepStrideSolver(), emptyList())
        assertEquals(0, summary.total)
        assertEquals(0, summary.wins)
        assertEquals(0, summary.withinBudget)
        assertEquals(0.0, summary.winRate)
        assertEquals(0.0, summary.avgBudgetUsed)
        assertEquals(0.0, summary.maxBudgetUsed)
    }

    // ── runBatch: all wins ────────────────────────────────────────────────────

    @Test
    fun `winRate is 1_0 when solver wins every board`() {
        val configs = listOf(
            BoardConfig(5,  5,  Point(0, 0), Point(4, 4)),
            BoardConfig(1,  10, Point(0, 0), Point(0, 9)),
            BoardConfig(10, 1,  Point(0, 0), Point(9, 0)),
        )
        val summary = runBatch(SweepStrideSolver(), configs)
        assertEquals(configs.size, summary.total)
        assertEquals(configs.size, summary.wins)
        assertEquals(1.0, summary.winRate)
        assertTrue(summary.withinBudget == configs.size)
    }

    // ── runBatch: zero budget forces all losses ───────────────────────────────

    @Test
    fun `winRate is 0_0 when budget is zero on every board`() {
        // Pass maxSteps=0 by creating a solver wrapped in a custom engine that
        // never returns true. Easier: use BoardConfig with a solver whose budget
        // is set to 0 by the evaluation harness — which uses config.budgetLimit.
        // For a 1×1 board the budget is 35. So use a different approach:
        // manually run with maxSteps=0 on each config.
        val configs = listOf(
            BoardConfig(10, 10, Point(0, 0), Point(9, 9)),
            BoardConfig(5,  3,  Point(0, 0), Point(4, 2)),
        )
        for (config in configs) {
            val engine = SimulationEngine(config.width, config.height, config.start, config.apple)
            val result = SweepStrideSolver().solve(engine, maxSteps = 0L)
            assertFalse(result.won)
            assertEquals(0L, result.stepsUsed)
        }
    }

    // ── BatchSummary invariants ───────────────────────────────────────────────

    @Test
    fun `withinBudget never exceeds wins`() {
        val configs = (1..10).map { k ->
            BoardConfig(k, k, Point(0, 0), Point(k - 1, k - 1))
        }
        val summary = runBatch(BaselineSolver(), configs)
        assertTrue(summary.withinBudget <= summary.wins,
            "withinBudget=${summary.withinBudget} > wins=${summary.wins}")
    }

    @Test
    fun `wins never exceeds total`() {
        val configs = listOf(
            BoardConfig(3, 3, Point(0, 0), Point(2, 2)),
            BoardConfig(1, 5, Point(0, 0), Point(0, 4)),
        )
        val summary = runBatch(SweepStrideSolver(), configs)
        assertTrue(summary.wins <= summary.total)
    }

    @Test
    fun `maxBudgetUsed is the worst individual fraction among winning runs`() {
        val configs = listOf(
            BoardConfig(5,  5,  Point(0, 0), Point(4, 4)),   // harder to find
            BoardConfig(5,  5,  Point(0, 0), Point(1, 0)),   // trivially close
        )
        val summary = runBatch(SweepStrideSolver(), configs)
        val results = configs.map { runSimulation(SweepStrideSolver(), it) }
        val expectedMax = results.filter { it.run.won }.maxOf { it.budgetUsed }
        assertEquals(expectedMax, summary.maxBudgetUsed, 1e-9)
    }

    @Test
    fun `avgBudgetUsed is the mean of winning runs' budget fractions`() {
        val configs = listOf(
            BoardConfig(5, 5, Point(0, 0), Point(4, 4)),
            BoardConfig(5, 5, Point(0, 0), Point(2, 2)),
        )
        val results = configs.map { runSimulation(SweepStrideSolver(), it) }
        val expectedAvg = results.filter { it.run.won }.map { it.budgetUsed }.average()
        val summary = runBatch(SweepStrideSolver(), configs)
        assertEquals(expectedAvg, summary.avgBudgetUsed, 1e-9)
    }

    // ── strategyName in summary ───────────────────────────────────────────────

    @Test
    fun `BatchSummary carries the correct strategy name`() {
        val configs = listOf(BoardConfig(3, 3, Point(0, 0), Point(2, 2)))
        assertEquals("BaselineSpiral", runBatch(BaselineSolver(),    configs).strategyName)
        assertEquals("SweepStride",    runBatch(SweepStrideSolver(), configs).strategyName)
    }

    // ── runSimulation creates a fresh engine each time ────────────────────────

    @Test
    fun `running the same config twice produces the same result`() {
        val config = BoardConfig(7, 11, Point(3, 5), Point(6, 10))
        val r1 = runSimulation(SweepStrideSolver(), config)
        val r2 = runSimulation(SweepStrideSolver(), config)
        assertEquals(r1.run.won,       r2.run.won)
        assertEquals(r1.run.stepsUsed, r2.run.stepsUsed)
    }
}
