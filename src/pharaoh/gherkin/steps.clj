(ns pharaoh.gherkin.steps
  (:require [pharaoh.gherkin.steps.setup :as setup]
            [pharaoh.gherkin.steps.pyramid :as pyramid]
            [pharaoh.gherkin.steps.events :as events]
            [pharaoh.gherkin.steps.loans :as loans]
            [pharaoh.gherkin.steps.trading :as trading]
            [pharaoh.gherkin.steps.health :as health]
            [pharaoh.gherkin.steps.feeding :as feeding]
            [pharaoh.gherkin.steps.planting :as planting]
            [pharaoh.gherkin.steps.economy :as economy]
            [pharaoh.gherkin.steps.contracts :as contracts]
            [pharaoh.gherkin.steps.neighbors :as neighbors]
            [pharaoh.gherkin.steps.persistence :as persistence]
            [pharaoh.gherkin.steps.workload :as workload]
            [pharaoh.gherkin.steps.overseers :as overseers]
            [pharaoh.gherkin.steps.input-validation :as input-validation]
            [pharaoh.gherkin.steps.generic :as generic]))

;; Generic catch-all patterns MUST be last — they use broad
;; regexes that would shadow specific patterns in domain files.
(defn all-steps []
  (vec (concat (setup/steps) (pyramid/steps) (events/steps)
               (loans/steps) (trading/steps) (health/steps)
               (feeding/steps) (planting/steps) (economy/steps)
               (contracts/steps) (neighbors/steps)
               (persistence/steps) (workload/steps)
               (overseers/steps) (input-validation/steps)
               (generic/steps))))
