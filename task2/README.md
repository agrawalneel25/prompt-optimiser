# Task 2 — Blind Snake on a Torus

## Problem

A snake occupies a single cell on an A×B board that wraps on all four edges
(a torus). An apple occupies one other cell. Your goal is to move the snake
onto the apple.

The hard part: you are completely blind. You do not know A, B, or S = A·B.
You do not know the snake's starting position, its position at any later
point, or the apple's position. The only feedback is a boolean signal that
fires when the snake lands on the apple. You lose if you send more than 35·S
moves without winning — and S itself is unknown to you.

---

## Key challenge

Because neither the board dimensions nor any position is known, the solver
cannot reason about "where it is" at any step. Every decision must be based
solely on the sequence of moves sent so far, not on any state received from
the environment.

The board constraint is S < 10⁶, so A and B can be anything from 1×1 up to
1×1,000,000 or 1,000×1,000.

---

## Why a naive spiral is weak on thin boards

An outward square spiral (`RIGHT×1, UP×1, LEFT×2, DOWN×2, …`) works well on
near-square boards: after k layers it has covered a (2k+1)² area, which
exceeds any A×B board once `k ≈ max(A,B)/2`. For a 1,000×1,000 board that
means roughly 500,000 moves — well under 35S = 35,000,000.

On a 1×S board the same spiral spends half its moves on vertical steps that
do nothing (height is 1), and needs `k ≈ S/2` layers to wrap around the
torus horizontally. Total moves grow as O(S²), which far exceeds 35S for
large S.

---

## Strategies

### BaselineSolver — outward square spiral

```
RIGHT×1, UP×1, LEFT×2, DOWN×2, RIGHT×3, UP×3, LEFT×4, DOWN×4, …
```

Each layer adds a ring around the previously-covered square. The step budget
to cover a square or near-square A×B board is approximately S/2 — well inside
35S. For very elongated boards the budget guarantee does not hold. Tests
explicitly label this as a known limitation and do not claim budget compliance
on thin boards.

**Complexity:** O(max(A,B)²) moves in the worst case.

---

### SweepStrideSolver — repeated torus sweeps

Repeats the macro-step `(RIGHT^m, DOWN)` for stride m = 1, 2, 3, …:
move right m times, then step down once, and repeat.

**Why stride 1 covers everything:**
With m=1, each macro-step is `(RIGHT, DOWN)`. On an A×B torus, this traces a
diagonal path. After A·B macro-steps (= 2·A·B individual moves) the path
visits every cell, because gcd(1, A) = 1 always holds — the stride of 1 is
coprime with any width. This gives a proved upper bound of 2S ≤ 35S for all
board shapes.

**Why multiple strides?**
Higher strides (m = 2, 3, …) are tried as a heuristic: on boards where the
stride happens to be coprime with A, the apple may be found in fewer total
steps. These phases are bounded so they do not break the overall 2S guarantee
if stride 1 completes first.

**Both orientations:**
Two sweep phases are interleaved — horizontal-first `(RIGHT^m, DOWN)` and
vertical-first `(DOWN^m, RIGHT)` — to handle both wide and tall boards
symmetrically.

**Complexity:** O(S) moves; proved worst case ≤ 2S.

---

## Guarantees vs heuristics

| Claim | Status |
|---|---|
| SweepStride stride-1 covers any A×B board in ≤ 2S moves | **Proved** |
| SweepStride multi-stride finds apple faster on some boards | Heuristic — not proved |
| Baseline covers square boards (A ≈ B) within 35S budget | **Proved** for A = B |
| Baseline covers thin boards (1×S) within 35S budget | **False** — known failure case |

---

## How the simulator works

`SimulationEngine` is a deterministic, fully-observable implementation of
`GameEngine`. It holds the board dimensions, snake start position, and apple
position as explicit constructor parameters, making tests reproducible without
any randomness. Calling `sendSignal(move)` moves the snake on the torus and
returns `true` if the new position equals the apple.

`runSimulation(solver, config)` constructs a fresh engine from a `BoardConfig`
and runs the solver against it, returning a `StrategyResult` with the outcome
and step count. `runBatch` aggregates results across a list of configs into a
`BatchSummary`.

---

## Experiment runner

`Experiment.run()` compares both strategies across several board groups:
square boards, thin boards, coprime-dimension boards, large-gcd boards, and
off-centre starts. Output is plain text — no external libraries.

```
  Small square boards (1×1 … 30×30)
    BaselineSpiral    wins=900/900  within35S=900/900  avg=0.71%  max=4.85%
    SweepStride       wins=900/900  within35S=900/900  avg=0.17%  max=2.00%

  Thin 1×N boards (N = 1 … 500, apple at mid-column)
    BaselineSpiral    wins=500/500  within35S=6/500    avg=...    max=...
    SweepStride       wins=500/500  within35S=500/500  avg=0.06%  max=0.20%
```

The experiment output makes the spiral's weakness on thin boards visible at a
glance, and confirms SweepStride's ≤ 2S guarantee across all tested groups.

---

## Solution process

### 1. Spiral — first instinct

The first natural idea was an outward spiral: start at the current cell and
trace a growing square outward. After k layers you have covered a
(2k+1)×(2k+1) region, which on a finite torus eventually wraps over every
cell. Simple, zero state, easy to reason about.

### 2. Realising the move model is different from classic Snake

In the classic Nokia Snake game you can only *turn* — left or right relative
to the direction you are currently facing. You cannot reverse or move
diagonally. This puzzle is different: the move commands are absolute
(`UP`, `DOWN`, `LEFT`, `RIGHT`), so you can change direction freely at any
step without needing to track your heading. That simplification matters for
the spiral, which requires immediate direction reversals (e.g. going RIGHT
then UP then LEFT) that would be illegal in the turn-only model.

### 3. Spiral's failure on thin boards

Once the spiral was implemented it became clear that it fails badly on boards
like 1×1,000,000. On a board of height 1 every `UP` and `DOWN` command is a
no-op, so half the moves are wasted, and the horizontal coverage only grows
one step per two spiral segments. The step count to cover a 1×S board with a
spiral grows as O(S²) — far outside the 35S budget for large S.

### 4. Modular arithmetic and coprime strides

The key observation is that moving `RIGHT^m, DOWN` repeatedly traces a
diagonal on the torus. Whether that diagonal eventually visits every cell
depends on `gcd(m, A)`: if `gcd(m, A) = 1` the path covers all A columns
before repeating, and since the DOWN step advances one row each macro-step,
the path covers all A·B cells.

The problem is that A is unknown, so we cannot guarantee coprimality for an
arbitrary stride m. However, `gcd(1, A) = 1` always, so stride m=1 is always
coprime with whatever A happens to be. This gives an unconditional guarantee:
the `(RIGHT, DOWN)` sweep covers every cell in at most A·B macro-steps = 2S
individual moves.

Higher strides (m=2, 3, …) are also tried as a heuristic. When the stride
happens to be coprime with A they can find the apple faster — but correctness
does not depend on them.

### 5. Boustrophedon (zig-zag) sweep

An alternative framing of the same idea is a boustrophedon traversal: sweep
left-to-right along row 0, step down, sweep right-to-left along row 1, step
down, and so on. This is equivalent to alternating-direction stride-1 sweeps
and would visit every cell in at most 2S moves for the same reason. The
current implementation uses unidirectional sweeps (always RIGHT, always DOWN)
for simplicity; a boustrophedon variant would achieve the same asymptotic
bound and might halve the constant in practice by eliminating backtracking
at row boundaries.

---

## Repository layout

```
task2/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/
└── src/
    ├── main/kotlin/com/jb3/promptoptimiser/snake/
    │   ├── Move.kt               — direction enum with opposite()
    │   ├── GameEngine.kt         — fun interface: the only channel to the game
    │   ├── Solver.kt             — solver interface + RunResult data class
    │   ├── Point.kt              — (x,y) with torus-aware step()
    │   ├── BaselineSolver.kt     — outward spiral strategy
    │   ├── SweepStrideSolver.kt  — stride sweep strategy
    │   ├── SimulationEngine.kt   — deterministic test harness
    │   ├── Evaluation.kt         — runSimulation, runBatch, BatchSummary
    │   ├── Experiment.kt         — strategy comparison across board groups
    │   └── Main.kt               — CLI entry point
    └── test/kotlin/com/jb3/promptoptimiser/snake/
        ├── MoveTest.kt              — enum values and opposite()
        ├── PointTorusTest.kt        — torus arithmetic for Point.step()
        ├── SimulationEngineTest.kt  — engine mechanics and determinism
        ├── BaselineSolverTest.kt    — spiral correctness and known limits
        ├── SweepStrideSolverTest.kt — sweep coverage and budget proofs
        ├── BudgetEnforcementTest.kt — step-limit contracts
        ├── BatchEvaluationTest.kt   — runBatch and BatchSummary invariants
        └── SolverTest.kt            — cross-cutting integration tests
```

---

## Design choices

**Separation of concerns.** Solver logic (`BaselineSolver`, `SweepStrideSolver`)
is pure: it reads from a `GameEngine` interface and writes nothing except the
return value. The `SimulationEngine` is the only class with mutable state.
This makes solvers trivially testable against any `GameEngine` implementation.

**`fun interface GameEngine`** lets test code pass a lambda as the engine,
keeping unit tests concise where the full simulation harness is not needed.

**No shared state between solver runs.** `Solver.solve()` takes an `engine`
parameter; the solver itself holds no fields that accumulate between calls.
Running the same solver twice on the same config always produces the same result.

**Honest complexity claims.** KDoc explicitly distinguishes proved bounds from
heuristic improvements. Tests echo this: thin-board tests for `BaselineSolver`
use a generous budget and assert `won = true` without asserting budget
compliance, while the same tests for `SweepStrideSolver` assert both.

---

## Build and run

Requires JDK 17+. If `gradlew` is not present, run `gradle wrapper` once.

```bash
# Run all tests
./gradlew test

# Run the demo + experiment
./gradlew run

# Test report (HTML)
open build/reports/tests/test/index.html
```

---

## What I would improve next

1. **Prove or disprove the multi-stride heuristic.** It is not currently
   proved that higher strides help on average; a formal analysis by board
   shape would either justify or remove them.

2. **Adaptive stride selection.** If the solver could detect a "no-op" axis
   (e.g., all UP moves return to the same y due to height=1), it could skip
   vertical phases entirely. This requires inferring board shape from the
   win signal timing, which is non-trivial.

3. **Boustrophedon (snake-row) traversal.** An alternating left-right row
   sweep may reduce redundant wrapping compared to the current unidirectional
   sweep, potentially halving the constant in the 2S bound.

4. **Parameterised test runner.** JUnit 5's `@ParameterizedTest` would make
   the property-loop tests more legible in IntelliJ's test report, showing
   individual board failures inline rather than at the first assertion failure.
