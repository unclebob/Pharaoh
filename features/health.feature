Feature: Health
  Health is tracked on a 0-to-1 scale for slaves, oxen, and horses.
  Nourishment improves health; overwork, lashing, and aging degrade it.
  Health affects birth rate, death rate, work ability, and selling price.
  See initial-spec.md for full details.

  Background:
    Given the game is running

  # -----------------------------------------------------------
  # Slave Health
  # -----------------------------------------------------------

  Scenario: Slave health improves with good nourishment
    Given the effective slave feed rate is 10
    When slave health is updated
    Then nourishment is looked up from the slave nourishment table based on feed rate
    And randomized with approximately 10% variance
    And slave health increases by the nourishment amount

  Scenario: Slave health decreases from sickness
    Given the slave lash rate is 0.4
    And slave labor (work per slave / ox multiplier) is 8
    When slave sickness is calculated
    Then lash sickness is looked up from the lash-to-sickness table, randomized ±10%
    And work sickness is looked up from the labor-to-sickness table
    And total sickness rate = work sickness + lash sickness
    And slave health decreases by total sickness rate

  Scenario: Slave health formula
    Given slave health is 0.7
    And nourishment is 0.05 and sickness rate is 0.03
    When slave health is updated
    Then slave health = 0.7 + 0.05 - 0.03 = 0.72

  Scenario: Slave health capped at 1.0
    Given slave health is 0.98 and nourishment is 0.05
    When slave health is updated
    Then slave health is clamped to 1.0

  Scenario: Slave health cannot go below 0
    Given slave health is 0.01 and sickness rate is 0.05
    When slave health is updated
    Then slave health is clamped to 0.0

  Scenario: No sickness when health is already 0
    Given slave health is 0
    Then sickness rate = 0
    # Dead slaves cannot get sicker

  # -----------------------------------------------------------
  # Oxen Health
  # -----------------------------------------------------------

  Scenario: Oxen health improves with feeding
    Given effective oxen feed rate is 60
    When oxen health is updated
    Then nourishment is looked up from the oxen nourishment table, randomized ±10%
    And if oxen health < 1.0 then diet = nourishment, else diet = 0
    And aging rate = 0.05 per month when health > 0
    And oxen health += diet - aging rate

  Scenario: Oxen stop healing when fully healthy
    Given oxen health is 1.0
    When oxen health is updated
    Then diet = 0
    And oxen health decreases by 0.05 (aging only)

  Scenario: Oxen stop aging when health reaches 0
    Given oxen health is 0
    When oxen health is updated
    Then aging rate = 0
    And oxen health remains at 0

  # -----------------------------------------------------------
  # Horse Health
  # -----------------------------------------------------------

  Scenario: Horse health improves with feeding
    Given effective horse feed rate is 50
    When horse health is updated
    Then nourishment is looked up from the horse nourishment table, randomized ±10%
    And if horse health < 1.0 then diet = nourishment, else diet = 0
    And aging rate = 0.08 per month when health > 0
    And horse health += diet - aging rate

  Scenario: Horses age faster than oxen
    When comparing aging rates
    Then horse aging rate is 0.08 per month
    And oxen aging rate is 0.05 per month
    # Horses require more feeding to maintain health

  # -----------------------------------------------------------
  # Birth and Death Rates
  # -----------------------------------------------------------

  Scenario Outline: Population changes depend on health
    Given <animal> health is <health>
    When birth and death rates are calculated
    Then birth rate constant is looked up from the <animal> birth table, randomized ±10%
    And death rate constant is looked up from the <animal> death table, randomized ±10%
    And births = birth rate constant * population
    And deaths = death rate constant * population
    And new population = population + births - deaths

    Examples:
      | animal | health |
      | slave  | 0.9    |
      | oxen   | 0.7    |
      | horse  | 0.5    |

  Scenario: Healthy slaves reproduce faster
    Given slave health is 0.95
    When births and deaths are calculated
    Then the birth rate exceeds the death rate
    And the slave population grows

  Scenario: Unhealthy slaves die faster
    Given slave health is 0.2
    When births and deaths are calculated
    Then the death rate exceeds the birth rate
    And the slave population declines

  Scenario: Population cannot go negative
    Given there are 5 slaves
    And the death rate would kill 10
    When population is updated
    Then slaves = 0
    # Population is clamped to a minimum of zero

  # -----------------------------------------------------------
  # Health Effects on Other Systems
  # -----------------------------------------------------------

  Scenario: Slave health affects work ability
    Given slave health is 0.5
    When work ability is determined
    Then work ability per slave is reduced
    And slaves produce less work per person

  Scenario: Oxen health affects efficiency
    Given oxen health is 0.4
    When oxen efficiency is determined
    Then oxen efficiency is reduced
    And the ox multiplier for slave work is diminished

  Scenario: Horse health affects overseer effectiveness
    Given horse health is 0.3
    When horse efficiency is determined
    Then horse efficiency is reduced
    And overseer effectiveness drops

  Scenario: Health affects selling price
    Given slave health is 0.5
    And the slave market price is 1000
    When the player sells slaves
    Then the actual price received per slave is 1000 * 0.5 = 500
