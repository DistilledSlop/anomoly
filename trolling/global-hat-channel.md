# Global hat toggle as a tab-list signal

**Status:** Source-confirmed for vanilla 26.3. No packet-capture test has been run for this idea. Skin blinking itself exists in mainstream clients; the distinct application here is a cross-dimension signal carried by the **tab-list hat bit**.

The server's `ServerGamePacketListenerImpl.handleClientInformation` compares the old and new `HAT` model-part bit and, when it changes, calls `PlayerList.broadcastAll` with `ClientboundPlayerInfoUpdatePacket.Action.UPDATE_HAT`. There is no distance or dimension filter in this handler. The receiving client updates `PlayerInfo.showHat`, and `PlayerTabOverlay` uses that flag when drawing the face icon. A cooperating client can decode a sequence of toggles even when the two players are far apart or in different dimensions, without chat or a plugin channel. A vanilla player who keeps the tab list open may see the hat layer flicker.

This is low bandwidth and visible in network traffic. The code does not contain a rate limiter in this handler, but network or server plugins may enforce one, and a large stream would be noisy. The ability to signal does not imply privilege gain, item creation, or control over another player's appearance beyond the sender's own tab face.

**Source anchors:** `ServerGamePacketListenerImpl#handleClientInformation` near line 2114, `ClientPacketListener#handlePlayerInfoUpdate` near line 1972, and `PlayerTabOverlay` near line 207. The same broadcast path exists in 26.2, so it is a new use case in this notebook, not a 26.3 regression.
