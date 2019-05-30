function getQueryParams(queryString) {
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
