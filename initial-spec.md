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

The screen is a 10-column by 25-row grid of labeled cells organized into
framed, titled sections. The default window size is 1024x768 pixels; cell
dimensions scale proportionally so the layout adapts to different window
sizes. The background is white with black text; font sizes scale with the
cell height. Light gray horizontal lines separate the rows within each
section for readability. Below is a wireframe:

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
| `w` | Buy/sell wheat       | `S` | Set slave feed rate  |
| `s` | Buy/sell slaves      | `O` | Set oxen feed rate   |
| `o` | Buy/sell oxen        | `H` | Set horse feed rate  |
| `h` | Buy/sell horses      | `p` | Set acres to plant   |
| `m` | Buy/sell manure      | `f` | Set manure to spread |
| `l` | Buy/sell land        | `q` | Set pyramid quota    |
| `L` | Borrow/repay loan    | `g` | Hire/fire overseers  |
| `r` | Run for one month    | Esc | Close dialog         |

All dialogs show their keyboard shortcut in parentheses after the title,
e.g. `buy-sell wheat (w)`.

### Mouse

Click on any section to open the corresponding dialog:

| Section        | Click action                                  |
|----------------|-----------------------------------------------|
| Commodities    | Buy/sell dialog for the clicked commodity row  |
| Prices         | Buy/sell dialog for the clicked commodity row  |
| Feed Rates     | Feed dialog for slaves (row 1), oxen (row 2), or horses (row 3) |
| Overseers      | Hire/fire overseers dialog                     |
| Loan           | Borrow/repay loan dialog                       |
| Land           | Buy/sell land dialog                           |
| Spread & Plant | Spread dialog (left column) or plant dialog (right column) |
| Gold           | Borrow/repay loan dialog                       |
| Pyramid        | Set pyramid stone quota dialog                 |
| Run button     | Advance one month                              |
| Quit button    | Exit the game                                  |

### Buy/Sell Dialog

Select a function via keystroke:
- `b` = Buy
- `s` = Sell

Then enter the desired amount and press Enter to confirm, or Esc to cancel.

### Overseer Dialog

- `h` = Hire
- `f` = Fire

Then enter the number and press Enter to confirm, or Esc to cancel.

### Loan Dialog

- `b` = Borrow
- `r` = Repay

Then enter the amount and press Enter to confirm, or Esc to cancel.

### Feed / Plant / Spread / Pyramid Dialogs

Enter the desired amount and press Enter to confirm, or Esc to cancel.

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

Each neighbor has a distinct face (portrait bitmap) extracted from the original
game's resource fork. The four portraits are stored as PNG files in
`resources/faces/man1.png` through `man4.png` (see Appendix A). At game start,
faces 0-3 are randomly assigned to the four personalities so that no two share
the same face. When a neighbor delivers a message, the message appears in a
dialog box overlaying the game screen with the neighbor's portrait displayed
alongside the message text.

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

## Messages

The game communicates through dialog alerts. Every message in the game is
spoken aloud using text-to-speech synthesis. Messages delivered by a neighbor
use that neighbor's voice settings (rate and pitch) and display the neighbor's
portrait in a dialog box (see Appendix A for portrait images); all other
messages (opening speeches, win/loss announcements, trading dialogs, input
errors, event narrations, etc.) use the default voice (rate 190, pitch 310)
and appear as a simple text bar. Each
message category has a pool of variant strings selected at random.

### Neighbor Advice Messages

Each of the 10 advice topics (oxen feeding, horse feeding, slave feeding,
overseers, stress, fertilizer, slave health, oxen health, horse health,
credit rating) has two pools of messages:

- **Good advice** (~6-15 variants) -- Compliments the player's management.
- **Bad advice** (~6-15 variants) -- Warns of poor conditions.

The good guy delivers the accurate pool; the bad guy delivers the inverted
pool; the village idiot randomly picks either; the banker never gives topical
advice. There is a 5% chance any advice is flipped regardless of personality.

### Generic Chat Messages

A pool of ~20 small-talk messages used when:
- The banker visits (he never gives topical advice).
- A 20% random chance overrides topical advice.
- The selected advice topic has no relevant resource (e.g., advising on
  oxen when the player has none).

Examples: "So, how ya doin there ol' buddy boy?", "Nice day, isn't it",
"Hey, how about those Crocks?"

### Idle Pep Talk Messages

A pool of ~50 messages delivered when the player is inactive for 60-90
seconds. These range from gentle prods to pop-culture references to absurd
humor. Examples: "Beam me up Scotty", "Boy are you ugly", "What did one
eye say to the other? There's something between us that smells."

### Dunning Notice Messages

A pool of ~40 loan-reminder messages delivered by the banker with escalating
severity. The frequency depends on credit rating (every 5 seconds for poor
credit, up to every 300 seconds for good credit). Messages range from polite
("May we respectfully remind you that you owe us some dough?") to
threatening ("Pay up your loan, or we'll break your legs") to absurd
("Olaf! Here boy.").

### Foreclosure Warning Messages

A pool of ~15 messages warning the player of imminent foreclosure when the
debt-to-asset ratio is dangerously high. These escalate from warnings to
game-over notices.

### Random Event Narration Messages

Each random event type has its own pool of narrative messages:

- **Acts of God** -- Descriptions assembled from adjective + disaster +
  consequence pools (e.g., "an incredibly large volcano devastated your
  property!").
- **Acts of Mobs** -- Assembled from crowd-size + motivation + action pools
  (e.g., "a huge crowd protesting animal abuse held a rock concert on your
  fields").
- **War** -- Win/loss messages with percentage outcomes.
- **Revolt** -- Descriptions of slave uprising with destruction percentage.
- **Health/Wheat/Gold/Economy** -- Each has ~5-10 narrative variants.
- **Locusts/Plagues** -- Brief destruction announcements.
- **Workload** -- Descriptions of extra labor demands with humorous premises.
- **Labor** -- Overseer raise demands and quit notices.

### Trading Dialog Messages

- **Supply limit** (~10 variants) -- When selling more than the market can
  absorb (e.g., "I am afraid I can't accept any more than %.0f.").
- **Demand limit** (~10 variants) -- When buying more than available supply.
- **Transaction success** (~10 variants) -- Confirming completed trades.
- **Insufficient funds** (~10 variants) -- When the player can't afford the
  purchase.

### Contract Messages

- **Counterparty default** (~8 variants) -- When the counterparty cancels.
- **Partial payment** (~8 variants) -- When the counterparty can't pay full
  amount this month.
- **Partial shipment** (~8 variants) -- When the counterparty can't deliver
  full amount.
- **Player insufficient goods** (~8 variants) -- When the player can't
  fulfill a sell contract.
- **Contract completion** (~15 variants each for buy and sell).

### Loan Messages

- **Credit check fee** (~10 variants) -- Informing the player of the cost
  to reassess credit.
- **Loan approval** (~15 variants) -- Confirming the loan with amount and
  interest rate.
- **Loan denial** (~15 variants) -- Gleefully rejecting the application.
- **Loan repayment** (~8 variants) -- Acknowledging full repayment.

### Input Error Messages

Each dialog (buy/sell, feed rate, planting, overseer, pyramid quota, loan)
has its own pool of ~5-10 humorous error messages for invalid input such as
non-numeric entries, negative values, or amounts exceeding available
resources.

### Opening Messages

A pool of ~20 speeches welcoming the player at game start, ranging from
solemn ("How high can you build your pyramid in 20 years") to humorous
("If you want to hear something clever, start the game over").

### Win Messages

A pool of ~10 congratulatory messages displayed when the pyramid reaches
target height, followed by ~5 farewell messages.

### Game Over Messages

A pool of ~10 messages for losing via foreclosure or bankruptcy, plus
~5 general farewell messages.

### Cash Shortage Messages

A pool of ~10 messages alerting the player when they run out of gold
during monthly costs, ranging from sympathetic to mocking.

### Overseer Messages

- **Missed payroll** (~8 variants) -- When the player can't pay overseers,
  they quit and demand a raise to return.
- **Raise demand** (~8 variants) -- Random event forcing an overseer pay
  increase.

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

---

## Appendix A: Message Pools

All messages below are extracted from the original game resources. Messages
containing `%s`, `%d%%`, `%.0f`, or `%.2f%%` are format templates where the
game inserts values at runtime. Messages are selected at random from their
pool each time they are needed.

### A.1 Opening Messages

- "Welcome to the game of Pharaoh."
- "Oh great and powerful Pharaoh, welcome to thy domain."
- "The game of Pharaoh is about to begin."
- "Hear ye one, and hear ye all. Here begineth the game of Pharaoh."
- "Now begins the greatest of all games. The king has taken his seat. The Game of Pharaoh is about to begin."
- "Blessed art thou oh great and dread lord. Hail thee Pharaoh, king of all the realm."
- "Prepare yourself for the greatest of all games. You are about to play,,,,, Pharaoh."
- "Mere mortal, dare you to attempt to play the game of games? Then ready yourself. Pharaoh is about to begin."
- "How high can you build your pyramid in 20 years?"
- "Many have tried, but few have succeeded. Try if you will, but beware. Such matters are the domain of kings, and not meant for the hands of mere mortal men."
- "Well, glad you could make it. This is the game of Pharaoh. Sit down. Relax. And prepare yourself. It begins."
- "Congratulations. You have just started up the game of Pharaoh. Now prepare yourself for battle in the world of ancient gods."
- "Are you prepared to match wits with the kings of old? Ancient ones who have learned great mysteries. Fare ye well, for luck you will surely need."
- "It takes great courage to do what you have done. But now you must face up to your decision. The test of pharaoh is upon you."
- "Can you withstand the test of kings, and the trial of power. Power is given to you, a land to rule. Rule it wisely and build a powerful nation. Then, if you dare, erect a monument to challenge the gods themselves."
- "Come into my kingdom young one. I give thee the staff of law and the crown of power. Decide wisely your course of action. You are now,,,,,,,,,,,, pharaoh."
- "So, you would like to be a king eh? Well, let's see if you can really cut it."
- "Those who aspire to be kings must pay the price of decision, and pass the test of knowledge. You have dared, so now the trial begins."
- "If you want to hear something clever, start the game over. I'm tired of coming up with new sayings all the time."
- "You dare to present yourself as one who could rule? You miserable weakling, do you think that you could decide between life and death for a nation? Since you have been so presumptuous, your wish is granted. Go forth and be,,,,,, pharaoh."
- "Good luck, you're going to need it."
- "Consider well your course. You have challenged the gods to make you a king. So be it. King you are. But beware, it's not as easy as you think."
- "oooooooo oooo oo oooo oooo o o o Gods of power, gods of strength. Show this fool that not just anyone can be a king."
- "The deed is done, the time has come. You pretend to be the one. To rule the land you now must try, but if you fail, then you must die. Good luck."
- "Fire and plague, pestilence and famine. Wars and droughts and rebellion and strife. These are the payment for those who would be king."
- "How good are you at handling riots? Do you know how much an ox eats each month? How much fertilizer does an acre of land require? How many soldiers do you need to protect your land? Well, you are going to find out."
- "The time has come, the hour is near, and even though you shake with fear, you must press on and find the path, gee you smell, please take a bath."
- "You look familiar, haven't we met before?"
- "One day, you might just win this game. Maybe today."

### A.2 Win Messages

- "You have won. You are wonderful. Hurray for you."
- "Wow! I never expected you to do it. You are great."
- "Your pyramid is complete! Strike up the band."
- "What a beautiful pyramid. It's so high and wide. Now you can die in peace."
- "This is terrific. You have done it."

Farewell messages after winning:

- "We are all so proud of you. You did a great job. Thanks for playing."
- "Boy what a fun time we all had playing with you. And the Banker didn't really mean all those mean things he said either. Bye Bye."
- "Well, you did it. And we never had any doubts at all, did we guys? Thanks for that great little slice of life. Adios."
- "Of all the pharaohs we have served, you were the best. Sorry you had to die. But you've completed a great tomb to spend eternity in."
- "Good work fella. We are all very very proud of you. Now you can die in peace."

### A.3 Idle Pep Talk Messages

- "Hey, I've got a dirty joke for you. . . . A white horse fell in the mud. Ha ha ha ha ha ha ha ha ha ha ha ha ha."
- "Of all the felt I ever felt, I never felt felt that felt like that felt felt."
- "Got any gum?"
- "You look pensive."
- "Can I help with anything?"
- "What's taking so long?"
- "Why are you staring at me like that?"
- "OK, let's get back to work."
- "Let's get down to business here."
- "I want to see activity. You can't run a kingdom by staring at a computer screen. Buy! Sell! Get to work."
- "Boy, these months seem to be going by slower and slower."
- "Is anything the matter?"
- "Boy are you ugly."
- "Hoooooo oo oooo oooooo ooooooo oo oo o o oooooo o o oo oooo oooooooo o o."
- "OK, stand up for exercises. 20 jumping jacks, ready? 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20. Good, now cool down and relax."
- "You're getting sleepier and sleepier. Your eyes are closing. Now get up and bark like a dog."
- "You look tired. Maybe you should just pack it in."
- "Wake up, get going. Let's make some progress here."
- "My apologies to the talking moose."
- "Hey, we've got lots of work to do. Now let's get busy."
- "If you don't do something soon, I may just run a month anyway."
- "I'm getting tired of waiting for you to make up your mind."
- "Decisions, decisions. What to do, what to do."
- "Maybe a cup of coffee would loosen up your brain cells."
- "Maybe a shot of whisky would clear the cobwebs."
- "What would happen if I hit the run button about now."
- "Do you know all the keyboard shortcuts? They sure make things easier."
- "Why aren't you paying attention to me like you used to. Is there someone else?"
- "Ho hum."
- "Get the lead out."
- "Boring. Very boring."
- "Gosh, you sure are indecisive."
- "Wishy washy. Make a move."
- "Haven't you got the guts to try something."
- "Let's go, I can't wait all day!"
- "Let's go now! I'm really getting tired of this."
- "What did one eye say to the other? There's something between us that smells."
- "It's so exciting when you look at me that way."
- "Go for it, it's the best you've got."
- "Abracadabra. You're a toad."
- "You have a little bit of goop in your eye."
- "Could you possibly breathe in a different direction? P U."
- "This is getting ridiculous. Let's get the show on the road."
- "snervelapitosisan nortilanimus. Gorthan shalumon, anthor saliday gamucha. Ventas cambosh goomidge."
- "Icky, icky, icky."
- "Oh my god, look behind you!"
- "Have you ever wondered about the meaning of life? I mean, why are we here? There must be more to all this than playing silly computer games."
- "I'm hungry. How about a hot dog."
- "How about some music, got any Strawberry Alarm Clock?"
- "Does anybody remember laughter?"
- "Badges? We don't got no badges. We don't need no badges. I don't have to show you no stinking badges!"
- "Do you feel lucky punk?"
- "Your mouthwash ain't making it."
- "There is a multi-legged creature on your shoulder."
- "I know what time it is. I don't need a blooming cuckoo clock."
- "Beam me up Scotty."
- "Book him Danno!"
- "Just the facts ma'am."
- "Warning Will Robinson."
- "There are always possibilities."

### A.4 Generic Chat Messages

- "So, how ya doin there ol' buddy boy?"
- "Yo! Pharaoh! Hows tricks. Farm runnin ok?"
- "Say, did you ever try selling slaves as a business. Yeah it's a bit seedy, but you can make some dough!"
- "My brother-in-law feeds his horses more than 90 bushels a month. Can you beat it? Of course they seem to enjoy it."
- "I had a buddy once who was determined to make his slaves work an acre apiece. They all died of course."
- "Some guys will put 50 slaves to an acre. This seems a lot to me."
- "Have you ever noticed that slaves mate more when they eat well?"
- "Razifratsasammanala! I just ran my fields with 10 tons of manure per acre. I had a horrible crop."
- "Yeah, fertilizer stinks, but it sure helps! A couple of tons per acre and zowie bowie, the crops take right off."
- "I have given 3 horses to each of my overseers. They love it. My slaves are far more organized now."
- "My aunt gussie used to give every two slaves their own ox. They had tremendous capacity to do work."
- "When my uncle finally figured out how much manure to use, he was growing crops in January that were bigger than any of his old July crops had been."
- "Some kings have been saying lately that a slave should be able to lay 3 pyramid stones per month. I'm not sure I buy it."
- "It stands to reason! Pyramids get harder to build as they get higher."
- "My cousin jake once enforced his quotas so firmly, that his overseers beat all his slaves to death. Then they all quit."
- "My father always used to say: 'keep your slaves fat and happy, and they will never revolt.'"
- "Nice day, isn't it."
- "Excuse me, but the wife sent me over to borrow a cup of sugar."
- "My mama told me: 'Son, never overwork your slaves. Otherwise they might be too tired to feed your horses.'"
- "Looks like rain."
- "Care for a show?"
- "Hey, how about those Crocks?"
- "My son just bought one of those big boom boxes. Jeez what a racket."
- "Hey, any fanatics stood on their heads in your fields? They ruined several of my crops!"
- "Your nationalist policies are a bit stringent."
- "I don't like the color of your hair."
- "oooooooo oooo o o oooo. What a hangover. o o o hooooee."
- "Hey, come on over later for a party. Should be some fun!"
- "We should get together sometime to see if we can't standardize on our slave branding techniques."
- "I love your sandals!"
- "God, I love hot peppers, don't you?"
- "Anyway, I figure that if I want to build a pyramid of any decent size, say 1000 feet, then I am going to have to employ around 100000000 slaves for several years."
- "I hear old Nebuchadnezzar is going to fight the Hittites again. Last time he did that, he lost half his kingdom."
- "Ya gotta make sure your slaves stay healthy. They can get real sick without your knowing it. And your overseers will make them work by beating them. Next thing you know, they're all dead."
- "Hey, sorry about last night. I didn't mean to drink so much wine. Guess I made kind of an idiot out of myself."
- "My wife asked me to tell your wife that she loved her dress at the party the other night."
- "Try to keep your assets at least twice as valuable as your debts. Otherwise the bank might be tempted to foreclose."

### A.5 Dunning Notice Messages

- "I want that dough, and I want it now!"
- "Get on your chariot and get over here. You owe us money, and we want it."
- "Excuse us, but we have a little matter of a loan to discuss. Would you please come by so we can chat."
- "I want that dough you owe me!"
- "Repay loan. Repay loan. Repay loan. Repay loan. Repay loan."
- "If I cut off your right ear, and your left little toe, do you think it might convince you to come in and pay your loan?"
- "Have you ever met Olaf? We sometimes send him around to deadbeats like you, to collect loan payments. You don't want to meet Olaf. Really you don't."
- "Olaf! Here boy."
- "How in the world did you ever get credit at our bank. It will be a long time before we give you another loan."
- "I would like to mention at this point, that you owe us money. When may we expect payment?"
- "When are you going to make your just debt right?"
- "We loaned you that money in good faith. Now we would appreciate payment in equally good faith."
- "We don't bother with debtor's prison around here. We just kill you, and take all your property."
- "We have ways of dealing with people like you who never pay their debts."
- "I call upon the gods: Sutec, Ra, Horus and Liela, to pour plagues and misfortune upon you until you make a payment on your loan."
- "A payment on your loan today, will make these messages go away."
- "We think you ought to pay your loan. Or you might find us on the phone. We'll call and visit and ruin your day. We'll make you sweat until you pay. Burma shave."
- "We haven't heard from you for such a long time, that we thought we'd come and visit you. Where's our dough?"
- "This is just a friendly reminder. You owe us money. Please pay. Do it now. Why wait."
- "If you like your eyes, do exactly as I say. Move the mouse to the loan box and click. Select repay. Enter a suitable amount. Click on OK."
- "Please pay your loan or you will get more poetry like this. . . Oh for the days when life was gay, and men were full of courage. But woe am I for now I cry over times when men eat porridge."
- "Would you mind making a payment or two on your loan."
- "You deadbeats are all the same. Never pay your loans. Now please make a payment."
- "If you don't make a payment soon, we may have to foreclose on your property."
- "You sure aren't doing your credit rating any good. Please make a payment on your loan."
- "May we respectfully remind you that you owe us some dough?"
- "Pay up your loan, or we'll break your legs."
- "Please, Please, Please, think about paying your loan."
- "Interest payments, like your mouthwash, aren't making it! Pay up now!. (Sorry Clint.)"
- "It's time for you to make another loan payment buddy boy. You'd better do it soon!"
- "You are in default of your loan. If you don't pay up soon, we will kill you."
- "We haven't seen you at the bank for a while. We would like to see you. Now. Right now."
- "This game can end very quickly. Would you like that? If so, continue to disregard your loan."
- "You had better make your loan payments in a hurry."
- "Our tellers are getting old and grey waiting for you to make the payments you have promised."
- "We are considering foreclosure. Please pay now."
- "If we don't receive payment from you soon, we will be forced to turn your name over to Olaf's collection agency."
- "Avoid the disgrace and embarrassment of bankruptcy. Pay your debts now."
- "You would be more than just wise to pay your debts now. You might also be living longer."
- "I'm afraid your health is being threatened by your delinquent account. If you get my drift."
- "We are here to help you. How can we help you pay this loan. I am sure we could think of a way."

### A.6 Foreclosure Warning Messages

- "Warning! You are in severe danger of foreclosure."
- "Your lawyer has been contacted. The bank is preparing to foreclose."
- "Take this seriously! Your debt is too great, your assets too low. The bank is seriously contemplating foreclosure."
- "Beware. You must placate the bank very soon. Foreclosure proceedings have already been started."
- "You are in severe danger of losing your estate. The bank has begun serious foreclosure discussions."
- "You are in big trouble buddy. If you don't pay some of your debt, or gain some assets soon, the bank will foreclose."
- "You had better turn this business around real fast. The bank is very very uneasy about your debt."
- "The bank is getting afraid that you will lose the assets you still have. They are planning to seize them soon."
- "This business is really on the rocks. The bank is preparing foreclosure documents. You will be served soon."
- "Be prepared for the repo man. He's coming soon to take your property."
- "Have you noticed that the guys in the bank are avoiding you? They are considering repossession!"
- "Get your act together real soon! You are about to lose this game."
- "This is no joke. You are about to lose everything you have worked for! Get this business running right!"
- "You are quickly losing this game. You have too much debt, too few assets."
- "You will lose this game in the next few turns."
- "I like beating you. It's fun."
- "You are about out of luck. Maybe you should just give up."
- "Your debt is way out of line with your assets. I can't let you keep this up much longer."
- "Forget it. You are really playing this game badly."
- "Kings who run their kingdoms into the ground like you are doing, usually get their heads handed to them on a plate."

### A.7 Foreclosure / Game Over Messages

- "I am sorry, but your assets can no longer support your loan. We are forced to foreclose on you. Goodbye."
- "Foreclosure proceedings are always so uncomfortable. Sorry about this. Good bye."
- "You should have kept your credit rating higher. The bank no longer trusts you to pay back their goods. They are seizing your property and writing you off."
- "You blew it. The bank is foreclosing on your property. You were warned!"
- "It's not as though we didn't warn you that this would happen. Your debts are too high, your assets too low, and your history stinks. Good bye."
- "Well, you have lost. I told you it would happen didn't I. Next time try not to get so deeply in debt. Adios muchacho."
- "Well, you've played this game about as badly as you could. Your debt is too high for your assets to support. You have failed big. Toodles."
- "Goodbye. You had your chance and you blew it. No more chances. It's over."
- "The first time I looked at you I thought you'd be a flop as a king. I was right. Goodbye."
- "So now you know: Being a king is harder than it looks."
- "You have let your financial position become too weak to continue. Farewell."
- "So long, farewell, auf wiedersehen, goodbye. Toodaloo."
- "That's all folks. You lose."
- "It certainly was nice playing with you. Please come back again some time and try again!"
- "I'm sorry, you have lost your kingdom. Please come back soon and try again."

### A.8 Cash Shortage Messages

- "oh oh. You have run out of cash!!"
- "Defaulting on your payments will not help your credit rating."
- "Hey! You have run out of money. How can you pay your bills?"
- "Ooops, you are bouncing checks ol' buddy boy."
- "The bank watched with interest as you ran out of dough this month."
- "You ran out of money! How could you do that? Oh I am so embarrassed for you."
- "Shoddy operation there old sport. You ran out of cash. Wish I could loan you a little, but I don't believe in that sort of thing."
- "Hey! You have run out of cash. Gee I sure hope the bank will give you a loan."
- "You have used all your fluid capital. Now you will have to go crawling on your belly to the bank and hope that they will have mercy on you."
- "Careless, careless. You have run out of cash."

### A.9 Bankruptcy Messages (No Loan Available)

- "The party's over. You have run out of dough, and the bank doesn't want to see your face."
- "The bank officer sneers as he boots you out of his office. You are out of it buddy. No dough, no go."
- "Ha Ha Ha Ha Ha. Got another one. You have lost. No dough, and No Credit."
- "Bankruptcy! You have lost the big one."
- "Your kingdom is pushing daisies Pharsie Warsie. You are out of cash, and the bank hates you."
- "We'll meet again, don't know where, don't know when. But I know we'll meet again some sunny day."
- "Ha ha ha ha ha, you lost. Ah ha ha ha ha ha ha. I think this is so funny. Ha ha ha ha ha ha ha ha ha ha ha."
- "Do you know what it means when you have run out of money, and the bank won't extend you a loan? It means that the game is over."
- "oooooooooooooooooooooooooooooooooooooooooooooooooooooo. This game is over now."
- "Well, we got you ace. You through, kaput."

### A.10 Loan Messages

#### Credit Check Fee

- "oooooo, I don't know about this. That's more than your credit limit. I'm afraid it will cost you %.0f to have your credit limit reassessed."
- "Ha ha ha ha ha. Ah ha ha ha ha. Its going to cost you %.0f to find out if you've even got the credit. Ha ha ha ha ha ha ha."
- "Well now. You seem to have exceeded your current line of credit. If you must have this loan, it will cost you %.0f for another credit check."
- "Your credit is blown. Pay %.0f and we'll see if you deserve a higher line of credit."
- "You deadbeats are all the same. Loan after loan. Well it's gonna cost you %.0f to find out if you've even got the credit rating."
- "Your current credit rating won't allow such a loan. If you would like we can run another credit check. You might just qualify. But it will cost you %.0f. Do you want to do it?"
- "We will gladly run a credit check to see you qualify. It will cost you %.0f, and you won't get it back if you don't qualify. Sound like a deal?"
- "Oh, I just love charging money for things. You have to pay %.0f to see if you qualify for that loan."
- "Pay %.0f right now, and we might just be able to find that loan for you."
- "Well, the loan officers are not inclined to grant you this loan. But if you pay me %.0f I might be able to talk them into it. No guarantees though."
- "You pay your money, you take your chances. A credit check is going to cost you %.0f non-refundable."
- "%.0f will be the cost, if you fail it's lost. Credit checks are fun when you hold the gun."
- "It's up to you, whatever you do. Pay %.0f to see what the answer will be."
- "I might just be able to do something for you here. But it will cost you %.0f to find out."
- "Isn't capitalism great. I am now going to charge you %.0f just to see if you qualify for this loan. And if you don't, I keep the money anyway."

#### Loan Approval

- "OK, you get the loan. %.0f at %.2f%%."
- "It would appear that you have sufficient collateral for a loan of %.0f at %.2f%%."
- "Big deal, so you've got the credit for %.0f. But can you afford %.2f%% interest. Ha ha ha ha ha."
- "Lucky break for you! %.0f at %.2f%% is not a bad deal, considering."
- "We'll get you next time deadbeat. This time you get %.0f at %.2f%%."
- "After much thought and deliberation, we could not find any good reason not to give you %.0f at %.2f%%."
- "As much as we would have liked to turn you down, we couldn't find any good reason. Your loan is %.0f at %.2f%%."
- "How does %.0f sound? Good? I'm glad you approve. Oh yes. The bad news is that you have to pay %.2f%% interest."
- "We like your face. (gasp, barf.) We will give you %.0f at %.2f%% if you will just take your money and get out of here."
- "How could we say no to breath like yours. Here is %.0f at %.2f%%."
- "I am most appreciative of the opportunity to serve you. (retch, puke) You are granted %.0f at %.2f%%."
- "Well I wish I could have turned you down. I always feel better when I can ruin someone's life. But the boss says you get %.0f at %.2f%%."
- "We were about to turn you down, but then you might come back in here, and we couldn't bear that thought. We will give you a loan for %.0f at %.2f%%."
- "%.0f is a lot of money. Make sure you don't spend it all in one place. And make sure you make your %.2f%% interest payments."
- "Done. %.0f at %.2f%%."

#### Loan Denial

- "Oh I'm so sorry. We just can't seem to find that loan for you. I hope you haven't been too inconvenienced by this. Have a nice day."
- "One look at you, and I knew you were a deadbeat. Why'd you ever think a credible institution like this would give you a loan. Ha ha ha ha ha ha ha."
- "Ha ha ha ha ha. Ah ha ha ha ha. Ha ha ho ho ha ha ha. NO DEAL! Ha ha ho ho ho ho ho. Ha ha ho ho ho ho."
- "This is just the very most favorite part of my job. I love telling people that they didn't get the loan. Now get lost loser."
- "We tried very hard to find a loan for you. But you are such an obvious loser that there just wasn't any way. Sorry about that."
- "If I didn't enjoy this so much, I might feel bad for you. But I don't. You don't get the loan."
- "I'm happy to say, that we won't pay. So hop on your horse and be on your way."
- "All it took was one good look, and we knew you were a schnook. No loan, no cash, no deal."
- "It gives me great pleasure to tell you that we have not approved your loan. And you are ugly too."
- "Life is tough sometimes. But you've got to go with it. Roll with the punches so to speak. No loan."
- "NO, NO, NO, NO, NO, NO, NO, NO, NO."
- "Well, after lots of consideration, we have come to the conclusion that you really deserve that loan. But we aren't going to give it to you."
- "OK. Loan approved. Stop in next week to pick up your check. Goodbye. (just kidding, ha ha ha)"
- "So, you want a loan eh? Well let's see now. No. sorry, no."
- "God I love this. Get lost!"

#### Loan Repayment

- "Congratulations! You have paid back your loan."
- "Well done my boy. Your loan is completely repaid. Care for another, or shall we leave it at that."
- "My but we are going to miss those nice little interest payments you've been making."
- "Hey, what's the rush? Why don't you borrow some more?"
- "Big deal. You want a medal now?"
- "Sir. There is a multi-legged creature on your shoulder."
- "Badges? We don't have no badges. We don't need no badges. I don't have to show you no stinkin badges."

Repayment sign-offs:

- "Well King, this case is closed."
- "Holy amortization Batman."
- "Oh Pancho. Oh Cisco."
- "Truth, Justice and the American way."
- "Happy trails to you, until we meet again."
- "You'll be back. They always come back."
- "Quiet in the peanut gallery."
- "Good night John boy."

### A.11 Trading Messages

#### Supply Limit (market won't absorb more)

- "I am afraid I can't accept any more than %.0f. My store rooms are full enough as it is."
- "Why is everybody unloading this stuff. I can only take %.0f."
- "I will take %.0f. You will have to keep the rest."
- "Finding buyers for this stuff is impossible. I will only take %.0f."
- "I am afraid there is a bit of a glut in this commodity. I can only take %.0f."
- "Yikes! Everybody has had enough of this stuff. I can only take %.0f."
- "Sorry, %.0f is all I am willing to take right now."
- "You can't force me into taking more than %.0f. I know my rights!"
- "Gee, I wish I could help you. But all I can take right now is %.0f. Sorry."

#### Demand Limit (market running out of supply)

- "I am afraid that I can only spare %.0f."
- "There has really been a run on this stuff lately. I only have %.0f left."
- "Everybody and his brother wants some. I can only let you have %.0f."
- "I hope %.0f is sufficient, it's all I have left."
- "The whole kingdom beat you here. All I have left is %.0f."
- "Wow, what a day. First the butcher's wife and now you. I'm afraid I only have %.0f left in stock."
- "Wait just a minute. Let me check in the back room. . . . . Nope, %.0f is all I have left. Sorry about that."
- "Somebody just beat you to it. They took all but %.0f. But you are welcome to take that."

#### Transaction Success

- "This transaction was successfully concluded."
- "Thank you for buying my quality merchandise."
- "You are a gentleman and a scholar. A pleasure doing business."
- "Contract terminated. All goods delivered."
- "Great. Thanks. It's over now."
- "I have delivered all the goods we agreed upon. Thank you for a pleasant association."
- "It was great doing business with you."
- "Thank you for the sale. I hope you enjoy your goods."
- "I left your goods in the barn. Thanks for buying them. Bye."

#### Insufficient Funds

- "You only have the cash for %.0f."
- "Hey, no credit! You can only afford %.0f."
- "OK Deadbeat, you know as well as I do that you only have the cash for %.0f."
- "For crying out loud, you can't buy more than %.0f!"
- "Oh! A joke. Ha ha Ha. oooooooo oo oo. You can only afford %.0f."
- "How can you expect to be taken seriously when you can't even count. You can only afford %.0f."
- "You don't have enough money ace."
- "Even if you used all your cash, you could only buy %.0f."
- "Hey, do your homework before you come in here. You don't have enough money for that."

#### Selling More Than Owned

- "Hey, you can't sell that much!"
- "We used to hang people for trying to sell more than they have."
- "You only have %f, how can you sell more?"
- "If you take a good close look at your books, you will see that you have only %f."
- "%f is all you have. Please don't try to sell more. I may have to get violent."

### A.12 Contract Messages

#### Counterparty Default

- "I'm terribly sorry, but I must cancel our agreement."
- "Buzz off dork. I'm not doing business with you."
- "Sudden circumstances make it impossible to continue with our agreement."
- "Unfortunately, an error has been made. This contract must be cancelled."
- "I am afraid I couldn't pay you if we continued this contract."
- "There has been a small change in plans. . . . I am cancelling our agreement as of now."
- "It just so happens that I got a much better deal from the Gasokary down in the valley. So I am afraid I must abort our tentative plans."
- "I honestly didn't think you were serious. I have since made other arrangements. Sorry about this. I'll catch you next time for sure."

#### Counterparty Partial Payment

- "I am afraid I can't accept a complete shipment this month."
- "Will you please hold some of it till next month."
- "Due to bad weather, my funds have not arrived from Tunzankipital. I will pay what I can. Please hold the rest."
- "Have no fear. Next month I will be able to take complete delivery. But this month I cannot."
- "I fear that I cannot take it all this month."
- "I really am quite sorry about this, but I just can't seem to raise all the necessary funds. I'll be back next month for the rest. You can depend on it."
- "Oh gee, I must have left my wallet at home. Well I have a little money with me, I'll bring the rest next month. Trust me."
- "You look like an understanding fella. You know how it is. Sometimes you just can't get all the dough. Next month for sure."

#### Counterparty Partial Shipment

- "I'm afraid I don't have all the goods for you this month."
- "Next month I will have the rest of your delivery."
- "Due to circumstances beyond our control, some of your goods have been backordered."
- "Can't do it all this month chum. We'll get around to it in a few weeks."
- "Most great apologies. But goods have not yet been completed. Please wait till next month."
- "Can I help it if my suppliers are turkeys? I wasn't able to completely fill your order. I'll make it good next month."
- "Hey, life is hard. I just couldn't find everything you asked for. Next month for sure."
- "This isn't my fault. My people really let me down. I'll try to complete your order next month."

#### Player Insufficient Goods (BUY contract)

- "You don't have sufficient goods to sell."
- "You have not met your contract."
- "You have insufficient goods."
- "You are a dirty welcher."
- "You better have it all next month."
- "Hey! You didn't send me everything. What am I supposed to do now, I need those goods!"
- "I was depending on receiving all the goods. Now you turn up short. Is this how you usually do business?"
- "If I'd known you weren't going to deliver all the goods on time I would have taken my business elsewhere."
- "Why haven't you sent me all the goods we agreed upon? This is going to cost you!"
- "It'll be a cold day in Hades before I buy anything from you again! You'd better have the balance next month!"

#### Player Insufficient Funds (SELL contract)

- "Hey, you don't have the money to pay me for my goods."
- "Welcher! You said you'd pay. Now you don't have the dough."
- "See if I ever do business with you again. Better pay next month puke face."
- "Oh no, not another deadbeat. Look, pay the rest next month or else."
- "I will await your payment next month with great interest."
- "I was really counting on completing this sale this month. I wish you had the money!"
- "Geez, what a nerd bag. Don't you know that you are supposed to have the money ready for me."
- "You nort fluke. I need my money now. Where do you get off telling me to wait?"
- "Just see if I ever do business with you again. I can't abide people who don't meet their commitments."

#### BUY Contract Completion (counterparty bought from player)

- "This contract is complete."
- "Let us go celebrate the sale."
- "The goods have been delivered."
- "This transaction is concluded."
- "Thanks very much."
- "Such wonderful merchandise I've never seen before. Thank you for your delivery."
- "Well, that's that. Looks like some good stuff you've sold me. Thanks a bunch."
- "Boy, I've bought this stuff just in time. Thanks for doing business with me."
- "I am pleased to take final possession of the agreed goods. It has been a pleasure doing business with you."
- "Thank you for selling me such high quality goods."
- "The more I think about this, the more I think you cheated me. Well, a deal's a deal, but I'm going to be more careful next time."
- "Your goods have been delivered, our agreement is complete. Thank you for doing business so honorably."
- "Hey, let's do this again sometime. I like buying things from you."
- "Well, this has been delightful. Please stop by any time to sell your goods to me. You schmuck."

### A.13 Neighbor Advice Messages

#### Oxen Feeding -- Good

- "Your oxen are looking mighty well fed."
- "Them oxen of yours sure do look like they're eating well."
- "I like the way your oxen look. I think I'll feed mine more too."
- "Say, your oxen look like they're fat and happy."
- "Y'know, I've been noticing your oxen. I think they look pretty damn good. You must be feeding them right."
- "I used to think you were nuts feeding your oxen as much as you do. But from the results I can see that you were right."
- "Your oxen look so healthy! It must be all that food you feed them."
- "You are going to receive a special commendation from the regional oxen care society for feeding your oxen in the proper manner."
- "You deserve a lot of credit for feeding your oxen so well."
- "You are bound to get a lot of very good work out of your oxen, the way you feed them."

#### Oxen Feeding -- Bad

- "Wow, your oxen sure are scrawny! Are you sure you're feeding them enough?"
- "Yikes, those oxen of yours look half starved. Maybe you should feed them more."
- "If I were you, I'd feed them oxen more wheat. They look like they could get sick and die on ya."
- "This ain't none of my business, but I think you must be starving your oxen."
- "If you don't want to feed those oxen, sell em. Otherwise they're just gonna die on ya."
- "Oxen need a lot of food. Much more than you are feeding them."
- "I think you should think about rethinking your oxen food allowance."
- "Oxen are big hairy beasts. They do lots of work, and they like to eat. They like to eat a lot. Yours aren't very happy."
- "Think about how big a man is. Then think about how big an ox is. Oxen need lots of food. (Hint, hint.)"
- "Uh, buddy. I'll give you a little tip. Are you ready? Feed your oxen more."

#### Slave Feeding -- Good

- "Hey, I like the way your slaves buckle down and get the job done. You must be feeding them right."
- "What type of food are you feeding your slaves. They really look good."
- "I wish I could get that type of energy out of my slaves. What are you feeding yours?"
- "I envy you your slaves. They work and work, and still they look good. Perhaps I should feed mine as much as you are feeding yours."
- "I know it's expensive to feed your slaves the way you do, but it sure looks like it's worth the cost."
- "You have certainly mastered the art of feeding your slaves."
- "Your slaves seem quite well fed."
- "Can you come to my castle and deliver a talk on how to properly feed slaves."
- "Your slaves are probably better fed than any in the kingdom."
- "If you can manage to keep feeding your slaves at the current rate, you are bound to get lots of work out of them."

#### Slave Feeding -- Bad

- "Your slaves look mighty scrawny. I know it's expensive, but maybe you should feed 'em a bit more."
- "Look, you aren't going to get any work out of your slaves if you starve them to death."
- "I have always found that you get much more work out of your slaves if you feed them well."
- "Hey, if you're going to keep those slaves so skinny and sickly, then please build a fence around your kingdom so that I don't have to look at them."
- "Excuse me for butting in, but those slaves of yours need more food man!"
- "I suggest you start feeding your slaves a bit more."
- "Your slaves are not being fed enough to keep them alive."
- "If you don't start feeding more to your slaves, you are going to lose them."
- "Your slaves need more food if you expect them to do any real work."
- "If you don't feed your slaves, they are going to be too weak to work, and then your overseers will beat them to death."

#### Horse Feeding -- Good

- "You must really know how to take care of your horses. They look great."
- "I guess there's a secret to proper care and feeding of horses. You must know that secret."
- "Boy, your horses look sleek and fine. You must be feeding them right."
- "I sure like the looks of your horses."
- "Say, can I get the recipe for your horse feed. Your horses look so good!"
- "Your horses sure look well fed."
- "Your horses have a nice layer of body fat."
- "Your horses never look hungry."
- "One thing. Your horses never look scrawny or weak."
- "Your horses seem to eat enough."

#### Horse Feeding -- Bad

- "Yow! Your horses look terribly malnourished."
- "Can you manage to feed your horses better?"
- "If you don't start feeding your horses more, I may just have to call the S P C A."
- "Jeez! If you're going to feed your horses so little, you might as well just shoot 'em."
- "Look, I hate to talk out of school, but the neighbors are getting upset at the horrible way you treat your horses."
- "Your horses need more food man."
- "Blither blather snort and snather. You should feed your horses better."
- "If you want to know what an undernourished horse looks like, go look in your stables."
- "Omigod what horrid little creatures your horses are. Can't you do something to fatten them up a bit."
- "You know, your horses look hungry."

#### Overseers -- Good

- "Your slaves and overseers really seem to work like a well oiled machine."
- "Boy, your slaves really jump to it, when your overseers crack the whip. Maybe I should hire as many as you have."
- "Overseers are expensive, but I see that you know how well it pays to have enough of them."
- "You know, your overseers never seem to have any trouble getting your slaves organized."
- "What's your slave per overseer ratio? Whatever it is, it must be right. Your slaves really work well."
- "Your overseers seem to have your operation well in hand."
- "I like the way your overseers have your slaves organized. Efficient and economical, but plenty of structure and guidance."
- "How did you learn to balance your organization so well. Your slaves and overseers work beautifully together."
- "I love to watch your overseers get the slaves into the fields in the morning. It's like watching a military display. Your slaves are so well disciplined."
- "I want your overseers to teach mine how to organize their slaves."

#### Overseers -- Bad

- "Say, I notice that your slaves look a bit misdirected. Perhaps they could use more guidance."
- "Why don't you hire more overseers. They might help you get your slaves organized."
- "I have found that slaves will be happy to sit and do nothing all day. That's why I always hire plenty of overseers."
- "It might be a good idea to hire some more overseers. Your slaves seem to wander around quite a bit, not knowing what to do."
- "Say, you don't keep many overseers do you? What protection do you have from invading armies?"

#### Stress -- Good

- "Your overseers seem relaxed and confident. I wish mine were like yours. Mine continually beat the slaves."
- "Have you sent your overseers to management training courses? They look so professional and cool about everything."
- "Could I borrow a few of your overseers to teach mine the right way to get things done? Mine are always so nervous."
- "How do your overseers get your slaves to make quota without beating them. My overseers kill lots of slaves every year with beatings."
- "What's your secret? I need overseers who can do their job as well as yours."
- "Your overseers are such model citizens. Always polite and always professional."
- "How do you get your overseers to meet their quotas without beating your slaves."
- "I love how your slaves seem to respect and admire your overseers. How do you work that?"
- "I wish I had a few of your overseers. They seem to be able to get things done."
- "Your overseers have the lowest suicide and divorce rates in the kingdom."

#### Stress -- Bad

- "I hate to complain, but the last few nights I have been woken up several times by the screams of your slaves as your overseers beat them."
- "Were you aware that your overseers have been tearing up the local bars lately. They seem to be under a lot of stress."
- "Say, a few of your slaves dug through into my kingdom the other day. I returned them at once, but I wanted you to know that they had been horribly beaten."
- "Say, I know you've been having some production problems lately, but beating your slaves so much will only get them sick."
- "Perhaps you should buy more slaves. Your overseers appear to be having problems getting what you have to do the work."
- "Some of your overseers have applied for work at my farm. I wouldn't hire them though, they were too nervous."
- "Your overseers have been getting into lots of trouble in town. I don't think they are happy working for you."
- "Boy are your overseers ever jittery and nervous. Sometimes they just stare into space with blank looks on their faces. What have you done to them."
- "I think you need to give your overseers a vacation. They all look so irritable and nervous."
- "Your overseers have been talking in the bars at night. They tell stories about how they have been beating your slaves to get them to fill your quotas."

#### Fertilizer -- Good

- "Say, I like the texture of your soil. It's rich and fragrant. How much manure do you use?"
- "The growth on your crops looks really good. You must be fertilizing well."
- "You seem to have found the knack for properly fertilizing your land."
- "I like the smell of your fields. They smell rich."
- "Say, that grain you have been producing certainly looks rich."
- "Your soil is just full of nutrients. You must be using the right amount of fertilizer."
- "I really like the quality of your crops. You must be doing something right."
- "Your wheat is so thick and full. How much manure are you using?"
- "You seem to have learned the secret of fertilization."
- "I want my crops to look like yours. Can you teach my overseers what you know about manure."

#### Fertilizer -- Bad

- "Say, you might want to use a bit more manure. Your soil seems pretty dry and brittle."
- "The dust from your fields is messing up my wife's laundry. Maybe you should spread a little more manure."
- "You know, the wheat you've been producing is mostly chaff. Hardly any kernel. I'll bet your yield is low too."
- "How do you expect to grow any wheat with such a small amount of fertilizer."
- "You need more fertilizer than you have been using. Your land looks sick."
- "You aren't using the right amount of fertilizer."
- "I think your soil lacks nutrients."
- "I think you ought to use more fertilizer."
- "Your crops look limp and small. You are probably not using enough fertilizer."
- "Why don't you try using an extra ton or two of fertilizer per acre."

#### Slave Health -- Good

- "Hey, your slaves look terrific."
- "I like the look of those tanned bodies your slaves strut around with all day long. oooooooooo o."
- "You really have a good crew of slaves out there. They're as strong as some of your oxen."
- "Say, could you keep your slaves dressed a bit more. My wife goes nuts whenever she sees them."
- "Hey, nice slaves you've got there. They ought to fetch top shekel on the market."
- "Boy, I sure like the looks of your slaves."
- "How do you get your slaves so strong and good looking."
- "Your slaves would do well in a Mr. Egypt contest."
- "Do your slaves lift weights? They sure look strong."
- "Your slaves are the best in the kingdom. How do you do it?"
- "I need my slaves to be as strong and energetic as yours. Can you give me some tips?"
- "Your slaves are terrific. I wish mine looked as good."
- "What can I do to have slaves as healthy as yours?"
- "Your slaves are setting the standard for fitness."
- "What great looking slaves you've got there. I need a few slaves like that."

#### Slave Health -- Bad

- "Your slaves look downright sick."
- "Oh please, do something about your slaves. They look revolting."
- "I think you have a problem with your slaves. They look dreadful."
- "If any of your slaves give what they have to any of mine, I am going to make you pay for them."
- "When the wind blows in my direction the smell coming from your slave quarters is intolerable."
- "oooooo, your slaves are really icky."
- "Your poor slaves can barely stand, let alone work."
- "I think you should give your slaves a rest. They look all in."
- "The way you treat those slaves, they aren't going to last much longer."
- "Can you really afford to lose your slaves to sickness and exhaustion?"
- "Your slaves are dying. What are you doing about it?"
- "You better ease off on your slaves. They are in very bad shape."
- "Why don't you give your slaves a month's rest. They really look like they could use it."
- "Please do something about your slaves. They are so very sick."
- "I can't stand seeing your slaves suffer the way they do."

#### Oxen Health -- Good

- "Your oxen really look good there buddy."
- "I like the sheen on the coats of your oxen."
- "I think those oxen of yours could out pull any two of mine."
- "Your oxen should sell quite well. They are fit and healthy."
- "We need to do something pharaoh. My female oxen are always coming over here to get at your males."
- "Wow, what great oxen you've got."
- "Your oxen are terrific looking."
- "Boy, those oxen of yours sure look strong."
- "I bet those oxen of yours are the healthiest in the kingdom."
- "With oxen like that, you should be getting lots of good work done."
- "I bet it really pays to keep oxen in such good shape."
- "Would you consider teaching my slaves how to take care of oxen the way yours do?"
- "Your oxen are certainly very elegant looking. How do you do it?"
- "I certainly like the way your oxen strut and stamp around when they are working. It shows that they have lots of energy."
- "You must have bought an excellent breed of oxen. They are very healthy looking."

#### Oxen Health -- Bad

- "Your oxen look downright terrible."
- "Hey, do you call those scrawny little bags of bones oxen?"
- "Why don't you treat your oxen better? They serve you better if you do."
- "You better do something quick, or all your oxen are going to die on you."
- "Keep those terrible looking oxen of yours away from mine. I don't want mine to get contaminated."
- "I suggest you call a vet. Your oxen are very sick."
- "Your oxen are so sick. How can you force them out into the fields every day."
- "Your oxen are the worst in the land."
- "You should take better care of your oxen. They are abominable."
- "Yikes, what terrible looking creatures your oxen are."
- "How can you look at those oxen and not know that there is something wrong with them."
- "Your oxen look as though they are on the verge of death."
- "I think your oxen are going to die soon."
- "How did your oxen ever get so ill."
- "Maybe if you kept your oxen in for a month, and fed them all the food they could eat, they would live. But I bet they wouldn't."

#### Horse Health -- Good

- "Wow, what good looking horses you've got there."
- "Say I'll bet some of those horses are prize winners."
- "Gee your horses are fast! They look good too."
- "You should be able to fetch a pretty sum for those horses of yours."
- "What do you do to get your horses looking so good."
- "Your horses are in gorgeous shape."
- "You've sure got great looking horses."
- "What kind of vitamins are you putting in your horse's feed. They sure are healthy looking."
- "What beautiful horses you've got."
- "Those horses of yours look like they could fight their way out of a cage of hungry tigers."
- "My goodness, what very fine looking horses you are raising."
- "I certainly like the looks of your horses, you must be feeding them well."
- "You should enter your horses in a show. They would be sure to win."
- "Would you consider giving a lecture on the care and feeding of horses. Nobody in the kingdom has such fine looking animals."
- "I sure wish I knew what you did to get your horses so healthy."

#### Horse Health -- Bad

- "Aye yi yi, your horses are in awful shape."
- "I can't stand to look at your horses any more. You'll never get a decent price for them. You'd better just shoot them."
- "If you are thinking of selling those horses, forget it. They are too sick to bring a decent price."
- "Those horses can't work! They are too damn sick."
- "Excuse me pharaoh, but your horses are about ready to keel over and die."
- "Why don't you feed your horses a bit more. They look awfully bad to me."
- "The society for the enforcement of kindness to animals has issued a huge complaint about the condition of your horses."
- "Ye gods, your horses are awful."
- "You have the worst batch of horses in all the land."
- "Your stables reek of sickness and death. I think you should check your horses."
- "The health of your horses is deplorable. They are nothing but skin and bones."
- "Please see what you can do to improve the health of your horses. They look so pitiful my wife cries whenever she sees them."
- "Excuse me, but your horses are really sick."
- "God, can't you feed your horses a bit better. They are nothing but bags of bones."
- "oooooooo o o oo what horrible horses you have."

#### Credit -- Good

- "Say, the bank has you on its list of the most trustworthy borrowers."
- "You must be paying your loans off right on time. The bank thinks quite highly of you."
- "I was at the bank the other day, and I overheard the tellers saying 'good old pharaoh, always gets his payments in on time.'"
- "You must really know how to make friends with the bank. They love you over there. They said I could have a loan if you would co-sign."
- "Boy, those bankers must believe all that rubbish about you being a god. They never say anything bad about you."
- "How did you get such a good credit rating? It sure wasn't clean living."
- "The bank has just erected a statue in your honor. You are very popular over there."
- "Powerful allies are nice to have. You certainly have one in the bank."
- "I see you have found the key to winning this game: Keep the bank happy."
- "Say, your credit rating is so high, the bank barely questions any loan you wish to make."
- "Good credit ratings are one of the keys of success. You are well on your way."
- "The bank has asked me to request that you give some lectures on keeping a good credit rating."
- "Say, don't believe your credit allowance, the bank loves you so much I am sure they would give you more if you asked."
- "You have managed your credit situation very very well. The bank considers you to be one of its best customers."
- "If you continue to keep the bank so happy, you will undoubtedly win this game."

#### Credit -- Bad

- "Whoooo boy, is the bank mad at you. You'd better start paying that loan."
- "You'd best get right over to the bank and pay that loan of yours. The bank is hopping mad."
- "Have you forgotten about your loan? I was just over by the bank, and they are getting a posse ready for you."
- "You might want to think about making a loan payment. The bank has put your name on the default list."
- "If you ever want another loan again, you'd better hop right over to the bank and pay them."
- "How did you manage to get the bank so mad at you? You have been making sufficient payments on your loan haven't you?"
- "There are wanted posters all over town with your name on them. The bank has issued a Dead or Alive reward to anyone who can get you to repay your loan."
- "I hate to intrude in your personal affairs, but if you don't repay your loan real soon, the bank is going to foreclose on you."
- "Did you know that you were recently named as the Pharaoh most likely to default on his loan?"
- "I can see why the bank doesn't trust you anymore. Your face does not inspire confidence."
- "Say, if you have any money stashed anywhere, I suggest you give it to the bank. They are really upset about the status of your loan."
- "Your credit rating really stinks. Are you thinking of declaring bankruptcy?"
- "At this rate, I don't see how the bank is going to let you finish this game."
- "I hope you have enjoyed your game, because I have a feeling that the bank is going to end it soon."
- "The bank wants money from you. The bank always gets what it wants."

### A.14 Random Event Narration Messages

#### Acts of God -- Sentence Templates

- "%s %s %s."
- "The anger of the Gods is kindled against you. They send %s %s which has %s."
- "Your scouts report %s %s. Further reports indicate that it has %s."
- "Your astrologer fails to predict %s %s which has %s. So you have him flayed."
- "The news of %s %s which has %s reaches you."
- "Your kingdom weeps after %s %s %s."

#### Acts of God -- Adjective Pool

- "an incredibly large"
- "an unpredicted"
- "a horrifyingly huge"
- "an immense"
- "a very very big"
- "an unbelievably gargantuan"

#### Acts of God -- Disaster Pool

- "volcano"
- "earthquake"
- "flood"
- "meteor impact"
- "forest fire"
- "hurricane"
- "explosion of unknown origin"
- "thunder storm"
- "dust storm"
- "rain storm"
- "typhoon"

#### Acts of God -- Consequence Pool

- "devastated your property!"
- "decimated your land and livestock"
- "destroyed much of all you own"
- "killed and ransacked your chattels"
- "wreaked havoc with your estate"
- "ruined your hard earned wealth"
- "razed your land and livestock"
- "ripped and rattled your property"
- "consumed much of your kingdom"
- "obliterated a portion of your land, and made hamburger out of some of your livestock"

#### Acts of Mobs -- Sentence Templates

- "It was %s of %s angered by %s who %s."
- "%s of %s motivated by %s %s."
- "This was not your month. %s consisting of %s incensed over %s just %s."
- "%s of %s accosted your estate. Presumably they were protesting %s. They %s."
- "Your scouts report %s populated by %s who were upset by %s. The reports indicate that they %s."
- "It is not easy to control %s of %s. Especially when they are upset by %s. Much to your sorrow they %s."
- "It is a sad state of affairs when %s of %s protesting %s can do these things. They %s."

#### Acts of Mobs -- Crowd Size Pool

- "a huge crowd"
- "an immense gathering"
- "a conglomerated mass"
- "a virtual nation"
- "a mob"
- "hordes"
- "an incredibly large gathering"
- "wave upon wave"
- "battalions"
- "veritable armies"

#### Acts of Mobs -- Motivation Pool

- "social injustice"
- "animal abuse"
- "your policies of slavery"
- "the hunting of whales"
- "your ugly face"
- "last year's winter"
- "crime and violence"
- "unsanitary bathrooms"
- "pets with fleas"
- "your nationalist policies"
- "your expansionist policies"
- "their mothers-in-law"
- "your monogamy laws"
- "your relative's deviate ways"
- "bad television shows"
- "the world series"
- "natural disasters"
- "animal nakedness"
- "prostitution"

#### Acts of Mobs -- Action Pool

- "held a rock concert on your fields"
- "held a protest march through your land"
- "threw rocks and sticks at your slaves"
- "attacked your residence and crops"
- "stood around and acted stupid for a few days"
- "dumped their garbage all over your land"
- "decimated your property with pickaxes and shovels"
- "held hands, and walked across your land single file"
- "built the world's largest human pyramid on your property"
- "set fire to your crops and lands"
- "held a huge bar-b-que on your fields"
- "committed mass suicide in the midst of your crops"
- "held a gymnastics meet on your land"
- "built dozens of little shacks all over your land"
- "stood on their heads for 7 days in the midst of your fields"
- "held a spitting contest on your crops"

#### War -- Attacker Pool

- "the Upper Slobovians"
- "the Ethiopians"
- "a horde of wild aborigines"
- "an incredibly aggressive flock of birds"
- "an army of revolutionaries"
- "men from Mars"
- "evil men from across the sea"
- "giants"
- "a small gaggle of dragons"
- "the Chicago Bears"
- "the crew of the Starship Enterprise"
- "Theodore Cleaver"
- "The Ayatolla Khomeini"

#### War -- Loss Messages

- "You have lost"
- "You have been decimated. Your losses amount to"
- "They have trampled over you and taken"
- "You have been utterly vanquished. They despoil you for"

#### War -- Win Messages

- "You have won"
- "You have blasted them to smithereens. You loot them for"
- "After thoroughly beating them, you despoil them for"
- "You are victorious and have increased your holdings by"

#### Revolt Messages

- "Your slaves are moved to revolt against you. In the struggle you lose %d%%."
- "Your mistreatment of your slaves results in a riot in which %d%% of your estate is destroyed."
- "A rabble rouser from a neighboring kingdom incites your slaves to take arms against you. In the struggle you lose %d%%."
- "A wave of discontentment motivates your slaves to strike against you. Their discontentment is such that you lose %d%% of your property."
- "The two major factions of your slaves declare war on each other. Resulting destruction to your holdings is %d%%."

#### Health Event Messages

- "The weather has been cool and damp, and everyone seems to have colds."
- "The hot dry weather has made everyone listless and weak."
- "Your slaves and animals seem unusually slow and irritable lately."
- "Something must have been wrong with the slaves' and animals' food. They seem to be suffering from severe diarrhea."
- "The water from the wells has been brackish lately. Your slaves and animals have been out of sorts."
- "A rival king has constructed a still on your lands. Your slaves and animals are all hung-over."
- "Drafty living quarters and rotten blankets have caused health problems in your living inventory."
- "The mosquitos this month have been so bad that all your slaves and livestock are suffering from blood loss."
- "A natural deposit of uranium has caused a mild case of radiation sickness to break out amongst your living inventories."
- "A slow poison was injected into the food supplies of your animals and slaves. Fortunately you discovered it before any permanent damage was done. But the victims are quite weak."

#### Plague -- Disease Pool

- "anthrax"
- "hoof and mouth disease"
- "typhoid fever"
- "Rocky Mountain spotted fever"
- "AIDS"
- "fulminous nechroids"
- "Diphtheria"
- "Tourette's syndrome"
- "Jungle rot"
- "measles"
- "chicken pox"
- "athlete's foot"

#### Plague -- Sentence Templates

- "Your slaves, oxen and horses have been decimated by a plague of %s."
- "A band of Gypsies passed through your slave encampments and brought %s with them."
- "Poor sanitation in the slave barracks exacerbated the spread of %s throughout the slaves and livestock."
- "Your wise men were unable to cope with the %s which has ravaged your living inventories."
- "The sexual preference of one of your relatives has angered the Gods into smiting your slaves and livestock with %s."

#### Locust Messages

- "Suddenly, a shadow passes over the land. . . An immense swarming cloud of hungry locusts descends upon your fields and devours your crop."
- "Ranks upon ranks of marching Soldier Ants cut a swath through the land and destroy your fields."
- "The temperature soars, and the sun stares down out of a cloudless sky for 3 solid weeks. Nothing remains of your crops but dried husks."
- "Rain pours from the sky day and night for nearly a month. Your fields have become lakes. Your crops are destroyed."
- "The angry Gods plunge your land into complete darkness for 27 days. When you finally see the sun again, your fields are covered with yellow, rotting, lifeless plants."
- "Temperatures plunge far below freezing for several days. Despite the valiant efforts of your slaves and overseers, your crops do not survive."

#### Wheat Event Messages

- "A horrible blight destroys %d%% of your crops."
- "%d%% of your crops are wiped out by a terrible thunder storm."
- "A grass fire destroys %d%% of your crops and silos."
- "Pests and critters of all kinds swarm over your fields and storage bins and eat up %d%% of your wheat."
- "An icky green slimy substance has been found covering portions of your wheat and silos. The affected wheat had to be discarded. Your losses amount to %d%%."
- "Little yellow crawly things were found in some of your wheat bins. You had to burn %d%% of your wheat to stop them from spreading."
- "Ickmach forsutia worms have invaded your land. You donate %d%% of your on hand wheat for the manufacture of worm-poison."
- "You feel like being a good guy today, so you give %d%% of your wheat to an orphanage. (brats!)"
- "The roofs in some of your silos leak. %d%% of your wheat has sprouted. Oh well."
- "You find the body of Jimmy Hoffa in amongst your wheat. You are forced to discard %d%% of your wheat."

#### Gold Event Messages

- "Thieves break into your treasury and take %d%% of your gold."
- "Your neighbor king has just bought a new home entertainment system. You spend %d%% of your gold to buy one better than his."
- "The queen needs more servants. You pay %d%% of your gold to a domestic employment agency."
- "Your overseers' children need new band equipment and uniforms. You donate %d%% of your gold for this worthy cause."
- "You pay your physicians %d%% of your gold to provide you with an immortality potion."
- "Your priests recommend that you create a golden idol to the Sun god RA. You give the goldsmiths %d%% of your gold for this purpose."
- "Your spouse gives %d%% of your gold to a used chariot salesman, and comes home with a 3 year old Marc Anthony, loaded."
- "A few well placed bribes with the overseers' union ought to keep your labor problems simple. %d%% of your on hand cash ought to do it."
- "Your blackmailer has been around. You pay him %d%% of your gold to keep him quiet a little longer."
- "You haven't been able to stay at the palace for weeks, the septic system backed up. P U. The plumbers charge %d%% for the repair job. (I would too.)"
- "You were in a generous mood this month. You spent %d%% of your gold on toys for the slaves' children."

#### Economy Event Messages

- "Your priests predict the end of the world. The market goes crazy. You have them all beheaded."
- "A solar eclipse causes hoarding and panic at the marketplace."
- "A recent overabundance of river frogs has caused strange effects in the market prices."
- "Who can say why the marketplace behaves the way it does."
- "Rumor has it that you are preparing for war. The speculation shows in the marketplace."
- "Hail and fire pour down from the sky. The market place is burned, and its ashes are pummeled into the ground. The market prices react accordingly."
- "A comet appears in the heavens. The market prices react with their usual rational behavior."
- "One thing you can't accuse the market prices of, is stability."
- "Darkness covers the land for days. The market prices do strange things."
- "Isn't it weird how a little thing like a plague from the gods can send the market prices haywire?"

#### Labor Event Messages

- "Overseers are disgruntled by working conditions. They strike for a %d%% raise."
- "Overseer turnover is high. You are forced to give them a %d%% raise to mollify them."
- "Your overseers hate your guts. You have to give them a %d%% raise just to keep working for you."
- "So many of your overseers have been quitting lately that your advisors have recommended that you pay them %d%% more."
- "You were feeling generous today. (again.) So, you gave your overseers a %d%% raise."
- "Your overseers have been doing so well lately (ha ha) that you are moved to give them a %d%% raise. (Actually the union threatened to strike.)"
- "A neighboring king, jealous of the incredible efficiency of your overseers (ha ha), has been trying to recruit them. You give them all a %d%% raise to make sure they stay with you."
- "It's salary review time. %d%% for all the overseers."
- "Some dork in the next county is paying his overseers more than you. You are forced to give your overseers a %d%% raise to keep them happy. (Rotten luck.)"

#### Workload Event Messages

- "%s are required for raking the leaves."
- "Your slaves require an extra %s in order to mow the lawn."
- "The high priestess of Isis is coming to visit. Your slaves expend %s to landscape and manicure the grounds."
- "Your son wants a treehouse. You order your slaves to build it. They expend %s to complete the task."
- "You need new wells. The job requires %s of your slaves effort."
- "The queen wants running water in her bathroom. Your slaves expend %s completing the job."
- "Your brother in law throws a party for his drinking buddies. He uses %s of your slaves time preparing and catering it."
- "%s of your slaves time is used up building an extension onto your study."
- "Your slaves spend %s at your son's orders, searching the land for small round stones that he can skip on the river Nile."
- "Your priests insist that a holy wheat kernel was grown in last month's crop. Your slaves spend %s searching for it. Without luck."

### A.15 Overseer Messages

#### Missed Payroll

- "Mooooooooooo you didn't have the dough to pay your overseers. They all quit."
- "Your overseers want a %5.1f%% raise. You missed their payroll. They have all quit."
- "The rotten thing about employees is that they want to be paid."
- "The Overseers union demands a %5.1f%% increase since you missed their payroll."
- "Shoddy operation. You ran out of cash on payday. Your overseers have quit."
- "A message from your overseers: 'We quit!' Perhaps they will come back if you offer them a %5.1f%% raise, and promise to pay them on time from now on."
- "Why didn't you keep enough cash to pay your overseers?"
- "It's awfully unfriendly to be late with your overseers' paychecks. Now they have all quit."
- "You should have paid your overseers on time. Now they are leaving in droves. Perhaps more money will entice them to return."

### A.16 Input Error Messages

#### Buy/Sell -- Invalid Input

- "I cannot read your horrible writing."
- "What exactly are you trying to say here."
- "I thought we were conducting a business transaction, but I guess we must be finger painting."
- "Commercial activities do not usually include randomly poking the keys."
- "Could you repeat that please. I can't seem to make any sense of it."

#### Buy/Sell -- No Function Selected

- "Excuse me, but are you buying or selling?"
- "What exactly is it that you would like to do?"
- "Ah yes, you would like me to read your mind. Well then, shall I sell all you have, or would you like to try again?"
- "Hey! I'm in a hurry here. Could you quit farbing around and tell me what you want to do?"
- "I can't complete your transaction, until you tell me what you are doing."

#### Buy/Sell -- Negative Input

- "Yeah, right. I'll just reach right into their guts and rip the food out."
- "Negative food. Hmmmm, an interesting concept."
- "Oh come now!"
- "Shall we try that again please?"
- "If you look closely, you will see a horizontal bar in front of the numbers. Please remove it."

#### Loan -- Invalid Input

- "This is a bank. We do things right. Now you try."
- "Confusing the clerks will not help your credit rating."
- "Next time, try typing slowly and carefully, one key at a time."
- "Oh! You've invented a new number system. Can you translate it please?"
- "Get lost bum."

#### Loan -- No Function Selected

- "And just exactly what did you want today."
- "Are you borrowing, or repaying?"
- "Hey snotface. Look carefully at the screen. See those buttons labelled borrow and repay? Push one."
- "Please, please select a function."
- "Frankly my dear, I don't give a damn."

#### Loan -- Insufficient Repayment Funds

- "We admire your zeal. But you really have to have the money before you can pay it back."
- "Good intentions aren't worth snarf droppings. Now pay what you can and get out."
- "Forgot to balance the check book again eh? You don't have the cash do you?"
- "Do this again, and we might just send someone around to break your legs. You don't got enough dough."
- "Promises, promises. I wish you could pay me that much, but you can't."

#### Overseer -- No Function Selected

- "A fine personnel director you would make. You don't even know if you are hiring or firing."
- "Hire or fire. Hey, that rhymes!"
- "Hire or fire? Oscar Meyer. Bad rear tire. Fire the buyer. bow to the sire. A funeral pyre."
- "There are these little buttons marked hire or fire. It would help if you would push one."
- "Quit wasting my time."

#### Overseer -- Fractional Input

- "How exactly do I deal with fractions? Dismemberment?"
- "That is likely to be a bloody operation."
- "Try again please. We like our people whole."
- "Right."
- "You are missing the concept. People can't be usefully divided."

#### Overseer -- Firing More Than Employed

- "You can't fire that many."
- "I can sympathize with your intention. But you can't fire more than you have."
- "Enthusiastic aren't we. But firing more than you have doesn't work."
- "You don't have that many."
- "Get lost."

#### Planting -- Invalid Input

- "We are planting wheat, not alphabet soup."
- "()(*$#+qopi21-3u."
- "aaaaaaaaaa oooooooooo help me."
- "Try typing normally for a change."
- "Digits please."

#### Planting -- Negative Input

- "Negative wheat. Grows down eh?"
- "I have this vision of whole fields sinking down into little seeds."
- "I am getting tired of this joke."
- "Scram."
- "Ok, it was a fun experiment. But it's over now. Can we get back to the game?"

#### Pyramid Quota -- Invalid Input

- "Stone dust has gotten into your fingers. Try again please."
- "Excuse me, I have to answer the phone."
- "Aye yi yi. Please try that one again."
- "I'm mad as hell, and I'm not gonna take it anymore."
- "Learn how to type. Please."

#### Pyramid Quota -- Negative Input

- "Negative signs are not permitted here. Please correct the situation immediately."
- "Oh damn. If you can't type, don't play."
- "I am getting very very angry. Now this time do it right!"
- "OK, that's it. I've had it. Get out of here and don't ever come back."
- "oooooo oooooooooo oo oooo oo oo oooo ooooooooo."

#### Negative Stones

- "Negative stones? AH you want me to remove stones!"
- "Do negative stones have negative mass? Perhaps they fall up."
- "Are you talking about antimatter? I wouldn't build a pyramid out of that."
- "I'm not putting those stones back! Never!"

#### Manure Spread -- Invalid Input

- "oooooooo oooooooo oo oooooo o oooo oooo o"
- "I want to see digits. Digits damn you. Digits."
- "We are talking about fertilizer, measured in tons. Now give me an intelligent answer."
- "Give me a number please."
- "OK, no more playing around ok?"

#### Manure Spread -- Negative Input

- "Negative fertilizer? Hey, maybe that's food!"
- "Negative fertilizer makes crops grow worse."
- "ha ha ha ha ha ha ha. Ah ha ha ha."
- "Seriously, you have to enter a positive number. (dork)"
- "Ratsifrats! Please type more carefully."

#### Feed Rate -- Invalid Input

- "Feed rates are odd things. They have to be numeric."
- "Are you using some code or something?"
- "Yikes! What a lousy typist. I need numbers!"
- "I realize that it's not always easy to keep your mind on things, but try ok?"
- "If you don't know what's wrong, I'm certainly not going to tell you!"

#### Generic Numeric Input Error

- "Kiss me you fool."
- "Did you go to school. Do you know how to write numbers? Try."
- "I want a number here. OK?"
- "Are you a newcomer to the human race? Don't you know the world is run by numbers?"
- "Either write a number or get out. You smell."
- "Please, let me help. You must use one of the numbers 0, 1, 2, 3, 4, 5, 6, 7, 8 or 9. OK?"

### A.17 Contract Player Names

- King HamuNam
- Regent Karada del Nor
- Emperor Falthazzar
- Jor-El of Krypton
- Shah Sataj Kampooli
- Akmad na Gandar
- Baron Tanjou d'Aranom
- Gort of Grunthos
- Prince Guelar of Xaptu
- Ganzaola pu

### A.18 Quit/Save Prompts

- "Giving up so soon? Do you want to save the game?"
- "Ah yes, on to another game. Shall I save this one first?"
- "Hey, I've got an idea, let's save this game first."
- "If you want me to save this game, push the YES button. (Simple huh?)"
- "Do you wanna keep this one around for awhile?"
- "I'll never forgive you for leaving me this way. The only way you can make it up to me is by saving your game now."
- "So, deserting me eh? Are you going to save your game just in case you want to start it up again?"
- "You're quitting? I don't believe it! How can you do this to me. Well, you are going to save your game aren't you?"
- "It's quittin' time. Would you like to save your game?"
- "Chicken! Well at least you ought to save your game."
- "If you don't save it, you'll lose it. Wanna save it?"
- "Time for a refreshing drink. Save game first?"
- "Is there something good on TV or what. Shall I save the game?"
- "I'm sure going to miss our little talks. Why don't you save your game now?"
- "OK, I know when I'm not wanted, I'll go. But before I do, let me ask you one last question. One last query for old time's sake. Do you want to save your game?"
- "Couldn't take the pace eh? Well that's all right. Lots of people drop out when the going gets tough. But perhaps you should save your game for when you are feeling stronger."
- "Quitter! At least save your game."
- "It was nice while it lasted. Do you want to save this game?"
- "All good things must come to an end. Why not save your game now?"
- "Well of all the dirty tricks. Getting out before I could beat you. Well, you'd better save your game."
- "They don't let real kings just quit like that you know. Do you want to save your game."
- "It might be a good idea to save your game now."
- "I L B C N U. Save game?"

---

## Appendix A: Character Portraits

The four neighbor character portraits were extracted from the original Pharaoh
1.2 resource fork (PICT v1 resources). The original 1-bit bitmaps have been
scaled up 150% with bilinear interpolation for anti-aliased smoothing.

| File | Original | Scaled | Description |
|------|----------|--------|-------------|
| `resources/faces/man1.png` | 76 x 94 | 114 x 141 | Afro man with goatee and open jacket |
| `resources/faces/man2.png` | 71 x 82 | 106 x 123 | Bearded man in plaid shirt |
| `resources/faces/man3.png` | 44 x 68 | 66 x 102 | Man with goatee in bow tie and suit |
| `resources/faces/man4.png` | 37 x 60 | 56 x 90 | Man with glasses and necktie |

### Man 1 — Afro man
![Man 1](resources/faces/man1.png)

A man with a large afro hairstyle and goatee, wearing an open jacket with
decorative buttons. His expressive, friendly appearance suits the Good Guy or
Village Idiot roles.

### Man 2 — Bearded man
![Man 2](resources/faces/man2.png)

A broad-shouldered man with a full beard and heavy brow, wearing a plaid
flannel shirt. His stern, no-nonsense look fits the Bad Guy or Banker roles.

### Man 3 — Bow-tie man
![Man 3](resources/faces/man3.png)

A dapper man with a pointed goatee, wearing a bow tie and formal suit jacket.
His sharp, sly expression suits the Bad Guy or Village Idiot roles.

### Man 4 — Glasses man
![Man 4](resources/faces/man4.png)

A clean-cut man with glasses and a necktie, carrying a professional demeanor.
His conservative, bookish appearance is a natural fit for the Banker role.

### Usage

At game startup the four portraits are loaded and randomly assigned to the
four neighbor personality roles (Good Guy, Bad Guy, Village Idiot, Banker) so
that each game has a different face-to-role mapping. When a neighbor delivers
a message — whether advice, contract offers, idle chatter, or banker
warnings — the message appears in a dialog box overlaying the game screen with
that neighbor's portrait displayed alongside the text. The message is also
spoken aloud via text-to-speech using voice settings (rate and pitch) specific
to that neighbor.
