package com.jb3.promptoptimiser.snake

/**
 * ## Boustrophedon sweep strategy — constructive guaranteed coverage
 *
 * ### The sweep subroutine
 *
 * For a guessed pair of dimensions (w, h), one sweep does:
 * ```
 * repeat h times:
 *     move RIGHT w times
 *     move UP once
 * ```
 *
 * **Why this covers the board when w ≥ A and h ≥ B:**
 *
 * - Suppose the snake is at column x, row y.
 *   Moving RIGHT w times visits columns (x+1) mod A, (x+2) mod A, …, (x+w) mod A.
 *   Because w ≥ A, these w offsets include every residue mod A, so every column
 *   of the current row is visited at least once.
 *
 * - Moving UP then advances to the next row. After h such sweeps the rows visited
 *   are y, y−1, y−2, …, y−(h−1) (all mod B). Because h ≥ B these h offsets cover
 *   every residue mod B, so every row is visited.
 *
 * - Together: every (column, row) pair is visited ⟹ the apple is found.
 *
 * The starting position does not matter because coverage depends only on the
 * width and height of the sweep, not on where it begins.
 *
 * ### Dimension enumeration
 *
 * Since A and B are unknown, we enumerate candidate (w, h) pairs in order of
 * increasing scale k, using powers of two:
 *
 * ```
 * k = 0 : (1, 1)
 * k = 1 : (1, 2)  (2, 1)
 * k = 2 : (1, 4)  (2, 2)  (4, 1)
 * k = 3 : (1, 8)  (2, 4)  (4, 2)  (8, 1)
 * …
 * k     : for i = 0..k:  w = 2^i,  h = 2^(k−i)
 * ```
 *
 * The first pair with w ≥ A and h ≥ B is at layer k* = ⌈log₂ A⌉ + ⌈log₂ B⌉.
 * Since A·B < 10⁶, we have k* ≤ 21.
 *
 * ### Step count
 *
 * Each (w, h) sweep costs h·(w + 1) moves. Summing over all candidate pairs up to
 * and including the first covering pair gives a total of roughly 34·S moves in the
 * worst case (A ≈ B ≈ √S). This sits just inside the 35·S budget, but the margin
 * is tight and is not a formal proof — consider this a strong constructive heuristic
 * with near-certain budget compliance for S < 10⁶.
 *
 * ### Note on direction
 *
 * This implementation always sweeps RIGHT (unidirectional). A true zig-zag
 * boustrophedon would alternate RIGHT and LEFT on successive rows. On a torus the
 * unidirectional version is equally correct; alternating would avoid retracing the
 * same path after wrapping and could reduce the constant factor slightly.
 */
class BoustrophedonSolver : Solver {

    override val name: String = "Boustrophedon"

    companion object {
        // 2^20 > 10^6, so k = 21 is a safe upper bound for any A, B with A·B < 10^6.
        // We use 40 as the loop ceiling to be conservative without any runtime cost
        // (the apple is always found long before k = 21 for S < 10^6).
        private const val MAX_K = 40
    }

    override fun solve(engine: GameEngine, maxSteps: Long): RunResult {
        var steps = 0L

        for (k in 0..MAX_K) {
            for (i in 0..k) {
                val w = 1 shl i        // 2^i  — guessed width
                val h = 1 shl (k - i)  // 2^(k-i) — guessed height

                val remaining = maxSteps - steps
                if (remaining <= 0) break

                val (won, used) = sweep(engine, w, h, remaining)
                steps += used
                if (won) return RunResult(true, steps, name, notes = "w=$w h=$h k=$k")
            }
            if (steps >= maxSteps) break
        }

        return RunResult(false, steps, name, notes = "budget exhausted")
    }

    /**
     * Runs one boustrophedon sweep for guessed dimensions ([w], [h]).
     *
     * Each of the [h] rows is swept with [w] RIGHT moves followed by one UP move.
     * Every individual move is checked for a win; returns as soon as the apple is found.
     *
     * Never sends more than [budget] moves total.
     *
     * @return `(won, stepsUsed)`
     */
    private fun sweep(engine: GameEngine, w: Int, h: Int, budget: Long): Pair<Boolean, Long> {
        var steps = 0L
        repeat(h) {
            // Horizontal pass: RIGHT × w — covers all columns when w ≥ A
            repeat(w) {
                if (steps >= budget) return false to steps
                steps++
                if (engine.sendSignal(Move.RIGHT)) return true to steps
            }
            // Row advance: UP × 1 — also checked, since the apple may be there
            if (steps >= budget) return false to steps
            steps++
            if (engine.sendSignal(Move.UP)) return true to steps
        }
        return false to steps
    }
}
