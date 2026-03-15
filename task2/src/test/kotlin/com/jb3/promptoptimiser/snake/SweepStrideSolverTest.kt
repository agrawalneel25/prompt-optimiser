package com.jb3.promptoptimiser.snake

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SweepStrideSolverTest {

    private val solver = SweepStrideSolver()

    @Test
    fun `name is SweepStride`() = assertEquals("SweepStride", solver.name)

    // ── helper ───────────────────────────────────────────────────────────────

    private fun assertWinsWithinBudget(config: BoardConfig, msg: String = config.toString()) {
        val result = runSimulation(solver, config)
        assertTrue(result.withinBudget, "$msg — steps=${result.run.stepsUsed} budget=${config.budgetLimit}")
    }

    // ── thin boards: 1×N ────────────────────────────────────────────────────

    // On a 1×N board, DOWN/UP are no-ops (height=1 means y never changes).
    // RIGHT alone cycles through all N columns. With stride=1, the sweep
    // (RIGHT, DOWN) degenerates to pure RIGHT steps and covers all cells
    // in exactly N moves. This is provably ≤ 2S.

    @Test
    fun `wins on 1x1 board`() = assertWinsWithinBudget(BoardConfig(1, 1, Point(0, 0), Point(0, 0)))

    @Test
    fun `wins on all 1xN boards up to N=300, all apple positions`() {
        for (n in 1..300) {
            for (appleY in 0 until n) {
                assertWinsWithinBudget(
                    BoardConfig(1, n, Point(0, 0), Point(0, appleY)),
                    "1×$n apple at y=$appleY"
                )
            }
        }
    }

    // ── thin boards: N×1 ────────────────────────────────────────────────────

    @Test
    fun `wins on all Nx1 boards up to N=300, all apple positions`() {
        for (n in 1..300) {
            for (appleX in 0 until n) {
                assertWinsWithinBudget(
                    BoardConfig(n, 1, Point(0, 0), Point(appleX, 0)),
                    "${n}×1 apple at x=$appleX"
                )
            }
        }
    }

    // ── square boards ────────────────────────────────────────────────────────

    @Test
    fun `wins on square boards with apple at far corner`() {
        for (size in listOf(1, 2, 5, 10, 20, 50, 100)) {
            assertWinsWithinBudget(BoardConfig(size, size, Point(0, 0), Point(size - 1, size - 1)))
        }
    }

    @Test
    fun `wins on all positions of a 15x15 board`() {
        for (ax in 0 until 15) for (ay in 0 until 15) {
            assertWinsWithinBudget(BoardConfig(15, 15, Point(0, 0), Point(ax, ay)))
        }
    }

    // ── coprime dimensions ───────────────────────────────────────────────────

    // These boards have gcd(A, B) = 1. Coprimality has no special significance
    // for the stride-1 sweep (which works on all boards), but these dimensions
    // exercise the solver across a variety of shapes.

    @Test
    fun `wins on coprime-dimension boards`() {
        val coprimePairs = listOf(7 to 11, 13 to 17, 3 to 100, 100 to 3, 11 to 13, 97 to 101)
        for ((w, h) in coprimePairs) {
            assertWinsWithinBudget(BoardConfig(w, h, Point(0, 0), Point(w / 2, h / 2)),
                "${w}×${h} (coprime)")
        }
    }

    // ── boards with large gcd ────────────────────────────────────────────────

    // When gcd(A, B) is large, stride m = gcd value would skip many cells.
    // Stride 1 is always coprime with any A, so the solver is unaffected.
    // These tests confirm that behaviour.

    @Test
    fun `wins on boards with large gcd(A,B)`() {
        // gcd(6,4)=2, gcd(12,8)=4, gcd(50,100)=50, gcd(100,100)=100
        val largGcdPairs = listOf(6 to 4, 12 to 8, 50 to 100, 100 to 100, 48 to 36)
        for ((w, h) in largGcdPairs) {
            assertWinsWithinBudget(BoardConfig(w, h, Point(0, 0), Point(w / 2, h / 2)),
                "${w}×${h} (gcd=${gcd(w, h)})")
        }
    }

    // ── stride-1 guarantee ───────────────────────────────────────────────────

    // With maxStrides=1, only the stride-1 phase runs. This is the proved
    // ≤ 2S guarantee: the (RIGHT, DOWN) sweep with stride 1 visits every cell
    // of any A×B torus because gcd(1, A) = 1 always holds.

    @Test
    fun `stride-1-only solver wins on all boards in the exhaustive small-board set`() {
        val stride1Solver = SweepStrideSolver(maxStrides = 1)
        // All boards up to area 50: every (w, h) with w*h ≤ 50
        for (w in 1..50) {
            for (h in 1..(50 / w)) {
                for (ax in 0 until w) for (ay in 0 until h) {
                    val config = BoardConfig(w, h, Point(0, 0), Point(ax, ay))
                    val result = runSimulation(stride1Solver, config)
                    assertTrue(result.withinBudget,
                        "stride-1 failed on ${w}×${h} apple=($ax,$ay): ${result.run.stepsUsed}/${config.budgetLimit}")
                }
            }
        }
    }

    @Test
    fun `stride-1-only solver uses at most 2S steps on any board`() {
        val stride1Solver = SweepStrideSolver(maxStrides = 1)
        val boards = listOf(
            BoardConfig(1, 1000, Point(0, 0), Point(0, 999)),
            BoardConfig(1000, 1, Point(0, 0), Point(999, 0)),
            BoardConfig(100, 100, Point(0, 0), Point(99, 99)),
            BoardConfig(7, 11, Point(0, 0), Point(6, 10)),
        )
        for (config in boards) {
            val result = runSimulation(stride1Solver, config)
            val twoS = 2L * config.area
            assertTrue(result.run.stepsUsed <= twoS,
                "${config}: stepsUsed=${result.run.stepsUsed} exceeds 2S=$twoS")
        }
    }

    // ── large thin boards ────────────────────────────────────────────────────

    @Test
    fun `wins on 1x100000 with apple at last cell`() =
        assertWinsWithinBudget(BoardConfig(1, 100_000, Point(0, 0), Point(0, 99_999)))

    @Test
    fun `wins on 100000x1 with apple at last cell`() =
        assertWinsWithinBudget(BoardConfig(100_000, 1, Point(0, 0), Point(99_999, 0)))

    // ── off-centre start ─────────────────────────────────────────────────────

    @Test
    fun `wins when start is not at origin`() {
        assertWinsWithinBudget(BoardConfig(20, 30, Point(10, 15), Point(5, 2)))
        assertWinsWithinBudget(BoardConfig(50, 50, Point(49, 49), Point(0, 0)))
    }

    // ── private helper ───────────────────────────────────────────────────────

    private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}
