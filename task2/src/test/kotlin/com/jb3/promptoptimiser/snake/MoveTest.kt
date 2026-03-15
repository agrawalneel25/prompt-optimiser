package com.jb3.promptoptimiser.snake

import kotlin.test.Test
import kotlin.test.assertEquals

class MoveTest {

    @Test
    fun `all four directions exist`() {
        assertEquals(4, Move.entries.size)
        Move.UP; Move.DOWN; Move.LEFT; Move.RIGHT  // compile-time existence check
    }

    @Test
    fun `opposite directions are correct`() {
        assertEquals(Move.DOWN,  Move.UP.opposite())
        assertEquals(Move.UP,    Move.DOWN.opposite())
        assertEquals(Move.RIGHT, Move.LEFT.opposite())
        assertEquals(Move.LEFT,  Move.RIGHT.opposite())
    }

    @Test
    fun `opposite is involutory - applying it twice returns the original`() {
        for (move in Move.entries) {
            assertEquals(move, move.opposite().opposite(),
                "${move}.opposite().opposite() should equal $move")
        }
    }

    @Test
    fun `horizontal and vertical moves are distinct`() {
        val horizontal = setOf(Move.LEFT, Move.RIGHT)
        val vertical   = setOf(Move.UP,   Move.DOWN)
        assertEquals(emptySet(), horizontal intersect vertical)
    }
}
