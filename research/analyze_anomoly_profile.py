#!/usr/bin/env python3
"""Summarize an Anomoly passive anticheat profile JSONL file."""

import argparse
import json
from collections import Counter
from pathlib import Path

SIGNATURES = Path(__file__).resolve().parents[1] / "src/main/resources/assets/anomoly/anticheat_signatures.json"


def classify(commands: set[str], channels: set[str]) -> list[dict]:
    signatures = json.loads(SIGNATURES.read_text(encoding="utf-8"))
    if signatures.get("schema") != 1:
        raise ValueError("Unsupported anticheat signature schema")
    commands = {name.strip().lower().removeprefix("/") for name in commands}
    channels = {name.strip().lower().removeprefix("/") for name in channels}
    candidates = []
    for signature in signatures["signatures"]:
        roots = set(signature["roots"])
        namespaces = set(signature["namespaces"])
        evidence = []
        namespaced = False
        for command in sorted(commands):
            if command in roots or ":" in command and command.split(":", 1)[0] in namespaces:
                evidence.append(f"visible command /{command}")
                if ":" in command:
                    namespaced = True
        for channel in sorted(channels):
            if ":" in channel and channel.split(":", 1)[0] in namespaces:
                evidence.append(f"observed payload {channel}")
                namespaced = True
        if evidence:
            candidates.append({"name": signature["name"],
                               "confidence": "plausible" if namespaced else "weak",
                               "evidence": evidence, "source": signature["source"]})
    return sorted(candidates, key=lambda candidate: (candidate["confidence"] != "plausible", candidate["name"]))


def analyze(path: Path) -> dict:
    events = []
    with path.open(encoding="utf-8") as stream:
        for line_no, line in enumerate(stream, 1):
            try:
                events.append(json.loads(line))
            except json.JSONDecodeError as error:
                raise ValueError(f"{path}:{line_no}: {error}") from error
    samples = [event for event in events if event.get("type") == "movement_sample"]
    corrections = [event for event in events if event.get("type") == "position_correction"]
    post_join = [event for event in corrections if event.get("tick", 0) >= 100]
    total_ticks = max((event.get("tick", 0) for event in events), default=0)
    module_counts = Counter()
    for event in post_join:
        for module in filter(None, event.get("modules", "").split(",")):
            module_counts[module] += 1
    exposure_samples = Counter()
    for event in samples:
        if event.get("tick", 0) < 100:
            continue
        active = set(filter(None, event.get("modules", "").split(",")))
        for module in active or {"(no modules)"}:
            exposure_samples[module] += 1
    correction_contexts = Counter()
    for event in post_join:
        active = set(filter(None, event.get("modules", "").split(",")))
        for module in active or {"(no modules)"}:
            correction_contexts[module] += 1
    exposure = {
        module: {
            "minutes_observed": round(count / 60, 2),
            "samples": count,
            "corrections": correction_contexts[module],
            "corrections_per_observed_minute": round(correction_contexts[module] * 60 / count, 2) if count >= 30 else None,
        }
        for module, count in sorted(exposure_samples.items())
    }
    for module, count in correction_contexts.items():
        if module not in exposure:
            exposure[module] = {"minutes_observed": 0, "samples": 0, "corrections": count,
                                "corrections_per_observed_minute": None}
    distances = sorted(event["distanceFromLastTick"] for event in post_join if "distanceFromLastTick" in event)
    latencies = sorted(event["latencyMs"] for event in samples if "latencyMs" in event)
    channels = {event["id"] for event in events if event.get("type") == "custom_payload_channel"}
    commands = {event["name"] for event in events if event.get("type") == "command_literal"}
    return {
        "server": next((event.get("server") for event in events if event.get("type") == "join"), "unknown"),
        "brand": next((event.get("value") for event in reversed(events) if event.get("type") == "brand"), "unknown"),
        "observed_channels": sorted(channels),
        "observed_command_namespaces": sorted({event["namespace"] for event in events if event.get("type") == "command_literal" and "namespace" in event}),
        "observed_root_commands": sorted(commands),
        "possible_anticheats": classify(commands, channels),
        "duration_minutes": round(total_ticks / 1200, 2),
        "movement_samples": len(samples),
        "all_position_corrections": len(corrections),
        "position_corrections_after_5_seconds": len(post_join),
        "corrections_per_minute_after_5_seconds": round(len(post_join) / max((total_ticks - 100) / 1200, 1 / 1200), 2),
        "largest_correction_distance_from_last_tick": round(max(distances, default=0), 3),
        "median_latency_ms": latencies[len(latencies) // 2] if latencies else None,
        "modules_active_during_corrections": dict(module_counts.most_common()),
        "correction_rate_by_module_exposure": exposure,
        "disconnect_reason": next((event.get("reason") for event in reversed(events) if event.get("type") == "leave"), None),
        "behavioral_signal": (
            "repeated post-join position corrections" if len(post_join) >= 3
            else "two post-join position corrections" if len(post_join) == 2
            else "isolated post-join position correction" if post_join
            else "no post-join position corrections observed"
        ),
        "interpretation": "Candidates are command or payload hints, not proof of installation. Hidden commands can yield no candidate. Corrections and exposure rates are observational, can reflect lag or server actions, and cannot identify a specific anticheat.",
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("profile", type=Path, help="Anomoly .jsonl profile log")
    parser.add_argument("--json", action="store_true", help="Print machine-readable JSON")
    args = parser.parse_args()
    result = analyze(args.profile)
    if args.json:
        print(json.dumps(result, indent=2))
    else:
        for key, value in result.items():
            print(f"{key.replace('_', ' ').capitalize()}: {value}")


if __name__ == "__main__":
    main()
