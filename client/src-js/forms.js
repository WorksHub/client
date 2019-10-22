function setQueryParam(key, value) {
    var url = new URL(location.href);
    url.searchParams.set(key, value);
    window.location = url.href;
}

function setQueryParams(params) {
    var url = new URL([location.protocol, '//', location.host, location.pathname].join(''));
    for(key in params) {
        url.searchParams.set(key, params[key]);
    }
    window.location = url.href;
}
