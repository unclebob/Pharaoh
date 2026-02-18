Feature: Trading
  The player buys and sells commodities on the open market.
  Tradeable commodities are: wheat, slaves, oxen, horses, manure, and land.
  Market supply and demand limit transaction sizes.
  Livestock health affects the selling price.
  See initial-spec.md for full details.

  Background:
    Given the game is running

  # -----------------------------------------------------------
  # Buy / Sell Operations
  # -----------------------------------------------------------

  Scenario: Buy a commodity
    Given the player has 10000 gold
    And wheat costs 10 per bushel
    And the wheat supply is 500
    When the player buys 100 bushels of wheat
    Then the player's wheat should increase by 100
    And the player's gold should decrease by 1000
    And the wheat supply should decrease by 100

  Scenario: Sell a commodity
    Given the player has 200 bushels of wheat
    When the player sells 50 bushels of wheat at 10 per bushel
    Then the player's wheat should decrease by 50
    And the player's gold should increase by 500
    And the wheat supply should increase by 50

  Scenario: Cannot sell more than owned
    Given the player has 100 bushels of wheat
    When the player tries to sell 150 bushels of wheat
    Then the transaction is rejected
    And a message shows the player owns 100 bushels

  Scenario: Cannot spend more gold than available
    Given the player has 500 gold
    And wheat costs 10 per bushel
    When the player tries to buy 100 bushels of wheat
    Then the transaction is rejected
    And a message shows the player can afford 50 bushels

  # -----------------------------------------------------------
  # Supply and Demand Limits
  # -----------------------------------------------------------

  Scenario: Market runs out of supply
    Given the wheat supply is 30
    When the player tries to buy 100 bushels of wheat
    Then only 30 bushels are purchased
    And a message indicates the market is out of stock

  Scenario: Market refuses excess supply
    Given the wheat demand is 1000
    When the player tries to sell more than 1100 bushels of wheat
    Then the sale is capped at the maximum the market will absorb
    # Max supply the market accepts = demand * 1.1

  # -----------------------------------------------------------
  # Keep / Acquire Mode
  # -----------------------------------------------------------

  Scenario: Acquire adjusts to target amount
    Given the player has 200 slaves
    When the player acquires 534 slaves
    Then the system buys 334 additional slaves

  Scenario: Keep adjusts by selling excess
    Given the player has 500 bushels of wheat
    When the player keeps 300 bushels of wheat
    Then the system sells 200 bushels of wheat

  # -----------------------------------------------------------
  # Health-Adjusted Sell Price
  # -----------------------------------------------------------

  Scenario Outline: Selling livestock adjusts price by health
    Given the player has <count> <animal>
    And <animal> health is <health>
    And the <animal> market price is <price>
    When the player sells <sell_count> <animal>
    Then the gold received is <sell_count> * <price> * <health>

    Examples:
      | animal | count | health | price | sell_count |
      | slaves | 100   | 0.8    | 1000  | 50         |
      | horses | 20    | 0.5    | 500   | 10         |
      | oxen   | 50    | 0.9    | 300   | 25         |

  # -----------------------------------------------------------
  # Health of Purchased Livestock
  # -----------------------------------------------------------

  Scenario: Buying livestock blends health
    Given the player has 100 slaves with health 0.6
    When the player buys 50 slaves
    Then the new slaves have nominal health of approximately 0.8
    And the blended slave health is (50 * 0.8 + 100 * 0.6) / 150
    # Purchased livestock arrive with health near 0.8, with slight random variance

  # -----------------------------------------------------------
  # Selling Land with Crops
  # -----------------------------------------------------------

  Scenario: Selling planted land destroys crops proportionally
    Given the player has 100 acres of planted land
    And those acres have 2000 bushels of wheat sewn
    When the player sells 25 acres of planted land
    Then 25% of the wheat sewn on that land is destroyed
    And the player has 75 acres of planted land
    And 1500 bushels remain sewn

  Scenario: Selling fallow land does not affect crops
    Given the player has 200 acres of fallow land
    When the player sells 50 acres of fallow land
    Then no crops are affected
    And the player has 150 acres of fallow land

  # -----------------------------------------------------------
  # Trading Messages (see initial-spec.md Messages section)
  # -----------------------------------------------------------

  Scenario: Supply limit message when selling exceeds market capacity
    Given the market can only absorb 500 bushels of wheat
    When the player tries to sell 800 bushels
    Then a random supply-limit message is displayed from the pool
    And the message includes the maximum amount the market will accept
    # Pool contains ~10 variants, e.g. "I am afraid I can't accept any more than 500."

  Scenario: Demand limit message when buying exceeds supply
    Given the market only has 300 bushels in stock
    When the player tries to buy 500 bushels
    Then a random demand-limit message is displayed from the pool
    And the message includes the amount available
    # Pool contains ~10 variants, e.g. "I am afraid that I can only spare 300."

  Scenario: Insufficient funds message
    Given the player cannot afford the purchase
    When the player attempts to buy
    Then a random insufficient-funds message is displayed from the pool
    And the message includes the maximum the player can afford
    # Pool contains ~10 variants, e.g. "You only have the cash for 500."

  Scenario: Successful transaction message
    When a buy or sell transaction completes normally
    Then a random success message is displayed from the pool
    # Pool contains ~10 variants, e.g. "Thank you for buying my quality merchandise."

  Scenario: Invalid trading input
    When the player enters non-numeric text in the buy/sell dialog
    Then a random input-error message is displayed from the trading error pool
    # Pool contains ~10 humorous variants per dialog type
