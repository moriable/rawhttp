package rawhttp.core;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * A {@link RawHttpResponse}'s start-line.
 */
public class StatusLine implements StartLine {

    private final HttpVersion httpVersion;
    private final int statusCode;
    private final String reason;

    public StatusLine(HttpVersion httpVersion, int statusCode, String reason) {
        this.httpVersion = httpVersion;
        this.statusCode = statusCode;
        this.reason = reason;
    }

    @Override
    public HttpVersion getHttpVersion() {
        return httpVersion;
    }

    /**
     * @return the status code in this status-code line.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @return the reason phrase in this status-code line.
     */
    public String getReason() {
        return reason;
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        byte[] bytes = toString().getBytes(StandardCharsets.UTF_8);
        outputStream.write(bytes);
        outputStream.write('\r');
        outputStream.write('\n');
    }

    /**
     * @return the start-line for this status-code line.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(httpVersion);
        builder.append(' ');
        builder.append(statusCode);
        if (!reason.isEmpty()) {
            builder.append(' ');
            builder.append(reason);
        }
        return builder.toString();
    }

}
