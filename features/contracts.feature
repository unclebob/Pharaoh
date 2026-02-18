Feature: Contracts
  Neighboring kings offer contracts to buy or sell commodities.
  Contracts operate outside the normal supply/demand market.
  They have fixed amounts, prices, and durations.
  Failure to fulfill contracts incurs penalties.

  Background:
    Given the game is running

  # -----------------------------------------------------------
  # Contract Structure
  # -----------------------------------------------------------

  Scenario: A contract has required attributes
    Given a contract is generated
    Then it has a type (BUY or SELL)
    And it has a counterparty (one of 10 players)
    And it has a commodity (wheat, slaves, oxen, horses, manure, or land)
    And it has an amount
    And it has a price
    And it has a duration in months

  # -----------------------------------------------------------
  # Contract Generation
  # -----------------------------------------------------------

  Scenario: Contract parameters are randomly generated
    When a new contract is created
    Then the counterparty and commodity are chosen to avoid duplicates
    And the type is BUY or SELL with equal probability
    And the amount is normally distributed around 6x the current amount (deviation 2x), with a floor of 200000/price
    And the price = amount * market value * a random premium (0.4 base + exponentially distributed bonus with mean 0.6)
    And the duration is uniformly random between 12 and 36 months

  Scenario: No duplicate counterparty-commodity pairs
    Given player "Ramses" already has a wheat contract
    When a new contract is generated
    Then it will not assign another wheat contract to "Ramses"

  # -----------------------------------------------------------
  # Monthly Contract Offers
  # -----------------------------------------------------------

  Scenario: Contract offers refresh each month
    Given there are 15 offer slots
    When a new month begins
    Then empty slots are filled with new contracts
    And existing contracts with duration <= 8 are replaced
    And 20% of remaining contracts are randomly replaced
    And surviving contracts are aged (duration decremented by 1)

  Scenario: BUY contract price increases with age
    Given an active BUY contract offer
    When the contract ages for one month
    Then the price increases by a random factor between 1% and 10%

  Scenario: SELL contract price decreases with age
    Given an active SELL contract offer
    When the contract ages for one month
    Then the price decreases by a random factor between 1% and 10%

  # -----------------------------------------------------------
  # Accepting Contracts
  # -----------------------------------------------------------

  Scenario: Player accepts a contract
    Given a contract offers to BUY 500 slaves for 100000 gold in 24 months
    When the player accepts the contract
    Then the contract moves from offers to pending
    And the offer slot becomes inactive
    And the pending contract list shows the commitment

  Scenario: Cannot accept more than 10 pending contracts
    Given the player has 10 pending contracts
    When the player tries to accept another contract
    Then a message says "You have too many contracts already"

  Scenario: Contracts are irrevocable
    Given the player has accepted a contract
    Then there is no way to cancel the commitment

  # -----------------------------------------------------------
  # Contract Fulfillment
  # -----------------------------------------------------------

  Scenario: BUY contract fulfilled successfully
    Given a pending BUY contract for 500 oxen at 50000 gold due this month
    And the player has 500 or more oxen
    And the counterparty can pay in full
    When the contract comes due
    Then the player's oxen decrease by 500
    And the player receives 50000 gold
    And the contract is marked inactive

  Scenario: BUY contract - player has insufficient goods
    Given a pending BUY contract for 500 oxen
    And the player only has 300 oxen
    When the contract comes due
    Then the player delivers all 300 oxen
    And the player receives payment for 300 oxen at the per-unit price
    And a penalty of 10% of the remaining contract value is deducted
    And the player's oxen are set to 0
    And the contract amount and price are reduced proportionally

  Scenario: BUY contract - counterparty cannot pay full amount
    Given a pending BUY contract
    And the counterparty's pay probability fails
    When the contract comes due
    Then the counterparty buys a reduced amount (50% to 95% of the contract, uniformly random)
    And the player receives payment for the reduced amount
    And a 10% bonus of remaining value is paid to the player
    And the contract is adjusted for the remainder

  # -----------------------------------------------------------
  # SELL Contracts
  # -----------------------------------------------------------

  Scenario: SELL contract fulfilled successfully
    Given a pending SELL contract for 1000 bushels of wheat at 20000 gold
    And the player has enough gold to pay
    And the counterparty can ship in full
    When the contract comes due
    Then the player pays 20000 gold
    And the player receives 1000 bushels of wheat
    And the contract is marked inactive

  Scenario: SELL contract - player cannot afford full purchase
    Given a pending SELL contract for 1000 wheat at 20000 gold
    And the player only has 15000 gold
    When the contract comes due
    Then a 10% penalty on total contract value is deducted from gold
    And the player buys as much as remaining gold allows
    And the contract is adjusted for the remainder

  Scenario: SELL contract - counterparty cannot ship full amount
    Given a pending SELL contract
    And the counterparty's ship probability fails
    When the contract comes due
    Then the counterparty ships a reduced amount (50% to 95%, uniformly random)
    And the player pays for the reduced amount
    And a 10% bonus of remaining value is paid to the player
    And the contract is adjusted for the remainder

  # -----------------------------------------------------------
  # Received Livestock Health
  # -----------------------------------------------------------

  Scenario: Livestock received via contract have 0.9 health
    Given the player receives 100 horses via a SELL contract
    And the player's current horses have health 0.7
    When the horses are added
    Then the blended health = (0.7 * existing + 0.9 * 100) / (existing + 100)

  # -----------------------------------------------------------
  # Contract Default
  # -----------------------------------------------------------

  Scenario: Counterparty defaults on contract
    Given a pending contract with a counterparty
    And the counterparty's default probability triggers
    When monthly contract progress is checked
    Then the contract is cancelled
    And the player receives a 5% cancellation penalty payment
    And a default notification is displayed

  # -----------------------------------------------------------
  # AI Player Personalities
  # -----------------------------------------------------------

  Scenario: Player personalities are generated at game start
    When the game initializes
    Then 10 contract players are created
    And each player has a name
    And each player's pay probability is the best of 2 random draws between 0.5 and 1.0
    And each player's ship probability is the best of 2 random draws between 0.5 and 1.0
    And each player's default probability is the best of 5 random draws between 0.95 and 1.0
