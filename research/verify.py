#!/usr/bin/env python3
"""Verify the local 26.3 research bundle and source evidence."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
from zipfile import ZipFile


ROOT = Path(__file__).resolve().parents[1]
BUNDLE = ROOT / "minecraft-26.3"


def verify_assets() -> tuple[int, int]:
    index = json.loads((BUNDLE / "assets/indexes/26.3-34.json").read_text())
    count = size = 0
    for logical_name, entry in index["objects"].items():
        digest = entry["hash"]
        path = BUNDLE / "assets/objects" / digest[:2] / digest
        if not path.is_file():
            raise AssertionError(f"Missing asset: {logical_name} ({digest})")
        data = path.read_bytes()
        if len(data) != entry["size"] or hashlib.sha1(data).hexdigest() != digest:
            raise AssertionError(f"Corrupt asset: {logical_name} ({digest})")
        count += 1
        size += len(data)
    return count, size


def verify_source() -> None:
    common = BUNDLE / "sources/minecraft-common-sources.jar"
    client = BUNDLE / "sources/minecraft-clientOnly-sources.jar"
    with ZipFile(common) as jar:
        handler = jar.read("net/minecraft/server/network/ServerGamePacketListenerImpl.java").decode()
        attack_range = jar.read("net/minecraft/world/item/component/AttackRange.java").decode()
        projectile_util = jar.read("net/minecraft/world/entity/projectile/ProjectileUtil.java").decode()
        kinetic_weapon = jar.read("net/minecraft/world/item/component/KineticWeapon.java").decode()
        maps = jar.read("net/minecraft/world/level/saveddata/maps/MapItemSavedData.java").decode()
        required = {
            "attack buffer": "isWithinAttackRange(mainHandItem, targetBounds, 3.0)",
            "block correction": "new ClientboundBlockUpdatePacket(level, pos.relative(direction))",
            "creative gate": "if (this.player.hasInfiniteMaterials())",
            "tick-end reset": "this.receivedPositionThisTick = false;",
            "teleport acknowledgment position": "this.handlePlayerPositionChange(packet.x(), packet.y(), packet.z(), packet.yRot(), packet.xRot(), false, false);",
            "vertical residual error": "if (yDist > -0.5 || yDist < 0.5)",
            "survival pick-block oracle": "int slotWithExistingItem = inventory.findSlotMatchingItem(itemStack);",
            "crafter slot mutation": "crafterBlockEntity.setSlotState(packet.slotId(), packet.newState());",
        }
        for label, token in required.items():
            if token not in handler:
                raise AssertionError(f"26.3 source evidence changed: {label}")
        if "this.effectiveMaxRange(attacker) + this.hitboxMargin + extraBuffer" not in attack_range:
            raise AssertionError("AttackRange distance calculation changed")
        if "attacker.getKnownMovement().dot(look)" not in projectile_util:
            raise AssertionError("Spear motion ray calculation changed")
        if "livingEntity.getKnownSpeed().scale(20.0)" not in kinetic_weapon:
            raise AssertionError("Kinetic spear speed calculation changed")
        if "this.carriedBy" not in maps or "this.addDecoration(MapDecorationTypes.PLAYER" not in maps:
            raise AssertionError("Shared map tracking changed")
    with ZipFile(client) as jar:
        minecraft = jar.read("net/minecraft/client/Minecraft.java").decode()
        if "connection.send(ServerboundClientTickEndPacket.INSTANCE)" not in minecraft:
            raise AssertionError("Client tick-end behavior changed")
    with ZipFile(BUNDLE / "jars/minecraft-common.jar") as jar:
        camp = json.loads(jar.read("data/minecraft/worldgen/structure_set/abandoned_camp.json"))
        placement = camp["placement"]
        if (placement["spacing"], placement["separation"], placement["salt"]) != (37, 8, 91231127):
            raise AssertionError("Abandoned camp placement changed")


if __name__ == "__main__":
    count, size = verify_assets()
    verify_source()
    print(f"Verified 26.3 bundle: {count} SHA-1 assets, {size} bytes; source evidence and camp data match")
