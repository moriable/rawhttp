package rawhttp.core.body;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;

import jdk.internal.net.http.frame.OutgoingHeaders;
import rawhttp.core.RawHttpHeaders;
import rawhttp.core.Writable;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Container of a HTTP message body which has the "chunked" transfer-coding.
 * <p>
 * See <a href="https://tools.ietf.org/html/rfc7230#section-4.1">Section 4.1</a>
 * of RFC-7230 for details.
 */
public class ChunkedBodyContents implements Writable {

    /**
     * A single chunk, part of {@link ChunkedBodyContents}.
     */
    public static class Chunk implements Writable {

        private final RawHttpHeaders extensions;
        private final byte[] data;

        public Chunk(RawHttpHeaders extensions, byte[] data) {
            this.extensions = extensions;
            this.data = data;
        }

        /**
         * @return the chunk extensions
         */
        public RawHttpHeaders getExtensions() {
            return extensions;
        }

        /**
         * @return the data contained in this chunk
         */
        public byte[] getData() {
            return data;
        }

        /**
         * @return the size of this chunk
         */
        public int size() {
            return data.length;
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            writeTo(new OutputStream[] {out});
        }

        @Override
        public void writeTo(OutputStream[] outputStream) throws IOException {
            for (OutputStream out : outputStream) {
                out.write(Integer.toString(size(), 16).getBytes());
                getExtensions().forEachIO((name, value) -> {
                    out.write(';');
                    out.write(name.getBytes(UTF_8));
                    if (!value.isEmpty()) {
                        out.write('=');
                        out.write(value.getBytes(UTF_8));
                    }
                });
                out.write('\r');
                out.write('\n');
                if (size() > 0) {
                    out.write(getData());
                    out.write('\r');
                    out.write('\n');
                }
            }
        }
    }

    private final List<Chunk> chunks;
    private final RawHttpHeaders trailerHeaders;

    public ChunkedBodyContents(List<Chunk> chunks, RawHttpHeaders trailerHeaders) {
        this.chunks = chunks;
        this.trailerHeaders = trailerHeaders;
    }

    /**
     * @return the chunks that make up the chunked body.
     */
    public List<Chunk> getChunks() {
        return chunks;
    }

    /**
     * @return the trailing headers included in the chunked body.
     */
    public RawHttpHeaders getTrailerHeaders() {
        return trailerHeaders;
    }

    /**
     * @return the total size of the body, including all chunks.
     */
    public long size() {
        return chunks.stream().mapToLong(Chunk::size).sum();
    }

    /**
     * @return the message body (after decoding).
     */
    public byte[] getData() {
        long totalSize = size();

        // this will result in an ArithmeticException if the totalSize does not fit into an int
        byte[] result = new byte[Math.toIntExact(totalSize)];
        int offset = 0;
        for (Chunk chunk : chunks) {
            System.arraycopy(chunk.data, 0, result, offset, chunk.size());
            offset += chunk.size();
        }
        return result;
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        writeTo(new OutputStream[]{outputStream});
    }

    @Override
    public void writeTo(OutputStream[] outputStream) throws IOException {
        for (OutputStream stream : outputStream) {
            for (Chunk chunk : chunks) {
                chunk.writeTo(stream);
            }
            trailerHeaders.writeTo(stream);
        }
    }

    /**
     * Convert the decoded body to a String using the provided charset.
     *
     * @param charset body's charset
     * @return decoded body as a String
     */
    public String asString(Charset charset) {
        return new String(getData(), charset);
    }

    @Override
    public String toString() {
        return asString(UTF_8);
    }
}
