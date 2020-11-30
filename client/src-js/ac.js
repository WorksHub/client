const HISTORY_SIZE = 20
const HISTORY_LIST_SIZE = 4
const DB_ID = "search_history"

const cls = {
    searchMenu: 'search-dropdown',
    cancelBtn: 'search-dropdown__remove',
    result: 'search-dropdown__result'
}

function range(n) {
    let res = [];
    for (let i=0; i<n; i++) {
        res.push(i);
    }
    return res;
}

function getHistory() {
  try {
      return JSON.parse(localStorage.getItem(DB_ID)) || [];
  } catch (e) {
      return [];
  }
}

function deleteEntry(idx) {
    const history = getHistory();
    history.splice(idx, 1);
    localStorage.setItem(DB_ID, JSON.stringify(history));
}

function hideMenu(n) {
    const e = n.parentNode.querySelector('.'+cls.searchMenu);
    e && e.remove();
}

function onSearchKey(e, onKeyDown) {
    const input = e.target || e;
    let history = getHistory();
    if (e.key === 'Enter') {
        const value = input.value;
        history = history.filter(e => e !== value);
        history.unshift(value);
        if (history.length > HISTORY_SIZE) {
            history.pop();
        }
        localStorage.setItem(DB_ID, JSON.stringify(history));
        hideMenu(input);
    } else if(onKeyDown) {
        // input doesn't contain the new value yet so we give it
        // a brief time to update.
        setTimeout(_ => {
            onKeyDown(input);
        }, 50);
    }
}

function highlight(text, q) {
    const loc = text.indexOf(q);
    if (loc === -1) {
        const res = document.createElement('span');
        res.innerText = text;
        return [res];
    } else {
        const res = range(3).map(() => document.createElement('span'));
        res[0].innerText = text.slice(0, loc);
        res[1].innerText = text.slice(loc, loc+q.length);
        res[2].innerText = text.slice(loc+q.length);
        res[1].style['font-weight'] = 600;
        return res;
    }
}

function showMenu(n, results, q) {
    if (results.length === 0) {
        return;
    }
    const w = document.createElement('div');
    w.classList.add(cls.searchMenu);
    n.parentNode.appendChild(w);
    results.map(({text, idx}) => {
        const el = document.createElement('div');
        const te = document.createElement('div');
        highlight(text, q)
            .forEach(e => te.appendChild(e));
        el.appendChild(te);

        const cancel = document.createElement('div');
        cancel.classList.add(cls.cancelBtn);
        cancel.innerText = '×';
        cancel.addEventListener('mousedown', e => {
            e.stopPropagation();
            e.preventDefault();
            deleteEntry(idx);
            el.remove();
        });
        el.appendChild(cancel);
        el.classList.add(cls.result);

        el.addEventListener('mousedown', (e) => {
            n.value = text;
            n.closest('form').submit();
        });
        return el;
    }).forEach(el => w.appendChild(el));
}

function onSearchQueryEdit(e) {
    const input = e.target || e;
    const history = getHistory();
    const query = input.value.toLowerCase();
    const results = history.map((res, idx) => ({text: res, idx}))
          .filter(e => e.text.indexOf(query) !== -1)
          .slice(0, HISTORY_LIST_SIZE);
    hideMenu(input);
    showMenu(input, results, query);
}

let elr;

function onSearchFocus(e) {
    const input = e.target || e;
    const history = getHistory();
    const query = input.value.toLowerCase();
    const results = history.slice(0, HISTORY_LIST_SIZE)
          .map((res, idx) => ({text: res, idx}))
          .filter(e => e.text.indexOf(query) !== -1);
    showMenu(input, results, query);

    elr = () => {
        hideMenu(input);
        input.removeEventListener('blur', elr);
    };
    input.addEventListener('blur', elr);
}

window.onSearchKey = onSearchKey;
window.onSearchQueryEdit = onSearchQueryEdit;
window.onSearchFocus = onSearchFocus;
