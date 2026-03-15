package com.jb3.promptoptimiser.snake

/**
 * ## Baseline strategy — outward square spiral
 *
 * The snake traces the classic "Ulam spiral" outward from its starting cell:
 *
 * ```
 * RIGHT×1, UP×1, LEFT×2, DOWN×2, RIGHT×3, UP×3, LEFT×4, DOWN×4, …
 * ```
 *
 * Each "layer" k adds a ring around the previously-covered square, growing the
 * covered region from (2k-1)² to (2k+1)² cells in the infinite plane.
 *
 * ### Why this works on a torus (and what the bound is)
 *
 * After k complete layers the spiral has visited every integer offset (dx, dy)
 * with |dx| ≤ k and |dy| ≤ k. Projected onto an A×B torus these offsets cover
 * every cell once the covered square's side (2k+1) exceeds both A and B:
 *
 *   k_needed = max(A, B) / 2       (roughly)
 *   total steps ≈ 2·k² ≈ max(A,B)² / 2
 *
 * **Square boards** (A ≈ B ≈ √S): steps ≈ S / 2         ✓ well inside 35S
 * **Thin boards**  (e.g. 1 × S):   steps ≈ S² / 2       ✗ far exceeds 35S
 *
 * The spiral is therefore a strong baseline for roughly-square boards but
 * degrades badly on very thin or very wide boards. See [SweepStrideSolver]
 * for a heuristic that handles thin boards much better.
 *
 * ### Complexity
 * - Time:  O(max(A,B)²)  moves in the worst case
 * - Space: O(1)          — the sequence is generated on-the-fly
 */
class BaselineSolver : Solver {

    override val name: String = "BaselineSpiral"

    override fun solve(engine: GameEngine, maxSteps: Long): RunResult {
        var steps = 0L
        for (move in spiralSequence()) {
            if (steps >= maxSteps) break
            steps++
            if (engine.sendSignal(move)) return RunResult(true, steps, name)
        }
        return RunResult(false, steps, name, notes = "budget exhausted")
    }

    // ── internal ────────────────────────────────────────────────────────────

    /**
     * Generates an infinite sequence of moves following the outward spiral:
     * RIGHT×1, UP×1, LEFT×2, DOWN×2, RIGHT×3, UP×3, …
     *
     * The pattern cycles through directions [RIGHT, UP, LEFT, DOWN]; the step
     * count increases by 1 after every two consecutive directions.
     */
    private fun spiralSequence(): Sequence<Move> = sequence {
        val dirs = listOf(Move.RIGHT, Move.UP, Move.LEFT, Move.DOWN)
        var dirIndex = 0
        var step = 1
        while (true) {
            // Same step length is shared by two consecutive directions
            repeat(step) { yield(dirs[dirIndex % 4]) }
            dirIndex++
            repeat(step) { yield(dirs[dirIndex % 4]) }
            dirIndex++
            step++
        }
    }
}
