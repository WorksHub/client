const sectionId = {
    blogs: 'trending-content-blogs',
    issues: 'trending-content-issues',
    jobs: 'trending-content-jobs',
}

const displaySection = elmId => (document.getElementById(elmId).style = 'display: grid')
const hideSection = elmId => (document.getElementById(elmId).style = '')
const isSectionDisplayed = elmId => Boolean(document.getElementById(elmId).style.display)
const areAllSectionsHidden = () =>
    ![sectionId.blogs, sectionId.issues, sectionId.jobs].some(isSectionDisplayed)
const getDisplayedSection = () =>
    [sectionId.blogs, sectionId.issues, sectionId.jobs].filter(isSectionDisplayed)[0]

function toggleDisplay(type) {
    const sectId = {
        blogs: sectionId.blogs,
        issues: sectionId.issues,
        jobs: sectionId.jobs,
    }[type]

    if (areAllSectionsHidden()) {
        displaySection(sectId)
    } else if (!areAllSectionsHidden() && !isSectionDisplayed(sectId)) {
        hideSection(getDisplayedSection())
        displaySection(sectId)
    } else if (!areAllSectionsHidden() && isSectionDisplayed(sectId)) {
        hideSection(sectId)
    }
}

window.toggleDisplay = toggleDisplay
