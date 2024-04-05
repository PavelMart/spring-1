package server;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public class Request {
    private String METHOD;
    private String PATH;
    private List<String> HEADERS;
    private List<NameValuePair> QUERY_PARAMS;
    private String BODY;

    public Request(BufferedInputStream in, BufferedOutputStream out) throws IOException, URISyntaxException {
        final var limit = 4096;

        in.mark(limit);
        final var buffer = new byte[limit];
        final var read = in.read(buffer);

        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = Server.indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            Server.badRequest(out, "Can't read request line");
            return;
        }

        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            Server.badRequest(out, "Incorrect request line");
            return;
        }

        final var method = requestLine[0];
        if (!Server.allowedMethods.contains(method)) {
            Server.badRequest(out, "Method don't allowed");
            return;
        }
        METHOD = method;

        final var path = requestLine[1];
        if (!path.startsWith("/")) {
            Server.badRequest(out, "Incorrect request path");
            return;
        }

        var uriBuilder = new URIBuilder(path);

        PATH = uriBuilder.getPath();
        QUERY_PARAMS = uriBuilder.getQueryParams();

        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = Server.indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            Server.badRequest(out, "Incorrect headers");
            return;
        }

        in.reset();
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        HEADERS = Arrays.asList(new String(headersBytes).split("\r\n"));
//        System.out.println(this);
    }

    public String getMethod() {
        return METHOD;
    }

    public String getPath() {
        return PATH;
    }

    public List<String> getHeaders() {
        return HEADERS;
    }
    public String getBody() {
        return BODY;
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(METHOD);
        builder.append(" ");
        builder.append(PATH);
        builder.append("\r\n");
        HEADERS.forEach(header -> {
            builder.append(header).append("\r\n");
        });

        if (BODY != null) {
            builder.append("\r\n");
            builder.append(BODY);
        }

        return builder.toString();
    }


}
