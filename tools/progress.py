#!/usr/bin/env python3
"""Build Anomoly's catalog-linked progress treemap and searchable report."""

import argparse
import html
import json
import math
import os
import re
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CATALOG = ROOT / "cheats/catalog.json"
STATUS = ROOT / "progress/status.json"
OUTPUT = ROOT / "docs"
REPOSITORY = os.environ.get("GITHUB_REPOSITORY", "DistilledSlop/anomoly")
GITHUB = f"https://github.com/{REPOSITORY}/blob/main"
COLORS = {"backlog": "#344054", "partial": "#f2b544", "coded": "#49a8e8", "verified": "#4bd598"}
ORDER = ("verified", "coded", "partial", "backlog")


def load_features():
    catalog = json.loads(CATALOG.read_text(encoding="utf-8"))["entries"]
    status = json.loads(STATUS.read_text(encoding="utf-8"))
    by_id = {entry["id"]: entry for entry in catalog}
    registered = set(re.findall(r"new\s+(\w+)\s*\(", (ROOT / "src/client/java/dev/anomoly/ModuleManager.java").read_text()))
    unknown = set(status["catalog"]) - set(by_id)
    if unknown:
        raise ValueError(f"Progress has unknown catalog IDs: {sorted(unknown)}")
    features = []
    for entry in catalog:
        state = status["catalog"].get(entry["id"], {})
        features.append({
            "id": entry["id"], "name": entry["name"], "category": entry["category"],
            "status": state.get("status", "backlog"), "module": state.get("module"),
            "note": state.get("note", ""), "url": f"{GITHUB}/cheats/features/{entry['id']}.md",
        })
    for item in status["additions"]:
        features.append({
            **item, "note": item.get("note", ""), "url": f"{GITHUB}/{item['doc']}",
        })
    seen = set()
    for feature in features:
        if feature["id"] in seen:
            raise ValueError(f"Duplicate feature ID: {feature['id']}")
        seen.add(feature["id"])
        if feature["status"] not in COLORS:
            raise ValueError(f"Unknown status for {feature['id']}: {feature['status']}")
        module = feature.get("module")
        if module:
            source = ROOT / f"src/client/java/dev/anomoly/modules/{module}.java"
            if module not in registered or not source.is_file():
                raise ValueError(f"Progress marks unregistered module {module} for {feature['id']}")
        elif feature["status"] != "backlog":
            raise ValueError(f"Non-backlog feature has no module: {feature['id']}")
    return features


def partition(groups, x, y, width, height):
    """Divide area recursively by feature count; each category gets proportional area."""
    if len(groups) == 1:
        return [(groups[0], (x, y, width, height))]
    total = sum(len(items) for _, items in groups)
    split = min(range(1, len(groups)), key=lambda i: abs(total / 2 - sum(len(v) for _, v in groups[:i])))
    left, right = groups[:split], groups[split:]
    fraction = sum(len(items) for _, items in left) / total
    if width >= height:
        cut = width * fraction
        return partition(left, x, y, cut, height) + partition(right, x + cut, y, width - cut, height)
    cut = height * fraction
    return partition(left, x, y, width, cut) + partition(right, x, y + cut, width, height - cut)


def svg(features, counts):
    groups = defaultdict(list)
    for feature in features:
        groups[feature["category"]].append(feature)
    ordered = sorted(groups.items(), key=lambda pair: -len(pair[1]))
    parts = [
        '<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 1200 720" role="img" aria-labelledby="title desc">',
        '<title id="title">Anomoly hack implementation treemap</title>',
        '<desc id="desc">Each tile is a researched feature. Blue is coded, amber partial, green gameplay verified, and gray backlog.</desc>',
        '<rect width="1200" height="720" rx="15" fill="#0b1220"/>',
        '<text x="20" y="37" fill="#e7f2ff" font-family="system-ui,sans-serif" font-size="25" font-weight="700">ANOMOLY / HACK PROGRESS</text>',
        f'<text x="20" y="61" fill="#a9bacd" font-family="system-ui,sans-serif" font-size="14">{counts["coded"]} coded · {counts["partial"]} partial · {counts["verified"]} gameplay verified · {counts["backlog"]} backlog / {len(features)} total</text>',
    ]
    for (category, items), (x, y, width, height) in partition(ordered, 12, 82, 1176, 625):
        subtotal = Counter(item["status"] for item in items)
        x += 2; y += 2; width -= 4; height -= 4
        parts.append(f'<rect x="{x:.2f}" y="{y:.2f}" width="{width:.2f}" height="{height:.2f}" rx="7" fill="#172235" stroke="#52657d" stroke-width="1"/>')
        label = f'{category}  {subtotal["coded"] + subtotal["verified"]}/{len(items)} coded'
        parts.append(f'<text x="{x + 8:.2f}" y="{y + 19:.2f}" fill="#e7f2ff" font-family="system-ui,sans-serif" font-size="14" font-weight="600">{html.escape(label)}</text>')
        tile_x, tile_y = x + 6, y + 28
        tile_w, tile_h = width - 12, height - 34
        columns = max(1, math.ceil(math.sqrt(len(items) * tile_w / max(tile_h, 1))))
        rows = math.ceil(len(items) / columns)
        cell_w, cell_h = tile_w / columns, tile_h / rows
        for index, item in enumerate(sorted(items, key=lambda feature: (ORDER.index(feature["status"]), feature["name"].casefold()))):
            cx = tile_x + (index % columns) * cell_w
            cy = tile_y + (index // columns) * cell_h
            title = f'{item["name"]} — {item["status"]}' + (f': {item["note"]}' if item["note"] else "")
            parts.extend([
                f'<a xlink:href="{html.escape(item["url"], quote=True)}" target="_blank">',
                f'<title>{html.escape(title)}</title>',
                f'<rect x="{cx + 1:.2f}" y="{cy + 1:.2f}" width="{max(cell_w - 2, 0):.2f}" height="{max(cell_h - 2, 0):.2f}" rx="2" fill="{COLORS[item["status"]]}"/>',
                '</a>',
            ])
    parts.append('</svg>')
    return "\n".join(parts) + "\n"


def webpage(features, counts):
    groups = defaultdict(list)
    for feature in features:
        groups[feature["category"]].append(feature)
    rows = []
    for category in sorted(groups):
        items = groups[category]
        c = Counter(item["status"] for item in items)
        rows.append(f'<tr><td>{html.escape(category)}</td><td>{c["verified"]}</td><td>{c["coded"]}</td><td>{c["partial"]}</td><td>{c["backlog"]}</td><td>{len(items)}</td></tr>')
    feature_rows = []
    for item in sorted(features, key=lambda feature: (feature["category"], feature["name"].casefold())):
        feature_rows.append(f'<tr><td>{html.escape(item["category"])}</td><td><a href="{html.escape(item["url"], quote=True)}">{html.escape(item["name"])}</a></td><td class="{item["status"]}">{item["status"]}</td><td>{html.escape(item["note"] or item.get("module") or "")}</td></tr>')
    return "\n".join([
        '<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">',
        '<title>Anomoly hack progress</title>',
        '<style>body{font:16px system-ui,sans-serif;background:#0b1220;color:#e7f2ff;max-width:1220px;margin:auto;padding:18px}a{color:#82c7fa}img{width:100%;height:auto}table{border-collapse:collapse;width:100%}th,td{border-bottom:1px solid #344054;padding:8px;text-align:left}input{background:#172235;color:#fff;border:1px solid #52657d;border-radius:5px;padding:10px;font:inherit;width:min(100%,450px)}.coded{color:#49a8e8}.partial{color:#f2b544}.verified{color:#4bd598}.backlog{color:#a9bacd}</style>',
        '</head><body><h1>Anomoly hack progress</h1>',
        '<p>One tile per researched feature. Tile area is proportional to category size. Blue means coded and compiled, amber means partial, green means gameplay verified, and gray means backlog. Startup validation alone does not count as gameplay verification.</p>',
        '<img src="progress.svg" alt="Treemap of Anomoly hack progress">',
        f'<p>{counts["coded"]} coded, {counts["partial"]} partial, {counts["verified"]} gameplay verified, {counts["backlog"]} backlog, {len(features)} total. <a href="progress.json">Raw data</a>.</p>',
        '<h2>By category</h2><table><thead><tr><th>Category</th><th>Verified</th><th>Coded</th><th>Partial</th><th>Backlog</th><th>Total</th></tr></thead><tbody>',
        *rows, '</tbody></table>',
        '<h2>Features</h2><label for="filter">Filter features</label><br><input id="filter" type="search" placeholder="Search name, category, or status">',
        '<table id="features"><thead><tr><th>Category</th><th>Feature</th><th>Status</th><th>Implementation</th></tr></thead><tbody>',
        *feature_rows, '</tbody></table>',
        '<script>const input=document.querySelector("#filter");input.addEventListener("input",()=>{const q=input.value.toLowerCase();for(const row of document.querySelectorAll("#features tbody tr"))row.hidden=!row.textContent.toLowerCase().includes(q)})</script>',
        '</body></html>',
    ]) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Fail if generated files differ")
    args = parser.parse_args()
    features = load_features()
    counts = Counter(feature["status"] for feature in features)
    report = {"counting_rule": "coded means a registered module exists; partial means the module covers only part of the catalog behavior; verified requires an in-game test", "counts": dict(counts), "total": len(features), "features": features}
    index = ('<!doctype html><html lang="en"><head><meta charset="utf-8">'
             '<meta http-equiv="refresh" content="0;url=progress.html">'
             '<title>Anomoly hack progress</title></head><body>'
             '<p><a href="progress.html">Open the progress treemap</a></p></body></html>\n')
    generated = {"progress.json": json.dumps(report, indent=2, ensure_ascii=False) + "\n",
                 "progress.svg": svg(features, counts), "progress.html": webpage(features, counts),
                 "index.html": index, ".nojekyll": ""}
    OUTPUT.mkdir(exist_ok=True)
    stale = []
    readme = ROOT / "README.md"
    begin, end = "<!-- anomoly-progress:start -->", "<!-- anomoly-progress:end -->"
    original = readme.read_text(encoding="utf-8")
    if original.count(begin) != 1 or original.count(end) != 1:
        raise ValueError("README progress marker missing or duplicated")
    count_text = (f'**{counts["coded"]} coded**, **{counts["partial"]} partial**, '
                  f'**{counts["verified"]} gameplay verified**, and **{counts["backlog"]} backlog**')
    before, tail = original.split(begin, 1)
    _, after = tail.split(end, 1)
    updated = before + begin + count_text + end + after
    if args.check:
        if updated != original: stale.append("README.md")
    elif updated != original:
        readme.write_text(updated, encoding="utf-8")
    for name, content in generated.items():
        path = OUTPUT / name
        if args.check:
            if not path.exists() or path.read_text(encoding="utf-8") != content: stale.append(name)
        else:
            path.write_text(content, encoding="utf-8")
    if stale:
        raise SystemExit("Stale progress artifacts: " + ", ".join(stale) + "; run python3 tools/progress.py")
    print(f'Progress: {counts["coded"]} coded, {counts["partial"]} partial, {counts["verified"]} verified, {counts["backlog"]} backlog / {len(features)} total')


if __name__ == "__main__":
    main()
