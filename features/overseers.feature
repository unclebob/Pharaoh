Feature: Overseers
  Overseers manage slaves and defend the estate.
  They are paid monthly salaries in gold.
  When work falls behind, overseers become stressed and lash slaves.
  Lashing increases short-term motivation but damages slave health.

  Background:
    Given the game is running

  # -----------------------------------------------------------
  # Hiring and Firing
  # -----------------------------------------------------------

  Scenario: Hire overseers
    Given the player has 5 overseers
    When the player hires 3 overseers
    Then the player should have 8 overseers

  Scenario: Fire overseers
    Given the player has 5 overseers
    When the player fires 2 overseers
    Then the player should have 3 overseers

  Scenario: Obtain a target number of overseers
    Given the player has 5 overseers
    When the player obtains 10 overseers
    Then the player should have 10 overseers

  Scenario: Cannot fire more overseers than employed
    Given the player has 3 overseers
    When the player tries to fire 5 overseers
    Then the action is rejected

  Scenario: Overseer count must be a whole number
    When the player tries to hire 2.5 overseers
    Then the action is rejected with a fractional number error

  # -----------------------------------------------------------
  # Monthly Salary
  # -----------------------------------------------------------

  Scenario: Overseers are paid monthly
    Given the player has 10 overseers
    And the overseer pay is 500 gold each
    When a month is simulated
    Then gold decreases by 5000

  # -----------------------------------------------------------
  # Overseer Effectiveness
  # -----------------------------------------------------------

  Scenario: Horse-to-overseer ratio affects effectiveness
    Given there are 5 overseers and 10 horses
    And horse efficiency is 0.9
    When overseer effectiveness is calculated
    Then horse-to-overseer ratio = 10 / 5 = 2.0
    And mounted effectiveness = 2.0 * 0.9 = 1.8
    And overseer effectiveness is looked up from the effectiveness table based on 1.8
    And randomized with approximately 10% variance

  Scenario: Slave-to-overseer ratio
    Given there are 5 overseers and 100 slaves
    When the ratio is calculated
    Then slave-to-overseer ratio = 100 / (5 + 1) = 16.67
    # The pharaoh counts as a permanent overseer (+1)
    And overseer effect per slave = overseer effectiveness / slave-to-overseer ratio

  # -----------------------------------------------------------
  # Stress and Lashing
  # -----------------------------------------------------------

  Scenario: Stress builds when work deficit exists
    Given the work deficit per slave is 5
    And the current overseer pressure is 0.3
    When stress is calculated
    Then stress increase = min(1, 5 / 10) = 0.5
    And relaxation = 0 (no relaxation when deficit exists)
    And overseer pressure = 0.3 + 0.5 - 0 = 0.8

  Scenario: Stress relaxes when no work deficit
    Given the work deficit per slave is 0
    And the current overseer pressure is 0.6
    When stress is calculated
    Then stress increase = 0
    And relaxation = 0.6 * 0.3 = 0.18
    And overseer pressure = 0.6 + 0 - 0.18 = 0.42

  Scenario: Lash rate from stress
    Given overseer pressure is 0.7
    When lashing is calculated
    Then stress-driven lashing is looked up from the stress-to-lash table
    And randomized with approximately 10% variance
    And slave lash rate = stress-driven lashing * overseer effect per slave

  # -----------------------------------------------------------
  # Motivation
  # -----------------------------------------------------------

  Scenario: Slave motivation is sum of positive and negative factors
    Given the overseer effect per slave is 0.5
    And the slave lash rate is 0.3
    When motivation is calculated
    Then positive motivation is looked up from the oversight table, randomized ±10%
    And negative motivation is looked up from the lashing table
    And total motivation = positive motivation + negative motivation

  # -----------------------------------------------------------
  # Overseers Quit When Unpaid
  # -----------------------------------------------------------

  Scenario: Overseers leave when gold is negative
    Given the player has 5 overseers
    And the player's gold is -1000
    When monthly costs are assessed
    Then all overseers are fired
    And the overseer pay rate increases by approximately 20% with slight variance
