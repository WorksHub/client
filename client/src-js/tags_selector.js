import {getCurrentSearchString, toggleParamValue, urlWithNewSearchString, removeParam} from './search_string'

function redirect(url) {
    window.location = url
}

function toggleTagAndRedirect(tagId) {
    const searchString = getCurrentSearchString(window)
    const newSearchString = toggleParamValue(searchString, "tags", tagId)
    const newUrl = urlWithNewSearchString(window, newSearchString)
    redirect(newUrl)
}

function removeSelectedTagsAndRedirect() {
    const searchString = getCurrentSearchString(window)
    const newSearchString = removeParam(searchString, "tags")
    const newUrl = urlWithNewSearchString(window, newSearchString)
    redirect(newUrl)
}

window.toggleTagAndRedirect = toggleTagAndRedirect
window.removeSelectedTagsAndRedirect = removeSelectedTagsAndRedirect