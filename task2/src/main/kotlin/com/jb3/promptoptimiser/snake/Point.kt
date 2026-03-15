package com.jb3.promptoptimiser.snake

/** An (x, y) coordinate on the board, zero-indexed from the top-left. */
data class Point(val x: Int, val y: Int) {

    /**
     * Returns the point reached after applying [move] on a torus of size
     * [width] × [height].
     */
    fun step(move: Move, width: Int, height: Int): Point = when (move) {
        Move.RIGHT -> copy(x = (x + 1).mod(width))
        Move.LEFT  -> copy(x = (x - 1).mod(width))
        Move.DOWN  -> copy(y = (y + 1).mod(height))
        Move.UP    -> copy(y = (y - 1).mod(height))
    }
}
