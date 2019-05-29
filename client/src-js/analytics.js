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

function hideTrackingPopup() {
    setClass("tracking-popups", "is-open", false);
}

function showTrackingPopup() {
    setClass("tracking-popups", "is-open", true);
}

/*--------------------------------------------------------------------------*/

function sendServerAnalytics(body) {
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
            analytics.page(evt, prps);
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
