/* Toggles class on element of given ID */
function toggleClass(id, cls){
    var d = document.getElementById(id);
    if(d) {
        d.classList.toggle(cls);
    } else {
        console.warn("Tried to toggle class on '" + id + "' but the element couldn't be found.")
    }
}

/* Sets class on or off element of given ID */
function setClass(id, cls, on){
    var d = document.getElementById(id);
    if(d) {
        if(on) {
            d.classList.add(cls);
        }
        else {
            d.classList.remove(cls);
        }
    } else {
        console.warn("Tried to set class on '" + id + "' but the element couldn't be found.")
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

function loadJavaScript(filename) {
    var r = new XMLHttpRequest();
    r.open("GET", "${prefix}/" + filename);
    r.onreadystatechange = function() {
        if (r.readyState == 4 && r.status == 200) {
            var container = document.createElement("script");
            container.innerHTML = r.responseText;
            document.body.append(container);
        }
    };
    r.send();
};

/*--------------------------------------------------------------------------*/

function enableCarousel($carousel) {

    function getArrow(dir, f) {
        const $arrow = $carousel.querySelector('.carousel-arrow--' + dir);
        if($arrow != null){
            f($arrow);
        }
    }

    function highlightArrows(currentIdx) {
        if((currentIdx + 1) >= total) {
            getArrow('left',  a => { a.classList.remove("carousel-arrow--disabled");});
            getArrow('right', a => { a.classList.add("carousel-arrow--disabled");});
        } else if (currentIdx <= 0) {
            getArrow('left',  a => { a.classList.add("carousel-arrow--disabled");});
            getArrow('right', a => { a.classList.remove("carousel-arrow--disabled");});
        } else {
            getArrow('left',  a => { a.classList.remove("carousel-arrow--disabled");});
            getArrow('right', a => { a.classList.remove("carousel-arrow--disabled");});
        }
    }

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
        highlightArrows(i);
    }

    function currentItem() {
        return Array.prototype.indexOf.call($carousel.getElementsByClassName('carousel-pip'),
                                            $carousel.getElementsByClassName('carousel-pip--active')[0]);
    }

    var total = $carousel.getElementsByClassName('carousel-pip').length,
        rotate = setInterval(() => switchItem((currentItem() + 1) % total), 5000);



    function slideItems(d) {
        const $newIdx = currentItem() + d;
        if($newIdx < total && $newIdx >= 0) {
            switchItem($newIdx);
        }
    }

    function pipClicked(i) {
        switchItem(i);
        clearInterval(rotate);
    }

    function slideLeftClicked() {
        slideItems(-1);
        clearInterval(rotate);
    }

    function slideRightClicked() {
        slideItems(1);
        clearInterval(rotate);
    }

    $carousel.querySelectorAll('.carousel-pip').forEach((node, i) => {
        node.addEventListener('click', event => pipClicked(i));
    });
    getArrow('left',  a => { a.addEventListener('click', event => slideLeftClicked());});
    getArrow('right', a => { a.addEventListener('click', event => slideRightClicked());});
}

/*--------------------------------------------------------------------------*/

function setClassAtScrollPosition(e, id, cls, scrollPosition){
    setClass(id, cls, (e.scrollTop > scrollPosition || e.scrollY > scrollPosition));
}

/* This function is used to attach another function f (which takes exactly one argument, i.e. the thing is attached to)
* which is called whenever there is a scroll. This mirrors wh.pages.util/attach-on-scroll-event and is meant to be used to
* replicate its behavior in SSR pages which do not have app-js. Note that it is not necessary to remove the ScrollListener
* because whatever action you do from a SSR page without app-js you do a full page navigation, so there are no handlers attached anymore. */
function attachOnScrollEvent(f) {
    var el = document.getElementById("app");
    el.addEventListener('scroll', function(){
        f(el);
    }); // desktop
    window.addEventListener('scroll', function(){ f(window)}); // mobile
}
