package com.jb3.promptoptimiser.snake

/**
 * Command-line entry point. Runs sample simulations then the full experiment.
 *
 * Usage: `./gradlew run`  (or run MainKt directly from IntelliJ)
 */
fun main() {
    val solvers = listOf(BaselineSolver(), SweepStrideSolver(), BoustrophedonSolver())

    // Representative boards covering common edge cases
    val sampleConfigs = listOf(
        BoardConfig(10,  10,  Point(0, 0),   Point(9, 9)),    // square, far corner
        BoardConfig(100, 100, Point(0, 0),   Point(99, 99)),  // larger square
        BoardConfig(1,   1000,Point(0, 0),   Point(0, 999)),  // tall thin board
        BoardConfig(1000,1,   Point(0, 0),   Point(999, 0)),  // wide thin board
        BoardConfig(20,  30,  Point(10, 15), Point(5, 2)),    // off-centre start
        BoardConfig(50,  50,  Point(25, 25), Point(24, 25)),  // apple one step behind start
        // Apple at the same position as start: the solver must trace a full
        // cycle on the torus before returning to that position.
        BoardConfig(5,   5,   Point(2, 2),   Point(2, 2)),
    )

    println("═".repeat(60))
    println("  Sample simulations")
    println("═".repeat(60))

    for (solver in solvers) {
        println()
        println("  ${solver.name}")
        println("  ${"─".repeat(56)}")
        for (config in sampleConfigs) {
            val result = runSimulation(solver, config)
            val budgetPct = "%.1f".format(result.budgetUsed * 100)
            val status = when {
                result.withinBudget -> "✓"
                result.run.won      -> "OVER_BUDGET"
                else                -> "✗"
            }
            println("  [$status] $config  steps=${result.run.stepsUsed}  budget=${budgetPct}%")
        }
    }

    Experiment.run()
}
