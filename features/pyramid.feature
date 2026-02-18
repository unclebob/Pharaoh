Feature: Pyramid
  The player builds a pyramid by setting a monthly stone quota.
  The pyramid is modeled as a 2D equilateral triangle.
  Each stone is one unit of area. As area increases, height grows.
  Completing the pyramid wins the game.
  See initial-spec.md for full details.

  Background:
    Given the game is running

  # -----------------------------------------------------------
  # Pyramid Geometry
  # -----------------------------------------------------------

  Scenario: Maximum pyramid height from base
    Given the pyramid base is 346.41 stones
    When the maximum height is calculated
    Then max height = (sqrt(3) / 2) * 346.41 = approximately 300

  Scenario Outline: Pyramid height for difficulty levels
    Given the difficulty is "<level>"
    And the pyramid base is <base>
    When the maximum height is calculated
    Then the target height is approximately <height> feet

    Examples:
      | level  | base    | height |
      | Easy   | 115.47  | 100    |
      | Normal | 346.41  | 300    |
      | Hard   | 1154.7  | 1000   |

  Scenario: Pyramid height from base and area
    Given the pyramid base is 346.41
    And the pyramid has 1000 stones (area units)
    When the height is calculated
    Then determinant = base^2 - 4 * area / sqrt(3)
    And height = (base - sqrt(determinant)) / (2 / sqrt(3))

  Scenario: Height capped at maximum
    Given the pyramid base is 115.47
    And the area exceeds (sqrt(3)/4) * base^2
    When the height is calculated
    Then the height equals the maximum for that base

  # -----------------------------------------------------------
  # Stone Laying
  # -----------------------------------------------------------

  Scenario: Pyramid stones added based on quota and efficiency
    Given the pyramid quota is 100 stones
    And slave efficiency is 0.8
    When a month is simulated
    Then stones added = 100 * 0.8 = 80
    And the pyramid stone count increases by 80

  Scenario: No stones laid when slaves cannot work
    Given the pyramid quota is 100 stones
    And slave efficiency is 0.0
    When a month is simulated
    Then no stones are added to the pyramid

  # -----------------------------------------------------------
  # Pyramid Work Cost
  # -----------------------------------------------------------

  Scenario: Pyramid work increases with height
    Given the pyramid quota is 50
    And the current pyramid height is 100
    And the projected new height is 102
    And the average height is ceil((100 + 102) / 2) = 101
    When pyramid work is calculated
    Then pyramid work = 50 * 101 * 12 = 60600 man-hours per day
    # Higher pyramids require more work per stone

  # -----------------------------------------------------------
  # Pyramid Gold Cost
  # -----------------------------------------------------------

  Scenario: Pyramid construction costs gold
    Given the average pyramid height is 150
    And stones added this month is 80
    When pyramid costs are deducted
    Then gold decreases by 150 * 80 = 12000

  # -----------------------------------------------------------
  # Setting the Quota
  # -----------------------------------------------------------

  Scenario: Player sets pyramid stone quota
    When the player sets the pyramid quota to 200
    Then the pyramid quota should be 200

  Scenario: Pyramid quota cannot be negative
    When the player tries to set the pyramid quota to -10
    Then the input is rejected

  # -----------------------------------------------------------
  # Win Condition
  # -----------------------------------------------------------

  Scenario: Player wins by completing the pyramid
    Given the pyramid base is 115.47
    And the maximum height is approximately 100
    When the pyramid height reaches within 1 unit of the maximum
    Then the player wins the game
    And a victory message is displayed

  Scenario: Player has not yet won
    Given the pyramid base is 346.41
    And the pyramid height is 200
    When the win condition is checked
    Then 200 + 1 = 201 which is less than the maximum of 300
    And the game continues

  # -----------------------------------------------------------
  # Pyramid Messages (see initial-spec.md Messages section)
  # -----------------------------------------------------------

  Scenario: Victory message on pyramid completion
    When the player wins the game
    Then a random win message is displayed from the congratulations pool
    And a farewell message is displayed from the farewell pool
    # Win pool contains ~10 variants, e.g. "Your pyramid is complete! Strike up the band."
    # Farewell pool contains ~5 variants of warm send-offs

  Scenario: Invalid pyramid quota input
    When the player enters non-numeric text in the pyramid quota dialog
    Then a random input-error message is displayed from the pyramid error pool
    # Pool contains ~5 variants, e.g. "Stone dust has gotten into your fingers."

  Scenario: Negative pyramid quota input
    When the player enters a negative number for pyramid quota
    Then a random negative-input message is displayed from the pyramid error pool
    # Pool contains ~5 variants, e.g. "Negative stones? Ah you want me to remove stones!"
