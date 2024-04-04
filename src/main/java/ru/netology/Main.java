package ru.netology;

import server.Request;
import server.Server;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class Main {
    public static List<String> pages = List.of("/index.html", "/events.html", "/forms.html", "/links.html", "/resources.html");

    public static void main(String[] args) {
        final var server = new Server(5000);

        pages.forEach(page -> {
            server.addHandler("GET", page, Main::getPage);
        });

        server.addHandler("GET", "/classic.html", (Request request, BufferedOutputStream response) -> {
            final var filePath = Path.of(".", "public", request.PATH);
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

    public static void getPage(Request request, BufferedOutputStream response) throws IOException {
        final var filePath = Path.of(".", "public", request.PATH);
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


