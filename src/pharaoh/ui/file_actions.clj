(ns pharaoh.ui.file-actions
  (:require [pharaoh.persistence :as ps]
            [pharaoh.state :as st]
            [pharaoh.ui.dialogs :as dlg]))

(defn do-save [state]
  (if-let [path (:save-path state)]
    (do (ps/save-game (dissoc state :dialog :dirty :save-path :pending-action) path)
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

(defn do-load-file [state filename]
  (let [path (ps/save-path filename)
        loaded (ps/load-game path)]
    (if loaded
      (assoc state :dirty false :save-path path :loaded-state loaded)
      (assoc state :message (str "Could not load: " filename)))))

(defn do-quit [state]
  (if (:dirty state)
    (dlg/open-dialog state :confirm-save {:next-action :quit})
    (assoc state :quit-clicked true)))
