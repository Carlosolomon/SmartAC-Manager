package com.example.SmartMosque331;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;




public class TCPClient {

    private static TCPClient tcpClientInstance; // Static instance for global access


    public static void main(String[] args) {
        String hostname = "10.0.2.16";  // loopback address
        int port = 1234;
        try (Socket socket = new Socket(hostname, port)) {
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            String message = "Hello from Client!";
            writer.println("Hello from the client!");
        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("Client error : " + ex.getMessage());
        }




    }
}