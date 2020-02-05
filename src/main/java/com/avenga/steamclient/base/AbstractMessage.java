package com.avenga.steamclient.base;

import com.avenga.steamclient.util.stream.BinaryReader;
import com.avenga.steamclient.util.stream.BinaryWriter;
import com.avenga.steamclient.util.stream.MemoryStream;
import com.avenga.steamclient.enums.SeekOrigin;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * This class provides a payload backing to client messages.
 */
public abstract class AbstractMessage {

    protected static final int DEFAULT_PAYLOAD_RESERVE = 0;

    protected MemoryStream payload;
    private final BinaryReader reader;
    private final BinaryWriter writer;

    /**
     * Initializes a new instance of the {@link AbstractMessage} class.
     */
    public AbstractMessage() {
        this(DEFAULT_PAYLOAD_RESERVE);
    }

    /**
     * Initializes a new instance of the {@link AbstractMessage} class.
     *
     * @param payloadReserve The number of bytes to initialize the payload capacity to.
     */
    public AbstractMessage(int payloadReserve) {
        payload = new MemoryStream(payloadReserve);

        reader = new BinaryReader(payload);
        writer = new BinaryWriter(payload.asOutputStream());
    }

    /**
     * Seeks within the payload to the specified offset.
     *
     * @param offset     The offset in the payload to seek to.
     * @param seekOrigin The origin to seek from.
     * @return The new position within the stream, calculated by combining the initial reference point and the offset.
     */
    public long seek(long offset, SeekOrigin seekOrigin) {
        return payload.seek(offset, seekOrigin);
    }

    public void write(byte data) throws IOException {
        writer.write(data);
    }

    public void write(short data) throws IOException {
        writer.writeShort(data);
    }

    public void write(int data) throws IOException {
        writer.writeInt(data);
    }

    public void write(long data) throws IOException {
        writer.writeLong(data);
    }

    public void write(byte[] data) throws IOException {
        writer.write(data);
    }

    public void write(float data) throws IOException {
        writer.writeFloat(data);
    }

    public void write(double data) throws IOException {
        writer.writeDouble(data);
    }

    public void write(String data) throws IOException {
        write(data, Charset.defaultCharset());
    }

    public void write(String data, Charset charset) throws IOException {
        if (data == null) {
            return;
        }

        Objects.requireNonNull(charset, "charset wasn't provided");

        write(data.getBytes(charset));
    }

    public void writeNullTermString(String data) throws IOException {
        writeNullTermString(data, Charset.defaultCharset());
    }

    public void writeNullTermString(String data, Charset charset) throws IOException {
        write(data, charset);
        write("\0", charset);
    }

    public byte readByte() throws IOException {
        return reader.readByte();
    }

    public byte[] readBytes(int numBytes) throws IOException {
        return reader.readBytes(numBytes);
    }

    public short readShort() throws IOException {
        return reader.readShort();
    }

    public int readInt() throws IOException {
        return reader.readInt();
    }

    public long readLong() throws IOException {
        return reader.readLong();
    }

    public float readFloat() throws IOException {
        return reader.readFloat();
    }

    public double readDouble() throws IOException {
        return reader.readDouble();
    }

    public String readNullTermString() throws IOException {
        return readNullTermString(Charset.defaultCharset());
    }

    public String readNullTermString(Charset charset) throws IOException {
        return reader.readNullTermString(charset);
    }

    public MemoryStream getPayload() {
        return payload;
    }
}
