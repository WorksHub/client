(ns wh.components.bubbles.views
  (:require [cljsjs.react-draggable]
            [clojure.set :refer [difference]]
            [clojure.string :as string]
            [physicsjs :as physics]
            [reagent.core :as reagent]))

(def draggable (reagent/adapt-react-class js/ReactDraggable))

;; We need to know the bubble radii in cljs, to be able to pass them to
;; the physics engine. But at the same time we also need them in SASS,
;; to define animations.

;; There used to be a hack here to pass that information from styles
;; to cljs code, but it turned out it doesn't work on Safari and other
;; Webkit-based browsers. So, begrudgingly, here it comes repeated.

;; Whenever you change these, be sure to change _bubbles.sass to match.

(def base-radius 52)
(def like-radius 64)
(def hover-radius 63)

(def radius-cycle {like-radius hover-radius, hover-radius like-radius, base-radius like-radius})
(def radius-cycle-hover {base-radius hover-radius, hover-radius base-radius, like-radius like-radius})
(def css-class-map {base-radius "base", like-radius "like", hover-radius "hover"})
(def size->radius (zipmap (mapv keyword (vals css-class-map)) (keys css-class-map)))

(def rows 3)         ; how many rows they initially come in
(def gap 20)         ; vertical spacing
(def velocity 1)  ; initial horizontal velocity
(def resize-threshold 10) ; don't resize when moved more than this many pixels
(def spreadout 0.95) ; how much spread apart they are initially, in screens

(defn initial-positions
  [balls]
  (let [mid-x 0
        mid-y 0
        diameter (* base-radius 2)
        base-attrs {:radius base-radius, :vy 0, :mass 1, :restitution 0.4, :cof 5}
        apart (* spreadout js/window.innerWidth 0.5)
        spread (fn [balls direction]
                 (for [[x column] (map-indexed vector (partition-all rows balls))
                       :let [rows (count column)
                             row-span (+ (* rows diameter) (* (dec rows) gap))
                             starting-y (- mid-y (/ row-span 2))]
                       [y {:keys [name size]}] (map-indexed vector column)]
                   (merge base-attrs
                          {:x (+ base-radius (+ mid-x (* direction (+ apart (* x (+ diameter gap))))))
                           :y (+ base-radius (+ starting-y (* y (+ diameter gap))))
                           :vx (* (- direction) velocity)
                           :name name
                           :radius (size->radius size)})))
        [left right] (split-at (/ (count balls) 2) balls)]
    (concat (spread left -1) (spread right 1))))

(defn add-bubbles [world bubbles]
  (doseq [bubble bubbles]
    (.add world (physics/body "circle" (clj->js bubble))))
  world)

(defn make-world
  [captions]
  (doto (physics)
    (add-bubbles (initial-positions captions))
    (.add (physics/behavior "attractor" (clj->js {:pos {:x 0 :y 0}, :strength 0.05, :order 1})))
    (.add (physics/behavior "body-collision-detection"))
    (.add (physics/behavior "body-impulse-response"))
    (.add (physics/behavior "sweep-prune"))))

(defn get-ball-by-name [world name]
  (first (filter #(= (.-name %) name) (.getBodies world))))

(defn move-ball! [world name x y]
  (-> world (get-ball-by-name name) .-state .-pos (set! (physics/vector x y))))

(defn translate-ball! [world name dx dy]
  (-> world (get-ball-by-name name) .-state .-pos (.add dx dy)))

(defn stop-ball! [world name]
  (let [state (-> world (get-ball-by-name name) .-state)]
    (set! (.-vel state) (physics/vector 0 0))
    (set! (.-acc state) (physics/vector 0 0))))

(defn resize-ball! [world name radius]
  (let [ball (get-ball-by-name world name)]
    (.options ball #js {:radius radius})
    (set! (-> ball .-geometry .-radius) radius)))

(defn cycle-ball-size! [world name]
  (let [old-radius (.-radius (get-ball-by-name world name))
        new-radius (radius-cycle old-radius base-radius)]
    (resize-ball! world name new-radius)
    (keyword (css-class-map new-radius))))

(defn cycle-ball-size-hover! [world name]
  (let [old-radius (.-radius (get-ball-by-name world name))
        new-radius (radius-cycle-hover old-radius base-radius)]
    (resize-ball! world name new-radius)))

(defn balls-coords [world]
  (mapv #(let [pos (-> % .-state .-pos)]
           [(.-x pos) (.-y pos) (.-radius %) (.-name %)])
        (.getBodies world)))

(defn event-position [^js/Event ev]
  (if-let [x (.-screenX ev)]
    [x (.-screenY ev)]
    (if-let [touches (.-changedTouches ev)]
      (let [t (aget touches 0)]
        [(.-screenX t) (.-screenY t)]))))

(defn ball [x y radius name world update-state options]
  (reagent/with-let [start-drag (reagent/atom nil)
                     prevent-resize (reagent/atom false)
                     {:keys [on-size-change]} options]
    [draggable
     {:position #js {:x 0 :y 0}
      :onStart #(reset! start-drag (event-position %))
      :onStop #(let [[start-x start-y] @start-drag
                     [stop-x stop-y] (event-position %)
                     dx (- stop-x start-x)
                     dy (- stop-y start-y)
                     moved-by (js/Math.sqrt (+ (* dx dx) (* dy dy)))]
                 (reset! prevent-resize (> moved-by resize-threshold))
                 (translate-ball! world name dx dy)
                 ;; Zero velocity/acceleration to prevent the momentum from throwing
                 ;; the ball away immediately. It will slowly start gravitating
                 ;; towards the centre of the mass again.
                 (stop-ball! world name)
                 ;; Sync re-frame ball positions with world now, rather than
                 ;; at the next animation frame, to prevent flickering.
                 ;; Somewhat hacky, but otherwise we'd have to use DraggableCore
                 ;; and do our own updating of bubble positions – much more verbose.
                 (update-state)
                 (reagent/flush))}
     [:div.bubble {:style {:left x :top y}
                   :class (css-class-map radius)
                   :on-click #(do
                                (when-not @prevent-resize
                                  (let [new-size (cycle-ball-size! world name)]
                                    (when on-size-change
                                      (on-size-change name new-size))))
                                (reset! prevent-resize false))
                   :on-mouse-enter #(cycle-ball-size-hover! world name)
                   :on-mouse-leave #(cycle-ball-size-hover! world name)}
      name]]))

(defn bubbles-internal
  [world state dimensions options]
  (let [update-state #(reset! state (balls-coords world))
        tick #(do (.step world) (update-state))]
    (fn [world state]
      (reagent/next-tick tick)
      (let [[width height] @dimensions
            midx (/ width 2)
            midy (/ height 2)]
        (into
         [:div.bubble-container]
         (for [[x y r n] @state]
           [ball (+ x midx) (+ y midy) r n world update-state options]))))))

(defn bubbles [captions & {:as options}]
  (let [world (make-world captions)
        state (reagent/atom [])
        dimensions (reagent/atom [0 0])]
    (reagent/create-class
     {:reagent-render (fn []
                        [bubbles-internal world state dimensions options])
      :component-did-mount
      (fn [this [_ v]]
        (let [width  (.-offsetWidth (reagent/dom-node this))
              height (.-offsetHeight (reagent/dom-node this))]
          (reset! dimensions [width height])
          (.add world (physics/behavior "edge-collision-detection"
                                        #js {:aabb (physics/aabb -50000 (- (/ height 2))
                                                                 50000 (/ height 2))}))))
      :component-will-receive-props
      (fn [this [_ v]]
        (let [new-bubbles (set (map :name v))
              old-bubbles (set (map :name (second (reagent/argv this))))
              diff (difference new-bubbles old-bubbles)
              added (filter #(diff (:name %)) v)]
          (add-bubbles world (initial-positions added))))})))
