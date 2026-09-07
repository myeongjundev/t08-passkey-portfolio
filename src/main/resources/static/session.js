const logoutButton = document.querySelector('#logout-button');
const csrfToken = document.querySelector('meta[name="csrf-token"]')?.content;

logoutButton?.addEventListener('click', async () => {
  logoutButton.disabled = true;
  try {
    const response = await fetch('/api/session/logout', {
      method: 'POST',
      credentials: 'same-origin',
      headers: {
        'Content-Type': 'application/json',
        'X-CSRF-Token': csrfToken
      },
      body: '{}'
    });
    if (!response.ok) throw new Error('logout_rejected');
    window.location.assign('/access');
  } catch (_) {
    logoutButton.disabled = false;
    logoutButton.textContent = '로그아웃 실패 · 다시 시도';
  }
});
