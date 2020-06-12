export function extractQueryParams(queryString) {
  if (queryString == null) {
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

export function mapAsQueryString(m) {
  return Array.from(m.keys())
    .map(key => key + '=' + m.get(key))
    .join('&');
}

export function setQueryParam(url, key, value) {
  url = url || new URL(location.href);
  url.searchParams.set(key, value);
  return url;
}
window.setQueryParam = setQueryParam;

export function addQueryParam(url, key, value) {
  url = url || new URL(location.href);
  url.searchParams.append(key, value);
  return url;
}

export function removeQueryParam(url, key, value) {
  var currentUrl = url || new URL(location.href);
  url = new URL(
    [location.protocol, '//', location.host, location.pathname].join('')
  );
  for (const [k, v] of currentUrl.searchParams.entries()) {
    if (!(k == key && v == value)) {
      url.searchParams.append(k, v);
    }
  }
  return url;
}

export function deleteQueryParam(url, key) {
  url = url || new URL(location.href);
  url.searchParams.delete(key);
  return url;
}
window.deleteQueryParam = deleteQueryParam;

export function isAppPresent() {
  return document.getElementById('data-init') !== null;
}