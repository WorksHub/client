/******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};
/******/
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/
/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId]) {
/******/ 			return installedModules[moduleId].exports;
/******/ 		}
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			i: moduleId,
/******/ 			l: false,
/******/ 			exports: {}
/******/ 		};
/******/
/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);
/******/
/******/ 		// Flag the module as loaded
/******/ 		module.l = true;
/******/
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/
/******/
/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;
/******/
/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;
/******/
/******/ 	// define getter function for harmony exports
/******/ 	__webpack_require__.d = function(exports, name, getter) {
/******/ 		if(!__webpack_require__.o(exports, name)) {
/******/ 			Object.defineProperty(exports, name, { enumerable: true, get: getter });
/******/ 		}
/******/ 	};
/******/
/******/ 	// define __esModule on exports
/******/ 	__webpack_require__.r = function(exports) {
/******/ 		if(typeof Symbol !== 'undefined' && Symbol.toStringTag) {
/******/ 			Object.defineProperty(exports, Symbol.toStringTag, { value: 'Module' });
/******/ 		}
/******/ 		Object.defineProperty(exports, '__esModule', { value: true });
/******/ 	};
/******/
/******/ 	// create a fake namespace object
/******/ 	// mode & 1: value is a module id, require it
/******/ 	// mode & 2: merge all properties of value into the ns
/******/ 	// mode & 4: return value when already ns object
/******/ 	// mode & 8|1: behave like require
/******/ 	__webpack_require__.t = function(value, mode) {
/******/ 		if(mode & 1) value = __webpack_require__(value);
/******/ 		if(mode & 8) return value;
/******/ 		if((mode & 4) && typeof value === 'object' && value && value.__esModule) return value;
/******/ 		var ns = Object.create(null);
/******/ 		__webpack_require__.r(ns);
/******/ 		Object.defineProperty(ns, 'default', { enumerable: true, value: value });
/******/ 		if(mode & 2 && typeof value != 'string') for(var key in value) __webpack_require__.d(ns, key, function(key) { return value[key]; }.bind(null, key));
/******/ 		return ns;
/******/ 	};
/******/
/******/ 	// getDefaultExport function for compatibility with non-harmony modules
/******/ 	__webpack_require__.n = function(module) {
/******/ 		var getter = module && module.__esModule ?
/******/ 			function getDefault() { return module['default']; } :
/******/ 			function getModuleExports() { return module; };
/******/ 		__webpack_require__.d(getter, 'a', getter);
/******/ 		return getter;
/******/ 	};
/******/
/******/ 	// Object.prototype.hasOwnProperty.call
/******/ 	__webpack_require__.o = function(object, property) { return Object.prototype.hasOwnProperty.call(object, property); };
/******/
/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";
/******/
/******/
/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(__webpack_require__.s = "./client/src-js/index.js");
/******/ })
/************************************************************************/
/******/ ({

/***/ "./client/src-js/ac.js":
/*!*****************************!*\
  !*** ./client/src-js/ac.js ***!
  \*****************************/
/*! no static exports found */
/***/ (function(module, exports) {

const HISTORY_SIZE = 20
const HISTORY_LIST_SIZE = 4
const DB_ID = "search_history"

const cls = {
    searchMenu: 'search-dropdown',
    cancelBtn: 'search-dropdown__remove',
    result: 'search-dropdown__result'
}

function range(n) {
    let res = [];
    for (let i=0; i<n; i++) {
        res.push(i);
    }
    return res;
}

function getHistory() {
  try {
      return JSON.parse(localStorage.getItem(DB_ID)) || [];
  } catch (e) {
      return [];
  }
}

function deleteEntry(idx) {
    const history = getHistory();
    history.splice(idx, 1);
    localStorage.setItem(DB_ID, JSON.stringify(history));
}

function hideMenu(n) {
    const e = n.parentNode.querySelector('.'+cls.searchMenu);
    e && e.remove();
}

function onSearchKey(e, onKeyDown) {
    const input = e.target || e;
    let history = getHistory();
    if (e.key === 'Enter') {
        const value = input.value;
        history = history.filter(e => e !== value);
        history.unshift(value);
        if (history.length > HISTORY_SIZE) {
            history.pop();
        }
        localStorage.setItem(DB_ID, JSON.stringify(history));
        hideMenu(input);
    } else if(onKeyDown) {
        // input doesn't contain the new value yet so we give it
        // a brief time to update.
        setTimeout(_ => {
            onKeyDown(input);
        }, 50);
    }
}

function highlight(text, q) {
    const loc = text.indexOf(q);
    if (loc === -1) {
        const res = document.createElement('span');
        res.innerText = text;
        return [res];
    } else {
        const res = range(3).map(() => document.createElement('span'));
        res[0].innerText = text.slice(0, loc);
        res[1].innerText = text.slice(loc, loc+q.length);
        res[2].innerText = text.slice(loc+q.length);
        res[1].style['font-weight'] = 600;
        return res;
    }
}

function showMenu(n, results, q) {
    if (results.length === 0) {
        return;
    }
    const w = document.createElement('div');
    w.classList.add(cls.searchMenu);
    n.parentNode.appendChild(w);
    results.map(({text, idx}) => {
        const el = document.createElement('div');
        const te = document.createElement('div');
        highlight(text, q)
            .forEach(e => te.appendChild(e));
        el.appendChild(te);

        const cancel = document.createElement('div');
        cancel.classList.add(cls.cancelBtn);
        cancel.innerText = '×';
        cancel.addEventListener('mousedown', e => {
            e.stopPropagation();
            e.preventDefault();
            deleteEntry(idx);
            el.remove();
        });
        el.appendChild(cancel);
        el.classList.add(cls.result);

        el.addEventListener('mousedown', (e) => {
            n.value = text;
            n.closest('form').submit();
        });
        return el;
    }).forEach(el => w.appendChild(el));
}

function onSearchQueryEdit(e) {
    const input = e.target || e;
    const history = getHistory();
    const query = input.value.toLowerCase();
    const results = history.map((res, idx) => ({text: res, idx}))
          .filter(e => e.text.indexOf(query) !== -1)
          .slice(0, HISTORY_LIST_SIZE);
    hideMenu(input);
    showMenu(input, results, query);
}

let elr;

function onSearchFocus(e) {
    const input = e.target || e;
    const history = getHistory();
    const query = input.value.toLowerCase();
    const results = history.slice(0, HISTORY_LIST_SIZE)
          .map((res, idx) => ({text: res, idx}))
          .filter(e => e.text.indexOf(query) !== -1);
    showMenu(input, results, query);

    elr = () => {
        hideMenu(input);
        input.removeEventListener('blur', elr);
    };
    input.addEventListener('blur', elr);
}

window.onSearchKey = onSearchKey;
window.onSearchQueryEdit = onSearchQueryEdit;
window.onSearchFocus = onSearchFocus;


/***/ }),

/***/ "./client/src-js/add_sourcing.js":
/*!***************************************!*\
  !*** ./client/src-js/add_sourcing.js ***!
  \***************************************/
/*! exports provided: addSourcing */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "addSourcing", function() { return addSourcing; });
function addSourcing(obj, atx) {
  var sourcing = {};

  if (atx.referrer != null && atx.referrer != '') {
    sourcing.referrer = atx.referrer;
  }

  if (atx.utms && atx.utms.size > 0) {
    const utmsEntries = Array.from(atx.utms.entries());

    sourcing.campaign = Object.assign(
      {}, ...utmsEntries.map(([k, v]) => ({ [k]: v })));

    // also add without the `utm_` part
    sourcing.campaign = Object.assign(
      sourcing.campaign,
      ...utmsEntries.map(([k, v]) => ({ [k.replace('utm_', '')]: v }))
    );
  }

  if (Object.keys(sourcing).length > 0) {
    obj.sourcing = sourcing;
  }

  return obj;
}


/***/ }),

/***/ "./client/src-js/analytics.js":
/*!************************************!*\
  !*** ./client/src-js/analytics.js ***!
  \************************************/
/*! exports provided: getCookie, storeReferralData, showTrackingPopup, initServerTracking, loadAnalytics, resetAnalytics, submitAnalyticsTrack */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "getCookie", function() { return getCookie; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "storeReferralData", function() { return storeReferralData; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "showTrackingPopup", function() { return showTrackingPopup; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "initServerTracking", function() { return initServerTracking; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "loadAnalytics", function() { return loadAnalytics; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "resetAnalytics", function() { return resetAnalytics; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "submitAnalyticsTrack", function() { return submitAnalyticsTrack; });
/* harmony import */ var _util__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! ./util */ "./client/src-js/util.js");
/* harmony import */ var _send_analytics__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! ./send_analytics */ "./client/src-js/send_analytics.js");
/* harmony import */ var _add_sourcing__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! ./add_sourcing */ "./client/src-js/add_sourcing.js");
/* harmony import */ var _public__WEBPACK_IMPORTED_MODULE_3__ = __webpack_require__(/*! ./public */ "./client/src-js/public.js");
/* global analytics */





window.wh_analytics = {};
window.wh_analytics.utms = null;
window.wh_analytics.referrer = null;
window.wh_analytics.landingPage = null;
window.wh_analytics.title = null;
window.wh_analytics.init = getCookie('wh_tracking_consent') != null;

/*--------------------------------------------------------------------------*/

function setCookie(name, value, days) {
  var expires = '';
  if (days) {
    var date = new Date();
    date.setTime(date.getTime() + days * 24 * 60 * 60 * 1000);
    expires = '; expires=' + date.toUTCString();
  }
  document.cookie = name + '=' + (value || '') + expires + '; path=/';
}

function getCookie(name) {
  var value = '; ' + document.cookie;
  var parts = value.split('; ' + name + '=');
  if (parts.length == 2) {
    return parts
      .pop()
      .split(';')
      .shift();
  }

  return undefined;
}

/*--------------------------------------------------------------------------*/

function extractUtmFields(qps) {
  const utmFields = [
    'utm_source',
    'utm_medium',
    'utm_campaign',
    'utm_term',
    'utm_content'
  ];
  const filteredQps = Array.from(qps.keys())
    .filter(key => utmFields.includes(key))
    .reduce((obj, key) => {
      var v = qps.get(key);
      if (v != 'undefined' && v != '') {
        obj.set(key, v);
      }
      return obj;
    }, new Map());
  return filteredQps;
}

function storeReferralData() {
  // get existing
  const existingQps = localStorage.getItem('wh_analytics_utms');
  const existingUtms = extractUtmFields(Object(_util__WEBPACK_IMPORTED_MODULE_0__["extractQueryParams"])(existingQps));
  const existingRef = localStorage.getItem('wh_analytics_referrer');
  const existingLP = localStorage.getItem('wh_analytics_landing_page');
  const existingTitle = localStorage.getItem('wh_analytics_title');
  // get current
  const currentQps = window.location.search.substring(1);
  const currentUtms = extractUtmFields(Object(_util__WEBPACK_IMPORTED_MODULE_0__["extractQueryParams"])(currentQps));
  const currentRef = window.document.referrer;
  const currentLP = window.location.toString();
  const currentTitle = window.document.title;
  // combined
  window.wh_analytics.utms = existingUtms.size > 0 ? existingUtms : currentUtms;
  window.wh_analytics.referrer =
    existingRef && existingRef != '' ? existingRef : currentRef;
  window.wh_analytics.landingPage =
    existingLP && existingLP != '' ? existingLP : currentLP;
  window.wh_analytics.title =
    existingTitle && existingTitle != '' ? existingTitle : currentTitle;
  // store
  localStorage.setItem(
    'wh_analytics_utms',
    Object(_util__WEBPACK_IMPORTED_MODULE_0__["mapAsQueryString"])(window.wh_analytics.utms)
  );
  localStorage.setItem(
    'wh_analytics_referrer',
    window.wh_analytics.referrer == null ? '' : window.wh_analytics.referrer
  );
  localStorage.setItem(
    'wh_analytics_landing_page',
    window.wh_analytics.landingPage == null
      ? ''
      : window.wh_analytics.landingPage
  );
  localStorage.setItem(
    'wh_analytics_title',
    window.wh_analytics.title == null ? '' : window.wh_analytics.title
  );
}

function clearStoredData() {
  localStorage.removeItem('wh_analytics_utms');
  localStorage.removeItem('wh_analytics_referrer');
  localStorage.removeItem('wh_analytics_landing_page');
  localStorage.removeItem('wh_analytics_title');
  window.wh_analytics.utms = null;
  window.wh_analytics.referrer = null;
  window.wh_analytics.landingPage = null;
  window.wh_analytics.title = null;
}

/*--------------------------------------------------------------------------*/

function hideTrackingPopup() {
  Object(_public__WEBPACK_IMPORTED_MODULE_3__["setClass"])('tracking-popups', 'is-open', false);
}

function showTrackingPopup() {
  Object(_public__WEBPACK_IMPORTED_MODULE_3__["setClass"])('tracking-popups', 'is-open', true);
}

/*--------------------------------------------------------------------------*/

function createPageProps(atx) {
  let url = null;
  let path = null;
  let search = null;
  let landingPage = null;
  let title = null;
  if (atx.landingPage != null && atx.landingPage != '') {
    landingPage = new URL(atx.landingPage);
    url = atx.landingPage;
    path = landingPage.pathname;
    search = landingPage.search;
  } else {
    url = window.location.toString();
    path = window.location.pathname;
    search = window.location.search;
  }
  if (atx.title != null && atx.title != '') {
    title = atx.title;
  } else {
    title = window.document.title;
  }
  const sourcing = Object(_add_sourcing__WEBPACK_IMPORTED_MODULE_2__["addSourcing"])({}, atx).sourcing;
  const pageProps = {
    path: path,
    search: search,
    url: url,
    title: title,
    referrer: sourcing != null ? sourcing.referrer : ''
  };
  return pageProps;
}

function sendServerPage(atx) {
  Object(_send_analytics__WEBPACK_IMPORTED_MODULE_1__["sendServerAnalytics"])({ type: 'page', payload: createPageProps(atx) });
}

function initServerTracking(onsuccess = null) {
  window.wh_analytics.init = true;
  Object(_send_analytics__WEBPACK_IMPORTED_MODULE_1__["sendServerAnalytics"])({ type: 'init' }, onsuccess);
}

/*--------------------------------------------------------------------------*/

function whenAnalytics(f) {
  var i = setInterval(function() {
    if (typeof analytics != 'undefined') {
      f();
      clearInterval(i);
    }
  }, 100);
}

function whenAnalyticsReady(f) {
  var i = setInterval(function() {
    if (
      typeof analytics != 'undefined' &&
      typeof analytics.user != 'undefined'
    ) {
      f();
      clearInterval(i);
    }
  }, 100);
}

function loadAnalytics(forcePage) {
    whenAnalytics(function() {
        analytics.ready(function() {
            if(window.onSegmentReady) {
                window.onSegmentReady();
            }});
        analytics.load();
        whenAnalyticsReady(function() {
            // if we have a wh_aid lets use it to make it easier to track
            // users across destinations
            if (getCookie('wh_aid') != null) {
                setAnalyticsAnonymousId(getCookie('wh_aid'));
            }
            if (!Object(_util__WEBPACK_IMPORTED_MODULE_0__["isAppPresent"])() || forcePage) {
                // we only do this if no app, otherwise it's done in
                // `wh.common.fx.analytics` (see `:analytics/pageview`)
                trackPage();
            }
        });
    });
}

function setAnalyticsAnonymousId(aid) {
  whenAnalyticsReady(function() {
    analytics.setAnonymousId(aid);
  });
}

function resetAnalytics() {
  whenAnalytics(function() {
    analytics.reset();
  });
}
window.resetAnalytics = resetAnalytics;

function submitAnalyticsAlias(id) {
  whenAnalytics(function() {
    analytics.alias(id);
  });
}
window.submitAnalyticsAlias = submitAnalyticsAlias;

function submitAnalyticsIdentify(id, user, integrations) {
  whenAnalytics(function() {
    analytics.identify(id, user, integrations);
  });
}

window.submitAnalyticsIdentify = submitAnalyticsIdentify;

function submitAnalyticsPage(atx) {
  whenAnalytics(function() {
    const sourcing = Object(_add_sourcing__WEBPACK_IMPORTED_MODULE_2__["addSourcing"])({}, atx).sourcing;
    const pageProps = createPageProps(atx);
    analytics.page(pageProps, { context: sourcing });
  });
}

function submitAnalyticsTrack(evt, prps) {
  whenAnalytics(function() {
    analytics.track(evt, prps);
  });
}
window.submitAnalyticsTrack = submitAnalyticsTrack;

/*--------------------------------------------------------------------------*/

function trackPage() {
  const atx = Object.assign({}, window.wh_analytics);
  const alternativeLandingPage =
    atx.landingPage &&
    atx.landingPage != '' &&
    atx.landingPage != window.location.toString();
  sendServerPage(atx);
  submitAnalyticsPage(atx);
  clearStoredData(); // this clears wh_analytics for new data later, including landingPage
  if (alternativeLandingPage) {
    trackPage();
  }
}
window.trackPage = trackPage;

function agreeToTracking() {
  if (getCookie('wh_tracking_consent') == null) {
      setCookie('wh_tracking_consent', true, 365);
      hideTrackingPopup();
      initServerTracking(r => loadAnalytics(true));
  }
}

window.agreeToTracking = agreeToTracking;


/***/ }),

/***/ "./client/src-js/auth.js":
/*!*******************************!*\
  !*** ./client/src-js/auth.js ***!
  \*******************************/
/*! no exports provided */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony import */ var _public__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! ./public */ "./client/src-js/public.js");
/* harmony import */ var _local_storage__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! ./local_storage */ "./client/src-js/local_storage.js");



const messageType = {
    apply: 'apply',
    contribute: 'contribute',
    searchJobs: 'search-jobs',
    seeMore: 'see-more',
    upvote: 'upvote',
    issue: 'issue',
    save: 'save'
};
const contextToMessageType = {
    'homepage-jobcard-apply': messageType.apply,
    contribute: messageType.contribute,
    'jobcard-apply': messageType.apply,
    'jobcard-save': messageType.save,
    'jobpage-apply': messageType.apply,
    'search-jobs': messageType.searchJobs,
    'jobpage-see-more': messageType.seeMore,
    upvote: messageType.upvote,
    issue: messageType.issue
};
const lsKey = {
    redirect: 'wh_auth.redirect_url'
};
const cls = {
    isOpen: 'is-open',
    isVisible: 'is-visible'
};
const id = {
    authPopup: 'auth-popup'
};

const messageId = s => id.authPopup + '__' + s;
const fiveMinutes = 5 * 60;

function showMessage(context) {
    Object.values(messageType).forEach(mt => Object(_public__WEBPACK_IMPORTED_MODULE_0__["setClass"])(messageId(mt), cls.isVisible, false));
    const messageTypeToShow = contextToMessageType[context];
    if (messageTypeToShow) {
        Object(_public__WEBPACK_IMPORTED_MODULE_0__["setClass"])(messageId(messageTypeToShow), cls.isVisible, true);
    }
}

function saveRedirect(redirect) {
    _local_storage__WEBPACK_IMPORTED_MODULE_1__["default"].setItem(lsKey.redirect, redirect, fiveMinutes);
}

function showAuthPopUp(context, redirect) {
    saveRedirect(redirect)
    showMessage(context);
    Object(_public__WEBPACK_IMPORTED_MODULE_0__["setClass"])(id.authPopup, cls.isOpen, true);
    Object(_public__WEBPACK_IMPORTED_MODULE_0__["setNoScroll"])(id.authPopup, true);
}

function hideAuthPopUp() {
    Object(_public__WEBPACK_IMPORTED_MODULE_0__["setClass"])(id.authPopup, cls.isOpen, false);
    Object(_public__WEBPACK_IMPORTED_MODULE_0__["setNoScroll"])(id.authPopup, false);
}

function popAuthRedirect() {
    return _local_storage__WEBPACK_IMPORTED_MODULE_1__["default"].getItem(lsKey.redirect);
}

window.saveRedirect = saveRedirect;
window.hideAuthPopUp = hideAuthPopUp;
window.showAuthPopUp = showAuthPopUp;
window.popAuthRedirect = popAuthRedirect;


/***/ }),

/***/ "./client/src-js/button_analytics.js":
/*!*******************************************!*\
  !*** ./client/src-js/button_analytics.js ***!
  \*******************************************/
/*! no exports provided */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony import */ var _analytics__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! ./analytics */ "./client/src-js/analytics.js");
/* harmony import */ var _send_analytics__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! ./send_analytics */ "./client/src-js/send_analytics.js");



function sendAnalytics(id) {
  let eventName = 'Button Pressed';
  let properties = { id };

  Object(_send_analytics__WEBPACK_IMPORTED_MODULE_1__["sendServerAnalytics"])({
    type: 'track',
    payload: { 'event-name': eventName, properties }
  });
  Object(_analytics__WEBPACK_IMPORTED_MODULE_0__["submitAnalyticsTrack"])(eventName, properties);
}

const isButton = elm => elm.tagName === 'BUTTON';
const hasId = elm => elm.id !== '';
const hasTrackClass = elm => elm.classList.contains('track-click');

document.addEventListener('click', evt => {
  const elm = evt.target;
  if (hasId(elm) && (isButton(elm) || hasTrackClass(elm))) {
    sendAnalytics(elm.id);
  }
});


/***/ }),

/***/ "./client/src-js/clipboard.js":
/*!************************************!*\
  !*** ./client/src-js/clipboard.js ***!
  \************************************/
/*! exports provided: copyStringToClipboard */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "copyStringToClipboard", function() { return copyStringToClipboard; });
// https://techoverflow.net/2018/03/30/copying-strings-to-the-clipboard-using-pure-javascript/
function copyStringToClipboard (str) {
    var el = document.createElement('textarea');
    el.value = str;
    el.setAttribute('readonly', '');
    el.style = {position: 'absolute', left: '-9999px'};
    document.body.appendChild(el);
    el.select();
    document.execCommand('copy');
    document.body.removeChild(el);
}

window.copyStringToClipboard = copyStringToClipboard;


/***/ }),

/***/ "./client/src-js/code_highlight.js":
/*!*****************************************!*\
  !*** ./client/src-js/code_highlight.js ***!
  \*****************************************/
/*! no static exports found */
/***/ (function(module, exports) {

function highlightCodeSnippets() {
  // code highlighting doesn't work in dev mode because highlight.js is in conflict with hightlightjs from reframe10x
  const HIGHTLIGHTJS =
    'https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.18.1/highlight.min.js';

  function alreadyLoaded(url) {
    return Boolean(document.querySelector('script[src="' + url + '"]'));
  }

  function loadScript(url, callback) {
    if (alreadyLoaded(url)) {
      callback();
      return;
    }
    const script = document.createElement('script');
    script.type = 'text/javascript';
    script.onload = callback;
    script.src = url;
    document.getElementsByTagName('head')[0].appendChild(script);
  }

  function getCodeHighlightingScriptUrl(language) {
    return (
      'https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.18.1/languages/' +
      language +
      '.min.js'
    );
  }

  function highlightLanguageSnippets(language) {
    const url = getCodeHighlightingScriptUrl(language);

    loadScript(url, () => {
      document.querySelectorAll('code.' + language).forEach(block => {
        window.hljs && window.hljs.highlightBlock(block);
      });
    });
  }

  function getLanguagesUsedInSnippets() {
    const languages = Array.from(document.querySelectorAll('code'))
      .map(elm => elm.classList.value)
      .filter(elm => elm !== '')
      .map(elm => elm.split(' ')[0]);

    return Array.from(new Set(languages));
  }

  loadScript(HIGHTLIGHTJS, () => {
    getLanguagesUsedInSnippets().forEach(highlightLanguageSnippets);
  });
}

window.highlightCodeSnippets = highlightCodeSnippets;


/***/ }),

/***/ "./client/src-js/feed_paging.js":
/*!**************************************!*\
  !*** ./client/src-js/feed_paging.js ***!
  \**************************************/
/*! no exports provided */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony import */ var _search_string__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! ./search_string */ "./client/src-js/search_string.js");


function redirect(url) {
    window.location = url;
}

function setBeforeIdAndRedirect(id) {
    const searchString = Object(_search_string__WEBPACK_IMPORTED_MODULE_0__["getCurrentSearchString"])(window);
    const newSearchString = Object(_search_string__WEBPACK_IMPORTED_MODULE_0__["removeParam"])(
        Object(_search_string__WEBPACK_IMPORTED_MODULE_0__["setParamValue"])(searchString, 'newer-than', id),
        'older-than'
    );
    const newSearchStringWithInteraction = Object(_search_string__WEBPACK_IMPORTED_MODULE_0__["setParamValue"])(
        newSearchString,
        'interaction',
        1
    );
    const newUrl = Object(_search_string__WEBPACK_IMPORTED_MODULE_0__["urlWithNewSearchString"])(window, newSearchStringWithInteraction);
    redirect(newUrl);
}

function setAfterIdAndRedirect(id) {
    const searchString = Object(_search_string__WEBPACK_IMPORTED_MODULE_0__["getCurrentSearchString"])(window);
    const newSearchString = Object(_search_string__WEBPACK_IMPORTED_MODULE_0__["removeParam"])(
        Object(_search_string__WEBPACK_IMPORTED_MODULE_0__["setParamValue"])(searchString, 'older-than', id),
        'newer-than'
    );
    const newSearchStringWithInteraction = Object(_search_string__WEBPACK_IMPORTED_MODULE_0__["setParamValue"])(
        newSearchString,
        'interaction',
        1
    );
    const newUrl = Object(_search_string__WEBPACK_IMPORTED_MODULE_0__["urlWithNewSearchString"])(window, newSearchStringWithInteraction);
    redirect(newUrl);
}

function setPublicFeed(id) {
  const searchString = Object(_search_string__WEBPACK_IMPORTED_MODULE_0__["getCurrentSearchString"])(window);
  const newSearchString = Object(_search_string__WEBPACK_IMPORTED_MODULE_0__["setParamValue"])(searchString, 'public', 'true');
  const newSearchStringWithInteraction = Object(_search_string__WEBPACK_IMPORTED_MODULE_0__["setParamValue"])(
    newSearchString,
    'interaction',
    1
  );
  const newUrl = Object(_search_string__WEBPACK_IMPORTED_MODULE_0__["urlWithNewSearchString"])(window, newSearchStringWithInteraction);
  redirect(newUrl);
}

window.setBeforeIdAndRedirect = setBeforeIdAndRedirect;
window.setAfterIdAndRedirect = setAfterIdAndRedirect;
window.setPublicFeed = setPublicFeed;


/***/ }),

/***/ "./client/src-js/index.js":
/*!********************************!*\
  !*** ./client/src-js/index.js ***!
  \********************************/
/*! no exports provided */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony import */ var _auth_js__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! ./auth.js */ "./client/src-js/auth.js");
/* harmony import */ var _video_js__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! ./video.js */ "./client/src-js/video.js");
/* harmony import */ var _video_js__WEBPACK_IMPORTED_MODULE_1___default = /*#__PURE__*/__webpack_require__.n(_video_js__WEBPACK_IMPORTED_MODULE_1__);
/* harmony import */ var _photo_js__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! ./photo.js */ "./client/src-js/photo.js");
/* harmony import */ var _code_highlight_js__WEBPACK_IMPORTED_MODULE_3__ = __webpack_require__(/*! ./code_highlight.js */ "./client/src-js/code_highlight.js");
/* harmony import */ var _code_highlight_js__WEBPACK_IMPORTED_MODULE_3___default = /*#__PURE__*/__webpack_require__.n(_code_highlight_js__WEBPACK_IMPORTED_MODULE_3__);
/* harmony import */ var _button_analytics_js__WEBPACK_IMPORTED_MODULE_4__ = __webpack_require__(/*! ./button_analytics.js */ "./client/src-js/button_analytics.js");
/* harmony import */ var _newsletter_js__WEBPACK_IMPORTED_MODULE_5__ = __webpack_require__(/*! ./newsletter.js */ "./client/src-js/newsletter.js");
/* harmony import */ var _newsletter_js__WEBPACK_IMPORTED_MODULE_5___default = /*#__PURE__*/__webpack_require__.n(_newsletter_js__WEBPACK_IMPORTED_MODULE_5__);
/* harmony import */ var _tags_selector_js__WEBPACK_IMPORTED_MODULE_6__ = __webpack_require__(/*! ./tags_selector.js */ "./client/src-js/tags_selector.js");
/* harmony import */ var _trending_content_js__WEBPACK_IMPORTED_MODULE_7__ = __webpack_require__(/*! ./trending_content.js */ "./client/src-js/trending_content.js");
/* harmony import */ var _trending_content_js__WEBPACK_IMPORTED_MODULE_7___default = /*#__PURE__*/__webpack_require__.n(_trending_content_js__WEBPACK_IMPORTED_MODULE_7__);
/* harmony import */ var _navigation_js__WEBPACK_IMPORTED_MODULE_8__ = __webpack_require__(/*! ./navigation.js */ "./client/src-js/navigation.js");
/* harmony import */ var _navigation_js__WEBPACK_IMPORTED_MODULE_8___default = /*#__PURE__*/__webpack_require__.n(_navigation_js__WEBPACK_IMPORTED_MODULE_8__);
/* harmony import */ var _feed_paging_js__WEBPACK_IMPORTED_MODULE_9__ = __webpack_require__(/*! ./feed_paging.js */ "./client/src-js/feed_paging.js");
/* harmony import */ var _clipboard_js__WEBPACK_IMPORTED_MODULE_10__ = __webpack_require__(/*! ./clipboard.js */ "./client/src-js/clipboard.js");
/* harmony import */ var _init_js__WEBPACK_IMPORTED_MODULE_11__ = __webpack_require__(/*! ./init.js */ "./client/src-js/init.js");
/* harmony import */ var _ac_js__WEBPACK_IMPORTED_MODULE_12__ = __webpack_require__(/*! ./ac.js */ "./client/src-js/ac.js");
/* harmony import */ var _ac_js__WEBPACK_IMPORTED_MODULE_12___default = /*#__PURE__*/__webpack_require__.n(_ac_js__WEBPACK_IMPORTED_MODULE_12__);















/***/ }),

/***/ "./client/src-js/init.js":
/*!*******************************!*\
  !*** ./client/src-js/init.js ***!
  \*******************************/
/*! no exports provided */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony import */ var _analytics__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! ./analytics */ "./client/src-js/analytics.js");
/* harmony import */ var _tags__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! ./tags */ "./client/src-js/tags.js");



function init() {
  /* tracking */
  var consent = Object(_analytics__WEBPACK_IMPORTED_MODULE_0__["getCookie"])('wh_tracking_consent');
  var aid = Object(_analytics__WEBPACK_IMPORTED_MODULE_0__["getCookie"])('wh_aid');
  Object(_analytics__WEBPACK_IMPORTED_MODULE_0__["storeReferralData"])();

  if (!consent) {
    Object(_analytics__WEBPACK_IMPORTED_MODULE_0__["showTrackingPopup"])();
  } else {
    if (!aid) {
      Object(_analytics__WEBPACK_IMPORTED_MODULE_0__["initServerTracking"])(r => Object(_analytics__WEBPACK_IMPORTED_MODULE_0__["loadAnalytics"])());
    } else {
      Object(_analytics__WEBPACK_IMPORTED_MODULE_0__["loadAnalytics"])();
    }
  }
  /* tags */
  let tagList = document.getElementById('tag-list');
  let tagBoxes = document.getElementsByClassName(
    'tags-container--wants-js-tags'
  );

  if (
    tagList &&
    tagList.innerText &&
    tagList.innerText != '' &&
    tagBoxes &&
    tagBoxes.length > 0
  ) {
    Object(_tags__WEBPACK_IMPORTED_MODULE_1__["initTagList"])(tagList.innerText);
    for (var i = tagBoxes.length - 1; i >= 0; i--) {
      Object(_tags__WEBPACK_IMPORTED_MODULE_1__["initTags"])(tagBoxes[i]);
    }
  }
}

/*--------------------------------------------------------------------------*/

function whLoaded() {
  return typeof wh != 'undefined' && typeof wh.core != 'undefined';
}

(function() {
  window.addEventListener('DOMContentLoaded', event => {
    init();
    // load immediately if we can
    if (whLoaded()) {
      wh.core.init();
    }
    // alternatively, start interval and try again every 50ms
    else {
      var i = setInterval(function() {
        if (whLoaded()) {
          wh.core.init();
          clearInterval(i);
        }
      }, 50);
    }
  });
})();


/***/ }),

/***/ "./client/src-js/local_storage.js":
/*!****************************************!*\
  !*** ./client/src-js/local_storage.js ***!
  \****************************************/
/*! exports provided: default */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
function setItem(key, value, ttlSeconds) {
    const now = new Date()
    const item = {
        value: value,
        expiry: now.getTime() + ttlSeconds * 1000,
    }
    localStorage.setItem(key, JSON.stringify(item))
}

function getItem(key) {
    const itemStr = localStorage.getItem(key)
    if (!itemStr) {
        return null
    }
    const item = JSON.parse(itemStr)
    const now = new Date()
    if (now.getTime() > item.expiry) {
        localStorage.removeItem(key)
        return null
    }
    return item.value
}

/* harmony default export */ __webpack_exports__["default"] = ({ setItem, getItem });


/***/ }),

/***/ "./client/src-js/navigation.js":
/*!*************************************!*\
  !*** ./client/src-js/navigation.js ***!
  \*************************************/
/*! no static exports found */
/***/ (function(module, exports) {

const mobileMenuId = 'navigation';
const openMenuIconId = 'toggle-button-menu-icon';
const closeMenuIconId = 'toggle-button-close-icon';

const displayMenu = elmId => {
    const elm = document.getElementById(elmId);
    elm.style.display = 'flex';
    elm.scrollTop = 0;
};
const displayIcon = elmId =>
    (document.getElementById(elmId).style.display = 'inline-flex');
const hide = elmId => (document.getElementById(elmId).style.display = 'none');
const isMenuDisplayed = elmId =>
    Boolean(document.getElementById(elmId).style.display) &&
    document.getElementById(elmId).style.display !== 'none';


function hideMenu() {
  hide(mobileMenuId);
  hide(closeMenuIconId);
  displayIcon(openMenuIconId);
}

function toggleMenuDisplay() {
    if (isMenuDisplayed(mobileMenuId)) {
        hideMenu();
    } else {
        displayMenu(mobileMenuId);
        hide(openMenuIconId);
        displayIcon(closeMenuIconId);
    }
}

window.toggleMenuDisplay = toggleMenuDisplay;
window.hideMenu = hideMenu;


/***/ }),

/***/ "./client/src-js/newsletter.js":
/*!*************************************!*\
  !*** ./client/src-js/newsletter.js ***!
  \*************************************/
/*! no static exports found */
/***/ (function(module, exports) {

const listenNewsletterForm = () => {
  const selectors = {
    formNewsletter: '#newsletter-subscription-form',
    messageSuccess: '#newsletter-subscription-success'
  };

  const form = document.querySelector(selectors.formNewsletter);

  if (form) {
    const toggleVisibility = elm => elm.classList.toggle('is-hidden');
    const CREATE_PROSPECT_USER = window.location.origin + '/api/prospect';

    form.addEventListener('submit', e => {
      e.preventDefault();

      fetch(CREATE_PROSPECT_USER, {
        method: 'POST',
        body: new FormData(form)
      }).catch(console.error);

      const elementToDisplay = document.querySelector(selectors.messageSuccess);

      toggleVisibility(elementToDisplay);
    });
  }
};

window.listenNewsletterForm = listenNewsletterForm;


/***/ }),

/***/ "./client/src-js/photo.js":
/*!********************************!*\
  !*** ./client/src-js/photo.js ***!
  \********************************/
/*! no exports provided */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony import */ var _public__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! ./public */ "./client/src-js/public.js");
/* global PhotoSwipeUI_Default, PhotoSwipe */



function openPhotoGallery(index, images) {
  var pswpElement = document.querySelectorAll('.pswp')[0];
  var options = { index: index, bgOpacity: 0.9, history: false };
  var items = [];
  for (var i = 0; i < images.length; i++) {
    items.push({ src: images[i].url, w: images[i].width, h: images[i].height });
  }

  // mobile safari bug
  // we need to force z-index: 0 on the nav
  Object(_public__WEBPACK_IMPORTED_MODULE_0__["setClass"])('wh-navbar', 'navbar--reset-z-index', true);

  var gallery = new PhotoSwipe(
    pswpElement,
    PhotoSwipeUI_Default,
    items,
    options
  );
  gallery.listen('close', function() {
    Object(_public__WEBPACK_IMPORTED_MODULE_0__["setClass"])('wh-navbar', 'navbar--reset-z-index', false);
  });

  gallery.init();
}

window.openPhotoGallery = openPhotoGallery;


/***/ }),

/***/ "./client/src-js/public.js":
/*!*********************************!*\
  !*** ./client/src-js/public.js ***!
  \*********************************/
/*! exports provided: setClass, setNoScroll, attachOnScrollEvent */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "setClass", function() { return setClass; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "setNoScroll", function() { return setNoScroll; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "attachOnScrollEvent", function() { return attachOnScrollEvent; });
/* global bodyScrollLock */

/* Toggles class on element of given ID */
function toggleClass(id, cls) {
  var d = document.getElementById(id);
  if (d) {
    d.classList.toggle(cls);
  } else {
    console.warn(
      'Tried to toggle class on \'' + id + '\' but the element couldn\'t be found.'
    );
  }
}
// TODO: use proper event handlers, with addEventListener
// instead of this weird "interop" mechanism
window.toggleClass = toggleClass;

/* Sets class on or off element of given ID */
function setClass(id, cls, on) {
  var d = document.getElementById(id);
  if (d) {
    if (on) {
      d.classList.add(cls);
    } else {
      d.classList.remove(cls);
    }
  }
}
// TODO: use proper event handlers, with addEventListener
// instead of this weird "interop" mechanism
window.setClass = setClass;

/* Sets class on or off for all elements of given class */
function setClassOnClass(c, cls, on) {
  var ds = document.getElementsByClassName(c);
  for (var i = 0; i < ds.length; i++) {
    if (on) {
      ds[i].classList.add(cls);
    } else {
      ds[i].classList.remove(cls);
    }
  }
}
// TODO: use proper event handlers, with addEventListener
// instead of this weird "interop" mechanism
window.setClassOnClass = setClassOnClass;

/*--------------------------------------------------------------------------*/

/* Toggles global "no scroll" mode with reference element by ID */
function toggleNoScroll(id) {
  var el = document.getElementById(id);
  if (!document.body.classList.contains('no-scroll')) {
    document.body.classList.add('no-scroll');
    bodyScrollLock.disableBodyScroll(el);
  } else {
    document.body.classList.remove('no-scroll');
    bodyScrollLock.enableBodyScroll(el);
  }
}
// TODO: use proper event handlers, with addEventListener
// instead of this weird "interop" mechanism
window.toggleNoScroll = toggleNoScroll;

/* Sets global "no scroll" mode on or off with reference element by ID*/
function setNoScroll(id, on) {
  var el = document.getElementById(id);
  if (on) {
    bodyScrollLock.disableBodyScroll(el);
  } else {
    bodyScrollLock.enableBodyScroll(el);
  }
}
// TODO: use proper event handlers, with addEventListener
// instead of this weird "interop" mechanism
window.setNoScroll = setNoScroll;

/* Turns off global "no scroll" mode*/
function disableNoScroll() {
  document.body.classList.remove('no-scroll');
  bodyScrollLock.clearAllBodyScrollLocks();
}
// TODO: use proper event handlers, with addEventListener
// instead of this weird "interop" mechanism
window.disableNoScroll = disableNoScroll;

/*--------------------------------------------------------------------------*/

/* Loads a symbols file and adds it to DOM */
function loadSymbols(filename) {
  var r = new XMLHttpRequest();
  r.open('GET', '${prefix}/' + filename);
  r.onreadystatechange = function() {
    if (r.readyState == 4 && r.status == 200) {
      var container = document.createElement('div');
      container.classList.add('svg-container');
      container.innerHTML = r.responseText;
      document.body.append(container);
    }
  };
  r.send();
}
// TODO: use proper event handlers, with addEventListener
// instead of this weird "interop" mechanism
window.loadSymbols = loadSymbols;

function loadJavaScript(filename) {
  var r = new XMLHttpRequest();
  r.open('GET', '${prefix}/' + filename);
  r.onreadystatechange = function() {
    if (r.readyState == 4 && r.status == 200) {
      var container = document.createElement('script');
      container.innerHTML = r.responseText;
      document.body.append(container);
    }
  };
  r.send();
}
window.loadJavaScript = loadJavaScript;

/*--------------------------------------------------------------------------*/

function enableCarousel($carousel) {
  function getArrow(dir, f) {
    const $arrow = $carousel.querySelector('.carousel-arrow--' + dir);
    if ($arrow != null) {
      f($arrow);
    }
  }

  function highlightArrows(currentIdx) {
    if (currentIdx + 1 >= total) {
      getArrow('left', a => {
        a.classList.remove('carousel-arrow--disabled');
      });
      getArrow('right', a => {
        a.classList.add('carousel-arrow--disabled');
      });
    } else if (currentIdx <= 0) {
      getArrow('left', a => {
        a.classList.add('carousel-arrow--disabled');
      });
      getArrow('right', a => {
        a.classList.remove('carousel-arrow--disabled');
      });
    } else {
      getArrow('left', a => {
        a.classList.remove('carousel-arrow--disabled');
      });
      getArrow('right', a => {
        a.classList.remove('carousel-arrow--disabled');
      });
    }
  }

  function switchItem(i) {
    const $prevPip = $carousel.getElementsByClassName(
        'carousel-pip--active'
      )[0],
      $nextPip = $carousel.getElementsByClassName('carousel-pip')[i],
      $prevItem = $carousel.getElementsByClassName('carousel-item--active')[0],
      $nextItem = $carousel.getElementsByClassName('carousel-item')[i];
    if ($prevPip !== $nextPip) {
      $prevPip.classList.remove('carousel-pip--active');
      $nextPip.classList.add('carousel-pip--active');
      $prevItem.classList.remove('carousel-item--active');
      $nextItem.classList.add('carousel-item--active');
    }
    highlightArrows(i);
  }

  function currentItem() {
    return Array.prototype.indexOf.call(
      $carousel.getElementsByClassName('carousel-pip'),
      $carousel.getElementsByClassName('carousel-pip--active')[0]
    );
  }

  var total = $carousel.getElementsByClassName('carousel-pip').length,
    rotate = setInterval(() => switchItem((currentItem() + 1) % total), 5000);

  function slideItems(d) {
    const $newIdx = currentItem() + d;
    if ($newIdx < total && $newIdx >= 0) {
      switchItem($newIdx);
    }
  }

  function pipClicked(i) {
    switchItem(i);
    clearInterval(rotate);
  }

  function slideLeftClicked() {
    slideItems(-1);
    clearInterval(rotate);
  }

  function slideRightClicked() {
    slideItems(1);
    clearInterval(rotate);
  }

  $carousel.querySelectorAll('.carousel-pip').forEach((node, i) => {
    node.addEventListener('click', event => pipClicked(i));
  });
  getArrow('left', a => {
    a.addEventListener('click', event => slideLeftClicked());
  });
  getArrow('right', a => {
    a.addEventListener('click', event => slideRightClicked());
  });
}
// TODO: use proper event handlers, with addEventListener
// instead of this weird "interop" mechanism
window.enableCarousel = enableCarousel;

/*--------------------------------------------------------------------------*/

function setClassAtScrollPosition(e, id, cls, scrollPosition) {
  setClass(id, cls, e.scrollTop > scrollPosition || e.scrollY > scrollPosition);
}
window.setClassAtScrollPosition = setClassAtScrollPosition;

/* This function is used to attach another function f (which takes exactly one argument, i.e. the thing is attached to)
 * which is called whenever there is a scroll. This mirrors wh.pages.util/attach-on-scroll-event and is meant to be used to
 * replicate its behavior in SSR pages which do not have app-js. Note that it is not necessary to remove the ScrollListener
 * because whatever action you do from a SSR page without app-js you do a full page navigation, so there are no handlers attached anymore. */
function attachOnScrollEvent(f) {
  var el = document.getElementById('app');
  el &&
    el.addEventListener('scroll', function() {
      f(el);
    }); // desktop

  var ssrEl = document.getElementById('app-ssr');
  ssrEl &&
    ssrEl.addEventListener('scroll', function() {
      f(ssrEl);
    }); // ssr

  window.addEventListener('scroll', function() {
    f(window);
  }); // mobile
}
// TODO: use proper event handlers, with addEventListener
// instead of this weird "interop" mechanism
window.attachOnScrollEvent = attachOnScrollEvent;


/***/ }),

/***/ "./client/src-js/search_string.js":
/*!****************************************!*\
  !*** ./client/src-js/search_string.js ***!
  \****************************************/
/*! exports provided: hasParam, getParamValue, isParamHasValue, setParamValue, removeParam, toggleParamValue, urlWithNewSearchString, getCurrentSearchString */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "hasParam", function() { return hasParam; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "getParamValue", function() { return getParamValue; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "isParamHasValue", function() { return isParamHasValue; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "setParamValue", function() { return setParamValue; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "removeParam", function() { return removeParam; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "toggleParamValue", function() { return toggleParamValue; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "urlWithNewSearchString", function() { return urlWithNewSearchString; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "getCurrentSearchString", function() { return getCurrentSearchString; });
const separator = ',';
const emptyValue = '';

function addValueToValue(v1, v2) {
    return v1 + separator + v2;
}

function removeValueFromValue(complexValue, value) {
    return complexValue
        .split(separator)
        .filter(v => v !== value)
        .join(separator);
}

function hasParam(searchString, paramName) {
    return new URLSearchParams(searchString).has(paramName);
}

function getParamValue(searchString, paramName) {
    return new URLSearchParams(searchString).get(paramName);
}

function isParamHasValue(searchString, paramName, paramValue) {
    const value = getParamValue(searchString, paramName);
    return value.split(separator).some(v => v === paramValue);
}

function setParamValue(searchString, paramName, paramValue) {
    const searchParams = new URLSearchParams(searchString);
    searchParams.set(paramName, paramValue);
    return searchParams.toString();
}

function removeParam(searchString, paramName) {
    const searchParams = new URLSearchParams(searchString);
    searchParams.delete(paramName);
    return searchParams.toString();
}

function toggleParamValue(searchString, paramName, paramValue) {
    if (hasParam(searchString, paramName)) {
        const currentValue = getParamValue(searchString, paramName);
        const newValue = isParamHasValue(searchString, paramName, paramValue)
            ? // value is already there -> remove
              removeValueFromValue(currentValue, paramValue)
            : // value is not yet there -> add to existing value
              addValueToValue(currentValue, paramValue);
        return newValue === emptyValue
            ? removeParam(searchString, paramName)
            : setParamValue(searchString, paramName, newValue);
    }
    // param is not set yet
    return setParamValue(searchString, paramName, paramValue);
}

function urlWithNewSearchString(window, newSearchString) {
    const baseUrl = window.location.origin + window.location.pathname;
    return newSearchString === '' ? baseUrl : baseUrl + '?' + newSearchString;
}

function getCurrentSearchString(window) {
    return window.location.search;
}


/***/ }),

/***/ "./client/src-js/send_analytics.js":
/*!*****************************************!*\
  !*** ./client/src-js/send_analytics.js ***!
  \*****************************************/
/*! exports provided: sendServerAnalytics */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "sendServerAnalytics", function() { return sendServerAnalytics; });
/* harmony import */ var _add_sourcing__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! ./add_sourcing */ "./client/src-js/add_sourcing.js");


function sendServerAnalytics(body, onsuccess = null) {
  var i = null;

  let send = function() {
    if (window.wh_analytics && window.wh_analytics.init) {
      window.clearInterval(i);

      const bodyWithSourcing = Object(_add_sourcing__WEBPACK_IMPORTED_MODULE_0__["addSourcing"])(body, window.wh_analytics);

      var r = new XMLHttpRequest();
      r.timeout = 10000;
      r.open('POST', '/api/analytics');
      r.setRequestHeader('Content-Type', 'application/json; charset=UTF-8');
      r.onloadend = function() {
        if (r.status != 200) {
          console.log('analytics failed: ' + r.status);
        } else if (onsuccess) {
          onsuccess(r);
        }
      };
      r.send(JSON.stringify(bodyWithSourcing));
    }
  };

  if (window.wh_analytics && window.wh_analytics.init) {
    send();
  } else {
    // try and send every 1000ms if no consent
    // TODO: remove this mechanism in favor of some proper queue or Pub/Sub
    i = window.setInterval(send, 1000);
  }
}
// TODO: use proper event handlers, with addEventListener
// instead of this weird "interop" mechanism
window.sendServerAnalytics = sendServerAnalytics;


/***/ }),

/***/ "./client/src-js/tags.js":
/*!*******************************!*\
  !*** ./client/src-js/tags.js ***!
  \*******************************/
/*! exports provided: initTagList, initTags */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "initTagList", function() { return initTagList; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "initTags", function() { return initTags; });
/* harmony import */ var _util__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! ./util */ "./client/src-js/util.js");


// Our code, currently, relies on whTags being public.
// TODO: Investigate and fix properly without binding it to window
window.whTags = null;
const whTagGroupLimit = 16;

function resetTagsElementVisibility(tagBox) {
  let tagsElements = tagBox.querySelectorAll('.tags--top-level');
  for (let i = tagsElements.length - 1; i >= 0; i--) {
    const hasSelectedTags =
      tagsElements[i].getElementsByClassName('tag--selected').length > 0;
    if (hasSelectedTags) {
      tagsElements[i].classList.add('tags--has-selected-tags');
    } else {
      tagsElements[i].classList.remove('tags--has-selected-tags');
    }
  }
}
window.resetTagsElementVisibility = resetTagsElementVisibility;

function resetTagVisibility(tagGroup) {
  let tags = tagGroup.querySelectorAll(
    '.tags--unselected .tag:not(.tag--selected):not(.tag--filtered)'
  );
  for (let i = tags.length - 1; i >= 0; i--) {
    if (i > whTagGroupLimit) {
      tags[i].classList.add('tag--hidden');
    } else {
      tags[i].classList.remove('tag--hidden');
    }
  }
  if (tags.length === 0) {
    tagGroup.classList.add('tag-group--empty');
  } else {
    tagGroup.classList.remove('tag-group--empty');
  }
}

function filterTags(tagBoxId, textInput) {
  let tagBox = document.getElementById(tagBoxId);
  let tagGroups = tagBox.querySelectorAll('.tags--unselected .tag-group');
  let unselectedTags = tagBox.querySelectorAll('.tags--unselected .tag');

  if (textInput.value && textInput.value !== '') {
    let tagSearch = textInput.value.toLowerCase();

    for (let i = unselectedTags.length - 1; i >= 0; i--) {
      let tag = unselectedTags[i];
      let tagLabel = tag.attributes['data-label'].value.toLowerCase();
      if (!tagLabel.includes(tagSearch)) {
        tag.classList.add('tag--filtered');
      } else {
        tag.classList.remove('tag--filtered');
      }
    }

    tagBox.classList.remove('tags-container--collapsed');
  } else {
    // search box is empty so remove all filtered classes
    for (let i = unselectedTags.length - 1; i >= 0; i--) {
      unselectedTags[i].classList.remove('tag--filtered');
    }
    tagBox.classList.add('tags-container--collapsed');
  }

  // reset visibility
  for (let j = tagGroups.length - 1; j >= 0; j--) {
    resetTagVisibility(tagGroups[j]);
  }
}
window.filterTags = filterTags;

function handleTagChange(tagBox, queryParamKey) {
  if (tagBox.focusedTag) {
    // apply skeleton class to all company cards AND all tags
    let companyCards = document.getElementsByClassName(
      'companies__company company-card'
    );
    for (let i = companyCards.length - 1; i >= 0; i--) {
      companyCards[i].classList.add('skeleton');
      let tags = companyCards[i].getElementsByClassName('tag');
      for (let j = tags.length - 1; j >= 0; j--) {
        tags[j].classList.add('tag--skeleton');
      }
    }

    let tagElement = tagBox.focusedTag;
    let tagQueryId = tagBox.focusedTagQueryId;
    let adding = tagElement.classList.contains('tag--selected');
    var url = Object(_util__WEBPACK_IMPORTED_MODULE_0__["setQueryParam"])(null, 'interaction', 1);
    url = Object(_util__WEBPACK_IMPORTED_MODULE_0__["deleteQueryParam"])(url, 'page');
    if (tagQueryId.endsWith(':size')) {
      if (adding) {
        return Object(_util__WEBPACK_IMPORTED_MODULE_0__["setQueryParam"])(url, 'size', tagQueryId.split(':')[0]);
      } else {
        return Object(_util__WEBPACK_IMPORTED_MODULE_0__["deleteQueryParam"])(url, 'size');
      }
    } else {
      if (adding) {
        return Object(_util__WEBPACK_IMPORTED_MODULE_0__["addQueryParam"])(url, queryParamKey, tagQueryId);
      } else {
        return Object(_util__WEBPACK_IMPORTED_MODULE_0__["removeQueryParam"])(url, queryParamKey, tagQueryId);
      }
    }
  }
}
window.handleTagChange = handleTagChange;

function createTagQueryId(slug, type) {
  return slug + ':' + type;
}

function parseSelectedTags() {
  var currentUrl = new URL(location.href);
  return currentUrl.searchParams.getAll('tag');
}

function parseSize() {
  var currentUrl = new URL(location.href);
  return currentUrl.searchParams.get('size');
}

function createIcon(icon) {
  const svgns = 'http://www.w3.org/2000/svg';
  const xlinkns = 'http://www.w3.org/1999/xlink';
  let svg = document.createElementNS(svgns, 'svg');
  let use = document.createElementNS(svgns, 'use');
  svg.classList.add('icon');
  svg.classList.add('icon--' + icon);
  use.setAttributeNS(xlinkns, 'href', '#' + icon);
  svg.appendChild(use);
  return svg;
}

function createTagGroup(parent, tagGroupType) {
  let tagGroupElement = document.createElement('div');
  let tagGroupInnerElement = document.createElement('ul');
  tagGroupElement.classList.add('tag-group');
  tagGroupElement.classList.add('tag-group--' + tagGroupType);
  tagGroupInnerElement.classList.add('tags');
  tagGroupElement.appendChild(tagGroupInnerElement);
  parent.appendChild(tagGroupElement);
  return tagGroupInnerElement;
}

function createTag({ tag, parent, grandParent, isSelected, hasIcon }) {
  let parents = grandParent.getElementsByClassName('tags');
  const tagElement = document.createElement('li');
  const tagSlugClass = 'tag--slug-' + tag.slug;
  const tagTypeClass = 'tag--type-' + tag.type;
  const tagQueryId = createTagQueryId(tag.slug, tag.type);
  tagElement.setAttribute('data-label', tag.label);
  tagElement.classList.add('tag');
  tagElement.classList.add(tagTypeClass);
  tagElement.classList.add(tagSlugClass);
  if (tag.subtype) {
    tagElement.classList.add('tag--subtype-' + tag.subtype);
  }
  if (isSelected) {
    tagElement.classList.add('tag--selected');
    // add a class to both parents too help with styling
    for (let i = parents.length - 1; i >= 0; i--) {
      parents[i].classList.add('tags--has-selected-tags');
    }
  }
  if (hasIcon) {
    tagElement.appendChild(createIcon('close'));
  }
  tagElement.onclick = function() {
    let elements = grandParent.querySelectorAll(
      '.' + tagSlugClass + '.' + tagTypeClass
    );
    for (let i = elements.length - 1; i >= 0; i--) {
      elements[i].classList.toggle('tag--selected');
    }
    // close the expansion
    grandParent.classList.add('tags-container--collapsed');
    if (grandParent.onchange) {
      grandParent.focusedTagQueryId = tagQueryId;
      grandParent.focusedTag = tagElement;
      grandParent.onchange(tagQueryId, tagElement);
      grandParent.focusedTagQueryId = grandParent.focusedTag = null;
    }
    resetTagsElementVisibility(grandParent);
  };

  let tagSpan = document.createElement('span');
  tagSpan.innerText = tag.label;
  tagElement.appendChild(tagSpan);
  parent.appendChild(tagElement);
}

const isElementCorrect = (element, label, type) => {
  return (
    element.innerText.toLowerCase() === label.toLowerCase() &&
    element.classList.contains('tag--type-' + type.toLowerCase())
  );
};

/**
 * Clicks on tag which includes provided label in text
 *                   & includes provided type in classlist.
 * Tag is selected from unselected tags area.
 *
 * @param {string} label
 * @param {string} type
 *
 * @example
 *     clickOnTag('Remote Working', 'benefit')
 *     clickOnTag('Scala', 'tech')
 */
const clickOnTag = (label, type) => {
  const UNSELECTED_TAGS = '.tags--unselected';
  const TAG = '.tag';

  const tagElement = Array.from(
    document.querySelector(UNSELECTED_TAGS).querySelectorAll(TAG)
  ).filter(elm => isElementCorrect(elm, label, type))[0];

  if (tagElement) {
    tagElement.click();
  }
};
window.clickOnTag = clickOnTag;

function initTagList(tagJson) {
  window.whTags =
    !window.whTags && tagJson ? JSON.parse(tagJson).tags : window.whTags;
}

function initTags(tagBox) {
  if (tagBox && window.whTags && window.whTags.length > 0) {
    let selectedTags = parseSelectedTags();
    let sizeValue = parseSize();
    let tagParents = [
      {
        parent: tagBox.querySelector('ul.tags.tags--unselected'),
        groups: true,
        icon: false,
        matchMsg: true
      },
      {
        parent: tagBox.querySelector('ul.tags.tags--selected'),
        groups: false,
        icon: true,
        matchMsg: false
      }
    ];
    let tagGroups = [];
    // remove any children from parent element such as loading msgs
    for (let k = tagParents.length - 1; k >= 0; k--) {
      let tagParent = tagParents[k].parent;
      let useGroups = tagParents[k].groups;
      let showMatchMsg = tagParents[k].matchMsg;
      while (tagParent.firstChild) {
        tagParent.removeChild(tagParent.firstChild);
      }
      let currentGroup = useGroups
        ? null
        : createTagGroup(tagParent, window.whTags[0].type);
      let currentType = null;
      for (let j = 0; j < window.whTags.length; j++) {
        let tag = window.whTags[j];
        // this group/type management code relies on tags being sorted by type
        if (useGroups && currentType != window.whTags[j].type) {
          currentType = window.whTags[j].type;
          currentGroup = createTagGroup(tagParent, currentType);
          tagGroups.push(currentGroup);
        }
        createTag({
          tag: tag,
          parent: currentGroup,
          grandParent: tagBox,
          isSelected:
            -1 != selectedTags.indexOf(createTagQueryId(tag.slug, tag.type)) ||
            (tag.type === 'size' && tag.slug === sizeValue),
          hasIcon: tagParents[k].icon
        });
      }
      for (let j = tagGroups.length - 1; j >= 0; j--) {
        resetTagVisibility(tagGroups[j]);
      }
      // add a text span after the tag groups
      if (showMatchMsg) {
        let noMatchingSpan = document.createElement('span');
        noMatchingSpan.innerText = 'No tags matched the search term!';
        tagParent.appendChild(noMatchingSpan);
      }
    }
  }
}
window.initTags = initTags;

function invertTag(tag) {
  tag.classList.toggle('tag--inverted');
}
window.invertTag = invertTag;


/***/ }),

/***/ "./client/src-js/tags_selector.js":
/*!****************************************!*\
  !*** ./client/src-js/tags_selector.js ***!
  \****************************************/
/*! no exports provided */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony import */ var _search_string__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! ./search_string */ "./client/src-js/search_string.js");


function redirect(url) {
    window.location = url;
}

function toggleTagAndRedirect(tagId) {
    const searchString = Object(_search_string__WEBPACK_IMPORTED_MODULE_0__["getCurrentSearchString"])(window);
    const newSearchStringWithoutActivityId = Object(_search_string__WEBPACK_IMPORTED_MODULE_0__["removeParam"])(
        Object(_search_string__WEBPACK_IMPORTED_MODULE_0__["removeParam"])(searchString, 'older-than'),
        'newer-than'
    );
    const newSearchString = Object(_search_string__WEBPACK_IMPORTED_MODULE_0__["toggleParamValue"])(
        newSearchStringWithoutActivityId,
        'tags',
        tagId
    );
    const newSearchStringWithInteraction = Object(_search_string__WEBPACK_IMPORTED_MODULE_0__["setParamValue"])(
        newSearchString,
        'interaction',
        1
    );
    const newUrl = Object(_search_string__WEBPACK_IMPORTED_MODULE_0__["urlWithNewSearchString"])(window, newSearchStringWithInteraction);
    redirect(newUrl);
}

window.toggleTagAndRedirect = toggleTagAndRedirect;


/***/ }),

/***/ "./client/src-js/trending_content.js":
/*!*******************************************!*\
  !*** ./client/src-js/trending_content.js ***!
  \*******************************************/
/*! no static exports found */
/***/ (function(module, exports) {

const sectionId = {
    blogs: 'trending-content-blogs',
    issues: 'trending-content-issues',
    jobs: 'trending-content-jobs'
};

const displaySection = elmId => (document.getElementById(elmId).style = 'display: grid');
const hideSection = elmId => (document.getElementById(elmId).style = '');
const isSectionDisplayed = elmId => Boolean(document.getElementById(elmId).style.display);
const areAllSectionsHidden = () =>
    ![sectionId.blogs, sectionId.issues, sectionId.jobs].some(isSectionDisplayed);
const getDisplayedSection = () =>
    [sectionId.blogs, sectionId.issues, sectionId.jobs].filter(isSectionDisplayed)[0];

function toggleDisplay(type) {
    const sectId = {
        blogs: sectionId.blogs,
        issues: sectionId.issues,
        jobs: sectionId.jobs
    }[type];

    if (areAllSectionsHidden()) {
        displaySection(sectId);
    } else if (!areAllSectionsHidden() && !isSectionDisplayed(sectId)) {
        hideSection(getDisplayedSection());
        displaySection(sectId);
    } else if (!areAllSectionsHidden() && isSectionDisplayed(sectId)) {
        hideSection(sectId);
    }
}

window.toggleDisplay = toggleDisplay;


/***/ }),

/***/ "./client/src-js/util.js":
/*!*******************************!*\
  !*** ./client/src-js/util.js ***!
  \*******************************/
/*! exports provided: extractQueryParams, mapAsQueryString, setQueryParam, addQueryParam, removeQueryParam, deleteQueryParam, isAppPresent */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "extractQueryParams", function() { return extractQueryParams; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "mapAsQueryString", function() { return mapAsQueryString; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "setQueryParam", function() { return setQueryParam; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "addQueryParam", function() { return addQueryParam; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "removeQueryParam", function() { return removeQueryParam; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "deleteQueryParam", function() { return deleteQueryParam; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "isAppPresent", function() { return isAppPresent; });
function extractQueryParams(queryString) {
  if (queryString == null) {
    return new Map();
  } else {
    const vars = queryString.split('&');
    var o = new Map();
    for (var i = 0; i < vars.length; i++) {
      const pair = vars[i].split('=');
      o.set(decodeURIComponent(pair[0]), decodeURIComponent(pair[1]));
    }
    return o;
  }
}

function mapAsQueryString(m) {
  return Array.from(m.keys())
    .map(key => key + '=' + m.get(key))
    .join('&');
}

function setQueryParam(url, key, value) {
  url = url || new URL(location.href);
  url.searchParams.set(key, value);
  return url;
}
window.setQueryParam = setQueryParam;

function addQueryParam(url, key, value) {
  url = url || new URL(location.href);
  url.searchParams.append(key, value);
  return url;
}

function removeQueryParam(url, key, value) {
  var currentUrl = url || new URL(location.href);
  url = new URL(
    [location.protocol, '//', location.host, location.pathname].join('')
  );
  for (const [k, v] of currentUrl.searchParams.entries()) {
    if (!(k == key && v == value)) {
      url.searchParams.append(k, v);
    }
  }
  return url;
}

function deleteQueryParam(url, key) {
  url = url || new URL(location.href);
  url.searchParams.delete(key);
  return url;
}
window.deleteQueryParam = deleteQueryParam;

function isAppPresent() {
  return document.getElementById('data-init') !== null;
}

/***/ }),

/***/ "./client/src-js/video.js":
/*!********************************!*\
  !*** ./client/src-js/video.js ***!
  \********************************/
/*! no static exports found */
/***/ (function(module, exports) {

/* global setClass */

var currentVideo = null;

function closeVideoPlayer() {
  document.getElementById('video-player-container').removeChild(currentVideo);
  currentVideo = null;
  setClass('video-player-container', 'is-open', false);
}
window.closeVideoPlayer = closeVideoPlayer;

function openVideoPlayer(youtubeId) {
  if (!currentVideo) {
    setClass('video-player-container', 'is-open', true);

    var videoWrapperOuter = document.createElement('div');
    videoWrapperOuter.classList.add('video-wrapper-outer');

    var videoWrapperInner = document.createElement('div');
    videoWrapperInner.classList.add('video-wrapper-inner');

    var iframe = document.createElement('iframe');
    iframe.classList.add('iframe-video--youtube');
    iframe.setAttribute(
      'src',
      'https://www.youtube.com/embed/' + youtubeId + '?autoplay=1'
    );
    iframe.setAttribute('frameborder', '0');
    iframe.setAttribute(
      'allow',
      'accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture'
    );
    iframe.setAttribute('allowfullscreen', '');

    videoWrapperInner.appendChild(iframe);
    videoWrapperOuter.appendChild(videoWrapperInner);
    document
      .getElementById('video-player-container')
      .appendChild(videoWrapperOuter);
    currentVideo = videoWrapperOuter;
  }
}
window.openVideoPlayer = openVideoPlayer;


/***/ })

/******/ });
//# sourceMappingURL=public.js.map