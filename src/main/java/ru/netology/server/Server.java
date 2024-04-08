package ru.netology.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String PUT = "PUT";
    public static final String DELETE = "DELETE";
    public static final List<String> allowedMethods = List.of(GET, POST, PUT, DELETE);

    private final Map<String, ConcurrentHashMap<Pattern, Handler>> handlers = new ConcurrentHashMap<>();
    private final ExecutorService threadPool = Executors.newFixedThreadPool(64);

    public Server(int port) {
        PORT = port;
    }

    public void addHandler(String method, String path, Handler handler) {
        if (!handlers.containsKey(method)) {
            handlers.put(method, new ConcurrentHashMap<>(){{put(Pattern.compile(path), handler);}});
        } else {
            var doHandlers = handlers.get(method);
            doHandlers.put(Pattern.compile(path), handler);
        }
    }

    public void handleSocket(Socket socket) {

        try (
                final var in = new BufferedInputStream(socket.getInputStream());
                final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            var request = new Request(in, out);

            var path = request.getPath();
            var method = request.getMethod();

            var doHandlers = handlers.get(method);

            for(Map.Entry<Pattern, Handler> entry : doHandlers.entrySet()) {
                Matcher matcher = entry.getKey().matcher(path);
                if (matcher.matches()) {
                    entry.getValue().handle(request, out);
                    return;
                }
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

    public static void badRequest(BufferedOutputStream out, String message) throws IOException {
        var body = "{\r\n" +
                "\"message\": " + message + "\r\n" +
                "}";
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: " + body.length() + "\r\n" +
                        "Connection: close\r\n" +
                        "Content-Type: application/json" +
                        "\r\n\r\n" +
                        body


        ).getBytes());
        out.flush();
    }

    public static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
