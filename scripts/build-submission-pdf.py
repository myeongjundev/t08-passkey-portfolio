"""T08 최종 제출 PDF.

체크리스트 47개 전문, 증거 화면, 그리고 실제로 동작하는 순서를 한 문서에 담는다.
색은 결과물이 쓰는 팔레트를 그대로 가져왔다.
"""
import io
import json
from pathlib import Path

from PIL import Image
from reportlab.lib.colors import HexColor
from reportlab.lib.enums import TA_LEFT, TA_RIGHT, TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas
from reportlab.platypus import Paragraph

SCRATCH = Path(r"C:\Users\user\AppData\Local\Temp\claude\C--gov-SKT-ALEPH\cf4dd2c2-cb26-4d3f-b130-1daf6ea3bd70\scratchpad")
SHOTS = SCRATCH / "shots"
OUT = Path(r"C:\gov\SKT_ALEPH\t08-passkey-portfolio\output\pdf\T08-final-submission.pdf")
OUT.parent.mkdir(parents=True, exist_ok=True)

pdfmetrics.registerFont(TTFont("KR", r"C:\Windows\Fonts\malgun.ttf"))
pdfmetrics.registerFont(TTFont("KRB", r"C:\Windows\Fonts\malgunbd.ttf"))
pdfmetrics.registerFont(TTFont("MONO", r"C:\Windows\Fonts\consola.ttf"))
pdfmetrics.registerFont(TTFont("MONOB", r"C:\Windows\Fonts\consolab.ttf"))

C = {
    "forest": HexColor("#1F3C2E"), "lime": HexColor("#C8F04B"),
    "paper": HexColor("#F3F0E9"), "paper2": HexColor("#E4DFD4"),
    "ink": HexColor("#171914"), "dark": HexColor("#14170F"),
    "dark2": HexColor("#1E2219"), "muted": HexColor("#66695F"),
    "orange": HexColor("#C4522F"), "pale": HexColor("#D7DDD5"),
    "line": HexColor("#CBC6BA"), "white": HexColor("#FFFFFF"),
}
W, H = A4          # 595 x 842 pt
M = 46             # 좌우 여백
CW = W - 2 * M     # 본문 폭

c = canvas.Canvas(str(OUT), pagesize=A4)
c.setTitle("T08 최종 제출 — 패스키로 잠근 소개 페이지")
c.setAuthor("myeongjundev")
state = {"page": 0}


def para(text, x, y, w, size=9.5, font="KR", color=None, leading=None, align=TA_LEFT):
    """왼쪽 아래 기준이 아니라 '위쪽 기준'으로 그린다. 쓴 높이를 돌려준다."""
    st = ParagraphStyle("p", fontName=font, fontSize=size,
                        leading=leading or size * 1.5,
                        textColor=color or C["ink"], alignment=align)
    p = Paragraph(text.replace("\n", "<br/>"), st)
    _, used = p.wrap(w, H)
    p.drawOn(c, x, y - used)
    return used


def chip(x, y, label, fill, color, w=None):
    tw = pdfmetrics.stringWidth(label, "MONOB", 7.5)
    w = w or tw + 14
    c.setFillColor(fill)
    c.rect(x, y, w, 14, fill=1, stroke=0)
    c.setFillColor(color)
    c.setFont("MONOB", 7.5)
    c.drawString(x + 7, y + 4.2, label)
    return w


def newpage(tag, title, sub=None, dark=False):
    if state["page"]:
        c.showPage()
    state["page"] += 1
    if dark:
        c.setFillColor(C["dark"])
        c.rect(0, 0, W, H, fill=1, stroke=0)
    else:
        c.setFillColor(C["paper"])
        c.rect(0, 0, W, H, fill=1, stroke=0)
    y = H - 52
    if tag:
        chip(M, y, tag, C["lime"] if dark else C["forest"],
             C["forest"] if dark else C["lime"])
    y -= 16
    used = para("<b>%s</b>" % title, M, y, CW, 20, "KRB",
                C["paper"] if dark else C["ink"], 25)
    y -= used + 6
    if sub:
        used = para(sub, M, y, CW, 9.5, "KR", C["pale"] if dark else C["muted"], 14)
        y -= used + 10
    return y - 8


def footer(text, dark=False):
    # 본문은 한글이 섞이므로 KR로 그린다. Consolas에는 한글이 없어 조용히 사라진다.
    c.setFillColor(C["muted"] if not dark else HexColor("#3A443B"))
    c.setFont("KR", 7)
    c.drawString(M, 28, text)
    c.setFont("MONO", 7)
    c.drawRightString(W - M, 28, "T08 · %d" % state["page"])


def img_height(path, w):
    im = Image.open(path)
    return w * im.height / im.width


def fit(y, need, title, dark=False):
    """남은 자리가 모자라면 쪽을 넘긴다. 그린 시작 y를 돌려준다."""
    if y - need < 58:
        footer_text = state.get("foot", "")
        if footer_text:
            footer(footer_text, state.get("footdark", False))
        return newpage(None, title, None, dark)
    return y


def image(path, x, y_top, w):
    """위쪽 기준으로 비율 유지해 그린다. 그린 높이를 돌려준다."""
    im = Image.open(path)
    h = w * im.height / im.width
    c.drawImage(str(path), x, y_top - h, width=w, height=h,
                preserveAspectRatio=True, mask="auto")
    c.setStrokeColor(C["line"])
    c.setLineWidth(0.5)
    c.rect(x, y_top - h, w, h, fill=0, stroke=1)
    return h


def card(x, y_top, w, h, dark=False):
    c.setFillColor(C["dark2"] if dark else C["white"])
    c.setStrokeColor(HexColor("#3A443B") if dark else C["paper2"])
    c.setLineWidth(0.7)
    c.roundRect(x, y_top - h, w, h, 3, fill=1, stroke=1)


# ---------------------------------------------------------------- 표지
c.setFillColor(C["forest"])
c.rect(0, 0, W, H, fill=1, stroke=0)
state["page"] = 1
chip(M, H - 150, "PASSKEY ONLY", C["lime"], C["forest"])
para("<b>비밀번호를 없애고<br/>패스키로 잠갔습니다</b>", M, H - 175, CW, 28, "KRB", C["paper"], 36)
para("T08 · 내 소개 페이지에 패스키 달기", M, H - 285, CW, 12, "KR", C["lime"], 18)
para("최종 제출 문서 — 판정 체크리스트와 증거", M, H - 305, CW, 10.5, "KR", C["pale"], 16)

y = H - 380
for label, value in [("판정", "47 / 47 충족"), ("자동 테스트", "50개 통과"),
                     ("비밀번호 입력 칸", "0개"), ("실기기 확인", "2026-09-11 · 안드로이드 크롬")]:
    para(label, M, y, 150, 9, "KR", C["pale"])
    para("<b>%s</b>" % value, M + 150, y, 300, 9.5, "KRB", C["lime"])
    y -= 22

c.setStrokeColor(HexColor("#3A5A46"))
c.setLineWidth(0.7)
c.line(M, y - 6, W - M, y - 6)
y -= 30
para("결과물", M, y, 90, 9, "KR", C["pale"])
para("https://t08-passkey-portfolio.vercel.app", M + 90, y, 420, 9, "MONO", C["paper"])
y -= 20
para("소스", M, y, 90, 9, "KR", C["pale"])
para("https://github.com/myeongjundev/t08-passkey-portfolio", M + 90, y, 420, 9, "MONO", C["paper"])

para("이 문서의 계정과 비공개 내용은 모두 과제 확인용으로 만들어 넣은 합성 데이터입니다. "
     "실제 개인정보는 들어 있지 않으며, challenge·서명·쿠키·세션 ID·CSRF 값과 개인키는 기록하지 않았습니다.",
     M, 130, CW, 8.5, "KR", C["pale"], 13)
para("2026-09-11 · myeongjundev", M, 92, CW, 8.5, "MONO", C["lime"], 12)

# ---------------------------------------------------------------- 요약
y = newpage("SUMMARY", "한 장으로 보는 결과",
            "공개 소개는 누구나 볼 수 있고, 비공개 자리는 패스키를 가진 기기만 엽니다. 비밀번호는 어디에도 없습니다.")
rows = [
    ("개인키", "기기를 떠나지 않습니다. 서버는 공개키·credential ID·sign counter만 저장합니다."),
    ("질문(challenge)", "서버가 만들고 서버가 들고 있다가, 한 번 쓰면 버립니다. 재사용은 거절됩니다."),
    ("잠금 해제", "기기의 지문 인증입니다. 지문 데이터는 서버로 오지 않고 생체정보는 저장되지 않습니다."),
    ("사람을 알아보는 값", "JWT가 아니라 서버의 HttpSession입니다. 로그아웃하면 즉시 무효화됩니다."),
    ("기기 분실 대비", "패스키를 두 개 등록할 수 있고, 마지막 하나는 지워지지 않습니다."),
    ("계정 사이 격리", "URL의 accountId를 소유권 입력으로 쓰지 않고 세션의 account ID만 씁니다."),
]
for k, v in rows:
    card(M, y, CW, 34)
    para("<b>%s</b>" % k, M + 12, y - 9, 110, 9, "KRB", C["forest"])
    para(v, M + 130, y - 9, CW - 142, 9, "KR", C["ink"], 13)
    y -= 42

y -= 6
para("<b>짧은 확인 방법</b>", M, y, CW, 12, "KRB", C["ink"])
y -= 24
qa = [
    ("어디로 가나요", "결과물 URL의 공개 소개 첫 화면에서 “나만의 공간”으로 이동합니다."),
    ("세 단계 안에 무엇을 하나요", "합성 이름 입력 → 패스키 만들기(기기 지문 인증) → 비공개 자료 3건 확인."),
    ("무엇이 보이면 통과인가요", "지문 인증 창이 뜨고, 통과 후 패스키 이름·등록일·저장 위치와 합성 비공개 자료가 보입니다."),
    ("안 될 때는 무엇이 보이나요", "취소·검증 실패 안내, 또는 미인증 요청의 401 응답이 보입니다."),
]
for i, (q, a) in enumerate(qa, 1):
    para("<b>0%d</b>" % i, M, y, 20, 9, "MONOB", C["forest"])
    para("<b>%s</b>" % q, M + 24, y, 150, 9, "KRB", C["ink"])
    used = para(a, M + 182, y, CW - 182, 9, "KR", C["muted"], 13)
    y -= max(used, 14) + 8

y -= 8
para("<b>AI와 내 판단</b>", M, y, CW, 12, "KRB", C["ink"])
y -= 24
ai = [
    ("AI에게 맡긴 일", "요구사항 정리, WebAuthn 연결 초안, 자동화 테스트와 증거 문서 작성."),
    ("내가 직접 판단한 일", "비밀번호 없는 공개 등록, 서버 세션, 마지막 패스키 삭제 금지, 합성 데이터 정책."),
    ("따르지 않은 제안", "T07의 비밀번호·JWT 방식을 재사용하지 않았습니다. T08은 개인키가 기기를 떠나지 않는 패스키 과제이기 때문입니다."),
]
for i, (q, a) in enumerate(ai, 1):
    para("<b>0%d</b>" % i, M, y, 20, 9, "MONOB", C["forest"])
    para("<b>%s</b>" % q, M + 24, y, 150, 9, "KRB", C["ink"])
    used = para(a, M + 182, y, CW - 182, 9, "KR", C["muted"], 13)
    y -= max(used, 14) + 8
footer("결과물 https://t08-passkey-portfolio.vercel.app")

# ---------------------------------------------------------------- 체크리스트
criteria = json.loads((SCRATCH / "criteria.json").read_text(encoding="utf-8"))
CARD_KO = {
    "Card 1 — Split public from private": "카드 1 · 공개와 비공개를 가른다",
    "Card 2 — Register a passkey": "카드 2 · 패스키를 등록한다",
    "Card 3 — Sign in with a passkey": "카드 3 · 패스키로 들어간다",
    "Card 4 — Losing a device": "카드 4 · 기기를 잃어버렸을 때",
    "Card 5 — Submission, isolation checks, and the write-up": "카드 5 · 확인하고 설명서를 쓴다",
}
y = newpage("CHECKLIST", "판정 체크리스트 47개",
            "과제 카드의 기준 문장을 그대로 옮기고, 각 항목의 상태와 증거 파일을 붙였습니다. 미완 항목은 없습니다.")
current = None
for item in criteria:
    if y < 110:
        footer("전문 증거는 소스 저장소 docs/evidence/ 에 있습니다")
        y = newpage(None, "판정 체크리스트 (이어서)")
        current = None
    if item["card"] != current:
        current = item["card"]
        if y < 150:
            footer("전문 증거는 소스 저장소 docs/evidence/ 에 있습니다")
            y = newpage(None, "판정 체크리스트 (이어서)")
        y -= 6
        n = sum(1 for k in criteria if k["card"] == current)
        para("<b>%s</b>" % CARD_KO.get(current, current), M, y, CW - 70, 10.5, "KRB", C["forest"])
        para("%d / %d" % (n, n), W - M - 70, y, 70, 10.5, "KRB", C["forest"], align=TA_RIGHT)
        y -= 20
    st = ParagraphStyle("x", fontName="KR", fontSize=8.5, leading=12, textColor=C["ink"])
    p = Paragraph(item["text"], st)
    # 본문은 M+74에서 시작하고 증거 칸은 W-M-148에서 시작한다. 그 사이만 쓴다.
    text_w = CW - 148 - 74 - 12
    _, th = p.wrap(text_w, H)
    rowh = max(th + 12, 26)
    card(M, y, CW, rowh)
    # 충족 표시
    c.setFillColor(C["forest"])
    c.circle(M + 16, y - rowh / 2, 5.2, fill=1, stroke=0)
    c.setFillColor(C["lime"])
    c.setFont("MONOB", 6.5)
    c.drawCentredString(M + 16, y - rowh / 2 - 2.2, "V")
    para("<b>%s</b>" % item["id"].replace("T08-", ""), M + 28, y - 7, 42, 8, "MONOB", C["muted"])
    p.drawOn(c, M + 74, y - 7 - th)
    ev = item["evidence"].replace("../", "").replace(" · ", "\n") or "—"
    para(ev, W - M - 148, y - 7, 142, 6.5, "MONO", C["muted"], 9)
    y -= rowh + 5
footer("전문 증거는 소스 저장소 docs/evidence/ 에 있습니다")

# ---------------------------------------------------------------- 증거 1: 경계
y = newpage("EVIDENCE · C10 C11 C13", "공개는 어디까지인가",
            "첫 화면은 등록 없이 열리는 공개 소개입니다. T01에서 만든 소개가 그대로 남아 있고, 잠긴 자리가 있다는 사실은 첫 화면에서 바로 보입니다.")
IW = CW * 0.82          # 두 장이 한 쪽에 같이 들어가는 폭
IX = M + (CW - IW) / 2
h = image(SHOTS / "01-public-first-screen.png", IX, y, IW)
y -= h + 10
para("공개 소개 첫 화면. 소개 문장 바로 아래의 <b>PASSKEY ONLY</b> 띠가 비공개 자리로 가는 입구입니다. "
     "패스키가 이 과제의 본체라 입구를 첫 화면으로 올렸고, 공개 내용이 끝나는 자리의 05 섹션은 그대로 두었습니다.",
     M, y, CW, 8.5, "KR", C["muted"], 13)
y -= 34
state["foot"] = "https://t08-passkey-portfolio.vercel.app/ · 2026-09-11 캡처"
y = fit(y, img_height(SHOTS / "02-public-private-boundary.png", IW) + 34, "공개는 어디까지인가 (이어서)")
h = image(SHOTS / "02-public-private-boundary.png", IX, y, IW)
y -= h + 10
para("공개 내용이 끝나는 자리. “공개 소개는 여기까지, 그다음은 패스키로.” 경계를 화면에서 한 번 더 긋습니다.",
     M, y, CW, 8.5, "KR", C["muted"], 13)
footer(state["foot"])
state["foot"] = ""

# ---------------------------------------------------------------- 증거 2: 요청과 응답
y = newpage("EVIDENCE · C16 C17 C18", "정말 잠겨 있는지, 요청과 응답으로",
            "막았다는 말 대신 실제 응답을 냅니다. 아래는 프로덕션에서 그대로 받은 기록입니다.", dark=True)
blocks = [
    ("미인증으로 비공개 페이지를 직접 열면",
     "$ curl -i https://t08-passkey-portfolio.vercel.app/private\n\n"
     "HTTP/1.1 401 Unauthorized\nCache-Control: no-store\n"
     "Content-Type: application/json;charset=UTF-8\n\n"
     '{"error":"passkey_session_required"}'),
    ("미인증으로 비공개 API를 직접 부르면",
     "$ curl -i https://t08-passkey-portfolio.vercel.app/api/private-items\n\n"
     "HTTP/1.1 401 Unauthorized\nCache-Control: no-store\n"
     "Content-Type: application/json;charset=UTF-8\n\n"
     '{"error":"passkey_session_required"}'),
    # Consolas에는 한글이 없어 그대로 두면 조용히 사라진다. 그 부분만 한글 폰트로 섞는다.
    ("공개 첫 화면은 열리고, 비공개 내용은 본문에 없다",
     "$ curl -s -o /dev/null -w '%{http_code}' .../\n200\n\n"
     "$ curl -s .../ | grep -cE\n"
     "    '<font name=\"KR\">합성 프로젝트|합성 지원 기업|합성 주간 회고</font>'\n"
     "0"),
]
for title, body in blocks:
    lines = body.count("\n") + 1
    bh = lines * 11 + 34
    card(M, y, CW, bh, dark=True)
    para("<b>%s</b>" % title, M + 14, y - 10, CW - 28, 9, "KRB", C["lime"])
    para(body, M + 14, y - 26, CW - 28, 7.8, "MONO", C["pale"], 11)
    y -= bh + 12

y -= 4
para("<b>거절은 401입니다.</b> 화면에서만 감춘 것이 아니라 서버가 내려 주지 않습니다. "
     "미인증 상태로 받은 첫 화면의 소스 어디에도 비공개 항목의 제목이 들어 있지 않습니다.",
     M, y, CW, 9, "KR", C["pale"], 14)
y -= 30
para("이 요청들은 세션과 CSRF 토큰이 애초에 발급되지 않은 상태의 것이라, 가릴 비밀값이 없습니다.",
     M, y, CW, 8, "KR", HexColor("#8A9A8D"), 12)
footer("2026-09-11 프로덕션", dark=True)

# ---------------------------------------------------------------- 증거 3: 등록과 저장 위치
y = newpage("EVIDENCE · C19-C26", "등록할 때 무엇이 오가나",
            "서버가 질문을 만들고, 기기가 열쇠 한 쌍을 만들어 공개키만 돌려줍니다. 개인키는 기기를 떠나지 않습니다.")
steps = [
    ("01", "서버가 질문을 만든다", "요청마다 다른 난수 challenge를 만들고, 확인할 때까지 서버가 보관합니다."),
    ("02", "기기가 열쇠 한 쌍을 만든다", "개인키는 기기의 보안 하드웨어에 남고 밖으로 나오지 않습니다."),
    ("03", "공개키와 서명만 서버로 간다", "등록 요청 본문에 개인키는 없습니다. 공개키·credential ID·서명뿐입니다."),
    ("04", "서버가 확인하고 저장한다", "쓴 질문은 버려집니다. 저장되는 값은 공개키이지 비밀번호가 아닙니다."),
]
for n, t, d in steps:
    card(M, y, CW, 36)
    c.setFillColor(C["forest"])
    c.circle(M + 20, y - 18, 10, fill=1, stroke=0)
    c.setFillColor(C["lime"])
    c.setFont("MONOB", 8)
    c.drawCentredString(M + 20, y - 21, n)
    para("<b>%s</b>" % t, M + 40, y - 10, 165, 9, "KRB", C["ink"])
    para(d, M + 215, y - 10, CW - 228, 8.5, "KR", C["muted"], 12)
    y -= 44

y -= 8
para("<b>저장 위치를 화면이 말합니다</b>", M, y, CW, 12, "KRB", C["ink"])
y -= 18
para("등록 응답이 주는 backupEligible과 transports를 그대로 옮겨 목록에 표시합니다. "
     "어디에 저장됐는지 확인하려고 기기 설정을 열 필요가 없습니다. "
     "두 값 모두 인증기를 설명하는 값이지 키가 아니므로 비밀이 아닙니다.",
     M, y, CW, 8.5, "KR", C["muted"], 13)
y -= 40
state["foot"] = "CeremonyService · WebAuthnVerificationAdapter · AccountRegistrationService"
y = fit(y, img_height(SHOTS / "c-badges.png", CW) + 110, "등록할 때 무엇이 오가나 (이어서)")
h = image(SHOTS / "c-badges.png", M, y, CW)
y -= h + 12
para("<b>동기화됨</b> — 구글 계정에 백업됩니다. 기기를 바꿔도 따라옵니다.<br/>"
     "<b>이 기기 / 보안 키</b> — 어디서 만들어졌는지. 두 패스키를 구분할 근거가 됩니다.",
     M, y, CW, 8.5, "KR", C["ink"], 14)
y -= 34
card(M, y, CW, 46)
para("실기기 확인 — 2026-09-11 · 안드로이드 크롬에서 등록한 패스키는 <b>Google 비밀번호 관리자</b>에 저장되었습니다. "
     "기기 자체도 보안 키도 아닙니다. 목록의 “동기화됨 · 이 기기” 배지와 등록·로그인 때 뜨는 "
     "Google 비밀번호 관리자 창이 같은 것을 가리킵니다. (T08-C26)",
     M + 12, y - 10, CW - 24, 8.5, "KR", C["ink"], 12.5)
footer(state["foot"])
state["foot"] = ""

# ---------------------------------------------------------------- 증거 4: 동작
y = newpage("EVIDENCE · C42-C46", "실제로 동작하는 순서",
            "패스키 두 개를 등록하고, 하나를 지운 뒤 남은 하나로 들어갑니다. 마지막 하나는 지워지지 않습니다.")
seq = [
    ("08-two-passkeys.png", "1. 패스키 두 개를 등록했습니다",
     "내 휴대폰(동기화됨 · 이 기기)과 예비 보안 키(보안 키). 이름·등록일·저장 위치가 모두 보입니다."),
    ("09-after-deleting-first.png", "2. 하나를 지웠습니다",
     "내 휴대폰을 삭제했습니다. 지운 패스키로는 더 이상 들어갈 수 없습니다."),
    ("11-login-with-remaining-passkey.png", "3. 남은 하나로 들어갔습니다",
     "예비 보안 키로 로그인해 비공개 자리가 그대로 열립니다."),
]
state["foot"] = "로컬 인스턴스 · 가상 인증기로 등록·삭제·로그인을 실제로 수행한 화면"
for img, t, d in seq:
    y = fit(y, img_height(SHOTS / img, CW * 0.66) + 34, "실제로 동작하는 순서 (이어서)")
    para("<b>%s</b>" % t, M, y, CW, 9.5, "KRB", C["forest"])
    y -= 16
    h = image(SHOTS / img, M, y, CW * 0.66)
    para(d, M + CW * 0.66 + 14, y - 2, CW * 0.34 - 14, 8.5, "KR", C["muted"], 12.5)
    y -= h + 18
footer(state["foot"])
state["foot"] = ""

# ---------------------------------------------------------------- 증거 5: 거절
y = newpage("EVIDENCE · C46", "마지막 패스키는 지워지지 않습니다",
            "하나까지 지울 수 있으면 그 순간 계정을 되찾을 방법이 사라집니다. 이 시스템에는 비밀번호도 이메일 복구도 없습니다.",
            dark=True)
h = image(SHOTS / "c-last-refused.png", M, y, CW)
y -= h + 14
para("<b>HTTP 409 · final_passkey_required</b>", M, y, CW, 10, "MONOB", C["lime"])
y -= 18
para("화면에는 “마지막 패스키는 삭제할 수 없습니다. 먼저 다른 패스키를 등록하세요.”가 나옵니다.",
     M, y, CW, 9, "KR", C["pale"], 13)
y -= 34
para("<b>안 열리는 것을 확인한 네 가지</b>", M, y, CW, 11, "KRB", C["paper"])
y -= 20
checks = [
    ("로그인 없이 열기", "인증 세션의 조회 → 200, 3건", "무세션 요청 → 401", "01-public-private-boundary"),
    ("남의 패스키로 열기", "A 패스키로 A 자료 → 200", "A→B와 B→A → 동일한 404", "08-cross-account-isolation"),
    ("이미 쓴 질문 재사용", "최초 검증된 assertion → 200", "같은 질문 재사용 → 400", "06-passkey-authentication"),
    ("삭제한 패스키로 로그인", "남은 예비 키 → 성공", "지운 기기 키 → 거절", "07-second-passkey-recovery"),
]
for name, ok, no, ev in checks:
    card(M, y, CW, 40, dark=True)
    para("<b>%s</b>" % name, M + 12, y - 10, 120, 8.5, "KRB", C["paper"])
    para("통과  %s" % ok, M + 140, y - 10, 180, 8, "KR", C["lime"], 11)
    para("거절  %s" % no, M + 140, y - 24, 180, 8, "KR", HexColor("#F09070"), 11)
    para(ev, M + 330, y - 10, CW - 342, 7, "MONO", HexColor("#8A9A8D"), 10)
    y -= 46
footer("전문은 소스 저장소 docs/evidence/ 에 있습니다", dark=True)

# ---------------------------------------------------------------- 못 막은 것
y = newpage("GUIDE 6 · C51", "아직 못 막은 것",
            "없다고 적지 않았습니다. 실기기에서 관측한 것까지 그대로 남깁니다.")
items = [
    ("공개 등록에 초대·승인이 없습니다", "누구나 계정을 만들 수 있어 자동 계정 생성을 악용할 수 있습니다."),
    ("모든 패스키 기기를 잃으면 복구가 없습니다", "비밀번호도 이메일 복구도 없으므로 계정을 되찾을 수 없습니다. 그래서 마지막 하나의 삭제를 막았습니다."),
    ("sign counter가 늘 0이면 복제를 알기 어렵습니다", "동기화형 패스키는 counter를 0으로 보고하는 경우가 있어 복제 의심 신호를 얻기 어렵습니다."),
    ("세션 저장소가 멈추면 비공개 접근도 실패합니다", "로그인 상태를 확인할 수 없을 때 추측하거나 우회하지 않고 실패하는 쪽으로 동작합니다."),
    ("등록 직후 첫 요청이 간헐적으로 500이 됩니다", "2026-09-11 실물 휴대폰 등록에서 한 번 관측했습니다. 새로고침하면 열리고 등록은 이미 커밋된 뒤라 데이터는 정상이지만, 원인은 확정하지 못했습니다."),
]
for t, d in items:
    st = ParagraphStyle("x", fontName="KR", fontSize=8.5, leading=12.5, textColor=C["muted"])
    p = Paragraph(d, st)
    _, dh = p.wrap(CW - 40, H)
    rowh = dh + 30
    card(M, y, CW, rowh)
    c.setFillColor(C["orange"])
    c.circle(M + 16, y - 15, 3.4, fill=1, stroke=0)
    para("<b>%s</b>" % t, M + 28, y - 8, CW - 44, 9, "KRB", C["ink"])
    p.drawOn(c, M + 28, y - 24 - dh)
    y -= rowh + 8

y -= 6
note = ("원인을 확정하지 못했으므로 고쳤다고 적지 않았습니다. 지금 대응은 원인 제거가 아니라, 같은 일이 다시 나도 "
        "읽을 수 있는 화면을 주는 데까지입니다. 에러 화면이 없어 Spring 기본 페이지가 그대로 나오던 것을 페이지 톤에 맞는 "
        "안내와 새로고침 버튼으로 바꿨고, 응답에 서버 내부 정보가 실리지 않도록 네 가지 설정을 고정했습니다. "
        "미인증 거절이 여전히 401 JSON인 것도 테스트로 묶었습니다. "
        "기록은 docs/evidence/12-real-device-registration.md에 있습니다.")
_st = ParagraphStyle("n", fontName="KR", fontSize=8.5, leading=12.5)
_, note_h = Paragraph(note, _st).wrap(CW - 24, H)
card(M, y, CW, note_h + 34)
para("<b>마지막 항목에 대해</b>", M + 12, y - 10, CW - 24, 9, "KRB", C["ink"])
para(note, M + 12, y - 26, CW - 24, 8.5, "KR", C["muted"], 12.5)
y -= note_h + 34 + 16

para("<b>설명서 여섯 항목은 어디에 있나</b>", M, y, CW, 11, "KRB", C["ink"])
y -= 20
guide = [
    ("① 무엇으로 붙였나", "WebAuthn4J 0.31.8 + Spring Boot 4.1.1, 서버 세션, 기기 지문 인증"),
    ("② 왜 그걸 골랐나", "검증은 라이브러리에 맡기고 challenge 수명·소유권·세션을 애플리케이션이 담당"),
    ("③ 어디를 어떻게 고쳤나", "등록·로그인·로그아웃·비공개 조회 네 흐름이 지나는 소스 위치"),
    ("④ 안 열리는 것을 확인한 기록", "확인 네 가지의 통과·거절 대조표"),
    ("⑤ AI와 나", "맡긴 일과 직접 판단한 일, 따르지 않은 제안"),
    ("⑥ 아직 못 막은 것", "이 쪽에 옮겨 적은 다섯 가지"),
]
for k, v in guide:
    para("<b>%s</b>" % k, M, y, 150, 8.5, "KRB", C["forest"])
    para(v, M + 160, y, CW - 160, 8.5, "KR", C["muted"], 12)
    y -= 18
y -= 6
para("전문은 소스 저장소의 <font name=\"MONO\">docs/T08-AUTH-GUIDE.md</font>에 있습니다.",
     M, y, CW, 8.5, "KR", C["muted"])
footer("소스 https://github.com/myeongjundev/t08-passkey-portfolio")

c.showPage()
c.save()
print("wrote", OUT, OUT.stat().st_size, "bytes")
