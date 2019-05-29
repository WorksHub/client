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

/* Toggles global "no scroll" mode with reference element by ID */
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

/*--------------------------------------------------------------------------*/

function enableCarousel($carousel) {
    function switchItem(i) {
        const $prevPip = $carousel.getElementsByClassName('carousel-pip--active')[0],
              $nextPip = $carousel.getElementsByClassName('carousel-pip')[i],
              $prevItem = $carousel.getElementsByClassName('carousel-item--active')[0],
              $nextItem = $carousel.getElementsByClassName('carousel-item')[i];
        if ($prevPip !== $nextPip) {
            $prevPip.classList.remove('carousel-pip--active');
            $nextPip.classList.add('carousel-pip--active');
            $prevItem.classList.remove('carousel-item--active');
            $nextItem.classList.add('carousel-item--active');
        }
    }

    function currentItem() {
        return Array.prototype.indexOf.call($carousel.getElementsByClassName('carousel-pip'),
                                            $carousel.getElementsByClassName('carousel-pip--active')[0]);
    }

    var total = $carousel.getElementsByClassName('carousel-pip').length,
        rotate = setInterval(() => switchItem((currentItem() + 1) % total), 5000);

    function pipClicked(i) {
        switchItem(i);
        clearInterval(rotate);
    }

    $carousel.querySelectorAll('.carousel-pip').forEach((node, i) => {
        node.addEventListener('click', event => pipClicked(i));
    });
}
