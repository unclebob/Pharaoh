Feature: Loans
  The player can borrow gold from the bank and must repay with interest.
  Credit rating determines borrowing capacity and interest rates.
  Failure to repay leads to penalties and eventual foreclosure.
  See initial-spec.md for full details.

  Background:
    Given the game is running

  # -----------------------------------------------------------
  # Borrowing
  # -----------------------------------------------------------

  Scenario: Borrow within credit limit
    Given the credit limit is 100000
    And the current loan is 0
    When the player borrows 50000 gold
    Then the loan balance should be 50000
    And the player's gold should increase by 50000

  Scenario: Borrow exceeding credit limit triggers credit check
    Given the credit limit is 100000
    And the current loan is 80000
    When the player tries to borrow 30000 gold
    Then a credit check fee is offered
    # Fee is approximately 5% of the total debt, with slight random variance
    And the player must accept or decline the fee

  Scenario: Credit check recalculates limit based on real net worth
    Given the player accepts the credit check fee
    Then the credit limit is recalculated as real net worth times credit rating
    # Real net worth = (slaves * slave price * slave health)
    #                + (oxen * oxen price * oxen health)
    #                + (horses * horse price * horse health)
    #                + total land * land price + manure * manure price
    #                + wheat * wheat price + gold - loan
    # New credit limit = max(real net worth * credit rating, credit lower bound)

  Scenario: Loan near limit increases interest
    Given the credit limit is 100000
    And the loan is 85000
    When the player borrows 5000 gold
    Then the interest addition increases by 0.2
    # Triggered when remaining credit headroom is less than 20% of the limit

  # -----------------------------------------------------------
  # Repaying
  # -----------------------------------------------------------

  Scenario: Full loan payoff
    Given the player has a loan of 50000
    And the player has 60000 gold
    When the player repays the full 50000
    Then the loan balance should be 0
    And the player's gold should decrease by 50000
    And the credit rating improves by (1 - credit rating) / 3
    And the interest addition is multiplied by 0.80

  Scenario: Partial repayment adjusts credit rating
    Given the player has a loan of 100000
    When the player repays 30000 gold
    Then the loan decreases by 30000
    And the credit rating is multiplied by the repay index
    # The repay index is looked up from a repayment table based on payment/loan ratio
    # Repay index ranges from 1.0 (tiny payment) to 1.3 (full payoff)
    And the interest addition is divided by the repay index

  Scenario: Cannot repay more than available gold
    Given the player has 10000 gold
    When the player tries to repay 20000
    Then the repayment is rejected

  # -----------------------------------------------------------
  # Monthly Interest
  # -----------------------------------------------------------

  Scenario: Monthly interest deducted from gold
    Given the player has a loan of 100000
    And the interest rate is 5
    And the interest addition is 2
    When a month is simulated
    Then gold decreases by 100000 * (5 + 2) / 100 = 7000

  # -----------------------------------------------------------
  # Credit Rating Dynamics
  # -----------------------------------------------------------

  Scenario: Credit rating decays while loan outstanding
    Given the player has an outstanding loan
    When a month is simulated
    Then the credit rating is multiplied by 0.96
    And the interest addition is multiplied by 1.02

  Scenario: Credit rating recovers when loan-free
    Given the player has no outstanding loan
    When a month is simulated
    Then the credit rating increases by (1 - credit rating) / 10
    And the interest addition is multiplied by 0.95

  # -----------------------------------------------------------
  # Emergency Loans
  # -----------------------------------------------------------

  Scenario: Running out of gold triggers emergency loan
    Given the player's gold drops below 0 at end of month
    Then the credit rating decreases by (1 - credit rating) / 3
    And the interest addition increases by 0.2
    And an emergency loan of |gold| * 1.1 is taken
    # The 1.1 multiplier is a 10% emergency negotiation fee

  Scenario: Bankruptcy when emergency loan fails
    Given the player's gold is negative
    And the emergency loan cannot cover the deficit
    Then the game ends with a bankruptcy message

  # -----------------------------------------------------------
  # Foreclosure
  # -----------------------------------------------------------

  Scenario: Foreclosure when debt-to-asset ratio exceeds limit
    Given the player has an outstanding loan
    And the net worth is calculated as total assets minus loan
    When the debt-to-asset ratio exceeds the debt support limit
    # The debt support limit is looked up from a table based on credit rating
    Then the bank forecloses
    And the game ends

  Scenario: Warning when approaching foreclosure
    Given the debt-to-asset ratio exceeds 80% of the debt support limit
    Then the bank issues a warning message

  # -----------------------------------------------------------
  # Overseers Fired When Gold is Negative
  # -----------------------------------------------------------

  Scenario: Negative gold causes overseers to quit
    Given the player has overseers
    And the player's gold drops below 0
    When a month is simulated
    Then all overseers are fired
    And the overseer pay increases by approximately 20% with slight random variance

  # -----------------------------------------------------------
  # Loan Messages (see initial-spec.md Messages section)
  # -----------------------------------------------------------

  Scenario: Credit check fee message
    Given the player requests a loan exceeding the credit limit
    When the credit check fee is offered
    Then a random credit-check-fee message is displayed from the pool
    And the message includes the fee amount
    # Pool contains ~10 variants, e.g. "It will cost you 500 to find out if you qualify."

  Scenario: Loan approval message
    Given the player passes the credit check
    When the loan is granted
    Then a random loan-approval message is displayed from the pool
    And the message includes the loan amount and interest rate
    # Pool contains ~15 variants, often sarcastic

  Scenario: Loan denial message
    Given the player fails the credit check
    When the loan is denied
    Then a random loan-denial message is displayed from the pool
    # Pool contains ~15 gleefully mocking variants

  Scenario: Loan repayment acknowledgment
    When the player fully repays the loan
    Then a random repayment message is displayed from the pool
    # Pool contains ~8 variants with humorous send-offs

  Scenario: Cash shortage message
    Given the player runs out of gold at end of month
    When the shortage is detected
    Then a random cash-shortage message is displayed from the pool
    # Pool contains ~10 variants, e.g. "You have run out of cash!"

  Scenario: Foreclosure message
    Given the bank forecloses on the player
    When the game ends
    Then a random foreclosure message is displayed from the pool
    # Pool contains ~15 variants escalating from warnings to game-over notices

  Scenario: Bankruptcy message when no loan available
    Given the player is out of gold and cannot get a loan
    When the game ends
    Then a random bankruptcy message is displayed from the pool
    # Pool contains ~10 variants, e.g. "The party's over."

  Scenario: Invalid loan input
    When the player enters non-numeric text in the loan dialog
    Then a random input-error message is displayed from the loan error pool
    # Pool contains ~5 variants, e.g. "This is a bank. We do things right. Now you try."
