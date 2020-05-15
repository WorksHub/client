/* global setClass */

var currentVideo = null;

function closeVideoPlayer() {
  document.getElementById('video-player-container').removeChild(currentVideo);
  currentVideo = null;
  setClass('video-player-container', 'is-open', false);
}
window.closeVideoPlayer = closeVideoPlayer;

function openVideoPlayer(youtubeId) {
  if (!currentVideo) {
    setClass('video-player-container', 'is-open', true);

    var videoWrapperOuter = document.createElement('div');
    videoWrapperOuter.classList.add('video-wrapper-outer');

    var videoWrapperInner = document.createElement('div');
    videoWrapperInner.classList.add('video-wrapper-inner');

    var iframe = document.createElement('iframe');
    iframe.classList.add('iframe-video--youtube');
    iframe.setAttribute(
      'src',
      'https://www.youtube.com/embed/' + youtubeId + '?autoplay=1'
    );
    iframe.setAttribute('frameborder', '0');
    iframe.setAttribute(
      'allow',
      'accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture'
    );
    iframe.setAttribute('allowfullscreen', '');

    videoWrapperInner.appendChild(iframe);
    videoWrapperOuter.appendChild(videoWrapperInner);
    document
      .getElementById('video-player-container')
      .appendChild(videoWrapperOuter);
    currentVideo = videoWrapperOuter;
  }
}
window.openVideoPlayer = openVideoPlayer;
