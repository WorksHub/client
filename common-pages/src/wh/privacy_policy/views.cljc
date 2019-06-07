(ns wh.privacy-policy.views)

(defn page [_]
  [:div.main.privacy
   [:h1 "Privacy policy"]
   [:div.container
    [:ol
     [:li
      [:span.heading "Introduction"]
      [:ol
       [:li "The Functional Works Privacy Policy (\"Policy\") sets out what, why and how personal data are collected and used by Functional Works [and its affiliates] ([collectively] “Functional Works”, \"we\". \"us\", \"our\") when you visit our online job matching website (\"Website\").  By continuing to use this Website, you consent to your information being processed in accordance with this Policy.    All Data Subjects provide consent of which we keep record.  Data Subjects have ability to withdraw consent.\n"]
       [:li "If you have questions or need to contact us about this Privacy Policy, please email us at "
        [:span [:a {:href "mailto:hello@functionalworks.com"} "hello@functionalworks.com"]]
        " or our Data Controller, Josh Kaplan at "
        [:span [:a {:href "mailto:josh.kaplan@functionalworks.com"} "josh.kaplan@functionalworks.com"]]]]]

     [:li
      [:span.heading "What personal information we collect and how we use it"]
      [:ol
       [:li "How we collect personal information and what sort of personal information:  You may give us information about you via your account (\"Account\") on our Website. Your Account requires a valid e-mail address as a login, along with other information which will be of a personal nature, your name, and professional experience. This information is mandatory and is required to set up your Account.  We also use cookies to collect personal information (see below)."]
       [:li "Why we collect personal information:  Your information will be used by Functional Works for job matching purposes. Once you have created your Account, Employers (being individuals, partnerships or companies that are searching for candidates) registered on our Website will be able to approach you if they wish to invite you for an interview if they feel that you may be suitable for a vacancy. At this point you can either accept or decline the interview request. If you accept the request then your personal details, including your name, email, telephone will be released to the Employer."]]]

     [:li
      [:span.heading "Sensitive personal information"]
      [:ol
       [:li "As part of the registration process, we do not require you to provide any sensitive personal information. Sensitive personal information means data revealing your racial / ethnic origin, political opinions, religious / philosophical beliefs, trade union membership, genetic / biometric data, data concerning health, data concerning sex life, data concerning sexual orientation and data concerning the commission or alleged commission of any offence.  To the extent that you voluntarily supply any sensitive information when registering on our Website, such sensitive personal information will be handled in accordance with this Policy."]]]

     [:li
      [:span.heading "How we share and disclose your personal information"]
      [:ol
       [:li "All Companies registered on the Website will have access to your information. By creating an Account on our Website, you accept and acknowledge that an Employer may contact you if they wish to interview you for a job. Once you accept the invitation to attend an interview, we will transfer your personal information to the Employer. We take steps to ensure that the Employers respect your privacy and treat your personal information in a confidential manner. With all companies signing terms that agree to treat your personal data in a confidential way.  For more information on how the Employer processes your personal information, please refer to the Employer's privacy policy."]
       [:li "Functional Works Ltd engages certain onward subprocessors that may process personal data submitted to Functional Works’s services. These subprocessors are listed below, and may be updated by Functional Works from time to time:"
        [:ul
         [:li "Algolia"]
         [:li "Amazon Web Services"]
         [:li "Clearbit"]
         [:li "Google"]
         [:li "Heap Analytics"]
         [:li "Heroku"]
         [:li "Hubspot"]
         [:li "MLab"]
         [:li "Neverbounce"]
         [:li "Segment"]
         [:li "Sendgrid"]
         [:li "Sentry"]
         [:li "Slack"]]]
       [:li "We may also share your information with third parties if required by a court order or any legal or regulatory requirement."]]]

     [:li
      [:span.heading "Links to other websites"]
      [:ol
       [:li "When providing us with your Account information, you may voluntarily supply us with a link to your social media website page (i.e. your GitHub, LinkedIn, Twitter or Facebook profile page). Please note that this information will be made available to the Employer when you accept the Employer's invitation for an interview. We therefore suggest that you consider this when adding links to your social media website pages to your Account.\n"]]]

     [:li
      [:span.heading "Data Retention"]
      [:ol
       [:li
        [:p "We will only retain your personal information for as long as necessary to fulfil the purposes we collected it for, including for the purposes of satisfying any legal, accounting, or reporting requirements."]
        [:p "To determine the appropriate retention period for personal information, we consider the amount, nature, and sensitivity of the personal information, the potential risk of harm from unauthorised use or disclosure of your personal information, the purposes for which we process your personal information and whether we can achieve those purposes through other means, and the applicable legal requirements."]]]]

     [:li
      [:span.heading "Your rights"]
      [:ol
       [:li "You can access the personal information via your Account to obtain a copy of it and to correct, amend, or delete information that is inaccurate. You can also delete your Account altogether. You also have right to request the transfer of your  personal data to another party."]
       [:li "You have the right to complain to our Data Controller via our internal complaints policy.  However, if you wish to obtain a copy of personal information that we hold or request it be amended, deleted etc.  you please contact us at the address "
        [:span [:a {:href "mailto:hello@functionalworks.com"} "hello@functionalworks.com"]]
        ". Before we are able to respond to your request we may ask you to verify your identity and to provide further details about your request. We will endeavour to respond within an appropriate timeframe and, in any event, within any timescales required by law."]
       [:li "It is your responsibility to ensure that you submit true, accurate and complete information on your Account and keep it up to date."]]]

     [:li
      [:span.heading "Information security"]
      [:ol
       [:li "We apply appropriate security measures to protect your personal information that is under our control, from unauthorised access, collection, use, disclosure, copying, modification or disposal. All information you provide to us is stored on our secure servers. Where you have a password which enables you to access our Website, you are responsible for keeping this password secure and confidential."]]]

     [:li
      [:span.heading "Internet-based transfers"]
      [:ol
       [:li "Given that the Internet is a global environment, using the Internet to collect and process personal information necessarily involves the transmission of data on an international basis. While we store all of the personal information that we collect about you through our Website in the UK, it is possible that [your personal information may be transmitted to parties outside the European Economic Area (\"EEA\"). For instance, is it possible that emails between you and an Employer may be sent via a third party service that stores emails outside the EEA. Therefore, by browsing this Website, you acknowledge our processing of personal information in this way. ] However, we will continue to use your personal information securely, lawfully and in the manner we describe in this Policy."]]]

     [:li
      [:span.heading "Cookies"]
      [:ol
       [:li "We collect information automatically through the use of \"cookies\". A cookie is a text file containing small amounts of information that a website can send to your browser, which may then be stored on your computer as an anonymous tag that identifies your computer but not you. Some of our Website pages use cookies, sent by Functional Works to better serve you when you return to the Website. Cookies are used for session management only on our Website. You can set your browser to notify you before you receive a cookie, giving you the chance to decide whether to accept it. You can also set your browser to turn off cookies; however, if you do this, some of our Website may not work properly."]
       [:li "For information about the specific cookies used on this website, please see below:"]]

      [:table.table
       [:thead
        [:tr
         [:th "Type of cookie"]
         [:th "Who serves these cookies"]
         [:th "How to refuse"]]]
       [:tbody
        [:tr
         [:td "Essential website cookies: These cookies are strictly necessary to provide you with services available through our Websites and to use some of its features, such as access to secure areas."]
         [:td "Functional Works"]
         [:td "[You may disable any of these cookies via your browser settings. If you do so, various functions of the Site may be unavailable to you or may not work the way you want them to.]"]]
        [:tr
         [:td "Analytics and customisation cookies: These cookies collect information that is used either in aggregate form to help us understand how our Websites are being used or how effective are marketing campaigns are, or to help us customise our Websites for you."]
         [:td
          [:p "Google AdWords Conversion"]
          [:p "Google Analytics"]
          [:p "Heap"]
          [:p "HubSpot"]
          [:p "Pingdom"]
          [:p "Segment"]]
         [:td "Please click on the relevant opt-out link below:"
          [:ul
           [:li [:a {:href "https://adssettings.google.com/authenticated?hl=en"
                     :target "_blank"
                     :rel "noopener"} "Google AdWords Conversion"]]
           [:li [:a {:href "https://tools.google.com/dlpage/gaoptout"
                     :target "_blank"
                     :rel "noopener"} "Google Analytics"]]
           [:li "Heap does not provide an opt-out link for its cookies. For more information about Heap cookies, please see " [:a {:href "https://heapanalytics.com/privacy"
                                                                                                                                   :target "_blank"
                                                                                                                                   :rel "noopener"} "this page"]]
           [:li "Hubspot does not provide an opt-out link for its cookies. For more information about Hubspot cookies, please see " [:a {:href "https://knowledge.hubspot.com/articles/kcs_article/reports/what-cookies-does-hubspot-set-in-a-visitor-s-browser"
                                                                                                                                         :target "_blank"
                                                                                                                                         :rel "noopener"} "this page"]]
           [:li "Pingdom does not provide an opt-out link for its cookies. For more information about Pingdom cookies, please see " [:a {:href "https://www.pingdom.com/legal/cookie-policy"
                                                                                                                                         :target "_blank"
                                                                                                                                         :rel "noopener"} "this page"]]
           [:li "Segment does not provide an opt-out link for its cookies. For more information about Segment cookies, please see " [:a {:href "https://segment.com/docs/legal/privacy/"
                                                                                                                                         :target "_blank"
                                                                                                                                         :rel "noopener"} "this page"]]]]]]]]

     [:li
      [:span.heading "Changes to the Policy"]
      [:ol
       [:li "This Policy was last updated on 5 June, 2019. A notice will be posted on our Websites' home pages for [30 days] whenever this Policy is changed in a material way. By continuing to use our Website you confirm your continuing acceptance of this Policy."]]]

     [:li
      [:span.heading "Questions about this Policy"]
      [:ol
       [:li "This Website is maintained by Functional Works. If you have a question, concern or complaint about this Policy or our handling of your information, you can contact Functional Works by email on "
        [:span [:a {:href "mailto:hello@functionalworks.com"} "hello@functionalworks.com"]]]]]]]])
