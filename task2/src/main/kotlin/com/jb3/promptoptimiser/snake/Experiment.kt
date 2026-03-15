package com.jb3.promptoptimiser.snake

/**
 * Compares all three solvers across a structured set of
 * board shapes. All output is plain text; no external libraries are used.
 *
 * Run via `./gradlew run` alongside the demo in [main].
 */
object Experiment {

    fun run() {
        section("Experiment: strategy comparison across board shapes")

        compareOnGroup("Small square boards (1×1 … 30×30)") {
            (1..30).flatMap { size ->
                (0 until size).flatMap { ax ->
                    (0 until size).map { ay ->
                        BoardConfig(size, size, Point(0, 0), Point(ax, ay))
                    }
                }
            }
        }

        compareOnGroup("Thin 1×N boards (N = 1 … 500, apple at mid-column)") {
            (1..500).map { n -> BoardConfig(1, n, Point(0, 0), Point(0, n / 2)) }
        }

        compareOnGroup("Thin N×1 boards (N = 1 … 500, apple at mid-row)") {
            (1..500).map { n -> BoardConfig(n, 1, Point(0, 0), Point(n / 2, 0)) }
        }

        compareOnGroup("Coprime-dimension boards") {
            listOf(7 to 11, 13 to 17, 3 to 100, 100 to 3, 97 to 101, 11 to 999).map { (w, h) ->
                BoardConfig(w, h, Point(0, 0), Point(w / 2, h / 2))
            }
        }

        compareOnGroup("Large-gcd boards (gcd(A,B) ≥ 4)") {
            listOf(6 to 4, 12 to 8, 50 to 100, 100 to 100, 48 to 36, 500 to 1000).map { (w, h) ->
                BoardConfig(w, h, Point(0, 0), Point(w / 2, h / 2))
            }
        }

        compareOnGroup("Off-centre starts") {
            listOf(
                BoardConfig(20, 30, Point(10, 15), Point(5, 2)),
                BoardConfig(50, 50, Point(49, 49), Point(0, 0)),
                BoardConfig(100, 7, Point(50, 3), Point(99, 6)),
            )
        }
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private fun compareOnGroup(label: String, configs: () -> List<BoardConfig>) {
        println("  $label")
        val boards = configs()
        val solvers: List<Solver> = listOf(BaselineSolver(), SweepStrideSolver(), BoustrophedonSolver())
        for (solver in solvers) {
            val summary = runBatch(solver, boards)
            println("    %-16s  wins=%d/%d  within35S=%d/%d  avg=%.2f%%  max=%.2f%%".format(
                solver.name,
                summary.wins, summary.total,
                summary.withinBudget, summary.total,
                summary.avgBudgetUsed * 100.0,
                summary.maxBudgetUsed * 100.0,
            ))
        }
        println()
    }

    private fun section(title: String) {
        println()
        println("═".repeat(70))
        println("  $title")
        println("═".repeat(70))
        println()
    }
}
