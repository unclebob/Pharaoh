Feature: Planting
  Land cycles through four states: fallow, planted (sewn), growing, and ripe.
  Wheat is planted on fallow land, grows over three months, and is harvested.
  Yield depends on fertilizer (manure per acre) and the planting season.
  Slave efficiency affects how much of the planned planting actually occurs.
  See initial-spec.md for full details.

  Background:
    Given the game is running

  # -----------------------------------------------------------
  # Land Cycle
  # -----------------------------------------------------------

  Scenario: Land progresses through the four-stage cycle
    Given the player has 100 acres of fallow land
    And the player sets the planting quota to 50 acres
    And slaves are fully efficient
    When month 1 is simulated
    Then 50 acres move from fallow to planted
    When month 2 is simulated
    Then 50 acres move from planted to growing
    When month 3 is simulated
    Then 50 acres move from growing to ripe
    When month 4 is simulated
    Then 50 acres are harvested and return to fallow

  Scenario: Total land is conserved
    Given the player has land in various stages
    When a month is simulated
    Then total land = fallow + planted + growing + ripe

  # -----------------------------------------------------------
  # Actual Planting Rate
  # -----------------------------------------------------------

  Scenario: Planting limited by slave efficiency
    Given the player sets the planting quota to 100 acres
    And slave efficiency is 0.7
    When a month is simulated
    Then only 70 acres are actually planted
    # Actual planting = planting quota * slave efficiency

  Scenario: Planting limited by available fallow land
    Given the player sets the planting quota to 100 acres
    And only 60 acres are fallow
    And slave efficiency is 1.0
    When a month is simulated
    Then only 60 acres are actually planted
    # Actual planting cannot exceed available fallow land

  # -----------------------------------------------------------
  # Wheat Sowing
  # -----------------------------------------------------------

  Scenario: Wheat consumed for sowing
    Given the sowing rate per acre is a fixed amount
    And 50 acres are being planted
    When a month is simulated
    Then wheat consumed for sowing is sowing rate * 50

  # -----------------------------------------------------------
  # Wheat Yield
  # -----------------------------------------------------------

  Scenario: Yield depends on manure per acre and season
    Given the manure spread per acre is 3.0 tons
    And the current month is July
    When the wheat yield is calculated
    Then yield = (fertilizer factor) * (random variance near 1.0) * (seasonal factor)
    # The fertilizer factor is looked up from a yield table based on manure-per-acre
    # The seasonal factor is looked up from a seasonal table based on the current month
    # June/July are best for planting; January is worst

  Scenario: Wheat growth stages track the crop
    Given 1000 bushels of wheat are sewn this month
    When the next month is simulated
    Then wheat sewn decreases by 1000
    And wheat growing increases by 1000
    When the following month is simulated
    Then wheat growing decreases by 1000
    And wheat ripe increases by 1000

  # -----------------------------------------------------------
  # Harvest
  # -----------------------------------------------------------

  Scenario: Ripe wheat harvested based on slave efficiency
    Given there are 5000 bushels of ripe wheat
    And slave efficiency is 0.8
    When a month is simulated
    Then 4000 bushels are harvested (ripe wheat * slave efficiency)
    And 1000 bushels are lost ((1 - slave efficiency) * ripe wheat)
    And the wheat store increases by 4000 bushels

  # -----------------------------------------------------------
  # Wheat Rot
  # -----------------------------------------------------------

  Scenario: Stored wheat rots each month
    Given the player has 10000 bushels of wheat
    And there is a monthly rot rate
    When a month is simulated
    Then wheat lost to rot = stored wheat * rot rate, randomized near 1.0
    And the rot is deducted before usage calculations

  # -----------------------------------------------------------
  # Manure Spreading
  # -----------------------------------------------------------

  Scenario: Manure spread limited by supply
    Given the player sets manure to spread at 500 tons
    And the player has only 300 tons of manure
    When a month is simulated
    Then only 300 tons are spread

  Scenario: Manure spread limited by slave efficiency
    Given the player sets manure to spread at 500 tons
    And the player has 500 tons of manure
    And slave efficiency is 0.6
    When a month is simulated
    Then only 300 tons are actually spread

  Scenario: Manure per acre ratio determines yield quality
    Given 200 tons of manure are spread on 100 acres
    Then the manure-per-acre ratio is 2.0

  # -----------------------------------------------------------
  # Seasonal Planting Guidance
  # -----------------------------------------------------------

  Scenario: Best planting months
    Given the current month is June or July
    When wheat is planted
    Then the seasonal yield multiplier is at its maximum
    And the resulting harvest will be the largest

  Scenario: Worst planting months
    Given the current month is January
    When wheat is planted
    Then the seasonal yield multiplier is at its minimum
    And the resulting harvest will be very poor

  # -----------------------------------------------------------
  # Planting Messages (see initial-spec.md Messages section)
  # -----------------------------------------------------------

  Scenario: Invalid planting input
    When the player enters non-numeric text in the planting dialog
    Then a random input-error message is displayed from the planting error pool
    # Pool contains ~5 variants, e.g. "We are planting wheat, not alphabet soup."

  Scenario: Negative planting input
    When the player enters a negative number for acres to plant
    Then a random negative-input message is displayed from the planting error pool
    # Pool contains ~5 variants, e.g. "Negative wheat. Grows down eh?"

  Scenario: Invalid manure spread input
    When the player enters non-numeric text in the manure spread dialog
    Then a random input-error message is displayed from the fertilizer error pool
    # Pool contains ~5 variants, e.g. "We are talking about fertilizer, measured in tons."

  Scenario: Negative manure spread input
    When the player enters a negative number for manure to spread
    Then a random negative-input message is displayed from the fertilizer error pool
    # Pool contains ~5 variants, e.g. "Negative fertilizer? Hey, maybe that's food!"
