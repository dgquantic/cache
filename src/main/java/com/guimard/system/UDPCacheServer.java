package com.guimard.system;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

public class UDPCacheServer {
	private static final int PORT = 9876;
	private static final int BUFFER_SIZE = 1024;
	private static final String AUTH_USERNAME = "admin";
	private static final String AUTH_PASSWORD = "password";
	private final SimpleCache cache;
	private final ConcurrentHashMap<String, Boolean> authenticatedClients;

	public UDPCacheServer() {
		this.cache = new SimpleCache(300_000);
		this.authenticatedClients = new ConcurrentHashMap<>();
		System.out.println("UDPCacheServer: Constructor called.");
	}

	public void start() throws IOException {
		try (DatagramSocket socket = new DatagramSocket(PORT)) {
			System.out.println("Cache Server started on port " + PORT);
			byte[] buffer = new byte[BUFFER_SIZE];

			while (true) {
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				System.out.println("UDPCacheServer: Waiting for packet...");
				socket.receive(packet);
				System.out.println("UDPCacheServer: Packet received!");

				InetAddress clientAddress = packet.getAddress();
				int clientPort = packet.getPort();
				String clientIdentifier = clientAddress.getHostAddress() + ":" + clientPort;

				Thread.ofVirtual().start(() -> handleClient(socket, clientAddress, clientPort, packet, clientIdentifier));
			}
		} catch (SocketException e) {
			System.err.println("Socket error: " + e.getMessage());
		}
	}

	private void handleClient(DatagramSocket socket, InetAddress clientAddress, int clientPort, DatagramPacket firstPacket, String clientIdentifier) {
		System.out.println("UDPCacheServer: handleClient started for " + clientIdentifier);
		byte[] buffer = new byte[BUFFER_SIZE];
		boolean firstRequest = true;

		try {
			while (true) {
				DatagramPacket packet = firstPacket;
				if (!firstRequest) {
					packet = new DatagramPacket(buffer, buffer.length);
					System.out.println("UDPCacheServer: handleClient: Waiting for packet from client...");
					try {
						socket.receive(packet);
					} catch (IOException e) {
						System.err.println("Socket is closed: " + e.getMessage());
						return;
					}
				}
				firstRequest = false;

				String request = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
				System.out.println("Received request from " + clientIdentifier + ": " + request);

				if (!authenticatedClients.getOrDefault(clientIdentifier, false)) {
					System.out.println("UDPCacheServer: handleClient: Checking authentication...");
					if (isAuthenticated(request)) {
						authenticatedClients.put(clientIdentifier, true);
						System.out.println("UDPCacheServer: handleClient: Authentication successful.");
						sendResponse("OK", clientAddress, clientPort, socket);
					} else {
						System.out.println("UDPCacheServer: handleClient: Authentication failed.");
						sendResponse("ERROR: Unauthorized", clientAddress, clientPort, socket);
						return;
					}
				} else {
					System.out.println("UDPCacheServer: handleClient: Handling authenticated request...");
					handleRequest(request, clientAddress, clientPort, socket);
				}
				java.util.Arrays.fill(buffer, (byte) 0);
			}
		} catch (Exception e) {
			System.err.println("Error handling client: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void handleRequest(String request, InetAddress clientAddress, int clientPort, DatagramSocket socket) throws IOException {
		System.out.println("UDPCacheServer: handleRequest called with: " + request);
		String response;
		String[] parts = request.split(" ");
		if (parts.length < 1) {
			response = "ERROR: Invalid request format";
		} else {
			String command = parts[0].toUpperCase();
			String key = (parts.length > 1) ? parts[1] : null;
			String value = (parts.length > 2) ? parts[2] : null;

			switch (command) {
				case "GET":
					if (key != null) {
						response = cache.get(key);
						System.out.println("UDPCacheServer: handleRequest: GET result: " + response);
						if (response == null) {
							response = "NOT FOUND";
						}
					} else {
						response = "ERROR: Key not provided for GET";
					}
					break;
				case "SET":
					if (key != null && value != null) {
						cache.set(key, value);
						System.out.println("UDPCacheServer: handleRequest: SET successful.");
						response = "OK";
					} else {
						response = "ERROR: Key or Value not provided for SET";
					}
					break;
				case "DELETE":
					if (key != null) {
						cache.delete(key);
						System.out.println("UDPCacheServer: handleRequest: DELETE successful.");
						response = "OK";
					} else {
						response = "ERROR: Key not provided for DELETE";
					}
					break;
				default:
					response = "ERROR: Unknown command";
			}
		}

		sendResponse(response, clientAddress, clientPort, socket);
	}

	private boolean isAuthenticated(String request) {
		System.out.println("UDPCacheServer: isAuthenticated called with: " + request);
		String[] parts = request.split(" ");
		if (parts.length >= 3 && "AUTH".equalsIgnoreCase(parts[0]) && "Basic".equalsIgnoreCase(parts[1])) {
			String base64Credentials = parts[2];
			String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
			String[] userPass = credentials.split(":");
			if (userPass.length == 2) {
				String username = userPass[0];
				String password = userPass[1];
				System.out.println("UDPCacheServer: isAuthenticated: Comparing " + username + "/" + password);
				return AUTH_USERNAME.equals(username) && AUTH_PASSWORD.equals(password);
			}
		}
		return false;
	}

	private void sendResponse(String response, InetAddress clientAddress, int clientPort, DatagramSocket socket) throws IOException {
		System.out.println("UDPCacheServer: sendResponse called with: " + response);
		byte[] data = response.getBytes(StandardCharsets.UTF_8);
		DatagramPacket packet = new DatagramPacket(data, data.length, clientAddress, clientPort);
		socket.send(packet);
	}

	public static void main(String[] args) throws IOException {
		UDPCacheServer server = new UDPCacheServer();
		server.start();
	}
}
