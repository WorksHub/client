# Layout Guidelines

---
**NOTE**

This document is a work in progress and is by no means complete. Please talk to
a member of WorksHub dev team on #workshub in the Clojurians Slack for
clarification on anything here that you think is missing or doesn't make sense.

---

## Background

Prior to mid 2019, the client was suffering from a lot of repetition each time a
new page was created. Often, split (two-thirds, one-third) layouts were remade
every time, each with subtly different rules and classes. In order to start
applying some coherence we started to develop re-usable styles and this document
talks about some of them and the reasons why. We do not currently have a style
guide; this document is a precursor.

## Quick Start

If you wanted to create a new page today there is one question to answer first:
split layout or full width? There are other questions, obviously, but for the
purposes of getting started quickly, this is all we need.

### Split layout

This is our preferred layout for pages which have a core interaction but also
want to present relevant information that complements the core.

``` clojure
[:div.main.my-view
  ;; header/full-width content here
  [:h1 "Welcome to my view"]
  [:div.split-content
    [:div.split-content__main.my-view__main
      [:h2 "First section"]
      [:section.split-content-section
        ;; section for primary content
      ]]
    [:div.split-content__side.my-view__side
      [:section.split-content-section
        ;; section for auxillary content
      ]]]]
```

Replace `my-view` with the name of your view.

The rules for this are implemented in `_layout.sass`. It should present almost
everything you need for a basic, responsive split layout. Padding, margins etc.
