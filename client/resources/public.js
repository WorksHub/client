/* Toggles class on element of given ID */
function toggleClass(id, cls){
    var d = document.getElementById(id);
    d.classList.toggle(cls);
}
/* Sets class on or off element of given ID */
function setClass(id, cls, on){
    var d = document.getElementById(id);
    if(on) {
        d.classList.add(cls);
    }
    else {
        d.classList.remove(cls);
    }
}

/*--------------------------------------------------------------------------*/

/* Toggles global "no scroll" mode with reference element by ID*/
function toggleNoScroll(id){
    var el = document.getElementById(id);
    if(!document.body.classList.contains("no-scroll")) {
        document.body.classList.add("no-scroll");
        bodyScrollLock.disableBodyScroll(el);
    } else {
        document.body.classList.remove("no-scroll");
        bodyScrollLock.enableBodyScroll(el);
    }
}
/* Sets global "no scroll" mode on or off with reference element by ID*/
function setNoScroll(id, on){
    var el = document.getElementById(id);
    if(on) {
        bodyScrollLock.disableBodyScroll(el);
    } else {
        bodyScrollLock.enableBodyScroll(el);
    }
}
/* Turns off global "no scroll" mode*/
function disableNoScroll(){
    document.body.classList.remove("no-scroll");
    bodyScrollLock.clearAllBodyScrollLocks();
}

/*--------------------------------------------------------------------------*/

/* Loads a symbols file and adds it to DOM */
function loadSymbols(filename) {
    var r = new XMLHttpRequest();
    r.open("GET", "${prefix}/" + filename);
    r.onreadystatechange = function() {
        if (r.readyState == 4 && r.status == 200) {
            var container = document.createElement("div");
            container.classList.add("svg-container")
            container.innerHTML = r.responseText;
            document.body.append(container);
        }
    };
    r.send();
};
