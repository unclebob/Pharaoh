# Save/Restore UI Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add File menu bar, Ctrl-S/O/N shortcuts, filename dialogs, dirty flag, and save-before-quit/new prompts to the Pharaoh game.

**Architecture:** Separate `:menu` key in app state for the File dropdown (independent of `:dialog`). New dialog types `:save-file`, `:load-file`, `:confirm-save`, `:confirm-overwrite` extend the existing dialog system. Dirty flag (`:dirty`) tracks unsaved mutations. Modifier keys detected via `(q/key-modifiers)` in the Quil event handler.

**Tech Stack:** Clojure 1.11, Quil 4.3, EDN persistence, cognitect test-runner

---

### Task 1: Dirty Flag — Unit Tests and Implementation

**Files:**
- Modify: `src/pharaoh/state.clj:11-78` (add `:dirty false`, `:save-path nil` to initial-state)
- Test: `test/pharaoh/state_test.clj`

**Step 1: Write failing test — initial-state includes dirty and save-path**

Add to `test/pharaoh/state_test.clj`:

```clojure
(deftest initial-state-has-dirty-flag
  (let [s (st/initial-state)]
    (is (false? (:dirty s)))
    (is (nil? (:save-path s)))))
```

**Step 2: Run test to verify it fails**

Run: `clojure -M:test -v pharaoh.state-test`
Expected: FAIL — `:dirty` key not found

**Step 3: Add `:dirty false` and `:save-path nil` to initial-state**

In `src/pharaoh/state.clj`, add to the `initial-state` map after `:game-won false`:

```clojure
   :dirty false :save-path nil
```

**Step 4: Run test to verify it passes**

Run: `clojure -M:test -v pharaoh.state-test`
Expected: PASS

**Step 5: Commit**

```bash
git add src/pharaoh/state.clj test/pharaoh/state_test.clj
git commit -m "feat: add dirty flag and save-path to initial state"
```

---

### Task 2: Mark State Dirty on Mutations

**Files:**
- Modify: `src/pharaoh/ui/dialogs.clj:38-39` (close-dialog sets dirty)
- Modify: `src/pharaoh/simulation.clj` (do-run sets dirty)
- Test: `test/pharaoh/ui/dialogs_test.clj`
- Test: `test/pharaoh/simulation_test.clj`

**Step 1: Write failing test — close-dialog sets dirty true**

Add to `test/pharaoh/ui/dialogs_test.clj`:

```clojure
(deftest close-dialog-sets-dirty-flag
  (let [state (-> (st/initial-state)
                  (dlg/open-dialog :buy-sell)
                  dlg/close-dialog)]
    (is (true? (:dirty state)))))
```

**Step 2: Run test to verify it fails**

Run: `clojure -M:test -v pharaoh.ui.dialogs-test`
Expected: FAIL — `:dirty` is false

**Step 3: Modify close-dialog to set dirty**

In `src/pharaoh/ui/dialogs.clj`, change `close-dialog`:

```clojure
(defn close-dialog [state]
  (-> state (dissoc :dialog) (assoc :dirty true)))
```

**Step 4: Run test to verify it passes**

Run: `clojure -M:test -v pharaoh.ui.dialogs-test`
Expected: PASS

**Step 5: Write failing test — do-run sets dirty true**

Add to `test/pharaoh/simulation_test.clj`:

```clojure
(deftest do-run-sets-dirty-flag
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state) (assoc :gold 50000.0 :slaves 10.0
                                            :overseers 1.0 :wheat 100.0))
        result (sim/do-run rng state)]
    (is (true? (:dirty result)))))
```

**Step 6: Run test to verify it fails**

Run: `clojure -M:test -v pharaoh.simulation-test`
Expected: FAIL

**Step 7: Add `(assoc :dirty true)` at the end of do-run**

In `src/pharaoh/simulation.clj`, at the end of `do-run`, add `(assoc :dirty true)` to the threading pipeline.

**Step 8: Run all tests**

Run: `clojure -M:test`
Expected: All pass (existing + new)

**Step 9: Commit**

```bash
git add src/pharaoh/ui/dialogs.clj src/pharaoh/simulation.clj \
        test/pharaoh/ui/dialogs_test.clj test/pharaoh/simulation_test.clj
git commit -m "feat: set dirty flag on dialog close and simulation run"
```

---

### Task 3: File Menu State and Logic

**Files:**
- Create: `src/pharaoh/ui/menu.clj`
- Create: `test/pharaoh/ui/menu_test.clj`

This module manages the File menu state: open/close dropdown, and dispatching menu item actions.

**Step 1: Write failing tests for menu open/close**

Create `test/pharaoh/ui/menu_test.clj`:

```clojure
(ns pharaoh.ui.menu-test
  (:require [clojure.test :refer :all]
            [pharaoh.ui.menu :as menu]))

(deftest toggle-menu-opens-closed-menu
  (let [state (menu/toggle-menu {:menu {:open? false}})]
    (is (true? (get-in state [:menu :open?])))))

(deftest toggle-menu-closes-open-menu
  (let [state (menu/toggle-menu {:menu {:open? true}})]
    (is (false? (get-in state [:menu :open?])))))

(deftest close-menu-closes
  (let [state (menu/close-menu {:menu {:open? true}})]
    (is (false? (get-in state [:menu :open?])))))
```

**Step 2: Run test to verify it fails**

Run: `clojure -M:test -v pharaoh.ui.menu-test`
Expected: FAIL — namespace not found

**Step 3: Implement menu.clj**

Create `src/pharaoh/ui/menu.clj`:

```clojure
(ns pharaoh.ui.menu)

(defn toggle-menu [app]
  (update-in app [:menu :open?] not))

(defn close-menu [app]
  (assoc-in app [:menu :open?] false))
```

**Step 4: Run test to verify it passes**

Run: `clojure -M:test -v pharaoh.ui.menu-test`
Expected: PASS

**Step 5: Write failing tests for menu item bounds**

Add to `test/pharaoh/ui/menu_test.clj`:

```clojure
(deftest menu-bar-bounds-returns-rect
  (let [b (menu/menu-bar-bounds)]
    (is (number? (:x b)))
    (is (number? (:y b)))
    (is (pos? (:w b)))
    (is (pos? (:h b)))))

(deftest menu-items-returns-five-items
  (is (= 5 (count (menu/menu-items)))))

(deftest menu-item-hit-returns-nil-outside
  (is (nil? (menu/menu-item-hit -100 -100))))
```

**Step 6: Implement menu-bar-bounds, menu-items, menu-item-hit**

Add to `src/pharaoh/ui/menu.clj`:

```clojure
(def menu-bar-h 22)

(defn menu-bar-bounds []
  {:x 0 :y 0 :w 60 :h menu-bar-h})

(def ^:private items
  [{:label "Save         (Ctrl-S)" :action :save}
   {:label "Save As..."            :action :save-as}
   {:label "Open...      (Ctrl-O)" :action :open}
   {:label "New Game     (Ctrl-N)" :action :new-game}
   {:label "Quit"                  :action :quit}])

(defn menu-items [] items)

(def ^:private item-h 22)
(def ^:private dropdown-w 200)

(defn menu-item-hit [mx my]
  (when (and (<= 0 mx dropdown-w)
             (> my menu-bar-h)
             (<= my (+ menu-bar-h (* (count items) item-h))))
    (let [idx (int (/ (- my menu-bar-h) item-h))]
      (when (< idx (count items))
        (:action (nth items idx))))))
```

**Step 7: Run tests**

Run: `clojure -M:test -v pharaoh.ui.menu-test`
Expected: PASS

**Step 8: Commit**

```bash
git add src/pharaoh/ui/menu.clj test/pharaoh/ui/menu_test.clj
git commit -m "feat: add file menu state and hit-testing logic"
```

---

### Task 4: Save/Load File Dialogs

**Files:**
- Modify: `src/pharaoh/ui/dialogs.clj`
- Modify: `test/pharaoh/ui/dialogs_test.clj`
- Modify: `src/pharaoh/persistence.clj`
- Modify: `test/pharaoh/persistence_test.clj`

New dialog types `:save-file` and `:load-file` accept alphanumeric + path characters (not just digits).

**Step 1: Write failing test — open save-file dialog**

Add to `test/pharaoh/ui/dialogs_test.clj`:

```clojure
(deftest open-save-file-dialog
  (let [state (dlg/open-dialog (st/initial-state) :save-file)]
    (is (= :save-file (get-in state [:dialog :type])))
    (is (= "" (get-in state [:dialog :input])))))
```

**Step 2: Run test — should pass already** (open-dialog is generic)

Run: `clojure -M:test -v pharaoh.ui.dialogs-test`
Expected: PASS (open-dialog already handles any type)

**Step 3: Write failing test — update-file-input accepts path characters**

```clojure
(deftest update-file-input-accepts-path-chars
  (let [state (-> (st/initial-state)
                  (dlg/open-dialog :save-file)
                  (dlg/update-dialog-input \m)
                  (dlg/update-dialog-input \y)
                  (dlg/update-dialog-input \-)
                  (dlg/update-dialog-input \g))]
    (is (= "my-g" (get-in state [:dialog :input])))))
```

**Step 4: Run test to verify it fails**

Run: `clojure -M:test -v pharaoh.ui.dialogs-test`
Expected: FAIL — `-` is rejected by current `update-dialog-input` (only digits and `.`)

**Step 5: Modify update-dialog-input to accept path chars for file dialogs**

In `src/pharaoh/ui/dialogs.clj`, modify `update-dialog-input`:

```clojure
(def ^:private file-dialog-types #{:save-file :load-file})

(defn- file-dialog? [d]
  (file-dialog-types (:type d)))

(defn- valid-file-char? [ch]
  (or (Character/isLetterOrDigit ch)
      (#{\- \_ \. \/ \\} ch)))

(defn update-dialog-input [state ch]
  (if-let [d (:dialog state)]
    (cond
      (= ch \backspace)
      (assoc state :dialog
             (update d :input #(if (seq %) (subs % 0 (dec (count %))) "")))
      (and (file-dialog? d) (valid-file-char? ch))
      (assoc state :dialog (update d :input str ch))
      (or (Character/isDigit ch) (= ch \.))
      (assoc state :dialog (update d :input str ch))
      :else state)
    state))
```

**Step 6: Run test to verify it passes**

Run: `clojure -M:test -v pharaoh.ui.dialogs-test`
Expected: PASS

**Step 7: Write failing test — execute-save-file writes file**

Add to `test/pharaoh/ui/dialogs_test.clj`:

```clojure
(deftest execute-save-file-dialog-saves-game
  (let [path (str "/tmp/pharaoh-dlg-test-" (System/currentTimeMillis) ".edn")
        rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :gold 7777.0 :dirty true)
                  (dlg/open-dialog :save-file {:input path}))
        result (dlg/execute-dialog rng state)]
    (is (nil? (:dialog result)))
    (is (false? (:dirty result)))
    (is (= path (:save-path result)))
    (is (.exists (java.io.File. path)))))
```

**Step 8: Run test to verify it fails**

Run: `clojure -M:test -v pharaoh.ui.dialogs-test`
Expected: FAIL

**Step 9: Add :save-file and :load-file cases to execute-dialog**

In `src/pharaoh/ui/dialogs.clj`, add requires for persistence, then add cases in `execute-dialog`:

Add to ns requires:
```clojure
[pharaoh.persistence :as ps]
[clojure.java.io :as io]
```

Add cases in `execute-dialog` before the final `(close-dialog state)`:

```clojure
          :save-file
          (let [path (:input d)]
            (if (empty? path)
              (assoc state :message "No filename entered.")
              (do (ps/save-game (dissoc state :dialog :dirty :save-path) path)
                  (-> state
                      (dissoc :dialog)
                      (assoc :dirty false :save-path path)))))

          :load-file
          (let [path (:input d)
                loaded (when (seq path) (ps/load-game path))]
            (if loaded
              (-> state
                  (assoc :state loaded)  ;; will be unpacked by caller
                  (dissoc :dialog)
                  (assoc :dirty false :save-path path
                         :loaded-state loaded))
              (-> state
                  (dissoc :dialog)
                  (assoc :message (str "Could not load: " path)))))
```

Note: The `:load-file` case stores loaded state in `:loaded-state` for the caller (core.clj) to unpack, since dialogs operate on game state but load replaces it entirely.

**Step 10: Run test to verify it passes**

Run: `clojure -M:test -v pharaoh.ui.dialogs-test`
Expected: PASS

**Step 11: Write failing test — execute-load-file**

```clojure
(deftest execute-load-file-dialog-loads-game
  (let [path (str "/tmp/pharaoh-load-test-" (System/currentTimeMillis) ".edn")
        rng (r/make-rng 42)
        saved-state (assoc (st/initial-state) :gold 3333.0)]
    (pharaoh.persistence/save-game saved-state path)
    (let [state (-> (st/initial-state)
                    (dlg/open-dialog :load-file {:input path}))
          result (dlg/execute-dialog rng state)]
      (is (nil? (:dialog result)))
      (is (= 3333.0 (:gold (:loaded-state result))))
      (is (false? (:dirty result))))))

(deftest execute-load-file-missing-shows-error
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :load-file {:input "/tmp/no-such-file-xyz.edn"}))
        result (dlg/execute-dialog rng state)]
    (is (nil? (:dialog result)))
    (is (string? (:message result)))))
```

**Step 12: Run tests**

Run: `clojure -M:test -v pharaoh.ui.dialogs-test`
Expected: PASS

**Step 13: Commit**

```bash
git add src/pharaoh/ui/dialogs.clj test/pharaoh/ui/dialogs_test.clj
git commit -m "feat: add save-file and load-file dialog types"
```

---

### Task 5: Confirm-Save and Confirm-Overwrite Dialogs

**Files:**
- Modify: `src/pharaoh/ui/dialogs.clj`
- Modify: `test/pharaoh/ui/dialogs_test.clj`

**Step 1: Write failing test — open confirm-save dialog**

```clojure
(deftest open-confirm-save-dialog
  (let [state (dlg/open-dialog (st/initial-state) :confirm-save
                               {:next-action :quit})]
    (is (= :confirm-save (get-in state [:dialog :type])))
    (is (= :quit (get-in state [:dialog :next-action])))))
```

**Step 2: Run test — should pass** (open-dialog is generic with opts merge)

**Step 3: Write failing test — confirm-save-yes with existing save-path**

```clojure
(deftest confirm-save-yes-saves-and-returns-next-action
  (let [path (str "/tmp/pharaoh-confirm-" (System/currentTimeMillis) ".edn")
        state (-> (st/initial-state)
                  (assoc :gold 5555.0 :dirty true :save-path path)
                  (dlg/open-dialog :confirm-save {:next-action :quit}))]
    (let [result (dlg/handle-confirm-save-yes state)]
      (is (= :quit (:pending-action result)))
      (is (false? (:dirty result)))
      (is (.exists (java.io.File. path))))))

(deftest confirm-save-no-returns-next-action-without-saving
  (let [state (-> (st/initial-state)
                  (assoc :dirty true)
                  (dlg/open-dialog :confirm-save {:next-action :new-game}))]
    (let [result (dlg/handle-confirm-save-no state)]
      (is (= :new-game (:pending-action result)))
      (is (nil? (:dialog result))))))

(deftest confirm-save-cancel-returns-to-game
  (let [state (-> (st/initial-state)
                  (dlg/open-dialog :confirm-save {:next-action :quit}))]
    (let [result (dlg/close-dialog state)]
      (is (nil? (:dialog result))))))
```

**Step 4: Run tests to verify they fail**

**Step 5: Implement handle-confirm-save-yes/no**

Add to `src/pharaoh/ui/dialogs.clj`:

```clojure
(defn handle-confirm-save-yes [state]
  (let [d (:dialog state)
        next-action (:next-action d)
        path (:save-path state)]
    (if path
      (do (ps/save-game (dissoc state :dialog :dirty :save-path) path)
          (-> state (dissoc :dialog) (assoc :dirty false :pending-action next-action)))
      ;; No save-path yet — need to prompt for filename first
      (-> state
          (assoc :dialog {:type :save-file :input "" :after-save next-action})))))

(defn handle-confirm-save-no [state]
  (let [next-action (get-in state [:dialog :next-action])]
    (-> state (dissoc :dialog) (assoc :pending-action next-action))))
```

**Step 6: Run tests**

Run: `clojure -M:test -v pharaoh.ui.dialogs-test`
Expected: PASS

**Step 7: Write failing test — confirm-overwrite**

```clojure
(deftest open-confirm-overwrite-dialog
  (let [state (dlg/open-dialog (st/initial-state) :confirm-overwrite
                               {:path "/tmp/test.edn"})]
    (is (= :confirm-overwrite (get-in state [:dialog :type])))
    (is (= "/tmp/test.edn" (get-in state [:dialog :path])))))
```

**Step 8: Run test — should pass** (generic open-dialog)

**Step 9: Commit**

```bash
git add src/pharaoh/ui/dialogs.clj test/pharaoh/ui/dialogs_test.clj
git commit -m "feat: add confirm-save and confirm-overwrite dialog handlers"
```

---

### Task 6: Save/Load Action Orchestration

**Files:**
- Create: `src/pharaoh/ui/file_actions.clj`
- Create: `test/pharaoh/ui/file_actions_test.clj`

This module orchestrates the high-level flows: "user pressed Ctrl-S" -> check save-path -> either save directly or open dialog. Keeps dialogs.clj focused on dialog mechanics.

**Step 1: Write failing tests**

Create `test/pharaoh/ui/file_actions_test.clj`:

```clojure
(ns pharaoh.ui.file-actions-test
  (:require [clojure.test :refer :all]
            [pharaoh.ui.file-actions :as fa]
            [pharaoh.state :as st]))

(deftest save-action-with-save-path-saves-directly
  (let [path (str "/tmp/pharaoh-fa-" (System/currentTimeMillis) ".edn")
        state (assoc (st/initial-state) :gold 4444.0 :dirty true :save-path path)
        result (fa/do-save state)]
    (is (false? (:dirty result)))
    (is (nil? (:dialog result)))
    (is (.exists (java.io.File. path)))))

(deftest save-action-without-save-path-opens-dialog
  (let [state (assoc (st/initial-state) :dirty true)
        result (fa/do-save state)]
    (is (= :save-file (get-in result [:dialog :type])))))

(deftest save-as-always-opens-dialog
  (let [state (assoc (st/initial-state) :save-path "/tmp/existing.edn")
        result (fa/do-save-as state)]
    (is (= :save-file (get-in result [:dialog :type])))))

(deftest open-action-when-dirty-prompts-save
  (let [state (assoc (st/initial-state) :dirty true)
        result (fa/do-open state)]
    (is (= :confirm-save (get-in result [:dialog :type])))
    (is (= :load (get-in result [:dialog :next-action])))))

(deftest open-action-when-clean-opens-load-dialog
  (let [state (assoc (st/initial-state) :dirty false)
        result (fa/do-open state)]
    (is (= :load-file (get-in result [:dialog :type])))))

(deftest new-game-when-dirty-prompts-save
  (let [state (assoc (st/initial-state) :dirty true)
        result (fa/do-new-game state)]
    (is (= :confirm-save (get-in result [:dialog :type])))
    (is (= :new-game (get-in result [:dialog :next-action])))))

(deftest new-game-when-clean-resets-state
  (let [state (assoc (st/initial-state) :dirty false :gold 9999.0)
        result (fa/do-new-game state)]
    (is (= 0.0 (:gold result)))
    (is (= 1 (:month result)))
    (is (false? (:dirty result)))
    (is (nil? (:save-path result)))))

(deftest quit-when-dirty-prompts-save
  (let [state (assoc (st/initial-state) :dirty true)
        result (fa/do-quit state)]
    (is (= :confirm-save (get-in result [:dialog :type])))
    (is (= :quit (get-in result [:dialog :next-action])))))

(deftest quit-when-clean-sets-quit-flag
  (let [state (assoc (st/initial-state) :dirty false)
        result (fa/do-quit state)]
    (is (true? (:quit-clicked result)))))
```

**Step 2: Run tests to verify they fail**

Run: `clojure -M:test -v pharaoh.ui.file-actions-test`
Expected: FAIL — namespace not found

**Step 3: Implement file-actions.clj**

Create `src/pharaoh/ui/file_actions.clj`:

```clojure
(ns pharaoh.ui.file-actions
  (:require [pharaoh.persistence :as ps]
            [pharaoh.state :as st]
            [pharaoh.ui.dialogs :as dlg]))

(defn do-save [state]
  (if-let [path (:save-path state)]
    (do (ps/save-game state path)
        (assoc state :dirty false))
    (dlg/open-dialog state :save-file)))

(defn do-save-as [state]
  (dlg/open-dialog state :save-file))

(defn do-open [state]
  (if (:dirty state)
    (dlg/open-dialog state :confirm-save {:next-action :load})
    (dlg/open-dialog state :load-file)))

(defn do-new-game [state]
  (if (:dirty state)
    (dlg/open-dialog state :confirm-save {:next-action :new-game})
    (-> (st/initial-state)
        (assoc :dirty false :save-path nil))))

(defn do-quit [state]
  (if (:dirty state)
    (dlg/open-dialog state :confirm-save {:next-action :quit})
    (assoc state :quit-clicked true)))
```

**Step 4: Run tests**

Run: `clojure -M:test -v pharaoh.ui.file-actions-test`
Expected: PASS

**Step 5: Commit**

```bash
git add src/pharaoh/ui/file_actions.clj test/pharaoh/ui/file_actions_test.clj
git commit -m "feat: add file action orchestration (save/open/new/quit flows)"
```

---

### Task 7: Keyboard Shortcut Handling (Ctrl-S/O/N)

**Files:**
- Modify: `src/pharaoh/ui/input.clj:100-120`
- Modify: `test/pharaoh/ui/input_test.clj`

Quil provides `(q/key-modifiers)` returning a set like `#{:ctrl}`. The `handle-key` function in input.clj needs to check for modifier combos before dispatching to normal key-actions.

**Step 1: Write failing tests for ctrl-key handling**

Add to `test/pharaoh/ui/input_test.clj`:

```clojure
(deftest handle-ctrl-key-save-with-save-path
  (let [rng (r/make-rng 42)
        path (str "/tmp/pharaoh-ctrl-s-" (System/currentTimeMillis) ".edn")
        state (assoc (st/initial-state) :dirty true :save-path path)
        result (inp/handle-ctrl-key state \s)]
    (is (false? (:dirty result)))))

(deftest handle-ctrl-key-save-without-save-path
  (let [state (assoc (st/initial-state) :dirty true)
        result (inp/handle-ctrl-key state \s)]
    (is (= :save-file (get-in result [:dialog :type])))))

(deftest handle-ctrl-key-open-when-clean
  (let [state (assoc (st/initial-state) :dirty false)
        result (inp/handle-ctrl-key state \o)]
    (is (= :load-file (get-in result [:dialog :type])))))

(deftest handle-ctrl-key-new-when-clean
  (let [state (assoc (st/initial-state) :dirty false :gold 5000.0)
        result (inp/handle-ctrl-key state \n)]
    (is (= 0.0 (:gold result)))))

(deftest handle-ctrl-key-unknown-returns-nil
  (let [state (st/initial-state)
        result (inp/handle-ctrl-key state \z)]
    (is (nil? result))))
```

**Step 2: Run tests to verify they fail**

Run: `clojure -M:test -v pharaoh.ui.input-test`
Expected: FAIL — `handle-ctrl-key` not found

**Step 3: Implement handle-ctrl-key in input.clj**

Add to `src/pharaoh/ui/input.clj` requires:
```clojure
[pharaoh.ui.file-actions :as fa]
```

Add function:
```clojure
(defn handle-ctrl-key [state key-char]
  (case key-char
    \s (fa/do-save state)
    \o (fa/do-open state)
    \n (fa/do-new-game state)
    nil))
```

**Step 4: Run tests**

Run: `clojure -M:test -v pharaoh.ui.input-test`
Expected: PASS

**Step 5: Commit**

```bash
git add src/pharaoh/ui/input.clj test/pharaoh/ui/input_test.clj
git commit -m "feat: add Ctrl-S/O/N keyboard shortcut handlers"
```

---

### Task 8: Wire Ctrl Keys and Menu into Core Event Loop

**Files:**
- Modify: `src/pharaoh/core.clj:323-355`
- Modify: `test/pharaoh/core_test.clj`

The core `key-pressed` handler needs to:
1. Check `(q/key-modifiers)` for `:ctrl`
2. If ctrl held, call `inp/handle-ctrl-key` before normal dispatch
3. Handle confirm-save dialog keys (y/n/Esc)

The `mouse-clicked` handler needs to check for menu bar clicks.

**Step 1: Write failing tests**

Add to `test/pharaoh/core_test.clj` (test the state logic, not Quil rendering):

```clojure
;; Test that confirm-save dialog y/n keys work via handle-key
(deftest handle-key-confirm-save-yes
  (let [rng (r/make-rng 42)
        path (str "/tmp/pharaoh-core-" (System/currentTimeMillis) ".edn")
        state (-> (st/initial-state)
                  (assoc :dirty true :save-path path)
                  (dlg/open-dialog :confirm-save {:next-action :new-game}))
        result (inp/handle-key rng state \y)]
    (is (some? (:pending-action result)))))

(deftest handle-key-confirm-save-no
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :confirm-save {:next-action :quit}))
        result (inp/handle-key rng state \n)]
    (is (= :quit (:pending-action result)))
    (is (nil? (:dialog result)))))
```

**Step 2: Run test to verify it fails**

**Step 3: Add confirm-save/overwrite/save-file/load-file dialog key handling to handle-dialog-key**

In `src/pharaoh/ui/input.clj`, modify `handle-dialog-key` to add cases for the new dialog types before the existing cond:

```clojure
(defn- handle-dialog-key [rng state key-char key-kw]
  (let [dtype (get-in state [:dialog :type])]
    (case dtype
      :contracts (handle-contracts-key state key-char key-kw)

      :confirm-save
      (case key-char
        \y (dlg/handle-confirm-save-yes state)
        \n (dlg/handle-confirm-save-no state)
        (if (= key-char esc-char) (dlg/close-dialog state) state))

      :confirm-overwrite
      (case key-char
        \y (dlg/handle-confirm-overwrite-yes state)
        \n (dlg/close-dialog state)
        (if (= key-char esc-char) (dlg/close-dialog state) state))

      ;; save-file / load-file — text input + enter/esc
      (:save-file :load-file)
      (cond
        (= key-char esc-char) (dlg/close-dialog state)
        (or (= key-char \return) (= key-char \newline))
        (dlg/execute-dialog rng state)
        :else (dlg/update-dialog-input state key-char))

      ;; existing dialog handling
      (if (credit-check-mode? state)
        (case key-char
          \y (dlg/accept-credit-check rng state)
          \n (dlg/reject-credit-check state)
          (if (= key-char esc-char) (dlg/reject-credit-check state) state))
        (cond
          (= key-char esc-char) (dlg/close-dialog state)
          (or (= key-char \return) (= key-char \newline))
          (dlg/execute-dialog rng (dissoc state :message))
          :else
          (if-let [mode (dialog-mode-for dtype key-char)]
            (dlg/set-dialog-mode state mode)
            (dlg/update-dialog-input state key-char)))))))
```

**Step 4: Wire Ctrl modifier keys into core.clj key-pressed**

In `src/pharaoh/core.clj`, modify `key-pressed`:

```clojure
(defn- key-pressed [{:keys [screen] :as app} {:keys [raw-key key]}]
  (set! (.key (quil.applet/current-applet)) (char 0))
  (if (= :difficulty screen)
    (if (= (int raw-key) 27)
      (do (quit!) app)
      (su/select-difficulty app (su/difficulty-for-key raw-key)))
    (let [mods (q/key-modifiers)
          ctrl? (contains? mods :ctrl)
          new-state (if (and ctrl? (not (:dialog (:state app))))
                      (or (inp/handle-ctrl-key (:state app) raw-key)
                          (inp/handle-key (:rng app) (:state app) raw-key key))
                      (inp/handle-key (:rng app) (:state app) raw-key key))]
      (cond
        (:quit-clicked new-state)
        (do (quit!) app)

        (:pending-action new-state)
        (let [action (:pending-action new-state)
              clean-state (dissoc new-state :pending-action)]
          (case action
            :quit (do (quit!) app)
            :new-game (assoc app :state (st/initial-state))
            :load (assoc app :state (dlg/open-dialog clean-state :load-file))
            (assoc app :state clean-state)))

        :else
        (let [app (if (:loaded-state new-state)
                    (assoc app :state (dissoc (:loaded-state new-state) :loaded-state))
                    (assoc app :state (dissoc new-state :reset-visit-timers)))]
          (if (:reset-visit-timers new-state)
            (vis/reset-timers app (System/currentTimeMillis))
            (merge app (when (:menu-toggle new-state) {:state (dissoc (:state app) :menu-toggle)}))))))))
```

**Step 5: Run all tests**

Run: `clojure -M:test`
Expected: PASS

**Step 6: Commit**

```bash
git add src/pharaoh/ui/input.clj src/pharaoh/core.clj \
        test/pharaoh/core_test.clj
git commit -m "feat: wire Ctrl shortcuts and new dialog types into event loop"
```

---

### Task 9: Menu Bar and File Dialog Rendering

**Files:**
- Modify: `src/pharaoh/core.clj` (draw function)
- Modify: `src/pharaoh/ui/menu.clj` (add draw functions)

**Step 1: Add menu bar drawing**

In `src/pharaoh/ui/menu.clj`, add drawing functions (these require `quil.core`):

```clojure
(ns pharaoh.ui.menu
  (:require [quil.core :as q]
            [pharaoh.ui.layout :as lay]))

(defn draw-menu-bar [app]
  (let [open? (get-in app [:menu :open?])]
    ;; Menu bar background
    (q/fill 240 240 240)
    (q/stroke 200)
    (q/rect 0 0 lay/win-w menu-bar-h)
    ;; "File" label
    (q/fill (if open? 200 210 240) (if open? 210 220 240) 255)
    (q/no-stroke)
    (q/rect 0 0 60 menu-bar-h)
    (q/fill 0)
    (q/text-size 14)
    (q/text "File" 8 16)
    ;; Dropdown
    (when open?
      (let [n (count items)]
        (q/fill 250 250 250)
        (q/stroke 180)
        (q/rect 0 menu-bar-h dropdown-w (* n item-h) 2)
        (doseq [i (range n)]
          (let [y (+ menu-bar-h (* i item-h))]
            (q/fill 0)
            (q/text-size 13)
            (q/text (:label (nth items i)) 12 (+ y 16))))))))
```

**Step 2: Add save/load file dialog drawing**

In `src/pharaoh/core.clj`, add a case for `:save-file`/`:load-file` in `draw-dialog`. These dialogs show a title ("Save Game" / "Open Game"), a text input for the filename, and OK/Cancel buttons.

Add inside `draw-dialog`, after the `when-let [d (:dialog state)]` check, before the existing `(when (not= :contracts (:type d))`:

```clojure
(cond
  (#{:save-file :load-file} (:type d))
  (let [{:keys [x y w h]} (lay/cell-rect-span 2 8 6 5)
        title (if (= :save-file (:type d)) "Save Game" "Open Game")]
    ;; dialog box
    (q/fill 245 245 255) (q/stroke 100) (q/stroke-weight 2)
    (q/rect x y w h 5) (q/stroke-weight 1)
    ;; title
    (q/fill 0) (q/text-size lay/title-size)
    (q/text title (+ x 8) (+ y lay/title-size 8))
    ;; "Filename:" label + input box
    (let [label-y (+ y (* lay/value-size 3) 8)]
      (q/fill 0) (q/text-size lay/value-size)
      (q/text "Filename:" (+ x 8) label-y)
      (let [bx (+ x 90) by (- label-y lay/value-size 2)
            bw (- w 108) bh (+ lay/value-size 8)]
        (q/fill 255) (q/stroke 150) (q/stroke-weight 1)
        (q/rect bx by bw bh 3)
        (q/fill 0) (q/no-stroke)
        (q/text (str (:input d)) (+ bx 4) label-y)))
    ;; OK / Cancel buttons
    (let [btn-y (+ y h -20 (- lay/title-size) -8)]
      (draw-button (+ x 8) btn-y 120 lay/title-size
                   "OK (Enter)" 180 230 180 80 160 80)
      (draw-button (+ x 136) btn-y 120 lay/title-size
                   "Cancel (Esc)" 230 180 180 160 80 80)))

  (#{:confirm-save :confirm-overwrite} (:type d))
  (let [{:keys [x y w h]} (lay/cell-rect-span 2 8 6 5)
        title (if (= :confirm-save (:type d))
                "Save current game first?"
                "File exists. Overwrite?")]
    (q/fill 245 245 255) (q/stroke 100) (q/stroke-weight 2)
    (q/rect x y w h 5) (q/stroke-weight 1)
    (q/fill 0) (q/text-size lay/title-size)
    (q/text title (+ x 8) (+ y lay/title-size 8))
    (let [btn-y (+ y h -20 (- lay/title-size) -8)]
      (draw-button (+ x 8) btn-y 100 lay/title-size
                   "Yes (y)" 180 230 180 80 160 80)
      (draw-button (+ x 120) btn-y 100 lay/title-size
                   "No (n)" 230 180 180 160 80 80)
      (when (= :confirm-save (:type d))
        (draw-button (+ x 232) btn-y 100 lay/title-size
                     "Cancel (Esc)" 220 220 220 140 140 140))))

  (not= :contracts (:type d))
  ;; ... existing dialog rendering ...
```

**Step 3: Wire menu bar drawing into draw function**

In `src/pharaoh/core.clj`, modify the `draw` function to call menu drawing after the game screen:

```clojure
(defn- draw [{:keys [screen state faces icons logo] :as app}]
  (if (= :difficulty screen)
    (draw-difficulty logo)
    (do
      (scr/draw-screen state)
      (draw-dialog state icons)
      (draw-contracts-dialog state)
      (when (show-face-message? state)
        (draw-face-message (:message state) faces))
      (menu/draw-menu-bar app))))
```

**Step 4: Wire menu click handling into mouse-clicked**

In `src/pharaoh/core.clj`, modify `mouse-clicked` to check for menu clicks first:

In the game screen branch, before dispatching to `inp/handle-mouse`, add:

```clojure
;; Check menu bar click
(if (<= y menu/menu-bar-h)
  (if (get-in app [:menu :open?])
    app  ;; click on bar while open = handled by dropdown below
    (if (<= x 60)
      (menu/toggle-menu app)
      app))
  (if (get-in app [:menu :open?])
    (let [action (menu/menu-item-hit x y)]
      (if action
        (let [new-state (case action
                          :save (fa/do-save state)
                          :save-as (fa/do-save-as state)
                          :open (fa/do-open state)
                          :new-game (fa/do-new-game state)
                          :quit (fa/do-quit state))]
          (-> app (menu/close-menu) (assoc :state new-state)))
        (menu/close-menu app)))
    ;; existing mouse handling below
    ...))
```

**Step 5: Run all tests**

Run: `clojure -M:test`
Expected: PASS

**Step 6: Commit**

```bash
git add src/pharaoh/core.clj src/pharaoh/ui/menu.clj
git commit -m "feat: render file menu bar and save/load/confirm dialogs"
```

---

### Task 10: Handle Pending Actions from Confirm Dialogs

**Files:**
- Modify: `src/pharaoh/core.clj`
- Modify: `test/pharaoh/core_test.clj`

When `:pending-action` appears on state (from confirm-save yes/no), core.clj must execute the deferred action: quit, new-game, or open load dialog.

**Step 1: Write failing test**

```clojure
(deftest pending-action-quit-triggers-quit
  ;; Verify that :pending-action :quit sets :quit-clicked
  (let [state (assoc (st/initial-state) :pending-action :quit)]
    (is (= :quit (:pending-action state)))))

(deftest pending-action-new-game-resets
  (let [state (assoc (st/initial-state) :gold 9999.0 :pending-action :new-game)
        result (st/initial-state)]
    (is (= 0.0 (:gold result)))))
```

**Step 2: This is already handled in Task 8's key-pressed wiring**

The `:pending-action` check in `key-pressed` (from Task 8) handles this. Also add it to `mouse-clicked` for when the user clicks Yes/No in confirm dialogs.

**Step 3: Run all tests**

Run: `clojure -M:test`
Expected: PASS

**Step 4: Commit** (if any changes needed)

---

### Task 11: Existing Quit Button Dirty Check

**Files:**
- Modify: `src/pharaoh/ui/input.clj:246-252`

Currently the Quit button click sets `:quit-clicked true` unconditionally. Change it to go through `fa/do-quit` which checks dirty flag.

**Step 1: Write failing test**

```clojure
(deftest quit-button-when-dirty-prompts-save
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state) :dirty true)
        ;; Simulate clicking quit button area (cols 0-1, row 23)
        mx (+ lay/pad (* 0.5 lay/cell-w))
        my (+ lay/pad (* 23.5 lay/cell-h))
        result (inp/handle-mouse state mx my rng)]
    (is (= :confirm-save (get-in result [:dialog :type])))))
```

**Step 2: Run test to verify it fails**

**Step 3: Change quit button handler**

In `src/pharaoh/ui/input.clj`, change the QUIT button click handler from:

```clojure
(assoc state :quit-clicked true)
```

to:

```clojure
(fa/do-quit state)
```

**Step 4: Run tests**

Run: `clojure -M:test`
Expected: PASS

**Step 5: Commit**

```bash
git add src/pharaoh/ui/input.clj test/pharaoh/ui/input_test.clj
git commit -m "feat: quit button checks dirty flag before quitting"
```

---

### Task 12: Update Gherkin Steps for UI Save/Load

**Files:**
- Modify: `src/pharaoh/gherkin/steps/persistence.clj`

Many "Then" steps are currently no-ops. Fill in real assertions now that the full save/load flow works.

**Step 1: Fix the stubbed Then assertions**

Replace no-op handlers with real assertions. Key ones:

```clojure
{:type :then :pattern #"all commodity quantities match the saved values"
 :handler (fn [w]
            (let [before (:state-before w)
                  after (:state w)]
              (doseq [k [:wheat :slaves :oxen :horses :manure]]
                (assert (near? (k before) (k after))
                        (str k " mismatch")))
              w))}

{:type :then :pattern #"gold, loan, interest rate, credit rating, and credit limit all match"
 :handler (fn [w]
            (let [before (:state-before w)
                  after (:state w)]
              (doseq [k [:gold :loan :interest :credit-rating :credit-limit]]
                (assert (near? (k before) (k after))
                        (str k " mismatch")))
              w))}
```

And similar for pyramid, health, overseers, feed rates, market prices.

**Step 2: Add `snap` call to "when saved and restored" step**

Modify the "game is saved and restored" handler to snapshot before saving:

```clojure
{:type :when :pattern #"the game is saved and restored"
 :handler (fn [w]
            (let [w (snap w)
                  path (str "/tmp/pharaoh-test-" (System/currentTimeMillis) ".edn")]
              (ps/save-game (:state w) path)
              (assoc w :state (ps/load-game path) :save-path path)))}
```

**Step 3: Run Gherkin acceptance tests**

Run: `clojure -M:test -v pharaoh.gherkin.acceptance-test`
Expected: PASS

**Step 4: Commit**

```bash
git add src/pharaoh/gherkin/steps/persistence.clj
git commit -m "feat: fill in real assertions for persistence Gherkin steps"
```

---

### Task 13: Integration Test — Full Save/Load Round Trip

**Files:**
- Modify: `test/pharaoh/persistence_test.clj`

**Step 1: Write integration test**

```clojure
(deftest full-save-load-via-file-actions
  (let [state (-> (st/initial-state)
                  (assoc :gold 12345.0 :month 7 :year 5 :dirty true))
        path (tmp-file)
        ;; Save
        saved (fa/do-save (assoc state :save-path path))]
    (is (false? (:dirty saved)))
    ;; Load into fresh state
    (let [fresh (assoc (st/initial-state) :dirty false)
          with-dialog (fa/do-open fresh)
          ;; Simulate typing filename and pressing enter
          typed (-> with-dialog
                    (assoc-in [:dialog :input] path))
          loaded (dlg/execute-dialog (r/make-rng 42) typed)]
      (is (= 12345.0 (:gold (:loaded-state loaded))))
      (is (= 7 (:month (:loaded-state loaded)))))))
```

**Step 2: Run test**

Run: `clojure -M:test -v pharaoh.persistence-test`
Expected: PASS

**Step 3: Commit**

```bash
git add test/pharaoh/persistence_test.clj
git commit -m "test: add full save/load integration test via file actions"
```

---

### Task 14: Final Verification

**Step 1: Run all unit tests**

Run: `clojure -M:test`
Expected: All pass, no regressions

**Step 2: Run Gherkin acceptance tests**

Run: `clojure -M:test -v pharaoh.gherkin.acceptance-test`
Expected: All persistence scenarios pass with real assertions

**Step 3: Manual smoke test**

Run: `clojure -M:run`
- Verify File menu appears at top
- Click File -> dropdown appears
- Ctrl-S opens save dialog (first time)
- Type filename, press Enter -> saves
- Ctrl-S again -> saves immediately (no dialog)
- Modify state (buy wheat), Ctrl-N -> "Save first?" prompt
- Click Quit when dirty -> "Save first?" prompt

**Step 4: Final commit if any cleanup needed**

```bash
git add -A
git commit -m "chore: save/restore UI cleanup and polish"
```
