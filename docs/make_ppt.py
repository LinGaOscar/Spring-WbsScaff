"""產生 WBS 管理系統投影片 (docs/presentation.pptx) — 含推銷版"""
from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN
import os

BASE = os.path.dirname(os.path.abspath(__file__))
SS   = os.path.join(BASE, "screenshots")
OUT  = os.path.join(BASE, "presentation.pptx")

W = Inches(13.33)
H = Inches(7.5)

C_DARK   = RGBColor(0x0F, 0x17, 0x2A)
C_NAVY   = RGBColor(0x1E, 0x29, 0x3B)
C_BLUE   = RGBColor(0x3B, 0x82, 0xF6)
C_ACCENT = RGBColor(0x0E, 0xA5, 0xE9)
C_WHITE  = RGBColor(0xFF, 0xFF, 0xFF)
C_GRAY   = RGBColor(0x94, 0xA3, 0xB8)
C_GREEN  = RGBColor(0x22, 0xC5, 0x5E)
C_RED    = RGBColor(0xEF, 0x44, 0x44)
C_YELLOW = RGBColor(0xFB, 0xBF, 0x24)
C_PURPLE = RGBColor(0xA8, 0x55, 0xF7)

prs = Presentation()
prs.slide_width  = W
prs.slide_height = H
BLANK = prs.slide_layouts[6]

# ── 工具函式 ──────────────────────────────────────────────

def add_rect(slide, l, t, w, h, fill=None, line=None, line_w=Pt(1.5), radius=False):
    shp = slide.shapes.add_shape(1, l, t, w, h)
    shp.line.fill.background()
    if fill:
        shp.fill.solid()
        shp.fill.fore_color.rgb = fill
    else:
        shp.fill.background()
    if line:
        shp.line.color.rgb = line
        shp.line.width = line_w
    else:
        shp.line.fill.background()
    return shp

def add_text(slide, text, l, t, w, h,
             size=Pt(18), bold=False, color=C_WHITE,
             align=PP_ALIGN.LEFT, wrap=True):
    txb = slide.shapes.add_textbox(l, t, w, h)
    txb.word_wrap = wrap
    tf  = txb.text_frame
    tf.word_wrap = wrap
    p   = tf.paragraphs[0]
    p.alignment = align
    run = p.add_run()
    run.text = text
    run.font.size  = size
    run.font.bold  = bold
    run.font.color.rgb = color
    return txb

def add_image(slide, path, l, t, w=None, h=None):
    if not os.path.exists(path):
        return
    if w and h:
        slide.shapes.add_picture(path, l, t, w, h)
    elif w:
        slide.shapes.add_picture(path, l, t, width=w)
    elif h:
        slide.shapes.add_picture(path, l, t, height=h)
    else:
        slide.shapes.add_picture(path, l, t)

def new_slide(bg=C_DARK):
    sl = prs.slides.add_slide(BLANK)
    add_rect(sl, 0, 0, W, H, fill=bg)
    return sl

def title_bar(slide, title, subtitle="", bar_color=C_BLUE):
    add_rect(slide, 0, 0, W, Inches(1.12), fill=bar_color)
    add_text(slide, title,
             Inches(0.4), Inches(0.14), Inches(11), Inches(0.62),
             size=Pt(26), bold=True, color=C_WHITE)
    if subtitle:
        add_text(slide, subtitle,
                 Inches(0.4), Inches(0.70), Inches(12.5), Inches(0.36),
                 size=Pt(13), color=RGBColor(0xBA, 0xD9, 0xF9))

def slide_num(slide, n, total=14):
    add_text(slide, f"{n} / {total}",
             Inches(12.5), Inches(7.1), Inches(0.8), Inches(0.35),
             size=Pt(11), color=C_GRAY, align=PP_ALIGN.RIGHT)

def badge(slide, text, l, t, w=Inches(2.2), h=Inches(0.42),
          bg=C_GREEN, fg=C_WHITE):
    add_rect(slide, l, t, w, h, fill=bg)
    add_text(slide, text, l, t, w, h,
             size=Pt(13), bold=True, color=fg, align=PP_ALIGN.CENTER)

TOTAL = 14

# ═══════════════════════════════════════════════════════════
# 投影片 1：封面
# ═══════════════════════════════════════════════════════════
sl = new_slide()
add_rect(sl, 0, 0, Inches(0.4), H, fill=C_BLUE)
add_rect(sl, Inches(0.4), Inches(2.55), Inches(12.93), Inches(0.06), fill=C_BLUE)

add_text(sl, "WBS 管理系統",
         Inches(0.7), Inches(1.55), Inches(12), Inches(1.1),
         size=Pt(54), bold=True, color=C_WHITE)
add_text(sl, "讓專案任務不再散落各處  一個平台，從分工到交付",
         Inches(0.7), Inches(2.75), Inches(11.5), Inches(0.6),
         size=Pt(20), color=C_ACCENT)

tags = [("Java 21",""), ("Spring Boot 3.4",""), ("Vue 3",""),
        ("WebSocket 即時協作",""), ("PostgreSQL","")]
x = Inches(0.7)
for tag, _ in tags:
    tw = Inches(len(tag) * 0.19 + 0.6)
    add_rect(sl, x, Inches(3.65), tw, Inches(0.42),
             fill=RGBColor(0x1E, 0x40, 0xAF))
    add_text(sl, tag, x, Inches(3.65), tw, Inches(0.42),
             size=Pt(13), color=C_WHITE, align=PP_ALIGN.CENTER)
    x += tw + Inches(0.15)

add_text(sl, "企業後台  ·  多人即時協作  ·  角色權限管理",
         Inches(0.7), Inches(6.7), Inches(10), Inches(0.45),
         size=Pt(13), color=C_GRAY)
slide_num(sl, 1)

# ═══════════════════════════════════════════════════════════
# 投影片 2：痛點（現況）
# ═══════════════════════════════════════════════════════════
sl = new_slide()
title_bar(sl, "您是否也遇到這些問題？", bar_color=C_RED)
slide_num(sl, 2)

pains = [
    ("📋  Excel 版本地獄",
     "N 個同事手上有 N 個不同版本，不知道哪一份才是最新的。"),
    ("🔁  反覆開會確認進度",
     "主管不知道哪個項目卡住了，只能排會議問人，浪費大量時間。"),
    ("👤  責任歸屬模糊",
     "任務沒有明確負責人，截止日到了才發現沒有人在做。"),
    ("📂  上線前文件倉促補齊",
     "SIT/UAT/PROD 各階段文件沒有固定規格，每次上線都要重新想一遍。"),
    ("🔒  跨科資料外洩風險",
     "同一份試算表，不該看的人也能看到所有科別的專案資訊。"),
]
for i, (title, desc) in enumerate(pains):
    row, col = divmod(i, 3)
    x = Inches(0.35 + col * 4.35)
    y = Inches(1.3 + row * 2.55)
    add_rect(sl, x, y, Inches(4.15), Inches(2.3),
             fill=RGBColor(0x3B, 0x10, 0x10))
    add_rect(sl, x, y, Inches(4.15), Inches(0.06), fill=C_RED)
    add_text(sl, title, x + Inches(0.15), y + Inches(0.18),
             Inches(3.9), Inches(0.55),
             size=Pt(14), bold=True, color=C_WHITE)
    add_text(sl, desc, x + Inches(0.15), y + Inches(0.75),
             Inches(3.9), Inches(1.35),
             size=Pt(12), color=C_GRAY)

# ═══════════════════════════════════════════════════════════
# 投影片 3：解方（Before → After）
# ═══════════════════════════════════════════════════════════
sl = new_slide()
title_bar(sl, "WBS 管理系統  ─  這樣解決", bar_color=C_GREEN)
slide_num(sl, 3)

befores = [
    "Excel 多版本並存，不知以誰為準",
    "進度靠開會問，資訊嚴重落後",
    "任務無負責人，截止日到才發現",
    "上線文件每次重頭想，格式不統一",
    "試算表無權限控管，資料外洩風險",
]
afters = [
    "單一平台集中管理，即時同步所有異動",
    "WBS 節點標示狀態與負責人，主管秒懂",
    "每個 L3 任務綁定負責人 + 截止日",
    "套用模板一鍵產生 SIT/UAT/PROD 架構",
    "角色與部門隔離，跨科資料看不到",
]

add_rect(sl, Inches(0.35), Inches(1.2), Inches(5.85), Inches(5.8),
         fill=RGBColor(0x2A, 0x10, 0x10))
add_rect(sl, Inches(7.15), Inches(1.2), Inches(5.85), Inches(5.8),
         fill=RGBColor(0x0A, 0x2A, 0x14))
add_text(sl, "😓  現況痛點",
         Inches(0.35), Inches(1.2), Inches(5.85), Inches(0.55),
         size=Pt(16), bold=True, color=C_RED, align=PP_ALIGN.CENTER)
add_text(sl, "✅  導入後",
         Inches(7.15), Inches(1.2), Inches(5.85), Inches(0.55),
         size=Pt(16), bold=True, color=C_GREEN, align=PP_ALIGN.CENTER)

# 中間箭頭
add_text(sl, "→",
         Inches(6.1), Inches(3.7), Inches(1.15), Inches(0.8),
         size=Pt(42), bold=True, color=C_BLUE, align=PP_ALIGN.CENTER)

for i, (b, a) in enumerate(zip(befores, afters)):
    y = Inches(1.85 + i * 0.88)
    add_rect(sl, Inches(0.5), y, Inches(5.55), Inches(0.75),
             fill=RGBColor(0x3B, 0x10, 0x10))
    add_text(sl, b, Inches(0.65), y + Inches(0.12),
             Inches(5.3), Inches(0.52),
             size=Pt(12), color=RGBColor(0xFC, 0xA5, 0xA5))
    add_rect(sl, Inches(7.3), y, Inches(5.55), Inches(0.75),
             fill=RGBColor(0x0A, 0x2A, 0x1A))
    add_text(sl, a, Inches(7.45), y + Inches(0.12),
             Inches(5.3), Inches(0.52),
             size=Pt(12), color=RGBColor(0x86, 0xEF, 0xAC))

# ═══════════════════════════════════════════════════════════
# 投影片 4：使用場景
# ═══════════════════════════════════════════════════════════
sl = new_slide()
title_bar(sl, "適用場景", "適合任何有 SIT → UAT → PROD 發布流程的 IT 團隊")
slide_num(sl, 4)

scenarios = [
    ("💡", "新功能開發",
     "從需求討論、功能設計、程式開發到三環境上線，全程追蹤。\n套用「功能新增」模板，30 秒建好完整 WBS。"),
    ("🚀", "全新系統建置",
     "系統設計、資料庫建置、環境申請一次展開。\n套用「專案開發」模板，56 個節點預設到位。"),
    ("📦", "系統下線（EOS）",
     "資料備份、影響分析、主機拆除流程標準化。\n套用「EOS 系統下線」模板，避免漏項。"),
    ("👥", "跨科協作",
     "部長可唯讀查閱全部門進度；科與科之間資料隔離，互不干擾。"),
    ("📊", "定期進度回報",
     "主管直接打開平台，各節點狀態一目了然，無需等人彙整報告。"),
    ("📁", "歷史存查",
     "專案完成後歸檔，保留完整 WBS 記錄，可隨時匯出 CSV / XLSX 供稽核。"),
]
for i, (icon, title, desc) in enumerate(scenarios):
    row, col = divmod(i, 3)
    x = Inches(0.35 + col * 4.35)
    y = Inches(1.3 + row * 2.8)
    add_rect(sl, x, y, Inches(4.15), Inches(2.6),
             fill=RGBColor(0x1E, 0x30, 0x4E))
    add_rect(sl, x, y, Inches(4.15), Inches(0.06), fill=C_BLUE)
    add_text(sl, f"{icon}  {title}", x + Inches(0.15), y + Inches(0.18),
             Inches(3.9), Inches(0.52),
             size=Pt(15), bold=True, color=C_WHITE)
    add_text(sl, desc, x + Inches(0.15), y + Inches(0.76),
             Inches(3.9), Inches(1.65),
             size=Pt(11.5), color=C_GRAY)

# ═══════════════════════════════════════════════════════════
# 投影片 5：核心價值（數字說話）
# ═══════════════════════════════════════════════════════════
sl = new_slide()
title_bar(sl, "核心價值", "從分工到交付，每個環節都看得到")
slide_num(sl, 5)

metrics = [
    ("4",   "套系統模板",  "功能新增 / 專案開發\nEOS 下線 / 系統開發",  C_BLUE),
    ("48",  "個快速子項",  "分 8 類，涵蓋所有\n標準 IT 工作項目",       C_ACCENT),
    ("3",   "層 WBS 結構", "L1 大項 → L2 工作包\n→ L3 任務（含備註）",  C_GREEN),
    ("即時","同步協作",    "WebSocket 廣播，\n所有人看到同一份資料",     C_PURPLE),
]
for i, (num, unit, desc, col) in enumerate(metrics):
    x = Inches(0.4 + i * 3.25)
    add_rect(sl, x, Inches(1.25), Inches(3.0), Inches(5.2),
             fill=RGBColor(0x1A, 0x29, 0x48))
    add_rect(sl, x, Inches(1.25), Inches(3.0), Inches(0.08), fill=col)
    add_text(sl, num,
             x, Inches(1.6), Inches(3.0), Inches(1.45),
             size=Pt(64), bold=True, color=col, align=PP_ALIGN.CENTER)
    add_text(sl, unit,
             x, Inches(3.15), Inches(3.0), Inches(0.55),
             size=Pt(18), bold=True, color=C_WHITE, align=PP_ALIGN.CENTER)
    add_text(sl, desc,
             x + Inches(0.1), Inches(3.82), Inches(2.8), Inches(1.35),
             size=Pt(13), color=C_GRAY, align=PP_ALIGN.CENTER)

# ═══════════════════════════════════════════════════════════
# 投影片 6：WBS 三層結構說明
# ═══════════════════════════════════════════════════════════
sl = new_slide()
title_bar(sl, "WBS 三層結構", "固定深度，清晰分工")
slide_num(sl, 6)

# 左側結構圖
levels = [
    (Inches(0.4),  Inches(1.3),  Inches(3.5), Inches(0.75), "L1  大項",    "SIT / UAT / PROD",    C_BLUE,   Pt(15)),
    (Inches(1.3),  Inches(2.35), Inches(3.5), Inches(0.75), "L2  工作包",  "環境建置 / 功能開發",  C_ACCENT, Pt(14)),
    (Inches(2.2),  Inches(3.4),  Inches(3.5), Inches(0.75), "L3  任務",    "程式開發 / 功能測試",  C_GREEN,  Pt(14)),
]
for lx, ly, lw, lh, label, example, col, fs in levels:
    add_rect(sl, lx, ly, lw, lh, fill=col)
    add_text(sl, label,   lx + Inches(0.12), ly + Inches(0.06), lw, Inches(0.36),
             size=fs, bold=True, color=C_WHITE)
    add_text(sl, example, lx + Inches(0.12), ly + Inches(0.4),  lw, Inches(0.32),
             size=Pt(11), color=C_WHITE)

# 右側欄位說明
fields = [
    ("L1 / L2 欄位", ["狀態（未開始／進行中／已完成）", "負責人", "開始日 / 結束日"], C_BLUE),
    ("L3 額外欄位",  ["備註（文字輸入框）"],                                          C_GREEN),
]
for i, (ftitle, items, col) in enumerate(fields):
    x = Inches(6.3)
    y = Inches(1.4 + i * 2.5)
    add_rect(sl, x, y, Inches(6.65), Inches(0.48), fill=col)
    add_text(sl, ftitle, x + Inches(0.15), y + Inches(0.07),
             Inches(6.4), Inches(0.38),
             size=Pt(15), bold=True, color=C_WHITE)
    for j, item in enumerate(items):
        add_text(sl, f"•  {item}",
                 x + Inches(0.3), y + Inches(0.62 + j * 0.55),
                 Inches(6.2), Inches(0.5),
                 size=Pt(13), color=C_WHITE)

add_text(sl,
         "最大深度 3 層，結構簡單易懂；拖曳快速子項即可新增節點，無需手動輸入。",
         Inches(0.4), Inches(6.8), Inches(12.6), Inches(0.5),
         size=Pt(13), color=C_GRAY)

# ═══════════════════════════════════════════════════════════
# 投影片 7：角色與權限（含部門隔離說明）
# ═══════════════════════════════════════════════════════════
sl = new_slide()
title_bar(sl, "角色與權限", "四層組織架構，精細存取控制")
slide_num(sl, 7)

headers  = ["角色",              "建立專案", "編輯 WBS", "管理成員", "跨科查閱"]
col_w    = [Inches(3.0), Inches(1.9), Inches(1.9), Inches(1.9), Inches(1.9)]
col_x    = [Inches(0.4)]
for w in col_w[:-1]:
    col_x.append(col_x[-1] + w)

rows = [
    ("DIRECTOR  部長",      "✅", "限自建", "限自建", "✅ 唯讀"),
    ("SECTION_CHIEF  科長", "✅", "✅ 本科", "✅ 本科", "❌"),
    ("PROJECT_LEADER",      "✅", "✅ 加入", "✅ 自建", "❌"),
    ("PROJECT_MEMBER  成員","❌", "✅ 加入", "❌",      "❌"),
]
for j, (hdr, x, w) in enumerate(zip(headers, col_x, col_w)):
    add_rect(sl, x, Inches(1.3), w - Inches(0.05), Inches(0.5), fill=C_BLUE)
    add_text(sl, hdr, x, Inches(1.3), w - Inches(0.05), Inches(0.5),
             size=Pt(14), bold=True, color=C_WHITE, align=PP_ALIGN.CENTER)
for i, row in enumerate(rows):
    y  = Inches(1.82 + i * 0.82)
    bg = RGBColor(0x1E, 0x38, 0x5E) if i % 2 == 0 else RGBColor(0x17, 0x2A, 0x48)
    for j, (val, x, w) in enumerate(zip(row, col_x, col_w)):
        add_rect(sl, x, y, w - Inches(0.05), Inches(0.75), fill=bg)
        clr = (C_WHITE   if j == 0 else
               C_GREEN   if "✅" in val else
               C_RED     if "❌" in val else
               C_YELLOW)
        add_text(sl, val, x, y, w - Inches(0.05), Inches(0.75),
                 size=Pt(13), bold=(j==0), color=clr, align=PP_ALIGN.CENTER)

# 部門隔離說明
add_rect(sl, Inches(0.4), Inches(5.18), Inches(12.6), Inches(1.72),
         fill=RGBColor(0x1E, 0x2A, 0x1A), line=C_GREEN, line_w=Pt(1))
add_text(sl, "🔒  部門隔離保護",
         Inches(0.6), Inches(5.3), Inches(12.2), Inches(0.45),
         size=Pt(14), bold=True, color=C_GREEN)
add_text(sl,
         "科長與 Leader 只能看到本科專案；部長可唯讀查閱下屬科所有專案，但不得編輯（自建除外）。\n"
         "歸檔後的專案對全員強制唯讀，保護已結案資料不被誤改。",
         Inches(0.6), Inches(5.78), Inches(12.2), Inches(0.9),
         size=Pt(12), color=C_GRAY)

# ═══════════════════════════════════════════════════════════
# 投影片 8：模板系統
# ═══════════════════════════════════════════════════════════
sl = new_slide()
title_bar(sl, "模板系統  —  新專案 30 秒建好 WBS")
slide_num(sl, 8)

templates = [
    ("系統開發標準", "SIT→UAT→PROD\n31 個節點",   C_BLUE),
    ("功能新增",     "SIT→UAT→PROD\n33 個節點",   C_ACCENT),
    ("EOS 系統下線", "SIT→UAT→PROD\n37 個節點",   C_PURPLE),
    ("專案開發",     "SIT→UAT→PROD\n56 個節點",   C_GREEN),
]
for i, (name, detail, col) in enumerate(templates):
    x = Inches(0.4 + i * 3.2)
    add_rect(sl, x, Inches(1.3), Inches(3.0), Inches(2.8),
             fill=RGBColor(0x1A, 0x29, 0x48))
    add_rect(sl, x, Inches(1.3), Inches(3.0), Inches(0.1), fill=col)
    add_text(sl, name,
             x + Inches(0.1), Inches(1.55), Inches(2.8), Inches(0.55),
             size=Pt(16), bold=True, color=C_WHITE, align=PP_ALIGN.CENTER)
    add_text(sl, detail,
             x + Inches(0.1), Inches(2.2), Inches(2.8), Inches(0.8),
             size=Pt(13), color=col, align=PP_ALIGN.CENTER)
    badge(sl, "系統模板", x + Inches(0.6), Inches(3.0),
          w=Inches(1.8), h=Inches(0.38), bg=col)

add_rect(sl, Inches(0.4), Inches(4.35), Inches(12.6), Inches(0.06), fill=C_BLUE)
add_text(sl, "科別模板（自訂）",
         Inches(0.4), Inches(4.55), Inches(12.6), Inches(0.45),
         size=Pt(15), bold=True, color=C_WHITE)

custom_points = [
    "科長 / Leader 可將現有 WBS「另存為模板」，保留本科最佳實踐",
    "本科模板僅本科成員可見，跨科不互見",
    "可基於系統模板修改後另存，快速客製化",
]
for i, pt in enumerate(custom_points):
    add_text(sl, f"•  {pt}",
             Inches(0.6), Inches(5.1 + i * 0.55), Inches(12.2), Inches(0.5),
             size=Pt(13), color=C_GRAY)

# ═══════════════════════════════════════════════════════════
# 投影片 9：快速子項
# ═══════════════════════════════════════════════════════════
sl = new_slide()
title_bar(sl, "快速子項  —  拖曳即新增節點")
slide_num(sl, 9)

cats = [
    ("需求",    5,  C_BLUE),
    ("環境建置",11, C_ACCENT),
    ("開發",    7,  C_PURPLE),
    ("測試",    3,  C_GREEN),
    ("文件",    12, C_YELLOW),
    ("上線",    4,  RGBColor(0xF9, 0x73, 0x16)),
    ("資料作業",4,  RGBColor(0xEC, 0x48, 0x99)),
    ("系統下線",2,  C_RED),
]
for i, (cat, cnt, col) in enumerate(cats):
    row, c = divmod(i, 4)
    x = Inches(0.4 + c * 3.25)
    y = Inches(1.3 + row * 2.0)
    add_rect(sl, x, y, Inches(3.05), Inches(1.75),
             fill=RGBColor(0x1A, 0x29, 0x48))
    add_rect(sl, x, y, Inches(3.05), Inches(0.08), fill=col)
    add_text(sl, cat, x + Inches(0.12), y + Inches(0.18),
             Inches(2.8), Inches(0.5),
             size=Pt(16), bold=True, color=col)
    add_text(sl, f"{cnt} 個項目",
             x + Inches(0.12), y + Inches(0.7),
             Inches(2.8), Inches(0.45),
             size=Pt(13), color=C_GRAY)

add_text(sl,
         "•  拖曳至節點 → 新增為子節點（最深 L3）     •  拖曳至空白區 → 新增 L1 根節點\n"
         "•  科長 / Leader 可管理快速子項清單           •  全域子項（所有人可見）+ 科別子項（本科專屬）",
         Inches(0.4), Inches(5.55), Inches(12.6), Inches(0.9),
         size=Pt(13), color=C_GRAY)

# ═══════════════════════════════════════════════════════════
# 投影片 10：測試截圖 — 登入 + 專案列表
# ═══════════════════════════════════════════════════════════
sl = new_slide()
title_bar(sl, "功能驗證 ①  —  登入 / 專案列表")
slide_num(sl, 10)

add_image(sl, os.path.join(SS, "ppt-login.png"),
          Inches(0.3), Inches(1.2), w=Inches(5.5))
add_image(sl, os.path.join(SS, "ppt-project-list.png"),
          Inches(6.1), Inches(1.2), w=Inches(7.0))

badge(sl, "✅  登入成功",           Inches(0.3), Inches(6.45), w=Inches(2.2), bg=C_GREEN)
badge(sl, "✅  角色過濾專案列表",   Inches(6.1), Inches(6.45), w=Inches(3.2), bg=C_GREEN)

# ═══════════════════════════════════════════════════════════
# 投影片 11：測試截圖 — WBS 編輯器
# ═══════════════════════════════════════════════════════════
sl = new_slide()
title_bar(sl, "功能驗證 ②  —  WBS 編輯器（功能新增模板，33 節點）")
slide_num(sl, 11)

add_image(sl, os.path.join(SS, "ppt-wbs-editor.png"),
          Inches(0.3), Inches(1.15), w=Inches(12.8))

badge(sl, "✅  SIT/UAT/PROD 三階段", Inches(0.3),  Inches(6.55), w=Inches(3.2), bg=C_GREEN)
badge(sl, "✅  L3 備註欄位",         Inches(3.75), Inches(6.55), w=Inches(2.2), bg=C_GREEN)
badge(sl, "✅  快速子項面板（48 項）",Inches(6.2),  Inches(6.55), w=Inches(3.6), bg=C_GREEN)

# ═══════════════════════════════════════════════════════════
# 投影片 12：測試截圖 — 模板管理 + 快速子項管理
# ═══════════════════════════════════════════════════════════
sl = new_slide()
title_bar(sl, "功能驗證 ③  —  主模板管理 / 快速子項管理")
slide_num(sl, 12)

add_image(sl, os.path.join(SS, "ppt-templates.png"),
          Inches(0.3), Inches(1.2), w=Inches(6.3))
add_image(sl, os.path.join(SS, "ppt-quick-items.png"),
          Inches(6.8), Inches(1.2), w=Inches(6.3))

badge(sl, "✅  4 套系統模板",         Inches(0.3), Inches(6.45), w=Inches(2.6), bg=C_GREEN)
badge(sl, "✅  48 個快速子項（8 類）", Inches(6.8), Inches(6.45), w=Inches(3.5), bg=C_GREEN)

# ═══════════════════════════════════════════════════════════
# 投影片 13：技術架構（精簡）
# ═══════════════════════════════════════════════════════════
sl = new_slide()
title_bar(sl, "技術架構")
slide_num(sl, 13)

layers = [
    ("瀏覽器層",  "Thymeleaf SSR  +  Vue 3 CDN（離線靜態，無建置工具）",    C_BLUE),
    ("通訊層",    "REST API（X-CSRF-TOKEN）  +  SockJS / STOMP（即時廣播）", C_ACCENT),
    ("應用層",    "Spring Boot 3.4  ·  Spring MVC  ·  Spring WebSocket  ·  Spring Security 6", C_PURPLE),
    ("資料層",    "PostgreSQL 16  ·  Spring Session JDBC（HttpOnly Cookie，無 JWT）",           C_GREEN),
]
for i, (lbl, desc, col) in enumerate(layers):
    y = Inches(1.4 + i * 1.3)
    add_rect(sl, Inches(0.4), y, Inches(12.5), Inches(1.0),
             fill=RGBColor(0x1A, 0x29, 0x48))
    add_rect(sl, Inches(0.4), y, Inches(2.0), Inches(1.0), fill=col)
    add_text(sl, lbl,
             Inches(0.4), y + Inches(0.24), Inches(2.0), Inches(0.5),
             size=Pt(14), bold=True, color=C_WHITE, align=PP_ALIGN.CENTER)
    add_text(sl, desc,
             Inches(2.55), y + Inches(0.24), Inches(10.2), Inches(0.5),
             size=Pt(14), color=C_WHITE)
    if i < 3:
        add_text(sl, "▼",
                 Inches(6.5), y + Inches(1.0), Inches(0.5), Inches(0.28),
                 size=Pt(13), color=C_GRAY, align=PP_ALIGN.CENTER)

# ═══════════════════════════════════════════════════════════
# 投影片 14：快速啟動 + 結語
# ═══════════════════════════════════════════════════════════
sl = new_slide()
title_bar(sl, "立即開始使用")
slide_num(sl, 14)

cmd_lines = [
    ("# 1. 複製環境設定",             False),
    ("cp .env.example .env",          True),
    ("# 2. 啟動 PostgreSQL",          False),
    ("docker compose up -d",          True),
    ("# 3. 編譯並啟動",               False),
    ("mvn clean install -DskipTests", True),
    ("mvn spring-boot:run",           True),
    ("# 開啟瀏覽器",                  False),
    ("http://localhost:8080",         True),
]
add_rect(sl, Inches(0.4), Inches(1.3), Inches(7.4), Inches(5.7),
         fill=RGBColor(0x05, 0x0A, 0x14))
y_off = Inches(1.5)
for line, is_cmd in cmd_lines:
    clr = C_WHITE if is_cmd else C_GREEN
    add_text(sl, line or " ",
             Inches(0.6), y_off, Inches(7.0), Inches(0.42),
             size=Pt(13), color=clr)
    y_off += Inches(0.48 if is_cmd else 0.42)

# 右側：測試帳號 + 結語
add_text(sl, "測試帳號（密碼皆為 test1234）",
         Inches(8.2), Inches(1.3), Inches(4.8), Inches(0.45),
         size=Pt(14), bold=True, color=C_WHITE)
accounts = [
    ("director@company.com",  "DIRECTOR 部長",   C_YELLOW),
    ("chief@infotech.com",    "SECTION_CHIEF 科長", C_ACCENT),
    ("leader@infotech.com",   "PROJECT_LEADER",  C_GREEN),
    ("member1@infotech.com",  "PROJECT_MEMBER",  C_GRAY),
]
for i, (email, role, col) in enumerate(accounts):
    y = Inches(1.88 + i * 1.0)
    add_rect(sl, Inches(8.2), y, Inches(4.8), Inches(0.88),
             fill=RGBColor(0x1A, 0x29, 0x48))
    add_text(sl, email, Inches(8.35), y + Inches(0.08),
             Inches(4.5), Inches(0.38),
             size=Pt(12), color=col)
    add_text(sl, role, Inches(8.35), y + Inches(0.46),
             Inches(4.5), Inches(0.36),
             size=Pt(11), color=C_GRAY)

add_rect(sl, Inches(8.2), Inches(6.1), Inches(4.8), Inches(0.82),
         fill=RGBColor(0x1E, 0x40, 0xAF))
add_text(sl, "一個平台解決所有 WBS 管理需求",
         Inches(8.2), Inches(6.1), Inches(4.8), Inches(0.82),
         size=Pt(15), bold=True, color=C_WHITE, align=PP_ALIGN.CENTER)

prs.save(OUT)
print(f"✅  已輸出：{OUT}  （共 {TOTAL} 張）")
