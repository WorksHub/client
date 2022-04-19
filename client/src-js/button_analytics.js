import { submitAnalyticsTrack } from './analytics'
import { sendServerAnalytics } from './send_analytics'

function sendAnalytics(id) {
    let eventName = 'Button Pressed'
    let properties = { id }

    sendServerAnalytics({
        type: 'track',
        payload: { 'event-name': eventName, properties },
    })
    submitAnalyticsTrack(eventName, properties)
}

const isButton = elm => elm.tagName === 'BUTTON'
const hasId = elm => elm.id !== ''
const hasTrackClass = elm => elm.classList.contains('track-click')

document.addEventListener('click', evt => {
    const elm = evt.target
    if (hasId(elm) && (isButton(elm) || hasTrackClass(elm))) {
        sendAnalytics(elm.id)
    }
})
