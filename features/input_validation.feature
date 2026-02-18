Feature: Input Validation
  All dialogs validate user input. Invalid numbers, missing mode selections,
  and negative values are rejected with category-specific error messages.
  See initial-spec.md Messages section A.16 for error message pools.

  Background:
    Given the game is running

  # -----------------------------------------------------------
  # Dialog Input Mechanics
  # -----------------------------------------------------------

  Scenario: Only digits and decimal points are accepted
    Given a buy-sell dialog is open for wheat
    When the player types "123.5"
    Then the dialog input contains "123.5"

  Scenario: Letters are rejected from dialog input
    Given a buy-sell dialog is open for wheat
    When the player types "12a3"
    Then the dialog input contains "123"

  Scenario: Backspace removes the last character
    Given a buy-sell dialog is open for wheat
    And the dialog input contains "456"
    When the player presses backspace
    Then the dialog input contains "45"

  Scenario: Backspace on empty input stays empty
    Given a buy-sell dialog is open for wheat
    When the player presses backspace
    Then the dialog input contains ""

  # -----------------------------------------------------------
  # Buy/Sell Mode Selection
  # -----------------------------------------------------------

  Scenario: Buy/sell dialog requires mode before executing
    Given a buy-sell dialog is open for wheat
    And the dialog input contains "100"
    When the player presses enter without selecting buy or sell
    Then a buy-sell mode error message is displayed

  Scenario: Buy mode is selected with 'b' key
    Given a buy-sell dialog is open for wheat
    When the player presses 'b'
    Then the dialog mode is set to buy

  Scenario: Sell mode is selected with 's' key
    Given a buy-sell dialog is open for wheat
    When the player presses 's'
    Then the dialog mode is set to sell

  # -----------------------------------------------------------
  # Loan Mode Selection
  # -----------------------------------------------------------

  Scenario: Loan dialog requires mode before executing
    Given a loan dialog is open
    And the dialog input contains "5000"
    When the player presses enter without selecting borrow or repay
    Then a loan mode error message is displayed

  Scenario: Borrow mode is selected with 'b' key
    Given a loan dialog is open
    When the player presses 'b'
    Then the dialog mode is set to borrow

  Scenario: Repay mode is selected with 'r' key
    Given a loan dialog is open
    When the player presses 'r'
    Then the dialog mode is set to repay

  # -----------------------------------------------------------
  # Overseer Mode Selection
  # -----------------------------------------------------------

  Scenario: Overseer dialog requires mode before executing
    Given an overseer dialog is open
    And the dialog input contains "3"
    When the player presses enter without selecting hire or fire
    Then an overseer mode error message is displayed

  Scenario: Hire mode is selected with 'h' key
    Given an overseer dialog is open
    When the player presses 'h'
    Then the dialog mode is set to hire

  Scenario: Fire mode is selected with 'f' key
    Given an overseer dialog is open
    When the player presses 'f'
    Then the dialog mode is set to fire

  # -----------------------------------------------------------
  # Invalid Number Input
  # -----------------------------------------------------------

  Scenario: Non-numeric input in buy-sell dialog
    Given a buy-sell dialog is open for wheat
    And the dialog input contains ""
    And the dialog mode is set to buy
    When the player presses enter
    Then a buy-sell input error message is displayed

  Scenario: Non-numeric input in loan dialog
    Given a loan dialog is open
    And the dialog input contains ""
    And the dialog mode is set to borrow
    When the player presses enter
    Then a loan input error message is displayed

  Scenario: Non-numeric input in planting dialog
    Given a planting dialog is open
    And the dialog input contains ""
    When the player presses enter
    Then a planting input error message is displayed

  Scenario: Non-numeric input in pyramid dialog
    Given a pyramid dialog is open
    And the dialog input contains ""
    When the player presses enter
    Then a pyramid input error message is displayed

  Scenario: Non-numeric input in manure dialog
    Given a manure spreading dialog is open
    And the dialog input contains ""
    When the player presses enter
    Then a manure input error message is displayed

  # -----------------------------------------------------------
  # Escape Key Closes Dialog
  # -----------------------------------------------------------

  Scenario: Escape key closes any dialog
    Given a buy-sell dialog is open for wheat
    When the player presses escape
    Then the dialog is closed

  # -----------------------------------------------------------
  # Error Message Categories
  # -----------------------------------------------------------

  Scenario: Each dialog type has its own error message pool
    Given there are input error pools for each dialog category
    Then buy-sell errors come from the buysell pool
    And loan errors come from the loan pool
    And planting errors come from the planting pool
    And pyramid errors come from the pyramid pool
    And manure errors come from the manure pool
    And overseer errors come from the overseer pool
    And feed errors come from the feed pool
