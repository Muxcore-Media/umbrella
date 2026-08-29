#!/usr/bin/env python3
"""
MuxCore brand-mark concept generator.

Every mark below is defined purely by geometry (circle equations, trig
sampling, evenly-spaced coordinates) rather than hand-drawn curves. Re-running
this script regenerates the SVGs in ./logo-concepts/ deterministically -- tweak
the CONFIG constants and re-run to iterate.

Usage:
    python3 generate_logos.py
"""

import math
import os

OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "logo-concepts")
os.makedirs(OUT_DIR, exist_ok=True)

CANVAS = 512
CENTER = CANVAS / 2

COLOR = {
    "bg_base": "#0B0C0F",
    "bg_elevated": "#15161B",
    "border": "#23252B",
    "text": "#F5F5F7",
    "text_secondary": "#A1A1AA",
    "teal": "#3DB8A8",
    "teal_hover": "#56D0C0",
    "amber": "#E6A23C",
}


def fmt(n):
    """Trim floats to 2 decimals, drop trailing zeros, keep ints clean."""
    r = round(n, 2)
    if r == int(r):
        return str(int(r))
    return f"{r:.2f}".rstrip("0").rstrip(".")


def pt(x, y):
    return f"{fmt(x)},{fmt(y)}"


def xml_escape(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def svg_header(title):
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {CANVAS} {CANVAS}" '
        f'width="{CANVAS}" height="{CANVAS}" role="img" aria-labelledby="title">\n'
        f'  <title id="title">{xml_escape(title)}</title>\n'
    )


def bg_tile(rx=112, color=None):
    color = color or COLOR["bg_base"]
    return f'  <rect x="0" y="0" width="{CANVAS}" height="{CANVAS}" rx="{rx}" fill="{color}" />\n'


def write_svg(filename, body, title):
    svg = svg_header(title) + bg_tile() + body + "</svg>\n"
    path = os.path.join(OUT_DIR, filename)
    with open(path, "w") as f:
        f.write(svg)
    print(f"wrote {path}")


# ---------------------------------------------------------------------------
# Catmull-Rom -> cubic-bezier smoothing helper. Turns N sampled points into
# a smooth "M ... C ... C ..." path -- every curved thread in this file is
# built by sampling a parametric function then smoothing it through here.
# ---------------------------------------------------------------------------
def catmull_rom_path(points):
    if len(points) < 3:
        return "M " + " L ".join(pt(x, y) for x, y in points)
    p = [points[0]] + points + [points[-1]]
    d = [f"M {pt(*points[0])}"]
    for i in range(1, len(p) - 2):
        p0, p1, p2, p3 = p[i - 1], p[i], p[i + 1], p[i + 2]
        c1 = (p1[0] + (p2[0] - p0[0]) / 6.0, p1[1] + (p2[1] - p0[1]) / 6.0)
        c2 = (p2[0] - (p3[0] - p1[0]) / 6.0, p2[1] - (p3[1] - p1[1]) / 6.0)
        d.append(f"C {pt(*c1)} {pt(*c2)} {pt(*p2)}")
    return " ".join(d)


def sample_range(t0, t1, n):
    return [t0 + (t1 - t0) * i / n for i in range(n + 1)]


def gapped_subpaths(param_fn, t0, t1, gap_ts, buf, min_steps=6, density=1 / 6):
    """Sample a parametric curve param_fn(t) over [t0, t1], cutting a small
    gap around each t in gap_ts, and return the resulting smoothed subpaths
    joined into one path `d` string. Shared by every woven-thread concept
    below so every crossing is a real, computed gap -- never a guessed one."""
    bounds = [t0]
    for c in sorted(gap_ts):
        bounds += [c - buf, c + buf]
    bounds.append(t1)
    subpaths = []
    for i in range(0, len(bounds), 2):
        a, b = bounds[i], bounds[i + 1]
        if b - a < 1e-6:
            continue
        n = max(min_steps, int((b - a) / (t1 - t0) / density))
        subpaths.append(catmull_rom_path([param_fn(t) for t in sample_range(a, b, n)]))
    return " ".join(subpaths)


# ---------------------------------------------------------------------------
# Concept F -- The Braid (refined)
# Two strands, phase-shifted 180 degrees (sin and -sin over the same period),
# rendered as a true 2-color twisted rope: at each interior crossing the
# "behind" strand gets a small gap cut out of its sampled path, alternating
# which strand is in front as you move down the braid. Refinement: small
# terminal hubs where the strands meet at top/bottom, echoing the "core"
# bead used in the Infinity Thread mark so the two share a visual language.
# ---------------------------------------------------------------------------
def concept_f():
    cx = 256
    amp = 66
    top_y = 112
    length = 288
    period = 144
    omega = 2 * math.pi / period
    buf = 15

    def strand_point(phase):
        return lambda t: (cx + amp * math.sin(omega * t + phase), top_y + t)

    crossings = [k * period / 2 for k in range(0, int(length / (period / 2)) + 1)]
    interior = [c for c in crossings if 8 < c < length - 8]

    gaps_a, gaps_b = [], []
    for i, c in enumerate(interior):
        (gaps_a if i % 2 == 0 else gaps_b).append(c)

    strand_a = (
        f'  <path d="{gapped_subpaths(strand_point(0), 0, length, gaps_a, buf)}" '
        f'fill="none" stroke="{COLOR["teal"]}" stroke-width="19" '
        f'stroke-linecap="round" stroke-linejoin="round" />\n'
    )
    strand_b = (
        f'  <path d="{gapped_subpaths(strand_point(math.pi), 0, length, gaps_b, buf)}" '
        f'fill="none" stroke="{COLOR["amber"]}" stroke-width="19" '
        f'stroke-linecap="round" stroke-linejoin="round" />\n'
    )
    hub_top = f'  <circle cx="{cx}" cy="{top_y}" r="10" fill="{COLOR["teal_hover"]}" />\n'
    hub_bottom = f'  <circle cx="{cx}" cy="{top_y + length}" r="10" fill="{COLOR["teal_hover"]}" />\n'

    return strand_a + strand_b + hub_top + hub_bottom


# ---------------------------------------------------------------------------
# Concept K -- Infinity Thread (refined)
# A lemniscate of Gerono (x = R cos t, y = R sin t cos t): two lobes, each
# extended slightly past the shared center so they genuinely overlap there
# instead of merely touching tip-to-tip. That overlap gets a real alternating
# gap at each of its two passes (amber wins the first pass, teal the second)
# -- an actual weave, not just two petals meeting at a point -- plus a small
# "core" bead marking the shared center, a nod to the product's own name.
# ---------------------------------------------------------------------------
def concept_k():
    cx, cy = 256, 256
    R = 148
    overlap = 0.16
    buf = 0.11

    def gerono(t):
        return (cx + R * math.cos(t), cy + R * math.sin(t) * math.cos(t))

    right_t0, right_t1 = -math.pi / 2 - overlap, math.pi / 2 + overlap
    left_t0, left_t1 = math.pi / 2 - overlap, 3 * math.pi / 2 + overlap

    right_d = gapped_subpaths(gerono, right_t0, right_t1, [-math.pi / 2], buf, density=1 / 40)
    left_d = gapped_subpaths(gerono, left_t0, left_t1, [math.pi / 2], buf, density=1 / 40)

    right_lobe = (
        f'  <path d="{right_d}" fill="none" stroke="{COLOR["amber"]}" '
        f'stroke-width="25" stroke-linecap="round" stroke-linejoin="round" />\n'
    )
    left_lobe = (
        f'  <path d="{left_d}" fill="none" stroke="{COLOR["teal"]}" '
        f'stroke-width="25" stroke-linecap="round" stroke-linejoin="round" />\n'
    )
    core = f'  <circle cx="{cx}" cy="{cy}" r="9" fill="none" stroke="{COLOR["text"]}" stroke-width="4" opacity="0.85" />\n'

    return right_lobe + left_lobe + core


# ---------------------------------------------------------------------------
# Concept N -- Circular Braid (the Wreath)
# Concept F's twisted rope, but wrapped around a ring instead of run
# straight: two radii r0 +/- amp*sin(k*theta) traced around a full circle.
# Crossings (where sin(k*theta)=0) alternate front/back exactly like the
# straight braid -- same math, closed into a loop.
# ---------------------------------------------------------------------------
def concept_n():
    cx, cy = 256, 256
    r0, amp, k = 128, 40, 3
    phase0 = math.pi / (2 * k)  # keeps a crossing away from the theta=0 seam

    def ring_point(sign):
        def fn(theta):
            r = r0 + sign * amp * math.sin(k * theta + phase0)
            return (cx + r * math.cos(theta), cy + r * math.sin(theta))

        return fn

    fine_n = 720
    fine_thetas = sample_range(0, 2 * math.pi, fine_n)
    sinvals = [math.sin(k * t + phase0) for t in fine_thetas]
    crossings = []
    for i in range(len(fine_thetas) - 1):
        if sinvals[i] == 0 or (sinvals[i] < 0) != (sinvals[i + 1] < 0):
            frac = abs(sinvals[i]) / (abs(sinvals[i]) + abs(sinvals[i + 1]))
            crossings.append(fine_thetas[i] + (fine_thetas[i + 1] - fine_thetas[i]) * frac)

    buf = 0.09
    gaps_a, gaps_b = [], []
    for i, c in enumerate(crossings):
        (gaps_a if i % 2 == 0 else gaps_b).append(c)

    strand_a = (
        f'  <path d="{gapped_subpaths(ring_point(+1), 0, 2 * math.pi, gaps_a, buf, density=1/50)}" '
        f'fill="none" stroke="{COLOR["teal"]}" stroke-width="18" '
        f'stroke-linecap="round" stroke-linejoin="round" />\n'
    )
    strand_b = (
        f'  <path d="{gapped_subpaths(ring_point(-1), 0, 2 * math.pi, gaps_b, buf, density=1/50)}" '
        f'fill="none" stroke="{COLOR["amber"]}" stroke-width="18" '
        f'stroke-linecap="round" stroke-linejoin="round" />\n'
    )
    return strand_a + strand_b


# ---------------------------------------------------------------------------
# Concept O -- Triskelion Weave
# The classic three-petal polar rose, r = R sin(3*theta), traced directly
# with theta as the true polar angle (not decoupled from it -- that
# earlier draft produced overlapping rings by mistake, not petals). Odd k
# means the whole 3-petal flower completes over theta in [0, pi]; each of
# the 3 natural zero-to-zero arcs is one petal, colored separately.
# ---------------------------------------------------------------------------
def concept_o():
    cx, cy = 256, 256
    R = 220
    k = 3
    colors = [COLOR["teal"], COLOR["amber"], COLOR["teal_hover"]]

    def rose(theta):
        r = R * math.sin(k * theta)
        return (cx + r * math.cos(theta), cy + r * math.sin(theta))

    zeros = [m * math.pi / k for m in range(k + 1)]
    body = ""
    for m in range(k):
        d = gapped_subpaths(rose, zeros[m], zeros[m + 1], [], 0, density=1 / 40)
        body += (
            f'  <path d="{d}" fill="none" stroke="{colors[m]}" stroke-width="22" '
            f'stroke-linecap="round" stroke-linejoin="round" />\n'
        )

    core = f'  <circle cx="{cx}" cy="{cy}" r="9" fill="none" stroke="{COLOR["text"]}" stroke-width="4" opacity="0.85" />\n'
    return body + core


# ---------------------------------------------------------------------------
# Concept P -- Chain Link
# Two equal circles, offset so they overlap; the two true intersection
# points are computed exactly (standard circle-circle intersection), and
# each ring is drawn as one continuous arc that leaves a single gap at
# whichever intersection point it loses -- circle 1 in front at the top
# crossing, circle 2 in front at the bottom one. A literal "linked modules"
# mark built from the same two-tone, real-crossing language as F/K/N/O.
# ---------------------------------------------------------------------------
def concept_p():
    r = 112
    d = 128
    c1 = (256 - d / 2, 256)
    c2 = (256 + d / 2, 256)
    a = d / 2
    h = math.sqrt(r * r - a * a)
    mid = (256, 256)
    top_pt = (mid[0], mid[1] - h)
    bot_pt = (mid[0], mid[1] + h)

    def angle_from(center, p):
        return math.atan2(p[1] - center[1], p[0] - center[0])

    def ring_with_gap(center, loser_pt, gap_half_angle=0.16, n=100):
        loser_angle = angle_from(center, loser_pt)
        start = loser_angle + gap_half_angle
        end = loser_angle + 2 * math.pi - gap_half_angle
        pts = [
            (center[0] + r * math.cos(a), center[1] + r * math.sin(a))
            for a in sample_range(start, end, n)
        ]
        return catmull_rom_path(pts)

    # circle 1 wins at the top intersection, loses (gets the gap) at the bottom
    ring1 = ring_with_gap(c1, bot_pt)
    # circle 2 wins at the bottom intersection, loses at the top
    ring2 = ring_with_gap(c2, top_pt)

    link1 = (
        f'  <path d="{ring1}" fill="none" stroke="{COLOR["teal"]}" '
        f'stroke-width="20" stroke-linecap="round" stroke-linejoin="round" />\n'
    )
    link2 = (
        f'  <path d="{ring2}" fill="none" stroke="{COLOR["amber"]}" '
        f'stroke-width="20" stroke-linecap="round" stroke-linejoin="round" />\n'
    )
    return link1 + link2


CONCEPTS = [
    ("f-braid.svg", concept_f, "MuxCore mark concept F: The Braid (refined)"),
    ("k-infinity-thread.svg", concept_k, "MuxCore mark concept K: Infinity Thread (refined)"),
    ("n-circular-braid.svg", concept_n, "MuxCore mark concept N: Circular Braid (the Wreath)"),
    ("o-triskelion-weave.svg", concept_o, "MuxCore mark concept O: Triskelion Weave"),
    ("p-chain-link.svg", concept_p, "MuxCore mark concept P: Chain Link"),
]

for filename, fn, title in CONCEPTS:
    write_svg(filename, fn(), title)

print("done")
