"""SANSARA logo source generator: wreath of leafy sprigs (clockwise), inner ring, dot, wordmark.
Outputs layered SVGs (wreath / center / full) in several color themes + PNG renders."""
import math, random, os, sys
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
import cairosvg

OUT = sys.argv[1] if len(sys.argv) > 1 else 'out'
os.makedirs(OUT, exist_ok=True)
W = 1024; C = W / 2
R_IN = 270                     # inner ring radius
R_W = R_IN * 1.29         # wreath stem radius (from original proportions)
FONT_DIR = '/usr/share/fonts/truetype/google-fonts/'

THEMES = {
    # name: leaf palette (dark→light), ghost leaf, stem, ring, text, red S, dot, background (for preview)
    'light': dict(leaves=['#1c1c1c', '#2a2a2a', '#3a3a3a', '#4a4a4a', '#5c5c5c'], ghost='#9c9c9c', stem='#2a2a2a', ring='#3a3a3a', text='#1d1d1d', red='#E0222C', dot='#3d3d3d', sub='#2b2b2b', bg='#F4F4F2'),
    'dark':  dict(leaves=['#F4F4F2', '#E6E6E3', '#D6D6D2', '#C6C6C1', '#B4B4AE'], ghost='#7d7d79', stem='#E6E6E3', ring='#D6D6D2', text='#F4F4F2', red='#E8323A', dot='#D6D6D2', sub='#CFCFCB', bg='#0A0A09'),
    'gold':  dict(leaves=['#E7C59C', '#D8B07C', '#C9A06B', '#B88F5C', '#A67E4F'], ghost='#6E5A40', stem='#D8B07C', ring='#D8B07C', text='#E7C59C', red='#E8323A', dot='#D8B07C', sub='#D8B07C', bg='#0A0A09'),
}


def leaf_path(L, w, bend, teeth=0, rng=None):
    """Leaf outline in local coords: base (0,0) → tip (L, bend). Optional teeth pointing to the tip."""
    n = 36
    up, lo = [], []
    ph = rng.uniform(0, 1) if rng else 0
    for i in range(n + 1):
        t = i / n
        hw = w * (math.sin(math.pi * min(1, t ** 0.85)) ** 0.85)
        if teeth and 0.2 < t < 0.92:
            u = (t * teeth + ph) % 1.0
            hw *= 1 + 0.22 * u ** 1.6
        cy = bend * t * t
        x = L * t
        up.append((x, cy - hw)); lo.append((x, cy + hw * 0.88))
    pts = up + lo[::-1]
    return 'M ' + ' L '.join(f'{x:.1f},{y:.1f}' for x, y in pts) + ' Z'


def add_leaf(out, rng, t, x0, y0, ang, L, side, ghost_p=0.18):
    w = L * rng.uniform(0.18, 0.26)
    bend = side * L * rng.uniform(0.04, 0.14)
    ghost = rng.random() < ghost_p
    ci = rng.randrange(len(t['leaves']))
    col = t['ghost'] if ghost else f"url(#g{ci})"
    op = rng.uniform(0.4, 0.6) if ghost else rng.uniform(0.82, 1.0)
    px, py = x0 + 2.5 * math.cos(ang), y0 + 2.5 * math.sin(ang)
    tr = f'translate({px:.1f},{py:.1f}) rotate({math.degrees(ang):.2f})'
    el = f'<path d="{leaf_path(L, w, bend, rng.choice([0, 3, 4, 5]), rng)}" transform="{tr}" fill="{col}" fill-opacity="{op:.2f}"/>'
    (out['back'] if ghost else out['front']).append(el)
    if not ghost and L > 38:
        out['front'].append(f'<path d="M 2,0 Q {0.5*L:.1f},{bend*0.3:.1f} {0.82*L:.1f},{bend*0.7:.1f}" transform="{tr}" fill="none" stroke="{t["bg"]}" stroke-opacity="0.2" stroke-width="1"/>')
    # short petiole stroke
    out['stems'].append(f'<path d="M {x0:.1f},{y0:.1f} L {px:.1f},{py:.1f}" stroke="{t["stem"]}" stroke-width="1.6" stroke-linecap="round"/>')


def branch(out, rng, t, pts, nleaves, lmax, width):
    d = 'M ' + ' L '.join(f'{x:.1f},{y:.1f}' for x, y in pts)
    out['stems'].append(f'<path d="{d}" fill="none" stroke="{t["stem"]}" stroke-width="{width}" stroke-linecap="round" stroke-linejoin="round"/>')
    steps = len(pts) - 1
    for j in range(nleaves + 1):
        terminal = j == nleaves
        s = 0.1 + 0.85 * j / nleaves if not terminal else 1.0
        i = min(steps - 1, int(s * steps))
        (x0, y0), (x1, y1) = pts[i], pts[i + 1]
        if terminal: x0, y0 = x1, y1
        tang = math.atan2(y1 - y0, x1 - x0) if not terminal else math.atan2(pts[-1][1] - pts[-2][1], pts[-1][0] - pts[-2][0])
        side = -1 if j % 2 == 0 else 1          # -1 = outward
        ang = tang if terminal else tang + side * math.radians(rng.uniform(28, 55))
        size = (1.0 - 0.4 * s) * (0.6 if side == 1 and not terminal else 1.0)
        add_leaf(out, rng, t, x0, y0, ang, lmax * size * rng.uniform(0.8, 1.15), side)


def build_wreath(rng, theme):
    t = THEMES[theme]
    out = {'back': [], 'stems': [], 'front': []}
    N = 8
    for k in range(N):
        a0 = math.radians(-100 + k * 360 / N + rng.uniform(-5, 5))
        span = math.radians(rng.uniform(48, 56))
        bow = rng.uniform(4, 16) * rng.choice([-1, 1])
        r0 = R_W + rng.uniform(-10, 10)
        steps = 60
        pts = []
        for i in range(steps + 1):
            s = i / steps
            a = a0 + span * s
            r = r0 + bow * math.sin(math.pi * s) + 10 * s   # tip drifts a little outward
            pts.append((C + r * math.cos(a), C + r * math.sin(a)))
        branch(out, rng, t, pts, rng.randint(8, 10), 92, 2.2)
        # one or two side twigs leaning outward
        for _ in range(rng.choice([1, 2])):
            s0 = rng.uniform(0.25, 0.6)
            i0 = int(s0 * steps)
            (x0, y0), (x1, y1) = pts[i0], pts[i0 + 1]
            tang = math.atan2(y1 - y0, x1 - x0) - math.radians(rng.uniform(25, 40))
            ln = rng.uniform(46, 66)
            tw = [(x0 + ln * q * math.cos(tang + 0.25 * q), y0 + ln * q * math.sin(tang + 0.25 * q)) for q in [i / 12 for i in range(13)]]
            branch(out, rng, t, tw, rng.randint(2, 3), 56, 1.5)
    return out['back'] + out['stems'] + out['front']


def text_path(text, font_file, cap_px, target_w, cx, baseline, colors, weight_px=0.0, hscale=1.0):
    """Converts text into SVG paths, spacing letters so the line is target_w wide."""
    f = TTFont(FONT_DIR + font_file)
    gs = f.getGlyphSet(); cmap = f.getBestCmap(); upm = f['head'].unitsPerEm
    cap = f['OS/2'].sCapHeight or 0.7 * upm
    sc = cap_px / cap
    names = [cmap[ord(ch)] for ch in text]
    adv = [gs[n].width * sc * hscale for n in names]
    natural = sum(adv)
    track = (target_w - natural) / max(1, len(text) - 1)
    x = cx - target_w / 2
    out = []
    for ch, n, a, col in zip(text, names, adv, colors):
        pen = SVGPathPen(gs)
        tp = TransformPen(pen, (sc * hscale, 0, 0, -sc, x, baseline))
        gs[n].draw(tp)
        if pen.getCommands():
            extra = f' stroke="{col}" stroke-width="{weight_px}" stroke-linejoin="round"' if weight_px else ''
            out.append(f'<path d="{pen.getCommands()}" fill="{col}"{extra}/>')
        x += a + track
    return out


def build_center(theme):
    t = THEMES[theme]
    els = [f'<circle cx="{C}" cy="{C}" r="{R_IN}" fill="none" stroke="{t["ring"]}" stroke-width="3"/>',
           f'<circle cx="{C}" cy="{C - R_IN*0.33:.1f}" r="{R_IN*0.043:.1f}" fill="{t["dot"]}"/>']
    word = 'SANSARA'
    els += text_path(word, 'Poppins-Medium.ttf', R_IN * 0.195, R_IN * 1.70, C + 2, C + R_IN * 0.11,
                     [t['red']] + [t['text']] * (len(word) - 1), weight_px=2.2, hscale=1.2)
    sub = 'Ritual attributes'
    els += text_path(sub, 'Poppins-Regular.ttf', R_IN * 0.085, R_IN * 1.12, C + 2, C + R_IN * 0.31, [t['sub']] * len(sub))
    return els


def svg(body, theme, bg=False):
    t = THEMES[theme]
    defs = '<defs>' + ''.join(
        f'<linearGradient id="g{i}" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="{c}"/>'
        f'<stop offset="0.55" stop-color="{c}" stop-opacity="0.92"/><stop offset="1" stop-color="{c}" stop-opacity="0.55"/></linearGradient>'
        for i, c in enumerate(t['leaves'])) + '</defs>'
    b = f'<rect width="{W}" height="{W}" fill="{t["bg"]}"/>' if bg else ''
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {W}" width="{W}" height="{W}">'
            + defs + b + ''.join(body) + '</svg>')


SEED = int(os.environ.get("SEED", "23"))
for theme in THEMES:
    rng = random.Random(SEED)
    wreath = build_wreath(rng, theme)
    center = build_center(theme)
    files = {
        f'sansara_logo_{theme}.svg': svg(wreath + center, theme),
        f'sansara_wreath_{theme}.svg': svg(wreath, theme),
        f'sansara_center_{theme}.svg': svg(center, theme),
    }
    for name, content in files.items():
        open(os.path.join(OUT, name), 'w').write(content)
    cairosvg.svg2png(bytestring=files[f'sansara_logo_{theme}.svg'].encode(), write_to=os.path.join(OUT, f'sansara_logo_{theme}_2048.png'), output_width=2048, output_height=2048)
    cairosvg.svg2png(bytestring=svg(wreath + center, theme, bg=True).encode(), write_to=os.path.join(OUT, f'preview_{theme}.png'), output_width=1024, output_height=1024)
print('ok', OUT)
