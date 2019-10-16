wh_analytics = {};
wh_analytics.utms = null;
wh_analytics.referrer = null;

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
    const renameFields = new Map().set("utm_campaign", "name");
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
    const existingQps = localStorage.getItem("wh_analytics_utms");
    const existingUtms = extractUtmFields(getQueryParams(existingQps));
    const existingRef  = localStorage.getItem("wh_analytics_referrer");
    // get current
    const currentQps  = window.location.search.substring(1);
    const currentUtms = extractUtmFields(getQueryParams(currentQps));
    const currentRef  = document.referrer;
    // combined
    wh_analytics.utms     = new Map([...existingUtms, ...currentUtms])
    wh_analytics.referrer = (currentRef == "" ? existingRef : currentRef);
    // store
    localStorage.setItem("wh_analytics_utms", mapAsQueryString(wh_analytics.utms));
    localStorage.setItem("wh_analytics_referrer", (wh_analytics.referrer == null ? "" : wh_analytics.referrer));
}

/*--------------------------------------------------------------------------*/

function hideTrackingPopup() {
    setClass("tracking-popups", "is-open", false);
}

function showTrackingPopup() {
    setClass("tracking-popups", "is-open", true);
}

/*--------------------------------------------------------------------------*/

function addSourcing(obj) {
    var sourcing = {};
    if(wh_analytics.referrer != null && wh_analytics.referrer != "") {
        sourcing.referrer= wh_analytics.referrer;
    }
    if(wh_analytics.utms.size > 0) {
        sourcing.campaign = Object.assign({}, ...[...wh_analytics.utms.entries()].map(([k, v]) => ({[k]: v})))
    }
    if(Object.keys(sourcing).length > 0) {
        obj.sourcing = sourcing;
    }
    return obj;
}

function sendServerAnalytics(body) {
    const bodyWithSourcing = addSourcing(body);
    var r = new XMLHttpRequest();
    r.timeout = 10000;
    r.open("POST", "/api/analytics");
    r.setRequestHeader('Content-Type', 'application/json; charset=UTF-8');
    r.onloadend = function () {
        if(r.status != 200) {
            console.log("analytics failed: " + r.status);
        }
    };
    r.send(JSON.stringify(body));
}

function initServerTracking() {
    sendServerAnalytics({"type":"init"});
}

/*--------------------------------------------------------------------------*/

function loadAnalytics() {
    var i = setInterval(function() {
        if(typeof analytics != "undefined") {
            analytics.load();
            clearInterval(i);
        }
    }, 100);
}

function resetAnalytics() {
    var i = setInterval(function() {
        if(typeof analytics != "undefined") {
            analytics.reset();
            clearInterval(i);
        }
    }, 100);
}

function submitAnalyticsAlias(id) {
    var i = setInterval(function() {
        if(typeof analytics != "undefined") {
            analytics.alias(id);
            clearInterval(i);
        }
    }, 100);
}

function submitAnalyticsIdentify(id, user, integrations) {
    var i = setInterval(function() {
        if(typeof analytics != "undefined") {
            analytics.identify(id, user, integrations);
            clearInterval(i);
        }
    }, 100);
}

function submitAnalyticsPage() {
    var i = setInterval(function() {
        if(typeof analytics != "undefined") {
            analytics.page();
            clearInterval(i);
        }
    }, 100);
}

function submitAnalyticsTrack(evt, prps) {
    var i = setInterval(function() {
        if(typeof analytics != "undefined") {
            analytics.track(evt, prps);
            clearInterval(i);
        }
    }, 100);
}

function agreeToTracking() {
    setCookie("wh_tracking_consent", true, 365);
    hideTrackingPopup();
    loadAnalytics();
    initServerTracking();
}
