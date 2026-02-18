(ns pharaoh.messages
  (:require [pharaoh.random :as r]))

(defn pick [rng pool]
  (nth pool (long (r/uniform rng 0 (- (count pool) 0.001)))))

;; --- Idle pep talk messages ---
(def idle-messages
  ["Beam me up Scotty."
   "Boy are you ugly."
   "What did one eye say to the other? There's something between us that smells."
   "OK, stand up for exercises. 20 jumping jacks, ready?"
   "Are you still there?"
   "Hello? Is anybody home?"
   "You could at least pretend to be working."
   "The pharaoh sleeps while Rome burns."
   "I've seen faster pyramids built by termites."
   "My grandmother could build a pyramid faster than you."
   "Do something! Anything!"
   "Time is money, and you're wasting both."
   "The slaves are getting restless. So am I."
   "Even the oxen look bored."
   "A watched pyramid never grows."
   "Tick tock, tick tock..."
   "Your pyramid isn't going to build itself."
   "I'm not getting any younger here."
   "The Nile isn't going to wait forever."
   "Did you fall asleep?"
   "Wake up! There's a kingdom to run!"
   "I could build a better pyramid with my eyes closed."
   "The sun is moving faster than you are."
   "Are we having fun yet?"
   "I've seen glaciers move faster."
   "Your subjects are starting a pool on when you'll do something."
   "The camels are laughing at you."
   "I think your hourglass is broken."
   "Is this a game or a screensaver?"
   "The pharaoh contemplates... and contemplates... and..."
   "You know, pyramids don't grow on trees."
   "Hello? McFly? Anybody home?"
   "Life moves pretty fast. You should try it sometime."
   "I'll be back. Try to do something while I'm gone."
   "Houston, we have a problem. The pharaoh is asleep."
   "Your pyramid-building skills are... theoretical."
   "The desert sand is more productive than you."
   "I'd offer advice, but you'd have to be doing something first."
   "The gods grow impatient."
   "Your legacy is currently a pile of sand."
   "Even the sphinx is more animated than you."
   "Is there a doctor in the house? The pharaoh seems comatose."
   "I bet you can't even spell 'pyramid'."
   "The slaves have started a book club. You should join."
   "Fun fact: this game has an ending. You should try to reach it."
   "The rats are starting to look comfortable."
   "Your advisor died of boredom."
   "Meanwhile, in a parallel universe, a pyramid is being built."
   "The market closes in... oh wait, you're not doing anything anyway."
   "I spy with my little eye... nothing happening."])

;; --- Generic chat messages ---
(def chat-messages
  ["So, how ya doin there ol' buddy boy?"
   "Nice day, isn't it?"
   "Hey, how about those Crocks?"
   "My brother in-law feeds his horses more than 90 bushels a month."
   "Did you hear about the pharaoh who walked into a bar?"
   "Weather's nice for pyramid building."
   "I hear the Nile is lovely this time of year."
   "Have you tried the new camel races?"
   "My wife says I spend too much time here."
   "These sandstorms are murder on my complexion."
   "You know what they say about pyramids..."
   "Business is business, am I right?"
   "I just came by to say hello."
   "How's the family?"
   "Did I ever tell you about my cousin in Memphis?"
   "The market's been crazy lately."
   "I was just in the neighborhood."
   "Some weather we're having, eh?"
   "Have you heard the latest gossip?"
   "Just checking in on my favorite pharaoh."])

;; --- Advice messages (good and bad per topic) ---
(def advice-messages
  {:good-ox-feed ["Your oxen look well-fed." "The oxen are thriving on that diet."
                   "Keep feeding the oxen like that." "Fine looking oxen you've got."
                   "The oxen are in top condition." "Excellent oxen management."]
   :bad-ox-feed ["Your oxen look starved." "Those oxen need more food."
                  "The oxen are wasting away." "Feed your oxen before they die."
                  "I've seen healthier oxen in a drought." "Your oxen are skin and bones."]
   :good-hs-feed ["Your horses are well-nourished." "Those are some fine horses."
                   "The horses look magnificent." "Good horse feeding strategy."
                   "Your horses could win races." "The horses are in peak form."]
   :bad-hs-feed ["Your horses are starving." "Feed those horses more."
                  "The horses look terrible." "Those nags need more grain."
                  "Your horses are too thin." "The horses need better care."]
   :good-sl-feed ["The slaves are well-fed." "Your slaves look healthy and strong."
                   "Good nutrition for the workforce." "The slaves are eating well."
                   "Keep the slaves fed like that." "Strong workers, well nourished."]
   :bad-sl-feed ["Your slaves are starving." "The slaves need more food."
                  "Feed your workers or they'll die." "The slaves look malnourished."
                  "You're starving your labor force." "Those slaves need food badly."]
   :good-overseers ["Good overseer-to-slave ratio." "Your oversight is well-managed."
                     "The overseers have things under control." "Solid management structure."]
   :bad-overseers ["You need more overseers." "The slaves outnumber the overseers badly."
                    "Your oversight is dangerously thin." "Hire more overseers before trouble starts."]
   :good-stress ["The overseers are calm and effective." "Low stress, good management."
                  "Your overseers are relaxed." "Things are running smoothly."]
   :bad-stress ["The overseers are stressed out." "Overseer pressure is dangerously high."
                 "Your overseers are cracking the whip too hard." "Stress is building dangerously."]
   :good-fertilizer ["Good fertilizer coverage." "Your fields are well-manured."
                      "Excellent soil management." "The crops should yield well."]
   :bad-fertilizer ["Your fields need more manure." "The soil is depleted."
                     "Spread more fertilizer." "Your crops will suffer without manure."]
   :good-sl-health ["The slaves look healthy." "Your workforce is in good shape."
                     "Healthy slaves are productive slaves." "The slaves are thriving."]
   :bad-sl-health ["The slaves look sick." "Your slaves are in terrible health."
                    "The workforce is deteriorating." "Slave health is dangerously low."]
   :good-ox-health ["The oxen are in good health." "Healthy oxen make for good plowing."
                     "Your oxen are strong." "The oxen look great."]
   :bad-ox-health ["The oxen are sick." "Your oxen need medical attention."
                    "Oxen health is declining." "Those oxen don't look so good."]
   :good-hs-health ["The horses are healthy." "Your horses are in fine form."
                     "Strong, healthy horses." "The horses look excellent."]
   :bad-hs-health ["The horses are ill." "Your horses need care."
                    "Horse health is declining rapidly." "Those horses look sick."]
   :good-credit ["Your credit rating is excellent." "The bank loves you."
                  "Your financial standing is strong." "Good credit management."]
   :bad-credit ["Your credit rating is terrible." "The bank is losing patience."
                 "Your finances are a mess." "You'd better improve your credit."]})

;; --- Dunning notice messages ---
(def dunning-messages
  ["May we respectfully remind you that you owe us some dough?"
   "About that little loan you took out..."
   "The bank would like to discuss your outstanding balance."
   "Your payment is overdue. Again."
   "We notice you haven't made a payment lately."
   "The interest on your loan is piling up."
   "A friendly reminder about your financial obligations."
   "Don't forget about your loan!"
   "The bank is getting a little nervous."
   "Your credit rating is suffering. Make a payment."
   "We're not a charity, you know."
   "Pay up or else!"
   "The bank demands payment."
   "Your loan is becoming a problem."
   "We've been very patient, but..."
   "The board is discussing your account."
   "Last warning before we send Olaf."
   "Olaf is sharpening his axe."
   "Olaf! Here boy."
   "Pay up your loan, or we'll break your legs."
   "The bank is considering foreclosure."
   "Your assets are looking very attractive to us."
   "We know where you live."
   "Have you considered selling something to pay us back?"
   "The bank requires immediate payment."
   "Your account is seriously delinquent."
   "We're adding this conversation to your file."
   "The bank's patience has limits."
   "Consider this your final notice."
   "We're sending a representative to collect."
   "Your loan is the talk of the banking district."
   "Even the pharaoh's bank has standards."
   "Would you like to discuss a payment plan?"
   "The interest alone could buy a small pyramid."
   "Your financial advisor sends his condolences."
   "The bank appreciates your creative approach to debt."
   "We've seen camels with better credit."
   "Perhaps you should consider a yard sale."
   "The bank is losing sleep over your account."
   "Tick tock. The bank's clock is running."])

;; --- Event narration messages ---
(def event-messages
  {:locusts ["Locusts devour your crops! Everything is destroyed!"
             "A swarm of locusts descends upon your fields!"
             "The sky darkens with locusts. Your crops are gone."]
   :plagues ["A terrible plague sweeps through your estate!"
              "Disease runs rampant among your livestock and workers!"
              "Pestilence strikes! Your people and animals are suffering."]
   :health ["A mysterious illness affects your livestock."
             "Sickness spreads through your estate."
             "Your workers and animals fall ill."
             "Bad water causes widespread illness."
             "A fever runs through the slave quarters."]
   :workload ["The gods demand a temple be repaired."
               "A flood has damaged roads and requires repair work."
               "Extra labor is needed to shore up the river banks."
               "The irrigation canals need emergency repairs."
               "A sandstorm has buried equipment that must be dug out."]
   :labor ["Overseers are disgruntled. They strike for a %d%% raise."
            "Your overseers demand higher wages!"
            "Labor unrest! The overseers want more pay."
            "The overseers threaten to quit without a raise."]
   :wheat ["A horrible blight destroys %d%% of your crops."
            "Weevils infest your grain stores!"
            "Rot has spread through the wheat."
            "A fungus attacks your crops."]
   :gold ["Thieves break into your treasury and take %d%%."
           "Corrupt officials embezzle %d%% of your gold."
           "Bandits raid your treasury!"
           "Tax collectors take %d%% of your gold."]
   :economy ["A solar eclipse causes hoarding and panic."
              "Trade disruptions shake the markets."
              "A neighboring kingdom's collapse affects prices."
              "Currency speculation causes wild price swings."
              "Foreign traders flood the market."]})

;; --- Acts of God word pools ---
(def aog-adjectives
  ["an incredibly large" "an unpredicted" "a devastating" "a catastrophic"
   "an enormous" "a terrible" "an unprecedented" "a massive"])

(def aog-disasters
  ["volcano" "earthquake" "flood" "tsunami" "sandstorm"
   "tornado" "hurricane" "lightning storm"])

(def aog-consequences
  ["devastated your property!" "decimated your land!"
   "destroyed everything in its path!" "laid waste to your estate!"
   "left nothing but rubble!" "wiped out your holdings!"])

;; --- Acts of Mobs word pools ---
(def aom-crowds
  ["a huge crowd" "an immense gathering" "a mob" "a horde"
   "an angry multitude" "a seething mass" "a rioting throng"])

(def aom-populations
  ["peasants" "villagers" "workers" "citizens" "foreigners"
   "nomads" "malcontents"])

(def aom-motivations
  ["social injustice" "animal abuse" "your ugly face" "high taxes"
   "bad working conditions" "religious fervor" "pure boredom"])

(def aom-actions
  ["held a rock concert on your fields" "set fire to your crops"
   "trampled your estate" "looted your stores" "ran amok through your land"
   "stampeded through your property" "made a real mess of things"])

;; --- War messages ---
(def war-attackers
  ["the Hittites" "the Nubians" "the Libyans" "the Sea Peoples"
   "a neighboring kingdom" "barbarian raiders" "desert nomads"])

(def war-win-messages
  ["Victory! You gained %d%% in the battle."
   "Your forces triumph! You capture %d%% more resources."
   "A glorious victory! %d%% gains across the board."])

(def war-lose-messages
  ["Defeat! You lost %d%% of your resources."
   "Your forces are routed. %d%% losses."
   "A crushing defeat. You lose %d%%."])

;; --- Revolt messages ---
(def revolt-messages
  ["Your slaves are moved to revolt. You lose %d%%."
   "The slaves rise up! %d%% destruction."
   "A slave uprising destroys %d%% of your holdings."
   "Revolt! The slaves destroy %d%% of everything."
   "The oppressed strike back. %d%% losses."])

;; --- Loan messages ---
(def credit-check-messages
  ["It will cost you %.0f to find out if you qualify."
   "A credit check will run you %.0f gold."
   "We'll need %.0f for the paperwork."
   "The fee for reassessing your credit is %.0f."
   "That'll be %.0f to check your eligibility."])

(def loan-approval-messages
  ["Loan of %.0f approved at %.1f%% interest."
   "Here's your %.0f gold. Interest: %.1f%%."
   "Approved! %.0f at %.1f%%. Don't spend it all in one place."
   "The bank graciously lends you %.0f at %.1f%%."
   "%.0f gold, %.1f%% interest. Try not to lose it all."])

(def loan-denial-messages
  ["Sorry, your credit is terrible. Application denied."
   "Ha! You think we'd lend YOU money?"
   "Denied. Come back when you have assets."
   "The bank laughs at your application."
   "No. Just no. Your credit is atrocious."])

(def loan-repayment-messages
  ["Loan fully repaid! Well done."
   "Congratulations on paying off your debt!"
   "The bank thanks you for your prompt repayment."
   "You're debt-free! Don't let it go to your head."])

;; --- Cash shortage messages ---
(def cash-shortage-messages
  ["You have run out of cash!"
   "Your treasury is empty!"
   "No more gold! Emergency measures needed."
   "The coffers are bare. We're taking out an emergency loan."
   "You're broke. The bank is stepping in."])

;; --- Foreclosure messages ---
(def foreclosure-messages
  ["The bank forecloses on your estate. Game over."
   "Foreclosure! You lose everything."
   "The bank has had enough. Your assets are seized."
   "Your debt has overwhelmed you. The bank takes all."
   "Game over. The bank owns everything now."])

;; --- Win messages ---
(def win-messages
  ["Congratulations! Your pyramid reaches the heavens!"
   "You did it! The pyramid is complete!"
   "A magnificent achievement! The pyramid stands tall!"
   "The gods smile upon your pyramid!"
   "Your legacy is secured. The pyramid is finished!"])

;; --- Game over messages ---
(def game-over-messages
  ["The party's over."
   "Your reign ends in disgrace."
   "Another pharaoh bites the dust."
   "History will not remember you kindly."
   "Game over. Better luck next time."])

;; --- Opening messages ---
(def opening-messages
  ["How high can you build your pyramid in 20 years?"
   "Welcome, Pharaoh! Your kingdom awaits."
   "If you want to hear something clever, start the game over."
   "Let the pyramid building begin!"
   "The desert awaits your command, mighty Pharaoh."])

;; --- Contract messages ---
(def contract-default-messages
  ["Your trading partner has defaulted on the contract."
   "The counterparty broke the deal!"
   "Contract cancelled due to counterparty default."
   "Your trading partner backed out."])

(def contract-partial-pay-messages
  ["The counterparty can only make a partial payment this month."
   "Partial payment received. They're short on gold."
   "Only a fraction of the payment came through."])

(def contract-partial-ship-messages
  ["The counterparty could only ship part of the goods."
   "Partial delivery this month."
   "Only some of the goods were delivered."])

(def contract-complete-messages
  ["Contract fulfilled successfully!"
   "The deal is done. Contract complete."
   "Contract finished. All obligations met."])

;; --- Trading messages ---
(def supply-limit-messages
  ["I am afraid I can't accept any more than %.0f."
   "The market can only absorb %.0f units."
   "Supply limit reached. Maximum: %.0f."])

(def insufficient-funds-messages
  ["You can't afford that!"
   "Not enough gold for this purchase."
   "Your treasury can't cover this transaction."])

;; --- Input error messages ---
(def input-error-messages
  ["This is a bank. We do things right. Now you try."
   "That's not a valid number."
   "Please enter a proper amount."
   "Try again with a real number."
   "Invalid input. Numbers only, please."])
