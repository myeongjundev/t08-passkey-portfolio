document.documentElement.classList.add('js');

const revealTargets = document.querySelectorAll('.reveal');

if ('IntersectionObserver' in window && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      entry.target.classList.add('is-visible');
      observer.unobserve(entry.target);
    });
  }, { threshold: 0.08, rootMargin: '0px 0px -6% 0px' });

  revealTargets.forEach((target) => observer.observe(target));
} else {
  revealTargets.forEach((target) => target.classList.add('is-visible'));
}

const scopeDialog = document.querySelector('#scope-dialog');
const openScopeButton = document.querySelector('[data-open-scope]');
const closeScopeButton = document.querySelector('[data-close-scope]');

if (scopeDialog && openScopeButton && closeScopeButton) {
  openScopeButton.addEventListener('click', () => scopeDialog.showModal());
  openScopeButton.addEventListener('keydown', (event) => {
    if (event.key !== 'Enter' && event.key !== ' ') return;
    event.preventDefault();
    if (!scopeDialog.open) scopeDialog.showModal();
  });
  closeScopeButton.addEventListener('click', () => scopeDialog.close());

  scopeDialog.addEventListener('click', (event) => {
    if (event.target === scopeDialog) scopeDialog.close();
  });
}
