(ns monologue.backend.generator
  (:require [monologue.backend.mapper :as mmap]
            [monologue.backend.constant :refer [db-config]]
            [muuntaja.core :as muun]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as rrc]
            [reitit.ring.middleware.muuntaja :as rrm-muuntaja]
            [reitit.ring.middleware.parameters :as rrm-parameter]
            [reitit.coercion.spec]
            [ring.adapter.jetty :as jetty]
            [taoensso.timbre :as b]
            [hiccup2.core :as h]))


(def hic-head
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:http-equiv "X-UA-Compatible"
           :content    "IE=edge"}]
   [:meta {:name    "viewport"
           :content "width=device-width, initial-scale=1"}]
   [:title "monologue.me"]
   [:link {:rel  "stylesheet"
           :href "/css/mystyles.css"}]
   [:script {:src "https://use.fontawesome.com/releases/v5.3.1/js/all.js"}]])



(def hic-left
  [:div.column.is-one-fifth
   [:p [:span.icon-text
        [:span.icon [:i.fas.fa-file]]
        [:a {:href "/pages"} "페이지"]]]
   [:p [:span.icon-text
        [:span.icon [:i.fas.fa-tag]]
        [:a {:href "/tags"} "태그"]]]
   [:p [:span.icon-text
        [:span.icon [:i.fas.fa-comment]]
        [:a {:href "/comments"} "댓글"]]]])

(def hic-footer
  [:footer
   [:p [:span.icon-text
        [:span.icon [:i.fas.fa-copyright]]
        [:span "monologue.me"]]]])
(defn html-wrap [content]
  (str "<!DOCTYPE html>"
       (h/html [:html
                hic-head
                [:body
                 [:div.container.p-3
                  [:header
                   [:div.columns
                    [:div.column.is-one-fifth
                     [:header.mb-5
                      [:figure.image
                       [:img {:src "/css/roomel_coffee.jpg"}]]]]
                    [:div.column.has-text-right
                     [:p.title "monologue"]
                     [:p "snail"]]]]
                  [:div.columns
                   hic-left
                   [:div.column
                    [:section.section.mt-0.pt-0
                     (h/raw content)
                     ]]]
                  hic-footer]
                 ]])))

(defn html-content-pages [pieces years]
  (str (h/html
         [:div
          [:h2.title.strong "최근 페이지"]
          (map #(vec [:p [:a {:href (str "/page/" (% :subject))} (% :subject)]]) pieces)
          [:p "..."]]
         [:br]
         [:div.mt2
          [:h2.title.strong "모든 페이지"]
          (map #(vec [:p [:a {:href (str "/pages/" (% :substring))}
                          (% :substring)]
                      [:small.pl-3 "(" (% :count) ")"]]) years)])))

(defn html-content-pages-in-year [pieces year]
  (str (h/html
         [:div
          [:p.pb-3 [:span.icon-text
                    [:span.icon [:i.fas.fa-caret-left]]
                    [:a {:href "/pages"} "pages"]]]
          [:h2.title.strong year]
          (map #(vec [:p [:a {:href (str "/page/" (% :subject))} (% :subject)]]) pieces)]
         ))
  )

(defn html-content-page [piece]
  (str (h/html
         (if-not (nil? piece)
           [:div
            [:h1.title [:strong (piece :subject)]]
            [:h5.subtitle.mb-2 (piece :summary)]
            [:content (piece :content)]]
           [:div "not found"]))))



