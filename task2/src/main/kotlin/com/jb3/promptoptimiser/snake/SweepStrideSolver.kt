package com.jb3.promptoptimiser.snake

/**
 * ## Strategy: diagonal torus sweeps with varying strides (heuristic)
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * ### Description
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * Repeatedly executes the macro-step `(RIGHT^m, DOWN)` for stride m = 1, 2, …:
 * move right m times, then step down once, and repeat.
 *
 * Each macro-step moves the snake by offset (+m, +1) on the torus. This traces
 * a diagonal path through the Z_A × Z_B grid.
 *
 * Two orientations are interleaved to handle both wide and tall boards:
 *   - Phase H: (RIGHT^m, DOWN)  — horizontal stride, vertical step
 *   - Phase V: (DOWN^m, RIGHT)  — vertical stride, horizontal step
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * ### When a diagonal sweep covers the whole board
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * The macro-step (+m, +1) generates an orbit in Z_A × Z_B. The orbit size is:
 *
 *   lcm(A / gcd(m, A),  B)
 *
 * For this to equal A·B (full coverage), we need:
 *
 *   lcm(A / gcd(m, A),  B)  =  A·B
 *
 * For stride m = 1: lcm(A, B) = A·B  iff  gcd(A, B) = 1.
 *
 * So stride-1 covers all cells **only when A and B are coprime**. For boards
 * where gcd(A, B) > 1 (e.g. 3×3, 4×4, 6×4), no single value of m guarantees
 * full coverage via the diagonal approach.
 *
 * Example — 3×3 torus, stride 1:
 *   (0,0) → (1,0) → (1,1) → (2,1) → (2,2) → (0,2) → (0,0) ← cycle (6 cells)
 *   Cells (0,1), (1,2), (2,0) are never reached.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * ### Why this is a heuristic, not a guarantee
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * When a phase ends (budget exhausted without finding the apple), the snake is
 * at a new position. The next phase begins from there, potentially covering
 * cells that the previous phase missed. In practice, the combination of
 * multiple phases across different strides and orientations tends to find the
 * apple quickly — but there is no clean proof that every board is covered
 * within 35S steps.
 *
 * For boards where gcd(A, B) = 1 (e.g. any 1×N, N×1, or dimension pair with
 * coprime sizes), stride-1 alone covers all cells in lcm(A,B) = A·B macro-steps
 * = 2·A·B = 2S individual moves. This is a proved guarantee for coprime boards.
 *
 * For all other boards (gcd(A, B) > 1), this solver is a heuristic only.
 * See [BoustrophedonSolver] for a row-by-row sweep strategy with a proof of
 * correctness that applies to all board shapes.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * ### Complexity
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * - Coprime boards: O(S) moves, ≤ 2S ≤ 35S  ✓  (proved)
 * - Non-coprime boards: no proved bound; treated as heuristic
 * - Space: O(1)
 */
class SweepStrideSolver(
    private val maxStrides: Int = DEFAULT_MAX_STRIDES,
) : Solver {

    override val name: String = "SweepStride"

    companion object {
        const val DEFAULT_MAX_STRIDES: Int = 4

        /** Problem guarantee: S < 10^6. Used to bound each phase's budget. */
        const val MAX_AREA: Long = 1_000_000L
    }

    override fun solve(engine: GameEngine, maxSteps: Long): RunResult {
        var steps = 0L

        for (stride in 1..maxStrides) {
            val phaseLimit = (maxSteps - steps).coerceAtMost(2L * MAX_AREA)
            if (phaseLimit <= 0) break

            // Phase H: horizontal stride, stepping down one row at a time
            val (wonA, stepsA) = runSweep(
                engine    = engine,
                primary   = Move.RIGHT,
                secondary = Move.DOWN,
                stride    = stride,
                budget    = phaseLimit / 2,
            )
            steps += stepsA
            if (wonA) return RunResult(true, steps, name, notes = "stride=$stride phase=H")

            // Phase V: vertical stride, stepping right one column at a time
            val (wonB, stepsB) = runSweep(
                engine    = engine,
                primary   = Move.DOWN,
                secondary = Move.RIGHT,
                stride    = stride,
                budget    = (phaseLimit / 2).coerceAtMost(maxSteps - steps),
            )
            steps += stepsB
            if (wonB) return RunResult(true, steps, name, notes = "stride=$stride phase=V")
        }

        return RunResult(false, steps, name, notes = "budget exhausted after $maxStrides strides")
    }

    /**
     * Runs one diagonal sweep phase: repeat ([primary]^[stride], [secondary])
     * until the apple is found or [budget] moves are used.
     */
    private fun runSweep(
        engine: GameEngine,
        primary: Move,
        secondary: Move,
        stride: Int,
        budget: Long,
    ): Pair<Boolean, Long> {
        var steps = 0L
        while (steps + stride + 1 <= budget) {
            repeat(stride) {
                steps++
                if (engine.sendSignal(primary)) return true to steps
            }
            steps++
            if (engine.sendSignal(secondary)) return true to steps
        }
        return false to steps
    }
}
