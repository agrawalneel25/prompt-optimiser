package com.jb3.promptoptimiser.snake

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimulationEngineTest {

    // ── step counting ────────────────────────────────────────────────────────

    @Test
    fun `stepCount starts at zero`() {
        val engine = SimulationEngine(3, 3, Point(0, 0), Point(2, 2))
        assertEquals(0L, engine.stepCount)
    }

    @Test
    fun `each sendSignal increments stepCount by exactly one`() {
        val engine = SimulationEngine(5, 5, Point(0, 0), Point(4, 4))
        repeat(6) { i ->
            engine.sendSignal(Move.RIGHT)
            assertEquals((i + 1).toLong(), engine.stepCount)
        }
    }

    // ── apple detection ──────────────────────────────────────────────────────

    @Test
    fun `returns true on the exact move that lands on the apple`() {
        val engine = SimulationEngine(3, 3, Point(0, 1), Point(1, 1))
        assertTrue(engine.sendSignal(Move.RIGHT))
    }

    @Test
    fun `returns false for moves that do not land on the apple`() {
        val engine = SimulationEngine(5, 5, Point(0, 0), Point(4, 4))
        assertFalse(engine.sendSignal(Move.RIGHT))
        assertFalse(engine.sendSignal(Move.DOWN))
        assertFalse(engine.sendSignal(Move.RIGHT))
    }

    @Test
    fun `no false positive - all moves on a path away from apple return false`() {
        // Apple at (4,4); we walk along x=0 column, never getting close
        val engine = SimulationEngine(5, 5, Point(0, 0), Point(4, 4))
        repeat(4) { assertFalse(engine.sendSignal(Move.DOWN)) }
        assertEquals(4L, engine.stepCount)
    }

    @Test
    fun `on 1x1 board any move returns to origin, which is the apple`() {
        val engine = SimulationEngine(1, 1, Point(0, 0), Point(0, 0))
        assertTrue(engine.sendSignal(Move.RIGHT))
    }

    // ── torus wrapping via win signal ────────────────────────────────────────

    @Test
    fun `wrap RIGHT lands on column 0`() {
        val engine = SimulationEngine(4, 4, Point(3, 0), Point(0, 0))
        assertTrue(engine.sendSignal(Move.RIGHT))
    }

    @Test
    fun `wrap LEFT lands on last column`() {
        val engine = SimulationEngine(4, 4, Point(0, 0), Point(3, 0))
        assertTrue(engine.sendSignal(Move.LEFT))
    }

    @Test
    fun `wrap DOWN lands on row 0`() {
        val engine = SimulationEngine(4, 4, Point(0, 3), Point(0, 0))
        assertTrue(engine.sendSignal(Move.DOWN))
    }

    @Test
    fun `wrap UP lands on last row`() {
        val engine = SimulationEngine(4, 4, Point(0, 0), Point(0, 3))
        assertTrue(engine.sendSignal(Move.UP))
    }

    @Test
    fun `wrapping is position-agnostic - works from any column`() {
        // Right wrap from column 6 on a width-7 board
        val engine = SimulationEngine(7, 5, Point(6, 2), Point(0, 2))
        assertTrue(engine.sendSignal(Move.RIGHT))
    }

    // ── reset ────────────────────────────────────────────────────────────────

    @Test
    fun `reset clears stepCount`() {
        val engine = SimulationEngine(5, 5, Point(1, 1), Point(3, 3))
        repeat(3) { engine.sendSignal(Move.RIGHT) }
        engine.reset()
        assertEquals(0L, engine.stepCount)
    }

    @Test
    fun `reset restores snake to start position`() {
        val engine = SimulationEngine(5, 5, Point(1, 1), Point(3, 3))
        engine.sendSignal(Move.RIGHT)   // now at (2,1)
        engine.sendSignal(Move.RIGHT)   // now at (3,1)
        engine.reset()
        // After reset, one RIGHT from (1,1) → (2,1) ≠ (3,3): must be false
        assertFalse(engine.sendSignal(Move.RIGHT))
        assertEquals(1L, engine.stepCount)
    }

    @Test
    fun `reset does not change board dimensions or apple position`() {
        val engine = SimulationEngine(3, 3, Point(0, 1), Point(1, 1))
        engine.sendSignal(Move.DOWN)
        engine.reset()
        // The apple is still at (1,1): one RIGHT from (0,1) should win
        assertTrue(engine.sendSignal(Move.RIGHT))
    }

    // ── area ─────────────────────────────────────────────────────────────────

    @Test
    fun `area equals width times height`() {
        assertEquals(1L,    SimulationEngine(1,   1,   Point(0, 0), Point(0, 0)).area)
        assertEquals(12L,   SimulationEngine(3,   4,   Point(0, 0), Point(1, 1)).area)
        assertEquals(10000L,SimulationEngine(100, 100, Point(0, 0), Point(0, 0)).area)
    }

    // ── determinism ──────────────────────────────────────────────────────────

    @Test
    fun `two engines with identical config produce identical outcomes`() {
        val moves = listOf(Move.RIGHT, Move.RIGHT, Move.DOWN, Move.LEFT, Move.UP)
        val e1 = SimulationEngine(5, 5, Point(1, 1), Point(3, 2))
        val e2 = SimulationEngine(5, 5, Point(1, 1), Point(3, 2))
        val results1 = moves.map { e1.sendSignal(it) }
        val results2 = moves.map { e2.sendSignal(it) }
        assertEquals(results1, results2)
        assertEquals(e1.stepCount, e2.stepCount)
    }
}
