# Crafter slot race

**Basis:** [crafter menu validity window](../exploits/crafter-menu-validity-window.md). A local server test disabled a crafter slot after the player had moved 10.5 blocks away and the menu had become invalid; the change persisted after the menu closed on the next tick.

A client could keep the crafter menu ID, perform a short accepted movement excursion, and send slot-state packets before the server's next menu-validity check. The narrow use is toggling empty slots of a crafter the player just opened, for example to alter an exposed automation setup while leaving the area. The implementation should verify the menu ID, slot state, and server confirmation, then stop when the menu closes.

The window is one server tick in the tested setup. The handler changes only empty crafter slots and grants no item or permission bypass. Remote-client timing and plugin checks remain untested.
