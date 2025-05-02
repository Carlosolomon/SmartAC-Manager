/* package com.example.tcpcommunication;


    import java.io.*;
    import java.net.*;

    public class TCPServer {
       public static void main(String[] args) {

           try (ServerSocket serverSocket = new ServerSocket(1234)) {
               System.out.println("Server is listening on port 1234");
               while (true) {
                   Socket socket = serverSocket.accept();
                   System.out.println("New client connected");
                   InputStream input = socket.getInputStream();
                   BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                   String message = reader.readLine();
                   System.out.println("Received from client: " + message);

                   sendServerMessage("Server message here");

                  // socket.close();
               }
           } catch (IOException ex) {
               System.out.println("Server exception: " + ex.getMessage());
               ex.printStackTrace();
           }
       }


        public static void sendServerMessage(String message) {
           if (pwClientOutput != null) {
               pwClientOutput.println(message);  // Send message to the client
           }
       }
    }

    */




     /*/latest version
    public class TCPServer {

        private static Socket clientSocket;
        private static BufferedReader brClientInput;
        private static PrintWriter pwClientOutput;

        public static void main(String[] args) {
            try (ServerSocket serverSocket = new ServerSocket(12345)) {
                System.out.println("Server started. Waiting for client to connect...");

                clientSocket = serverSocket.accept();  // Wait for the client to connect
                System.out.println("Client connected!");

                brClientInput = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                pwClientOutput = new PrintWriter(clientSocket.getOutputStream(), true);

                // You can receive data from the client here
                String clientMessage;
                while ((clientMessage = brClientInput.readLine()) != null) {
                    System.out.println("Client: " + clientMessage);
                }

                // You can now send a message to the client (e.g., from the UI)
                sendServerMessage("Server message here");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public static void sendServerMessage(String message) {
            if (pwClientOutput != null) {
                pwClientOutput.println(message);  // Send message to the client
            }
        }
    }

      */


    /*/-------------------------------------------------------------------------------------------


     package com.example.tcpcommunication;

     import java.io.*;
     import java.net.*;

     public class TCPServer {
         private static PrintWriter pwClientOutput;  // Declare at class level
         private static BufferedReader reader;       // To read client messages
         public static void main(String[] args) {
             try (ServerSocket serverSocket = new ServerSocket(1234)) {
                 System.out.println("Server is listening on port 1234");

                 while (true) {
                     Socket socket = serverSocket.accept();
                     System.out.println("New client connected");

                     // Get the client output stream (to send messages to the client)
                     pwClientOutput = new PrintWriter(socket.getOutputStream(), true);  // Now accessible from sendServerMessage()
                     reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     // Create a thread to handle receiving messages from the client
                     new Thread(() -> handleClientMessages()).start();
                     // Read message from the client
                     InputStream input = socket.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                     String message = reader.readLine();
                     System.out.println("Received from client: " + message);

                     // Now you can send a message back to the client
                     sendServerMessage("Server message here");

                     // socket.close(); // Commented out to keep the connection open for further communication
                 }
             } catch (IOException ex) {
                 System.out.println("Server exception: " + ex.getMessage());
                 ex.printStackTrace();
             }
         }

         // Method to send a message to the client
         public static void sendServerMessage(String message) {
             if (pwClientOutput != null) {
                 pwClientOutput.println(message);  // Send message to the client
             }
         }
     }


     */





//------------------------------------------------------------------------------------------------------


package com.example.SmartMosque331;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer {
    private static PrintWriter pwClientOutput;  // Declare at class level
    private static BufferedReader reader;       // To read client messages

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(1234)) {
            System.out.println("Server is listening on port 1234");

            while (true) {
                Socket socket = serverSocket.accept();  // Wait for client to connect
                System.out.println("New client connected");

                // Set up input and output streams
                pwClientOutput = new PrintWriter(socket.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Create a thread to handle receiving messages from the client
                new Thread(() -> handleClientMessages()).start();

                // Continuously allow server to send messages to the client
                BufferedReader serverInput = new BufferedReader(new InputStreamReader(System.in));
                String serverMessage;
                while ((serverMessage = serverInput.readLine()) != null) {
                    sendServerMessage(serverMessage);  // Send server's message to the client
                }
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Method to send messages to the client
    public static void sendServerMessage(String message) {
        if (pwClientOutput != null) {
            pwClientOutput.println(message);  // Send message to the client
        }
    }

    // Thread function to handle incoming client messages
    private static void handleClientMessages() {
        String clientMessage;
        try {
            // Continuously read and print client messages
            while ((clientMessage = reader.readLine()) != null) {
                System.out.println("Received from client: " + clientMessage);
            }
        } catch (IOException ex) {
            System.out.println("Error reading client message: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}