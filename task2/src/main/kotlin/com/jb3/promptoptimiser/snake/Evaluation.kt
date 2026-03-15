package com.jb3.promptoptimiser.snake

/**
 * Parameters describing a single board configuration for simulation.
 *
 * @property width   board width  (A)
 * @property height  board height (B)
 * @property start   snake start position
 * @property apple   apple position
 */
data class BoardConfig(
    val width: Int,
    val height: Int,
    val start: Point,
    val apple: Point,
) {
    val area: Long get() = width.toLong() * height.toLong()
    val budgetLimit: Long get() = 35L * area

    override fun toString(): String = "${width}×${height} start=$start apple=$apple"
}

/**
 * The result of running one solver on one board configuration.
 *
 * @property config      the board that was simulated
 * @property run         the [RunResult] from the solver
 * @property budgetUsed  fraction of the allowed 35S budget consumed (0.0–1.0+)
 */
data class StrategyResult(
    val config: BoardConfig,
    val run: RunResult,
) {
    val budgetUsed: Double get() = run.stepsUsed.toDouble() / config.budgetLimit
    val withinBudget: Boolean get() = run.won && run.stepsUsed <= config.budgetLimit
}

/**
 * Summary statistics across a batch of [StrategyResult] instances.
 *
 * @property strategyName  the solver name
 * @property total         number of boards tested
 * @property wins          number of boards where the apple was found
 * @property withinBudget  number of wins that also respected the 35S limit
 * @property avgBudgetUsed average fraction of 35S budget used (wins only)
 * @property maxBudgetUsed worst-case fraction of 35S budget used (wins only)
 */
data class BatchSummary(
    val strategyName: String,
    val total: Int,
    val wins: Int,
    val withinBudget: Int,
    val avgBudgetUsed: Double,
    val maxBudgetUsed: Double,
) {
    val winRate: Double get() = if (total == 0) 0.0 else wins.toDouble() / total

    override fun toString(): String = buildString {
        appendLine("Strategy : $strategyName")
        appendLine("Boards   : $total")
        appendLine("Win rate : $wins/$total (${(winRate * 100).fmt(1)}%)")
        appendLine("≤35S wins: $withinBudget/$total")
        appendLine("Avg budget used: ${(avgBudgetUsed * 100).fmt(2)}%")
        append(    "Max budget used: ${(maxBudgetUsed * 100).fmt(2)}%")
    }
}

// ── simulation helpers ───────────────────────────────────────────────────────

/**
 * Runs [solver] on a single [config], returning a [StrategyResult].
 * The engine is freshly constructed from [config] so there is no shared state.
 */
fun runSimulation(solver: Solver, config: BoardConfig): StrategyResult {
    val engine = SimulationEngine(config.width, config.height, config.start, config.apple)
    val result = solver.solve(engine, maxSteps = config.budgetLimit)
    return StrategyResult(config, result)
}

/**
 * Runs [solver] on every config in [configs] and returns aggregated [BatchSummary].
 */
fun runBatch(solver: Solver, configs: List<BoardConfig>): BatchSummary {
    val results = configs.map { runSimulation(solver, it) }
    val wins = results.filter { it.run.won }
    val withinBudget = results.count { it.withinBudget }
    val avgBudget = if (wins.isEmpty()) 0.0 else wins.sumOf { it.budgetUsed } / wins.size
    val maxBudget = wins.maxOfOrNull { it.budgetUsed } ?: 0.0
    return BatchSummary(
        strategyName  = solver.name,
        total         = configs.size,
        wins          = wins.size,
        withinBudget  = withinBudget,
        avgBudgetUsed = avgBudget,
        maxBudgetUsed = maxBudget,
    )
}

// ── formatting ───────────────────────────────────────────────────────────────

private fun Double.fmt(decimals: Int) = "%.${decimals}f".format(this)
