const addForm = document.querySelector('#add-passkey-form');
const managerStatus = document.querySelector('#passkey-manager-status');
const managerCsrf = document.querySelector('meta[name="csrf-token"]')?.content;

function decodeBase64Url(value) {
  const padding = '='.repeat((4 - (value.length % 4)) % 4);
  const decoded = atob(value.replaceAll('-', '+').replaceAll('_', '/') + padding);
  return Uint8Array.from(decoded, (character) => character.charCodeAt(0));
}

function encodeBase64Url(buffer) {
  let binary = '';
  new Uint8Array(buffer).forEach((value) => { binary += String.fromCharCode(value); });
  return btoa(binary).replaceAll('+', '-').replaceAll('/', '_').replaceAll('=', '');
}

function registrationJson(credential) {
  return {
    id: credential.id,
    rawId: encodeBase64Url(credential.rawId),
    type: credential.type,
    authenticatorAttachment: credential.authenticatorAttachment,
    clientExtensionResults: credential.getClientExtensionResults(),
    response: {
      clientDataJSON: encodeBase64Url(credential.response.clientDataJSON),
      attestationObject: encodeBase64Url(credential.response.attestationObject),
      transports: credential.response.getTransports?.() ?? []
    }
  };
}

async function managementRequest(path, method, body) {
  const response = await fetch(path, {
    method,
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': managerCsrf },
    body: body ? JSON.stringify(body) : '{}'
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.error ?? 'request_rejected');
  }
  return response.status === 204 ? null : response.json();
}

addForm?.addEventListener('submit', async (event) => {
  event.preventDefault();
  const button = addForm.querySelector('button');
  button.disabled = true;
  managerStatus.textContent = '새 패스키를 등록하고 있습니다.';
  try {
    const nickname = new FormData(addForm).get('nickname');
    const envelope = await managementRequest('/api/passkeys/options', 'POST', { nickname });
    const publicKey = envelope.publicKey;
    publicKey.challenge = decodeBase64Url(publicKey.challenge);
    publicKey.user.id = decodeBase64Url(publicKey.user.id);
    publicKey.excludeCredentials = publicKey.excludeCredentials.map((item) => ({
      ...item, id: decodeBase64Url(item.id)
    }));
    const credential = await navigator.credentials.create({ publicKey });
    if (!credential) throw new Error('credential_missing');
    await managementRequest('/api/passkeys/finish', 'POST', {
      ceremonyId: envelope.ceremonyId,
      credential: registrationJson(credential)
    });
    window.location.reload();
  } catch (error) {
    if (error?.name === 'NotAllowedError') {
      managerStatus.textContent = '패스키 추가를 취소했습니다.';
    } else if (error?.message === 'nickname_taken') {
      managerStatus.textContent = '이미 같은 이름의 패스키가 있습니다. 다른 이름을 입력해 주세요.';
    } else {
      managerStatus.textContent = '패스키를 추가하지 못했습니다. 다시 시도해 주세요.';
    }
    button.disabled = false;
  }
});

document.querySelectorAll('.passkey-delete').forEach((button) => {
  button.addEventListener('click', async () => {
    button.disabled = true;
    try {
      await managementRequest(`/api/passkeys/${button.dataset.passkeyId}`, 'DELETE');
      window.location.reload();
    } catch (error) {
      managerStatus.textContent = error.message === 'final_passkey_required'
        ? '마지막 패스키는 삭제할 수 없습니다. 먼저 다른 패스키를 등록하세요.'
        : '이 패스키를 삭제하지 못했습니다.';
      button.disabled = false;
    }
  });
});
