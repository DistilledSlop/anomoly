#!/usr/bin/env python3
"""Keep Minecraft Controls labels in sync with registered Anomoly modules."""

import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MODULES = ROOT / "src/client/java/dev/anomoly/modules"
MANAGER = ROOT / "src/client/java/dev/anomoly/ModuleManager.java"
LANG = ROOT / "src/main/resources/assets/anomoly/lang/en_us.json"


def expected_text():
    manager = MANAGER.read_text(encoding="utf-8")
    registered = manager.split("private final List<Module> modules = List.of(", 1)[1].split(");", 1)[0]
    classes = re.findall(r"new\s+(\w+)\s*\(", registered)
    language = json.loads(LANG.read_text(encoding="utf-8"))
    language = {key: value for key, value in language.items() if not key.startswith("key.anomoly.toggle.")}
    language["key.category.anomoly.modules"] = "Anomoly Modules"
    language["key.anomoly.free_look"] = "Hold Anomoly Free Look"
    for class_name in classes:
        source = (MODULES / f"{class_name}.java").read_text(encoding="utf-8")
        match = re.search(r'super\(\s*"([^"]+)",\s*"([^"]+)"', source)
        if not match:
            raise ValueError(f"Missing module id/name in {class_name}")
        module_id, module_name = match.groups()
        language[f"key.anomoly.toggle.{module_id}"] = f"Toggle Anomoly {module_name}"
    return json.dumps(language, indent=2, ensure_ascii=False) + "\n"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="Fail if the language file is stale")
    args = parser.parse_args()
    expected = expected_text()
    if args.check:
        if LANG.read_text(encoding="utf-8") != expected:
            raise SystemExit("Module key labels are stale; run python3 tools/module_keys.py")
        print("Module key labels match registered modules")
    else:
        LANG.write_text(expected, encoding="utf-8")
        print("Updated module key labels")


if __name__ == "__main__":
    main()
