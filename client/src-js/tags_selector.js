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

function toggleTagAndRedirect(tagId) {
    const searchString = getCurrentSearchString(window);
    const newSearchStringWithoutActivityId = removeParam(
        removeParam(searchString, 'older-than'),
        'newer-than'
    );
    const newSearchString = toggleParamValue(
        newSearchStringWithoutActivityId,
        'tags',
        tagId
    );
    const newSearchStringWithInteraction = setParamValue(
        newSearchString,
        'interaction',
        1
    );
    const newUrl = urlWithNewSearchString(window, newSearchStringWithInteraction);
    redirect(newUrl);
}

function removeSelectedTagsAndRedirect() {
    const searchString = getCurrentSearchString(window);
    const newSearchString = removeParam(searchString, 'tags');
    const newUrl = urlWithNewSearchString(window, newSearchString);
    redirect(newUrl);
}

window.toggleTagAndRedirect = toggleTagAndRedirect;
window.removeSelectedTagsAndRedirect = removeSelectedTagsAndRedirect;
