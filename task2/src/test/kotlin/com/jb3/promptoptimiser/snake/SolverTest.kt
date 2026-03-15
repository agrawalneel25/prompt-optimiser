package com.jb3.promptoptimiser.snake

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Cross-cutting integration tests: both solvers run on the same boards.
 * Strategy-specific and budget-specific tests live in their own files.
 */
class SolverTest {

    private val solvers = listOf(BaselineSolver(), SweepStrideSolver(), BoustrophedonSolver())

    private fun assertBothSolve(config: BoardConfig) {
        for (solver in solvers) {
            val result = runSimulation(solver, config)
            assertTrue(result.withinBudget,
                "${solver.name} failed on $config: ${result.run.stepsUsed} steps, budget ${config.budgetLimit}")
        }
    }

    @Test fun `both solve 10x10 with apple at far corner`() =
        assertBothSolve(BoardConfig(10, 10, Point(0, 0), Point(9, 9)))

    @Test fun `both solve 20x30 with off-centre start`() =
        assertBothSolve(BoardConfig(20, 30, Point(10, 15), Point(5, 2)))

    @Test fun `both solve 7x7 with apple one step behind start`() =
        assertBothSolve(BoardConfig(7, 7, Point(3, 3), Point(2, 3)))

    @Test fun `both solve 1x1 board`() =
        assertBothSolve(BoardConfig(1, 1, Point(0, 0), Point(0, 0)))

    @Test fun `both solve 2x3 board`() =
        assertBothSolve(BoardConfig(2, 3, Point(0, 0), Point(1, 2)))

    // Property loop: both strategies on all positions of a 12x12 board
    @Test
    fun `both solve all apple positions on 12x12 board`() {
        for (ax in 0 until 12) for (ay in 0 until 12) {
            assertBothSolve(BoardConfig(12, 12, Point(0, 0), Point(ax, ay)))
        }
    }
}
