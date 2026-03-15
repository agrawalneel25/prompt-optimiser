package com.jb3.promptoptimiser.snake

/**
 * A deterministic, fully-observable simulation of the game for local testing.
 *
 * The board is an [width] × [height] torus. The snake starts at [start] and
 * the apple is at [apple]. All parameters are explicit so tests are
 * reproducible without any randomness.
 *
 * @property width   board width  (A in the problem statement), must be ≥ 1
 * @property height  board height (B in the problem statement), must be ≥ 1
 * @property start   snake's initial position
 * @property apple   apple's fixed position
 */
class SimulationEngine(
    val width: Int,
    val height: Int,
    val start: Point,
    val apple: Point,
) : GameEngine {

    init {
        require(width >= 1 && height >= 1) { "Board dimensions must be positive" }
        require(start.x in 0 until width && start.y in 0 until height) { "start out of bounds" }
        require(apple.x in 0 until width && apple.y in 0 until height) { "apple out of bounds" }
    }

    /** The area of the board; the problem budget is 35 × [area]. */
    val area: Long get() = width.toLong() * height.toLong()

    /** Number of [sendSignal] calls made so far. */
    var stepCount: Long = 0L
        private set

    private var pos: Point = start

    override fun sendSignal(move: Move): Boolean {
        pos = pos.step(move, width, height)
        stepCount++
        return pos == apple
    }

    /** Resets the snake to [start] and clears [stepCount], keeping board/apple. */
    fun reset() {
        pos = start
        stepCount = 0L
    }
}
