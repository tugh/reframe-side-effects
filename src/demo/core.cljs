(ns demo.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [re-frame.core :refer [dispatch dispatch-sync inject-cofx reg-event-fx reg-event-db reg-cofx reg-fx reg-sub subscribe]]))

;; simple sub for username
(reg-sub
 :username
 (fn [db]
   (:username db)))

;; simple sub for registration date
(reg-sub
 :registered-at
 (fn [db]
   (-> db :registered-at str)))

;; profile page
(defn- profile
  []
  (let [username      @(subscribe [:username])
        registered-at @(subscribe [:registered-at])]
    [:div.grid.justify-items-center.my-20
     [:div.my-5 username]
     [:div registered-at]]))

;; getting current time is a co-effect because it's provided by the wild world
(reg-cofx
 :now
 (fn [cofx]
   (assoc cofx :now (js/Date.))))

;; changing url/history is an effect because we're triggering it
(reg-fx
 :change-path
 (fn [path]
   (js/window.history.pushState nil nil path)))

;; naive navigation implementation
(reg-event-fx
 :go-to-profile
 (fn [{:keys [db]}]
   {:db          (assoc db :page profile)
    :change-path "/profile"}))

;; event handler handler describing more than one effects. 1 changing db, 2 changing url
(reg-event-fx
 :register
 [(inject-cofx :now)] ; we need the current time for deciding registeration time
 (fn [{:keys [db now]} [_ username]]
   {:db       (assoc db :username      username
                        :registered-at now)
    :dispatch [:go-to-profile]}))

;; our lovely component
(defn- main
  []
  (r/with-let [username (r/atom "")] ; use reagent atom to prevent altering db on every change
    [:div.grid.justify-items-center.my-20
     [:input.border-solid.border-2.rounded.p-2.my-5
      {:on-change #(reset! username (-> % .-target .-value))}] ; update reagent atom on every change
     [:button.button.border-solid.border-2.rounded.p-2
      {:on-click #(dispatch [:register @username])} ; fire up register event for the given username
      "Register"]]))

;; simple sub for page component
(reg-sub
 :page
 (fn [db _]
   (:page db)))

(defn- router
  []
  (let [page @(subscribe [:page])]
    [page]))

(defn- ^:dev/after-load mount!
  []
  (rdom/render [router] (.getElementById js/document "app")))

(reg-event-db
 :init
 (fn [_ _]
   {:page main})) ; set current page to main

;; bring your app to initial state
(defn init!
  []
  (dispatch-sync [:init])
  (mount!))
