Feature: Random Events
  Each month there is a 12.5% chance (1 in 8) of a random event.
  Events range from catastrophic (locusts, acts of god) to economic (price shifts).
  Each event type has a probability within the event roll and specific effects.

  Background:
    Given a random event has been triggered
    # A uniform random number 0-8 is drawn each month; event fires if < 1

  # -----------------------------------------------------------
  # Event Probability Distribution
  # -----------------------------------------------------------

  Scenario Outline: Event type probability ranges
    Given the event roll (0-100) is <roll>
    Then the event type is "<event>"

    Examples:
      | roll   | event          |
      | 0-1    | Locusts        |
      | 2-5    | Plagues        |
      | 6-7    | Acts of God    |
      | 8-19   | Acts of Mobs   |
      | 20     | War            |
      | 21-29  | Revolt         |
      | 30-44  | Workload       |
      | 45-59  | Health Events  |
      | 60-64  | Labor Event    |
      | 65-74  | Wheat Event    |
      | 75-84  | Gold Event     |
      | 85-99  | Economy Event  |

  # -----------------------------------------------------------
  # Locusts (2% chance)
  # -----------------------------------------------------------

  Scenario: Locusts devour all crops
    Given the player has planted, growing, and ripe land
    When locusts strike
    Then all planted, growing, and ripe land reverts to fallow
    And all wheat sewn, growing, and ripe is destroyed
    And extra work is added: approximately 15 man-hours per slave plus 5 per acre
    But nothing happens if the player has no land

  # -----------------------------------------------------------
  # Plagues (4% chance)
  # -----------------------------------------------------------

  Scenario: Plague strikes all living things
    Given the player has slaves, oxen, and horses
    When a plague strikes
    Then slave health is reduced by a random factor between 0.2 and 0.9
    And oxen health is reduced by a random factor between 0.2 and 0.9
    And horse health is reduced by a random factor between 0.2 and 0.9
    And slave population is reduced by a random factor between 0.7 and 0.95
    And oxen population is reduced by a random factor between 0.7 and 0.95
    And horse population is reduced by a random factor between 0.7 and 0.95
    But nothing happens if population is zero

  # -----------------------------------------------------------
  # Acts of God (2% chance)
  # -----------------------------------------------------------

  Scenario: Catastrophic natural disaster
    When an act of God occurs
    Then each resource is reduced by a random factor between 0.3 and 0.8
    # Fallow land, each growing stage (with its wheat), slaves, oxen,
    # horses, wheat stores, and manure are all independently reduced
    And extra work is added: approximately 11 man-hours per slave plus 5 per acre

  # -----------------------------------------------------------
  # Acts of Mobs (12% chance)
  # -----------------------------------------------------------

  Scenario: Mob violence damages estate
    When mobs attack
    Then wheat in all growing stages is reduced by a random factor between 0.6 and 0.8
    And slave population is reduced by a random factor between 0.6 and 0.8
    And oxen population is reduced by a random factor between 0.6 and 0.8
    And horse population is reduced by a random factor between 0.6 and 0.8
    And wheat stores are reduced by a random factor between 0.6 and 0.8
    And manure increases slightly (mobs leave a mess)
    And extra work is added: approximately 5-10 man-hours per slave plus 5 per acre

  # -----------------------------------------------------------
  # War (1% chance)
  # -----------------------------------------------------------

  Scenario: War outcome depends on overseers
    Given the player has overseers
    When war occurs
    Then the player's army strength = overseers + 1
    And the enemy army is proportional to the player's overseers, randomized ±20%
    And each side rolls with approximately 30% variance
    And gain = player roll / enemy roll, capped at a maximum ratio

  Scenario: Winning a war
    Given the war gain is 1.3
    When war effects are applied
    Then all resources are multiplied by approximately 1.3, each randomized ±20%
    And the player gains land, livestock, and wheat

  Scenario: Losing a war
    Given the war gain is 0.7
    When war effects are applied
    Then all resources are multiplied by approximately 0.7, each randomized ±20%
    And the player loses land, livestock, and wheat
    And extra work is added: approximately 15 man-hours per slave plus work from the enemy army

  # -----------------------------------------------------------
  # Revolt (9% chance)
  # -----------------------------------------------------------

  Scenario: Slave revolt based on suffering and sickness
    Given the slave lash rate is 0.5
    And slave health is 0.4
    When a revolt occurs
    Then suffering is looked up from the lash-to-suffering table
    And sickness is looked up from the health-to-sickness table
    And hatred = (suffering + sickness) / 2
    And destruction is looked up from the hatred-to-destruction table, randomized ±20%
    And survival factor = 1 - destruction (clamped to [0, 1])
    And all resources are multiplied by the survival factor
    And extra work = approximately 18 man-hours per slave plus 30 per overseer

  Scenario: No revolt when there are no slaves
    Given there are 0 slaves
    Then the revolt event is skipped

  # -----------------------------------------------------------
  # Workload Events (15% chance)
  # -----------------------------------------------------------

  Scenario: Extra workload imposed
    Given there are slaves
    When a workload event occurs
    Then extra work = approximately 10 man-hours per slave plus 8 per acre
    But nothing happens if there are no slaves

  # -----------------------------------------------------------
  # Health Events (15% chance)
  # -----------------------------------------------------------

  Scenario: Health of all livestock deteriorates
    When a health event occurs
    Then slave health is multiplied by approximately 0.6 with slight variance
    And oxen health is multiplied by approximately 0.6 with slight variance
    And horse health is multiplied by approximately 0.6 with slight variance
    But nothing happens if total population is zero

  # -----------------------------------------------------------
  # Labor Event (5% chance)
  # -----------------------------------------------------------

  Scenario: Overseer labor unrest
    Given the player has overseers
    When a labor event occurs
    Then the overseer pay increases by approximately 20% with slight variance
    And some overseers leave (population reduced by approximately 10%, rounded down)
    And overseer stress increases by approximately 0.5
    But nothing happens if there are no overseers

  # -----------------------------------------------------------
  # Wheat Event (10% chance)
  # -----------------------------------------------------------

  Scenario: Wheat stores and crops damaged
    Given the player has wheat in various stages
    When a wheat event occurs
    Then a loss factor of approximately 30% is applied (normally distributed, capped at 99%)
    And wheat stores are reduced by the loss factor
    And wheat sewn is reduced by the loss factor
    And wheat growing is reduced by the loss factor
    And wheat ripe is reduced by the loss factor
    But nothing happens if there are no crops

  # -----------------------------------------------------------
  # Gold Event (10% chance)
  # -----------------------------------------------------------

  Scenario: Gold reserves diminished
    Given the player has gold
    When a gold event occurs
    Then a loss factor of approximately 35% is applied (normally distributed, capped at 99%)
    And gold is reduced by the loss factor
    But nothing happens if gold is zero

  # -----------------------------------------------------------
  # Economy Event (15% chance)
  # -----------------------------------------------------------

  Scenario: Market prices shift randomly
    When an economy event occurs
    Then each commodity price shifts by approximately ±15% (normally distributed)
    # Wheat, oxen, horse, slave, and manure prices are all independently shifted
    And the inflation rate shifts by a small normally-distributed amount
