package ru.netology;

import ru.netology.server.Request;
import ru.netology.server.Server;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class Main {
    public static List<String> pages = List.of("/index.html", "/events.html", "/forms.html", "/links.html", "/resources.html");
    public static List<String> resources = List.of("/app.js", "/events.js", "/spring.png", "/spring.svg", "/styles.css");

    public static void main(String[] args) {
        final var server = new Server(8080);

        pages.forEach(page -> {
            server.addHandler("GET", page, Main::send);
        });
        resources.forEach(resource -> {
            server.addHandler("GET", resource, Main::send);
        });

        server.addHandler("POST", "/index.html", Main::send);

        server.addHandler("GET", "/classic.html", (Request request, BufferedOutputStream response) -> {
            final var filePath = Path.of(".", "public", request.getPath());
            final var mimeType = Files.probeContentType(filePath);
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            response.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            response.write(content);
            response.flush();
        });

        server.listen();

    }

    public static void send(Request request, BufferedOutputStream response) throws IOException {
        final var filePath = Path.of(".", "public", request.getPath());
        final var mimeType = Files.probeContentType(filePath);
        final var length = Files.size(filePath);
        response.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(filePath, response);
        response.flush();
    }
}


