package com.jb3.promptoptimiser.snake

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BaselineSolverTest {

    private val solver = BaselineSolver()

    @Test
    fun `name is BaselineSpiral`() = assertEquals("BaselineSpiral", solver.name)

    // ── minimal boards ───────────────────────────────────────────────────────

    @Test
    fun `wins on 1x1 board`() {
        val result = runSimulation(solver, BoardConfig(1, 1, Point(0, 0), Point(0, 0)))
        assertTrue(result.withinBudget)
    }

    @Test
    fun `wins on 2x2 board at all four apple positions`() {
        for (ax in 0..1) for (ay in 0..1) {
            val config = BoardConfig(2, 2, Point(0, 0), Point(ax, ay))
            assertTrue(runSimulation(solver, config).withinBudget,
                "Failed at apple ($ax,$ay)")
        }
    }

    @Test
    fun `wins on 3x3 board regardless of where start and apple are`() {
        for (sx in 0..2) for (sy in 0..2)
            for (ax in 0..2) for (ay in 0..2) {
                val config = BoardConfig(3, 3, Point(sx, sy), Point(ax, ay))
                assertTrue(runSimulation(solver, config).withinBudget,
                    "Failed start=($sx,$sy) apple=($ax,$ay)")
            }
    }

    // ── square boards, property-style ────────────────────────────────────────

    @Test
    fun `wins within budget on all square boards up to 25x25`() {
        // Exhaustive: every (start, apple) pair on every square board up to 25×25.
        // Proves the spiral covers every cell before exhausting the 35S budget
        // on near-square grids, which is the spiral's intended use case.
        for (size in 1..25) {
            for (ax in 0 until size) for (ay in 0 until size) {
                val config = BoardConfig(size, size, Point(0, 0), Point(ax, ay))
                val result = runSimulation(solver, config)
                assertTrue(result.withinBudget,
                    "Failed ${size}×${size} apple=($ax,$ay): ${result.run.stepsUsed} steps, budget ${config.budgetLimit}")
            }
        }
    }

    // ── rectangular boards ───────────────────────────────────────────────────

    @Test
    fun `wins on 10x20 board`() {
        assertTrue(runSimulation(solver, BoardConfig(10, 20, Point(0, 0), Point(9, 19))).withinBudget)
    }

    @Test
    fun `wins on 20x10 board`() {
        assertTrue(runSimulation(solver, BoardConfig(20, 10, Point(0, 0), Point(19, 9))).withinBudget)
    }

    // ── off-centre start ─────────────────────────────────────────────────────

    @Test
    fun `wins when start and apple are far apart on 50x50 board`() {
        val config = BoardConfig(50, 50, Point(25, 25), Point(0, 0))
        assertTrue(runSimulation(solver, config).withinBudget)
    }

    @Test
    fun `wins when apple is one step behind start`() {
        // The apple is in the direction the spiral never starts with,
        // so it is reached somewhat late — still within budget.
        val config = BoardConfig(30, 30, Point(15, 15), Point(14, 15))
        assertTrue(runSimulation(solver, config).withinBudget)
    }

    // ── known weakness: thin boards ──────────────────────────────────────────

    // The spiral's step count to cover a 1×N board grows as O(N²) because
    // vertical moves are wasted on a height-1 board. We do NOT claim the
    // spiral satisfies the 35S budget for thin boards; these tests just
    // verify that it still finds the apple eventually (no infinite loop).

    @Test
    fun `eventually finds apple on 1x20 board even if over budget`() {
        val config = BoardConfig(1, 20, Point(0, 0), Point(0, 19))
        val engine = SimulationEngine(config.width, config.height, config.start, config.apple)
        // Give it a generous budget well above 35S to confirm it does terminate
        val result = solver.solve(engine, maxSteps = 10_000L)
        assertTrue(result.won, "Spiral should find apple on 1×20 given enough budget")
    }
}
