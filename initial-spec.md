# Pharaoh: Game Specification

**Pharaoh** is a turn-based economic simulation game for the classic Macintosh,
written by Robert Martin (v1.2). The player assumes the role of a disinherited
noble who must, within 40 years, build a kingdom from nothing and erect a great
pyramid.

---

## Objective

Starting with only borrowed capital, the player must acquire land, slaves,
oxen, and horses; manage an agricultural economy; and construct a pyramid to a
target height before time expires. The target height depends on difficulty
level:

| Level    | Pyramid Height | Min Credit Limit | World Growth |
|----------|---------------|------------------|--------------|
| Easy     | 100 ft        | 5,000,000 gold   | 15%/yr       |
| Moderate | 300 ft        | 500,000 gold     | 10%/yr       |
| Hard     | 1,000 ft      | 50,000 gold      | 5%/yr        |

---

## Screen Layout

The screen is a 10-column by 25-row grid of labeled cells (50 x 12 pixels
each), organized into framed, titled sections. Below is a wireframe:

```
+--Col 0---Col 1---Col 2---Col 3-+--Col 4---Col 5-+--Col 6---Col 7-+--Col 8---Col 9-+
|          Commodities            |     Prices      |   Feed Rates   |      Date       |
|         Current   d%     Old    | Wheat    [prc]  | Slaves  [rate] | Year    [year]  |
| Wheat   [value] [d%]   [old]   | Manure   [prc]  | Oxen    [rate] | Month   [month] |
| Manure  [value] [d%]   [old]   | Slaves   [prc]  | Horses  [rate] +--------+--------+
| Slaves  [value] [d%]   [old]   | Horses   [prc]  +----------------+      Loan       |
| Horses  [value] [d%]   [old]   | Oxen     [prc]  |   Overseers    | Loan   [amount] |
| Oxen    [value] [d%]   [old]   | Land     [prc]  | O'seers [cnt]  | int %  [rate]   |
+---------------------------------+-----------------+ Salary  [pay]  | Cred   [limit]  |
+-----------Land (acres)----------+--Spread&Plant---+--------+-------+-----------------+
| Fallow  Planted Growing Ripe  Total | Manure Land | Cur Gold  d%     Old Gold        |
| [val]   [val]   [val]   [val] [val] | [val] [val] | [value] [d%]    [old]            |
+--------------------------------------+-------------+---------------------------------+
+----Pyramid----+-------------------Pending Contracts-------------------+
| StoneQt Stones| contract 1 text ...                                   |
| [quota] [cnt] | contract 2 text ...                                   |
|         Hght  | contract 3 text ...                                   |
|         [ft]  | contract 4 text ...                                   |
|               | contract 5 text ...                                   |
|     /\        | contract 6 text ...                                   |
|    /  \       | contract 7 text ...                                   |
|   /    \      | contract 8 text ...                                   |
|  /      \     | contract 9 text ...                                   |
| /________\    | contract 10 text ...                                  |
|               |                                                       |
|               |                                                       |
|               |                                                       |
+---------------+-------------------------------------------------------+
| (Quit) |                                       | [status] |  (Run)   |
+--------+---------------------------------------+----------+----------+
```

### Section Descriptions

- **Date** -- Current month and year (game begins January, Year 1).
- **Gold** -- Current gold, previous month's gold, and percentage change.
  Clickable to borrow or repay.
- **Loan** -- Outstanding loan balance, monthly interest rate, and current
  credit limit. Clickable to borrow or repay.
- **Commodities** -- Current holdings, previous month's holdings, and delta %
  for: Wheat, Manure, Slaves, Horses, Oxen. Clickable to buy or sell.
- **Prices** -- Current market price for each commodity plus Land.
- **Land** -- Acres in each state: Fallow, Planted, Growing, Ripe, and Total.
  Clickable to sell land in any category.
- **Feed Rates** -- Bushels of wheat fed per individual per month for Slaves,
  Oxen, and Horses. Clickable to adjust.
- **Spread & Plant** -- Tons of manure to spread and acres to plant per month.
  Clickable to adjust.
- **Overseers** -- Number employed and monthly salary per overseer. Clickable
  to hire or fire.
- **Pending Contracts** -- Active contracts with neighboring kings (read-only).
- **Pyramid** -- A graphical rendering of the pyramid, plus stone count,
  current height, and monthly stone quota. Quota cell is clickable.
- **Run / Quit** -- Rounded-rect buttons to advance one month or exit.

---

## Commands

### Keyboard

| Key | Action               | Key | Action               |
|-----|----------------------|-----|----------------------|
| `l` | Buy/sell land        | `L` | Borrow/repay loan    |
| `w` | Buy/sell wheat       | `O` | Set oxen feed rate   |
| `s` | Buy/sell slaves      | `S` | Set slave feed rate  |
| `o` | Buy/sell oxen        | `H` | Set horse feed rate  |
| `h` | Buy/sell horses      | `p` | Set acres to plant   |
| `m` | Buy/sell manure      | `f` | Set manure to spread |
| `g` | Hire/fire overseers  | `q` | Set pyramid quota    |
| `r` | Run for one month    |     |                      |

### Mouse

Click on any value cell to invoke the appropriate dialog. Click **Run** to
advance one month; click **Quit** to exit.

### Buy/Sell Dialog

Select a function via radio button or keystroke:
- `b` = Buy
- `s` = Sell
- `k` = Keep (buy or sell to reach a target quantity)
- `a` = Acquire (same as Keep)

Then enter the desired amount and confirm.

### Overseer Dialog

- `h` = Hire
- `f` = Fire
- `o` = Obtain (hire or fire to reach a target headcount)

### Loan Dialog

- `b` = Borrow
- `r` = Repay

### Contracts Menu

Select from offered contracts via the Contracts pull-down menu. Confirm to
commit. There is no way to cancel a committed contract.

---

## Formulae

All formulae below use the following random distributions:

- **uniform(a, b)** -- Uniform on [a, b).
- **gaussian(mean, sigma)** -- Normal distribution, mean and standard
  deviation.
- **absGaussian(mean, sigma)** -- Gaussian clamped to non-negative values.
- **exponential(mean)** -- Exponential distribution with the given mean.
- **lookup(x, table)** -- Piecewise-linear interpolation over an 11-point
  table mapping an input range to output values.

### Planting Cycle

Land progresses monthly through four stages:

    Fallow --> Planted --> Growing --> Ripe --> Harvested (back to Fallow)

Each acre supports approximately 20 bushels of seed. June and July plantings
yield the best harvests; January plantings yield the worst. The seasonal
modifier is applied via `lookup(month, seasonalYieldTable)`.

### Manure Production

For every 100 bushels of wheat eaten by livestock and slaves, approximately
1 ton of manure is produced:

    manureMade = wheatEaten / 100 * absGaussian(1.0, 0.1)

### Wheat Yield

Determined by the fertilizer-to-land ratio and a seasonal multiplier:

    wheatYield = lookup(manurePerAcre, yieldTable)
                 * absGaussian(1.0, 0.1)
                 * lookup(month, seasonalYieldTable)

### Workload

Monthly required work (in man-hours per day) is:

    requiredWork = oxen * 1
                 + manureToSpread * 64
                 + landToSow * 30
                 + landPlanted * 20
                 + landGrowing * 15
                 + wheatRipe * 0.1
                 + landRipe * 20
                 + horses * 1
                 + pyramidQuota * avgPyramidHeight * 12
                 + temporaryWorkAddition

The total is then randomized by `absGaussian(1.0, 0.1)`.

### Slave Output

Maximum work a single slave can produce:

    maxWorkPerSlave = motivation * workAbility * oxenMultiplier

where:

    motivation       = positiveMotivation + negativeMotivation
    positiveMotivation = lookup(overseerEffPerSlave, positiveMotiveTable)
    negativeMotivation = lookup(lashRate, negativeMotiveTable)
    workAbility      = lookup(slaveHealth, workAbilityTable)
    oxenMultiplier   = max(lookup(oxenPerSlave, oxMultTable) * oxenEfficiency, 1)

### Slave Efficiency

If slaves cannot meet the workload, all activities are proportionally reduced:

    slaveEfficiency = totalWorkDone / requiredWork     (capped at 1.0)

When efficiency < 1, every activity (planting, harvesting, feeding, pyramid
construction) is scaled by this fraction.

### Overseer Stress

When work goes unfinished, overseers accumulate pressure:

    stress  = min(1, workDeficitPerSlave / 10)     (if deficit > 0)
    relaxation = overseerPressure * 0.3            (if no deficit)
    overseerPressure += stress - relaxation

Stressed overseers lash slaves:

    lashRate = lookup(overseerPressure, stressLashTable)
               * overseerEffPerSlave

Lashing increases short-term slave output but causes sickness and accelerated
death.

### Health

Slave, oxen, and horse health are each tracked on a 0-to-1 scale. Each month:

    healthDelta = nourishment - sicknessRate

where nourishment comes from feeding (via lookup tables), and sickness comes
from overwork and lashing (via lookup tables). Death and birth rates for each
population are interpolated from health. Selling sick livestock yields reduced
prices (sale price is scaled by health).

### Pyramid Geometry

The pyramid is modeled as a 2D equilateral triangle. Each stone is one unit
of area.

    maxHeight = (sqrt(3) / 2) * base
    height    = (base - sqrt(base^2 - 4 * area / sqrt(3))) / (2 / sqrt(3))

The base is fixed at game start by difficulty level:

| Level    | Base (stones) | Max Height |
|----------|---------------|------------|
| Easy     | 115.47        | ~100 ft    |
| Moderate | 346.41        | ~300 ft    |
| Hard     | 1154.70       | ~1000 ft   |

The game is won when `height + 1 > maxHeight`.

### Monthly Costs

    gold -= overseers * overseerSalary
    gold -= (totalLand*100 + slaves*10 + horses*5 + oxen*3)
            * (absGaussian(0.7, 0.3) + 0.3)
    gold -= avgPyramidHeight * stonesAdded
    gold -= loanBalance * (interestRate + interestSurcharge) / 100

### Market Prices

Prices undergo monthly random inflation:

    inflation += gaussian(0.0, 0.001)
    price *= absGaussian(1 + inflation, 0.02)

Prices are also subject to supply and demand. The global economy simulation
runs each month for every commodity:

    demand *= 1 + (worldGrowth / 12)
    monthlyDemand = demand / 12
    supply -= monthlyDemand * 0.8

    if supply < 0:
        price      *= uniform(1.0, 1.2)
        production *= uniform(1.0, 1.1)

    supply -= monthlyDemand * 0.2
    supply = max(0, supply)

    if supply > 0:
        price      *= uniform(0.8, 1.0)
        production *= uniform(0.9, 1.0)

    production *= uniform(0.95, 1.05)
    supply += production / 12

If the player overproduces a commodity, global supply rises and prices fall.
If the player becomes a major consumer, demand outstrips supply and prices
rise.

The market also has finite capacity: attempting to sell more than the market
can absorb (supply exceeds 110% of demand) is refused. Attempting to buy more
than available supply is capped.

### Credit and Loans

Net worth determines borrowing capacity:

    netWorth = slaves*slavePrice + oxen*oxenPrice + horses*horsePrice
             + totalLand*landPrice + manure*manurePrice + wheat*wheatPrice
             + gold

    creditLimit = realNetWorth * creditRating

(where `realNetWorth` discounts livestock by health when recalculated for
credit checks).

The bank forecloses if `loan/netWorth` exceeds a threshold interpolated from
the credit rating. Regular payments improve credit rating; missed payments
degrade it. Running out of gold triggers an emergency loan at punitive rates;
failure to obtain one ends the game.

Credit rating adjustment on repayment uses an interpolation table indexed by
(payment / loanBalance), returning a multiplier between 1.0 and 1.3.

---

## Random Hazards

Each month there is a **1-in-8 chance** of a random event. When one occurs,
a value from 0 to 100 is drawn uniformly and the event is selected as follows:

| Range | Event            | Effect                                           |
|-------|------------------|--------------------------------------------------|
| 0-1   | **Locusts**      | All planted, growing, and ripe land reverts to fallow. All crops destroyed. Heavy extra workload imposed. |
| 2-5   | **Plague**       | Health of all livestock multiplied by uniform(0.2, 0.9). Populations reduced by uniform(0.7, 0.95). |
| 6-7   | **Act of God**   | Fallow land, all crops, livestock, wheat stores, and manure each independently multiplied by uniform(0.3, 0.8). Massive extra workload imposed. |
| 8-19  | **Act of Mobs**  | Crops and livestock reduced by uniform(0.6, 0.8). Manure increases (the mob leaves a mess). Extra workload: uniform(5, 10) per slave + gaussian(5, 1) per acre. |
| 20    | **War**          | Outcome depends on overseer count vs. a randomly scaled enemy army. All commodities multiplied by `gain * absGaussian(1.0, 0.2)` where `gain = playerRoll / enemyRoll`. Losing a war can halve everything; winning can double it. Extra workload proportional to enemy army size. |
| 21-29 | **Slave Revolt** | Severity based on slave suffering (lash rate) and sickness. A "hatred" index is computed as `(suffering + sickness) / 2`, then interpolated into a destruction fraction (0 to 1). All commodities scaled by `(1 - destruction)`. Enormous extra workload: gaussian(18, 3) per slave + gaussian(30, 5) per overseer. |
| 30-44 | **Workload**     | A random extra labor burden is imposed: gaussian(10, 3) man-hours/day per slave + gaussian(8, 2) per acre of land. |
| 45-59 | **Health Event** | Health of all livestock multiplied by gaussian(0.6, 0.1). |
| 60-64 | **Labor Event**  | Overseers demand a raise of gaussian(20%, 5%). Some overseers quit (population multiplied by gaussian(0.9, 0.03)). Overseer stress increases by gaussian(0.5, 0.1). |
| 65-74 | **Wheat Event**  | Wheat stores and all growing crops reduced by approximately 30% (multiplied by gaussian(0.7, 0.07)). |
| 75-84 | **Gold Event**   | Gold reduced by approximately 35% (multiplied by gaussian(0.65, 0.1)). |
| 85-99 | **Economy Event**| All market prices randomly shocked (each multiplied by gaussian(1.0, 0.15), yielding up to ~15% swings in either direction). Inflation rate randomly perturbed. |

---

## Neighbors

Four AI neighbors provide commentary during gameplay. Their personalities are
shuffled at the start of each new game:

- **The Good Guy** -- Gives reliable advice about slave health, crop quality,
  and other conditions.
- **The Bad Guy** -- Deliberately gives misleading advice. Will commend the
  player's standing just before catastrophe.
- **The Village Idiot** -- Advice is randomly correct or incorrect.
- **The Banker** -- Visits with increasing frequency as the bank grows
  concerned about outstanding debt.

Neighbors also deliver idle messages during periods of player inactivity and
occasionally drop in for "chats" containing hints or misdirection.

---

## Contracts

Neighboring kings offer contracts to buy or sell commodities outside the
normal market supply/demand system. Contracts specify:

- **Commodity** (wheat, slaves, oxen, horses, manure, or land)
- **Type** (buy or sell)
- **Quantity** (scaled to approximately 6x the player's current holdings,
  with a minimum floor of 200,000 / unit price)
- **Price** (quantity * unit price * (0.4 + exponential(0.6)))
- **Duration** (uniform 12 to 36 months)

Failure to fulfill a contract incurs a 10% penalty on the remaining contract
value. The counterparty may also default or partially fulfill their
obligations based on per-player probability traits (payment reliability,
shipping reliability, default probability), which are themselves randomized at
game start.

Contracts are offered via the Contracts menu, which refreshes each month.
Old contracts have a 20% chance of being replaced; contracts with fewer than
8 months remaining are always replaced. Aging buy-contracts increase in price
(multiplied by uniform(1.01, 1.1)); aging sell-contracts decrease (multiplied
by uniform(0.90, 0.99)).

---

## Random Number Distributions

The simulation employs four probability distributions:

- **uniform(a, b)** -- Uniformly distributed on [a, b). Implemented via the
  Coveyou quadratic congruential method.
- **gaussian(mean, sigma)** -- Normal distribution using the polar method
  (Box-Muller variant).
- **absGaussian(mean, sigma)** -- A Gaussian that resamples until a
  non-negative value is obtained.
- **exponential(mean)** -- Exponential distribution via inverse-transform
  sampling: `-ln(u) * mean` where `u` is uniform on (0, 1).

All interpolation uses piecewise linear tables with 11 points spanning a
defined input range. Values outside the range are clamped to the nearest
endpoint.
