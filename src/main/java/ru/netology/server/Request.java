package ru.netology.server;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public class Request {
    private String method;
    private String path;
    private List<String> headers;
    private List<NameValuePair> queryParams;
    private String body;

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

        final var reqMethod = requestLine[0];
        if (!Server.allowedMethods.contains(reqMethod)) {
            Server.badRequest(out, "Method don't allowed");
            return;
        }
        method = reqMethod;

        final var reqPath = requestLine[1];
        if (!reqPath.startsWith("/")) {
            Server.badRequest(out, "Incorrect request path");
            return;
        }

        var uriBuilder = new URIBuilder(reqPath);

        path = uriBuilder.getPath();
        queryParams = uriBuilder.getQueryParams();

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
        headers = Arrays.asList(new String(headersBytes).split("\r\n"));
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List<NameValuePair> getQueryParam(String name) {
        if (queryParams == null) {
            return null;
        }
        return queryParams.stream()
                .filter(entry -> name.equals(entry.getName()))
                .toList();
    }

    public List<NameValuePair> getQueryParams() {
        return queryParams;
    }


    public List<String> getHeaders() {
        return headers;
    }
    public String getBody() {
        return body;
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(method);
        builder.append(" ");
        builder.append(path);
        builder.append("\r\n");
        builder.append(headers);
        headers.forEach(header -> {
            builder.append(header).append("\r\n");
        });

        if (body != null) {
            builder.append("\r\n");
            builder.append(body);
        }

        return builder.toString();
    }


}
