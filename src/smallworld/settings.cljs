(ns smallworld.settings
  (:require [clojure.string         :as str]
            [goog.dom               :as dom]
            [reagent.core           :as r]
            [smallworld.decorations :as decorations]
            [smallworld.mapbox      :as mapbox]
            [smallworld.session     :as session]
            [smallworld.user-data   :as user-data]
            [smallworld.util        :as util]))

(def debug? false)
(defonce *locations-new  (r/atom :loading))
(defonce *minimaps       (r/atom {}))
(defonce *settings       (r/atom :loading))
(defonce *email-address  (r/atom :loading))
(defonce *form-errors    (r/atom {}))

(defn fetch-coordinates! [minimap-id location-name-input index]
  (if (str/blank? location-name-input)
    (.flyTo (get @*minimaps minimap-id) #js{:essential true ; this animation is essential with respect to prefers-reduced-motion
                                            :zoom 0})
    (util/fetch-post "/coordinates" {:location-name location-name-input}
                     (fn [result]
                       (.flyTo (get @*minimaps minimap-id)
                               #js{:essential true ; this animation is essential with respect to prefers-reduced-motion
                                   :zoom 3
                                   :center (mapbox/coords-to-mapbox-array result)})
                       (swap! *locations-new assoc index (merge (get @*locations-new index)
                                                                {:coords result}))))))

(def fetch-coordinates-debounced! (util/debounce fetch-coordinates! 200))

(def email_notifications_options ["instant" "daily" "weekly" "muted"])

(defn minimap [minimap-id location-name coords]
  (println "coords: ")
  (println coords)
  (r/create-class {:component-did-mount
                   (fn [] ; this should be called just once when the component is mounted
                     (swap! *minimaps assoc minimap-id
                            (new js/mapboxgl.Map
                                 #js{:container minimap-id
                                     :key    (get-in mapbox/config [mapbox/style :access-token])
                                     :style  (get-in mapbox/config [mapbox/style :style])
                                     :center (or (mapbox/coords-to-mapbox-array coords)
                                                 (clj->js mapbox/Miami))
                                     :interactive false ; makes the map not zoomable or draggable
                                     :attributionControl false ; removes the Mapbox copyright symbol
                                     :zoom 3
                                     :maxZoom 8
                                     :minZoom 0}))
                     ; zoom out if they haven't provided a location
                     (when (clojure.string/blank? location-name)
                       (.setZoom (get @*minimaps minimap-id) 0)))
                   :reagent-render (fn [] [:div {:id minimap-id}])}))

(defn location-field [{index         :index
                       auto-focus    :auto-focus
                       label         :label
                       placeholder   :placeholder
                       not-provided? :not-provided?
                       from-twitter? :from-twitter?
                       value         :value
                       coords        :coords
                       update!       :update!}]
  (let [id         (str "location-" index)
        minimap-id (str "minimap--" id)]
    [:div.field.location-field {:id id :key id}
     [:div.delete-location-btn {:title "delete this location"
                                :on-click #(when (js/confirm "are you sure you want to delete this location?")
                                             (reset! *locations-new (util/rm-from-list @*locations-new index)))}
      (decorations/cancel-icon)]
     [:label label]
     (when-not (and not-provided? from-twitter?)
       [:<> [:div
             [:input.location-input
              {:type "text"
               :tab-index (str index)
               :auto-focus auto-focus
               :id   (str id "-input")
               :key  (str id "-input")
               :name (str id "-input")
               :value value
               :autoComplete "off"
               :auto-complete "off"
               :on-change #(let [new-value  (-> % .-target .-value)
                                 _tmp       (fetch-coordinates-debounced! minimap-id new-value index)
                                ;;  new-coords (get @*saved-coords new-value)
                                 ]
                            ;;  (println "saved-coords: " @*saved-coords)
                            ;;  (println "  new-coords: " new-coords)
                             (update! new-value))
               :placeholder placeholder}]
             (decorations/edit-icon)]
        (when from-twitter? ; only the first two locations
          [:div.small-info-text {:style {:margin-bottom "12px"}}
           "don't worry, this won't update your Twitter profile :)"])
        [:div.mapbox-container
         [minimap minimap-id value coords]
         (when-not (clojure.string/blank? value)
           [:div.center-point])]
        [:br]])]))

(defn input-by-name [name & [additional-attrs]]
  (.querySelector js/document (str "input[name= \"" name "\"]"
                                   (or additional-attrs ""))))

(defn input-value-by-name [name]
  (let [input-elem (input-by-name name)]
    (when input-elem
      (.-value input-elem))))

(defn add-form-error [id error-msg] (swap! *form-errors assoc id error-msg))

(defn invalid-email? [email]
  (let [regex-pattern #"(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])"
        regex-result (re-matches regex-pattern email)]
    (nil? regex-result)))

(defn valid-inputs!? [{email_address       :email_address
                       email_notifications :email_notifications}]
  (reset! *form-errors {})

  ; email_notifications
  (when (contains? email_notifications_options email_notifications)
    (add-form-error :email_notifications (str "must be one of: " (str email_notifications_options))))

  ; email_address
  (cond
    (clojure.string/blank? email_address)
    (add-form-error :email-address-input "oops, email can't be blank!")

    (invalid-email? email_address)
    (add-form-error :email-address-input "oops, that's not a valid email address!"))

  ; global
  (when (> (count (keys @*form-errors))  0)
    (add-form-error :global "there was an error in the form"))

  (= 0 (count (keys @*form-errors))))


(defn submit-welcome-form []

  (let [new-settings {:email_address           (input-value-by-name "email-address-input")
                      :email_notifications     (.-id (input-by-name "email_notification" ":checked"))
                      :locations               @*locations-new
                      :welcome_flow_complete   true}]
    (when (valid-inputs!? new-settings) ; TODO: check that all of the locations are valid too (e.g. can't be blank)
      (swap! session/*store assoc :locations @*locations-new)
      ; TODO: send to backend to save it in db too – first, need a way to reflect this in backend, since right now we only have local storage as source of truth for the current user's session
      (reset! *settings new-settings)
      (util/fetch-post "/settings/update" new-settings user-data/recompute-friends))))

(defn location-label [location]
  (condp = (:special-status location)
    "twitter-location" (if (str/blank? (:name-initial-value location))
                         [:<> ; TODO: improve the UX of this state
                          "you haven't set a location on Twitter," [:br] "but that's okay!"
                          [:div.small-info-text {:style {:margin-top "12px"}}
                           "when you "  [:a {:href "https://twitter.com/settings/profile"} "update your location"] " on Twitter," [:br]
                           "Small World will automatically follow it for you"]]
                         [:<>
                          "based on your Twitter location, you’re in"  [:br]])

    "from-display-name" (if (str/blank? (:name-initial-value location))
                          [:<> ; TODO: improve the UX of this state
                           "we didn't find a destination in your" [:br]
                           "Twitter display name, but that's okay!"
                           [:div.small-info-text {:style {:margin-top "12px"}}
                            "when you " [:a {:href "https://twitter.com/settings/profile"} "add a destination"] " to the end of your " [:br]
                            "Twitter display name, Small World will " [:br]
                            "automatically follow it for you"]]
                          [:<>
                           "based on your display name, you’re in"  [:br]])

    "added-manually" "add a location to follow"))

(defn location-placeholder [location]
  (condp = (:special-status location)
    "twitter-location"  "what city do you live in?"
    "from-display-name" "any plans to travel?"
    "share a location"))

(defn welcome-flow-screen []
  (when (nil? @*email-address)
    (reset! *email-address (:email @session/*store)))
  [:div.welcome-flow
   [:<>
    [:div.title
     [:p.serif {:style {:font-size "1.3em" :margin-bottom "12px"}}
      "welcome to"]
     [:h1 {:style {:font-weight "bold" :font-size "2.2em"}}
      "Small World"]]]

   [:div.twitter-data-explanation
    [:div.explanation
     (decorations/twitter-icon)
     [:span "Small World looks at the name & location on your "
      [:a {:href "https://twitter.com/settings/profile" :target "_blank"} "Twitter profile"]
      " to find nearby friends"]]
    [:div.twitter-data
     [:img {:src (:profile_image_url_large @session/*store)}]
     [:div.right-side
      [:div.name     (:name @session/*store)]
      [:div.location (:main_location_corrected @*settings)]]]]

   (let [locations (remove nil? (:locations @session/*store))]
     (when (= :loading @*locations-new) ; TODO: clean this up, it's kinda hacky
       (reset! *locations-new (vec (map ; store the original value pulled from Twitter so we have a record of it in case the user edits it
                                    (fn [location] (merge location {:name-initial-value (:name location)}))
                                    locations))))

     [:div.location-fields ; TODO: add a way to delete locations from the list
      [:br]
      (when debug?
        [:pre {:style {:text-align "left"}} (util/preify @*locations-new)])
      (map-indexed (fn [i location]
                     (location-field
                      {:index         i
                       :auto-focus    (or (= 0 i) (= (:special-status location) "added-manually"))
                       :label         (location-label location)
                       :placeholder   (location-placeholder location)
                       :from-twitter? (or (= (:special-status location) "twitter-location")
                                          (= (:special-status location) "from-display-name"))
                       :not-provided? (str/blank? (:name-initial-value location))
                       :value         (or (:name location) "")
                       :coords        (:coords location)
                       :update!       (fn [new-name]
                                        (swap! *locations-new assoc i (merge location {:name new-name})))}))
                   @*locations-new)
      [:div#track-new-location-field
       {:on-click (fn []
                    (reset! *locations-new (vec
                                            (concat @*locations-new
                                                    [{:special-status "added-manually"
                                                      :name "" ; the value starts out blank
                                                      :coords nil}])))
                    ; scroll to the newly-added location field
                    (js/setTimeout #(.scrollIntoView
                                     (last (array-seq (goog.dom/getElementsByClass "location-field")))
                                     #js{:behavior "smooth" :block "center" :inline "center"})
                                   50))}
       (decorations/plus-icon "scale(0.15)") "follow another location"]
      [:div.small-info-text {:style {:margin-top "6px"}}
       "you can always add more locations later"]])
   [:br]
   [:div.email-options {:tab-index "3"}
    [:p "would you like email notifications" [:br] "when your friends are nearby? *"]
    [:div.radio-btns
     [:div.radio-btn
      [:input {:name "email_notification" :type "radio" :value "instant" :id "instant"}]
      [:label {:for "instant"} "yes, notify me immediately"]]
     [:div.radio-btn
      [:input {:name "email_notification" :type "radio" :value "daily" :id "daily"}]
      [:label {:for "daily"} "yes, send me daily digests"]]
     [:div.radio-btn
      [:input {:name "email_notification" :type "radio" :value "weekly" :id "weekly" :default-checked true}]
      [:label {:for "weekly"} "yes, send me weekly digests"]]
     [:div.radio-btn
      [:input {:name "email_notification" :type "radio" :value "muted" :id "muted"}]
      [:label {:for "muted"} "no, don't notify me by email"]]]
    [:br]]
   [:br]
   [:div.email-options {:class (when (:email-address-input @*form-errors) "error")}
    [:div.email-address
     [:label "what's your email address? *"] [:br]
     [:div.field
      [:input {:type "text"
               :tab-index "4"
               :id "email-address-input"
               :name "email-address-input"
               :value @*email-address ; TODO: this is a hack - do it the same way as (location-input) instead, i.e. remove the atom
               :autoComplete "off"
               :auto-complete "off"
               :on-change #(let [input-elem (.-target %)
                                 new-value  (.-value input-elem)]
                             (reset! *email-address new-value))
               :placeholder "email address"}]
      (decorations/edit-icon)]
     [:div.error-msg (:email-address-input @*form-errors)]]]
   [:br]
   [:button.btn {:on-click submit-welcome-form} "let's go!"]
   (when debug?
     [:br] [:br]
     [:button.btn {:on-click #(reset! *form-errors {})} "clear errors"]
     [:div {:style {:text-align "left"}}
      [:br]
      [:pre "@session/*store: \n" (util/preify @session/*store)]
      [:pre "@*locations-new: \n" (util/preify @*locations-new)]
      [:pre "@*form-errors:   \n" (util/preify @*form-errors)]])
   [:br] [:br] [:br] [:br] [:br]
   util/info-footer])