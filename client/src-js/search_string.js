const separator = ','
const emptyValue = ''

function addValueToValue(v1, v2) {
    return v1 + separator + v2
}

function removeValueFromValue(complexValue, value) {
    return complexValue
        .split(separator)
        .filter(v => v !== value)
        .join(separator)
}

export function hasParam(searchString, paramName) {
    return new URLSearchParams(searchString).has(paramName)
}

export function getParamValue(searchString, paramName) {
    return new URLSearchParams(searchString).get(paramName)
}

export function isParamHasValue(searchString, paramName, paramValue) {
    const value = getParamValue(searchString, paramName)
    return value.split(separator).some(v => v === paramValue)
}

export function setParamValue(searchString, paramName, paramValue) {
    const searchParams = new URLSearchParams(searchString)
    searchParams.set(paramName, paramValue)
    return searchParams.toString()
}

export function removeParam(searchString, paramName) {
    const searchParams = new URLSearchParams(searchString)
    searchParams.delete(paramName)
    return searchParams.toString()
}

export function toggleParamValue(searchString, paramName, paramValue) {
    if (hasParam(searchString, paramName)) {
        const currentValue = getParamValue(searchString, paramName)
        const newValue = isParamHasValue(searchString, paramName, paramValue)
            ? // value is already there -> remove
              removeValueFromValue(currentValue, paramValue)
            : // value is not yet there -> add to existing value
              addValueToValue(currentValue, paramValue)
        return newValue === emptyValue
            ? removeParam(searchString, paramName)
            : setParamValue(searchString, paramName, newValue)
    }
    // param is not set yet
    return setParamValue(searchString, paramName, paramValue)
}

export function urlWithNewSearchString(window, newSearchString) {
    const baseUrl = window.location.origin + window.location.pathname
    return newSearchString === '' ? baseUrl : baseUrl + '?' + newSearchString
}

export function getCurrentSearchString(window) {
    return window.location.search
}
