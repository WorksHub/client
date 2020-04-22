function highlightCodeSnippets() {
  // code highlighting doesn't work in dev mode because highlight.js is in conflict with hightlightjs from reframe10x
  const HIGHTLIGHTJS =
    "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.18.1/highlight.min.js";

  function alreadyLoaded(url) {
    return Boolean(document.querySelector('script[src="' + url + '"]'));
  }

  function loadScript(url, callback) {
    if (alreadyLoaded(url)) {
      callback();
      return;
    }
    const script = document.createElement("script");
    script.type = "text/javascript";
    script.onload = callback;
    script.src = url;
    document.getElementsByTagName("head")[0].appendChild(script);
  }

  function getCodeHighlightingScriptUrl(language) {
    return (
      "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.18.1/languages/" +
      language +
      ".min.js"
    );
  }

  function highlightLanguageSnippets(language) {
    const url = getCodeHighlightingScriptUrl(language);

    loadScript(url, () => {
      document.querySelectorAll("code." + language).forEach(block => {
        window.hljs && window.hljs.highlightBlock(block);
      });
    });
  }

  function getLanguagesUsedInSnippets() {
    const languages = Array.from(document.querySelectorAll("code"))
      .map(elm => elm.classList.value)
      .filter(elm => elm !== "")
      .map(elm => elm.split(" ")[0]);

    return Array.from(new Set(languages));
  }

  loadScript(HIGHTLIGHTJS, () => {
    getLanguagesUsedInSnippets().forEach(highlightLanguageSnippets);
  });
}
