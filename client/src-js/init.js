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
    /* tags */
    let tagList = document.getElementById("tag-list");
    let tagBoxes = document.getElementsByClassName("tags-container--wants-js-tags");
    if(tagList && tagList.innerText && tagList.innerText != "" && tagBoxes && tagBoxes.length > 0) {
        initTagList(tagList.innerText);
        for(var i = (tagBoxes.length  -1); i >= 0; i--) {
            initTags(tagBoxes[i]);
        }
    };
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
