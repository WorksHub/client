var wh_analytics = {};
wh_analytics.utms = null;
wh_analytics.referrer = null;
wh_analytics.landingPage = null;
wh_analytics.title = null;
wh_analytics.init = getCookie("wh_tracking_consent") != null;

/*--------------------------------------------------------------------------*/

function setCookie(name,value,days) {
    var expires = "";
    if (days) {
        var date = new Date();
        date.setTime(date.getTime() + (days*24*60*60*1000));
        expires = "; expires=" + date.toUTCString();
    }
    document.cookie = name + "=" + (value || "")  + expires + "; path=/";
}

function getCookie(name) {
    var value = "; " + document.cookie;
    var parts = value.split("; " + name + "=");
    if (parts.length == 2) return parts.pop().split(";").shift();
}

/*--------------------------------------------------------------------------*/

function extractUtmFields(qps) {
    const utmFields = ["utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content"];
    const filteredQps = Array.from(qps.keys())
          .filter(key => utmFields.includes(key))
          .reduce((obj, key) => {
              var v = qps.get(key);
              if(v != "undefined" && v != "") {
                  obj.set(key, v);
              }
              return obj;
          }, new Map());
    return filteredQps;
}

function storeReferralData() {
    // get existing
    const existingQps   = localStorage.getItem("wh_analytics_utms");
    const existingUtms  = extractUtmFields(extractQueryParams(existingQps));
    const existingRef   = localStorage.getItem("wh_analytics_referrer");
    const existingLP    = localStorage.getItem("wh_analytics_landing_page");
    const existingTitle = localStorage.getItem("wh_analytics_title");
    // get current
    const currentQps   = window.location.search.substring(1);
    const currentUtms  = extractUtmFields(extractQueryParams(currentQps));
    const currentRef   = window.document.referrer;
    const currentLP    = window.location.toString();
    const currentTitle = window.document.title;
    // combined
    wh_analytics.utms        = (existingUtms.size > 0 ? existingUtms : currentUtms);
    wh_analytics.referrer    = (existingRef   && existingRef   != "" ? existingRef   : currentRef);
    wh_analytics.landingPage = (existingLP    && existingLP    != "" ? existingLP    : currentLP);
    wh_analytics.title       = (existingTitle && existingTitle != "" ? existingTitle : currentTitle);
    // store
    localStorage.setItem("wh_analytics_utms", mapAsQueryString(wh_analytics.utms));
    localStorage.setItem("wh_analytics_referrer", (wh_analytics.referrer == null ? "" : wh_analytics.referrer));
    localStorage.setItem("wh_analytics_landing_page", (wh_analytics.landingPage == null ? "" : wh_analytics.landingPage));
    localStorage.setItem("wh_analytics_title", (wh_analytics.title == null ? "" : wh_analytics.title));
}

function clearStoredData() {
    localStorage.removeItem("wh_analytics_utms");
    localStorage.removeItem("wh_analytics_referrer");
    localStorage.removeItem("wh_analytics_landing_page");
    localStorage.removeItem("wh_analytics_title");
    wh_analytics.utms        = null;
    wh_analytics.referrer    = null;
    wh_analytics.landingPage = null;
    wh_analytics.title       = null;
}

/*--------------------------------------------------------------------------*/

function hideTrackingPopup() {
    setClass("tracking-popups", "is-open", false);
}

function showTrackingPopup() {
    setClass("tracking-popups", "is-open", true);
}

/*--------------------------------------------------------------------------*/

function addSourcing(obj, atx) {
    var sourcing = {};
    if(atx.referrer != null && atx.referrer != "") {
        sourcing.referrer = atx.referrer;
    }
    if(atx.utms && atx.utms.size > 0) {
        const utmsEntries = Array.from(atx.utms.entries())

        sourcing.campaign = Object.assign.apply(Object,
                                                [{}]
                                                .concat(utmsEntries
                                                        .map(([k, v]) => ({[k]: v}))))

        // also add without the `utm_` part
        sourcing.campaign = Object.assign.apply(Object,
                                                [sourcing.campaign]
                                                .concat(utmsEntries
                                                        .map(([k, v]) => ({[k.replace("utm_", "")]: v}))))
    }
    if(Object.keys(sourcing).length > 0) {
        obj.sourcing = sourcing;
    }
    return obj;
}

function createPageProps(atx) {
    let url         = null;
    let path        = null;
    let search      = null;
    let landingPage = null;
    let title       = null;
    if(atx.landingPage != null && atx.landingPage != "") {
        landingPage = new URL(atx.landingPage);
        url         = atx.landingPage;
        path        = landingPage.pathname;
        search      = landingPage.search;
    } else {
        url    = window.location.toString();
        path   = window.location.pathname;
        search = window.location.search;
    }
    if(atx.title != null && atx.title != "") {
        title = atx.title;
    } else {
        title = window.document.title;
    }
    const sourcing  = addSourcing({}, atx).sourcing;
    const pageProps = {path:     path,
                       search:   search,
                       url:      url,
                       title:    title,
                       referrer: (sourcing != null ? sourcing.referrer : "")};
    return pageProps;
}

function sendServerAnalytics(body, onsuccess = null) {
    var i = null;
    let send = function() {
        if(wh_analytics.init) {
            clearInterval(i);
            const bodyWithSourcing = addSourcing(body, wh_analytics);
            var r = new XMLHttpRequest();
            r.timeout = 10000;
            r.open("POST", "/api/analytics");
            r.setRequestHeader('Content-Type', 'application/json; charset=UTF-8');
            r.onloadend = function () {
                if(r.status != 200) {
                    console.log("analytics failed: " + r.status);
                } else if(onsuccess) {
                    onsuccess(r);
                }
            };
            r.send(JSON.stringify(body));
        }
    };
    if(wh_analytics.init)
    {
        send();
    } else {
        // try and send every 1000ms if no consent
        i = setInterval(send, 1000);
    }
}

function sendServerPage(atx) {
    sendServerAnalytics({"type":    "page",
                         "payload":  createPageProps(atx)});
}

function initServerTracking(onsuccess = null) {
    wh_analytics.init = true;
    sendServerAnalytics({"type":"init"}, onsuccess);
}

/*--------------------------------------------------------------------------*/

function whenAnalytics(f) {
    var i = setInterval(function() {
        if(typeof analytics != "undefined") {
            f();
            clearInterval(i);
        }
    }, 100);
}

function whenAnalyticsReady(f) {
    var i = setInterval(function() {
        if(typeof analytics != "undefined" &&
           typeof analytics.user != "undefined") {
            f();
            clearInterval(i);
        }
    }, 100);
}

function loadAnalytics() {
    whenAnalytics(function () {
        analytics.on("load", x => console.log("LOADED AJS"));
        analytics.load();
        whenAnalyticsReady(function () {
            // if we have a wh_aid lets use it to make it easier to track
            // users across destinations
            if(getCookie("wh_aid") != null) {
                setAnalyticsAnonymousId(getCookie("wh_aid"));
            }
            if(!isAppPresent()) {
                // we only do this if no app, otherwise it's done in
                // `wh.common.fx.analytics` (see `:analytics/pageview`)
                trackPage();
            }
        });
    });
}

function setAnalyticsAnonymousId(aid) {
    whenAnalyticsReady(function () {
        analytics.setAnonymousId(aid);
    });
}

function resetAnalytics() {
    whenAnalytics(function () {
        analytics.reset();
    });
}

function submitAnalyticsAlias(id) {
    whenAnalytics(function () {
        analytics.alias(id);
    });
}

function submitAnalyticsIdentify(id, user, integrations) {
    whenAnalytics(function () {
        analytics.identify(id, user, integrations);
    });
}

function submitAnalyticsPage(atx) {
    whenAnalytics(function () {
        const sourcing = addSourcing({}, atx).sourcing;
        const pageProps = createPageProps(atx);
        analytics.page(pageProps, {context: sourcing});
    });
}

function submitAnalyticsTrack(evt, prps) {
    whenAnalytics(function () {
        analytics.track(evt, prps);
    });
}

/*--------------------------------------------------------------------------*/

function trackPage() {
    const atx = Object.assign({}, wh_analytics);
    const alternativeLandingPage =
          atx.landingPage &&
          atx.landingPage != "" &&
          atx.landingPage != window.location.toString();
    sendServerPage(atx);
    submitAnalyticsPage(atx);
    clearStoredData(); // this clears wh_analytics for new data later, including landingPage
    if(alternativeLandingPage) {
        trackPage();
    }
}

function agreeToTracking() {
    if(getCookie("wh_tracking_consent") == null) {
        setCookie("wh_tracking_consent", true, 365);
        hideTrackingPopup();
        initServerTracking(r => loadAnalytics());
    }
}
