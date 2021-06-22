import { setClass, setNoScroll } from './public';
import customLocalStorage from './local_storage';

const messageType = {
    apply: 'apply',
    publish: 'publish',
    contribute: 'contribute',
    searchJobs: 'search-jobs',
    seeMore: 'see-more',
    upvote: 'upvote',
    issue: 'issue',
    save: 'save',
    saveBlog: 'save-blog',
    savedJobs: 'saved-jobs',
    recommendedJobs: 'recommended-jobs',
    appliedJobs: 'applied-jobs',
    seeApplication: 'see-application'
};
const contextToMessageType = {
    'homepage-jobcard-apply': messageType.apply,
    contribute: messageType.contribute,
    'jobcard-apply': messageType.apply,
    'jobsboard-recommended': messageType.recommendedJobs,
    'jobsboard-save': messageType.savedJobs,
    'jobcard-save': messageType.save,
    'save-blog': messageType.saveBlog,
    'jobsboard-applied': messageType.appliedJobs,
    'jobpage-apply': messageType.apply,
    'jobpage-publish': messageType.publish,
    'search-jobs': messageType.searchJobs,
    'jobpage-see-more': messageType.seeMore,
    [messageType.seeApplication]: messageType.seeApplication,
    upvote: messageType.upvote,
    issue: messageType.issue
};

const contextToAdditionalContent = {
    [messageType.seeApplication]: messageType.seeApplication
};

const lsKey = {
    redirect: 'wh_auth.redirect_url',
    redirectCurrentUrl: 'wh_auth.redirect_current_url'
};
const cls = {
    isOpen: 'is-open',
    isVisible: 'is-visible'
};
const id = {
    authPopup: 'auth-popup',
    additionalContent: 'additional-content'
};

const messageId = s => id.authPopup + '__' + s;
const additionalContentId = s => id.additionalContent + '__' + s;
const fiveMinutes = 5 * 60;

function showMessage(context) {
    Object.values(messageType).forEach(mt => {
        setClass(messageId(mt), cls.isVisible, false);
        setClass(additionalContentId(mt), cls.isVisible, false);
    });

    const messageTypeToShow = contextToMessageType[context];
    if (messageTypeToShow) {
        setClass(messageId(messageTypeToShow), cls.isVisible, true);
    }

    const additionalContent = contextToAdditionalContent[context] || 'default';
    setClass(additionalContentId(additionalContent), cls.isVisible, true);
}

function saveRedirect(redirect) {
    // I'm deeply sorry
    if (redirect === ':current-url') {
        customLocalStorage.setItem(
            lsKey.redirectCurrentUrl,
            window.location.pathname + window.location.search,
            fiveMinutes
        );
    } else {
        customLocalStorage.setItem(lsKey.redirect, redirect, fiveMinutes);
    }
}

function showAuthPopUp(context, redirect) {
    saveRedirect(redirect);
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

function popAuthRedirectCurrentUrl() {
    return customLocalStorage.getItem(lsKey.redirectCurrentUrl);
}

window.saveRedirect = saveRedirect;
window.hideAuthPopUp = hideAuthPopUp;
window.showAuthPopUp = showAuthPopUp;
window.popAuthRedirect = popAuthRedirect;
window.popAuthRedirectCurrentUrl = popAuthRedirectCurrentUrl;
