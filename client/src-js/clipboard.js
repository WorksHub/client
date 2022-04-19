// https://techoverflow.net/2018/03/30/copying-strings-to-the-clipboard-using-pure-javascript/
export function copyStringToClipboard(str) {
    var el = document.createElement('textarea')
    el.value = str
    el.setAttribute('readonly', '')
    el.style = { position: 'absolute', left: '-9999px' }
    document.body.appendChild(el)
    el.select()
    document.execCommand('copy')
    document.body.removeChild(el)
}

window.copyStringToClipboard = copyStringToClipboard
