var wh_auth = {};
wh_auth.redirect = null;
wh_auth.contexts = new Map();
wh_auth.contexts.set("homepage-jobcard-apply", "apply");
wh_auth.contexts.set("contribute",             "contribute");
wh_auth.contexts.set("jobcard-apply",          "apply");
wh_auth.contexts.set("jobpage-apply",          "apply");
wh_auth.contexts.set("search-jobs",            "search-jobs");
wh_auth.contexts.set("jobpage-see-more",       "see-more");
wh_auth.contexts.set("upvote",                 "upvote");
wh_auth.contexts.set("issue",                  "issue");

function showAuthPopUp(context, redirect) {
    localStorage.setItem('wh_auth.redirect', redirect);
    var newContext = wh_auth.contexts.get(context);
    var arr = new Set(Array.from(wh_auth.contexts.values()));
    arr.forEach(item => setClass("auth-popup__" + item, "is-visible", false));
    setClass("auth-popup__" + newContext, "is-visible", true);
    setClass("auth-popup", "is-open", true);
    setNoScroll("auth-popup", true);
}

function hideAuthPopUp() {
    setClass("auth-popup", "is-open", false);
    setNoScroll("auth-popup", false);
}

function popAuthRedirect() {
    var r = localStorage.getItem('wh_auth.redirect');
    localStorage.removeItem('wh_auth.redirect');
    return r;
}
