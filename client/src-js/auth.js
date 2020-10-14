import { setClass, setNoScroll } from './public';
import customLocalStorage from './local_storage';

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
    Object.values(messageType).forEach(mt => setClass(messageId(mt), cls.isVisible, false));
    const messageTypeToShow = contextToMessageType[context];
    if (messageTypeToShow) {
        setClass(messageId(messageTypeToShow), cls.isVisible, true);
    }
}

function showAuthPopUp(context, redirect) {
    customLocalStorage.setItem(lsKey.redirect, redirect, fiveMinutes);
    showMessage(context);
    setClass(id.authPopup, cls.isOpen, true);
    setNoScroll(id.authPopup, true);
}

function hideAuthPopUp() {
    setClass(id.authPopup, cls.isOpen, false);
    setNoScroll(id.authPopup, false);
}

function popAuthRedirect() {
    return customLocalStorage.getItem(lsKey.redirect);
}

window.hideAuthPopUp = hideAuthPopUp;
window.showAuthPopUp = showAuthPopUp;
window.popAuthRedirect = popAuthRedirect;
