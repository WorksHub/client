import { toggleClass, setNoScroll } from './public'

let filtersAreOpened = false

export function toggleJobsFilters() {
    toggleClass('search-box', 'search-box--hidden')
    setNoScroll('app', !filtersAreOpened)
    filtersAreOpened = !filtersAreOpened
}

window.toggleJobsFilters = toggleJobsFilters
