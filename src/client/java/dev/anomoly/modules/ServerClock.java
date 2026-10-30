package dev.anomoly.modules;

/** Timestamps of server traffic, fed by packet mixins and read by lag and TPS displays. */
public final class ServerClock {
    private static volatile long lastPacketNanos = System.nanoTime();
    private static long lastTimePacketNanos;
    private static double tps = 20.0;

    private ServerClock() { }

    public static void packetReceived() { lastPacketNanos = System.nanoTime(); }

    /** Called for each world time update, which a server sends once per 20 of its ticks. */
    public static synchronized void timeUpdate() {
        long now = System.nanoTime();
        if (lastTimePacketNanos != 0) {
            double seconds = (now - lastTimePacketNanos) / 1.0E9;
            if (seconds > 0.1) tps = Math.min(20.0, 20.0 / seconds * 0.5 + tps * 0.5);
        }
        lastTimePacketNanos = now;
    }

    public static synchronized void reset() {
        lastTimePacketNanos = 0;
        tps = 20.0;
        lastPacketNanos = System.nanoTime();
    }

    public static double secondsSincePacket() { return (System.nanoTime() - lastPacketNanos) / 1.0E9; }

    public static synchronized double tps() { return tps; }
}
