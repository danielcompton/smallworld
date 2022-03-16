(ns smallworld.user-data
  (:require [reagent.core   :as r]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [smallworld.util :as util]
            [smallworld.mapbox :as mapbox]
            [smallworld.session :as session]
            [smallworld.decorations :as decorations]))

(defonce *friends (r/atom :loading))

(defn closer-than [max-distance dist-key]
  (fn [friend]
    (let [smallest-distance (get-in friend [:distance dist-key])]
      (and (< smallest-distance max-distance)
           (not (nil? smallest-distance))))))

(defn render-user [k user]
  (let [twitter-pic    (:profile_image_url_large user)
        twitter-name   (:name user)
        twitter-handle (:screen-name user)
        twitter-link   (str "http://twitter.com/" twitter-handle)
        twitter-href   {:href twitter-link :target "_blank" :title "Twitter"}
        first-location (first (:locations user)) ; consider pulling from the "Twitter location" location or from the nearest location to the current user, instead of simply pulling the first location in the array
        lat            (when first-location (:lat (:coords first-location)))
        lng            (when first-location (:lng (:coords first-location)))]
    [:div.friend {:key twitter-name}
     [:a twitter-href
      [:div.twitter-pic [:img {:src twitter-pic :key k}]]]
     [:div.right-section
      [:a.top twitter-href
       [:span.name twitter-name]
       [:span.handle "@" twitter-handle]]
      [:div.bottom
       [:a {:href (str "https://www.google.com/maps/search/" lat "%20" lng "?hl=en&source=opensearch")
            :title "Google Maps"
            :target "_blank"}
        [:span.location (:name first-location)]]]]]))

(defn get-close-friends [distance-key max-distance]
  (->> @*friends
       (sort-by #(get-in % [:distance distance-key]))
       (filter (closer-than max-distance distance-key))))

(def *collapsed? (r/atom {:main-main false
                          :main-name false
                          :name-main false
                          :name-name false}))

(defn render-friends-list [friends-list-key verb-gerund location-name]
  (let [friends-list      (if (= :loading @*friends)
                            []
                            (get-close-friends friends-list-key 100))
        list-count        (count friends-list)
        friend-pluralized (if (= list-count 1) "friend is" "friends are")
        collapsed?        (get @*collapsed? friends-list-key)]

    [util/error-boundary
     [:div.friends-list
      (if (= :loading @*friends)
        [:div.loading
         (decorations/simple-loading-animation)
         "the first time takes a while to load"]

        (if (> list-count 0)
          [:<>
           [:p.location-info
            {:on-click ; toggle collapsed state
             #(swap! *collapsed? assoc friends-list-key (not collapsed?))}
            (decorations/triangle-icon (clojure.string/join " "  ["caret" (if collapsed? "right" "down")]))
            [:<>
             list-count " "
             friend-pluralized " "
             verb-gerund " " location-name ":"]]
           (when-not collapsed?
             [:div.friends (map-indexed render-user friends-list)])]

          [:div.no-friends-found
           (decorations/x-icon)
           "0 friends are " verb-gerund " " location-name]))]]))

(defn refresh-friends []
  (util/fetch "/friends/refresh"
              (fn [result]
                (doall (map (mapbox/remove-friend-marker) @mapbox/markers))
                (reset! *friends result)
                (mapbox/add-friends-to-map @*friends @session/*store))))

(defn recompute-friends []
  (util/fetch "/friends/recompute"
              (fn [result]
                (doall (map (mapbox/remove-friend-marker) @mapbox/markers))
                (reset! *friends result)
                (mapbox/add-friends-to-map @*friends @session/*store))))