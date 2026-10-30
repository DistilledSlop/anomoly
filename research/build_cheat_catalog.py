"""Snapshot official client inventories into a conservatively deduplicated catalog.

Run with /tmp/mc-catalog-venv/bin/python research/build_cheat_catalog.py.
Meteor and Wurst are read from the public source checkouts used by the research
notebook; other inventories are fetched from their publishers' feature pages.
"""

from __future__ import annotations

import json
import re
from concurrent.futures import ThreadPoolExecutor
from collections import defaultdict
from collections import Counter
from datetime import date
from pathlib import Path

import requests
from bs4 import BeautifulSoup


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "cheats"
OUT.mkdir(exist_ok=True)
S = requests.Session()
S.headers["User-Agent"] = "Mozilla/5.0 research catalog"
METEOR_COMMIT = "e3a2e77967629d216e127b5ceecf71fb15f9d2e5"
WURST_COMMIT = "4fa258b7b339f1823e73ae8455c404209e3690cd"
THUNDER_COMMIT = "49c76bcc5a5e4fbd12b06305c09f0c8fb76c510b"

SOURCE = {
    "Meteor": f"https://github.com/MeteorDevelopment/meteor-client/blob/{METEOR_COMMIT}/src/main/java/meteordevelopment/meteorclient/systems/modules/Modules.java",
    "Wurst": f"https://github.com/Wurst-Imperium/Wurst7/blob/{WURST_COMMIT}/src/main/java/net/wurstclient/hack/HackList.java",
    "Impact": "https://impactclient.net/features",
    "RusherHack": "https://rusherhack.org/features/",
    "LiquidBounce": "https://liquidbounce.net/docs/modules/overview",
    "Vape": "https://www.vape.gg/index.php/features",
    "Future": "https://www.futureclient.net/",
    "ThunderHack Recode": f"https://github.com/Pan4ur/ThunderHack-Recode/blob/{THUNDER_COMMIT}/src/main/java/thunder/hack/core/manager/client/ModuleManager.java",
}

entries = []


def add(client, name, category="Other", description="", url=None, provenance="module list", description_url=None,
        description_basis="publisher description"):
    name = re.sub(r"\s+", " ", name).strip()
    description = re.sub(r"§.", "", description)
    if name:
        entries.append({"client": client, "name": name, "category": category,
                        "description": description.strip(), "url": url or SOURCE[client],
                        "provenance": provenance,
                        "description_url": description_url or url or SOURCE[client],
                        "description_basis": description_basis})


meteor = Path("/tmp/meteor-client/src/main/java/meteordevelopment/meteorclient/systems/modules")
registry = (meteor / "Modules.java").read_text()
for name in re.findall(r"\badd\(new ([A-Za-z0-9_]+)\(\)\);", registry):
    candidates = list(meteor.rglob(f"{name}.java"))
    if not candidates:
        continue
    path = candidates[0]
    source = path.read_text()
    match = re.search(r'super\s*\(Categories\.(\w+),\s*"([^"]+)",\s*"([^"]+)"', source)
    display = match.group(2).replace("-", " ").title() if match else name
    desc = match.group(3) if match else ""
    category = match.group(1) if match else path.relative_to(meteor).parts[0].title()
    add("Meteor", display, category, desc,
        f"https://github.com/MeteorDevelopment/meteor-client/blob/{METEOR_COMMIT}/src/main/java/meteordevelopment/meteorclient/systems/modules/{path.relative_to(meteor).as_posix()}")

wurst = Path("/tmp/wurst7/src/main/java/net/wurstclient/hack/HackList.java").read_text()
wurst_descriptions = json.loads(Path("/tmp/wurst7/src/main/resources/assets/wurst/translations/en_us.json").read_text())
wurst_supplements = {
    "tpaura": "Moves the client player near a selected entity and attacks when the weapon cooldown is ready.",
    "xray": "Changes block rendering to reveal selected blocks through surrounding terrain.",
}
wurst_desc_url = f"https://github.com/Wurst-Imperium/Wurst7/blob/{WURST_COMMIT}/src/main/resources/assets/wurst/translations/en_us.json"
for name in re.findall(r"public final (\w+)Hack \w+ = new \w+Hack\(\);", wurst):
    code = Path(f"/tmp/wurst7/src/main/java/net/wurstclient/hacks/{name}Hack.java").read_text()
    category_match = re.search(r"setCategory\(Category\.([A-Z_]+)\)", code)
    category = category_match.group(1).title() if category_match else "Other"
    desc_key = "description.wurst.hack." + re.sub(r"[^a-z0-9]", "", name.lower())
    code_url = f"https://github.com/Wurst-Imperium/Wurst7/blob/{WURST_COMMIT}/src/main/java/net/wurstclient/hacks/{name}Hack.java"
    translated = wurst_descriptions.get(desc_key, "")
    supplemented = wurst_supplements.get(re.sub(r"[^a-z0-9]", "", name.lower()), "")
    add("Wurst", name, category, translated or supplemented,
        url=code_url, description_url=wurst_desc_url if translated else code_url,
        description_basis="publisher description" if translated else "source review")

thunder_root = Path("/tmp/thunderhack-recode/src/main/java/thunder/hack/features")
thunder_registry = Path("/tmp/thunderhack-recode/src/main/java/thunder/hack/core/manager/client/ModuleManager.java").read_text()
thunder_descriptions = json.loads(Path("/tmp/thunderhack-recode/src/main/resources/assets/thunderhack/lang/en_us.json").read_text())
thunder_supplements = json.loads((ROOT / "research/sources/thunderhack-supplements.json").read_text())
thunder_desc_url = f"https://github.com/Pan4ur/ThunderHack-Recode/blob/{THUNDER_COMMIT}/src/main/resources/assets/thunderhack/lang/en_us.json"
for name in re.findall(r"public static \w+ \w+ = new (\w+)\(\);", thunder_registry):
    candidates = list(thunder_root.rglob(f"{name}.java"))
    if not candidates:
        continue
    path = candidates[0]
    code = path.read_text()
    match = re.search(r'super\("([^"]+)",\s*Category\.(\w+)\)', code)
    display = match.group(1) if match else name
    parts = path.relative_to(thunder_root).parts
    fallback_category = parts[1].title() if parts[0] == "modules" else parts[0].title()
    category = match.group(2).title() if match else fallback_category
    desc_key = "descriptions." + category.lower() + "." + re.sub(r"[^a-z0-9]", "", display.lower())
    code_url = f"https://github.com/Pan4ur/ThunderHack-Recode/blob/{THUNDER_COMMIT}/src/main/java/thunder/hack/features/{path.relative_to(thunder_root).as_posix()}"
    translated = thunder_descriptions.get(desc_key, "")
    supplemented = thunder_supplements.get(re.sub(r"[^a-z0-9]", "", display.lower()), "")
    add("ThunderHack Recode", display, category, translated or supplemented,
        url=code_url, description_url=thunder_desc_url if translated else code_url,
        description_basis="publisher description" if translated else "source review")

impact_html = S.get(SOURCE["Impact"], timeout=30).text
match = re.search(r"var features = (\{.*?\});", impact_html, re.S)
if not match:
    raise RuntimeError("Impact inventory was not found")
for category, modules in json.loads(match.group(1)).items():
    for module in modules:
        add("Impact", module["name"], category, module.get("description", ""))

soup = BeautifulSoup(S.get(SOURCE["RusherHack"], timeout=30).text, "html.parser")
links = soup.select('a[href^="/features/modules/"]')


def rusher_category(link):
    url = "https://rusherhack.org" + link["href"]
    try:
        page = BeautifulSoup(S.get(url, timeout=20).text, "html.parser")
        heading = page.find(string=re.compile(r"Modules\s*->"))
        if heading:
            match = re.search(r"Modules\s*->\s*(\w+)\s*->", str(heading))
            if match:
                return match.group(1)
    except requests.RequestException:
        pass
    return "Other"


with ThreadPoolExecutor(max_workers=12) as pool:
    rusher_categories = list(pool.map(rusher_category, links))
for link, category in zip(links, rusher_categories):
    name = link.get_text(" ", strip=True)
    description = link.parent.get("content", "")
    basis = "publisher description"
    if name == "Debug" and not description:
        description = "Developer-facing module page with a configurable block list; the publisher gives no gameplay behavior description."
        basis = "publisher page review"
    add("RusherHack", name, category, description,
        "https://rusherhack.org" + link["href"], description_basis=basis)

lb_categories = ["combat", "movement", "player", "render", "world", "exploit", "misc", "fun"]
lb_descriptions = json.loads((ROOT / "research/sources/liquidbounce-descriptions.json").read_text())
for category in lb_categories:
    url = f"https://liquidbounce.net/docs/modules/{category}/overview"
    soup = BeautifulSoup(S.get(url, timeout=30).text, "html.parser")
    seen = set()
    for link in soup.select(f'a[href^="/docs/modules/{category}/"]'):
        href = link.get("href", "")
        if href.endswith("/overview") or href in seen or href.count("/") != 4:
            continue
        seen.add(href)
        module_url = "https://liquidbounce.net" + href
        add("LiquidBounce", link.get_text(" ", strip=True), category.title(),
            lb_descriptions.get(module_url, ""), url=module_url)

# Vape publishes a smaller, specialized inventory. Its feature page has labeled
# modules, but includes non-module UI and marketing copy, so capture only the
# explicitly named features below and keep the provenance visible.
vape = ["AimAssist", "AutoClicker", "Reach", "Velocity", "WTap", "STap", "Sprint",
        "ThrowPot", "Refill", "AutoArmor", "InventoryManager", "ArmorSwitch",
        "FreeLook", "NoClickDelay", "TimeChanger", "ClearWater", "BlockOverlay",
        "HitColor", "Radar", "Rearview", "TargetInfo"]
vape_soup = BeautifulSoup(S.get(SOURCE["Vape"], timeout=30).text, "html.parser")
vape_text = vape_soup.get_text(" ", strip=True)
for name in vape:
    if re.search(r"\b" + re.escape(name) + r"\b", vape_text, re.I):
        title = vape_soup.find("span", class_="title", string=lambda x: x and x.strip().lower() == name.lower())
        description = title.find_next_sibling("span", class_="description") if title else None
        desc = description.get_text(" ", strip=True) if description else ""
        desc = re.sub(r"^" + re.escape(name) + r"\s+", "", desc, flags=re.I)
        add("Vape", name, "Other", desc)

# Future's saved page contains the first, server-rendered Combat tab. Other
# tabs are loaded by JavaScript and are absent from the single saved HTML file.
future_html = ROOT / "Future - Minecraft Client.html"
future_soup = BeautifulSoup(future_html.read_text(), "html.parser")
future_features = future_soup.select_one("section#features")
if future_features is None:
    raise RuntimeError("Future feature section was not found in the saved HTML")
future_category = future_features.select_one("h3").get_text(" ", strip=True)
future_cards = future_features.select("div.grid h4")
if len(future_cards) != 22 or future_category != "Combat":
    raise RuntimeError("Future saved page structure changed; review parser")
for heading in future_cards:
    description = heading.find_next_sibling("p")
    add("Future", heading.get_text(" ", strip=True), future_category,
        description.get_text(" ", strip=True) if description else "",
        "https://www.futureclient.net/#features", provenance="saved feature tab")

# The user also saved the text of all the JavaScript-rendered tabs. The tab
# transcript has one name/description pair separated by blank lines.
future_tab_lines = [line.strip() for line in (ROOT / "tabs.txt").read_text().splitlines()]
future_tab_names = ["Exploits", "Movement", "Render", "World", "Miscellaneous", "Other"]
future_tab_categories = {"Exploits": "Exploit", "Miscellaneous": "Misc", "Other": "Misc"}
future_tab_counts = {"Exploits": 17, "Movement": 26, "Render": 26,
                     "World": 14, "Miscellaneous": 28, "Other": 8}
for index, tab in enumerate(future_tab_names):
    start = future_tab_lines.index(tab) + 1
    end = future_tab_lines.index(future_tab_names[index + 1]) if index + 1 < len(future_tab_names) else len(future_tab_lines)
    values = [value for value in future_tab_lines[start:end] if value]
    if len(values) != 2 * future_tab_counts[tab]:
        raise RuntimeError(f"Future {tab} tab count changed: {len(values) // 2}")
    for name, description in zip(values[::2], values[1::2]):
        add("Future", name, future_tab_categories.get(tab, tab), description,
            "https://www.futureclient.net/#features", provenance="saved feature tab transcript")


def key(name):
    return re.sub(r"[^a-z0-9]", "", name.lower())


# Merge only semantic aliases with established equivalent behavior. Ambiguous
# names (e.g. Aura vs KillAura, Search vs XRay) are retained separately.
ALIASES = {
    "aura": "killaura", "hitboxes": "hitbox", "holefiller": "holefill",
    "noclickdelay": "nomisscooldown", "nohitdelay": "nomisscooldown",
    "packetcanceller": "packetcanceler", "mobowners": "mobowner",
    "shaders": "shader", "autotpaccept": "autoaccept",
    "autoshearer": "autoshear", "nolevitation": "antilevitation",
    "prophuntesp": "prophunt",
    "cameranoclip": "cameraclip", "nocameraclip": "cameraclip",
    "fly": "flight", "boatflight": "boatfly",
    "elytraflight": "elytrafly", "autocrystal": "crystalaura",
    "autoxp": "autoexp", "fastxp": "autoexp", "autoleave": "autodisconnect",
    "autolog": "autodisconnect", "autosteal": "cheststealer",
    "autochest": "cheststealer", "safewalk": "safewalk",
    "scaffoldwalk": "scaffold", "scaffold": "scaffold",
    "chestesp": "storageesp", "storageesp": "storageesp",
    "fullbright": "fullbright", "light": "fullbright",
    "antivanish": "antivanish", "noroateset": "norotate",
    "noforcerotate": "norotate", "norotateset": "norotate",
    "antiwobble": "antinausea", "noslowdown": "noslow",
    "antibots": "antibot",
    "bowaim": "bowaimbot", "killaura": "killaura",
    "autoarmor": "autoarmor", "sneak": "autosneak",
    "antiknockback": "velocity",
    "inventorymove": "invwalk", "inventorywalk": "invwalk",
    "skinblink": "skinblinker", "autosprint": "sprint",
    "fastmine": "fastbreak", "speedmine": "fastbreak",
    "inventorymanager": "inventorycleaner",
    "hotbarreplenish": "replenish", "restock": "replenish",
    "search": "search", "speedhack": "speed", "speedhackhack": "speed",
    "noblockinteract": "noblockinteract",
}


def canonical(name):
    normalized = key(name)
    return ALIASES.get(normalized, normalized)


groups = defaultdict(list)
for item in entries:
    groups[canonical(item["name"])].append(item)

category_order = ["Combat", "Movement", "Player", "World", "Render", "Exploit", "Misc", "Fun", "Other"]
category_alias = {"Minigame": "Fun", "Client": "Misc", "Hud": "Render"}
CATEGORY_OVERRIDES = {
    "Combat": "BonemealAura FeedAura HitboxIgnore KillPotion Tillaura TrollPotion WTap NoClickDelay",
    "Movement": "AntiCactus HandNoClip Liquids Navigator PortalGui",
    "Player": "ArmorSwitch AutoAccept AutoDrop AutoLibrarian AutoReply AutoShear AutoSorter AutoSwitch AutoTrader Refill Throw",
    "World": "BuildRandom CrashChest ExtraChest ExtraCraft ExtraSign ForceOp Gamemode InstaBuild InstantBunker ItemGenerator NukerLegit SpeedNuker TemplateTool TreeBot Tunneller",
    "Render": "ExtraScreenshot ExtraTab ExtraTooltips LagDetector LagReducer Rearview",
    "Exploit": "AntiPacket Kaboom MassTpa",
    "Misc": "Announcer AntiQuit AntiSpam ArmorAlert AutoComplete ChatAppend ChatColors ExtraChat GreenText Greeter InfiniChat Panic RusherCrypt SkinBlink Timestamps TooManyHax Translate",
}
category_override_by_key = {key(name): category for category, names in CATEGORY_OVERRIDES.items() for name in names.split()}
SCOPE_NOTES = {
    "forceop": "Wurst describes an AuthMe password attack. AuthMe is a third-party authentication plugin; this is not a stock vanilla operator bypass. See the 26.3 [permission audit](../../exploits/permission-boundaries.md).",
    "itemgenerator": "Wurst labels this feature creative-mode only. Its name does not imply survival item creation on a stock 26.3 server.",
    "crashchest": "Wurst describes a harmful item payload from an older client snapshot. This catalog does not establish that it works against stock Minecraft 26.3.",
}


def preferred(group):
    priorities = {"LiquidBounce": 0, "Meteor": 1, "Impact": 2, "RusherHack": 3,
                  "Wurst": 4, "ThunderHack Recode": 5, "Future": 6, "Vape": 7}
    return sorted(group, key=lambda x: (priorities[x["client"]], len(x["name"])))[0]


catalog = []
for slug, group in sorted(groups.items()):
    lead = preferred(group)
    categories = [category_alias.get(x["category"], x["category"]) for x in group]
    known_categories = Counter(c for c in categories if c in category_order and c != "Other")
    lead_category = category_alias.get(lead["category"], lead["category"])
    category = max(known_categories, key=lambda c: (known_categories[c], c == lead_category, -category_order.index(c))) if known_categories else "Other"
    if category == "Other":
        category = category_override_by_key.get(slug, "Other")
    descriptions = [x["description"] for x in group if x["description"]]
    description = lead["description"] or (descriptions[0] if descriptions else "Published module name; behavior varies by client and server.")
    summary_source = lead if lead["description"] else next((x for x in group if x["description"]), lead)
    catalog.append({"id": slug, "name": lead["name"], "category": category,
                    "description": description,
                    "description_source": summary_source["description_url"],
                    "description_basis": summary_source["description_basis"],
                    "clients": sorted({x["client"] for x in group}),
                    "aliases": sorted({x["name"] for x in group}, key=str.lower),
                    "evidence": [{"client": x["client"], "name": x["name"], "url": x["url"],
                                  "category": x["category"],
                                  "provenance": x["provenance"], "description": x["description"],
                                  "description_source": x["description_url"],
                                  "description_basis": x["description_basis"]} for x in group]})

(OUT / "catalog.json").write_text(json.dumps({"snapshot": str(date.today()), "sources": SOURCE,
                                                 "local_source_snapshots": {"Future": ["Future - Minecraft Client.html", "tabs.txt"]},
                                                 "entries": catalog}, indent=2, ensure_ascii=False) + "\n")

feature_dir = OUT / "features"
feature_dir.mkdir(exist_ok=True)
current_ids = {item["id"] for item in catalog}
for old in feature_dir.glob("*.md"):
    if old.stem not in current_ids and old.read_text().startswith("<!-- generated by research/build_cheat_catalog.py -->"):
        old.unlink()
for item in catalog:
    lines = ["<!-- generated by research/build_cheat_catalog.py -->",
             f"# {item['name']}", "",
             f"**Category:** {item['category']}  ",
             f"**Aliases:** {', '.join(item['aliases'])}", "",
             "## What it does", "",
             item["description"].replace("\n", " "), "",
             f"Description basis: [{item['description_basis']}]({item['description_source']}). The module's behavior on a stock Minecraft 26.3 server has not been verified unless separately documented in [research](../../RESEARCH.md).",
             "", "## Client variants", ""]
    if item["id"] in SCOPE_NOTES:
        lines[12:12] = ["**Scope note:** " + SCOPE_NOTES[item["id"]], ""]
    for evidence in item["evidence"]:
        label = evidence["client"] + " — " + evidence["name"]
        desc = evidence["description"].replace("\n", " ") or "This source snapshot does not provide a behavior description."
        lines.extend([f"### [{label}]({evidence['url']})", "", desc, "",
                      f"Basis: [{evidence['description_basis']}]({evidence['description_source']}).", ""])
    (feature_dir / f"{item['id']}.md").write_text("\n".join(lines))

for category in category_order:
    selected = [x for x in catalog if x["category"] == category]
    if not selected:
        continue
    lines = [f"# {category} cheats and utilities", "", f"{len(selected)} deduplicated entries from the [catalog](catalog.json). Names link to detailed feature pages with each client's description and source. Descriptions are publisher claims or labeled source reviews; server compatibility is not implied.", "",
             "| Feature | What it does | Clients |", "| --- | --- | --- |"]
    for item in selected:
        detail = f"features/{item['id']}.md"
        desc = item["description"].replace("|", "\\|").replace("\n", " ")
        if len(desc) > 145:
            desc = desc[:142].rstrip() + "…"
        clients = ", ".join(item["clients"])
        lines.append(f"| [{item['name']}]({detail}) | {desc} | {clients} |")
    (OUT / f"{category.lower()}.md").write_text("\n".join(lines) + "\n")

counts = {client: sum(client in x["clients"] for x in catalog) for client in SOURCE}
raw_counts = {client: sum(x["client"] == client for x in entries) for client in SOURCE}
print(json.dumps({"raw": len(entries), "unique": len(catalog), "raw_by_client": raw_counts,
                  "unique_by_client": counts, "categories": {c: sum(x["category"] == c for x in catalog) for c in category_order}}, indent=2))
