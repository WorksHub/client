const HISTORY_SIZE = 20
const HISTORY_LIST_SIZE = 4
const DB_ID = "search_history"
const WRAPPER_CLASS = "search_class"
const HOVER_COLOR = '#f4ada4'
const HOVER_COLOR_CANCEL = '#dddddd'
const BG_COLOR = '#fefefe'
const PRIMARY = '#ef8374'

function range(n) {
  let res = []
  for (let i=0; i<n; i++) {
    res.push(i)
  }
  return res
}

function getHistory() {
  try {
    return JSON.parse(localStorage.getItem(DB_ID))
  } catch (e) {
    return []
  }
}

function deleteEntry(idx) {
  const history = getHistory()
  history.splice(idx, 1)
  localStorage.setItem(DB_ID, JSON.stringify(history))
}

function hideMenu(n) {
  const e = n.parentNode.querySelector('.'+WRAPPER_CLASS)
  e && e.remove()
}

function onSearchKey(e) {
  const ip = e.target
  let history = getHistory()
  console.log(e.key)
  if (e.key === 'Enter') {
    const value = ip.value
    history = history.filter(e => e !== value)
    history.unshift(value)
    if (history.length > HISTORY_SIZE) {
      history.pop()
    }
    localStorage.setItem(DB_ID, JSON.stringify(history))
    hideMenu(ip)
  }
}

function highlight(text, q) {
  const loc = text.indexOf(q)
  if (loc === -1) {
    const res = document.createElement('span')
    res.innerText = text
    return [res]
  } else {
    const res = range(3).map(() => document.createElement('span'))
    res[0].innerText = text.slice(0, loc)
    res[1].innerText = text.slice(loc, loc+q.length)
    res[2].innerText = text.slice(loc+q.length)
    res[1].style['font-weight'] = 1000
    return res
  }
}

function showMenu(n, results, q) {
  if (results.length === 0) {
    return
  }
  const w = document.createElement('div')
  w.classList.add(WRAPPER_CLASS)
  w.style.position = 'absolute'
  w.style.top = '100%'
  w.style.left = '0'
  w.style.width = '100%'
  w.style['font-size'] = '1.3em'
  w.style.border = '2px solid ' + PRIMARY
  w.style['border-radius'] = '4px'
  n.parentNode.appendChild(w)
  results.map(({text, idx}) => {
    const el = document.createElement('div')
    const te = document.createElement('div')
    highlight(text, q)
      .forEach(e => te.appendChild(e))
    el.appendChild(te)

    const cancel = document.createElement('div')
    cancel.style['font-family'] = 'monospace'
    cancel.innerText = 'x'
    cancel.style.width = '20px'
    cancel.style.height = '20px'
    cancel.style['line-height'] = '20px'
    cancel.style['text-align'] = 'center'
    cancel.style['border-radius'] = '100px'
    cancel.addEventListener('mouseenter', () => cancel.style.background=HOVER_COLOR_CANCEL)
    cancel.addEventListener('mouseleave', () => cancel.style.background='none')
    cancel.addEventListener('mousedown', e => {
      e.stopPropagation()
      e.preventDefault()
      deleteEntry(idx)
      el.remove()
    })
    el.appendChild(cancel)

    el.style.width = '100%'
    el.style.height = '40px'
    el.style['line-height'] = '40px'
    el.style['box-shadow'] = '0 0 0.2em #bdbbbb'
    el.style.padding = '0 0.5em 0 1em'
    el.style.display = 'flex'
    el.style.cursor = 'pointer'
    el.style['justify-content'] = 'space-between'
    el.style['align-items'] = 'center'
    el.style.background = BG_COLOR
    el.addEventListener('mousedown', (e) => {
      n.value = text
      n.closest('form').submit()
    })
    el.addEventListener('mouseenter', () => el.style.background=HOVER_COLOR)
    el.addEventListener('mouseleave', () => el.style.background=BG_COLOR)
    return el
  }).forEach(el => w.appendChild(el))
}

function onSearchQueryEdit(e) {
  const ip = e.target
  const history = getHistory()
  const query = ip.value.toLowerCase()
  const results = history.map((res, idx) => ({text: res, idx}))
    .filter(e => e.text.indexOf(query) !== -1)
    .slice(0, HISTORY_LIST_SIZE)
  hideMenu(ip)
  showMenu(ip, results, query)
}

let elr

function onSearchFocus(e) {
  const ip = e.target
  const history = getHistory()
  const query = ip.value.toLowerCase()
  const results = history.slice(0, HISTORY_LIST_SIZE)
    .map((res, idx) => ({text: res, idx}))
    .filter(e => e.text.indexOf(query) !== -1)
  showMenu(ip, results, query)

  elr = () => {
    hideMenu(ip);
    ip.removeEventListener('blur', elr)
  }
  ip.addEventListener('blur', elr)
}

window.onSearchKey = onSearchKey
window.onSearchQueryEdit = onSearchQueryEdit
window.onSearchFocus = onSearchFocus
