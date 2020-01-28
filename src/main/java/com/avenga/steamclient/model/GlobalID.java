package com.avenga.steamclient.model;

import java.util.Date;
import java.util.Objects;

/**
 * Represents a globally unique identifier within the Steam network.
 * Guaranteed to be unique across all racks and servers for a given Steam universe.
 */
public class GlobalID {
    private static final long DEFAULT_GLOBAL_ID = 0xFFFFFFFFFFFFFFFFL;
    private static final long SEQUENTIAL_MASK = 0xFFFFFL;
    private static final short SEQUENTIAL_BIT_OFFSET = 0;
    private static final long MILLISECOND_MULTIPLIER = 1000L;
    private static final long START_TIME_MASK = 0x3FFFFFFFL;
    private static final short START_TIME_BIT_OFFSET = 20;
    private static final long PROCESS_ID_MASK = 0xFL;
    private static final short PROCESS_ID_BIT_OFFSET = 50;
    private static final long BOX_ID_MASK = 0x3FFL;
    private static final short BOX_ID_BIT_OFFSET = 54;
    private static final long INITIAL_SERVER_TIME = 1104537600000L;

    private BitVector64 gidBits;

    /**
     * Initializes a new instance of the {@link GlobalID} class.
     */
    public GlobalID() {
        this(DEFAULT_GLOBAL_ID);
    }

    /**
     * Initializes a new instance of the {@link GlobalID} class.
     *
     * @param gid The GID value.
     */
    public GlobalID(long gid) {
        gidBits = new BitVector64(gid);
    }

    /**
     * Sets the sequential count for this GID.
     *
     * @param value The sequential count.
     */
    public void setSequentialCount(long value) {
        gidBits.setMask(SEQUENTIAL_BIT_OFFSET, SEQUENTIAL_MASK, value);
    }

    /**
     * Gets the sequential count for this GID.
     *
     * @return The sequential count.
     */
    public long getSequentialCount() {
        return gidBits.getMask(SEQUENTIAL_BIT_OFFSET, SEQUENTIAL_MASK);
    }

    /**
     * Sets the start time of the server that generated this GID.
     *
     * @param startTime The start time.
     */
    public void setStartTime(Date startTime) {
        long startTimeSeconds = (startTime.getTime() - INITIAL_SERVER_TIME) / MILLISECOND_MULTIPLIER;
        gidBits.setMask(START_TIME_BIT_OFFSET, START_TIME_MASK, startTimeSeconds);
    }

    /**
     * Gets the start time of the server that generated this GID.
     *
     * @return The start time.
     */
    public Date getStartTime() {
        long startTimeSeconds = gidBits.getMask(START_TIME_BIT_OFFSET, START_TIME_MASK);
        return new Date(startTimeSeconds * MILLISECOND_MULTIPLIER);
    }

    /**
     * Sets the process ID of the server that generated this GID.
     *
     * @param value The process ID.
     */
    public void setProcessID(long value) {
        gidBits.setMask(PROCESS_ID_BIT_OFFSET, PROCESS_ID_MASK, value);
    }

    /**
     * Gets the process ID of the server that generated this GID.
     *
     * @return The process ID.
     */
    public long getProcessID() {
        return gidBits.getMask(PROCESS_ID_BIT_OFFSET, PROCESS_ID_MASK);
    }

    /**
     * Sets the box ID of the server that generated this GID.
     *
     * @param value The box ID.
     */
    public void setBoxID(long value) {
        gidBits.setMask(BOX_ID_BIT_OFFSET, BOX_ID_MASK, value);
    }

    /**
     * Gets the box ID of the server that generated this GID.
     *
     * @return The box ID.
     */
    public long getBoxID() {
        return gidBits.getMask(BOX_ID_BIT_OFFSET, BOX_ID_MASK);
    }

    /**
     * Sets the entire 64bit value of this GID.
     *
     * @param value The value.
     */
    public void setValue(long value) {
        gidBits.setData(value);
    }

    /**
     * Sets the entire 64bit value of this GID.
     *
     * @return The value.
     */
    public long getValue() {
        return gidBits.getData();
    }

    /**
     * Determines whether the specified {@link Object} is equal to this instance.
     *
     * @param obj The {@link Object} to compare with this instance.
     * @return <b>true</b> if the specified {@link Object} is equal to this instance; otherwise, <b>false</b>.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof GlobalID)) {
            return false;
        }

        return Objects.equals(gidBits.getData(), ((GlobalID) obj).gidBits.getData());
    }

    /**
     * Returns a hash code for this instance.
     *
     * @return A hash code for this instance, suitable for use in hashing algorithms and data structures like a hash table.
     */
    @Override
    public int hashCode() {
        return gidBits.getData().hashCode();
    }

    /**
     * Returns a {@link String} that represents this instance.
     *
     * @return A {@link String} that represents this instance.
     */
    @Override
    public String toString() {
        return String.valueOf(getValue());
    }
}
