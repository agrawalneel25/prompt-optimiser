package com.jb3.promptoptimiser.snake

/** A cardinal direction the snake can move on the torus. */
enum class Move {
    UP, DOWN, LEFT, RIGHT;

    /** Returns the opposite direction. */
    fun opposite(): Move = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
    }
}
