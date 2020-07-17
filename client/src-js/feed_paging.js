import {
    getCurrentSearchString,
    toggleParamValue,
    urlWithNewSearchString,
    removeParam,
    setParamValue
} from './search_string';

function redirect(url) {
    window.location = url;
}

function setBeforeIdAndRedirect(id) {
    const searchString = getCurrentSearchString(window);
    const newSearchString = removeParam(
        setParamValue(searchString, 'newer-than', id),
        'older-than'
    );
    const newSearchStringWithInteraction = setParamValue(
        newSearchString,
        'interaction',
        1
    );
    const newUrl = urlWithNewSearchString(window, newSearchStringWithInteraction);
    redirect(newUrl);
}

function setAfterIdAndRedirect(id) {
    const searchString = getCurrentSearchString(window);
    const newSearchString = removeParam(
        setParamValue(searchString, 'older-than', id),
        'newer-than'
    );
    const newSearchStringWithInteraction = setParamValue(
        newSearchString,
        'interaction',
        1
    );
    const newUrl = urlWithNewSearchString(window, newSearchStringWithInteraction);
    redirect(newUrl);
}

window.setBeforeIdAndRedirect = setBeforeIdAndRedirect;
window.setAfterIdAndRedirect = setAfterIdAndRedirect;
