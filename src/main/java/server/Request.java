package server;

import java.net.Socket;
import java.util.Map;

public class Request {
    public String METHOD;
    public String PATH;
    public String HEADERS;
    public String BODY;

    public Request(String method, String path, String headers) {
        METHOD = method;
        PATH = path;
        HEADERS = headers;
    }

    public Request(String method, String path, String headers, String body) {
        METHOD = method;
        PATH = path;
        HEADERS = headers;
        BODY = body;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(METHOD);
        builder.append(" ");
        builder.append(PATH);
        builder.append("\r\n");
        builder.append(HEADERS);

        if (BODY != null) {
            builder.append("\r\n");
            builder.append(BODY);
        }

        return builder.toString();
    }
}
