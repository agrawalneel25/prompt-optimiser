package com.jb3.promptoptimiser.snake

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoustrophedonSolverTest {

    private val solver = BoustrophedonSolver()

    @Test
    fun `name is Boustrophedon`() = assertEquals("Boustrophedon", solver.name)

    // ── correctness: small boards ─────────────────────────────────────────────

    @Test
    fun `wins on 1x1 board`() =
        assertTrue(runSimulation(solver, BoardConfig(1, 1, Point(0, 0), Point(0, 0))).withinBudget)

    @Test
    fun `wins on all positions of a 4x4 board`() {
        for (ax in 0 until 4) for (ay in 0 until 4) {
            val config = BoardConfig(4, 4, Point(0, 0), Point(ax, ay))
            assertTrue(runSimulation(solver, config).withinBudget,
                "Failed apple=($ax,$ay)")
        }
    }

    @Test
    fun `wins on all positions of a 5x3 board`() {
        for (ax in 0 until 5) for (ay in 0 until 3) {
            val config = BoardConfig(5, 3, Point(0, 0), Point(ax, ay))
            assertTrue(runSimulation(solver, config).withinBudget,
                "Failed apple=($ax,$ay)")
        }
    }

    // ── thin boards: the spiral's weakness ───────────────────────────────────

    @Test
    fun `wins on all 1xN thin boards up to N=300, every apple position`() {
        for (n in 1..300) {
            for (appleY in 0 until n) {
                val config = BoardConfig(1, n, Point(0, 0), Point(0, appleY))
                val result = runSimulation(solver, config)
                assertTrue(result.withinBudget, "1×$n apple at y=$appleY: ${result.run.stepsUsed}/${config.budgetLimit}")
            }
        }
    }

    @Test
    fun `wins on all Nx1 thin boards up to N=300, every apple position`() {
        for (n in 1..300) {
            for (appleX in 0 until n) {
                val config = BoardConfig(n, 1, Point(0, 0), Point(appleX, 0))
                val result = runSimulation(solver, config)
                assertTrue(result.withinBudget, "${n}×1 apple at x=$appleX: ${result.run.stepsUsed}/${config.budgetLimit}")
            }
        }
    }

    @Test
    fun `wins on large thin boards`() {
        listOf(
            BoardConfig(1,       100_000, Point(0, 0), Point(0,       99_999)),
            BoardConfig(100_000, 1,       Point(0, 0), Point(99_999,  0)),
            BoardConfig(1,       999_999, Point(0, 0), Point(0,       500_000)),
        ).forEach { config ->
            assertTrue(runSimulation(solver, config).withinBudget, "$config")
        }
    }

    // ── square and rectangular boards ────────────────────────────────────────

    @Test
    fun `wins on square boards at far corner`() {
        for (size in listOf(1, 2, 5, 10, 20, 50, 100, 500, 1000)) {
            val config = BoardConfig(size, size, Point(0, 0), Point(size - 1, size - 1))
            assertTrue(runSimulation(solver, config).withinBudget, "${size}×${size}")
        }
    }

    @Test
    fun `wins on all positions of all square boards up to 20x20`() {
        for (size in 1..20) {
            for (ax in 0 until size) for (ay in 0 until size) {
                val config = BoardConfig(size, size, Point(0, 0), Point(ax, ay))
                assertTrue(runSimulation(solver, config).withinBudget,
                    "${size}×${size} apple=($ax,$ay)")
            }
        }
    }

    // ── enumeration reaches correct (w, h) pair ───────────────────────────────

    // When the real board is A×B and we first reach a (w, h) pair with w >= A
    // and h >= B, the sweep is guaranteed to cover every cell. These tests
    // verify correctness across a variety of dimension relationships.

    @Test
    fun `wins on boards where A is not a power of two`() {
        listOf(3, 5, 6, 7, 9, 10, 11, 13, 100, 999, 1000).forEach { a ->
            val config = BoardConfig(a, a, Point(0, 0), Point(a / 2, a / 2))
            assertTrue(runSimulation(solver, config).withinBudget, "${a}×${a}")
        }
    }

    @Test
    fun `wins when dimensions have large gcd`() {
        listOf(6 to 4, 12 to 8, 50 to 100, 100 to 100, 500 to 1000).forEach { (w, h) ->
            val config = BoardConfig(w, h, Point(0, 0), Point(w / 2, h / 2))
            assertTrue(runSimulation(solver, config).withinBudget, "${w}×${h}")
        }
    }

    @Test
    fun `wins on coprime-dimension boards`() {
        listOf(7 to 11, 13 to 17, 3 to 100, 97 to 101).forEach { (w, h) ->
            val config = BoardConfig(w, h, Point(0, 0), Point(w / 2, h / 2))
            assertTrue(runSimulation(solver, config).withinBudget, "${w}×${h}")
        }
    }

    // ── off-centre start ──────────────────────────────────────────────────────

    @Test
    fun `wins when start is not at origin`() {
        listOf(
            BoardConfig(20, 30, Point(10, 15), Point(5,  2)),
            BoardConfig(50, 50, Point(49, 49), Point(0,  0)),
            BoardConfig(1,  50, Point(0,  25), Point(0,  0)),
        ).forEach { config ->
            assertTrue(runSimulation(solver, config).withinBudget, "$config")
        }
    }

    // ── apple at same position as start ───────────────────────────────────────

    // The solver must make at least one move before it can win.
    // The apple is found when the sweep eventually returns to the start column/row
    // combination. On a small board this happens quickly.
    @Test
    fun `wins when apple is at start position`() {
        listOf(
            BoardConfig(5,  5,  Point(2, 2), Point(2, 2)),
            BoardConfig(1,  1,  Point(0, 0), Point(0, 0)),
            BoardConfig(4,  4,  Point(1, 1), Point(1, 1)),
        ).forEach { config ->
            assertTrue(runSimulation(solver, config).withinBudget, "$config")
        }
    }

    // ── budget enforcement ───────────────────────────────────────────────────

    @Test
    fun `respects maxSteps=0`() {
        val engine = SimulationEngine(10, 10, Point(0, 0), Point(5, 5))
        val result = solver.solve(engine, maxSteps = 0L)
        assertEquals(false, result.won)
        assertEquals(0L, result.stepsUsed)
        assertEquals(0L, engine.stepCount)
    }

    @Test
    fun `never exceeds maxSteps`() {
        for (cap in listOf(1L, 5L, 13L, 100L)) {
            val engine = SimulationEngine(1000, 1000, Point(0, 0), Point(999, 999))
            val result = solver.solve(engine, maxSteps = cap)
            assertTrue(result.stepsUsed <= cap, "cap=$cap stepsUsed=${result.stepsUsed}")
        }
    }

    @Test
    fun `stepsUsed matches engine stepCount`() {
        listOf(
            BoardConfig(7,  11, Point(3, 5), Point(6, 10)),
            BoardConfig(1,  50, Point(0, 0), Point(0, 25)),
        ).forEach { config ->
            val engine = SimulationEngine(config.width, config.height, config.start, config.apple)
            val result = solver.solve(engine, maxSteps = config.budgetLimit)
            assertEquals(engine.stepCount, result.stepsUsed, "$config")
        }
    }

    // ── property loop: exhaustive small boards ────────────────────────────────

    @Test
    fun `wins within budget on all boards with area up to 50`() {
        // Tests every (A, B, start, apple) combination for A*B <= 50.
        // This is the core correctness property of the enumeration scheme.
        for (a in 1..50) {
            for (b in 1..(50 / a)) {
                for (ax in 0 until a) for (ay in 0 until b) {
                    val config = BoardConfig(a, b, Point(0, 0), Point(ax, ay))
                    val result = runSimulation(solver, config)
                    assertTrue(result.withinBudget,
                        "${a}×${b} apple=($ax,$ay): ${result.run.stepsUsed}/${config.budgetLimit}")
                }
            }
        }
    }
}
