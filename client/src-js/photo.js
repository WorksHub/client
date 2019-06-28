function openPhotoGallery(index, images) {
    var pswpElement = document.querySelectorAll('.pswp')[0];
    var options = {index: index, bgOpacity: 0.9, history: false};
    var items = [];
    for(var i = 0; i < images.length; i++) {
        items.push({src: images[i].url,
                    w: images[i].width,
                    h: images[i].height});
    }
    var gallery = new PhotoSwipe( pswpElement, PhotoSwipeUI_Default, items, options);
    gallery.init();
}
