package com.jb3.promptoptimiser.snake

import kotlin.test.Test
import kotlin.test.assertEquals

class PointTorusTest {

    // ── basic move semantics ─────────────────────────────────────────────────

    @Test
    fun `RIGHT increments x`() = assertEquals(Point(3, 2), Point(2, 2).step(Move.RIGHT, 10, 10))

    @Test
    fun `LEFT decrements x`() = assertEquals(Point(1, 2), Point(2, 2).step(Move.LEFT, 10, 10))

    @Test
    fun `DOWN increments y`() = assertEquals(Point(2, 3), Point(2, 2).step(Move.DOWN, 10, 10))

    @Test
    fun `UP decrements y`() = assertEquals(Point(2, 1), Point(2, 2).step(Move.UP, 10, 10))

    @Test
    fun `horizontal moves do not change y`() {
        val p = Point(3, 5)
        assertEquals(p.y, p.step(Move.RIGHT, 10, 10).y)
        assertEquals(p.y, p.step(Move.LEFT,  10, 10).y)
    }

    @Test
    fun `vertical moves do not change x`() {
        val p = Point(3, 5)
        assertEquals(p.x, p.step(Move.DOWN, 10, 10).x)
        assertEquals(p.x, p.step(Move.UP,   10, 10).x)
    }

    // ── torus wrapping ───────────────────────────────────────────────────────

    @Test
    fun `RIGHT wraps from last column to column 0`() =
        assertEquals(Point(0, 2), Point(9, 2).step(Move.RIGHT, 10, 10))

    @Test
    fun `LEFT wraps from column 0 to last column`() =
        assertEquals(Point(9, 2), Point(0, 2).step(Move.LEFT, 10, 10))

    @Test
    fun `DOWN wraps from last row to row 0`() =
        assertEquals(Point(2, 0), Point(2, 9).step(Move.DOWN, 10, 10))

    @Test
    fun `UP wraps from row 0 to last row`() =
        assertEquals(Point(2, 9), Point(2, 0).step(Move.UP, 10, 10))

    @Test
    fun `wrapping works on non-square boards`() {
        assertEquals(Point(0, 0), Point(4, 0).step(Move.RIGHT, 5, 3))  // width=5
        assertEquals(Point(4, 0), Point(0, 0).step(Move.LEFT,  5, 3))
        assertEquals(Point(0, 0), Point(0, 2).step(Move.DOWN,  5, 3))  // height=3
        assertEquals(Point(0, 2), Point(0, 0).step(Move.UP,    5, 3))
    }

    // ── 1×1 board ────────────────────────────────────────────────────────────

    @Test
    fun `on a 1x1 board every move stays at origin`() {
        val origin = Point(0, 0)
        for (move in Move.entries) {
            assertEquals(origin, origin.step(move, 1, 1),
                "Expected to stay at origin after $move on 1×1 board")
        }
    }

    // ── inverse moves cancel ─────────────────────────────────────────────────

    @Test
    fun `move followed by its opposite returns to start`() {
        val boards = listOf(10 to 10, 1 to 7, 7 to 1, 3 to 5)
        for ((w, h) in boards) {
            for (start in listOf(Point(0, 0), Point(w - 1, h - 1), Point(w / 2, h / 2))) {
                for (move in Move.entries) {
                    val after = start.step(move, w, h).step(move.opposite(), w, h)
                    assertEquals(start, after,
                        "$move then ${move.opposite()} should cancel on ${w}×${h} board from $start")
                }
            }
        }
    }

    // ── full lap returns to start ────────────────────────────────────────────

    @Test
    fun `N right moves on width-N board returns to start`() {
        val start = Point(2, 3)
        var p = start
        repeat(7) { p = p.step(Move.RIGHT, 7, 10) }
        assertEquals(start, p)
    }

    @Test
    fun `M down moves on height-M board returns to start`() {
        val start = Point(2, 0)
        var p = start
        repeat(5) { p = p.step(Move.DOWN, 9, 5) }
        assertEquals(start, p)
    }

    // ── dimension-1 boards ───────────────────────────────────────────────────

    @Test
    fun `on a 1-wide board LEFT and RIGHT both stay at x=0`() {
        val p = Point(0, 3)
        assertEquals(0, p.step(Move.RIGHT, 1, 10).x)
        assertEquals(0, p.step(Move.LEFT,  1, 10).x)
    }

    @Test
    fun `on a 1-tall board UP and DOWN both stay at y=0`() {
        val p = Point(3, 0)
        assertEquals(0, p.step(Move.DOWN, 10, 1).y)
        assertEquals(0, p.step(Move.UP,   10, 1).y)
    }
}
