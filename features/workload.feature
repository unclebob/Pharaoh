Feature: Workload
  All activities require slave labor measured in man-hours per day.
  The total required work is divided among all slaves.
  When slaves cannot meet the workload, all activities are proportionally reduced.
  Oxen multiply slave work capacity; motivation and health affect output.

  Background:
    Given the game is running

  # -----------------------------------------------------------
  # Work Quota Calculation
  # -----------------------------------------------------------

  Scenario Outline: Individual work components
    Given the game state has the following values
    When required work is calculated
    Then the work component <component> equals <formula>

    Examples:
      | component        | formula                                     |
      | ox tending       | oxen * 1                                    |
      | manure spreading | tons to spread * 64                         |
      | wheat sowing     | acres to sow * 30                           |
      | field tending    | acres planted * 20 + acres growing * 15     |
      | wheat harvest    | bushels ripe * 0.1 + acres ripe * 20        |
      | horse tending    | horses * 1                                  |
      | pyramid work     | stone quota * average pyramid height * 12   |

  Scenario: Total required work includes randomness
    Given all work components sum to 10000
    When the total required work is calculated
    Then total required work = 10000, randomized with approximately 10% variance
    And required work per slave = total required work / slaves

  Scenario: Temporary work additions from events
    Given a random event adds 5000 man-hours of extra work
    When the total required work is calculated
    Then the 5000 extra man-hours are included in the total
    And the extra work resets to 0 after the month

  # -----------------------------------------------------------
  # Slave Work Capacity
  # -----------------------------------------------------------

  Scenario: Maximum work per slave
    Given motivation is 1.2
    And work ability per slave is 8.0
    And ox multiplier is 1.5
    When maximum work per slave is calculated
    Then max work per slave = 1.2 * 8.0 * 1.5 = 14.4

  Scenario: Work ability depends on slave health
    Given slave health is 0.7
    When work ability is calculated
    Then work ability is looked up from the health-to-ability table
    And randomized with approximately 10% variance

  # -----------------------------------------------------------
  # Ox Multiplier
  # -----------------------------------------------------------

  Scenario Outline: Oxen boost slave productivity
    Given there are <slaves> slaves and <oxen> oxen
    And oxen efficiency is <oxen_efficiency>
    When the ox multiplier is calculated
    Then oxen-to-slave ratio = <oxen> / <slaves>
    And the raw ox multiplier is looked up from the ox-ratio table
    And the effective ox multiplier = max(raw multiplier * <oxen_efficiency>, 1)

    Examples:
      | slaves | oxen | oxen_efficiency |
      | 100    | 50   | 0.9             |
      | 100    | 0    | 1.0             |
      | 100    | 200  | 0.5             |

  # -----------------------------------------------------------
  # Proportional Reduction
  # -----------------------------------------------------------

  Scenario: Slaves meet full workload
    Given max work per slave is 12 and required work per slave is 10
    When actual work per slave is determined
    Then actual work per slave = 10 (the required amount)
    And slave efficiency = 1.0
    And all activities proceed at full capacity

  Scenario: Slaves cannot meet workload
    Given max work per slave is 8 and required work per slave is 10
    And there are 100 slaves
    When actual work per slave is determined
    Then actual work per slave = 8 (the maximum they can do)
    And total work = 8 * 100 = 800
    And slave efficiency = 800 / (10 * 100) = 0.8
    And all activities are reduced to 80%

  Scenario: Proportional reduction affects all activities
    Given slave efficiency is 0.7
    When a month is simulated
    Then planting rate is reduced to 70%
    And harvest is 70% of ripe wheat
    And manure spread is reduced to 70%
    And pyramid stones added is 70% of quota
    And livestock feeding rates are reduced to 70%

  # -----------------------------------------------------------
  # Work Deficit and Overseer Stress
  # -----------------------------------------------------------

  Scenario: Work deficit exists when slaves are overloaded
    Given max work per slave is 8 and required work per slave is 12
    When the work deficit is calculated
    Then work deficit per slave = 12 - 8 = 4
    And overseer stress increases

  Scenario: No work deficit when slaves meet quota
    Given max work per slave is 12 and required work per slave is 10
    When the work deficit is calculated
    Then work deficit per slave = 0
    And overseers begin to relax
