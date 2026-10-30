package dev.anomoly.modules;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;

/** Ordered queue of packets with release times. */
final class PacketDelay {
    private record Held(Packet<?> packet, long dueMillis) { }

    private final Deque<Held> queue = new ArrayDeque<>();

    void hold(Packet<?> packet, long delayMillis) {
        queue.addLast(new Held(packet, System.currentTimeMillis() + delayMillis));
    }

    boolean empty() { return queue.isEmpty(); }

    int size() { return queue.size(); }

    void releaseDue(Minecraft client) {
        long now = System.currentTimeMillis();
        while (!queue.isEmpty() && queue.peekFirst().dueMillis() <= now) PacketInterceptor.replay(client, queue.pollFirst().packet());
    }

    void releaseAll(Minecraft client) {
        while (!queue.isEmpty()) PacketInterceptor.replay(client, queue.pollFirst().packet());
    }

    void clear() { queue.clear(); }
}
