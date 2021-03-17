import {
    showTrackingPopup,
    loadAnalytics,
    storeReferralData,
    initServerTracking,
    getCookie
} from './analytics';
import { initTags } from './tags';

function init() {
    /* tracking */
    var consent = getCookie('wh_tracking_consent');
    var aid = getCookie('wh_aid');
    storeReferralData();

    if (!consent) {
        showTrackingPopup();
    } else {
        if (!aid) {
            initServerTracking(r => loadAnalytics());
        } else {
            loadAnalytics();
        }
    }

    /* tags */
    const tagBoxes = document.getElementsByClassName('tags-container--wants-js-tags');

    if (tagBoxes && tagBoxes.length > 0) {
        Array.from(tagBoxes).forEach(tagBox => {
            const tagsUrl = tagBox.getAttribute('data-tags-url');
            initTags(tagBox, null, null, null, null, null, tagsUrl);
        });
    }
}

/*--------------------------------------------------------------------------*/

function whLoaded() {
    return typeof wh != 'undefined' && typeof wh.core != 'undefined';
}

window.addEventListener('DOMContentLoaded', event => {
    init();
    // load immediately if we can
    if (whLoaded()) {
        wh.core.init();
    }
    // alternatively, start interval and try again every 50ms
    else {
        var i = setInterval(function () {
            if (whLoaded()) {
                wh.core.init();
                clearInterval(i);
            }
        }, 50);
    }
});
