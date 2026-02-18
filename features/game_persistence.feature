Feature: Game Persistence
  The player can save the current game state to a file and restore it later.
  All game variables are preserved across save and restore cycles.

  Background:
    Given the game is running

  # -----------------------------------------------------------
  # Saving
  # -----------------------------------------------------------

  Scenario: Save the current game
    Given the player has been playing for several months
    When the player saves the game
    Then all game state is written to a file
    And the player can continue playing

  Scenario: Save prompts for a filename
    When the player chooses to save
    Then the player is prompted for a save file name

  Scenario: Save overwrites an existing file
    Given a save file already exists with that name
    When the player saves to the same filename
    Then the file is overwritten with the current state

  # -----------------------------------------------------------
  # Restoring
  # -----------------------------------------------------------

  Scenario: Restore a saved game
    When the player opens a saved game file
    Then all game state is restored from the file
    And the screen displays the restored state
    And contract offers are refreshed

  Scenario: Restore replaces current game state
    Given the player has been playing a game
    When the player opens a different saved game
    Then the current game is replaced by the saved game
    And the player is prompted to save the current game first

  # -----------------------------------------------------------
  # State Preservation
  # -----------------------------------------------------------

  Scenario: All commodities are preserved
    Given the player has 5000 wheat, 200 slaves, 50 oxen, 30 horses, 100 manure
    And the player has 400 acres of land in various stages
    When the game is saved and restored
    Then all commodity quantities match the saved values

  Scenario: Financial state is preserved
    Given the player has 25000 gold
    And a loan of 10000 at a certain interest rate
    And a credit rating and credit limit
    When the game is saved and restored
    Then gold, loan, interest rate, credit rating, and credit limit all match

  Scenario: Pyramid progress is preserved
    Given the pyramid has 5000 stones and a height of 50
    And the stone quota is 100
    When the game is saved and restored
    Then pyramid stones, height, and quota all match

  Scenario: Date is preserved
    Given the current date is month 7 of year 12
    When the game is saved and restored
    Then the date is month 7 of year 12

  Scenario: Health values are preserved
    Given slave health is 0.75, oxen health is 0.8, horse health is 0.6
    When the game is saved and restored
    Then all health values match the saved values

  Scenario: Overseer state is preserved
    Given the player has 8 overseers
    And the overseer pressure is 0.4
    When the game is saved and restored
    Then overseer count and pressure match the saved values

  Scenario: Pending contracts are preserved
    Given the player has 3 pending contracts
    When the game is saved and restored
    Then all 3 pending contracts are present with matching terms

  Scenario: Market prices are preserved
    Given commodity prices have changed from their starting values
    When the game is saved and restored
    Then all market prices match the saved values

  Scenario: Feed rates and quotas are preserved
    Given the slave feed rate is 9.5
    And the oxen feed rate is 60
    And the horse feed rate is 50
    And the planting quota is 200 acres
    And the manure spread rate is 500 tons
    When the game is saved and restored
    Then all feed rates and quotas match the saved values

  # -----------------------------------------------------------
  # New Game
  # -----------------------------------------------------------

  Scenario: Starting a new game prompts to save
    Given the player has been playing
    When the player starts a new game
    Then the player is prompted to save the current game first
    And then all state is reset to initial values

  Scenario: Starting a new game resets all state
    When the player starts a new game
    Then all commodities are reset to 0
    And gold is reset to 0
    And the date is reset to January of year 1
    And the pyramid is empty
