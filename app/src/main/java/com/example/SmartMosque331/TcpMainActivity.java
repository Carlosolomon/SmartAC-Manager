
package com.example.SmartMosque331;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.DhcpInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.provider.Settings;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.annotation.RequiresApi;
import android.os.Handler;
import android.os.Looper;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.location.LocationManager;
import android.content.SharedPreferences;
import androidx.appcompat.app.AlertDialog;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


import com.example.SmartMosque331.databinding.TcpActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;


public class TcpMainActivity extends AppCompatActivity { // TcpMainActivity class start


    private TcpActivityMainBinding binding;

    // UI Components
    private TextView tvReceivedData, MessageReceived, tvServerName, tvServerPort, tvServerStatus;
    private EditText etServerName, etServerPort, etClientMessage, etServerMessage;
    private Button btnClientConnect, discButton, btnSendData, btnSendServer, btnStartServer, btnStopServer, btnSwitchUI, WifiButton, ServerListBtn;

    // Network Components
    private Socket socket; // Client socket
    private BufferedReader brInput;
    private PrintWriter pwOutput;
    private ServerSocket serverSocket; // Server socket

    private static TcpMainActivity instance; // Singleton instance of TcpMainActivity

    private Thread serverThread;
    //private Switch ACSwitch;
    private boolean isServerRunning = false;
    public MainActivity MinActv = MainActivity.getInstance();


    private String serverIP = "10.0.2.16"; // Client IP
    private int serverPort = 1234; // Client Port
    private static TCPClient clientInstance; // Static instance for global access
    private WifiManager wifiManager;
    private final String WIFI_PASSWORD = "12345678"; // Common password
    private String userEnteredPassword = ""; // Common password

    private int connectionAttempts = 0; // Retry count
    private static final int MAX_ATTEMPTS = 5; // Max retry count
    private static final int MAX_CONNECTION_ATTEMPTS = 3; // Max retries per network
    private static final long SCAN_TIMEOUT_MS = 30000; // 30 seconds total timeout
    private Handler timeoutHandler = new Handler();
    private Runnable timeoutRunnable;
    private static boolean isConnected = false; // Tracks connection status
    private static boolean isConnectedWifi = false; // Tracks WiFi connection status

    private String latestESPMessage = null; // Store last received message
    private List<String> messageQueue = new ArrayList<>();  // List to store multiple messages

    private ImageView wifiStatus;
    private ImageView tcpStatus;
    private static final String PREFS_NAME = "ESP_PREFS";
    private static final String KEY_SAVED_SERVERS = "SAVED_SERVERS";
    private boolean isWifiPickerShown = false;  // **🔹 Track whether the system Wi-Fi picker dialog is shown**

    private final Queue<String> messageQueue1 = new LinkedList<>();



    private BottomNavigationView bottomNavigationView;




    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) { // onCreate method start
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tcp_activity_main);

        // Inflate the layout using View Binding
        binding = TcpActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        instance = this;


        // Initialize UI components
        tvReceivedData = findViewById(R.id.tvRecTxt);
        etServerName = findViewById(R.id.etServerName);
        etServerPort = findViewById(R.id.etServerPort);
        etClientMessage = findViewById(R.id.MessageFromClient);
        btnClientConnect = findViewById(R.id.btnClientConnect);
        discButton = findViewById(R.id.discButton);
        btnSendData = findViewById(R.id.btnSendData);
        etServerMessage = findViewById(R.id.MessageFromServer);
        btnSendServer = findViewById(R.id.btnServerSend);
        tvServerPort = findViewById(R.id.tvServerPort);
        tvServerStatus = findViewById(R.id.tvServerStatus);
        tvServerName = findViewById(R.id.tvServerName);
        btnStartServer = findViewById(R.id.btnStartServer);
        btnStopServer = findViewById(R.id.btnStopServer);
        MessageReceived = findViewById(R.id.ServerReceivedData);
        // btnSwitchUI = findViewById(R.id.btnSwitchUi);
        WifiButton = findViewById(R.id.Wifi);
        wifiStatus = findViewById(R.id.wifiStatusIcon);
        tcpStatus = findViewById(R.id.tcpStatusIcon);
        ServerListBtn = findViewById(R.id.serverList);
        //ACSwitch = findViewById(R.id.switchAC);
        ServerListBtn.setOnClickListener(v -> showSavedServersDialog());


        // Set up client connection
        btnClientConnect.setOnClickListener(view -> onClickConnect());
        discButton.setOnClickListener(view -> onClickDisconnect());
        btnSendData.setOnClickListener(view -> sendMessage(etClientMessage.getText().toString()));
        btnSendServer.setOnClickListener(view -> sendMessageServer(etServerMessage.getText().toString()));

        // Server start and stop buttons
        btnStartServer.setOnClickListener(view -> startServer());
        btnStopServer.setOnClickListener(view -> stopServer());
        WifiButton.setOnClickListener(view -> {
            requestLocationPermission();
            requestWriteSettingsPermission();
            connectToStrongestWifi();
        });


        //Navigation listener:
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.homey) { //handle home
                Intent intent = new Intent(TcpMainActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                startActivity(intent);
                return true;
            } else if (item.getItemId() == R.id.connectiony) {
                // Handle Connection navigation

                return true;
            } else if (item.getItemId() == R.id.setupy) {
                Intent intent = new Intent(TcpMainActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("openSetupDialog", true); // Pass an extra flag
                startActivity(intent);
                return true;

                // Handle Setup
                //MinActv.getshowSetupDialog();
                // return true;
            }

            return false;
        });


       /*  new Handler(Looper.getMainLooper()).postDelayed(() -> {
            runOnUiThread(() -> {
                try {
                    BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
                    if (bottomNav == null) {
                        Log.e("WiFiIcon", "❌ BottomNavigationView is NULL!");
                        return;
                    }

                    Menu menu = bottomNav.getMenu();
                    if (menu == null) {
                        Log.e("WiFiIcon", "❌ Menu is NULL!");
                        return;
                    }

                    MenuItem wifiItem = menu.findItem(R.id.connectiony);
                    if (wifiItem == null) {
                        Log.e("WiFiIcon", "❌ MenuItem connectiony is NULL!");
                        return;
                    }

                    Drawable icon = ContextCompat.getDrawable(this, R.drawable.baseline_wifi_green_24);
                    if (icon != null) {
                        icon.mutate().setTint(Color.GREEN); //  FORCE ICON TO GREEN
                        wifiItem.setIcon(icon);
                        Log.d("WiFiIcon", "✅ WiFi icon REALLY changed to GREEN now!");
                    } else {
                        Log.e("WiFiIcon", "❌ Drawable is NULL!");
                    }

                } catch (Exception e) {
                    Log.e("WiFiIcon", "❌ Error changing icon: " + e.getMessage());
                }
            });
        }, 1000); // Run after 1 second    */


        /*/ Introduce a delay of 2 seconds before applying the green tint
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                MenuItem connectionItem = bottomNavigationView.getMenu().findItem(R.id.connectiony);

                // Apply the green color only to the Wi-Fi icon
                connectionItem.getIcon().setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.SRC_IN);            }
        }, 2000);  // 2000 milliseconds = 2 seconds    */

        // Now that the BottomNavigationView is initialized, modify the Wi-Fi icon color.
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView); // Make sure bottomNavigationView is properly initialized


        // Update this code AFTER the Navigation listener to change the Wi-Fi icon color:
        MenuItem connectionItem = binding.bottomNavigationView.getMenu().findItem(R.id.connectiony);

// Apply the green color to the Wi-Fi icon
        if (connectionItem != null) {
            connectionItem.getIcon().setTint(getResources().getColor(R.color.green));
        }



        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        requestPermissions();

        updateConnectionStatusUI();





    } // onCreate method end


    // Method to broadcast message
    private void broadcastMessage(String message) { // broadcastMessage method start
        try {
            if (pwOutput != null) { // Ensure PrintWriter is initialized
                pwOutput.println(message); // Send message
            } else {
                System.out.println("PrintWriter not initialized. Cannot broadcast message.");
            }
        } catch (Exception e) {
            e.printStackTrace(); // Handle any exceptions
        }
    } // broadcastMessage method end


    // Static method to get the TcpMainActivity instance
    public static TcpMainActivity getInstance() {
        return instance;
    }


    // Expose the sendMessage method for external usage
    public void sendMessageExternally(String message) {
        sendMessage(message); // Call the existing sendMessage method
    }

    // Process feedback from ESP32
   /* public void processFeedback(String feedback) {
        runOnUiThread(() -> {
            if (feedback.equals("1")) {  // Expecting "1" as feedback
                MainActivity.getInstance().updateACStatus();  // Call UI update method in MainActivity
            } else {
                Log.e("TCPMainActivity", "Unexpected response from ESP32: " + feedback);
            }
        });
    }
*/
    public void listenForServerMessagesExternally() { // listenForServerMessages method start
         listenForServerMessages();
    } // listenForServerMessages method end

    private void startServer() { // startServer method start
        if (isServerRunning) {
            tvServerStatus.setText("Server is already running");
            return;
        }

        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(serverPort);
                isServerRunning = true;
                runOnUiThread(() -> tvServerStatus.setText("Server started on port: " + serverPort));

                while (isServerRunning) {
                    Socket clientSocket = serverSocket.accept();
                    handleClient(clientSocket);
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> tvServerStatus.setText("Error starting server: " + e.getMessage()));
            }
        });
        serverThread.start();
    } // startServer method end

    private void stopServer() { // stopServer method start
        isServerRunning = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                runOnUiThread(() -> tvServerStatus.setText("Server stopped"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (serverThread != null) {
            serverThread.interrupt();
        }
    } // stopServer method end






    private void handleClient(Socket clientSocket) { // handleClient method start
        new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                String clientMessage;

                while ((clientMessage = in.readLine()) != null) {
                    final String receivedMsg = clientMessage;
                    runOnUiThread(() -> {
                        Log.d("TCP_SERVER_RECEIVED", "📨 Received from real client: " + receivedMsg);
                        if (MessageReceived != null) {
                            MessageReceived.setText("Client: " + receivedMsg); // Show actual TCP message
                        }
                    });
                    out.println("Echo from Server: " + receivedMsg); // Echo back to client
                }

            } catch (IOException e) {
                String Errory = e.getMessage();

                e.printStackTrace();
                runOnUiThread(() -> tvServerStatus.setText("Error handling client: " + e.getMessage()));
            }
        }).start();
    } // handleClient method end

    /* private void onClickConnect() { // onClickConnect method start
        String serverName;
        //String serverName = etServerName.getText().toString();
        int serverPort = Integer.parseInt(etServerPort.getText().toString());
        if ( etServerName.getText().toString().isEmpty() ) {
            // No manual IP entered, fetch automatically
            serverName = getHostIp();
        }
        else  {serverName = etServerName.getText().toString(); }



        new Thread(() -> {
            try {

                socket = new Socket(serverName, serverPort);
                brInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                pwOutput = new PrintWriter(socket.getOutputStream(), true);

                runOnUiThread(() -> tvReceivedData.setText("Connected to server"));

                // Start listening for messages from the server
                listenForServerMessages();

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> tvReceivedData.setText("Error connecting: " + e.getMessage()));
            }
        }).start();
    } // onClickConnect method end  */



    private void onClickConnect() {
        String serverName;
        int serverPort;
        Log.d("TCP_CONNECT", "🔄 Starting connection attempt...");

        // If user didn't enter an IP, fetch automatically
        if (etServerName.getText().toString().trim().isEmpty()) {
            serverName = getHostIp(); // Get host IP
            runOnUiThread(() -> etServerName.setText(serverName)); // Show detected IP in UI
        } else {
            serverName = etServerName.getText().toString().trim();
        }

        // If user didn't enter a port, set default (1234)
        if (etServerPort.getText().toString().trim().isEmpty()) {
            serverPort = 1234;
            runOnUiThread(() -> etServerPort.setText(String.valueOf(serverPort))); // Show default port
        } else {
            try {
                serverPort = Integer.parseInt(etServerPort.getText().toString().trim());
            } catch (NumberFormatException e) {
                runOnUiThread(() -> tvReceivedData.setText("Invalid port number"));
                return;
            }
        }

        new Thread(() -> {
            try {
                socket = new Socket(serverName, serverPort);
                brInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                pwOutput = new PrintWriter(socket.getOutputStream(), true);

                runOnUiThread(() -> { Toast.makeText(this,"✅ Connected to server at: " + serverName + ":" + serverPort, Toast.LENGTH_SHORT).show();
                    isConnected = true; // Set connection status to true
                    updateConnectionStatusUI();
                    updateWiFiIcon(); // Update the WiFi icon

                    //  Save the server IP:Port
                    saveServerToHistory(serverName + ":" + serverPort);
                });
                // Start listening for messages from the server
                listenForServerMessages();

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->  {           Toast.makeText(this, "❌ Error connecting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    isConnected = false; // Set connection status to false
                    updateConnectionStatusUI();
                    updateWiFiIcon(); // Update the WiFi icon
                });
            }
        }).start();
    }   // on click method   */


    /*
    private void onClickConnect() {
        String serverName;
        int serverPort;

        // 🆕 ADDED: Show we're starting connection attempt
        Log.d("TCP_CONNECT", "🔄 Starting connection attempt...");

        // If user didn't enter an IP, fetch automatically
        if (etServerName.getText().toString().trim().isEmpty()) {
            serverName = getHostIp(); // Get host IP
            runOnUiThread(() -> etServerName.setText(serverName)); // Show detected IP in UI
        } else {
            serverName = etServerName.getText().toString().trim();
        }

        // If user didn't enter a port, set default (1234)
        if (etServerPort.getText().toString().trim().isEmpty()) {
            serverPort = 1234;
            runOnUiThread(() -> etServerPort.setText(String.valueOf(serverPort))); // Show default port
        } else {
            try {
                serverPort = Integer.parseInt(etServerPort.getText().toString().trim());
            } catch (NumberFormatException e) {
                runOnUiThread(() -> tvReceivedData.setText("Invalid port number"));
                return;
            }
        }

        int finalServerPort = serverPort;
        new Thread(() -> {

            // 🆕 ADDED: Retry logic to attempt connection 3 times
            int attempts = 0;
            while (attempts < 3) {
                try {
                    Log.d("TCP_CONNECT", "🔁 Attempt " + (attempts + 1) + ": Connecting to " + serverName + ":" + finalServerPort); // 🆕 ADDED

                    socket = new Socket();
                    socket.connect(new java.net.InetSocketAddress(serverName, finalServerPort), 3000); // ⬅️ MODIFIED: 3-second timeout

                    brInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    pwOutput = new PrintWriter(socket.getOutputStream(), true);

                    isConnected = true; // ⬅️ MODIFIED
                    runOnUiThread(() -> {
                        Toast.makeText(this, "✅ Connected to server at: " + serverName + ":" + finalServerPort, Toast.LENGTH_SHORT).show();
                        updateWiFiIcon();
                    });

                    // 🆕 ADDED: Start listening only if connected
                    listenForServerMessages();
                    return; // 🆕 ADDED: Exit after successful connection

                } catch (IOException e) {
                    attempts++;

                    Log.e("TCP_CONNECT", "❌ Attempt " + attempts + " failed: " + e.getMessage(), e); // 🆕 ADDED

                    if (attempts == 3) {
                        isConnected = false;
                        runOnUiThread(() -> {
                            Toast.makeText(this, "❌ Failed to connect after 3 attempts", Toast.LENGTH_LONG).show();
                            updateWiFiIcon();
                            tvReceivedData.setText("❌ Connection failed: " + e.getMessage());
                        });
                    }

                    try {
                        Thread.sleep(1000); // 🆕 ADDED: Short wait before retrying
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            }
        }).start();
    } // onClickConnect methodd
    */



    private void onClickDisconnect() { // onClickDisconnect method start
        new Thread(() -> {
            try {
                if (socket != null) {
                    socket.close();
                    socket = null;
                    runOnUiThread(() -> tvReceivedData.setText("Disconnected from server"));
                    isConnected = false; // Set connection status to false
                    updateWiFiIcon(); // Update the WiFi icon
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    } // onClickDisconnect method end

/*/ Last Working method 5 April 2025
    private String listenForServerMessages() {
        try {
            String message;
            while ((message = brInput.readLine()) != null) {
                final String serverMessage = message;
                runOnUiThread(() -> tvReceivedData.setText("Server: " + serverMessage));

                //if (serverMessage.contains("1")) { // ✅ ACK Detected
                return message;
                //}
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;  // ✅ Ensures No Crash on Missing Response
    }   */
   /* private void listenForServerMessages() {
        Log.d("TCP_CLIENT", "🔁 listenForServerMessages() started...");
        new Thread(() -> {
            try {
                String message;
                while ((message = brInput.readLine()) != null) {
                    final String serverMessage = message;
                    latestESPMessage = serverMessage; // 🆕 Save the latest message for MainActivity access
                    messageQueue.add(serverMessage); // Add to the message queue
                    Log.d("TCP_CLIENT_RECEIVED", "📩 Received from real server: " + serverMessage);
                    Log.d("TCP_CLIENT", "Message received and added to queue: " + serverMessage);

                    runOnUiThread(() -> {
                            tvReceivedData.setText("Server: " + serverMessage);
                    });
                }
            } catch (IOException e) {
                Log.e("TCP_CLIENT_RECEIVED", "❌ Error reading from ESP32", e);
                e.printStackTrace();
            }
        }).start();
    }   */



    private void listenForServerMessages() {
        Log.d("TCP_CLIENT", "🔁 listenForServerMessages() started...");
        new Thread(() -> {
            try {
                String message;
                while ((message = brInput.readLine()) != null) {
                    final String serverMessage = message; // Create a final copy
                    synchronized (messageQueue1) {
                        messageQueue1.add(message); // Add to queue
                        latestESPMessage = message; // Update latest message
                        Log.d("TCP_CLIENT_RECEIVED", "📩 Received from ESP32: " + message);
                    }
                    runOnUiThread(() -> {
                        tvReceivedData.setText("Server: " + serverMessage);
                        // Notify MainActivity to process the queue
                        MainActivity mainActivity = MainActivity.getInstance();
                        if (mainActivity != null) {
                            mainActivity.processFeedbackQueue();
                        }
                    });
                }
            } catch (IOException e) {
                Log.e("TCP_CLIENT_RECEIVED", "❌ Error reading from ESP32", e);
                e.printStackTrace();
            }
        }).start();
    }

    public Queue<String> getMessageQueue() {
        synchronized (messageQueue) {
            return messageQueue1;
        }
    }





    private void sendMessage(String message) { // sendMessage method start
        if (socket != null && socket.isConnected()) {
            new Thread(() -> {
                pwOutput.println(message);
                runOnUiThread(() -> MessageReceived.setText("Client: " + message)); // Show latest sent message
            }).start();
        } else {
            runOnUiThread(() -> tvReceivedData.setText("Not connected to server"));
        }
    } // sendMessage method end



    private void sendMessageServer(String message) { // sendMessageServer method start
        if (socket != null && socket.isConnected()) {
            new Thread(() -> {
                pwOutput.println(message);
                runOnUiThread(() -> tvReceivedData.setText("Server: " + message)); // Show latest sent message
            }).start();
        } else {
            runOnUiThread(() -> tvReceivedData.setText("Server not connected"));
        }
    } // sendMessageServer method end

    public String getLatestESPMessage() {
        return latestESPMessage; // 🆕 External access to last message
    }

    // This is the method you're already using to get the latest message
    public String getLatestESPMessageS() {
        // Return the latest message from the queue
        if (messageQueue.size() > 0) {
            return messageQueue.get(messageQueue.size() - 1);  // Latest message
        }
        return "";  // Return empty if no messages
    }



    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE
            }, 1);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE
            }, 1);
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @SuppressLint("MissingPermission")
    private void connectToStrongestWifi() {
        if (isConnectedWifi) {
            Toast.makeText(this, "Already connected to WiFi", Toast.LENGTH_SHORT).show();
            return; // **Don't proceed if already connected**
        }
        connectionAttempts = 0;
        startConnectionTimeout();

        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "Turning WiFi ON...", Toast.LENGTH_SHORT).show();
            wifiManager.setWifiEnabled(true);
            new Handler(Looper.getMainLooper()).postDelayed(this::connectToStrongestWifi, 5000);
            return;
        }

        if (!isLocationEnabled()) {
            Toast.makeText(this, "Location services must be enabled for WiFi scanning!", Toast.LENGTH_LONG).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
            return;
        }

        // **🔹 Trigger system Wi-Fi picker** (replacing custom dialog)
        //showSystemWifiPicker();

        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        boolean scanStarted = wifiManager.startScan();

        if (!scanStarted) {
            handleScanFailure();
        }
    }

    private void showSystemWifiPicker() {
        isWifiPickerShown = true;  // **🔹 Mark Wi-Fi picker as shown**
        Intent intent = new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK);
        //startActivityForResult(intent, REQUEST_WIFI_CONNECT);  // This will trigger the system Wi-Fi dialog
    }


    private void startConnectionTimeout() {
        timeoutRunnable = () -> {
            Toast.makeText(TcpMainActivity.this,
                    "Connection timed out after " + (SCAN_TIMEOUT_MS / 1000) + " seconds",
                    Toast.LENGTH_LONG).show();
            cleanupConnectionProcess();
        };
        timeoutHandler.postDelayed(timeoutRunnable, SCAN_TIMEOUT_MS);
    }

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);


            if (networkInfo != null) {
                if (networkInfo.isConnected()) {
                    // **🔹 Wi-Fi is connected**
                    isConnectedWifi = true;
                } else {
                    // **🔹 Wi-Fi is disconnected**
                    isConnectedWifi = false;
                }
            }
            updateConnectionStatusUI();


            timeoutHandler.removeCallbacks(timeoutRunnable);
            unregisterReceiver(this);

            // **🔹 ADDED: Check if already connected**
            if (isConnectedWifi) {
                Toast.makeText(context, "Already connected to WiFi", Toast.LENGTH_SHORT).show();
                return; // **Don't continue if already connected**
            }

            List<ScanResult> scanResults = wifiManager.getScanResults();
            if (scanResults.isEmpty()) {
                handleNoNetworksFound();
                return;
            }

            scanResults.sort((a, b) -> Integer.compare(b.level, a.level));
            // Get the top 3 networks
            List<ScanResult> top3Networks = scanResults.subList(0, Math.min(3, scanResults.size()));

            showWifiSelectionDialog(top3Networks);

           // StringBuilder networksList = new StringBuilder("Top 3 Networks:\n");

            //for (int i = 0; i < top3Networks.size(); i++) {
             //   networksList.append((i + 1) + ". " + top3Networks.get(i).SSID + " (Signal Strength: " + top3Networks.get(i).level + " dBm)\n");
            //}


            // Show the top 3 networks in a Toast (or use any other UI element)
            //Toast.makeText(context, networksList.toString(), Toast.LENGTH_LONG).show();

            // Prompt the user to select a network and enter a password
           // if (!top3Networks.isEmpty()) {
                // Automatically select the strongest network (first in the list)
             //   connectToNetwork(top3Networks.get(0).SSID); // Connect to the strongest network
            //}



           //Last Working one  ScanResult bestNetwork = scanResults.get(0);
          // Toast.makeText(context, "Trying to connect to: " + bestNetwork.SSID, Toast.LENGTH_SHORT).show();
           // connectToNetwork(bestNetwork.SSID);
        }
    };

    private void connectToNetwork(String ssid) {
        //showPasswordInputDialog(ssid);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectToWifiModern(ssid);
        } else {
            connectToWifiLegacy(ssid);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void connectToWifiModern(String ssid) {
        try {
            String wifiPassword = userEnteredPassword;  // The password entered by the user


            WifiNetworkSpecifier wifiSpecifier = new WifiNetworkSpecifier.Builder()
                    .setSsid(ssid)
                    .setWpa2Passphrase(WIFI_PASSWORD)
                    .build();

            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(wifiSpecifier)
                    .build();

            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            connectivityManager.requestNetwork(request, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    handleConnectionSuccess(ssid);
                }

                @Override
                public void onUnavailable() {
                    super.onUnavailable();
                    handleConnectionFailure(ssid);
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error connecting to " + ssid + ": " + e.getMessage(), Toast.LENGTH_LONG).show();

        }
    }

    private void connectToWifiLegacy(String ssid) {
        try {
            WifiConfiguration config = new WifiConfiguration();
            config.SSID = String.format("\"%s\"", ssid);
            config.preSharedKey = String.format("\"%s\"", WIFI_PASSWORD);

            int networkId = wifiManager.addNetwork(config);
            if (networkId == -1) {
                handleConnectionFailure(ssid);
                return;
            }
            wifiManager.disconnect();
            wifiManager.enableNetwork(networkId, true);
            wifiManager.reconnect();

            new Handler().postDelayed(() -> verifyLegacyConnection(ssid), 5000);
        } catch (Exception e) {
            Toast.makeText(this, "Error connecting to " + ssid + ": " + e.getMessage(), Toast.LENGTH_LONG).show();

        }
    }

    private void verifyLegacyConnection(String ssid) {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = connManager.getActiveNetwork();
        NetworkCapabilities capabilities = connManager.getNetworkCapabilities(network);

        if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            handleConnectionSuccess(ssid);
        } else {
            handleConnectionFailure(ssid);
        }
    }

    private void handleScanFailure() {
        Toast.makeText(this, "WiFi Scan Failed!", Toast.LENGTH_SHORT).show();
    }

    private void handleNoNetworksFound() {
        Toast.makeText(this, "No WiFi networks found!", Toast.LENGTH_SHORT).show();
    }

    private void handleConnectionSuccess(String ssid) {
        runOnUiThread(() -> {
            Toast.makeText(TcpMainActivity.this, "✅ Connected to " + ssid, Toast.LENGTH_SHORT).show();
            cleanupConnectionProcess();
            isConnectedWifi = true; // Set connection status to true
            updateConnectionStatusUI();
            updateWiFiIcon(); // Update the WiFi icon
        });
    }

    private void handleConnectionFailure(String ssid) {
        runOnUiThread(() -> {
            Toast.makeText(TcpMainActivity.this, "❌ Failed to connect to " + ssid, Toast.LENGTH_LONG).show();
            isConnectedWifi = false; // ✅ Moved inside UI thread
            updateConnectionStatusUI();
            updateWiFiIcon(); // Update WiFi icon
        });
    }

    private void cleanupConnectionProcess() {
        timeoutHandler.removeCallbacks(timeoutRunnable);
    }

    private void requestLocationPermission() {  // requestLocationPermission start
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {  // **🔹 FIXED: Now also checks WiFi permission**

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_WIFI_STATE
            }, 1);
        } else {
            connectToStrongestWifi();  // **If permission is already granted, start scanning**
        }
    }  // requestLocationPermission end

    private void requestWriteSettingsPermission() {
        if (!Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            Toast.makeText(this, "Grant 'Modify System Settings' permission!", Toast.LENGTH_LONG).show();
        }
    }


    private void showWifiSelectionDialog(List<ScanResult> top3Networks) {
        CharSequence[] networkNames = new CharSequence[top3Networks.size()];
        for (int i = 0; i < top3Networks.size(); i++) {
           // networkNames[i] = (i+1) +" "+ top3Networks.get(i).SSID + convertDbmToPercentage(top3Networks.get(i).level)  + " (Signal: " + top3Networks.get(i).level + " dBm)";
            networkNames[i] = (String.format(" %d. %s  |  Signal: %d%%\n", i + 1, top3Networks.get(i).SSID, convertDbmToPercentage(top3Networks.get(i).level) )); // **🔹 Improved formatting**

        }

        new AlertDialog.Builder(this)
                .setTitle("Select WiFi Network")
                .setItems(networkNames, (dialog, which) -> {
                    String selectedSsid = top3Networks.get(which).SSID;
                    connectToNetwork(selectedSsid);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private int convertDbmToPercentage(int dBm) {
        // Convert dBm to a percentage between 0 and 100
        return Math.min(100, Math.max(0, (dBm + 100) * 100 / 100)); // Ensure the result is between 0 and 100
    }




    private String getHostIp() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        int ipAddress = dhcpInfo.gateway; //

        return String.format(
                "%d.%d.%d.%d",
                (ipAddress & 0xFF),
                (ipAddress >> 8 & 0xFF),
                (ipAddress >> 16 & 0xFF),
                (ipAddress >> 24 & 0xFF)
        );
    }

    public boolean isWifiConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected();
    }
    //        icon.setColorFilter(ContextCompat.getColor(this, colorId));
    @SuppressLint("NewApi")
    private void updateWiFiIcon() {
        runOnUiThread(() -> {
            BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

            Log.d("WiFiIcon", "Updating WiFi icon. isConnected = " + isConnectedWifi); // Debugging log

            if (bottomNav != null) {
                int iconRes = isConnectedWifi ? R.drawable.baseline_wifi_green_24 : R.drawable.baseline_wifi_24; // Select the correct icon
                bottomNav.getMenu().findItem(R.id.connectiony).setIcon(iconRes); // Set new icon
                bottomNav.post(() -> bottomNav.getMenu().findItem(R.id.connectiony).setIcon(iconRes));

                Log.d("WiFiIcon", "WiFi icon updated successfully to: " + (isConnectedWifi ? "Green" : "Gray"));
            } else {
                Log.e("WiFiIcon", "bottomNav is NULL, cannot update icon!"); // Log if it's not found
            }
        });
    }

    // Function to change itemIconTint to green dynamically
    private void changeWiFiIconColor(BottomNavigationView bottomNavigationView) {
        // Safely get the Wi-Fi (connection) item and apply the green tint
        MenuItem connectionItem = bottomNavigationView.getMenu().findItem(R.id.connectiony);
        if (connectionItem != null) {
            connectionItem.getIcon().setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.SRC_IN);
        }
    }




    private void updateConnectionStatusUI() {

        /*
        boolean isWifi = isConnectedWifi;
        boolean isTcp = (socket != null && socket.isConnected());

        if (wifiStatus != null && tcpStatus != null) {
            wifiStatus.setColorFilter(ContextCompat.getColor(this, isWifi ? R.color.green : R.color.red));
            tcpStatus.setColorFilter(ContextCompat.getColor(this, isTcp ? R.color.green : R.color.red));
        }
        */
        boolean isWifi = isConnectedWifi;
        boolean isTcp = (socket != null && socket.isConnected());

        // **🔹 Special check for WiFi SSID starting with specific words ("smart" or "mosque") case-insensitive**
        if (isWifi) {
            WifiInfo currentWifi = wifiManager.getConnectionInfo();
            String ssid = currentWifi.getSSID().replace("\"", "");  // Remove quotes if present

            // **🔹 Check if SSID starts with "smart" or "mosque" (case-insensitive)**
            if (ssid.toLowerCase().startsWith("smart") || ssid.toLowerCase().startsWith("mosque")) {
                // **🔹 WiFi connected to "smart" or "mosque" (green)**
                wifiStatus.setColorFilter(ContextCompat.getColor(this, R.color.green));
            } else {
                // **🔹 WiFi connected to any other network (yellow)**
                wifiStatus.setColorFilter(ContextCompat.getColor(this, R.color.yellow));
            }
        } else {
            // **🔹 No WiFi connected (red)**
            wifiStatus.setColorFilter(ContextCompat.getColor(this, R.color.red));
        }


        // **🔹 TCP status update**
        tcpStatus.setColorFilter(ContextCompat.getColor(this, isTcp ? R.color.green : R.color.red));


    }

    public boolean isTcpConnected() {
        return socket != null && socket.isConnected();
    }
    public boolean isWifiConnected1() {
        return isConnectedWifi;
    }
    private void saveServerToHistory(String serverAddress) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> servers = prefs.getStringSet(KEY_SAVED_SERVERS, new HashSet<>());
        Set<String> updatedServers = new HashSet<>(servers);
        updatedServers.add(serverAddress); // Avoid duplicates
        prefs.edit().putStringSet(KEY_SAVED_SERVERS, updatedServers).apply();
    }
    private void showSavedServersDialog() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> servers = prefs.getStringSet(KEY_SAVED_SERVERS, new HashSet<>());

        if (servers.isEmpty()) {
            Toast.makeText(this, "No saved servers found.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] serversArray = servers.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Previously Connected ESP Servers");
        builder.setItems(serversArray, (dialog, which) -> {
            String selected = serversArray[which];
            String[] parts = selected.split(":");
            if (parts.length == 2) {
                etServerName.setText(parts[0]);
                etServerPort.setText(parts[1]);
                Toast.makeText(this, "Selected: " + selected, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void showPasswordInputDialog(String ssid) {
        final EditText passwordEditText = new EditText(this);
        passwordEditText.setHint("Enter password");

        new AlertDialog.Builder(this)
                .setTitle("Enter WiFi Password for " + ssid)
                .setView(passwordEditText)
                .setPositiveButton("Connect", (dialog, which) -> {
                    userEnteredPassword = passwordEditText.getText().toString();
                    connectToNetworkWithPassword(ssid);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void connectToNetworkWithPassword(String ssid) {
        // Use the user input password instead of the hardcoded password
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectToWifiModern(ssid);  // Using modern connection for Android Q and above
        } else {
            connectToWifiLegacy( ssid);  // Using legacy method for lower versions
        }
    }

// Abd Zaid editioni:

    public String getConnectedEspId() {
        if (socket != null && socket.isConnected()) {
            // Get the connected IP address as the ESP ID
            return "ESP_" + socket.getInetAddress().getHostAddress().replace(".", "");
        }
        return null;
    }

    public String getConnectedEspSSID() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null && wifiInfo.getSSID() != null) {
                return wifiInfo.getSSID().replace("\"", ""); // Remove surrounding quotes
            }
        }
        return "UNKNOWN_SSID";
    }






} // TcpMainActivity class end
