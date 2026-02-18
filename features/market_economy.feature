Feature: Market Economy
  Commodity prices fluctuate based on inflation and supply/demand dynamics.
  The world economy has its own production and consumption that the player
  competes with. Oversupply drives prices down; undersupply drives them up.

  Background:
    Given the game is running

  # -----------------------------------------------------------
  # Monthly Price Inflation
  # -----------------------------------------------------------

  Scenario: Prices adjust by inflation each month
    Given the current inflation rate is 0.02
    When monthly prices are updated
    Then each commodity price is multiplied by approximately (1 + inflation)
    And the adjustment has slight random variance of about 2%
    # Applied to: wheat, land, horse, oxen, slave, and manure prices,
    # as well as overseer pay and interest rate

  Scenario: Inflation rate takes a random walk
    Given the current inflation rate is 0.02
    When inflation is updated
    Then inflation shifts by a small normally-distributed random amount (mean 0, deviation 0.001)
    And inflation can drift positive or negative over time

  # -----------------------------------------------------------
  # Supply and Demand
  # -----------------------------------------------------------

  Scenario: Demand grows over time
    Given the world growth rate is 0.10
    And current wheat demand is 10000
    When a month is simulated
    Then wheat demand grows by 10000 * (0.10 / 12)

  Scenario: Undersupply causes price and production to rise
    Given monthly demand consumes 80% and supply goes negative
    When the production cycle adjusts
    Then price increases by a random factor between 1.0 and 1.2
    And production increases by a random factor between 1.0 and 1.1

  Scenario: Remaining demand consumed and oversupply check
    Given the remaining 20% of monthly demand is consumed
    And supply is still positive after all consumption
    When the production cycle adjusts
    Then price decreases by a random factor between 0.8 and 1.0
    And production decreases by a random factor between 0.9 and 1.0

  Scenario: Production fluctuates randomly
    When the production cycle adjusts
    Then production varies by a random factor between 0.95 and 1.05
    And supply increases by production / 12

  Scenario Outline: Supply/demand cycle for all commodities
    Given the commodity "<commodity>" has supply, demand, and production
    When the production cycle runs
    Then the supply, demand, production, and price for "<commodity>" are adjusted

    Examples:
      | commodity |
      | wheat     |
      | land      |
      | manure    |
      | slaves    |
      | horses    |
      | oxen      |

  # -----------------------------------------------------------
  # Player Impact on Economy
  # -----------------------------------------------------------

  Scenario: Large wheat sales increase supply and lower price
    Given the player is producing and selling large quantities of wheat
    When the wheat supply grows above demand
    Then the wheat price begins to fall
    And global wheat production contracts as competitors exit

  Scenario: Large wheat purchases increase demand and raise price
    Given the player is buying large quantities of wheat
    When wheat demand increases
    Then the wheat price begins to rise
    And global wheat production increases to meet demand

  # -----------------------------------------------------------
  # Monthly Ownership Costs
  # -----------------------------------------------------------

  Scenario: Monthly ownership costs deducted from gold
    Given the player has 1000 acres of total land
    And 200 slaves, 50 horses, and 100 oxen
    When monthly costs are calculated
    Then base cost = 1000 * 100 + 200 * 10 + 50 * 5 + 100 * 3 = 102550
    And actual cost = base cost * a random factor (at least 30% of base, up to 100%)
    And gold decreases by the actual cost

  # -----------------------------------------------------------
  # Net Worth Calculation
  # -----------------------------------------------------------

  Scenario: Net worth computed from all assets
    Given the player has various commodities and gold
    When net worth is calculated
    Then net worth = slaves * slave price + oxen * oxen price + horses * horse price
    And net worth += total land * land price + manure * manure price + wheat * wheat price + gold
    And debt-to-asset ratio = loan / net worth (before subtracting loan)
    And net worth -= loan
