import { addSourcing } from './add-sourcing';

const wh_analytics = window.wh_analytics || {};

export function sendServerAnalytics(body, onsuccess = null) {
  var i = null;
  let send = function() {
    if (wh_analytics.init) {
      window.clearInterval(i);

      const bodyWithSourcing = addSourcing(body, wh_analytics);

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
      r.send(JSON.stringify(body));
    }
  };

  if (wh_analytics.init) {
    send();
  } else {
    // try and send every 1000ms if no consent
    i = window.setInterval(send, 1000);
  }
}
// TODO: use proper event handlers, with addEventListener
// instead of this weird "interop" mechanism
window.sendServerAnalytics = sendServerAnalytics;
