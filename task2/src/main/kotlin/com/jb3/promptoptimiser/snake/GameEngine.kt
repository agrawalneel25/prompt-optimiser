package com.jb3.promptoptimiser.snake

/**
 * The sole channel between the solver and the game.
 *
 * The solver is completely blind: it does not know the board dimensions,
 * its own position, or the apple's position. The only feedback is whether
 * the latest move landed on the apple.
 *
 * The game runs on a **torus**: moving past any edge wraps to the opposite
 * edge. Concretely, a board of width A and height B means:
 *   - moving RIGHT from column A-1 → column 0
 *   - moving LEFT  from column 0   → column A-1
 *   - moving DOWN  from row    B-1 → row    0
 *   - moving UP    from row    0   → row    B-1
 */
fun interface GameEngine {
    /**
     * Sends one move to the game.
     *
     * @param move the direction to move the snake
     * @return `true` if the snake is now on the apple (game won), `false` otherwise
     */
    fun sendSignal(move: Move): Boolean
}
