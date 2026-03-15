package com.jb3.promptoptimiser.snake

/**
 * A strategy that drives a [GameEngine] until the apple is found or the
 * step budget is exhausted.
 *
 * Implementations must be stateless between calls to [solve] — all mutable
 * state lives inside the engine, not the solver.
 */
interface Solver {
    /** Human-readable name used in result reporting. */
    val name: String

    /**
     * Executes the strategy, sending moves to [engine] until it returns `true`
     * or [maxSteps] moves have been sent.
     *
     * @param engine    the game channel
     * @param maxSteps  hard upper bound on moves (caller enforces budget)
     * @return [RunResult] describing outcome and metrics
     */
    fun solve(engine: GameEngine, maxSteps: Long = Long.MAX_VALUE): RunResult
}

/**
 * The outcome of a single solver run.
 *
 * @property won          whether the apple was found
 * @property stepsUsed    total moves sent to the engine
 * @property strategyName the [Solver.name] that produced this result
 * @property notes        optional free-text annotation (e.g. why it stopped)
 */
data class RunResult(
    val won: Boolean,
    val stepsUsed: Long,
    val strategyName: String,
    val notes: String = "",
)
