Feature: Game Setup
  The player begins as a disinherited noble with no assets.
  They must borrow from the bank to start building an empire.
  The game offers three difficulty levels that set pyramid size,
  credit limits, market prices, and world growth rate.

  Background:
    Given the game has been initialized
    And the date is January of year 1

  # -----------------------------------------------------------
  # Difficulty Levels
  # -----------------------------------------------------------

  Scenario: Easy difficulty settings
    When the player selects "Easy" difficulty
    Then the pyramid base should be 115.47 stones
    And the pyramid target height should be approximately 100 feet
    And the credit limit should be 5000000 gold
    And the credit lower bound should be 5000000 gold
    And the world growth rate should be 0.15
    And the land price should be 1000
    And the wheat price should be 10
    And the slave price should be 1000

  Scenario: Normal difficulty settings
    When the player selects "Normal" difficulty
    Then the pyramid base should be 346.41 stones
    And the pyramid target height should be approximately 300 feet
    And the credit limit should be 500000 gold
    And the credit lower bound should be 500000 gold
    And the world growth rate should be 0.10
    And the land price should be 5000
    And the wheat price should be 8
    And the slave price should be 800

  Scenario: Hard difficulty settings
    When the player selects "Hard" difficulty
    Then the pyramid base should be 1154.7 stones
    And the pyramid target height should be approximately 1000 feet
    And default credit limits apply
    And default market prices apply

  Scenario: Unlicensed players are forced to Easy
    Given the player has not purchased a license
    When the game starts
    Then the difficulty is set to "Easy"
    And saving the game is disabled

  # -----------------------------------------------------------
  # Starting Conditions
  # -----------------------------------------------------------

  Scenario: Player starts with no assets
    When the game begins
    Then the player should have 0 gold
    And the player should have 0 slaves
    And the player should have 0 oxen
    And the player should have 0 horses
    And the player should have 0 wheat
    And the player should have 0 manure
    And the player should have 0 land
    And the player should have 0 overseers
    And the loan balance should be 0
    And the pyramid stones should be 0
    And the pyramid height should be 0

  Scenario: Player must borrow to begin
    Given the player has 0 gold
    When the player borrows from the bank
    Then the player should receive gold
    And the loan balance should increase by the borrowed amount
    And the player can now buy commodities

  # -----------------------------------------------------------
  # Monthly Turn Structure
  # -----------------------------------------------------------

  Scenario: Monthly turn sequence
    Given the game is running
    When the player clicks "Run"
    Then the previous month values are recorded for display
    And there is a 1-in-8 chance of a random event
    And the monthly simulation runs
    And contract offers are refreshed
    And the screen is updated with new values

  Scenario: Random event probability per turn
    Given the game is running
    When a month is simulated
    Then a random event occurs with probability 12.5%

  # -----------------------------------------------------------
  # Time Progression
  # -----------------------------------------------------------

  Scenario: Month advances each turn
    Given the current month is 5
    And the current year is 3
    When a month is simulated
    Then the month should be 6
    And the year should be 3

  Scenario: Year rolls over after December
    Given the current month is 12
    And the current year is 3
    When a month is simulated
    Then the month should be 1
    And the year should be 4
