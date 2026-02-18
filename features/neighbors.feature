Feature: Neighbors
  The player has four AI neighbors who visit with messages.
  Each has a distinct personality: good guy, bad guy, village idiot, banker.
  Neighbors give advice on game state, but reliability varies by personality.
  They also deliver idle messages, dunning notices, and random chats.
  See initial-spec.md Messages section for message pool details.

  Background:
    Given the game is running

  # -----------------------------------------------------------
  # Neighbor Personalities
  # -----------------------------------------------------------

  Scenario: Four distinct personalities assigned at game start
    When the game begins
    Then four faces are assigned
    And one face is the banker
    And one face is the good guy (truth-sayer)
    And one face is the bad guy (liar)
    And one face is the village idiot
    And no two personalities share the same face
    And assignments are randomized each new game

  # -----------------------------------------------------------
  # Advice System
  # -----------------------------------------------------------

  Scenario: The good guy gives accurate advice
    Given the good guy visits
    And slave health is below 0.6
    When he gives advice about slave health
    Then he says the slaves look bad (accurate)

  Scenario: The bad guy inverts advice
    Given the bad guy visits
    And slave health is below 0.6
    When he gives advice about slave health
    Then he says the slaves look great (inverted)

  Scenario: The village idiot gives random advice
    Given the village idiot visits
    And slave health is below 0.6
    When he gives advice about slave health
    Then he randomly says good or bad

  Scenario: The banker only makes small talk
    Given the banker visits for a chat
    Then he delivers generic chat messages
    And he never gives topical advice

  Scenario: 5% chance any neighbor flips accuracy
    Given any neighbor gives advice
    Then there is a 5% chance the advice meaning is inverted

  Scenario: 20% chance a chat is just small talk
    Given any neighbor visits
    Then there is a 20% chance the chat is generic regardless of game state

  # -----------------------------------------------------------
  # Advice Topics
  # -----------------------------------------------------------

  Scenario Outline: Neighbor advises on a game topic
    Given the topic is "<topic>"
    And the "<metric>" is "<condition>"
    When a neighbor gives advice
    Then the "<quality>" advice is selected

    Examples:
      | topic           | metric                  | condition | quality |
      | oxen feeding    | oxen feed rate          | < 50      | bad     |
      | oxen feeding    | oxen feed rate          | > 80      | good    |
      | horse feeding   | horse feed rate         | < 40      | bad     |
      | horse feeding   | horse feed rate         | > 65      | good    |
      | slave feeding   | slave feed rate         | < 5       | bad     |
      | slave feeding   | slave feed rate         | > 8       | good    |
      | overseers       | slave-to-overseer ratio | > 30      | bad     |
      | overseers       | slave-to-overseer ratio | < 15      | good    |
      | stress          | overseer pressure       | > 0.5     | bad     |
      | stress          | overseer pressure       | < 0.2     | good    |
      | fertilizer      | manure per acre         | < 2       | bad     |
      | fertilizer      | manure per acre         | 3.5 - 7   | good    |
      | slave health    | slave health            | < 0.6     | bad     |
      | slave health    | slave health            | > 0.9     | good    |
      | oxen health     | oxen health             | < 0.5     | bad     |
      | oxen health     | oxen health             | > 0.85    | good    |
      | horse health    | horse health            | < 0.5     | bad     |
      | horse health    | horse health            | > 0.85    | good    |
      | credit          | credit rating           | < 0.4     | bad     |
      | credit          | credit rating           | > 0.8     | good    |

  Scenario: Advice skipped if relevant resource is zero
    Given the topic is "oxen feeding"
    And the player has 0 oxen
    Then the advice is skipped and generic chat is shown instead

  # -----------------------------------------------------------
  # Idle Messages
  # -----------------------------------------------------------

  Scenario: Idle message after inactivity
    Given the player has been inactive for 60-90 seconds
    When the idle timer fires
    Then a random neighbor delivers an idle pep talk
    And the idle timer resets to 60-90 seconds

  Scenario: Activity cancels pending idle messages
    Given the player interacts with the game
    And more than 2 seconds have passed since the last idle check
    Then the idle nag is cancelled
    And the timer resets

  # -----------------------------------------------------------
  # Dunning Notices
  # -----------------------------------------------------------

  Scenario: Banker sends dunning notices when loan exists
    Given the player has an outstanding loan
    When the dunning timer expires
    Then the banker delivers a loan payment reminder
    And the next dunning interval depends on credit rating
    # Low credit = frequent notices (every 5 seconds)
    # High credit = rare notices (every 300 seconds)

  Scenario: Dunning cancelled when player makes a payment
    Given the player repays part of the loan
    Then the dunning timer is reset
    And the next dunning notice is postponed

  # -----------------------------------------------------------
  # Random Chats
  # -----------------------------------------------------------

  Scenario: Neighbors pop in for random chats
    Given 90-200 seconds have elapsed since the last chat
    When the chat timer fires
    Then a random neighbor visits
    And the chat may contain advice or be generic small talk
    And the chat timer resets to 90-200 seconds

  # -----------------------------------------------------------
  # Voice
  # -----------------------------------------------------------

  Scenario: Each neighbor has a distinct voice
    Given speech synthesis is available
    Then Face 1 speaks at rate 100, pitch 200
    And Face 2 speaks at rate 150, pitch 66
    And Face 3 speaks at rate 200, pitch 100
    And Face 4 speaks at rate 250, pitch 150

  Scenario: Default voice for unknown speaker
    Given a message is not attributed to a specific face
    Then the default voice is rate 190, pitch 310

  # -----------------------------------------------------------
  # Message Pools (see initial-spec.md Messages section)
  # -----------------------------------------------------------

  Scenario: Idle pep talk pool
    When a neighbor delivers an idle message
    Then the message is selected randomly from a pool of approximately 50 variants
    And messages range from gentle prods to pop-culture references
    # Examples: "Beam me up Scotty", "Boy are you ugly",
    # "OK, stand up for exercises. 20 jumping jacks, ready?"

  Scenario: Generic chat pool
    When a neighbor delivers generic small talk
    Then the message is selected randomly from a pool of approximately 20 variants
    And messages include jokes, observations, and game hints
    # Examples: "So, how ya doin there ol' buddy boy?",
    # "My brother in-law feeds his horses more than 90 bushels a month."

  Scenario: Advice message pools per topic
    Given a neighbor gives advice on a topic
    Then the good message is selected from a pool of approximately 6-15 variants for that topic
    And the bad message is selected from a separate pool of approximately 6-15 variants
    # Each of the 10 topics has its own good and bad message pools

  Scenario: Dunning notice pool with escalating severity
    When the banker delivers a dunning notice
    Then the message is selected randomly from a pool of approximately 40 variants
    And messages range from polite reminders to threats
    # Early examples: "May we respectfully remind you that you owe us some dough?"
    # Late examples: "Pay up your loan, or we'll break your legs"

  Scenario: All messages are spoken aloud
    Given speech synthesis is available
    When any neighbor message is displayed
    Then the message text is spoken using the delivering neighbor's voice settings
    And the message appears in a dialog box with the neighbor's face

  # -----------------------------------------------------------
  # Character Portraits
  # -----------------------------------------------------------

  Scenario: Character portraits loaded from PNG files
    Given the game starts
    Then four portrait images are loaded from resources/faces/man1.png through man4.png
    And each portrait is a black-and-white bitmap extracted from the original resource fork

  Scenario: Face message dialog displays portrait and text
    Given a neighbor with face 2 delivers a message "Your oxen look starved."
    Then a dialog box appears overlaying the game screen
    And the dialog contains the portrait for face 2 on the left
    And the message text "Your oxen look starved." appears on the right
    And pressing any key dismisses the dialog

  Scenario: Face messages block other key actions until dismissed
    Given a face message dialog is displayed
    When the player presses any key
    Then the message is dismissed
    And the key press is not processed as a game action
