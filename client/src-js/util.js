function extractQueryParams(queryString) {
    if(queryString == null) {
        return new Map();
    } else {
        const vars = queryString.split('&');
        var o = new Map();
        for (var i = 0; i < vars.length; i++) {
            const pair = vars[i].split('=');
            o.set(decodeURIComponent(pair[0]), decodeURIComponent(pair[1]));
        }
        return o;
    }
}

function mapAsQueryString(m) {
    return queryString = Array.from(m.keys()).map(key => key + '=' + m.get(key)).join('&');
}

function setQueryParam(url, key, value) {
    var url = url || new URL(location.href);
    url.searchParams.set(key, value);
    return url;
}

function addQueryParam(url, key, value) {
    var url = url || new URL(location.href);
    url.searchParams.append(key, value);
    return url;
}

function removeQueryParam(url, key, value) {
    var currentUrl = url || new URL(location.href);
    var url = new URL([location.protocol, '//', location.host, location.pathname].join(''));
    for(const [k, v] of currentUrl.searchParams.entries()) {
        if(!(k == key && v == value)) {
            url.searchParams.append(k, v);
        }
    };
    return url;
}

function setQueryParams(url, params) {
    var url = url || new URL([location.protocol, '//', location.host, location.pathname].join(''));
    for(key in params) {
        url.searchParams.set(key, params[key]);
    }
    return url;
}
