function setItem(key, value, ttlSeconds) {
    const now = new Date()
    const item = {
        value: value,
        expiry: now.getTime() + ttlSeconds * 1000,
    }
    localStorage.setItem(key, JSON.stringify(item))
}

function getItem(key) {
    const itemStr = localStorage.getItem(key)
    if (!itemStr) {
        return null
    }
    const item = JSON.parse(itemStr)
    const now = new Date()
    if (now.getTime() > item.expiry) {
        localStorage.removeItem(key)
        return null
    }
    return item.value
}

export default { setItem, getItem }
