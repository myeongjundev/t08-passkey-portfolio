import pptxgen from "pptxgenjs";
import path from "node:path";
import fs from "node:fs";

const SHOTS = "C:\\Users\\user\\AppData\\Local\\Temp\\claude\\C--gov-SKT-ALEPH\\cf4dd2c2-cb26-4d3f-b130-1daf6ea3bd70\\scratchpad\\shots";
const OUTDIR = "C:\\gov\\SKT_ALEPH\\t08-passkey-portfolio\\output\\presentation";
fs.mkdirSync(OUTDIR, { recursive: true });
const OUT = path.join(OUTDIR, "T08-evidence.pptx");

// 결과물이 쓰는 색을 그대로 가져온다.
const C = {
  forest: "1F3C2E", lime: "C8F04B", paper: "F3F0E9", paper2: "E9E4DA",
  ink: "171914", dark: "14170F", dark2: "1E2219", muted: "66695F",
  orange: "EE6B45", pale: "D7DDD5", line: "3A443B",
};
const F = "Malgun Gothic";
const MONO = "Consolas";

const pres = new pptxgen();
pres.layout = "LAYOUT_WIDE";
pres.author = "myeongjundev";
pres.title = "T08 패스키 증거";
const W = 13.333, H = 7.5;

const shot = (n) => path.join(SHOTS, n);
const has = (n) => fs.existsSync(shot(n));

function chip(slide, x, y, label, fill, color) {
  slide.addShape(pres.ShapeType.rect, { x, y, w: 1.85, h: 0.3, fill: { color: fill } });
  slide.addText(label, {
    x, y, w: 1.85, h: 0.3, isTextBox: true, margin: 0, align: "center", valign: "middle",
    fontFace: MONO, fontSize: 10, bold: true, color, charSpacing: 1.2,
  });
}

function head(slide, title, sub, dark) {
  slide.addText(title, {
    x: 0.75, y: 0.85, w: W - 1.5, h: 0.9, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 34, bold: true, color: dark ? C.paper : C.ink,
  });
  if (sub) slide.addText(sub, {
    x: 0.78, y: 1.75, w: W - 1.6, h: 0.45, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 13.5, color: dark ? C.pale : C.muted,
  });
}

function page(dark, tag) {
  const s = pres.addSlide();
  s.background = { color: dark ? C.dark : C.paper };
  chip(s, 0.75, 0.42, tag, dark ? C.lime : C.forest, dark ? C.forest : C.lime);
  return s;
}

function foot(slide, text, dark) {
  slide.addText(text, {
    x: 0.75, y: H - 0.62, w: W - 1.5, h: 0.3, isTextBox: true, margin: 0,
    fontFace: MONO, fontSize: 9.5, color: dark ? C.line : C.muted,
  });
}

function card(slide, x, y, w, h, dark) {
  slide.addShape(pres.ShapeType.roundRect, {
    x, y, w, h, rectRadius: 0.04,
    fill: { color: dark ? C.dark2 : "FFFFFF" },
    line: { color: dark ? C.line : C.paper2, width: 1 },
  });
}

/* 1 — 표지 */
{
  const s = pres.addSlide();
  s.background = { color: C.forest };
  chip(s, 0.9, 1.5, "PASSKEY ONLY", C.lime, C.forest);
  s.addText("비밀번호를 없애고\n패스키로 잠갔습니다", {
    x: 0.9, y: 2.0, w: 8.4, h: 2.1, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 44, bold: true, color: C.paper, lineSpacing: 52,
  });
  s.addText("T08 · 내 소개 페이지에 패스키 달기 — 증거 모음", {
    x: 0.92, y: 4.2, w: 8.4, h: 0.4, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 15, color: C.lime,
  });
  s.addText("판정 47개 전부 충족 · 테스트 50개 통과 · 비밀번호 입력 칸 0개", {
    x: 0.92, y: 4.75, w: 8.4, h: 0.4, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 12.5, color: C.pale,
  });
  s.addText("t08-passkey-portfolio.vercel.app\ngithub.com/myeongjundev/t08-passkey-portfolio", {
    x: 0.92, y: 5.6, w: 8.4, h: 0.8, isTextBox: true, margin: 0,
    fontFace: MONO, fontSize: 11, color: C.pale, lineSpacing: 18,
  });
  s.addText("2026-09-11 · myeongjundev", {
    x: W - 4.5, y: H - 0.95, w: 3.6, h: 0.3, isTextBox: true, margin: 0, align: "right",
    fontFace: MONO, fontSize: 10, color: C.lime,
  });
  s.addNotes("T08은 T01 소개 페이지에 패스키 잠금을 붙인 과제다. 비밀번호는 어디에도 없다.");
}

/* 2 — 한 장 요약 */
{
  const s = page(false, "SUMMARY");
  head(s, "한 장으로 보는 결과", "공개 소개는 누구나, 비공개 자리는 패스키를 가진 기기만.", false);
  const stats = [
    ["47 / 47", "판정 전부 충족", C.forest],
    ["50", "자동 테스트 통과", C.forest],
    ["0", "비밀번호 입력 칸", C.orange],
  ];
  stats.forEach(([big, label, col], i) => {
    const x = 0.75 + i * 4.0;
    card(s, x, 2.4, 3.6, 1.7, false);
    s.addText(big, {
      x: x + 0.3, y: 2.6, w: 3.0, h: 0.85, isTextBox: true, margin: 0,
      fontFace: F, fontSize: 38, bold: true, color: col,
    });
    s.addText(label, {
      x: x + 0.32, y: 3.48, w: 3.0, h: 0.35, isTextBox: true, margin: 0,
      fontFace: F, fontSize: 12.5, color: C.muted,
    });
  });
  const rows = [
    ["개인키", "기기를 떠나지 않습니다. 서버는 공개키·credential ID·sign counter만 저장합니다."],
    ["질문(challenge)", "서버가 만들고 서버가 들고 있다가, 한 번 쓰면 버립니다."],
    ["잠금 해제", "기기의 지문 인증입니다. 지문 데이터는 서버로 오지 않습니다."],
    ["기기 분실 대비", "패스키를 두 개 등록할 수 있고, 마지막 하나는 지워지지 않습니다."],
  ];
  rows.forEach(([k, v], i) => {
    const y = 4.5 + i * 0.62;
    s.addText(k, {
      x: 0.78, y, w: 2.4, h: 0.4, isTextBox: true, margin: 0,
      fontFace: F, fontSize: 12.5, bold: true, color: C.forest,
    });
    s.addText(v, {
      x: 3.3, y, w: 9.3, h: 0.4, isTextBox: true, margin: 0,
      fontFace: F, fontSize: 12, color: C.ink,
    });
  });
  s.addNotes("과제가 요구하는 네 가지 성질을 먼저 요약한다.");
}

/* 3 — 공개 첫 화면 */
{
  const s = page(false, "C10 C11 C13");
  head(s, "첫 화면은 공개 소개입니다", "심사하는 사람이 아무것도 등록하지 않아도 열립니다. T01에서 만든 소개가 그대로 남아 있습니다.", false);
  if (has("01-public-first-screen.png"))
    s.addImage({ path: shot("01-public-first-screen.png"), x: 0.75, y: 2.35, w: 7.4, h: 4.05 });
  card(s, 8.55, 2.35, 4.05, 4.05, false);
  s.addText("첫 화면에서 바로 보이는 것", {
    x: 8.85, y: 2.58, w: 3.5, h: 0.35, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 13, bold: true, color: C.forest,
  });
  s.addText([
    { text: "공개 소개 — 이름·기술·프로젝트", options: { bullet: true, breakLine: true } },
    { text: "PASSKEY ONLY 띠 — 잠긴 자리가 있다는 사실", options: { bullet: true, breakLine: true } },
    { text: "등록·로그인 없이 열림 (HTTP 200)", options: { bullet: true, breakLine: true } },
    { text: "비공개 내용은 본문에 없음", options: { bullet: true } },
  ], {
    x: 8.85, y: 3.05, w: 3.5, h: 2.2, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 11.5, color: C.ink, paraSpaceAfter: 8, lineSpacing: 16,
  });
  s.addText("패스키가 이 과제의 본체라, 입구를 첫 화면으로 올렸습니다.", {
    x: 8.85, y: 5.45, w: 3.5, h: 0.75, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 11, italic: true, color: C.muted, lineSpacing: 15,
  });
  foot(s, "https://t08-passkey-portfolio.vercel.app/  ·  2026-09-11 캡처", false);
}

/* 4 — 경계 */
{
  const s = page(true, "C13");
  head(s, "공개는 여기까지", "공개 내용을 다 보여 준 뒤에 경계를 긋습니다. 그다음은 패스키입니다.", true);
  if (has("02-public-private-boundary.png"))
    s.addImage({ path: shot("02-public-private-boundary.png"), x: 0.75, y: 2.35, w: 7.4, h: 4.05 });
  card(s, 8.55, 2.35, 4.05, 4.05, true);
  s.addText("경계를 두 번 알립니다", {
    x: 8.85, y: 2.58, w: 3.5, h: 0.35, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 13, bold: true, color: C.lime,
  });
  s.addText([
    { text: "첫 화면의 PASSKEY ONLY 띠", options: { bullet: true, breakLine: true } },
    { text: "공개 내용이 끝나는 자리의 05 섹션", options: { bullet: true } },
  ], {
    x: 8.85, y: 3.05, w: 3.5, h: 1.1, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 11.5, color: C.pale, paraSpaceAfter: 8, lineSpacing: 16,
  });
  s.addText("“공개 소개는 여기까지”는 공개 내용을 다 보여 준 뒤에야 뜻이 섭니다. 그래서 05 섹션을 옮기지 않고 입구를 하나 더 만들었습니다.", {
    x: 8.85, y: 4.35, w: 3.5, h: 1.8, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 11, color: C.pale, lineSpacing: 16,
  });
  foot(s, "https://t08-passkey-portfolio.vercel.app/#private-boundary", true);
}

/* 5 — 비밀번호 칸이 없다 */
{
  const s = page(false, "C35");
  head(s, "비밀번호를 입력하는 칸이 없습니다", "표시 이름과 패스키 이름만 받고, 잠금은 기기의 지문이 엽니다.", false);
  if (has("03-access-no-password-field.png"))
    s.addImage({ path: shot("03-access-no-password-field.png"), x: 0.75, y: 2.35, w: 7.4, h: 4.05 });
  card(s, 8.55, 2.35, 4.05, 4.05, false);
  s.addText("userVerification: required", {
    x: 8.85, y: 2.6, w: 3.5, h: 0.35, isTextBox: true, margin: 0,
    fontFace: MONO, fontSize: 12, bold: true, color: C.forest,
  });
  s.addText("등록과 로그인 options 양쪽에 걸려 있고, 서버 검증에서도 다시 강제합니다. 지문을 거치지 않아 UV 플래그가 꺼진 응답은 서명이 맞아도 거절됩니다.", {
    x: 8.85, y: 3.08, w: 3.5, h: 1.7, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 11.5, color: C.ink, lineSpacing: 16,
  });
  s.addText("지문 자체는 기기를 벗어나지 않습니다. 서버에 생체정보는 저장되지 않습니다.", {
    x: 8.85, y: 4.95, w: 3.5, h: 1.2, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 11, italic: true, color: C.muted, lineSpacing: 16,
  });
  foot(s, "WebAuthnController:141 · PasskeyController:76 · WebAuthnVerificationAdapter:77,147", false);
}

/* 6 — 요청과 응답 */
{
  const s = page(true, "C16 C17 C18");
  head(s, "정말 잠겨 있는지, 요청과 응답으로", "막았다는 말 대신 실제 응답을 냅니다. 프로덕션에서 그대로 받은 기록입니다.", true);
  const blocks = [
    ["미인증 · 비공개 페이지", "$ curl -i .../private\n\nHTTP/1.1 401 Unauthorized\nCache-Control: no-store\nContent-Type: application/json\n\n{\"error\":\n  \"passkey_session_required\"}"],
    ["미인증 · 비공개 API", "$ curl -i .../api/private-items\n\nHTTP/1.1 401 Unauthorized\nCache-Control: no-store\nContent-Type: application/json\n\n{\"error\":\n  \"passkey_session_required\"}"],
    ["공개 첫 화면 · 유출 여부", "$ curl -o /dev/null -w '%{http_code}' .../\n200\n\n$ curl -s .../ |\n    grep -c '합성 프로젝트|...'\n0"],
  ];
  blocks.forEach(([title, body], i) => {
    const x = 0.75 + i * 4.0;
    card(s, x, 2.45, 3.6, 3.45, true);
    s.addText(title, {
      x: x + 0.25, y: 2.65, w: 3.1, h: 0.3, isTextBox: true, margin: 0,
      fontFace: F, fontSize: 11.5, bold: true, color: C.lime,
    });
    s.addText(body, {
      x: x + 0.25, y: 3.02, w: 3.15, h: 2.7, isTextBox: true, margin: 0,
      fontFace: MONO, fontSize: 9, color: C.pale, lineSpacing: 13,
    });
  });
  s.addText("거절은 401입니다. 화면에서만 감춘 것이 아니라 서버가 내려 주지 않습니다.", {
    x: 0.78, y: 6.1, w: 11.8, h: 0.4, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 12, color: C.lime,
  });
  foot(s, "2026-09-11 프로덕션 · 세션·토큰이 애초에 발급되지 않은 요청입니다", true);
}

/* 7 — 등록 흐름 */
{
  const s = page(false, "C19 ~ C23");
  head(s, "등록할 때 무엇이 오가나", "서버는 질문을 만들고, 기기는 열쇠 한 쌍을 만들어 공개키만 돌려줍니다.", false);
  const steps = [
    ["01", "서버가 질문을 만든다", "요청마다 다른 난수 challenge를 만들고, 확인할 때까지 서버가 들고 있습니다."],
    ["02", "기기가 열쇠를 만든다", "개인키는 기기의 보안 하드웨어에 남고, 밖으로 나오지 않습니다."],
    ["03", "공개키와 서명만 간다", "등록 요청 본문에 개인키는 없습니다. 공개키·credential ID·서명뿐입니다."],
    ["04", "서버가 확인하고 저장한다", "질문은 한 번 쓰면 버려집니다. 저장되는 것은 공개키입니다."],
  ];
  steps.forEach(([n, t, d], i) => {
    const y = 2.45 + i * 1.1;
    s.addShape(pres.ShapeType.ellipse, { x: 0.78, y, w: 0.62, h: 0.62, fill: { color: C.forest } });
    s.addText(n, {
      x: 0.78, y, w: 0.62, h: 0.62, isTextBox: true, margin: 0, align: "center", valign: "middle",
      fontFace: MONO, fontSize: 12, bold: true, color: C.lime,
    });
    s.addText(t, {
      x: 1.65, y: y + 0.02, w: 3.6, h: 0.34, isTextBox: true, margin: 0,
      fontFace: F, fontSize: 13.5, bold: true, color: C.ink,
    });
    s.addText(d, {
      x: 1.65, y: y + 0.38, w: 6.6, h: 0.55, isTextBox: true, margin: 0,
      fontFace: F, fontSize: 11.5, color: C.muted, lineSpacing: 15,
    });
  });
  card(s, 8.75, 2.45, 3.85, 4.0, false);
  s.addText("서버에 저장되는 것", {
    x: 9.05, y: 2.68, w: 3.3, h: 0.32, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 13, bold: true, color: C.forest,
  });
  s.addText("public key (COSE)\ncredential ID\nsign counter\nbackup 상태\n등록 시각 · 별칭", {
    x: 9.05, y: 3.12, w: 3.3, h: 1.55, isTextBox: true, margin: 0,
    fontFace: MONO, fontSize: 11, color: C.ink, lineSpacing: 18,
  });
  s.addText("저장되지 않는 것", {
    x: 9.05, y: 4.85, w: 3.3, h: 0.32, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 13, bold: true, color: C.orange,
  });
  s.addText("개인키\n비밀번호\n생체정보", {
    x: 9.05, y: 5.3, w: 3.3, h: 0.95, isTextBox: true, margin: 0,
    fontFace: MONO, fontSize: 11, color: C.orange, lineSpacing: 18,
  });
  foot(s, "CeremonyService · WebAuthnVerificationAdapter · AccountRegistrationService", false);
}

/* 8 — 저장 위치 배지 */
{
  const s = page(true, "C26 C43");
  head(s, "저장 위치를 화면이 말합니다", "어디에 저장됐는지 확인하려고 기기 설정을 열 필요가 없습니다.", true);
  if (has("c-badges.png"))
    s.addImage({ path: shot("c-badges.png"), x: 0.75, y: 2.45, w: 11.85, h: 2.32 });
  const notes = [
    ["동기화됨", "구글 계정에 백업됩니다. 기기를 바꿔도 따라옵니다.", C.lime],
    ["이 기기 / 보안 키", "어디서 만들어졌는지. 두 패스키를 구분할 근거가 됩니다.", C.orange],
  ];
  notes.forEach(([k, v, col], i) => {
    const x = 0.78 + i * 6.2;
    s.addText(k, {
      x, y: 5.1, w: 3.0, h: 0.34, isTextBox: true, margin: 0,
      fontFace: F, fontSize: 13, bold: true, color: col,
    });
    s.addText(v, {
      x, y: 5.5, w: 5.6, h: 0.7, isTextBox: true, margin: 0,
      fontFace: F, fontSize: 11.5, color: C.pale, lineSpacing: 15,
    });
  });
  s.addText("등록 응답이 주는 backupEligible과 transports를 그대로 옮긴 값입니다. 인증기를 설명하는 값이지 키가 아니므로 비밀이 아닙니다.", {
    x: 0.78, y: 6.3, w: 11.8, h: 0.5, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 11, italic: true, color: C.line,
  });
}

/* 9 — 두 개 → 삭제 → 재로그인 */
{
  const s = page(false, "C42 ~ C45");
  head(s, "기기를 잃어버렸을 때", "패스키를 두 개 등록하고, 하나를 지운 뒤에도 들어갑니다.", false);
  const seq = [
    ["08-two-passkeys.png", "두 개 등록", "내 휴대폰 · 예비 보안 키"],
    ["09-after-deleting-first.png", "하나 삭제", "내 휴대폰을 지웠습니다"],
    ["11-login-with-remaining-passkey.png", "남은 것으로 로그인", "예비 보안 키로 통과"],
  ];
  seq.forEach(([img, t, d], i) => {
    const x = 0.75 + i * 4.0;
    if (has(img)) s.addImage({ path: shot(img), x, y: 2.45, w: 3.6, h: 2.42 });
    s.addText((i + 1) + ". " + t, {
      x, y: 5.02, w: 3.6, h: 0.34, isTextBox: true, margin: 0,
      fontFace: F, fontSize: 13, bold: true, color: C.forest,
    });
    s.addText(d, {
      x, y: 5.42, w: 3.6, h: 0.4, isTextBox: true, margin: 0,
      fontFace: F, fontSize: 11.5, color: C.muted,
    });
  });
  s.addText("지운 패스키로는 더 이상 들어갈 수 없습니다. 남은 하나로는 그대로 들어갑니다.", {
    x: 0.78, y: 6.05, w: 11.8, h: 0.4, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 12, color: C.ink,
  });
  foot(s, "로컬 인스턴스 · 가상 인증기로 실제 등록·삭제·로그인을 수행한 화면", false);
}

/* 10 — 마지막 하나 */
{
  const s = page(true, "C46");
  head(s, "마지막 패스키는 지워지지 않습니다", "하나까지 지울 수 있으면 그 순간 계정을 되찾을 방법이 사라집니다.", true);
  if (has("c-last-refused.png"))
    s.addImage({ path: shot("c-last-refused.png"), x: 0.75, y: 2.55, w: 11.85, h: 2.32 });
  s.addText("HTTP 409 · final_passkey_required", {
    x: 0.78, y: 5.15, w: 6.0, h: 0.4, isTextBox: true, margin: 0,
    fontFace: MONO, fontSize: 13, bold: true, color: C.lime,
  });
  s.addText("화면에는 “마지막 패스키는 삭제할 수 없습니다. 먼저 다른 패스키를 등록하세요.”가 나옵니다. 이 시스템에는 비밀번호도 이메일 복구도 없기 때문입니다.", {
    x: 0.78, y: 5.62, w: 11.8, h: 0.9, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 12, color: C.pale, lineSpacing: 17,
  });
}

/* 11 — 확인 네 가지 */
{
  const s = page(false, "C50");
  head(s, "안 열리는 것을 확인한 기록", "통과한 요청과 막힌 요청을 나란히 냅니다.", false);
  const rows = [
    ["로그인 없이 열기", "인증 세션의 조회 → 200, 3건", "무세션 요청 → 401", "01-public-private-boundary"],
    ["남의 패스키로 열기", "A 패스키로 A 자료 → 200", "A→B, B→A → 동일한 404", "08-cross-account-isolation"],
    ["쓴 질문 재사용", "최초 검증된 assertion → 200", "같은 질문 재사용 → 400", "06-passkey-authentication"],
    ["삭제 뒤 로그인", "남은 예비 키 → 성공", "지운 기기 키 → 거절", "07-second-passkey-recovery"],
  ];
  const cols = [3.0, 3.3, 2.9, 2.55];
  const xs = [0.75];
  cols.forEach((c, i) => xs.push(xs[i] + c + 0.12));
  ["확인", "통과한 요청", "막힌 요청", "증거 파일"].forEach((h, i) => {
    s.addText(h, {
      x: xs[i] + 0.16, y: 2.4, w: cols[i] - 0.2, h: 0.32, isTextBox: true, margin: 0,
      fontFace: MONO, fontSize: 10, bold: true, color: C.muted, charSpacing: 1,
    });
  });
  rows.forEach((r, ri) => {
    const y = 2.85 + ri * 0.95;
    card(s, 0.75, y, 11.85, 0.8, false);
    r.forEach((cell, ci) => {
      s.addText(cell, {
        x: xs[ci] + 0.16, y: y + 0.16, w: cols[ci] - 0.2, h: 0.5, isTextBox: true, margin: 0,
        fontFace: ci === 3 ? MONO : F,
        fontSize: ci === 3 ? 9 : 11.5,
        bold: ci === 0,
        color: ci === 0 ? C.ink : (ci === 1 ? C.forest : (ci === 2 ? C.orange : C.muted)),
        lineSpacing: 14,
      });
    });
  });
  foot(s, "전문은 소스 저장소 docs/evidence/ 에 있습니다", false);
}

/* 12 — 아직 못 막은 것 */
{
  const s = page(true, "C51");
  head(s, "아직 못 막은 것", "없다고 적지 않았습니다. 실기기에서 관측한 것까지 그대로 남깁니다.", true);
  const items = [
    "공개 등록에 초대·승인이 없어 자동 계정 생성을 악용할 수 있습니다.",
    "모든 패스키 기기를 잃으면 비밀번호·이메일 복구가 없어 계정을 되찾을 수 없습니다.",
    "동기화형 패스키가 sign counter를 늘 0으로 보고하면 복제 의심 신호를 얻기 어렵습니다.",
    "세션 저장소가 중단되면 로그인 상태를 확인할 수 없어 비공개 접근도 실패합니다.",
    "등록 직후 비공개 화면 첫 요청이 간헐적으로 500이 됩니다. 원인 미확정.",
  ];
  items.forEach((t, i) => {
    const y = 2.45 + i * 0.68;
    s.addShape(pres.ShapeType.ellipse, { x: 0.8, y: y + 0.1, w: 0.16, h: 0.16, fill: { color: C.orange } });
    s.addText(t, {
      x: 1.25, y, w: 11.3, h: 0.5, isTextBox: true, margin: 0,
      fontFace: F, fontSize: 12.5, color: C.pale, lineSpacing: 16,
    });
  });
  card(s, 0.75, 6.0, 11.85, 0.85, true);
  s.addText("마지막 항목은 2026-09-11 실물 휴대폰 등록에서 한 번 관측했습니다. 등록은 이미 커밋된 뒤라 데이터는 정상이고 새로고침하면 열립니다. 원인을 확정하지 못했으므로 고쳤다고 적지 않고, 실패를 읽을 수 있는 화면을 주는 데까지 했습니다.", {
    x: 1.0, y: 6.14, w: 11.4, h: 0.6, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 10.5, color: C.pale, lineSpacing: 14,
  });
}

/* 13 — 체크리스트 */
{
  const s = page(false, "CHECKLIST");
  head(s, "판정 47개", "카드 다섯 장의 기준을 전부 충족했습니다.", false);
  const cards = [
    ["카드 1", "공개와 비공개를\n가른다", "C13 ~ C18", 6],
    ["카드 2", "패스키를\n등록한다", "C19 ~ C26", 8],
    ["카드 3", "패스키로\n들어간다", "C27 ~ C35", 9],
    ["카드 4", "기기를\n잃어버렸을 때", "C42 ~ C46", 5],
    ["카드 5", "확인하고\n설명서를 쓴다", "C01~C12, C36~C53", 19],
  ];
  cards.forEach(([n, t, range, count], i) => {
    const x = 0.75 + i * 2.44;
    card(s, x, 2.4, 2.28, 2.6, false);
    s.addText(n, {
      x: x + 0.22, y: 2.6, w: 1.9, h: 0.3, isTextBox: true, margin: 0,
      fontFace: MONO, fontSize: 10, bold: true, color: C.muted, charSpacing: 1,
    });
    s.addText(t, {
      x: x + 0.22, y: 2.95, w: 1.9, h: 0.85, isTextBox: true, margin: 0,
      fontFace: F, fontSize: 12.5, bold: true, color: C.ink, lineSpacing: 16,
    });
    s.addText(count + " / " + count, {
      x: x + 0.22, y: 3.88, w: 1.9, h: 0.45, isTextBox: true, margin: 0,
      fontFace: F, fontSize: 21, bold: true, color: C.forest,
    });
    s.addText(range, {
      x: x + 0.22, y: 4.45, w: 1.95, h: 0.3, isTextBox: true, margin: 0,
      fontFace: MONO, fontSize: 8.5, color: C.muted,
    });
  });
  card(s, 0.75, 5.3, 11.85, 1.3, false);
  s.addText("47 / 47", {
    x: 1.1, y: 5.55, w: 2.5, h: 0.8, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 32, bold: true, color: C.forest,
  });
  s.addText("미완 항목 없음. 마지막까지 남아 있던 C26(저장 위치)은 2026-09-11 실물 휴대폰 등록으로 닫았습니다. 항목별 상태와 증거 파일은 소스 저장소의 T08-ACCEPTANCE-MATRIX.md에 있습니다.", {
    x: 3.9, y: 5.6, w: 8.4, h: 0.8, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 12, color: C.ink, lineSpacing: 17,
  });
}

/* 14 — 확인 방법 */
{
  const s = pres.addSlide();
  s.background = { color: C.forest };
  chip(s, 0.75, 0.5, "HOW TO CHECK", C.lime, C.forest);
  s.addText("짧은 확인 방법", {
    x: 0.75, y: 0.95, w: 11.8, h: 0.8, isTextBox: true, margin: 0,
    fontFace: F, fontSize: 34, bold: true, color: C.paper,
  });
  const qa = [
    ["어디로 가나요", "결과물 URL 첫 화면에서 “나만의 공간”으로 이동합니다."],
    ["세 단계 안에 무엇을 하나요", "합성 이름 입력 → 패스키 만들기(지문) → 비공개 자료 3건 확인."],
    ["무엇이 보이면 통과인가요", "지문 창이 뜨고, 통과 후 패스키 이름·등록일·저장 위치와 합성 자료가 보입니다."],
    ["안 될 때는 무엇이 보이나요", "취소·검증 실패 안내, 또는 미인증 요청의 401 응답이 보입니다."],
  ];
  qa.forEach(([q, a], i) => {
    const y = 2.05 + i * 1.0;
    s.addText("0" + (i + 1), {
      x: 0.78, y, w: 0.7, h: 0.35, isTextBox: true, margin: 0,
      fontFace: MONO, fontSize: 12, bold: true, color: C.lime,
    });
    s.addText(q, {
      x: 1.55, y, w: 3.6, h: 0.35, isTextBox: true, margin: 0,
      fontFace: F, fontSize: 13, bold: true, color: C.paper,
    });
    s.addText(a, {
      x: 5.3, y, w: 7.3, h: 0.7, isTextBox: true, margin: 0,
      fontFace: F, fontSize: 12, color: C.pale, lineSpacing: 16,
    });
  });
  s.addText("결과물   t08-passkey-portfolio.vercel.app\n소스       github.com/myeongjundev/t08-passkey-portfolio", {
    x: 0.78, y: 6.2, w: 11.8, h: 0.85, isTextBox: true, margin: 0,
    fontFace: MONO, fontSize: 12, color: C.lime, lineSpacing: 20,
  });
  s.addNotes("첫 요청은 콜드스타트로 몇 초 걸릴 수 있다. 제출 직전에 한 번 열어 두면 대기 화면 없이 열린다.");
}

await pres.writeFile({ fileName: OUT });
console.log("wrote", OUT);
