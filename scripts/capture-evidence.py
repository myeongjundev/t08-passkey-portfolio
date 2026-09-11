from pathlib import Path
from playwright.sync_api import sync_playwright

OUT = Path(r"C:\Users\user\AppData\Local\Temp\claude\C--gov-SKT-ALEPH\cf4dd2c2-cb26-4d3f-b130-1daf6ea3bd70\scratchpad\shots")
PROD = "https://t08-passkey-portfolio.vercel.app"
LOCAL = "http://localhost:8080"
VP = {"width": 1280, "height": 860}


def shot(page, name, full=False):
    page.screenshot(path=str(OUT / f"{name}.png"), full_page=full,
                    animations="disabled", caret="hide", timeout=20000)
    print("saved", name)


def add_authenticator(client, transport="internal", backup=True):
    return client.send("WebAuthn.addVirtualAuthenticator", {"options": {
        "protocol": "ctap2", "transport": transport,
        "hasResidentKey": True, "hasUserVerification": True,
        "isUserVerified": True, "automaticPresenceSimulation": True,
        "defaultBackupEligibility": backup, "defaultBackupState": backup,
    }})["authenticatorId"]


with sync_playwright() as p:
    browser = p.chromium.launch()

    ctx = browser.new_context(viewport=VP, locale="ko-KR")
    page = ctx.new_page()
    page.goto(PROD + "/nope", wait_until="domcontentloaded", timeout=60000)
    page.wait_for_timeout(1500)
    shot(page, "06-error-page")
    ctx.close()

    ctx = browser.new_context(viewport=VP, locale="ko-KR")
    page = ctx.new_page()
    client = ctx.new_cdp_session(page)
    client.send("WebAuthn.enable", {"enableUI": False})
    phone = add_authenticator(client, "internal", backup=True)

    page.goto(LOCAL + "/access", wait_until="networkidle")
    page.fill("input[name=displayName]", "합성 사용자")
    page.fill("input[name=nickname]", "내 휴대폰")
    page.click("#passkey-registration-form button[type=submit]")
    page.wait_for_url("**/private", timeout=30000)
    page.wait_for_timeout(1500)
    shot(page, "07-private-after-registration", full=True)

    key = add_authenticator(client, "usb", backup=False)
    page.fill("#add-passkey-form input[name=nickname]", "예비 보안 키")
    page.click("#add-passkey-form button[type=submit]")
    page.wait_for_timeout(4000)
    shot(page, "08-two-passkeys")

    page.locator(".passkey-row", has_text="내 휴대폰").locator(".passkey-delete").click()
    page.wait_for_timeout(4000)
    shot(page, "09-after-deleting-first")

    page.locator(".passkey-delete").first.click()
    page.wait_for_timeout(3000)
    shot(page, "10-last-passkey-refused")

    client.send("WebAuthn.removeVirtualAuthenticator", {"authenticatorId": phone})
    page.evaluate("fetch('/api/session/logout',{method:'POST',headers:{'X-CSRF-Token':document.querySelector('meta[name=csrf-token]').content}})")
    page.wait_for_timeout(1200)
    page.goto(LOCAL + "/access", wait_until="networkidle")
    page.click("#passkey-login-button")
    page.wait_for_url("**/private", timeout=30000)
    page.wait_for_timeout(1500)
    shot(page, "11-login-with-remaining-passkey")

    ctx.close()
    browser.close()
print("done")
