Feature: Feeding
  The player sets feed rates for slaves, oxen, and horses.
  Wheat is consumed each month based on these rates.
  When wheat is insufficient, all consumption is reduced proportionally.
  Eating produces manure as a byproduct.

  Background:
    Given the game is running

  # -----------------------------------------------------------
  # Feed Rate Configuration
  # -----------------------------------------------------------

  Scenario: Player sets slave feed rate
    When the player sets the slave feed rate to 9.5 bushels per slave per month
    Then the slave feed rate should be 9.5

  Scenario: Player sets oxen feed rate
    When the player sets the oxen feed rate to 60 bushels per ox per month
    Then the oxen feed rate should be 60

  Scenario: Player sets horse feed rate
    When the player sets the horse feed rate to 50 bushels per horse per month
    Then the horse feed rate should be 50

  Scenario: Feed rate cannot be negative
    When the player tries to set a feed rate to -5
    Then the input is rejected

  # -----------------------------------------------------------
  # Monthly Wheat Consumption
  # -----------------------------------------------------------

  Scenario: Wheat consumed for feeding livestock
    Given there are 100 slaves with feed rate of 10
    And there are 50 oxen with feed rate of 60 and slave efficiency of 0.8
    And there are 20 horses with feed rate of 50 and slave efficiency of 0.8
    When a month is simulated
    Then wheat fed to slaves = 100 * 10 = 1000
    And wheat fed to oxen = 60 * 0.8 * 50 * 0.8 = 1920
    And wheat fed to horses = 50 * 0.8 * 20 * 0.8 = 640
    # Slave wheat = slaves * slave feed rate
    # Oxen wheat = (oxen feed rate * slave efficiency) * oxen * slave efficiency
    # Horse wheat = (horse feed rate * slave efficiency) * horses * slave efficiency

  # -----------------------------------------------------------
  # Wheat Shortage Proportioning
  # -----------------------------------------------------------

  Scenario: Insufficient wheat reduces all usage proportionally
    Given total wheat usage would be 5000 bushels
    And the player has only 3000 bushels of wheat after rot
    When a month is simulated
    Then the wheat efficiency factor is 3000 / 5000 = 0.6
    And wheat sown is reduced to 60% of planned
    And wheat fed to horses is reduced to 60%
    And wheat fed to oxen is reduced to 60%
    And wheat fed to slaves is reduced to 60%
    And actual feed rates are reduced to 60%
    And actual sowing rate is reduced to 60%
    # Wheat efficiency = (wheat available after rot) / (total wheat needed)

  Scenario: Sufficient wheat means no reduction
    Given total wheat usage would be 3000 bushels
    And the player has 5000 bushels of wheat after rot
    When a month is simulated
    Then the wheat efficiency factor is 1.0
    And all consumption proceeds as planned

  # -----------------------------------------------------------
  # Manure Production
  # -----------------------------------------------------------

  Scenario: Manure produced from consumed wheat
    Given 3000 bushels of wheat are eaten this month
    When manure production is calculated
    Then approximately 30 tons of manure are added to the stockpile
    # For every 100 bushels eaten, about 1 ton of manure is produced
    # with slight random variance

  Scenario: Manure stockpile cannot go negative
    Given the player has 10 tons of manure
    And 15 tons of manure are used for spreading
    When the manure balance is calculated
    Then the manure stockpile is clamped to 0
