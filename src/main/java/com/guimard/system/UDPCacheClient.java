package com.guimard.system;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class UDPCacheClient {
	private static final String SERVER_ADDRESS = "localhost";
	private static final int SERVER_PORT = 9876;
	private static final String USERNAME = "admin";
	private static final String PASSWORD = "password";

	public static void main(String[] args) {
		try (DatagramSocket socket = new DatagramSocket()) {
			InetAddress address = InetAddress.getByName(SERVER_ADDRESS);

			// 1. Authenticate
			String authRequest = "AUTH Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
			System.out.println("UDPCacheClient: Sending auth request: " + authRequest); // Log auth request
			sendRequest(socket, address, authRequest);
			String authResponse = receiveResponse(socket);
			System.out.println("UDPCacheClient: Auth Response: " + authResponse);

			if ("OK".equals(authResponse)) {
				// 2. Perform cache operations (SEPARATE requests)
				System.out.println("UDPCacheClient: Sending SET request..."); // Log SET request
				sendRequest(socket, address, "SET key1 value1");
				System.out.println("UDPCacheClient: SET Response: " + receiveResponse(socket));

				System.out.println("UDPCacheClient: Sending GET request..."); // Log GET request
				sendRequest(socket, address, "GET key1");
				System.out.println("UDPCacheClient: GET Response: " + receiveResponse(socket));

				System.out.println("UDPCacheClient: Sending DELETE request..."); // Log DELETE request
				sendRequest(socket, address, "DELETE key1");
				System.out.println("UDPCacheClient: DELETE Response: " + receiveResponse(socket));

				System.out.println("UDPCacheClient: Sending GET request..."); // Log GET request
				sendRequest(socket, address, "GET key1");
				System.out.println("UDPCacheClient: GET Response: " + receiveResponse(socket));
			} else {
				System.out.println("UDPCacheClient: Authentication failed!");
			}
		} catch (IOException e) {
			System.err.println("Client error: " + e.getMessage());
		}
	}

	private static void sendRequest(DatagramSocket socket, InetAddress address, String request) throws IOException {
		System.out.println("UDPCacheClient: sendRequest called with: " + request); // Log request sending
		byte[] data = request.getBytes(StandardCharsets.UTF_8);
		DatagramPacket packet = new DatagramPacket(data, data.length, address, SERVER_PORT);
		socket.send(packet);
	}

	private static String receiveResponse(DatagramSocket socket) throws IOException {
		System.out.println("UDPCacheClient: receiveResponse: Waiting for response..."); // Log before receive
		byte[] buffer = new byte[1024];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		socket.receive(packet);
		String response = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
		System.out.println("UDPCacheClient: receiveResponse: Received: " + response); // Log received response
		return response;
	}
}