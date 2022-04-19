import { addSourcing } from './add_sourcing'

export function sendServerAnalytics(body, onsuccess = null) {
    var i = null

    let send = function () {
        if (window.wh_analytics && window.wh_analytics.init) {
            window.clearInterval(i)

            const bodyWithSourcing = addSourcing(body, window.wh_analytics)

            var r = new XMLHttpRequest()
            r.timeout = 10000
            r.open('POST', '/api/analytics')
            r.setRequestHeader('Content-Type', 'application/json; charset=UTF-8')
            r.onloadend = function () {
                if (r.status != 200) {
                    console.log('analytics failed: ' + r.status)
                } else if (onsuccess) {
                    onsuccess(r)
                }
            }
            r.send(JSON.stringify(bodyWithSourcing))
        }
    }

    if (window.wh_analytics && window.wh_analytics.init) {
        send()
    } else {
        // try and send every 1000ms if no consent
        // TODO: remove this mechanism in favor of some proper queue or Pub/Sub
        i = window.setInterval(send, 1000)
    }
}
// TODO: use proper event handlers, with addEventListener
// instead of this weird "interop" mechanism
window.sendServerAnalytics = sendServerAnalytics
