const mobileMenuId = 'navigation';
const openMenuIconId = 'toggle-button-menu-icon';
const closeMenuIconId = 'toggle-button-close-icon';

const displayMenu = elmId => {
    const elm = document.getElementById(elmId);
    elm.style.display = 'flex';
    elm.scrollTop = 0;
};
const displayIcon = elmId =>
    (document.getElementById(elmId).style.display = 'inline-flex');
const hide = elmId => (document.getElementById(elmId).style.display = 'none');
const isMenuDisplayed = elmId =>
    Boolean(document.getElementById(elmId).style.display) &&
    document.getElementById(elmId).style.display !== 'none';

function toggleMenuDisplay() {
    if (isMenuDisplayed(mobileMenuId)) {
        hide(mobileMenuId);
        hide(closeMenuIconId);
        displayIcon(openMenuIconId);
    } else {
        displayMenu(mobileMenuId);
        hide(openMenuIconId);
        displayIcon(closeMenuIconId);
    }
}

window.toggleMenuDisplay = toggleMenuDisplay;
