package com.example.client;

import com.example.ExampleMod;
import net.minecraft.client.MinecraftClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class CommentServer {
	public static final int PORT = 8765;
	private static boolean started = false;

	public static synchronized void start() {
		if (started) {
			return;
		}
		started = true;
		Thread thread = new Thread(CommentServer::run, "comment-server");
		thread.setDaemon(true);
		thread.start();
	}

	private static void run() {
		try (ServerSocket server = new ServerSocket(PORT, 20, InetAddress.getLoopbackAddress())) {
			ExampleMod.LOGGER.info("Comment server listening on 127.0.0.1:{}", PORT);
			while (true) {
				try (Socket socket = server.accept()) {
					handle(socket);
				} catch (IOException ignored) {
				}
			}
		} catch (IOException e) {
			ExampleMod.LOGGER.warn("Comment server failed to start", e);
		}
	}

	private static void handle(Socket socket) throws IOException {
		socket.setSoTimeout(2000);
		BufferedReader reader = new BufferedReader(
			new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
		String line = reader.readLine();

		String text = extractText(line);
		if (text != null && !text.isBlank()) {
			String clean = text.replaceAll("[\\r\\n]+", " ").trim();
			MinecraftClient.getInstance().execute(() -> CommentSpawner.spawn(clean));
		}

		OutputStream out = socket.getOutputStream();
		out.write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nOK"
			.getBytes(StandardCharsets.UTF_8));
		out.flush();
	}

	// строка запроса выглядит так: GET /?t=%D0%9F HTTP/1.1
	private static String extractText(String requestLine) {
		if (requestLine == null) {
			return null;
		}
		String[] parts = requestLine.split(" ");
		if (parts.length < 2) {
			return null;
		}
		int q = parts[1].indexOf('?');
		if (q < 0) {
			return null;
		}
		for (String param : parts[1].substring(q + 1).split("&")) {
			if (param.startsWith("t=")) {
				return URLDecoder.decode(param.substring(2), StandardCharsets.UTF_8);
			}
		}
		return null;
	}
}
