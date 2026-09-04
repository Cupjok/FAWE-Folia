package com.github.ssquadteam.fawe.scheduler;

/**
 * A tick counter the platform adapters can read from any thread.
 *
 * <p>
 * Paper keeps a single server-wide tick counter, and the adapters read it
 * straight off {@code MinecraftServer}. A regionised server has no such
 * counter: every region counts its own ticks, from the moment that region was
 * created, and the only accessor for it - {@code RegionizedServer#getCurrentTick}
 * - throws once the calling thread is not ticking a region, which is the normal
 * case for FAWE's own worker threads and for anything running at enable time.
 * </p>
 *
 * <p>
 * The adapters use the counter to notice that time has passed and flush what
 * they have cached, so what they need is a number that moves forward at roughly
 * tick rate and can be compared against itself from any thread. Elapsed
 * wall-clock time gives that, and unlike a region's counter it stays comparable
 * across an edit that spans several regions.
 * </p>
 */
public final class ServerTicks {

    private static final long NANOS_PER_TICK = 50_000_000L;

    private static final long START_NANOS = System.nanoTime();

    private ServerTicks() {
    }

    /**
     * Get the number of ticks' worth of wall-clock time that has passed since the plugin was loaded.
     *
     * @return a counter that advances at tick rate, readable from any thread
     */
    public static int elapsed() {
        return (int) ((System.nanoTime() - START_NANOS) / NANOS_PER_TICK);
    }

}
