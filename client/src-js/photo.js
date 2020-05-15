/* global PhotoSwipeUI_Default, PhotoSwipe */

import { setClass } from './public';

function openPhotoGallery(index, images) {
  var pswpElement = document.querySelectorAll('.pswp')[0];
  var options = { index: index, bgOpacity: 0.9, history: false };
  var items = [];
  for (var i = 0; i < images.length; i++) {
    items.push({ src: images[i].url, w: images[i].width, h: images[i].height });
  }

  // mobile safari bug
  // we need to force z-index: 0 on the nav
  setClass('wh-navbar', 'navbar--reset-z-index', true);

  var gallery = new PhotoSwipe(
    pswpElement,
    PhotoSwipeUI_Default,
    items,
    options
  );
  gallery.listen('close', function() {
    setClass('wh-navbar', 'navbar--reset-z-index', false);
  });

  gallery.init();
}

window.openPhotoGallery = openPhotoGallery;
