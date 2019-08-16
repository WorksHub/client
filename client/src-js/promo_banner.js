function initPromoBanner(id) {
    attachOnScrollEvent(function(e) {
        const el = document.getElementById(id);
        if(el) {
            const maxOffset = el.offsetHeight;
            const amount = (e.scrollTop || e.scrollY || 0);
            el.style["margin-top"] = "-" + Math.min(amount, maxOffset) + "px";
            if(amount >= maxOffset) {
                el.classList.add("scrolled");
            } else {
                el.classList.remove("scrolled");
            }
        }
    });
};
