(ns wh.privacy-policy.views)

(defn page []
  [:div.main.privacy
   [:h1 "Privacy policy"]
   [:div.container
    [:ol
     [:li
      [:span.heading "Introduction"]
      [:ol
       [:li [:p "The WorksHub Privacy Policy (\"Policy\") sets out what, why and how personal data are collected and used by WorksHub (aka Functional Works Ltd.) [and its affiliates] ([collectively] “WorksHub”, \"we\". \"us\", \"our\") when you visit any of our online job matching websites (e.g. FunctionalWorks) (\"Websites\").  By continuing to use these Websites, you consent to your information being processed in accordance with this Policy.    All Data Subjects provide consent of which we keep record.  Data Subjects have ability to withdraw consent.\n"]]
       [:li [:p "If you have questions or need to contact us about this Privacy Policy, please email us at "
             [:span [:a {:href "mailto:hello@functionalworks.com"} "hello@functionalworks.com"]]
             " or our Data Controller/DPO, Antony Woods at "
             [:span [:a {:href "mailto:antony@functionalworks.com"} "antony@functionalworks.com"]]]]]]

     [:li
      [:span.heading "What personal information we collect and how we use it"]
      [:ol
       [:li [:p "How we collect personal information and what sort of personal information:  You may give us information about you via your account (\"Account\") on our Websites. Your Account requires a valid e-mail address as a login, along with other information which will be of a personal nature, your name, and professional experience. This information is mandatory and is required to set up your Account.  We also use cookies to collect personal information (see below)."]]
       [:li [:p "Why we collect personal information:  Your information will be used by WorksHub for job matching purposes. Once you have created your Account, Employers (being individuals, partnerships or companies that are searching for candidates) registered on our Websites will be able to approach you if they wish to invite you for an interview if they feel that you may be suitable for a vacancy, based on information they are able to view (see below)."]]]]

     [:li
      [:span.heading "Sensitive personal information"]
      [:ol
       [:li [:p "As part of the registration process, we do not require you to provide any sensitive personal information. Sensitive personal information means data revealing your racial / ethnic origin, political opinions, religious / philosophical beliefs, trade union membership, genetic / biometric data, data concerning health, data concerning sex life, data concerning sexual orientation and data concerning the commission or alleged commission of any offence.  To the extent that you voluntarily supply any sensitive information when registering on our Websites, such sensitive personal information will be handled in accordance with this Policy."]]]]

     [:li
      [:span.heading "How we share and disclose your personal information"]
      [:ol
       [:li [:p "All Companies registered on our Websites will have access to your information. By creating an Account on any of our Websites, you accept and acknowledge that an Employer may contact you if they wish to interview you for a job. Certain pieces of personal information will be released to the Employer during this process, such as the contents of any volunary documents you provided as part of your application (e.g. resume, cover letter), your name and email address. We take steps to ensure that the Employers respect your privacy and treat your personal information in a confidential manner, with all companies signing terms that agree to treat your personal data in a confidential way.  For more information on how the Employer processes your personal information, please refer to the " [:a {:href "/terms-of-service"} "Terms of Service"]]]
       [:li [:p "WorksHub engages certain onward subprocessors that may process personal data submitted to WorksHub’s services. These subprocessors are listed below, and may be updated by WorksHub from time to time:"]
        [:ul
         [:li "Algolia - we use internally, for our team to search through users"]
         [:li "Amazon Web Services - we send backups of our data to S3"]
         [:li "Clearbit - we lookup associated companies based on user emails"]
         [:li "Google - events (which could include user data) is received via Analytics and stored in BigQuery"]
         [:li "Heroku - hosts our application, so user data passes through their servers"]
         [:li "Hubspot - we use this as our main CRM when communicating with users"]
         [:li "Mixpanel - we use to analyze user behaviour on our site"]
         [:li "MongoDB Atlas - hosts our databases, where user data is stored"]
         [:li "Neverbounce - we use to check for spam email addresses/domains"]
         [:li "Segment - we use as a proxy for analytics events (which could include user data)"]
         [:li "Sendgrid - we use to send transactional emails"]
         [:li "Sentry - we use to collect application errors (which could include user data)"]
         [:li "Slack - we use to notify ourselves and external partners about certain user activity (applications, articles etc)"]
         [:li "Stream (getstream.io) - we use to power the 'feed' feature in our application"]]]
       [:li [:p "We may also share your information with third parties if required by a court order or any legal or regulatory requirement."]]]]

     [:li
      [:span.heading "Links to other websites"]
      [:ol
       [:li [:p "When providing us with your Account information, you may voluntarily supply us with a link to your social media website page (i.e. your GitHub, LinkedIn, Twitter or Facebook profile page). Please note that this information will be made available to potential Employers. We therefore suggest that you consider this when adding links to your social media website pages to your Account.\n"]]]]

     [:li
      [:span.heading "Data Retention"]
      [:ol
       [:li
        [:p "We will only retain your personal information for as long as necessary to fulfil the purposes we collected it for, including for the purposes of satisfying any legal, accounting, or reporting requirements."]]
       [:li [:p "To determine the appropriate retention period for personal information, we consider the amount, nature, and sensitivity of the personal information, the potential risk of harm from unauthorised use or disclosure of your personal information, the purposes for which we process your personal information and whether we can achieve those purposes through other means, and the applicable legal requirements."]]]]

     [:li
      [:span.heading "Your rights"]
      [:ol
       [:li [:p "You can access the personal information via your Account to obtain a copy of it and to correct, amend, or delete information that is inaccurate. You can also delete your Account altogether. You also have right to request the transfer of your  personal data to another party."]]
       [:li [:p "You have the right to complain to our Data Controller/DPO via our internal complaints policy.  However, if you wish to obtain a copy of personal information that we hold or request it be amended, deleted etc.  you please contact us at the address "
             [:span [:a {:href "mailto:hello@functionalworks.com"} "hello@functionalworks.com"]]
             ". Before we are able to respond to your request we may ask you to verify your identity and to provide further details about your request. We will endeavour to respond within an appropriate timeframe and, in any event, within any timescales required by law."]]
       [:li [:p "It is your responsibility to ensure that you submit true, accurate and complete information on your Account and keep it up to date."]]]]

     [:li
      [:span.heading "Information security"]
      [:ol
       [:li [:p "We apply appropriate security measures to protect your personal information that is under our control, from unauthorised access, collection, use, disclosure, copying, modification or disposal. All information you provide to us is stored on our secure servers. Where you have a password which enables you to access our Websites, you are responsible for keeping this password secure and confidential."]]]]

     [:li
      [:span.heading "Internet-based transfers"]
      [:ol
       [:li [:p "Given that the Internet is a global environment, using the Internet to collect and process personal information necessarily involves the transmission of data on an international basis. While we store all of the personal information that we collect about you through our Websites in the UK, it is possible that [your personal information may be transmitted to parties outside the European Economic Area (\"EEA\"). For instance, is it possible that emails between you and an Employer may be sent via a third party service that stores emails outside the EEA. Therefore, by browsing any of our Websites, you acknowledge our processing of personal information in this way. ] However, we will continue to use your personal information securely, lawfully and in the manner we describe in this Policy."]]]]

     [:li
      [:span.heading "Cookies"]
      [:ol
       [:li [:p "We collect information automatically through the use of \"cookies\". A cookie is a text file containing small amounts of information that a website can send to your browser, which may then be stored on your computer as an anonymous tag that identifies your computer but not you. Some of our Websites pages use cookies, sent by WorksHub to better serve you when you return to our Websites. Cookies are used for session management only on our Websites. You can set your browser to notify you before you receive a cookie, giving you the chance to decide whether to accept it. You can also set your browser to turn off cookies; however, if you do this, some parts of some of our Websites may not work properly."]]
       [:li [:p "For information about the specific cookies used on this website, please see below:"]]]

      [:table.table
       [:thead
        [:tr
         [:th "Type of cookie"]
         [:th "Who serves these cookies"]
         [:th "How to refuse"]]]
       [:tbody
        [:tr
         [:td [:p "Essential website cookies: These cookies are strictly necessary to provide you with services available through our Websites and to use some of its features, such as access to secure areas."]]
         [:td "WorksHub"]
         [:td [:p "You may disable any of these cookies via your browser settings. If you do so, various functions of the Site may be unavailable to you or may not work the way you want them to."]]]
        [:tr
         [:td [:p "Analytics and customisation cookies: These cookies collect information that is used either in aggregate form to help us understand how our Websites are being used or how effective are marketing campaigns are, or to help us customise our Websites for you."]
          [:td
           [:p "Google AdWords Conversion"]
           [:p "Google Analytics"]
           [:p "HubSpot"]
           [:p "Pingdom"]
           [:p "Segment"]]]
         [:td "Please click on the relevant opt-out link below:"
          [:ul
           [:li [:a {:href   "https://adssettings.google.com/authenticated?hl=en"
                     :target "_blank"
                     :rel    "noopener"} "Google AdWords Conversion"]]
           [:li [:a {:href   "https://tools.google.com/dlpage/gaoptout"
                     :target "_blank"
                     :rel    "noopener"} "Google Analytics"]]
           [:li "Hubspot does not provide an opt-out link for its cookies. For more information about Hubspot cookies, please see " [:a {:href   "https://knowledge.hubspot.com/articles/kcs_article/reports/what-cookies-does-hubspot-set-in-a-visitor-s-browser"
                                                                                                                                         :target "_blank"
                                                                                                                                         :rel    "noopener"} "this page"]]
           [:li "Pingdom does not provide an opt-out link for its cookies. For more information about Pingdom cookies, please see " [:a {:href   "https://www.pingdom.com/legal/cookie-policy"
                                                                                                                                         :target "_blank"
                                                                                                                                         :rel    "noopener"} "this page"]]
           [:li "Segment does not provide an opt-out link for its cookies. For more information about Segment cookies, please see " [:a {:href   "https://segment.com/docs/legal/privacy/"
                                                                                                                                         :target "_blank"
                                                                                                                                         :rel    "noopener"} "this page"]]]]]]]]

     [:li
      [:span.heading "Changes to the Policy"]
      [:ol
       [:li [:p "This Policy was last updated on 12 January, 2021. A notice will be posted on our Websites' home pages for [30 days] whenever this Policy is changed in a material way. By continuing to use our Websites you confirm your continuing acceptance of this Policy."]]]]

     [:li
      [:span.heading "Questions about this Policy"]
      [:ol
       [:li [:p "These Websites are maintained by WorksHub. If you have a question, concern or complaint about this Policy or our handling of your information, you can contact WorksHub by email on "
             [:span [:a {:href "mailto:hello@functionalworks.com"} "hello@functionalworks.com"]]]]]]]]])
