#!/usr/bin/env python3
"""Summarize an Anomoly passive anticheat profile JSONL file."""

import argparse
import json
from collections import Counter
from pathlib import Path


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
    distances = sorted(event["distanceFromLastTick"] for event in post_join if "distanceFromLastTick" in event)
    latencies = sorted(event["latencyMs"] for event in samples if "latencyMs" in event)
    return {
        "server": next((event.get("server") for event in events if event.get("type") == "join"), "unknown"),
        "brand": next((event.get("value") for event in reversed(events) if event.get("type") == "brand"), "unknown"),
        "observed_channels": sorted({event["id"] for event in events if event.get("type") == "custom_payload_channel"}),
        "duration_minutes": round(total_ticks / 1200, 2),
        "movement_samples": len(samples),
        "all_position_corrections": len(corrections),
        "position_corrections_after_5_seconds": len(post_join),
        "corrections_per_minute_after_5_seconds": round(len(post_join) / max((total_ticks - 100) / 1200, 1 / 1200), 2),
        "largest_correction_distance_from_last_tick": round(max(distances, default=0), 3),
        "median_latency_ms": latencies[len(latencies) // 2] if latencies else None,
        "modules_active_during_corrections": dict(module_counts.most_common()),
        "disconnect_reason": next((event.get("reason") for event in reversed(events) if event.get("type") == "leave"), None),
        "interpretation": "This is behavioral evidence; brand and channels do not reliably identify the anticheat plugin.",
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
