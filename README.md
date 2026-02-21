# Pharaoh

Originally written in C for the classic Macintosh by Robert C. Martin
(circa 1988), this is a ground-up rewrite in Clojure, guided by the
original source code and a detailed specification extracted from it.

What follows is an updated version of the original Pharaoh Manual 1.0.

---

## User Manual

You are the offspring of a famous noble. Your father is revered
throughout the land for his financial and military prowess. He is very
wealthy. But he doesn't think much of you. As far as he is concerned
you are never going to amount to much. One day, in a fit of
frustration, he throws you out of his house with nothing but your
clothing. He has written you off as a certain failure. Is he right?

Welcome to the game of Pharaoh. Pharaoh is a simulation game which
gives you a chance to become the Pharaoh of all Egypt (and possibly
the whole world). By applying common sense and experience, you attempt
to make a vast agricultural community thrive and grow. You make and
break deals with other kingdoms, fight wars, manage revolts, and
monitor the day to day operation of your farming communities.

You start with nothing, but the banks remember your heritage and are
willing to extend you a small loan. You use the money they lend you to
build the seeds of a huge empire. Over the years you expand your
empire, gaining land, wealth, and power. And when your empire has
reached the size and power which you deem necessary, you attempt to
erect a monument of such vast enormity that you will be remembered for
thousands of years to come.

The going is never easy. There are many obstacles in your way.
Famines, floods, aggressive neighbors, even terrorists. But with
skillful management, and some clever strategies, you can prevail and
grow. And if you do, you will become the greatest ruler of all time.

### Overview

**Scenario.** You begin the game as a disinherited noble. Your father,
a man of great wealth and power, has deemed you to be a ne'er do
well, and has thrown you out into the inhospitalities of the world,
without a shekel to your name. But the bank knows your heritage, and
is willing to extend to you a small, unsecured, loan.

**Object.** With the money you are able to borrow from the bank, you
must build a kingdom of great power. You must buy land, slaves, oxen
and horses. You must hire mercenaries and overseers to guard your
kingdom, and manage your slaves. You must plant your fields, and reap
your harvests, and make a profit. You must grow.

And when you have grown to the size you deem enough, you invest your
money, property, and strength into the construction of a vast pyramid.

Your goal is to complete this pyramid within 40 years. It's not easy.

**Obstacles.** You will face many obstacles along the way towards
building your pyramid. Famines, floods, wars, and rumours of wars,
plagues, pestilence, and even fire from heaven.

Still, if you are careful, and clever, you can overcome these foes,
and grow your kingdom to cover the planet if you wish.

### Commodities

As pharaoh, you make your kingdom grow by wisely managing the
commodities that you own. You buy them, sell them, and produce them.
Many of the commodities can be used as cash crops, depending on the
type of business that you, as pharaoh, wish to invest in.

**Wheat.** This is the product of your land. Your slaves and oxen must
plant wheat, fertilize the fields, cultivate and harvest the crop.
Then you can use the resulting wheat as further seed, and food for
your oxen, horses, and slaves. Wheat is bought and sold by the
bushel. Wheat is the most common cash crop. The traditional way to
play pharaoh is to produce and sell wheat on the open market.

**Slaves.** Slaves represent your primary motive force. Slaves do the
work! It is your slaves which plant your seed, and spread your
fertilizer. They feed and tend your oxen and horses. They cultivate
and harvest your fields. And they build your pyramid. Without them,
nothing can be done. Slaves are also a common cash crop. Under the
right circumstances, slaves reproduce at an alarming rate, and can
therefore become a lucrative trading business.

**Oxen.** As beasts of burden, oxen help the slaves immensely. Slaves
can do many times as much work if they are assisted by oxen. But oxen
eat a lot, and don't reproduce quickly. You must find the best ratio
of slaves to oxen.

**Horses.** Horses are the vehicles for your overseers. They make the
overseers' jobs much easier by allowing them to get around quickly
and efficiently. But does every overseer need a horse? Horses eat a
lot, and reproduce slowly.

**Manure.** Manure is produced by your slaves and livestock in
proportion to the amount of food that they eat. It is bought and sold
by the ton and is the fertilizer that you spread on your fields. You
must decide how much to spread on the acres that you plant each month.

**Land.** Land is listed by the acre. This is where you plant your
seed, and where your crops grow. Each acre of land can support the
planting of only about 20 bushels of seed, and no more. How much
wheat that seed will produce depends on the fertilizer, weather, and
the efficiency of your slaves.

### Planting and Reaping

In order to grow wheat, you must plant it. Your land starts out
fallow, which means that it has lain unplanted for a month or more.
You must specify the total number of acres that you intend to plant
each month. Every time a new month passes, you will see the land that
you planted move into the *planted* category. The month after, it
will move into the *growing* category. Then it will move into the
*ripe* category. The next month, it will be harvested, and will move
back into the *fallow* category.

You must also specify the total number of tons of manure you want the
slaves to spread each month on the land you intend to plant. Too
little fertilizer will cause very poor crops, but too much will
completely kill the wheat. It is up to you to figure out the proper
amount to spread.

June and July are the best months for planting wheat. The harvest of
this crop will be the biggest. By the same token, January plantings
will often be very bad. (Make hay while the sun shines.)

### Feeding Livestock

You must specify how many bushels of wheat you want to feed each of
your slaves, oxen and horses each month. If you feed them too little,
they will sicken and die. If you feed them too much, they can get
lazy. In general, the more work you make your slaves do, the more
food they will require.

Remember, the food you feed your living inventories is deducted from
your store of wheat. Make sure you keep enough wheat around, both for
food and for planting.

### Overseers

Overseers are the managers of your slaves, and the mercenaries which
protect your grounds. They represent both your middle management, and
your military. They are employees, and as such you must pay them a
monthly salary in gold. (And occasionally deal with their unions.)

Without overseers to drive them on, your slaves will do little if any
work. Also, if you don't have enough overseers, enemy armies will
plunder and pillage you into bankruptcy.

You must carefully manage the stress level of your overseers. If you
make unreasonable requests of them, or have exaggerated expectations
of what they can drive the slaves to do, you will find that they get
quite nervous about their jobs. Nervous overseers tend to take their
frustrations out on the slaves, usually with a whip.

### Loans

Probably your first action in the game will be to approach the bank
for a loan. The bank is an enigmatic institution (what bank isn't?)
which seems to have an infinite supply of money. However it is quite
stingy about loaning any of that money to you.

At first you will find that the bank trusts you for a "fairly
substantial" loan. As you grow, the bank will allow you to borrow
more and more money. But you will find that the bank is very concerned
about your debt to asset ratio. In fact, they will foreclose on you
if you let it get too bad.

### Market Prices

You will notice that the prices for the various commodities change
from month to month. Although this may seem like a random fluctuation,
there is in fact an underlying cause. Although the market prices are
indeed subject to random, month to month variations, they are also
subject to the classical laws of supply and demand. If you produce a
lot of a certain commodity, you will be increasing the world supply,
and that will in turn cause the price to drop.

Of course to have a visible effect on the price, you will have to be
dealing with fairly large quantities.

### Work

All activity on your estate requires work. There are 4 monthly work
quotas which you must specify. They are: the number of acres to plant
each month, the number of tons of manure to spread each month, the
number of stones to add to your pyramid each month, and the amount of
food to be distributed to the livestock each month. You must manage
these quotas, making sure that the goals you set are both ambitious
enough to assure your growth, and conservative enough to be realistic.

Your overseers are paid to make sure that the quotas you have
specified get met. If they fail to meet these quotas, they get nervous
about their jobs and will beat the slaves. This will increase the
motivation of the slaves and they will work even harder. But beatings
take a health toll.

It is possible to get the overseers to the point that they are
beating the slaves to great excess. This will cause the slaves to do
a tremendous amount of work for a month or two, while they sicken and
suddenly die on you. It is very easy for this to get out of control,
and you must watch it very closely.

When your slaves fail to get all their work done, it is assumed that
they did all the work proportionally. i.e. if they did 90% of the
required work, then they only planted 90% of the wheat, harvested 90%
of the crops, fed 90% of the wheat to the oxen, etc.

### Running the Simulation

The game consists of a set of one month turns. At the beginning of
each month, you buy and sell commodities, hire or fire overseers, and
adjust planting, spreading, and feeding quotas. Then you *run* for a
month.

The simulation then figures out what happened that month, and displays
the results on the screen.

### Events

Most months are uneventful. Your slaves reap, sow, and have babies.
Your oxen and horses produce lots of manure. And your land grows
wheat. But every once in a while, something happens to disturb the
serenity of the scene.

There are all kinds of events that can occur. Acts of God, acts of
mobs, plagues, wars, uprisings, etc. Each event will challenge the
security of your estate and the wisdom of your strategies. Never
trust your life to the status quo.

### Contracts

There are contracts being offered by other kings in the area. These
contracts fall outside of the usual laws of supply and demand. So even
if you would not be able to buy 1000 acres from the market, you can
contract to buy them from a neighboring king.

Sometimes these contracts can be real bargains, and sometimes they are
horrible rip-offs. You must be the judge. Also, there are penalties
involved. If you fail to meet your contracts, you will be assessed a
percentage of the value of the contract each month until your
commitment is fulfilled.

In general, contracts are an interesting way to make money, and to
influence the market prices. It is possible to procure large amounts
of a commodity from a neighboring king, and then dump it into your
economy, creating a glut.

### Your Neighbors

Like all good pharaohs, you have some neighbors. These are people
just like you, trying to run their estates. You have 4 such
neighbors. You will get to know their distinctive faces and voices.

During the course of any particular game, the personalities of these
players remains constant. But at the beginning of each new game they
are shuffled. It therefore becomes one of the puzzles of the game to
figure out which face belongs to which personality.

**The Good Guy.** The good guy is pretty reliable. He always tries to
give you good advice. If he sees that your slaves are underfed, he
will tell you that they look bad. If he notices that your crops are
poor, he will suggest that you spread more manure.

**The Bad Guy.** The bad guy is working against you all the time.
Practically all the advice he gives you is bad. He will tell you how
wonderful your slaves look, just before they keel over and die from
overwork. He will commend you on your great standing at the bank,
just as they foreclose.

**The Village Idiot.** This guy's advice is completely undependable.
Sometimes it's right, and sometimes it's wrong. There is no telling.

**The Banker.** The banker is the stooge that the bank sends around
to bother you about making loan payments. The more worried the bank
is, the more often he comes.

**Idle messages.** Your neighbors will try to wake you up if you go
to sleep while playing the game. Any time there is a period of
significant inactivity, these helpful little guys will barge right in
with encouraging little slogans or pep talks.

**Chats.** Sometimes your neighbors just pop in for a talk. It is
these particular chats which often contain advice or hints as to your
status. But remember, don't believe everything they tell you. One of
those guys is an idiot, and the other is out to get you.

### Building the Pyramid

You build the pyramid by setting the monthly stone quota. This
specifies the number of stones to be laid per month. The slaves lay
the stones, and the activity requires work. Just how many slaves it
takes to lay stones is a variable you will have to discover.

Pyramid stones are cubes of granite 6 feet on a side. They are carved
from a nearby quarry and carted to the pyramid site. From there they
must be fitted into the pyramid structure itself. It takes a lot of
work and equipment to do all this. Work is provided by the slaves,
and the cost is automatically deducted from your treasury. Make sure
you keep a close eye on these variables, they can easily surprise you.

As the pyramid grows, you will see it grow on the screen. Piece by
piece you will see it being assembled.

### What You Can't See Can Hurt You

There are quite a few invisible processes which go on behind the
screen. It is not possible to directly observe the state of these
processes, so it must be inferred. It is very important to keep close
track of them, because if they get out of control you can rapidly
lose the game.

**Health.** The health of all your livestock is monitored. If they are
overworked, underfed, or mistreated, their health is adversely
affected. If a sick slave or horse is left to recuperate, with little
work to do, and plenty of food to eat, it will heal quite rapidly.
Bad health affects work output, reproduction rate, and the final
selling price (the buyers aren't dummies).

**Stress.** When work gets done on time, the overseers feel confident
in their jobs. But when the work schedule starts to slip, they get
nervous. This will cause them to beat the slaves, which has the
rewarding effect of getting them to do more work.

It is very important to manage the stress level of your overseers,
otherwise they will quickly kill all your slaves. This is very tricky
to manage, since there is no direct indicator of their stress (except
for an occasional chat from a neighbor).

Just remember, whenever a month goes by where all the work did not
get done, the overseers will be a bit nervous that month. If two or
three such months go by, then the overseers will be that much more
nervous. The more nervous they are, the more they will mistreat the
slaves, and the sicker the slaves will become. It takes several good
productive months for overseers to completely relax and regain their
composure.

**Economy.** There are interesting processes underlying the market
prices. Behind the scenes there are other estate owners producing and
consuming a global supply of commodities.

When you become a major producer of a commodity, you start to compete
with these other producers. An oversupply will result, and the price
will begin to fall. Now it's a race. The longer you can maintain your
production in the face of falling prices, the more of your
competition you will displace, until you have carved yourself out a
nice little niche.

On the other hand, if you become a major consumer of a certain
commodity, you will add to the demand for the commodity. This will
result in an under-supply, and the price will begin to rise. Rising
prices attract new competitors to the arena, and they will begin to
produce more and more of the commodity until your demand has been
satisfied. Then the price will stabilize at its new higher level.

The market can only bear so much stress. If you attempt to sell a
large enough quantity of a commodity, the market will refuse to take
it. The same goes for attempting to buy too much -- the market can
run out.

The only cue you have to these economic factors of supply, demand,
and current global production capacity, is the price of the
commodity, and the comments made by the market keepers when you try
to buy or sell. If you are good, you will be able to predict
fluctuations, and finally even control them to your benefit.

**Credit rating.** The bank has a varying opinion of your ability to
repay any outstanding debts. As long as you make frequent payments,
the bank will love you. But if you forget to pay for a few months, or
worse: run out of money at the end of a month, the bank will change
its opinion.

This all has to do with your credit rating. You can never see your
credit rating, but it is there.

If your credit rating is high, then the bank will feel good about
giving you big loans, and will allow relatively severe debt to asset
ratios. But if your credit rating is poor, they will charge you
outlandish interest rates, and refuse to give you even tiny little
loans. They will also yell at you an awful lot, and may even close
you down if your debt gets too out of line.

### The Screen

The pharaoh screen is divided into sections. Each section displays
the data for a certain category of information. The sections are
surrounded by rectangles and titled. Many of the cells are sensitive
to mouse clicks.

**Date.** Each game starts in January of Year 1.

**Gold.** Current gold, last month's gold, and the percentage change.
Click to borrow or repay a loan.

**Loan.** Outstanding balance, monthly interest rate, and credit
limit. Click to borrow or repay.

**Commodities.** Current holdings, last month's holdings, and delta
percent for wheat, manure, slaves, horses, and oxen. Click to buy or
sell.

**Land.** Acres in each state: fallow, planted, growing, ripe, and
total. Selling planted, growing, or ripe land destroys the crops on
it.

**Prices.** Current market prices. These represent how much you must
pay to purchase, and the maximum you will receive if you sell. The
selling price can be less than the market price if the quality of the
merchandise is low (e.g. diseased horses).

**Feed Rates.** How many bushels of wheat you are feeding each slave,
ox, and horse per month. Click to adjust.

**Spread & Plant.** How many acres you are planting and how many tons
of manure you are spreading. Click to adjust.

**Overseers.** How many overseers are employed and the monthly salary
per overseer. Click to hire, fire, or obtain.

**Pending Contracts.** The contracts you are committed to.

**Pyramid.** A picture of the pyramid, its stone count, height, and
the monthly stone quota. Click to adjust the quota.

### Controls

#### Keyboard

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

#### Mouse

The game can be driven almost entirely by the mouse. To adjust a
parameter, simply click on its current value -- to buy wheat, click
on the current amount of wheat. Click the Run button to advance a
month, or Quit to exit.

#### Buying and Selling

When you select a commodity to buy or sell, a dialog box appears.
Select the function by clicking the appropriate radio button, or type
the respective character: `b` = buy, `s` = sell, `k` = keep/acquire.
Then enter the amount and click OK.

Keep and Acquire mean that you want the system to buy or sell whatever
is necessary so that when the transaction is over you will own the
specified amount. i.e., if you think you need 534 slaves total, then
acquire 534.

#### Hiring and Firing

When you select the overseer function, you must specify whether you
want to hire, fire, or obtain overseers. Click the appropriate radio
button, or type: `h` = hire, `f` = fire, `o` = obtain.

Obtain means that you wish to hire or fire the appropriate number in
order to get your current head count to the specified number.

#### Contracts

To browse the currently offered contracts, press `c` or click on the
contracts section. If you see one you like, select it and confirm
your commitment. The contract will appear in your list of pending
contracts.

There is no way to escape from a contract once you have committed it.

Save and load are available from the File menu. Games are saved to
the `saves/` directory by default.

### Skill Levels

**Easy:** You win by building a pyramid 100 ft tall. The bank will
never reduce your credit limit lower than 5,000,000 gold pieces, and
the market prices start out in your overwhelming favor. The game
starts with commodities, overseers, feed rates, and planting already
configured. This does not mean the easy level is easy to play. It
should take you some time to get the hang of how to win. But winning
is possible if you develop the proper business strategy, and are
careful. At this level, your wheat production will most likely never
get high enough to have serious economic effects.

**Moderate:** Your pyramid must reach 300 ft. The bank can reduce
your credit limit all the way to 500,000, and the market prices start
out weak at best. Great care must be taken at this level; it is very
easy to get the business out of balance. Towards the end of the game,
you will probably find that you have to make a transition in the way
you do business. Your wheat production will be so high that the price
of wheat will come crashing down and make your farming business
unprofitable. You can solve this problem by dealing in other
commodities, or relying on external contracts.

**Hard:** You must complete a pyramid of 1,000 ft. This is hard! The
bank may deign to give you a piddling 50,000 gold pieces, and the
market prices are decidedly poor. The rule of this game is flexibility
and changability. You will no doubt be forced to change the
composition of your business several times. Your production levels
and purchasing powers will have to get so high that they can exert
overwhelming effects on the local economy. You will be able to exert
vast control over the market prices. The hard part is keeping your
business in balance, and keeping the bank happy.

### Ownership Costs

Every month you pay a maintenance cost for your holdings. If your
gold goes negative before the emergency loan tops it up, your
overseers quit and demand a raise.

| Asset  | Cost per unit per month |
|--------|------------------------|
| Land   | 100 gold / acre        |
| Slaves | 10 gold / slave        |
| Horses | 5 gold / horse         |
| Oxen   | 3 gold / ox            |

These costs are multiplied by a random factor averaging ~1.0 each
month. In addition you pay overseer salaries (default 300 gold each
per month) and loan interest (0.5% of outstanding loan per month).

### Hints

- Be aware of the workload of your slaves. It depends upon a lot of
  factors, among them: the amount of livestock you have, the amount of
  land you are planting, the amount of fertilizer you are spreading,
  the number of pyramid stones you are laying, random events such as
  wars and revolts, and the amount of wheat that needs harvesting.

- You will know if your slaves are not meeting their workload quota if
  they fail to plant everything you asked for, and you know you had
  enough land and wheat for the planting. In this case, the slaves'
  work will be spread evenly over all their activities. If they only
  managed to plant half the wheat you demanded, then they were only
  able to feed the livestock half their food, harvest half the wheat,
  and lay half the pyramid stones.

- If your slaves are working fine through the winter, spring, and
  summer, but fail to meet quota during the fall, it may be that the
  fall harvests of the summer plantings (the really big harvests) are
  too much for the slaves.

- Watch the prices. Especially watch the price of your cash crop. If
  you see it falling rapidly, you are probably overproducing. The
  demand for products in your local economy increases with time. If
  you produce at a rate which is in excess of this demand, then the
  price will plummet as the inventories grow. If this happens to you,
  then consider changing your cash crop, or selling outside your local
  economy. Don't let the prices get out of control -- you may find
  that you have devalued the worth of your estate to the point that
  the bank will not be able to find sufficient collateral for a loan.

---

## Running

Requires Java 11+ and [Clojure CLI tools](https://clojure.org/guides/install_clojure).

```bash
clojure -M:run
```

Or use the shell script:

```bash
./run.sh
```

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
