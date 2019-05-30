function init() {
    /* tracking */
    var consent = getCookie("wh_tracking_consent");
    var aid = getCookie("wh_aid");
    storeReferralData();
    if(!consent) {
        showTrackingPopup();
    } else {
        loadAnalytics();
        if(!aid) {
            initServerTracking();
        }
    }
}

/*--------------------------------------------------------------------------*/

function whLoaded () {
    return typeof wh != "undefined" && typeof wh.core != "undefined";
}

(function () {
    window.addEventListener('DOMContentLoaded', (event) => {
        init();
        // load immediately if we can
        if(whLoaded()) {
            wh.core.init();
        }
        // alternatively, start interval and try again every 50ms
        else {
            var i = setInterval(function() {
                if(whLoaded()) {
                    wh.core.init();
                    clearInterval(i);
                }
            }, 50);
        }
    });
})();
