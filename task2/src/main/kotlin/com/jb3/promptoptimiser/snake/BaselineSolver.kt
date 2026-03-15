package com.jb3.promptoptimiser.snake

/**
 * ## Strategy: outward square spiral (baseline)
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * ### Description
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * The snake traces an outward spiral from its starting cell, following the
 * pattern:
 *
 *   RIGHT×1, UP×1, LEFT×2, DOWN×2, RIGHT×3, UP×3, LEFT×4, DOWN×4, …
 *
 * Directions cycle through [RIGHT, UP, LEFT, DOWN]. The step length increases
 * by 1 after every two consecutive directions.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * ### Why this covers the board
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * In the infinite plane the spiral visits every integer offset (dx, dy) from
 * the start exactly once, sweeping outward in concentric squares. After k
 * complete layers, every offset with |dx| ≤ k and |dy| ≤ k has been visited.
 *
 * Projected onto an A×B torus, offset (dx, dy) maps to the cell at position
 * (start_x + dx) mod A, (start_y + dy) mod B). Every cell (c, r) appears as
 * some offset once |dx| reaches A and |dy| reaches B, i.e. once the spiral's
 * bounding square exceeds max(A, B):
 *
 *   k_needed ≈ max(A, B) / 2
 *   total steps to reach layer k ≈ 2·k² ≈ max(A,B)² / 2
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * ### Step-count analysis
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * The step count to layer k is:
 *   Σ_{j=1}^{k} 4j  =  2k(k+1)
 *
 * For the spiral to guarantee coverage we need k ≥ max(A, B) / 2, giving:
 *   total ≈ 2 · (max(A,B)/2)² = max(A,B)² / 2
 *
 * **Square boards** (A ≈ B ≈ √S):
 *   max(A,B)² / 2 ≈ S / 2   ✓  well inside 35S
 *
 * **Thin boards** (e.g. 1×S):
 *   max(A,B) = S, total ≈ S²/2   ✗  far exceeds 35S
 *
 * On a 1×S board, UP and DOWN moves are no-ops (height 1, so the snake
 * wraps back to the same row). Half of all spiral moves are wasted, and
 * horizontal coverage only grows linearly while the step count grows
 * quadratically. The spiral is therefore unsuitable for thin boards.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * ### Summary
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * The spiral is a clean, simple O(max(A,B)²) baseline. It is efficient for
 * roughly square boards (≤ S/2 steps) but can use O(S²) steps on thin boards,
 * far exceeding the 35S budget. See [BoustrophedonSolver] for a strategy
 * that handles all board shapes within budget.
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

    /**
     * Infinite sequence of moves for the outward spiral:
     * RIGHT×1, UP×1, LEFT×2, DOWN×2, RIGHT×3, UP×3, …
     *
     * Directions cycle [RIGHT, UP, LEFT, DOWN]; the step count increments
     * by 1 after every two consecutive directions.
     */
    private fun spiralSequence(): Sequence<Move> = sequence {
        val dirs = listOf(Move.RIGHT, Move.UP, Move.LEFT, Move.DOWN)
        var dirIndex = 0
        var step = 1
        while (true) {
            // Each step length is used for exactly two consecutive directions
            repeat(step) { yield(dirs[dirIndex % 4]) }
            dirIndex++
            repeat(step) { yield(dirs[dirIndex % 4]) }
            dirIndex++
            step++
        }
    }
}
