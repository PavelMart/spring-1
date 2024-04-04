package server;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    final List<String> validPaths = List.of(
            "/index.html",
            "/spring.svg",
            "/spring.png",
            "/resources.html",
            "/styles.css",
            "/app.js",
            "/links.html",
            "/forms.html",
            "/classic.html",
            "/events.html",
            "/events.js"
    );

    private final int PORT;

    private Map<String, Handler> GET_handlers = new HashMap<>();
    private Map<String, Handler> POST_handlers = new HashMap<>();
    private Map<String, Handler> PUT_handlers = new HashMap<>();
    private Map<String, Handler> DELETE_handlers = new HashMap<>();
    private final ExecutorService threadPool = Executors.newFixedThreadPool(64);

    public Server(int port) {
        PORT = port;
    }

    public void addHandler(String method, String path, Handler handler) {
        switch (method) {
            case "GET":
                GET_handlers.put(path, handler);
                break;
            case "POST":
                POST_handlers.put(path, handler);
                break;
            case "PUT":
                PUT_handlers.put(path, handler);
                break;
            case "DELETE":
                DELETE_handlers.put(path, handler);
                break;
        }
    }

    public boolean validateRequest(String requestLine) {
        if (requestLine == null) {
            return false;
        }

        if (requestLine.split(" ").length != 3) {
            return false;
        }

        return true;
    }

    public void handleSocket(Socket socket) {
        try (
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            final var requestLine = in.readLine();

            if (!validateRequest(requestLine)) {
                return;
            }

            StringBuilder headersBuilder = new StringBuilder();

            String line;
            while (true) {
                line = in.readLine();
                if (line == null || line.isEmpty()) {
                    break;
                }
                headersBuilder.append(line).append("\r\n");
            }

            String headers = headersBuilder.toString();

            final var parts = requestLine.split(" ");

            final var path = parts[1];
            if (!validPaths.contains(path)) {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
                return;
            }

            var method = parts[0];
            var requestPath = parts[1];

            var request = new Request(method, requestPath, headers);

            switch (method) {
                case "GET":
                    GET_handlers.get(requestPath).handle(request, out);
                    break;
                case "POST":
                    POST_handlers.get(requestPath).handle(request, out);
                    break;
                case "PUT":
                    PUT_handlers.get(requestPath).handle(request, out);
                    break;
                case "DELETE":
                    DELETE_handlers.get(requestPath).handle(request, out);
                    break;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void listen() {
        try (final var serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server has been started on PORT: " + PORT);
            while (true) {
                final var socket = serverSocket.accept();

                threadPool.execute(() -> handleSocket(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
