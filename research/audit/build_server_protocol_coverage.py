"""Inventory relevant 26.3 packet-boundary source declarations.

The JSON is a prioritized worklist with explicit manual-review labels. Merely
appearing in the worklist does not imply that a declaration was audited.
"""

import json
import os
import sys
from pathlib import Path

from tree_sitter import Language, Parser
import tree_sitter_java


ROOT = Path("/tmp/mc26.3-common/net/minecraft")
CORE_FILES = {
    "network/Connection.java",
    "server/network/ServerCommonPacketListenerImpl.java",
    "server/network/ServerConfigurationPacketListenerImpl.java",
    "server/network/ServerLoginPacketListenerImpl.java",
    "server/network/ServerGamePacketListenerImpl.java",
    "server/network/ServerCommandSuggestionsProvider.java",
    "server/network/config/ServerResourcePackConfigurationTask.java",
    "server/players/PlayerList.java",
    "server/level/ServerPlayer.java",
    "util/debug/ServerDebugSubscribers.java",
    "world/inventory/AbstractContainerMenu.java",
    "world/inventory/MerchantMenu.java",
    "world/inventory/CrafterMenu.java",
    "world/inventory/AnvilMenu.java",
    "world/item/BundleItem.java",
    "world/item/ItemStack.java",
    "world/item/component/BundleContents.java",
}
DEEP_REVIEW = {
    "server/network/ServerCommonPacketListenerImpl.java": {"handleResourcePackResponse", "handleCustomClickAction"},
    "server/network/ServerConfigurationPacketListenerImpl.java": {"handleResourcePackResponse", "finishCurrentTask", "startNextTask", "startConfiguration", "addOptionalTasks", "handleSelectKnownPacks", "handleAcceptCodeOfConduct", "handleConfigurationFinished"},
    "server/network/ServerLoginPacketListenerImpl.java": {"handleHello", "handleKey", "handleLoginAcknowledgement", "verifyLoginAndFinishConnectionSetup", "handleCustomQueryPacket"},
    "server/network/ServerGamePacketListenerImpl.java": {"handleBundleItemSelectedPacket", "handleContainerClick", "handlePlaceRecipe", "handleContainerButtonClick", "handleSetCreativeModeSlot", "handleSelectTrade", "handleEditBook", "handleSignUpdate", "handleSetCommandBlock", "handleChangeGameMode", "handleCustomCommandSuggestions", "handleContainerSlotStateChanged", "handlePickItemFromBlock", "handlePickItemFromEntity", "handleDebugSubscriptionRequest"},
    "server/network/config/ServerResourcePackConfigurationTask.java": {"start"},
    "world/inventory/AbstractContainerMenu.java": {"setRemoteSlotUnsafe", "setSelectedBundleItemIndex"},
    "world/inventory/MerchantMenu.java": {"tryMoveItems"},
    "world/item/BundleItem.java": {"toggleSelectedItem"},
    "world/item/component/BundleContents.java": {"toggleSelectedItem", "indexIsOutsideAllowedBounds", "removeOne"},
}
LANG = Language(tree_sitter_java.language())
PARSER = Parser(LANG)
TYPE_NODES = {
    "class_declaration", "interface_declaration", "enum_declaration",
    "record_declaration", "annotation_type_declaration",
}
METHOD_NODES = {"method_declaration", "constructor_declaration", "compact_constructor_declaration"}


def decode_name(source, node):
    name = node.child_by_field_name("name")
    return source[name.start_byte:name.end_byte].decode() if name else None


def inventory_file(path):
    source = path.read_bytes()
    tree = PARSER.parse(source)
    rel = str(path.relative_to(ROOT))
    classes = []
    methods = []

    def walk(node, ancestors):
        next_ancestors = ancestors
        if node.type in TYPE_NODES:
            name = decode_name(source, node)
            if name:
                next_ancestors = ancestors + [name]
                classes.append({"name": ".".join(next_ancestors), "line": node.start_point.row + 1,
                                "kind": node.type})
        elif node.type in METHOD_NODES:
            name = decode_name(source, node)
            if name:
                methods.append({"owner": ".".join(ancestors), "name": name,
                                "line": node.start_point.row + 1,
                                "kind": node.type,
                                "review": "source_review" if name in DEEP_REVIEW.get(rel, ()) else "inventory_only"})
        for child in node.named_children:
            walk(child, next_ancestors)

    walk(tree.root_node, [])
    return {"file": str(path.relative_to(ROOT)), "class_count": len(classes),
            "method_count": len(methods), "parse_errors": tree.root_node.has_error,
            "classes": classes, "methods": methods}


def main():
    packets = {p for p in (ROOT / "network/protocol").rglob("Serverbound*.java")}
    files = sorted({ROOT / rel for rel in CORE_FILES} | packets)
    records = [inventory_file(path) for path in files]
    output = {
        "scope": "Relevant client-originated packet types and selected downstream handlers/auth/menu/item paths",
        "meaning": "Static declaration worklist; only methods labeled source_review received targeted manual review",
        "omitted_categories": ["Clientbound-only packet classes", "rendering and UI", "unrelated item behavior", "menus without reviewed packet path", "unrelated server utility classes"],
        "files": len(records),
        "classes": sum(x["class_count"] for x in records),
        "methods": sum(x["method_count"] for x in records),
        "parse_error_files": [x["file"] for x in records if x["parse_errors"]],
        "records": records,
    }
    out = Path(__file__).with_name("server-protocol-coverage.json")
    out.write_text(json.dumps(output, indent=2) + "\n")
    print(f"{output['files']} files, {output['classes']} classes, {output['methods']} methods; "
          f"{len(output['parse_error_files'])} files with parse errors")
    # tree-sitter-java crashes during interpreter teardown under this Python
    # 3.14 environment after all output is already written. Exit explicitly.
    sys.stdout.flush()
    os._exit(0)


if __name__ == "__main__":
    main()
