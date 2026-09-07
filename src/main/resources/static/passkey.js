const form = document.querySelector('#passkey-registration-form');
const statusBox = document.querySelector('#passkey-status');
const loginButton = document.querySelector('#passkey-login-button');
const csrfToken = document.querySelector('meta[name="csrf-token"]')?.content;

function fromBase64Url(value) {
  const padding = '='.repeat((4 - (value.length % 4)) % 4);
  const bytes = atob(value.replaceAll('-', '+').replaceAll('_', '/') + padding);
  return Uint8Array.from(bytes, (character) => character.charCodeAt(0));
}

function toBase64Url(buffer) {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  bytes.forEach((value) => { binary += String.fromCharCode(value); });
  return btoa(binary).replaceAll('+', '-').replaceAll('/', '_').replaceAll('=', '');
}

function serializeRegistration(credential) {
  return {
    id: credential.id,
    rawId: toBase64Url(credential.rawId),
    type: credential.type,
    authenticatorAttachment: credential.authenticatorAttachment,
    clientExtensionResults: credential.getClientExtensionResults(),
    response: {
      clientDataJSON: toBase64Url(credential.response.clientDataJSON),
      attestationObject: toBase64Url(credential.response.attestationObject),
      transports: credential.response.getTransports?.() ?? []
    }
  };
}

function serializeAuthentication(credential) {
  return {
    id: credential.id,
    rawId: toBase64Url(credential.rawId),
    type: credential.type,
    authenticatorAttachment: credential.authenticatorAttachment,
    clientExtensionResults: credential.getClientExtensionResults(),
    response: {
      clientDataJSON: toBase64Url(credential.response.clientDataJSON),
      authenticatorData: toBase64Url(credential.response.authenticatorData),
      signature: toBase64Url(credential.response.signature),
      userHandle: credential.response.userHandle ? toBase64Url(credential.response.userHandle) : null
    }
  };
}

function showStatus(step, message, isError = false) {
  statusBox.querySelector('span').textContent = step;
  statusBox.querySelector('strong').textContent = message;
  statusBox.classList.toggle('is-error', isError);
}

async function postJson(path, body) {
  const response = await fetch(path, {
    method: 'POST',
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
      'X-CSRF-Token': csrfToken
    },
    body: JSON.stringify(body)
  });
  if (!response.ok) throw new Error('server_rejected');
  return response.json();
}

form?.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (!window.PublicKeyCredential || !navigator.credentials) {
    showStatus('!', '이 브라우저에서는 패스키를 사용할 수 없습니다.', true);
    return;
  }

  const button = form.querySelector('button');
  const data = new FormData(form);
  button.disabled = true;
  showStatus('02', '이 기기에 패스키 생성을 요청하고 있습니다.');
  let ceremonyId;

  try {
    const optionsEnvelope = await postJson('/api/webauthn/register/options', {
      displayName: data.get('displayName'),
      nickname: data.get('nickname')
    });
    ceremonyId = optionsEnvelope.ceremonyId;
    const publicKey = optionsEnvelope.publicKey;
    publicKey.challenge = fromBase64Url(publicKey.challenge);
    publicKey.user.id = fromBase64Url(publicKey.user.id);
    publicKey.excludeCredentials = (publicKey.excludeCredentials ?? []).map((item) => ({
      ...item,
      id: fromBase64Url(item.id)
    }));

    const credential = await navigator.credentials.create({ publicKey });
    if (!credential) throw new Error('credential_missing');
    showStatus('03', '서버가 패스키 공개키를 확인하고 있습니다.');
    const completed = await postJson('/api/webauthn/register/finish', {
      ceremonyId: optionsEnvelope.ceremonyId,
      credential: serializeRegistration(credential)
    });
    window.location.assign(completed.redirectTo);
  } catch (error) {
    if (error?.name === 'NotAllowedError') {
      if (ceremonyId) {
        try {
          await postJson('/api/webauthn/register/cancel', { ceremonyId });
        } catch (_) {
          // The server-held challenge still expires automatically after five minutes.
        }
      }
      showStatus('—', '패스키 만들기를 취소했습니다. 계정과 공개키는 저장되지 않았습니다.', true);
    } else {
      showStatus('!', '패스키를 확인하지 못했습니다. 새로고침한 뒤 다시 시도해 주세요.', true);
    }
    button.disabled = false;
  }
});

loginButton?.addEventListener('click', async () => {
  if (!window.PublicKeyCredential || !navigator.credentials) {
    showStatus('!', '이 브라우저에서는 패스키를 사용할 수 없습니다.', true);
    return;
  }
  loginButton.disabled = true;
  showStatus('02', '등록된 패스키를 찾고 있습니다.');
  try {
    const optionsEnvelope = await postJson('/api/webauthn/authenticate/options', {});
    const publicKey = optionsEnvelope.publicKey;
    publicKey.challenge = fromBase64Url(publicKey.challenge);
    publicKey.allowCredentials = (publicKey.allowCredentials ?? []).map((item) => ({
      ...item,
      id: fromBase64Url(item.id)
    }));
    const credential = await navigator.credentials.get({ publicKey });
    if (!credential) throw new Error('credential_missing');
    showStatus('03', '저장된 공개키로 서명을 확인하고 있습니다.');
    const completed = await postJson('/api/webauthn/authenticate/finish', {
      ceremonyId: optionsEnvelope.ceremonyId,
      credential: serializeAuthentication(credential)
    });
    window.location.assign(completed.redirectTo);
  } catch (error) {
    if (error?.name === 'NotAllowedError') {
      showStatus('—', '패스키 로그인을 취소했습니다.', true);
    } else {
      showStatus('!', '패스키 서명을 확인하지 못했습니다. 다시 시도해 주세요.', true);
    }
    loginButton.disabled = false;
  }
});
