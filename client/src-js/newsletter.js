const listenNewsletterForm = () => {
  const selectors = {
    formNewsletter: '#newsletter-subscription-form',
    messageSuccess: '#newsletter-subscription-success'
  };

  const form = document.querySelector(selectors.formNewsletter);

  if (form) {
    const toggleVisibility = elm => elm.classList.toggle('is-hidden');
    const CREATE_PROSPECT_USER = window.location.origin + '/api/prospect';

    form.addEventListener('submit', e => {
      e.preventDefault();

      fetch(CREATE_PROSPECT_USER, {
        method: 'POST',
        body: new FormData(form)
      }).catch(console.error);

      const elementToDisplay = document.querySelector(selectors.messageSuccess);

      toggleVisibility(elementToDisplay);
    });
  }
};

window.listenNewsletterForm = listenNewsletterForm;
