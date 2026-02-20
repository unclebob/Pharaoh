# Pharaoh

A turn-based economic simulation game in which the player, a disinherited
noble, must build a kingdom from nothing and erect a great pyramid -- all
within 40 years.

Originally written in C for the classic Macintosh by Robert C. Martin
(circa 1988), this is a ground-up rewrite in Clojure, guided by the
original source code and a detailed specification extracted from it.

## The Game

You start with borrowed capital and an empty plot of land. Each month
you make decisions:

- **Trade** commodities (wheat, slaves, oxen, horses, manure, land) on
  the open market, where prices fluctuate with supply, demand, and
  random inflation.
- **Feed** your slaves, oxen, and horses. Underfeed them and they
  sicken and die. Overfeed them and you waste wheat.
- **Plant** crops and **spread** manure to improve yields. The harvest
  depends on season, fertilizer, and how much labor your slaves can
  provide.
- **Hire overseers** to drive your slaves harder -- but push too hard
  and they revolt.
- **Borrow** from the bank when gold runs short, but watch your
  credit rating. The banker gets increasingly unpleasant about unpaid
  debts.
- **Negotiate contracts** with neighboring kings to buy or sell
  commodities outside the normal market.
- **Build your pyramid**, stone by stone, month by month.

Random hazards -- locusts, plagues, wars, slave revolts, acts of God
and acts of mobs -- keep things interesting.

Four AI neighbors visit with advice (some reliable, some not):
the Good Guy, the Bad Guy, the Village Idiot, and the Banker.
All dialog is spoken aloud via text-to-speech.

### Difficulty

| Level  | Pyramid Height | Credit Limit | World Growth |
|--------|---------------|--------------|--------------|
| Easy   | 100 ft        | 5,000,000    | 15%/yr       |
| Normal | 300 ft        | 500,000      | 10%/yr       |
| Hard   | 1,000 ft      | 50,000       | 5%/yr        |

## Running

Requires Java 11+ and [Clojure CLI tools](https://clojure.org/guides/install_clojure).

```bash
clojure -M:run
```

Or use the shell script:

```bash
./run.sh
```

## Controls

### Keyboard

| Key | Action             | Key | Action             |
|-----|--------------------|-----|--------------------|
| `w` | Buy/sell wheat     | `S` | Set slave feed rate |
| `s` | Buy/sell slaves    | `O` | Set oxen feed rate  |
| `o` | Buy/sell oxen      | `H` | Set horse feed rate |
| `h` | Buy/sell horses    | `p` | Set acres to plant  |
| `m` | Buy/sell manure    | `f` | Set manure to spread|
| `l` | Buy/sell land      | `q` | Set pyramid quota   |
| `L` | Borrow/repay loan  | `g` | Hire/fire overseers |
| `c` | Contract offers    | `r` | Run one month       |
| Esc | Close dialog       |     |                     |

### Dialog Modes

**Buy/Sell:** `b` = Buy, `s` = Sell, `k` = Keep/Acquire (set holdings
to a target amount)

**Overseers:** `h` = Hire, `f` = Fire, `o` = Obtain (set count directly)

**Loans:** `b` = Borrow, `r` = Repay

### Mouse

Click any section on the main screen to open its dialog. Click the
Run button to advance a month, or Quit to exit.

### File Operations

| Shortcut | Action |
|----------|--------|
| Ctrl-S   | Save   |
| Ctrl-O   | Open   |
| Ctrl-N   | New    |

Games are saved to the `saves/` directory by default.

## Testing

```bash
clojure -M:test
```

The test suite includes:

- **Unit tests** covering trading, loans, overseers, health, planting,
  contracts, persistence, input handling, dialog execution, and more.
- **Gherkin acceptance tests** -- 15 feature files with 370 scenarios,
  executed by a custom Gherkin parser and runner.

```bash
# Coverage report
clojure -M:coverage -o target/coverage
```

## Project Structure

```
src/pharaoh/           Clojure source
  core.clj             Main game loop and rendering (Quil)
  state.clj            Initial state and defaults
  simulation.clj       Monthly simulation tick
  trading.clj          Buy/sell market operations
  overseers.clj        Hire/fire/obtain, stress, lashing
  contracts.clj        Contract negotiation and settlement
  loans.clj            Borrowing, repayment, credit checks
  health.clj           Livestock and slave health model
  feeding.clj          Feed rate calculations
  planting.clj         Crop cycle and harvest
  economy.clj          Market price and supply/demand simulation
  events.clj           Random hazards
  messages.clj         All message pools (~500 strings)
  random.clj           Coveyou PRNG with uniform/gaussian/exponential
  tables.clj           Piecewise-linear interpolation tables
  ui/                  Input handling, layout, dialogs, menus
  gherkin/             Custom Gherkin parser and step definitions

features/              Gherkin feature files
resources/             Icons, face portraits, logo
test/                  Unit and acceptance tests
```

## History

Pharaoh was originally written in C for the Macintosh around 1988. The
game's resource fork contained bitmap portraits, icons, and hundreds of
humorous message strings -- all of which have been extracted and
preserved in this port.

The Clojure rewrite began in February 2026 as an experiment in using
Claude Code to translate a legacy C codebase into modern Clojure. The
original C source (in `Pharaoh src/c/`) served as the reference. A
detailed specification (`initial-spec.md`) was reverse-engineered from
the C code, and the Clojure implementation was built from that spec
using test-driven development.

The port uses [Quil](http://quil.info) (a Clojure wrapper around
Processing) for rendering, matching the original's grid-based layout
with clickable sections, dialog overlays, and animated face portraits.

Key milestones in the rewrite:

- Core economic simulation (trading, planting, feeding, health)
- Overseer stress and lashing mechanics
- Loan system with credit checks and foreclosure
- Contract negotiation with four AI neighbors
- Random events (locusts, plagues, wars, revolts, acts of God)
- Neighbor visit system with text-to-speech
- Difficulty selection screen
- Save/load with dirty-flag prompting
- 632 unit tests and 370 Gherkin acceptance scenarios
