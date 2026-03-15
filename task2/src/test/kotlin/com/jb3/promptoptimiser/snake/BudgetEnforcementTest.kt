package com.jb3.promptoptimiser.snake

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BudgetEnforcementTest {

    private val solvers = listOf(BaselineSolver(), SweepStrideSolver())

    // ── zero budget ──────────────────────────────────────────────────────────

    @Test
    fun `maxSteps=0 returns immediately without sending any moves`() {
        for (solver in solvers) {
            val engine = SimulationEngine(10, 10, Point(0, 0), Point(5, 5))
            val result = solver.solve(engine, maxSteps = 0L)
            assertFalse(result.won, "${solver.name}: should not win with zero budget")
            assertEquals(0L, result.stepsUsed, "${solver.name}: should use 0 steps")
            assertEquals(0L, engine.stepCount, "${solver.name}: engine should receive 0 signals")
        }
    }

    // ── tight budget ─────────────────────────────────────────────────────────

    @Test
    fun `solver never sends more moves than maxSteps allows`() {
        for (maxSteps in listOf(1L, 3L, 10L, 17L, 50L)) {
            for (solver in solvers) {
                val engine = SimulationEngine(100, 100, Point(0, 0), Point(99, 99))
                val result = solver.solve(engine, maxSteps = maxSteps)
                assertTrue(result.stepsUsed <= maxSteps,
                    "${solver.name} with maxSteps=$maxSteps: stepsUsed=${result.stepsUsed}")
            }
        }
    }

    // ── steps reported match engine received ─────────────────────────────────

    @Test
    fun `RunResult stepsUsed exactly matches SimulationEngine stepCount`() {
        val configs = listOf(
            BoardConfig(5, 5,    Point(0, 0), Point(4, 4)),
            BoardConfig(1, 50,   Point(0, 0), Point(0, 25)),
            BoardConfig(50, 1,   Point(0, 0), Point(25, 0)),
            BoardConfig(10, 7,   Point(3, 2), Point(7, 5)),
        )
        for (config in configs) {
            for (solver in solvers) {
                val engine = SimulationEngine(config.width, config.height, config.start, config.apple)
                val result = solver.solve(engine, maxSteps = config.budgetLimit)
                assertEquals(engine.stepCount, result.stepsUsed,
                    "${solver.name} on $config: reported ${result.stepsUsed} but engine saw ${engine.stepCount}")
            }
        }
    }

    // ── budget < apple distance ───────────────────────────────────────────────

    @Test
    fun `solver stops and returns won=false when budget is smaller than required steps`() {
        // Apple is far away; budget is very tight — solver cannot win
        for (solver in solvers) {
            val engine = SimulationEngine(1000, 1000, Point(0, 0), Point(999, 999))
            val result = solver.solve(engine, maxSteps = 5L)
            assertFalse(result.won, "${solver.name}: should not win with only 5 steps on 1000×1000")
            assertTrue(result.stepsUsed <= 5L)
        }
    }

    // ── budget exactly at apple ───────────────────────────────────────────────

    @Test
    fun `solver wins on the last allowed step`() {
        // Apple is exactly N steps away via the spiral's first leg.
        // BaselineSolver starts with RIGHT moves, so apple at (3,0) is found after 3 RIGHT steps.
        val engine = SimulationEngine(10, 10, Point(0, 0), Point(3, 0))
        val result = BaselineSolver().solve(engine, maxSteps = 3L)
        assertTrue(result.won)
        assertEquals(3L, result.stepsUsed)
    }

    // ── budget is StrategyResult.withinBudget ─────────────────────────────────

    @Test
    fun `withinBudget is false when solver exceeds 35S`() {
        // Confirm the helper correctly flags over-budget runs.
        // We fake an over-budget result by checking the data class logic directly.
        val config = BoardConfig(2, 2, Point(0, 0), Point(1, 1))  // area=4, budget=140
        val overBudget = StrategyResult(config, RunResult(won = true, stepsUsed = 141L, strategyName = "test"))
        assertFalse(overBudget.withinBudget)
    }

    @Test
    fun `withinBudget is true when won within 35S`() {
        val config = BoardConfig(2, 2, Point(0, 0), Point(1, 1))
        val ok = StrategyResult(config, RunResult(won = true, stepsUsed = 140L, strategyName = "test"))
        assertTrue(ok.withinBudget)
    }

    @Test
    fun `withinBudget is false when won=false even if steps are low`() {
        val config = BoardConfig(5, 5, Point(0, 0), Point(4, 4))
        val notWon = StrategyResult(config, RunResult(won = false, stepsUsed = 1L, strategyName = "test"))
        assertFalse(notWon.withinBudget)
    }
}
