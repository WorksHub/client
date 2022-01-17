function initSearchHints(historySize, historyListSize, dbId, searchSubmitId) {
    const cls = {
        searchMenu: 'search-dropdown',
        cancelBtn: 'search-dropdown__remove',
        result: 'search-dropdown__result',
    }

    function range(n) {
        const res = []
        for (let i = 0; i < n; i++) {
            res.push(i)
        }
        return res
    }

    function getHistory() {
        try {
            return JSON.parse(localStorage.getItem(dbId)) || []
        } catch (e) {
            return []
        }
    }

    function deleteEntry(idx) {
        const history = getHistory()
        history.splice(idx, 1)
        localStorage.setItem(dbId, JSON.stringify(history))
    }

    function hideMenu(n) {
        const e = n.parentNode.querySelector('.' + cls.searchMenu)
        e && e.remove()
    }

    function onSearchKey(e, onKeyDown) {
        const input = e.target || e
        const value = input.value

        if (value === '') {
            return
        }

        if (e.key === 'Enter') {
            const history = getHistory().filter(e => e !== value)
            history.unshift(value)
            if (history.length > historySize) {
                history.pop()
            }
            localStorage.setItem(dbId, JSON.stringify(history))
            hideMenu(input)
        } else if (onKeyDown) {
            // input doesn't contain the new value yet so we give it
            // a brief time to update.
            setTimeout(_ => {
                onKeyDown(input)
            }, 50)
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
            res[1].innerText = text.slice(loc, loc + q.length)
            res[2].innerText = text.slice(loc + q.length)
            res[1].style['font-weight'] = 600
            return res
        }
    }

    let prevN

    function updateFormAction(form, appendValue) {
        if (!!appendValue) {
            form.action += appendValue.replace(' ', '+')
        }
    }

    function showMenu(n, results, q) {
        const form = n.closest('form')
        if (n !== prevN) {
            prevN = n
            form.onsubmit = _ => {
                updateFormAction(form, n.value)
            }
        }
        if (results.length === 0) {
            return
        }

        const w = document.createElement('div')
        w.classList.add(cls.searchMenu)
        n.parentNode.appendChild(w)
        results
            .map(({ text, idx }) => {
                const el = document.createElement('div')
                const te = document.createElement('div')
                highlight(text, q).forEach(e => te.appendChild(e))
                el.appendChild(te)

                const cancel = document.createElement('div')
                cancel.classList.add(cls.cancelBtn)
                cancel.innerText = '×'
                cancel.addEventListener('mousedown', e => {
                    e.stopPropagation()
                    e.preventDefault()
                    deleteEntry(idx)
                    el.remove()
                })
                el.appendChild(cancel)
                el.classList.add(cls.result)

                el.addEventListener('mousedown', _ => {
                    n.value = text // NB: This doesn't trigger an 'on-change' event!

                    const submitBtn = document.getElementById(searchSubmitId)
                    if (submitBtn) {
                        // NB: Simple `n.closest('form').submit()` won't do, since it
                        //     passes by the 'on-submit' fn and directly calls <form>
                        //     action, while in CLJS version we would like to trigger
                        //     the re-frame subscriptions-based logic.
                        submitBtn.click()
                    } else {
                        // NB: On SSR'ed version (of some public page) we simply fall
                        //     back to an ordinary HTML <form> submit which redirects
                        //     user to a '/search/<text>' URI.
                        updateFormAction(form, n.value)
                        form.submit()
                    }
                })
                return el
            })
            .forEach(el => w.appendChild(el))
    }

    function onSearchQueryEdit(e) {
        const input = e.target || e
        const history = getHistory()
        const query = input.value.toLowerCase()
        const results = history
            .map((res, idx) => ({ text: res, idx }))
            .filter(e => e.text.indexOf(query) !== -1)
            .slice(0, historyListSize)
        hideMenu(input)
        showMenu(input, results, query)
    }

    let elr

    function onSearchFocus(e) {
        const input = e.target || e
        const history = getHistory()
        const query = input.value.toLowerCase()
        const results = history
            .slice(0, historyListSize)
            .map((res, idx) => ({ text: res, idx }))
            .filter(e => e.text.indexOf(query) !== -1)
        showMenu(input, results, query)

        elr = () => {
            hideMenu(input)
            input.removeEventListener('blur', elr)
        }
        input.addEventListener('blur', elr)
    }

    return {
        onSearchKey,
        onSearchQueryEdit,
        onSearchFocus,
    }
}

window.SearchHints = initSearchHints(20, 4, 'search_history', 'navbar__search-submit')
