package com.github.ssquadteam.fawe.compat;

import com.github.ssquadteam.fawe.scheduler.FaweScheduler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

/**
 * Block-capture state for a regionised server.
 *
 * <p>
 * CraftBukkit collects the blocks a feature places by flipping a pair of flags on the world and reading back the map
 * they fill. A regionised server cannot keep that state on the world - two regions may be generating at once - so it
 * moved all three onto the per-region world data, reachable through {@code Level#getCurrentWorldData()} on the thread
 * that owns the region. The adapters cannot name those types: they compile against Paper, where neither the method nor
 * the class exists, so the members are looked up once by reflection and only ever touched on a regionised server.
 * </p>
 *
 * <p>
 * Every method here has to be called from the thread ticking the region that owns the blocks being generated, which is
 * what {@link com.github.ssquadteam.fawe.scheduler.RegionSync} arranges for the callers.
 * </p>
 */
public final class RegionisedCapture {

    private static final Method GET_CURRENT_WORLD_DATA;
    private static final Field CAPTURE_BLOCK_STATES;
    private static final Field CAPTURE_TREE_GENERATION;
    private static final Field CAPTURED_BLOCK_STATES;

    static {
        Method getCurrentWorldData = null;
        Field captureBlockStates = null;
        Field captureTreeGeneration = null;
        Field capturedBlockStates = null;
        if (FaweScheduler.isFolia()) {
            try {
                Class<?> level = Class.forName("net.minecraft.world.level.Level");
                getCurrentWorldData = level.getMethod("getCurrentWorldData");
                Class<?> worldData = getCurrentWorldData.getReturnType();
                captureBlockStates = worldData.getField("captureBlockStates");
                captureTreeGeneration = worldData.getField("captureTreeGeneration");
                capturedBlockStates = worldData.getField("capturedBlockStates");
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
        GET_CURRENT_WORLD_DATA = getCurrentWorldData;
        CAPTURE_BLOCK_STATES = captureBlockStates;
        CAPTURE_TREE_GENERATION = captureTreeGeneration;
        CAPTURED_BLOCK_STATES = capturedBlockStates;
    }

    private RegionisedCapture() {
    }

    /**
     * Start capturing the blocks placed on the region that owns the current thread.
     *
     * @param level the {@code ServerLevel} being generated into
     */
    public static void preCapture(Object level) {
        Object worldData = worldData(level);
        set(CAPTURE_TREE_GENERATION, worldData, true);
        set(CAPTURE_BLOCK_STATES, worldData, true);
    }

    /**
     * Get the blocks captured so far on the region that owns the current thread.
     *
     * @param level the {@code ServerLevel} being generated into
     * @return the captured {@code CraftBlockState}s, in the map's order
     */
    public static Collection<?> capturedBlockStates(Object level) {
        return captured(worldData(level)).values();
    }

    /**
     * Stop capturing and drop what was captured on the region that owns the current thread.
     *
     * @param level the {@code ServerLevel} being generated into
     */
    public static void postCapture(Object level) {
        Object worldData = worldData(level);
        set(CAPTURE_BLOCK_STATES, worldData, false);
        set(CAPTURE_TREE_GENERATION, worldData, false);
        captured(worldData).clear();
    }

    private static Object worldData(Object level) {
        try {
            return GET_CURRENT_WORLD_DATA.invoke(level);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not reach the region's world data", e);
        }
    }

    private static Map<?, ?> captured(Object worldData) {
        try {
            return (Map<?, ?>) CAPTURED_BLOCK_STATES.get(worldData);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not read the region's captured block states", e);
        }
    }

    private static void set(Field field, Object worldData, boolean value) {
        try {
            field.setBoolean(worldData, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not set " + field.getName() + " on the region's world data", e);
        }
    }

}
