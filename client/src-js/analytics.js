/* global analytics */
import { isAppPresent, mapAsQueryString, extractQueryParams } from './util';
import { sendServerAnalytics } from './send_analytics';
import { addSourcing } from './add_sourcing';
import { setClass } from './public';

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

export function getCookie(name) {
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

export function storeReferralData() {
  // get existing
  const existingQps = localStorage.getItem('wh_analytics_utms');
  const existingUtms = extractUtmFields(extractQueryParams(existingQps));
  const existingRef = localStorage.getItem('wh_analytics_referrer');
  const existingLP = localStorage.getItem('wh_analytics_landing_page');
  const existingTitle = localStorage.getItem('wh_analytics_title');
  // get current
  const currentQps = window.location.search.substring(1);
  const currentUtms = extractUtmFields(extractQueryParams(currentQps));
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
    mapAsQueryString(window.wh_analytics.utms)
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
  setClass('tracking-popups', 'is-open', false);
}

export function showTrackingPopup() {
  setClass('tracking-popups', 'is-open', true);
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
  const sourcing = addSourcing({}, atx).sourcing;
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
  sendServerAnalytics({ type: 'page', payload: createPageProps(atx) });
}

export function initServerTracking(onsuccess = null) {
  window.wh_analytics.init = true;
  sendServerAnalytics({ type: 'init' }, onsuccess);
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

export function loadAnalytics() {
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
          if (!isAppPresent()) {
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

export function resetAnalytics() {
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
    const sourcing = addSourcing({}, atx).sourcing;
    const pageProps = createPageProps(atx);
    analytics.page(pageProps, { context: sourcing });
  });
}

export function submitAnalyticsTrack(evt, prps) {
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
    initServerTracking(r => loadAnalytics());
  }
}

window.agreeToTracking = agreeToTracking;
