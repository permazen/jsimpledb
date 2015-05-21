
/*
 * Copyright (C) 2014 Archie L. Cobbs. All rights reserved.
 *
 * $Id$
 */

package org.jsimpledb.kv.raft;

import com.google.common.base.Preconditions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;

/**
 * Utility methods.
 */
final class Util {

    /**
     * Minimum file size to use memory mapping.
     */
    public static final long MIN_MAP_SIZE = 1024 * 1024;                     // 1MB

    /**
     * Minimum buffer size to use a direct buffer.
     */
    public static final int MIN_DIRECT_BUFFER_SIZE = 128;

    private Util() {
    }

    /**
     * Close object if it implements {@link AutoCloseable}, otherwise do nothing. Any exceptions thrown are ignored.
     *
     * @param obj object to close
     */
    public static void closeIfPossible(Object obj) {
        if (obj instanceof AutoCloseable) {
            try {
                ((AutoCloseable)obj).close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    /**
     * Allocate a {@link ByteBuffer}.
     *
     * @param capacity capacity of buffer
     * @return new buffer with the given capacity
     */
    public static ByteBuffer allocateByteBuffer(int capacity) {
        return capacity >= MIN_DIRECT_BUFFER_SIZE ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity);
    }

    /**
     * Get the length of a file.
     *
     * @param file file to measure
     * @return file length
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if {@code file} is null
     */
    public static long getLength(File file) throws IOException {
        Preconditions.checkArgument(file != null, "null file");
        return (Long)Files.getAttribute(file.toPath(), "size");
    }

    /**
     * Read a non-empty {@link File} into a buffer.
     *
     * @param file file to read
     * @param length file's length, or -1 if unknown
     * @return new buffer containing the contents of {@code file}
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if {@code file} is null
     */
    public static ByteBuffer readFile(File file, long length) throws IOException {

        // Get file length, if not provided already
        if (length < 0)
            length = Util.getLength(file);

        // Read file
        try (FileInputStream input = new FileInputStream(file)) {

            // Just memory map the file if it's sufficiently large
            if (length >= MIN_MAP_SIZE || length != (int)length)
                return input.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, length);

            // Read file into allocated buffer
            final ByteBuffer buf = Util.allocateByteBuffer((int)length);
            while (buf.hasRemaining()) {
                final int numRead = input.getChannel().read(buf);
                if (numRead == -1)
                    throw new IOException("file length was " + length + " but only read " + buf.position() + " bytes");
            }
            return (ByteBuffer)buf.flip();
        }
    }

    /**
     * Mask a checked exception.
     *
     * @param t any exception
     * @param <T> exception type
     * @return never
     * @throws T always
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> T maskException(Throwable t) throws T {
        throw (T)t;
    }

    /**
     * Prepend stack frames from the current thread onto the given exception's stack frames.
     *
     * @param t exception from another thread
     * @param description description of the operation (will appear in the intermediate "glue" stack frame)
     */
    public static void prependCurrentStackTrace(Throwable t, String description) {
        Util.prependStackFrames(t, new Throwable(), description, 1);
    }

    private static void prependStackFrames(Throwable t, Throwable p, String description, int skip) {
        final StackTraceElement[] innerFrames = t.getStackTrace();
        final StackTraceElement[] outerFrames = p.getStackTrace();
        final StackTraceElement[] frames = new StackTraceElement[innerFrames.length + 1 + Math.max(outerFrames.length - skip, 0)];
        System.arraycopy(innerFrames, 0, frames, 0, innerFrames.length);
        frames[innerFrames.length] = new StackTraceElement(Util.class.getName(), "prependStackFrames", description, -1);
        for (int i = 0; i < outerFrames.length - skip; i++)
            frames[innerFrames.length + 1 + i] = outerFrames[i + skip];
        t.setStackTrace(frames);
    }
}

