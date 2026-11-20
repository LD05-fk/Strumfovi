#!/usr/bin/env bash
# Izrada PDF-a dokumentacije za predaju (DOC-22).
#
# Spaja Markdown iz docs/ (Home, poglavlja i Dodatak A) u jedan HTML s naslovnicom
# i sadržajem, a Chromium ga ispisuje u PDF u docs/deliverables/.
#
# Potrebno: python3 s paketom `markdown` i Chromium (ili Google Chrome).
#
# Uporaba (iz korijena repozitorija):
#   scripts/build-docs-pdf.sh            # v1, prva predaja
#   VERSION=v2 CYCLE=2 scripts/build-docs-pdf.sh
#
# Za prvu predaju dokumentacija se traži do §4.3, bez §4.2, pa se iz poglavlja 4
# uzimaju samo uvod, §4.1 i §4.3. Za v2 promijenite popis CHAPTERS u Python dijelu.
set -euo pipefail

cd "$(dirname "$0")/.."

VERSION="${VERSION:-v1}"
CYCLE="${CYCLE:-1}"
DATE_HR="${DATE_HR:-20. 11. 2026.}"
OUT="docs/deliverables/PROINZ26_G06.4_ArtConnect_${VERSION}.pdf"
BROWSER="$(command -v chromium || command -v google-chrome || command -v chromium-browser || true)"
if [ -z "$BROWSER" ]; then
    echo "Chromium ili Google Chrome nije pronađen." >&2
    exit 1
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
HTML="$TMP/dokumentacija.html"

python3 - "$HTML" "$VERSION" "$CYCLE" "$DATE_HR" <<'PY'
import html, os, re, sys
import markdown

out, version, cycle, date_hr = sys.argv[1:5]
root = os.getcwd()
docs = os.path.join(root, "docs")
repo_url = "https://github.com/LD05-fk/Projekt-PROGI/blob/master/"

def chapter4_first_submission(text):
    """Uvod, §4.1 i §4.3 (bez §4.2 i dijelova iza §4.3)."""
    intro = text[:text.index("## 4.1 ")]
    intro = re.sub(r"\nZa svaki UML dijagram čuvajte[^\n]*\n", "\n", intro)
    intro = re.sub(r"\n> \*\*Stanje:\*\*[^\n]*\n", "\n", intro)
    s41 = text[text.index("## 4.1 "):text.index("## 4.2 ")]
    s43 = text[text.index("## 4.3 "):text.index("## 4.4 ")]
    return intro + s41 + s43

# (datoteka, funkcija koja bira dio teksta ili None za cijelu datoteku)
CHAPTERS = [
    ("Home.md", None),
    ("1-Projekt.md", None),
    ("2-Analiza-zahtjeva.md", None),
    ("3-Obrasci-uporabe.md", None),
    ("4-Arhitektura-i-oblikovanje.md", chapter4_first_submission),
    ("A-Pregled-aktivnosti-grupe.md", None),
]

def fix_links(text, src_dir):
    """Slike postaju apsolutne lokalne putanje, a poveznice na datoteke vode na GitHub."""
    def repl(m):
        bang, label, target = m.group(1), m.group(2), m.group(3)
        if re.match(r"^(https?:|mailto:|#)", target):
            return m.group(0)
        path, _, anchor = target.partition("#")
        full = os.path.normpath(os.path.join(src_dir, path))
        if bang:
            return f"{bang}[{label}](file://{full})"
        rel = os.path.relpath(full, root)
        url = repo_url + rel + (f"#{anchor}" if anchor else "")
        return f"[{label}]({url})"
    return re.sub(r"(!?)\[([^\]]*)\]\(([^)\s]+)\)", repl, text)

def strip_details(text):
    return re.sub(r"<details>.*?</details>", "", text, flags=re.S)

md = markdown.Markdown(extensions=["tables", "fenced_code", "toc", "sane_lists"],
                       extension_configs={"toc": {"permalink": False}})
parts, toc = [], []
for i, (name, select) in enumerate(CHAPTERS):
    path = os.path.join(docs, name)
    text = open(path, encoding="utf-8").read()
    if select:
        text = select(text)
    text = strip_details(text)
    text = fix_links(text, os.path.dirname(path))
    md.reset()
    body = md.convert(text)
    prefix = f"c{i}-"
    body = re.sub(r'id="([^"]+)"', lambda m: f'id="{prefix}{m.group(1)}"', body)
    for tok in md.toc_tokens:
        toc.append((1, prefix + tok["id"], tok["name"]))
        for child in tok.get("children", []):
            toc.append((2, prefix + child["id"], child["name"]))
    parts.append(f'<section class="chapter">{body}</section>')

members = [
    ("Luka Dakić", "voditelj tima"),
    ("Luka Dimjašević", ""),
    ("Luka Stare", ""),
    ("Bepa Tabulov-Štrelov", ""),
    ("Vedran Novaković", ""),
    ("Nikola Mustač", ""),
    ("Bruno Kosmačin", ""),
]
member_rows = "".join(
    f"<li>{html.escape(n)}{' (' + r + ')' if r else ''}</li>" for n, r in members)

cover = f"""
<section class="cover">
  <p class="course">Programsko inženjerstvo · Fakultet elektrotehnike i računarstva, Sveučilište u Zagrebu · ak. god. 2026./2027.</p>
  <h1 class="title">ArtConnect</h1>
  <p class="subtitle">Projektna dokumentacija – {cycle}. ciklus ({version})</p>
  <p class="meta">Grupa G06 · Tim 4</p>
  <div class="members"><p><strong>Članovi tima</strong></p><ul>{member_rows}</ul></div>
  <p class="meta">Nastavnik: Vlado Sruk · Asistentica: Eda Jovičić</p>
  <p class="meta">Javna adresa: https://artconnect-fer.duckdns.org</p>
  <p class="meta">Repozitorij: https://github.com/LD05-fk/Projekt-PROGI</p>
  <p class="date">{date_hr}</p>
</section>
"""

toc_html = "".join(
    f'<li class="l{lvl}"><a href="#{aid}">{html.escape(name)}</a></li>' for lvl, aid, name in toc)
toc_section = f'<section class="toc"><h1>Sadržaj</h1><ul>{toc_html}</ul></section>'

css = """
@page { size: A4; margin: 18mm 16mm 20mm 16mm;
        @bottom-center { content: counter(page); font: 9pt Arial, sans-serif; color: #555; } }
@page :first { @bottom-center { content: none; } }
body { font: 10.5pt/1.45 Arial, Helvetica, sans-serif; color: #1c1917; }
h1 { font-size: 20pt; margin: 0 0 10pt; }
h2 { font-size: 14pt; margin: 16pt 0 6pt; break-after: avoid; }
h3 { font-size: 12pt; margin: 12pt 0 4pt; break-after: avoid; }
h4 { font-size: 11pt; margin: 10pt 0 4pt; break-after: avoid; }
table { border-collapse: collapse; width: 100%; margin: 6pt 0 10pt; font-size: 9pt; }
th, td { border: 1px solid #c8c8c8; padding: 3pt 5pt; vertical-align: top; text-align: left; }
th { background: #eef2f7; }
tr { break-inside: avoid; }
code { font: 8.8pt Consolas, "DejaVu Sans Mono", monospace; background: #f3f3f3; padding: 0 2pt; }
pre { background: #f6f6f6; padding: 6pt; white-space: pre-wrap; font-size: 8.5pt; }
img { max-width: 100%; height: auto; display: block; margin: 6pt auto; break-inside: avoid; }
blockquote { border-left: 3px solid #c8c8c8; margin: 6pt 0; padding: 2pt 8pt; color: #444; }
a { color: #3730a3; text-decoration: none; }
.chapter { break-before: page; }
.cover { height: 250mm; display: flex; flex-direction: column; justify-content: center; text-align: center; }
.cover .course { font-size: 10pt; color: #555; }
.cover .title { font-size: 34pt; margin: 30pt 0 6pt; }
.cover .subtitle { font-size: 15pt; margin: 0 0 24pt; }
.cover .members ul { list-style: none; padding: 0; margin: 4pt 0 18pt; }
.cover .meta { margin: 3pt 0; }
.cover .date { margin-top: 30pt; font-size: 12pt; }
.toc { break-before: page; }
.toc ul { list-style: none; padding: 0; }
.toc li.l1 { margin-top: 6pt; font-weight: bold; }
.toc li.l2 { margin-left: 14pt; }
"""

doc = f"""<!doctype html><html lang="hr"><head><meta charset="utf-8">
<title>ArtConnect – dokumentacija {version}</title><style>{css}</style></head>
<body>{cover}{toc_section}{''.join(parts)}</body></html>"""
open(out, "w", encoding="utf-8").write(doc)
PY

"$BROWSER" --headless=new --disable-gpu --no-sandbox --allow-file-access-from-files \
    --no-pdf-header-footer --print-to-pdf="$OUT" "file://$HTML" >/dev/null 2>&1

echo "PDF: $OUT"
