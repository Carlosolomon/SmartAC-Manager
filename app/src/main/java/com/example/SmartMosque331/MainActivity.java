package com.example.SmartMosque331;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Locale;



import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.batoulapps.adhan2.CalculationMethod;
import com.batoulapps.adhan2.CalculationParameters;
import com.batoulapps.adhan2.Coordinates;
import com.batoulapps.adhan2.Prayer;
import com.batoulapps.adhan2.PrayerTimes;
import com.batoulapps.adhan2.data.DateComponents;
import com.example.SmartMosque331.databinding.ActivityMainBinding;

import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;
import org.json.JSONException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.time.Instant;
import java.util.Random;
import java.time.format.DateTimeFormatter;
import java.util.Map;


import java.util.concurrent.TimeUnit;




public class MainActivity extends AppCompatActivity {

    // Class to store command details
    private static class PendingCommand {
        ImageView icon;
        ACStatus status;
        boolean wasOn;
        boolean isChecked;

        PendingCommand(ImageView icon, ACStatus status, boolean wasOn, boolean isChecked) {
            this.icon = icon;
            this.status = status;
            this.wasOn = wasOn;
            this.isChecked = isChecked;
        }
    }


    private EditText editTextACUp, editTextACDown, editTextACLeft, editTextACRight, editMinutesBefore, editMinutesAfter;
    private LinearLayout upACContainer, downACContainer, leftACContainer, rightACContainer;
    private TextView terminalOutput, tvCurrentTemp, fanLevelDisplay, tempDisplay;
    private ScrollView terminalScroll;
    private Switch acPowerSwitch, swingSwitch, testModeSwitch, testModeSwitchDefault;
    private ImageButton decreaseTempButton, increaseTempButton;
    private Button allButton, fanButton, btnSwitchUi, btnPrayerTimes, setupButton;
    private int currentTemp = 0;
    private static final int DEFAULT_ON_TEMP = 23;
    private int acID, globalACCounter = 1;
    private boolean shouldAutoScroll = true;

    private TextView testModeIndicator;
    private boolean isTestMode = false;
    private boolean isTestModeDefault = false;

    private HashMap<ImageView, ACStatus> acStatusMap = new HashMap<>();
    private List<ImageView> selectedIcons = new ArrayList<>();
    private final Map<String, PendingCommand> pendingCommands = new ConcurrentHashMap<>();
    private final Map<String, String> espToAcMap = new HashMap<>();


    private final Map<Integer, Boolean> pendingACStates = new HashMap<>();

    private Handler handler = new Handler();

    private int minutesBeforePrayer = 0;
    private int minutesAfterPrayer = 0;

    private int AllPressCount;
    private boolean schedulePrinted = false;  // ✅ Track if commands have been printed
    private boolean allSelected = false;

    private String lastPrintedSchedule = ""; // ✅ Store last printed schedule
    private int lastFajrBefore, lastFajrAfter, lastDhuhrBefore, lastDhuhrAfter, lastAsrBefore, lastAsrAfter, lastMaghribBefore, lastMaghribAfter, lastIshaBefore, lastIshaAfter;


    private ScheduledExecutorService scheduler;
    private static MainActivity instance; // Singleton instance of TcpMainActivity
    private ImageView wifiStatus;
    private ImageView tcpStatus;
     private TextView espTimeTextView; // 🆕 For ESP time display
    private Date espClockStartTime = null;     // 🆕 The received ESP clock
    private long espClockStartSystemMillis = 0; // 🆕 When we received it (device time)
    private final Handler espClockHandler = new Handler(); // 🆕 For live ticking
    boolean isInAssignmentMode = false;
    String currentESPID = "";

    ActivityMainBinding binding;

    private TcpMainActivity tcpActivity1;


//TcpHelper.sendMessage("Hello from MainActivity");


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;
        //Navigation Bar
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize all UI elements
        editTextACUp = findViewById(R.id.editText_ac_up);
        editTextACDown = findViewById(R.id.editText_ac_down);
        editTextACLeft = findViewById(R.id.editText_ac_left);
        editTextACRight = findViewById(R.id.editText_ac_right);
        upACContainer = findViewById(R.id.up_ac_container);
        downACContainer = findViewById(R.id.down_ac_container);
        leftACContainer = findViewById(R.id.left_ac_container);
        rightACContainer = findViewById(R.id.right_ac_container);
        terminalOutput = findViewById(R.id.terminal_output);
        terminalScroll = findViewById(R.id.terminalScroll);
        //increaseTempButton = findViewById(R.id.button_increase_temp);
        //decreaseTempButton = findViewById(R.id.button_decrease_temp);
        //acPowerSwitch = findViewById(R.id.ac_power_switch);
        //swingSwitch = findViewById(R.id.swing);

        allButton = findViewById(R.id.button_select_all);
        //setupButton = findViewById(R.id.button_setup);
        //fanButton = findViewById(R.id.fan);
        //fanLevelDisplay = findViewById(R.id.fan_level_display);
        // btnSwitchUi = findViewById(R.id.btnSwitchUi);
        testModeIndicator = findViewById(R.id.testModeIndicator);
        //tvCurrentTemp = findViewById(R.id.tvCurrentTemp);
        //tempDisplay = findViewById(R.id.temp_display); // Initialize temp display
        increaseTempButton = findViewById(R.id.button_increase_temp);
        decreaseTempButton = findViewById(R.id.button_decrease_temp);

        testModeIndicator = findViewById(R.id.testModeIndicator);
        testModeSwitch = findViewById(R.id.testmode_switch);
        testModeSwitchDefault = findViewById(R.id.testmode_swtichDefault);
        wifiStatus = findViewById(R.id.wifiStatusIcon);
        tcpStatus = findViewById(R.id.tcpStatusIcon);
        Button btnClearTerminal = findViewById(R.id.btnClearTerminal);
        btnClearTerminal.setOnClickListener(v -> clearTerminal());


        MaterialButton btnRealtime = findViewById(R.id.btn_realtime_tab);
        MaterialButton btnScheduled = findViewById(R.id.btn_scheduled_tab);
        ViewStub stubRealtime = findViewById(R.id.realtime_controls);
        ViewStub stubScheduled = findViewById(R.id.scheduled_controls);



        View realtimeControls = stubRealtime.inflate();
        View scheduledControls = stubScheduled.inflate();
        scheduledControls.setVisibility(View.GONE);

        // ✅ Initialize real-time controls safely
        increaseTempButton = realtimeControls.findViewById(R.id.button_increase_temp);
        decreaseTempButton = realtimeControls.findViewById(R.id.button_decrease_temp);
        acPowerSwitch = realtimeControls.findViewById(R.id.ac_power_switch);
        swingSwitch = realtimeControls.findViewById(R.id.swing);
        fanButton = realtimeControls.findViewById(R.id.fan);
        fanLevelDisplay = realtimeControls.findViewById(R.id.fan_level_display);
        tvCurrentTemp = realtimeControls.findViewById(R.id.tvCurrentTemp);
        tempDisplay = realtimeControls.findViewById(R.id.temp_display);




        btnRealtime.setOnClickListener(v -> {
            btnRealtime.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple_500)));
            btnScheduled.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            btnRealtime.setTextColor(Color.WHITE);
            btnScheduled.setTextColor(Color.BLACK);
            realtimeControls.setVisibility(View.VISIBLE);
            scheduledControls.setVisibility(View.GONE);
        });

        btnScheduled.setOnClickListener(v -> {
            btnScheduled.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple_500)));
            btnRealtime.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            btnScheduled.setTextColor(Color.WHITE);
            btnRealtime.setTextColor(Color.BLACK);
            realtimeControls.setVisibility(View.GONE);
            scheduledControls.setVisibility(View.VISIBLE);
        });

        Button testESPButton = findViewById(R.id.btn_test_esp_assignment);

        testESPButton.setOnClickListener(v -> {
                    TcpMainActivity tcpActivity = TcpMainActivity.getInstance();
                    if (tcpActivity != null && tcpActivity.isTcpConnected()) {
                        // ✅ Get Wi-Fi SSID (e.g., Smart_Mosque_001)
                        String espSSID = tcpActivity.getConnectedEspSSID();
                        currentESPID = espSSID;

                        if (espSSID != null && !espSSID.equals("UNKNOWN_SSID")) {
                            isInAssignmentMode = true;
                            Toast.makeText(this, "Choose an AC icon to assign", Toast.LENGTH_SHORT).show();

                            // Temporarily override icon clicks for assignment
                            for (ImageView icon : acStatusMap.keySet()) {
                                icon.setOnClickListener(v1 -> {
                                    if (isInAssignmentMode) {
                                        showNumberingDialog(icon, espSSID);
                                        isInAssignmentMode = false;
                                        restoreOriginalClickListeners();
                                    }
                                });
                            }
                        } else {
                            Toast.makeText(this, "Could not detect ESP Wi-Fi name", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Not connected to ESP", Toast.LENGTH_SHORT).show();
                    }
                });




        // Attach listeners to update temperature
        increaseTempButton.setOnClickListener(v -> updateTemperature(true));
        decreaseTempButton.setOnClickListener(v -> updateTemperature(false));



        setACInputListeners();
        /* Button to switch UI
        btnSwitchUi.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TcpMainActivity.class);
            startActivity(intent);

        }); */
        // setupButton.setOnClickListener(v -> showSetupDialog());
        //Navigation listener:
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.homey) {
                // Handle Home navigation
                return true;
            } else if (item.getItemId() == R.id.connectiony) {
                // Handle Connection navigation
                Intent intent = new Intent(MainActivity.this, TcpMainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                startActivity(intent);
                return true;
            }
            else if (item.getItemId() == R.id.setupy) {
                // Handle Setup
                showSetupDialog();
                return true;
            }

            return false;

        });

        // "Select All" button
        allButton.setOnClickListener(v -> toggleSelectAllACIcons());

        // Power switch functionality
        acPowerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            List<ImageView> iconsToRemove = new ArrayList<>(); // Temporary list to track icons to remove

            for (ImageView icon : selectedIcons) {
                ACStatus status = acStatusMap.get(icon);
                boolean wasOn = status.isPowerOn;
                String acIDString = icon.getTag().toString();
                String acIDD = acIDString.substring(3, 6);
                int acIDInt = Integer.parseInt(acIDD);

                // Set temperature and update icon color to yellow
                status.temperature = isChecked ? DEFAULT_ON_TEMP : 0;
                updateIconColor(icon, R.color.yellow); // Mock transition

                // Build the message body
                String messageBody = "state:" + (isChecked ? "ON" : "OFF") + ", Temp:" + (isChecked ? DEFAULT_ON_TEMP : "0") + ", Swing:ON, fan:1";
                byte messageType = 1; // Control Command

                // Handle broadcast for all ACs
                if (allSelected) {
                    acIDInt = Integer.parseInt("000");
                    updateTerminal("Broadcast acID 000 since allSelected = " + allSelected);
                }

                // Send the command
                byte[] structuredMessage = TcpHelper.composeMessage(messageBody, acIDInt, messageType);
                TcpHelper.sendMessage(new String(structuredMessage, StandardCharsets.US_ASCII));
                updateTerminal("Sent to AC" + acIDInt);

                // Store the pending command
                pendingCommands.put(acIDD, new PendingCommand(icon, status, wasOn, isChecked));

                // Set up a timeout for this command
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    PendingCommand pending = pendingCommands.get(acIDD);
                    if (pending != null) {
                        updateTerminal("⏰ Timeout! No response for AC" + acIDD);
                        updateIconColor(pending.icon, pending.wasOn ? R.color.green : R.color.gray);
                        pending.status.isPowerOn = pending.wasOn;
                        acPowerSwitch.setChecked(pending.wasOn);
                        pendingCommands.remove(acIDD);
                    }
                }, 4000); // 4s timeout

                iconsToRemove.add(icon); // Add to remove list
            }

            selectedIcons.removeAll(iconsToRemove); // Clear processed icons

            // Start processing feedback queue
            processFeedbackQueue();
        });

// Method to process feedback from the queue
// acPower listener



        // Power switch functionality
        // acPower listener





        // Temperature control
        increaseTempButton.setOnClickListener(v -> updateTemperature(true));
        decreaseTempButton.setOnClickListener(v -> updateTemperature(false));

        // Swing switch functionality
        swingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (ImageView icon : selectedIcons) {
                ACStatus status = acStatusMap.get(icon);
                status.swing = isChecked;
                //Last working String message = icon.getTag() + " Swing " + (isChecked ? "ON" : "OFF");

                // Build the control message
                //String messageBody = icon.getTag() + " Swing " + (isChecked ? "ON" : "OFF");

                // Build the JSON message body for swing control (state: ON/OFF)
                JSONObject messageJson = new JSONObject();
                try {
                    //messageJson.put("acID", icon.getTag().toString());
                    messageJson.put("swing", isChecked ? "ON" : "OFF");
                } catch (JSONException e) {
                    e.printStackTrace();
                }



                // Convert the JSON object to a string
                String messageBody = messageJson.toString();
                // Message Type for control commands is 1
                byte messageType = 1;  // Control Command

                // Call the TcpHelper to compose the structured message
                // Last working byte[] structuredMessage = TcpHelper.composeMessage(messageBody, (String) icon.getTag(), messageType);

                byte[] structuredMessage = TcpHelper.composeMessage(messageBody, acID, messageType);
                // Send the structured message via TCP
                TcpHelper.sendMessage(new String(structuredMessage, StandardCharsets.US_ASCII));

                // Send the message via TCP
                //Last working TcpHelper.sendMessage(message);
                updateTerminal("Swing is " + (isChecked ? "ON" : "OFF") + " for " + icon.getTag());
            }
        });

        /*
        // Ensure the switch is not null before setting the listener
        if (testModeSwitch != null) {
            testModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {

                Log.d("ACControl", "Test mode switch changed: " + (isChecked ? "ON" : "OFF"));
                if (isChecked) { isTestMode = true;
                    Toast.makeText(this, "Random Test Mode Activated", Toast.LENGTH_SHORT).show(); // Inform user
                    testACControlScheduler();  // Start test mode
                } else {  Toast.makeText(this, "Random Test Mode Deactivated", Toast.LENGTH_SHORT).show(); // Inform user
                    isTestMode = false;
                    if (scheduler != null && !scheduler.isShutdown()) {
                        scheduler.shutdownNow();  // Stop test mode when switch is turned off
                    }
                }
            });
        } else {
            Log.e("ACControl", "testModeSwitch is null, make sure the Switch is initialized properly");
        }


        if (testModeSwitchDefault != null) {
            testModeSwitchDefault.setOnCheckedChangeListener((buttonView, isChecked) -> {

                Log.d("ACControl", "Default Test mode switch changed: " + (isChecked ? "ON" : "OFF"));
                if (isChecked) { isTestModeDefault = true;
                    Toast.makeText(this, "Test Mode Activated", Toast.LENGTH_SHORT).show(); // Inform user
                    testACControlScheduler2();  // Start test mode
                } else {  Toast.makeText(this, "Test Mode Deactivated", Toast.LENGTH_SHORT).show(); // Inform user
                    if (scheduler != null && !scheduler.isShutdown()) {
                        scheduler.shutdownNow();  // Stop test mode when switch is turned off
                    }
                }
            });
        } else {
            Log.e("ACControl", "testModeSwitch is null, make sure the Switch is initialized properly");
        }  */



        // Fan button functionality
        fanButton.setOnClickListener(v -> {
            for (ImageView icon : selectedIcons) {
                ACStatus status = acStatusMap.get(icon);
                status.fanLevel = status.fanLevel < 4 ? status.fanLevel + 1 : 1;
                // Last working String message = icon.getTag() + " Fan " + status.fanLevel;
                fanLevelDisplay.setText("Fan Level: " + status.fanLevel);
                // Build the control message
                // String messageBody = icon.getTag() + " Fan " + status.fanLevel;
                // Build the JSON message body for fan speed control
                JSONObject messageJson = new JSONObject();
                try {
                    // messageJson.put("acID", icon.getTag().toString());
                    messageJson.put("fan", status.fanLevel);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // Convert the JSON object to a string
                String messageBody = messageJson.toString();

                // Message Type for control commands is 1
                byte messageType = 1;  // Control Command

                // Call the TcpHelper to compose the structured message
                byte[] structuredMessage = TcpHelper.composeMessage(messageBody, acID, messageType);

                // Send the structured message via TCP
                TcpHelper.sendMessage(new String(structuredMessage, StandardCharsets.US_ASCII));


                // Send the message via TCP
                //Last working  TcpHelper.sendMessage(message);
                updateTerminal("Fan is at level " + status.fanLevel + " for " + icon.getTag());
            }
        });

        updateConnectionStatusUI();
        tcpActivity1 = TcpMainActivity.getInstance();
        startListeningForMessages();






    } // end of the OnCreate


    public void processFeedbackQueue() {
        TcpMainActivity tcpActivity = TcpMainActivity.getInstance();
        if (tcpActivity == null) return;

        // Process all messages in the queue
        while (!tcpActivity.getMessageQueue().isEmpty()) {
            String msg = tcpActivity.getMessageQueue().poll(); // Remove and process the oldest message
            if (msg == null || !msg.startsWith("AC_")) {
                updateTerminal("⚠️ Invalid feedback: " + msg);
                continue;
            }

            // Parse feedback
            String acID1 = msg.substring(3, 6); // Extract AC ID (e.g., "001")
            String state1 = msg.substring(6);   // Extract state ("1" or "0")
            updateTerminal("Received feedback: AC" + acID1 + " state: " + state1);

            // Check if this feedback matches a pending command
            PendingCommand pending = pendingCommands.get(acID1);
            if (pending == null) {
                updateTerminal("⚠️ No pending command for AC" + acID1);
                continue;
            }

            // Process the feedback
            if ("1".equals(state1) && pending.isChecked) {
                // Positive feedback for ON
                updateIconColor(pending.icon, R.color.green);
                pending.status.isPowerOn = true;
                updateTerminal("✅ AC" + acID1 + " turned ON");
            } else if ("0".equals(state1) && !pending.isChecked) {
                // Positive feedback for OFF
                updateIconColor(pending.icon, R.color.gray);
                pending.status.isPowerOn = false;
                updateTerminal("✅ AC" + acID1 + " turned OFF");
            } else {
                // Negative or unexpected feedback
                updateIconColor(pending.icon, pending.wasOn ? R.color.green : R.color.gray);
                pending.status.isPowerOn = pending.wasOn;
                acPowerSwitch.setChecked(pending.wasOn);
                updateTerminal("❌ Unexpected feedback for AC" + acID1 + ": " + state1);
            }

            // Remove the processed command
            pendingCommands.remove(acID1);
        }
    }


    private void startListeningForMessages() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {  // 🆕 Use controlled loop instead of infinite loop
                    // Check if tcpActivity is not null before accessing the method
                    if (tcpActivity1 != null) {
                        // Get the latest message from TcpMainActivity
                        String message = tcpActivity1.getLatestESPMessageS();

                        // 🆕 Add logging for debugging purposes
                        Log.d("MainActivity", "Received Message: " + message);

                        // If message is not null and non-empty, update the terminal
                        if (message != null && !message.isEmpty()) {
                            // Use runOnUiThread to update the UI on the main thread
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Call the updateTerminal method to append the message
                                    updateTerminal("startListening: " + message);
                                }
                            });
                        }
                    }

                    // 🆕 Add sleep to avoid consuming too much CPU (adjust time as necessary)
                    try {
                        Thread.sleep(100);  // Sleep for 100ms to reduce CPU usage
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();  // Handle thread interruption
                    }
                }
            }
        }).start();
    }

    // Method to continuously listen for TCP messages and update the terminal
       /* private void startListeningForMessages() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    // Check if tcpActivity is not null before accessing the method
                    if (tcpActivity1 != null) {
                        // Get the latest message from TcpMainActivity
                        String message = tcpActivity1.getLatestESPMessageS();

                        // If message is not null and non-empty, update the terminal
                        if (message != null && !message.isEmpty()) {
                            // Use runOnUiThread to update the UI on the main thread
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Call the updateTerminal method to append the message
                                    updateTerminal("startListening: "+message);
                                }
                            });
                        }
                    }

                    // Sleep for a small time to avoid high CPU usage
                    try {
                        Thread.sleep(100);  // Adjust the delay as needed
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }    */

    // Replace promptESPAssignment with:
    public void promptESPAssignment(String espID) {
        TcpMainActivity tcpActivity = TcpMainActivity.getInstance();
        if (tcpActivity == null || !tcpActivity.isTcpConnected()) {
            Toast.makeText(this, "Not connected to any ESP", Toast.LENGTH_SHORT).show();
            return;
        }

        isInAssignmentMode = true;
        currentESPID = espID != null ? espID : tcpActivity.getConnectedEspId();

        if (currentESPID != null) {
            Toast.makeText(this, "Tap an AC icon to assign " + currentESPID, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No ESP ID available", Toast.LENGTH_SHORT).show();
            isInAssignmentMode = false;
        }
    }

    private void assignESPToIcon(String espID, ImageView icon) {
        String originalTag = icon.getTag().toString();

        // Update mapping
        espToAcMap.put(espID, originalTag);
        icon.setTag(espID); // Now shows ESP ID

        // Visual update
        Toast.makeText(this,
                originalTag + " → " + espID,
                Toast.LENGTH_SHORT).show();

        // Send assignment message via TCP
        JSONObject messageJson = new JSONObject();
        try {
            messageJson.put("command", "ASSIGN");
            messageJson.put("acID", originalTag);
            messageJson.put("espID", espID);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        byte[] structuredMessage = TcpHelper.composeMessage(messageJson.toString(), 0, (byte) 1);
        TcpHelper.sendMessage(new String(structuredMessage, StandardCharsets.US_ASCII));

        updateTerminal("✅ ESP " + espID + " assigned to " + originalTag);
    }

    private void restoreOriginalClickListeners() {
        for (ImageView icon : acStatusMap.keySet()) {
            icon.setOnClickListener(v -> selectACIcon(icon));
        }
    }

    private void disableIconSelectionMode() {
        for (ImageView icon : acStatusMap.keySet()) {
            // Restore original colors
            ACStatus status = acStatusMap.get(icon);
            updateIconColor(icon, status.isPowerOn ? R.color.green : R.color.gray);
            // Restore original click listener
            icon.setOnClickListener(v -> selectACIcon(icon));
        }
    }

    private void showACSelectionDialog(String espID) {
        List<String> availableACs = getAvailableACIconIDs();
        String[] acNames = availableACs.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Select AC to assign")
                .setItems(acNames, (dialog, which) -> {
                    String selectedAC = acNames[which];
                    mapESPToAC(espID, selectedAC);
                    Toast.makeText(this, "✅ Mapped ESP " + espID + " to " + selectedAC, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    public List<String> getAvailableACIconIDs() {
        List<String> ids = new ArrayList<>();
        for (ImageView icon : acStatusMap.keySet()) {
            ids.add(icon.getTag().toString());
        }
        return ids;
    }
    private void mapESPToAC(String espID, String acTag) {
        espToAcMap.put(espID, acTag);
        // Loop through all icons and find the one with matching tag (existing code)
        for (ImageView icon : acStatusMap.keySet()) {
            String iconTag = icon.getTag().toString().trim();
            if (iconTag.equals(acTag.trim())) {
                // Extract the number from the ESP ID (e.g., "ESP_001" -> "001")
                String espNumber = espID.replaceAll("\\D+", "");
                icon.setTag("ESP_" + espNumber);
                updateTerminal("✅ ESP " + espID + " assigned to " + acTag
                        + " → new tag: " + icon.getTag());
                break;
            }
        }

        // **NEW** – Compose and send the Type 3 "Change ID and Password" message
        String espNumber = espID.replaceAll("\\D+", "");              // numeric part of ESP ID
        String messageBody = "ID:" + espNumber + ",PASS:12345678";    // body with default password
        // Build header fields
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
                .format(new Date());
        //String msgIdStr = String.format("%02d", messageCounter);     // two-digit message ID&#8203;:contentReference[oaicite:4]{index=4}
        //messageCounter = (messageCounter + 1) % 100;                 // increment and rollover after 99&#8203;:contentReference[oaicite:5]{index=5}
        String destId = "000";                                       // broadcast destination&#8203;:contentReference[oaicite:6]{index=6}
        String lengthStr = String.format("%05d", messageBody.length()); // 5-digit body length&#8203;:contentReference[oaicite:7]{index=7}
        // Concatenate all parts: "MOSQ" + version + timestamp + type + msgId + dest + length + body
        //String fullMessage = "MOSQ" + "1" + timestamp + "3" + msgIdStr + destId + lengthStr + messageBody;
        // Send the message via TCP and log it to the terminal
       // TcpHelper.sendMessage(fullMessage);
        //updateTerminal("📧 Sent Type 3 message: " + fullMessage);

        Toast.makeText(this, "✅ Mapped ESP " + espID + " to " + acTag, Toast.LENGTH_SHORT).show();
    }
    // Add this method to handle the numbering dialog
    private void showNumberingDialog(ImageView selectedIcon, String espSSID) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Number AC Icon");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter new ESP ID (0–99)");
        builder.setView(input);

        builder.setPositiveButton("Assign", (dialog, which) -> {
            String acNumber = input.getText().toString().trim();
            if (!acNumber.isEmpty()) {
                try {
                    int parsed = Integer.parseInt(acNumber);
                    if (parsed < 0 || parsed > 99) {
                        Toast.makeText(this, "Only numbers from 0 to 99 are allowed", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Format as AC_XXX (e.g., 4 → AC_004)
                    String acTag = "AC_" + String.format("%03d", parsed);
                    assignESPWithNumber(selectedIcon, espSSID, acTag, acNumber);

                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    // Modified assignment method
    private void assignESPWithNumber(ImageView icon, String espSSID, String acTag, String newUserID) {
        // ✅ Extract and format the ID of the passed icon (e.g., AC_3 → 003)
        String oldID = acTag.replaceAll("\\D+", ""); // Extract digits only
        oldID = String.format("%03d", Integer.parseInt(oldID)); // Format to 3 digits
        int acoldId= acID;
        espToAcMap.put(espSSID, acTag);
        icon.setTag(acTag);
        updateIconColor(icon, R.color.green);

        // 🔹 Destination: from the passed icon ID
        int destinationID = Integer.parseInt(oldID);
        int destinationID1 = 0;
        int connectedEspID = Integer.parseInt(currentESPID);
        if(acoldId != connectedEspID)
            destinationID1 = connectedEspID;
        else destinationID1 = acoldId;

        // 🔹 New ID: from dialog input (e.g., "4" → "004")
        String newID = String.format("%03d", Integer.parseInt(newUserID));

        // 🔹 Message body
        String messageBody = "ID:" + newID + ",PASS:12345678";

        // 🔹 Compose and send
        byte[] structuredMessage = TcpHelper.composeMessage(messageBody, destinationID1, (byte) 3);
        TcpHelper.sendMessage(new String(structuredMessage, StandardCharsets.US_ASCII));

        updateTerminal("📤 Sent to AC_" + oldID + ": ID → " + newID);
        Toast.makeText(this, "Assigned new ID " + newID + " to AC_" + oldID, Toast.LENGTH_SHORT).show();
    }


    public List<DailyPrayerTimes> getNext30DaysPrayerTimes(String city) {
        List<DailyPrayerTimes> prayerTimesList = new ArrayList<>();

        // 1. Get coordinates
        Coordinates coordinates;
        switch (city) {
            case "Riyadh":
                coordinates = new Coordinates(24.7136, 46.6753);
                break;
            case "Jeddah":
                coordinates = new Coordinates(21.4858, 39.1925);
                break;
            case "Mecca":
                coordinates = new Coordinates(21.4225, 39.8262);
                break;
            default:
                Toast.makeText(this, "Invalid city selected", Toast.LENGTH_SHORT).show();
                return prayerTimesList;
        }

        LocalDate today = LocalDate.now();
        CalculationParameters params = CalculationMethod.UMM_AL_QURA.getParameters();

        // 2. Calculate for next 30 days
        for (int i = 0; i < 30; i++) {
            LocalDate date = today.plusDays(i);
            DateComponents dateComponents = new DateComponents(
                    date.getYear(),
                    date.getMonthValue(),
                    date.getDayOfMonth()
            );

            PrayerTimes times = new PrayerTimes(coordinates, dateComponents, params);

            // 3. Convert kotlinx.datetime.Instant to java.util.Date
            DailyPrayerTimes dailyTimes = new DailyPrayerTimes(
                    date,
                    convertToDate(times.timeForPrayer(com.batoulapps.adhan2.Prayer.FAJR)),
                    convertToDate(times.timeForPrayer(com.batoulapps.adhan2.Prayer.SUNRISE)),
                    convertToDate(times.timeForPrayer(com.batoulapps.adhan2.Prayer.DHUHR)),
                    convertToDate(times.timeForPrayer(com.batoulapps.adhan2.Prayer.ASR)),
                    convertToDate(times.timeForPrayer(com.batoulapps.adhan2.Prayer.MAGHRIB)),
                    convertToDate(times.timeForPrayer(com.batoulapps.adhan2.Prayer.ISHA))
            );

            prayerTimesList.add(dailyTimes);
            logDailyTimes(dailyTimes);
        }

        return prayerTimesList;
    }
    private void send30DayPrayerSchedule(String city) {
        try {
            Coordinates coordinates;
            switch (city) {
                case "Riyadh":
                    coordinates = new Coordinates(24.7136, 46.6753);
                    break;
                case "Jeddah":
                    coordinates = new Coordinates(21.4858, 39.1925);
                    break;
                case "Mecca":
                    coordinates = new Coordinates(21.4225, 39.8262);
                    break;
                default:
                    coordinates = new Coordinates(24.7136, 46.6753); // default to Riyadh
            }

            CalculationParameters params = CalculationMethod.UMM_AL_QURA.getParameters();

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            LocalDate today = LocalDate.now();
            DateComponents todayDate = new DateComponents(today.getYear(), today.getMonthValue(), today.getDayOfMonth());
            PrayerTimes todayTimes = new PrayerTimes(coordinates, todayDate, params);
            ZoneId zoneId = TimeZone.getDefault().toZoneId();

            String fajr = toJavaInstant(todayTimes.timeForPrayer(Prayer.FAJR)).atZone(zoneId).toLocalTime().minusMinutes(fajrBeforeTime).format(timeFormatter)
                    + " F " +
                    toJavaInstant(todayTimes.timeForPrayer(Prayer.FAJR)).atZone(zoneId).toLocalTime().plusMinutes(fajrAfterTime).format(timeFormatter);

            String dhuhr = toJavaInstant(todayTimes.timeForPrayer(Prayer.DHUHR)).atZone(zoneId).toLocalTime().minusMinutes(dhuhrBeforeTime).format(timeFormatter)
                    + " D " +
                    toJavaInstant(todayTimes.timeForPrayer(Prayer.DHUHR)).atZone(zoneId).toLocalTime().plusMinutes(dhuhrAfterTime).format(timeFormatter);

            String asr = toJavaInstant(todayTimes.timeForPrayer(Prayer.ASR)).atZone(zoneId).toLocalTime().minusMinutes(asrBeforeTime).format(timeFormatter)
                    + " A " +
                    toJavaInstant(todayTimes.timeForPrayer(Prayer.ASR)).atZone(zoneId).toLocalTime().plusMinutes(asrAfterTime).format(timeFormatter);

            String maghrib = toJavaInstant(todayTimes.timeForPrayer(Prayer.MAGHRIB)).atZone(zoneId).toLocalTime().minusMinutes(maghribBeforeTime).format(timeFormatter)
                    + " M " +
                    toJavaInstant(todayTimes.timeForPrayer(Prayer.MAGHRIB)).atZone(zoneId).toLocalTime().plusMinutes(maghribAfterTime).format(timeFormatter);

            String isha = toJavaInstant(todayTimes.timeForPrayer(Prayer.ISHA)).atZone(zoneId).toLocalTime().minusMinutes(ishaBeforeTime).format(timeFormatter)
                    + " I " +
                    toJavaInstant(todayTimes.timeForPrayer(Prayer.ISHA)).atZone(zoneId).toLocalTime().plusMinutes(ishaAfterTime).format(timeFormatter);

            String baseDay = today.format(dateFormatter); // e.g., 2024/04/09

            StringBuilder scheduleBuilder = new StringBuilder();
            scheduleBuilder.append(baseDay)
                    .append(" ").append(fajr)
                    .append(" ").append(dhuhr)
                    .append(" ").append(asr)
                    .append(" ").append(maghrib)
                    .append(" ").append(isha);

            for (int i = 1; i < 30; i++) {
                scheduleBuilder.append(" ").append(fajr)
                        .append(" ").append(dhuhr)
                        .append(" ").append(asr)
                        .append(" ").append(maghrib)
                        .append(" ").append(isha);
            }

            String messageBody = scheduleBuilder.toString();
            byte messageType = 2;


            byte[] structuredMessage = TcpHelper.composeMessage(messageBody, 0, messageType);

            // ✅ Send the message
            String finalMessage = new String(structuredMessage, StandardCharsets.US_ASCII);
            TcpHelper.sendMessage(finalMessage);

            runOnUiThread(() -> updateTerminal("📤 Sent 30-day schedule:\n" + finalMessage));
        } catch (Exception e) {
            Log.e("PrayerSchedule", "❌ Failed to build/send schedule", e);
            runOnUiThread(() -> updateTerminal("❌ Error generating or sending schedule: " + e.getMessage()));
        }
    }






    // ✅ Helper: Convert kotlinx.datetime.Instant → java.util.Date
    private Date convertToDate(kotlinx.datetime.Instant instant) {
        return Date.from(java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds()));
    }

    // ✅ Model class for one day's prayer times
    public class DailyPrayerTimes {
        public LocalDate date;
        public Date fajr, sunrise, dhuhr, asr, maghrib, isha;

        public DailyPrayerTimes(LocalDate date, Date fajr, Date sunrise,
                                Date dhuhr, Date asr, Date maghrib, Date isha) {
            this.date = date;
            this.fajr = fajr;
            this.sunrise = sunrise;
            this.dhuhr = dhuhr;
            this.asr = asr;
            this.maghrib = maghrib;
            this.isha = isha;
        }
    }

    // ✅ Logging
    private void logDailyTimes(DailyPrayerTimes daily) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
        Log.d("PrayerTimes", String.format(Locale.US,
                "%s - Fajr: %s, Sunrise: %s, Dhuhr: %s, Asr: %s, Maghrib: %s, Isha: %s",
                daily.date,
                sdf.format(daily.fajr),
                sdf.format(daily.sunrise),
                sdf.format(daily.dhuhr),
                sdf.format(daily.asr),
                sdf.format(daily.maghrib),
                sdf.format(daily.isha))
        );
    }







    @Override
    protected void onResume() {
        super.onResume();
        updateConnectionStatusUI(); // ✅ Refresh status every time this page resumes
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Ensure the new intent is used

        // Check if we need to show the setup dialog
        if (intent.getBooleanExtra("openSetupDialog", false)) {
            showSetupDialog();
        }
    }

    // Class-level variables to retain the user input for prayer times
    private int fajrBeforeTime = 5;
    private int fajrAfterTime = 5;
    private int dhuhrBeforeTime = 5;
    private int dhuhrAfterTime = 5;
    private int asrBeforeTime = 15;
    private int asrAfterTime = 15;
    private int maghribBeforeTime = 15;
    private int maghribAfterTime = 15;
    private int ishaBeforeTime = 15;
    private int ishaAfterTime = 15;

    private void showSetupDialog() {
        // Create an AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Inflate the dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_setup, null);


        // Find expandable sections
        LinearLayout acConfigSection = dialogView.findViewById(R.id.ac_config_section);
        LinearLayout prayerConfigSection = dialogView.findViewById(R.id.prayer_config_section);
        TextView acConfigHeader = dialogView.findViewById(R.id.ac_config_header);
        TextView prayerConfigHeader = dialogView.findViewById(R.id.prayer_config_header);

        // Find dialog views
        Spinner citySpinner = dialogView.findViewById(R.id.city_spinner);
        Spinner brandSpinner = dialogView.findViewById(R.id.ac_brand);

        // AC Numbering Input Fields in Setup Dialog
        EditText setupACUp = dialogView.findViewById(R.id.setup_editText_ac_up);
        EditText setupACLeft = dialogView.findViewById(R.id.setup_editText_ac_left);
        EditText setupACRight = dialogView.findViewById(R.id.setup_editText_ac_right);
        EditText setupACDown = dialogView.findViewById(R.id.setup_editText_ac_down);

        // AC Numbering Input Fields in Main Screen
        EditText mainACUp = findViewById(R.id.editText_ac_up);
        EditText mainACLeft = findViewById(R.id.editText_ac_left);
        EditText mainACRight = findViewById(R.id.editText_ac_right);
        EditText mainACDown = findViewById(R.id.editText_ac_down);

        // Prayer Time Input Fields
        EditText fajrBefore = dialogView.findViewById(R.id.edit_fajr_before);
        EditText fajrAfter = dialogView.findViewById(R.id.edit_fajr_after);
        EditText dhuhrBefore = dialogView.findViewById(R.id.edit_dhuhr_before);
        EditText dhuhrAfter = dialogView.findViewById(R.id.edit_dhuhr_after);
        EditText asrBefore = dialogView.findViewById(R.id.edit_asr_before);
        EditText asrAfter = dialogView.findViewById(R.id.edit_asr_after);
        EditText maghribBefore = dialogView.findViewById(R.id.edit_maghrib_before);
        EditText maghribAfter = dialogView.findViewById(R.id.edit_maghrib_after);
        EditText ishaBefore = dialogView.findViewById(R.id.edit_isha_before);
        EditText ishaAfter = dialogView.findViewById(R.id.edit_isha_after);

        // Prayer Time Display TextViews
        TextView tvFajrTime = dialogView.findViewById(R.id.tv_fajr_time);
        TextView tvDhuhrTime = dialogView.findViewById(R.id.tv_dhuhr_time);
        TextView tvAsrTime = dialogView.findViewById(R.id.tv_asr_time);
        TextView tvMaghribTime = dialogView.findViewById(R.id.tv_maghrib_time);
        TextView tvIshaTime = dialogView.findViewById(R.id.tv_isha_time);
        TextView systemTimeTextView = dialogView.findViewById(R.id.tv_system_time);
        TextView espTimeTextView = dialogView.findViewById(R.id.tv_esp_time); // ✅ Correct

        // Buttons
        // Button prayerTimesButton = dialogView.findViewById(R.id.btn_prayer_times);
        Button confirmButton = dialogView.findViewById(R.id.button_confirm);
        Button cancelButton = dialogView.findViewById(R.id.button_cancel);
        Switch testModeSwitch = dialogView.findViewById(R.id.testmode_switch);
        Switch testModeSwitchDefault = dialogView.findViewById(R.id.testmode_swtichDefault);

        Button send30DayButton = dialogView.findViewById(R.id.btn_send_30_day_schedule);
        send30DayButton.setOnClickListener(v -> {
            try {
                String city = citySpinner.getSelectedItem() != null ? citySpinner.getSelectedItem().toString() : "Riyadh";

                send30DayPrayerSchedule(city);  // ✅ Correct method name

                Toast.makeText(this, " 30-day schedule sent", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("PrayerSchedule", " Failed to send 30-day schedule", e);
                Toast.makeText(this, "️ Error sending schedule", Toast.LENGTH_SHORT).show();
            }
        });

        // Populate the city spinner
        ArrayAdapter<CharSequence> cityAdapter = ArrayAdapter.createFromResource(
                this, R.array.city_list, android.R.layout.simple_spinner_item);
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        citySpinner.setAdapter(cityAdapter);

        // Populate the brand spinner
        ArrayAdapter<CharSequence> brandAdapter = ArrayAdapter.createFromResource(
                this, R.array.ac_brands, android.R.layout.simple_spinner_item);
        brandAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        brandSpinner.setAdapter(brandAdapter);

        // Toggle dropdown sections
        acConfigHeader.setOnClickListener(v -> {
            acConfigSection.setVisibility(acConfigSection.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        prayerConfigHeader.setOnClickListener(v -> {
            prayerConfigSection.setVisibility(prayerConfigSection.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        // Initialize previous AC counts to prevent crashes
        int previousUpCount = parseEditTextValue(mainACUp, 0);
        int previousLeftCount = parseEditTextValue(mainACLeft, 0);
        int previousRightCount = parseEditTextValue(mainACRight, 0);
        int previousDownCount = parseEditTextValue(mainACDown, 0);

        // Set switch states to **remember previous values**
        testModeSwitch.setChecked(isTestMode);
        testModeSwitchDefault.setChecked(isTestModeDefault);

        // Set default values in setup dialog
        setupACUp.setText(String.valueOf(previousUpCount));
        setupACLeft.setText(String.valueOf(previousLeftCount));
        setupACRight.setText(String.valueOf(previousRightCount));
        setupACDown.setText(String.valueOf(previousDownCount));



        // Set the dialog's view and show
        builder.setView(dialogView);
        // Handler to update time every second
        Handler timeHandler = new Handler();

        Runnable updateTimeRunnable = new Runnable() {
            final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            //String espMessage = "20230412155955";  // Example ESP message

            long espClockStartMillis = 0;  // Store the start time
            long espTimeInMillis = 0;  // Store the elapsed ESP time in milliseconds


            @Override
            public void run() {
                SimpleDateFormat espSdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()); // Changed to match the format of espMessage

                String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                long now = System.currentTimeMillis();

                systemTimeTextView.setText("System Time: " + currentTime);
                //String espTime = sdf.format(now + 30000);  // 30-second delay
                //espTimeTextView.setText("ESP Time: " +  espTime);

                /*/ 🟢 Update ESP Time if available
                TcpMainActivity tcp = TcpMainActivity.getInstance();
                if (tcp != null) {
                    String espMsg = tcp.getLatestESPMessage();
                    if (espMsg != null && espMsg.length() >= 14 && espMsg.matches("\\d{14}")) {
                        String formattedEspTime = espMsg.substring(8,10) + ":" + espMsg.substring(10,12) + ":" + espMsg.substring(12,14);
                        if (espTimeTextView != null) {
                            espTimeTextView.setText("ESP Time: " + formattedEspTime);
                        } else {
                            // 🟠 **Log error if espTimeTextView is null** (this will help us catch any potential issues)
                            Log.e("EspClock", "espTimeTextView is null");
                        }

                    } else {
                        // 🟠 **Log an error if ESP message is invalid**
                        Log.e("EspClock", "Invalid ESP message received.");
                    }
                } else {
                    // 🟠 **Log an error if TcpMainActivity is null**
                    Log.e("EspClock", "TcpMainActivity instance is null.");
                }

                timeHandler.postDelayed(this, 1000); */ // Update every second // Last worknig one

                // 🟢 Update ESP Time if available
                TcpMainActivity tcp = TcpMainActivity.getInstance();
                if (tcp != null) {
                    String espMsg = tcp.getLatestESPMessage();
                    if (espMsg != null && espMsg.length() >= 14 && espMsg.matches("\\d{14}")) {
                        //String formattedEspTime = espMsg.substring(8,10) + ":" + espMsg.substring(10,12) + ":" + espMsg.substring(12,14);
                        if (espClockStartMillis == 0) {  // Only set this once

                            try {
                                Date espDate = espSdf.parse(espMsg);  // Parsing the ESP message into Date
                                espClockStartMillis = espDate.getTime();  // Get the time in milliseconds from ESP message
                                espTimeInMillis = espClockStartMillis;  // Initialize the start time in millis

                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        } //if espClockStartMillis


                        // Increase the ESP time by 1 second every tick
                        espTimeInMillis += 1000;  // Add 1 second (1000 milliseconds)

                        // Format the ESP time to display in HH:mm:ss format
                        String espTime = sdf.format(new Date(espTimeInMillis));  // Update espTime based on ESP clock

                        if (espTimeTextView != null) {
                            espTimeTextView.setText("ESP Time: " + espTime);  // Display ticking ESP time
                        } else {
                            // 🟠 **Log error if espTimeTextView is null** (this will help us catch any potential issues)
                            Log.e("EspClock", "espTimeTextView is null");
                        }

                    } else {
                        // 🟠 **Log an error if ESP message is invalid**
                        Log.e("EspClock", "Invalid ESP message received.");
                    }
                } else {
                    // 🟠 **Log an error if TcpMainActivity is null**
                    Log.e("EspClock", "TcpMainActivity instance is null.");
                }

                /*/ 🟢 Process ESP Time once (when it's the first time or after restart) Last Working espTime Ticking
                if (espClockStartMillis == 0) {  // Only set this once

                    try {
                        Date espDate = espSdf.parse(espMessage);  // Parsing the ESP message into Date
                        espClockStartMillis = espDate.getTime();  // Get the time in milliseconds from ESP message
                        espTimeInMillis = espClockStartMillis;  // Initialize the start time in millis

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } //if espClockStartMillis


                // Increase the ESP time by 1 second every tick
                espTimeInMillis += 1000;  // Add 1 second (1000 milliseconds)

                // Format the ESP time to display in HH:mm:ss format
                String espTime = sdf.format(new Date(espTimeInMillis));  // Update espTime based on ESP clock

                // Set the ESP time on the espTimeTextView
                espTimeTextView.setText("ESP Time: " + espTime);  */ // Display ticking ESP time


                // 🟡 Re-run the timeHandler to update both system time and ESP time every second
                timeHandler.postDelayed(this, 1000);  // Ticking every second
            }
        };// end of Times clocks

// Start updating time
        handler.post(updateTimeRunnable);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Handle city selection change
        citySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCity = citySpinner.getSelectedItem().toString();
                Coordinates coordinates = getCoordinatesForCity(selectedCity);
                CalculationParameters params = CalculationMethod.MUSLIM_WORLD_LEAGUE.getParameters();
                ZoneId zoneId = TimeZone.getDefault().toZoneId();
                LocalDate today = LocalDate.now();
                DateComponents date = new DateComponents(today.getYear(), today.getMonthValue(), today.getDayOfMonth());
                PrayerTimes prayerTimes = new PrayerTimes(coordinates, date, params);
                updatePrayerTimesForCity(prayerTimes, tvFajrTime, tvDhuhrTime, tvAsrTime, tvMaghribTime, tvIshaTime);

                //updatePrayerTimesForCity(selectedCity, tvFajrTime, tvDhuhrTime, tvAsrTime, tvMaghribTime, tvIshaTime);
                updateTerminal("Updated prayer times for: " + selectedCity);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Confirm button functionality
        confirmButton.setOnClickListener(v -> {
            // Preserve previous prayer time settings
            fajrBefore.setText(String.valueOf(fajrBeforeTime));
            fajrAfter.setText(String.valueOf(fajrAfterTime));
            dhuhrBefore.setText(String.valueOf(dhuhrBeforeTime));
            dhuhrAfter.setText(String.valueOf(dhuhrAfterTime));
            asrBefore.setText(String.valueOf(asrBeforeTime));
            asrAfter.setText(String.valueOf(asrAfterTime));
            maghribBefore.setText(String.valueOf(maghribBeforeTime));
            maghribAfter.setText(String.valueOf(maghribAfterTime));
            ishaBefore.setText(String.valueOf(ishaBeforeTime));
            ishaAfter.setText(String.valueOf(ishaAfterTime));

            // Get new AC values
            int upCount = parseEditTextValue(setupACUp, previousUpCount);
            int leftCount = parseEditTextValue(setupACLeft, previousLeftCount);
            int rightCount = parseEditTextValue(setupACRight, previousRightCount);
            int downCount = parseEditTextValue(setupACDown, previousDownCount);
            boolean isTestModeSelected = testModeSwitch.isChecked();
            boolean isTestModeDefaultSelected = testModeSwitchDefault.isChecked();

            // Update the main screen input fields
            mainACUp.setText(String.valueOf(upCount));
            mainACLeft.setText(String.valueOf(leftCount));
            mainACRight.setText(String.valueOf(rightCount));
            mainACDown.setText(String.valueOf(downCount));

            // Hide the main AC input fields after values are set
            mainACUp.setVisibility(View.GONE);
            mainACLeft.setVisibility(View.GONE);
            mainACRight.setVisibility(View.GONE);
            mainACDown.setVisibility(View.GONE);

            // Update AC icons only if numbers changed
            if (previousUpCount != upCount) updateACIcons(upACContainer, upCount, "UP");
            if (previousLeftCount != leftCount) updateACIcons(leftACContainer, leftCount, "LEFT");
            if (previousRightCount != rightCount) updateACIcons(rightACContainer, rightCount, "RIGHT");
            if (previousDownCount != downCount) updateACIcons(downACContainer, downCount, "DOWN");

            // Update AC brand for selected ACs in terminal
            String selectedBrand = brandSpinner.getSelectedItem().toString();
            for (ImageView icon : selectedIcons) {
                ACStatus status = acStatusMap.get(icon);
                status.brand = selectedBrand;
                updateTerminal("AC " + icon.getTag() + " brand is " + selectedBrand);

                String messageBody1 = selectedBrand;
                // Message Type for control commands is 1
                byte messageType = 0;  // Control Command
                // 🆕 Set acID to "000" if all ACs are selected
                if (allSelected) {
                    acID = Integer.parseInt("000");
                    updateTerminal("Broadcast acID 000 since allSelected = "+ allSelected);
                }

                // Call the TcpHelper to compose the structured message
                byte[] structuredMessage = TcpHelper.composeMessage(messageBody1, acID, messageType);

                // Send the structured message via TCP
                TcpHelper.sendMessage(new String(structuredMessage, StandardCharsets.US_ASCII));
            }


            // Ensure the switch is not null before setting the listener
            // Apply test mode selections AFTER user confirms
            if (isTestModeSelected) {
                isTestMode = true;
                Toast.makeText(this, "Random Test Mode Activated", Toast.LENGTH_SHORT).show();
                testACControlScheduler();
            } else {
                isTestMode = false;
                if (scheduler != null && !scheduler.isShutdown()) {
                    scheduler.shutdownNow();
                }
                Toast.makeText(this, "Random Test Mode Deactivated", Toast.LENGTH_SHORT).show();
            }

            if (isTestModeDefaultSelected) {
                isTestModeDefault = true;
                Toast.makeText(this, "Test Mode Activated", Toast.LENGTH_SHORT).show();
                testACControlScheduler2();
            } else {
                isTestModeDefault = false;
                if (scheduler != null && !scheduler.isShutdown()) {
                    scheduler.shutdownNow();
                }
                Toast.makeText(this, "Test Mode Deactivated", Toast.LENGTH_SHORT).show();
            }

            // Ensure prayer times are set correctly
            if (!isTestMode && !isTestModeDefault) {
                Toast.makeText(this, "Prayer Time Scheduling Activated", Toast.LENGTH_SHORT).show();

                String selectedCity = citySpinner.getSelectedItem().toString();
                PrayerTimes currentPrayerTimes = getPrayerTimes(selectedCity); // ✅ Now uses selected city


                // Start the AC scheduling with user-defined prayer times
                //startACControlTimer(currentPrayerTimes,
                  //      fajrBeforeTime, fajrAfterTime, dhuhrBeforeTime, dhuhrAfterTime,
                    //    asrBeforeTime, asrAfterTime, maghribBeforeTime, maghribAfterTime,
                      //  ishaBeforeTime, ishaAfterTime);
            }


            handler.removeCallbacks(updateTimeRunnable);
            // Close the dialog
            dialog.dismiss();
        });
        cancelButton.setOnClickListener(v -> {
            handler.removeCallbacks(updateTimeRunnable); // Simply dismiss the dialog without saving any changes
            dialog.dismiss();
        });
    }





    /*/ Helper method to update prayer times based on selected city
    private void updatePrayerTimesForCity(String city, TextView tvFajr, TextView tvDhuhr, TextView tvAsr, TextView tvMaghrib, TextView tvIsha) {
        String apiUrl = "https://api.aladhan.com/v1/timingsByCity?city=" + city + "&country=Saudi%20Arabia&method=2";

        // Create a new request queue
        RequestQueue queue = Volley.newRequestQueue(this);

        // Create a JSON Object request
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, apiUrl, null,
                response -> {
                    try {
                        JSONObject timings = response.getJSONObject("data").getJSONObject("timings");

                        // Get prayer times
                        String fajrTime = timings.getString("Fajr");
                        String dhuhrTime = timings.getString("Dhuhr");
                        String asrTime = timings.getString("Asr");
                        String maghribTime = timings.getString("Maghrib");
                        String ishaTime = timings.getString("Isha");

                        // Update TextViews with real-time prayer times
                        tvFajr.setText(fajrTime);
                        tvDhuhr.setText(dhuhrTime);
                        tvAsr.setText(asrTime);
                        tvMaghrib.setText(maghribTime);
                        tvIsha.setText(ishaTime);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing prayer times", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // Handle errors
                    Toast.makeText(this, "Failed to fetch prayer times", Toast.LENGTH_SHORT).show();
                });

        // Add the request to the queue
        queue.add(jsonObjectRequest);
    }     // updatePrayerTimes method

    private void updatePrayerTimesForCity(PrayerTimes prayerTimes, ZoneId zoneId,
                                   TextView tvFajr, TextView tvDhuhr, TextView tvAsr,
                                   TextView tvMaghrib, TextView tvIsha) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        kotlinx.datetime.Instant kFajr = prayerTimes.timeForPrayer(com.batoulapps.adhan2.Prayer.FAJR);
        kotlinx.datetime.Instant kDhuhr = prayerTimes.timeForPrayer(com.batoulapps.adhan2.Prayer.DHUHR);
        kotlinx.datetime.Instant kAsr = prayerTimes.timeForPrayer(com.batoulapps.adhan2.Prayer.ASR);
        kotlinx.datetime.Instant kMaghrib = prayerTimes.timeForPrayer(com.batoulapps.adhan2.Prayer.MAGHRIB);
        kotlinx.datetime.Instant kIsha = prayerTimes.timeForPrayer(com.batoulapps.adhan2.Prayer.ISHA);

        tvFajr.setText(formatPrayerTime(kFajr, zoneId, formatter));
        tvDhuhr.setText(formatPrayerTime(kDhuhr, zoneId, formatter));
        tvAsr.setText(formatPrayerTime(kAsr, zoneId, formatter));
        tvMaghrib.setText(formatPrayerTime(kMaghrib, zoneId, formatter));
        tvIsha.setText(formatPrayerTime(kIsha, zoneId, formatter));
    }   */

    // ✅ This helper replicates your exact getAdjustedTime base logic, just no offset

    private void updatePrayerTimesForCity(PrayerTimes prayerTimes,
                                          TextView tvFajr, TextView tvDhuhr, TextView tvAsr,
                                          TextView tvMaghrib, TextView tvIsha) {

        ZoneId zoneId = TimeZone.getDefault().toZoneId();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        tvFajr.setText(getAdjustedTime(prayerTimes, com.batoulapps.adhan2.Prayer.FAJR, 0, zoneId).format(formatter));
        tvDhuhr.setText(getAdjustedTime(prayerTimes, com.batoulapps.adhan2.Prayer.DHUHR, 0, zoneId).format(formatter));
        tvAsr.setText(getAdjustedTime(prayerTimes, com.batoulapps.adhan2.Prayer.ASR, 0, zoneId).format(formatter));
        tvMaghrib.setText(getAdjustedTime(prayerTimes, com.batoulapps.adhan2.Prayer.MAGHRIB, 0, zoneId).format(formatter));
        tvIsha.setText(getAdjustedTime(prayerTimes, com.batoulapps.adhan2.Prayer.ISHA, 0, zoneId).format(formatter));
    }

    private String formatPrayerTime(PrayerTimes prayerTimes,
                                    com.batoulapps.adhan2.Prayer prayer,
                                    ZoneId zoneId,
                                    DateTimeFormatter formatter) {
        kotlinx.datetime.Instant kInstant = prayerTimes.timeForPrayer(prayer);
        if (kInstant == null) return "--:--";

        Instant javaInstant = Instant.ofEpochMilli(kInstant.toEpochMilliseconds());
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(javaInstant, zoneId);

        return zonedDateTime.toLocalTime().format(formatter);
    }

    private String formatPrayerTime(kotlinx.datetime.Instant kInstant, ZoneId zoneId, DateTimeFormatter formatter) {
        if (kInstant == null) return "--:--";
        Instant javaInstant = Instant.ofEpochMilli(kInstant.toEpochMilliseconds());
        return ZonedDateTime.ofInstant(javaInstant, zoneId).toLocalTime().format(formatter);
    }

    private Coordinates getCoordinatesForCity(String city) {
        switch (city) {
            case "Riyadh":
                return new Coordinates(24.7136, 46.6753);
            case "Jeddah":
                return new Coordinates(21.4858, 39.1925);
            case "Dammam":
                return new Coordinates(26.3927, 49.9777);
            case "Tabuk":
                return new Coordinates(28.3838, 36.5550);
            case "Mecca":
                return new Coordinates(21.4225, 39.8262);
            default:
                return new Coordinates(24.7136, 46.6753); // Fallback: Riyadh
        }
    }






    private String getPrayerTime(String city, String prayer) {
        // Example of a static prayer time retrieval method
        HashMap<String, HashMap<String, String>> cityPrayerTimes = new HashMap<>();

        // Example: Define some mock prayer times for different cities
        HashMap<String, String> riyadhTimes = new HashMap<>();
        riyadhTimes.put("Fajr", "04:30");
        riyadhTimes.put("Dhuhr", "12:15");
        riyadhTimes.put("Asr", "15:45");
        riyadhTimes.put("Maghrib", "18:30");
        riyadhTimes.put("Isha", "20:00");
        cityPrayerTimes.put("Riyadh", riyadhTimes);

        // Fetch prayer time for the given city and prayer
        if (cityPrayerTimes.containsKey(city)) {
            return cityPrayerTimes.get(city).getOrDefault(prayer, "--:--");
        }
        return "--:--"; // Default if not found
    }








    // Helper to update AC icons
    private void updateACIcons(LinearLayout container, int newCount, String direction) {
        // Reset counters only when updating icons
        if (direction.equalsIgnoreCase("UP")) topCounter = 1;
        if (direction.equalsIgnoreCase("RIGHT")) rightCounter = 0;
        if (direction.equalsIgnoreCase("DOWN")) bottomCounter = 0;
        if (direction.equalsIgnoreCase("LEFT")) leftCounter = 0;

        // Clear previously selected icons if the number of icons changes
        selectedIcons.clear();

        // Remove all existing views in the container
        container.removeAllViews();

        // Generate the new icons
        generateIcons(container, newCount, direction);
    }







    // Helper to parse EditText values safely
    private int parseEditTextValue(EditText editText, int defaultValue) {
        try {
            return Integer.parseInt(editText.getText().toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }







    private void showPrayerTimes(String selectedCity, int fajrBefore, int fajrAfter, int dhuhrBefore, int dhuhrAfter,
                                 int asrBefore, int asrAfter, int maghribBefore, int maghribAfter, int ishaBefore, int ishaAfter) {
        Coordinates coordinates;

        // Map the city to its coordinates
        switch (selectedCity) {
            case "Riyadh":
                coordinates = new Coordinates(24.7136, 46.6753);
                break;
            case "Jeddah":
                coordinates = new Coordinates(21.4858, 39.1925);
                break;
            case "Mecca":
                coordinates = new Coordinates(21.4225, 39.8262);
                break;
            default:
                Toast.makeText(this, "City not recognized", Toast.LENGTH_SHORT).show();
                return;
        }

        // Use Umm al-Qura calculation method
        CalculationParameters params = CalculationMethod.UMM_AL_QURA.getParameters();

        // Get today's date using Adhan's DateComponents
        java.time.LocalDate today = java.time.LocalDate.now();
        DateComponents date = new DateComponents(today.getYear(), today.getMonthValue(), today.getDayOfMonth());

        // Calculate prayer times
        PrayerTimes prayerTimes = new PrayerTimes(coordinates, date, params);

        // Convert prayer times to the local timezone
        ZoneId zoneId = TimeZone.getDefault().toZoneId();

        // Prepare prayer times output
        StringBuilder prayerTimesOutput = new StringBuilder("Prayer Times for " + selectedCity + " (Today):\n");
        prayerTimesOutput.append("Fajr: ").append(formatPrayerTime(toJavaInstant(prayerTimes.timeForPrayer(com.batoulapps.adhan2.Prayer.FAJR)), zoneId))
                .append(", ").append(fajrBefore).append(" minutes before, ").append(fajrAfter).append(" minutes after\n");
        prayerTimesOutput.append("Dhuhr: ").append(formatPrayerTime(toJavaInstant(prayerTimes.timeForPrayer(com.batoulapps.adhan2.Prayer.DHUHR)), zoneId))
                .append(", ").append(dhuhrBefore).append(" minutes before, ").append(dhuhrAfter).append(" minutes after\n");
        prayerTimesOutput.append("Asr: ").append(formatPrayerTime(toJavaInstant(prayerTimes.timeForPrayer(com.batoulapps.adhan2.Prayer.ASR)), zoneId))
                .append(", ").append(asrBefore).append(" minutes before, ").append(asrAfter).append(" minutes after\n");
        prayerTimesOutput.append("Maghrib: ").append(formatPrayerTime(toJavaInstant(prayerTimes.timeForPrayer(com.batoulapps.adhan2.Prayer.MAGHRIB)), zoneId))
                .append(", ").append(maghribBefore).append(" minutes before, ").append(maghribAfter).append(" minutes after\n");
        prayerTimesOutput.append("Isha: ").append(formatPrayerTime(toJavaInstant(prayerTimes.timeForPrayer(com.batoulapps.adhan2.Prayer.ISHA)), zoneId))
                .append(", ").append(ishaBefore).append(" minutes before, ").append(ishaAfter).append(" minutes after\n");

        // Display the prayer times in an AlertDialog
        new AlertDialog.Builder(this)
                .setTitle("Prayer Times for " + selectedCity)
                .setMessage(prayerTimesOutput.toString())
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();

        // Update the terminal output with prayer times
        updateTerminal(prayerTimesOutput.toString());

        // Build the scheduling message in JSON format
        JSONObject messageJson = new JSONObject();
        try {
            messageJson.put("prayer_times", prayerTimesOutput.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Convert the JSON object to a string
        String messageBody = messageJson.toString();

        // Message Type for scheduling is 3
        byte messageType = 2;

        // Compose and send the structured message via TCP
        byte[] structuredMessage = TcpHelper.composeMessage(messageBody, 0, messageType);
        TcpHelper.sendMessage(new String(structuredMessage, StandardCharsets.US_ASCII));
    }

    private PrayerTimes getPrayerTimes(String selectedCity) {
        // Define coordinates based on the selected city
        Coordinates coordinates;
        switch (selectedCity) {
            case "Riyadh":
                coordinates = new Coordinates(24.7136, 46.6753);
                break;
            case "Jeddah":
                coordinates = new Coordinates(21.4858, 39.1925);
                break;
            case "Mecca":
                coordinates = new Coordinates(21.4225, 39.8262);
                break;
            default:
                coordinates = new Coordinates(24.7136, 46.6753); // ✅ Fallback to Riyadh
                Toast.makeText(this, "City not recognized, using Riyadh", Toast.LENGTH_SHORT).show();
                break;
        }

        // Use Umm al-Qura calculation method
        CalculationParameters params = CalculationMethod.UMM_AL_QURA.getParameters();

        // Get today's date using Adhan's DateComponents
        java.time.LocalDate today = java.time.LocalDate.now();
        DateComponents date = new DateComponents(today.getYear(), today.getMonthValue(), today.getDayOfMonth());

        // Return calculated prayer times
        return new PrayerTimes(coordinates, date, params);
    }





    // Helper method to format prayer times
    private String formatPrayerTime(java.time.Instant prayerTime, ZoneId zoneId) {
        if (prayerTime == null) {
            return "N/A"; // Handle cases where a prayer time might not be available
        }
        return prayerTime.atZone(zoneId).toLocalTime().toString();
    }

    // ✅ FIXED: This method correctly adjusts the prayer time and returns a formatted string
    private String formatAdjustedTime(LocalDateTime prayerTime, int offsetMinutes, ZoneId zoneId) {
        return prayerTime.plusMinutes(offsetMinutes)
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }


    // Function to start timer to control the AC based on prayer times
    private void startACControlTimer(PrayerTimes prayerTimes, int fajrBefore, int fajrAfter,
                                     int dhuhrBefore, int dhuhrAfter, int asrBefore, int asrAfter,
                                     int maghribBefore, int maghribAfter, int ishaBefore, int ishaAfter) {
        // Make sure the scheduler is only created once and stopped properly
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow(); // Stop the previous scheduler if any
        }
        scheduler = Executors.newScheduledThreadPool(1);
        ZoneId zoneId = TimeZone.getDefault().toZoneId();

        Runnable acControlTask = new Runnable() {
            @Override
            public void run() {
                try {
                    LocalDateTime now = LocalDateTime.now();

                    // Calculate prayer times with adjustments
                    LocalDateTime fajrOn = getAdjustedTime(prayerTimes, com.batoulapps.adhan2.Prayer.FAJR, -fajrBefore, zoneId);
                    LocalDateTime fajrOff = getAdjustedTime(prayerTimes, com.batoulapps.adhan2.Prayer.FAJR, fajrAfter, zoneId);

                    LocalDateTime dhuhrOn = getAdjustedTime(prayerTimes, com.batoulapps.adhan2.Prayer.DHUHR, -dhuhrBefore, zoneId);
                    LocalDateTime dhuhrOff = getAdjustedTime(prayerTimes, com.batoulapps.adhan2.Prayer.DHUHR, dhuhrAfter, zoneId);

                    LocalDateTime asrOn = getAdjustedTime(prayerTimes, com.batoulapps.adhan2.Prayer.ASR, -asrBefore, zoneId);
                    LocalDateTime asrOff = getAdjustedTime(prayerTimes, com.batoulapps.adhan2.Prayer.ASR, asrAfter, zoneId);

                    LocalDateTime maghribOn = getAdjustedTime(prayerTimes, com.batoulapps.adhan2.Prayer.MAGHRIB, -maghribBefore, zoneId);
                    LocalDateTime maghribOff = getAdjustedTime(prayerTimes, com.batoulapps.adhan2.Prayer.MAGHRIB, maghribAfter, zoneId);

                    LocalDateTime ishaOn = getAdjustedTime(prayerTimes, com.batoulapps.adhan2.Prayer.ISHA, -ishaBefore, zoneId);
                    LocalDateTime ishaOff = getAdjustedTime(prayerTimes, com.batoulapps.adhan2.Prayer.ISHA, ishaAfter, zoneId);

                    // Control AC based on the calculated prayer time ranges
                    if (isTestMode) { controlAC(fajrOn, fajrOff, now);
                        controlAC(dhuhrOn, dhuhrOff, now);
                        controlAC(asrOn, asrOff, now);
                        controlAC(maghribOn, maghribOff, now);
                        controlAC(ishaOn, ishaOff, now);
                    }
                    else if (isTestModeDefault) {
                        controlAC2(fajrOn, fajrOff, now);
                        controlAC2(dhuhrOn, dhuhrOff, now);
                        controlAC2(asrOn, asrOff, now);
                        controlAC2(maghribOn, maghribOff, now);
                        controlAC2(ishaOn, ishaOff, now);

                    }
                    else {
                        // ✅ Only reset schedulePrinted if any of the user-entered times changed
                        boolean scheduleChanged = (fajrBefore != lastFajrBefore || fajrAfter != lastFajrAfter ||
                                dhuhrBefore != lastDhuhrBefore || dhuhrAfter != lastDhuhrAfter ||
                                asrBefore != lastAsrBefore || asrAfter != lastAsrAfter ||
                                maghribBefore != lastMaghribBefore || maghribAfter != lastMaghribAfter ||
                                ishaBefore != lastIshaBefore || ishaAfter != lastIshaAfter);

                        if (scheduleChanged) {
                            lastPrintedSchedule = "";  // ✅ Reset last printed schedule only if needed
                            schedulePrinted = false;  // ✅ Allow printing only if input times changed

                            // ✅ Store the new prayer time inputs
                            lastFajrBefore = fajrBefore;
                            lastFajrAfter = fajrAfter;
                            lastDhuhrBefore = dhuhrBefore;
                            lastDhuhrAfter = dhuhrAfter;
                            lastAsrBefore = asrBefore;
                            lastAsrAfter = asrAfter;
                            lastMaghribBefore = maghribBefore;
                            lastMaghribAfter = maghribAfter;
                            lastIshaBefore = ishaBefore;
                            lastIshaAfter = ishaAfter;
                        }
                        printScheduledCommands(now, fajrOn, fajrOff, dhuhrOn, dhuhrOff, asrOn, asrOff, maghribOn, maghribOff, ishaOn, ishaOff);
                        controlAC3(fajrOn, fajrOff, now);
                        controlAC3(dhuhrOn, dhuhrOff, now);
                        controlAC3(asrOn, asrOff, now);
                        controlAC3(maghribOn, maghribOff, now);
                        controlAC3(ishaOn, ishaOff, now);
                        Log.d("ACControl", "Calculated Fajr ON Time: " + fajrOn.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                        Log.d("ACControl", "Calculated Fajr OFF Time: " + fajrOff.format(DateTimeFormatter.ofPattern("HH:mm:ss")));

                    }

                } catch (Exception e) {
                    Log.e("ACControl", "Error during prayer time comparison", e);
                }
            }
        };

        // Schedule the task to run periodically
        scheduler.scheduleWithFixedDelay(acControlTask, 0, 4, TimeUnit.SECONDS); // delay was 3 last time
    }

    private void printScheduledCommands(LocalDateTime now, LocalDateTime... times) {
        StringBuilder allCommands = new StringBuilder();

        for (int i = 0; i < times.length; i += 2) {
            LocalDateTime onTime = times[i];
            LocalDateTime offTime = times[i + 1];

            allCommands.append("ACs will turn ON at ")
                    .append(onTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                    .append("\n");

            allCommands.append("ACs will turn OFF at ")
                    .append(offTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                    .append("\n");
        }

        String newSchedule = allCommands.toString().trim();

        if (!newSchedule.equals(lastPrintedSchedule) || !schedulePrinted ) { // ✅ Print only if the schedule has changed
            lastPrintedSchedule = newSchedule; // ✅ Update last printed schedule
            schedulePrinted = true; // ✅ Mark as printed

              runOnUiThread(() -> updateTerminal(newSchedule)); // commented ACs will turn ON at
        }
    }


    /*/ default one
    private LocalDateTime getAdjustedTime(PrayerTimes prayerTimes, com.batoulapps.adhan2.Prayer prayer, int offsetMinutes, ZoneId zoneId) {
        Instant instant = toJavaInstant(prayerTimes.timeForPrayer(prayer));
        return LocalDateTime.ofInstant(instant, zoneId).plusMinutes(offsetMinutes);
    }  */

    // ✅ 1. NEW CLEAN WORKING VERSION — Put this inside MainActivity or Utils class
// ✅ Fixed: No UTC shifting. Correct local time conversion
// ✅ Final, tested, accurate: converts to ZonedDateTime first
    private LocalDateTime getAdjustedTime(PrayerTimes prayerTimes, com.batoulapps.adhan2.Prayer prayer, int offsetMinutes, ZoneId zoneId) {
        kotlinx.datetime.Instant kInstant = prayerTimes.timeForPrayer(prayer);
        if (kInstant == null) return null;

        // ✅ Step 1: Convert to Java Instant correctly
        Instant javaInstant = Instant.ofEpochMilli(kInstant.toEpochMilliseconds());

        // ✅ Step 2: Convert to ZonedDateTime with your zone
        ZonedDateTime zonedTime = ZonedDateTime.ofInstant(javaInstant, zoneId);
        Log.d("AdjustedTime", "Base: " + zonedTime.toLocalTime() + ", Offset: " + offsetMinutes + ", Result: " + zonedTime.plusMinutes(offsetMinutes).toLocalTime());


        // ✅ Step 3: Apply minute offset in proper local time context
        return zonedTime.plusMinutes(offsetMinutes).toLocalDateTime();
    }



    // Function to control AC based on prayer times
    private void controlAC(LocalDateTime onTime, LocalDateTime offTime, LocalDateTime now) {
        // If in test mode, toggle every 3 seconds
        /*if (isTestMode) {
        boolean turnOn = now.isBefore(offTime); // ✅ FIX: Turns ON before OFF time, then OFF after
        runOnUiThread(() -> toggleAC(turnOn));
        return;
        }
      */
        // If in test mode, toggle every 3 seconds
        runOnUiThread(() -> {
            if (now.isEqual(onTime) || now.isAfter(onTime) && now.isBefore(offTime) ) {
                toggleAC(true); // ✅ Turns ON
            }

            else if (now.isEqual(offTime) || now.isAfter(offTime) ) {
                toggleAC(false); // ✅ Ensures OFF executes
            }
        });
    }

    private void controlAC3(LocalDateTime onTime, LocalDateTime offTime, LocalDateTime now) {
        runOnUiThread(() -> {
            Log.d("ACControl", "Current Time: " + now.format(DateTimeFormatter.ofPattern("HH:mm:ss")) +
                    " | Checking ON: " + onTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) +
                    " | Checking OFF: " + offTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));

            if (now.isEqual(onTime) || now.isAfter(onTime) && now.isBefore(offTime)) {
                toggleAC(true);
                Log.d("ACControl", "✅ AC Turned ON");
            } else if (now.isEqual(offTime) || now.isAfter(offTime)) {
                toggleAC(false);
                Log.d("ACControl", "❌ AC Turned OFF");
            }
        });
    }



    private void toggleAC(boolean turnOn) {
        runOnUiThread(() -> { // Ensure UI updates on the main thread
            for (ImageView icon : selectedIcons) {
                if (acStatusMap.containsKey(icon)) { // Ensure icon exists in AC status map
                    ACStatus status = acStatusMap.get(icon);
                    status.isPowerOn = turnOn;
                    status.temperature = turnOn ? DEFAULT_ON_TEMP : 0;
                    updateIconColor(icon, turnOn ? R.color.green : R.color.gray);

                    String messageBody1 = turnOn ? "ON" : "OFF";
                    // Message Type for control commands is 1
                    byte messageType = 1;  // Control Command

                    // Call the TcpHelper to compose the structured message
                    byte[] structuredMessage = TcpHelper.composeMessage(messageBody1, acID, messageType);

                    // Send the structured message via TCP
                    //TcpHelper.sendMessage(new String(structuredMessage, StandardCharsets.US_ASCII));
                    // updateTerminal( icon.getTag() + " TestMode switched " + (turnOn ? "ON" : "OFF") +
                    // (turnOn ? " with temperature " + DEFAULT_ON_TEMP + "°C" : ""));
                }
            }
        });
    }


    // for test mode
    private LocalDateTime testgetAdjustedTime(PrayerTimes prayerTimes, com.batoulapps.adhan2.Prayer prayer, int offsetMinutes, ZoneId zoneId) {
        //Instant instant = Instant.now(); // Use current time for testing
        // return LocalDateTime.ofInstant(instant, zoneId).plusMinutes(offsetMinutes);
        LocalDateTime now = LocalDateTime.now();
        return now.plusMinutes(offsetMinutes);  // Offset by the specified minutes for test
    }

    private void testACControlScheduler() { //Random Testing
        try {
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdownNow(); // Ensure old scheduler is stopped
            }
            scheduler = Executors.newScheduledThreadPool(1);
            LocalDateTime now = LocalDateTime.now();
            ZoneId zoneId = TimeZone.getDefault().toZoneId();
            // Call existing AC initialization function
            generateTestACs();
            //  Select a random number of ACs (2 to 5)
            int maxSelectableACs = acStatusMap.size(); // Get the total number of available ACs
            int randomSelectionCount = maxSelectableACs > 0 ? 1 + new Random().nextInt(maxSelectableACs) : 0; // Ensure it's within range
            selectRandomACsForTest(randomSelectionCount);
            // Simulated test times (1 min apart for quick testing)
            PrayerTimes mockPrayerTimesInstance = new PrayerTimes(
                    new Coordinates(0, 0), // Dummy coordinates
                    new DateComponents(now.getYear(), now.getMonthValue(), now.getDayOfMonth()),
                    CalculationMethod.UMM_AL_QURA.getParameters()
            );
            startACControlTimer(mockPrayerTimesInstance, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);

            Runnable testTask = new Runnable() {
                private int cycleCount = 0;
                private boolean isOn=false;

                @Override
                public void run() {
                    try {
                        /*if (cycleCount >= 6) {
                          logTestModeAction("✅ Test Mode Completed.");
                            scheduler.shutdown();
                            return;
                        } */
                        LocalDateTime updatedNow = LocalDateTime.now();  // Update current time dynamically
                        // Simulated prayer times
                        LocalDateTime testOn = updatedNow; // AC should turn ON in 5 seconds
                        LocalDateTime testOff = updatedNow.plusSeconds(3); // AC should turn OFF 5 sec later

                        //int maxSelectableACs = acStatusMap.size(); // Get the total number of available ACs
                        //int randomSelectionCount = maxSelectableACs > 0 ? 1 + new Random().nextInt(maxSelectableACs) : 0; // Ensure it's within range
                        //selectRandomACsForTest(randomSelectionCount);
                        //  Trigger Additional Control Tests (Fan, Swing, Temperature)
                        controlAC(testOn, testOff, updatedNow);
                        fanButton.performClick();
                        swingSwitch.setChecked(!swingSwitch.isChecked());
                        increaseTempButton.performClick();
                        decreaseTempButton.performClick();

                        // cycleCount++;

                    } catch (Exception e) {
                        Log.e("ACControl", "Error in test AC control", e);
                    }
                }
            };

            // Run test mode every second
            scheduler.scheduleWithFixedDelay(testTask, 0, 2, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e("ACControl", "Error in testing AC control", e);
        }
    }


    // Function to control AC based on prayer times
    private void controlAC2(LocalDateTime onTime, LocalDateTime offTime, LocalDateTime now) {
        final boolean[] isOn = {false};
        // /*
        // If in test mode, toggle every 3 seconds
        if (isTestMode) {
            isOn[0] = !isOn[0]; // Toggle the ON/OFF state
            runOnUiThread(() -> toggleAC(isOn[0])); // Update UI & send command
            return; }// Exit function early in test mode
        // */
        if (now.isEqual(onTime) || now.isAfter(onTime) && now.isBefore(offTime)) {
            runOnUiThread(() -> toggleAC(true)); // Ensure UI updates on the main thread
        } else if (now.isEqual(offTime) || now.isAfter(offTime)) {
            runOnUiThread(() -> toggleAC(false)); // Ensure UI updates on the main thread
        }
    }

    private void testACControlScheduler2() {
        try {
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdownNow(); // Ensure old scheduler is stopped
            }
            scheduler = Executors.newScheduledThreadPool(1);

            LocalDateTime now = LocalDateTime.now();
            ZoneId zoneId = TimeZone.getDefault().toZoneId();

            // Simulated test times (1 min apart for quick testing)
            PrayerTimes mockPrayerTimesInstance = new PrayerTimes(
                    new Coordinates(0, 0), // Dummy coordinates
                    new DateComponents(now.getYear(), now.getMonthValue(), now.getDayOfMonth()),
                    CalculationMethod.UMM_AL_QURA.getParameters()
            );
            startACControlTimer(mockPrayerTimesInstance, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);



            Runnable testTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        LocalDateTime updatedNow = LocalDateTime.now();  // Update current time dynamically

                        // Simulated prayer times
                        LocalDateTime testOn = updatedNow; // AC should turn ON in 5 seconds
                        LocalDateTime testOff = testOn.plusSeconds(2);  // AC should turn OFF 5 sec later

                        controlAC(testOn, testOff, updatedNow);
                    } catch (Exception e) {
                        Log.e("ACControl", "Error in test AC control", e);
                    }
                }
            };

            // Run test mode every second
            scheduler.scheduleWithFixedDelay(testTask, 0, 1, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e("ACControl", "Error in testing AC control", e);
        }
    }


    private void generateTestACs() {
        logTestModeAction(" Initializing Test ACs...");
        // 🔹 Initialize a random number of ACs (4 to 8)
        int testACsUP = 1 + new Random().nextInt(4);
        int testACsDown = 1 + new Random().nextInt(4);
        int testACsLeft = 1 + new Random().nextInt(4);
        int testACsRight = 1 + new Random().nextInt(4);


        if (parseEditTextValue(editTextACUp, 0) != testACsUP) {
            updateACIcons(upACContainer, testACsUP, "UP");
        }
        if (parseEditTextValue(editTextACLeft, 0) != testACsLeft) {
            updateACIcons(leftACContainer, testACsLeft, "LEFT");
        }
        if (parseEditTextValue(editTextACRight, 0) != testACsRight) {
            updateACIcons(rightACContainer, testACsRight, "RIGHT");
        }
        if (parseEditTextValue(editTextACDown, 0) != testACsDown) {
            updateACIcons(downACContainer, testACsDown, "DOWN");
        }



        //Simulate user entering AC counts
        editTextACUp.setText(String.valueOf(testACsUP));
        editTextACDown.setText(String.valueOf(testACsDown));
        editTextACLeft.setText(String.valueOf(testACsLeft));
        editTextACRight.setText(String.valueOf(testACsRight));
        simulateEnterPress(editTextACUp);
        simulateEnterPress(editTextACDown);
        simulateEnterPress(editTextACLeft);
        simulateEnterPress(editTextACRight);
/*
        //Trigger existing AC creation method
        generateIconsForDirection(editTextACUp, EditorInfo.IME_ACTION_DONE, null);
        generateIconsForDirection(editTextACDown, EditorInfo.IME_ACTION_DONE, null);
        generateIconsForDirection(editTextACLeft, EditorInfo.IME_ACTION_DONE, null);
        generateIconsForDirection(editTextACRight, EditorInfo.IME_ACTION_DONE, null);
       */

    }

    private void selectRandomACsForTest(int count) {
        selectedIcons.clear(); // Reset selection
        List<ImageView> availableACs = new ArrayList<>(acStatusMap.keySet());

        Random randomy = new Random();
        for (int i = 1; i < count && !availableACs.isEmpty(); i++) {
            ImageView ac = availableACs.remove(randomy.nextInt(availableACs.size()));
            selectACIcon(ac); // Use existing selection method
        }

        logTestModeAction("✅ Selected " + count + " random ACs for test.");
    }


    private void simulateEnterPress(EditText editText) {
        editText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
        editText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
    }

    private void logTestModeAction(String message) {
        runOnUiThread(() -> updateTerminal("TEST MODE: " + message));
    }

    // Helper method to format prayer times






    // Helper method to convert kotlinx.datetime.Instant to java.time.Instant
    private java.time.Instant toJavaInstant(kotlinx.datetime.Instant kotlinxInstant) {
        if (kotlinxInstant == null) {
            return null; // Handle null cases
        }
        return java.time.Instant.ofEpochMilli(kotlinxInstant.toEpochMilliseconds());
    }

    private void updateTerminal(String message) {
        terminalOutput.append("\n" + message);
        terminalScroll.postDelayed(() -> terminalScroll.fullScroll(View.FOCUS_DOWN), 50);
    }

    private void clearTerminal() {
        terminalOutput.setText(""); // 🧼 Clear the terminal text
        updateTerminal("Terminal cleared"); // Optional log line
    }


    private void setACInputListeners() {
        editTextACUp.setOnEditorActionListener(this::generateIconsForDirection);
        editTextACDown.setOnEditorActionListener(this::generateIconsForDirection);
        editTextACLeft.setOnEditorActionListener(this::generateIconsForDirection);
        editTextACRight.setOnEditorActionListener(this::generateIconsForDirection);
    }

    private int parseEditTextValue1(EditText editText, int defaultValue) {
        try {
            String text = editText.getText().toString().trim();
            if (text.isEmpty()) return 0;
            int value = Integer.parseInt(text);
            return value >= 0 ? value : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }


   /* private boolean generateIconsForDirection(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            LinearLayout container;
            int iconCount;

            try {
                if (v == editTextACUp) {
                    container = upACContainer;
                    iconCount = Integer.parseInt(editTextACUp.getText().toString());
                    generateIcons(container, iconCount, "UP");
                } else if (v == editTextACRight) {
                    container = rightACContainer;
                    iconCount = Integer.parseInt(editTextACRight.getText().toString());
                    generateIcons(container, iconCount, "RIGHT");
                } else if (v == editTextACDown) {
                    container = downACContainer;
                    iconCount = Integer.parseInt(editTextACDown.getText().toString());
                    generateIcons(container, iconCount, "DOWN");
                } else {
                    container = leftACContainer;
                    iconCount = Integer.parseInt(editTextACLeft.getText().toString());
                    generateIcons(container, iconCount, "LEFT");
                }

                v.clearFocus();
                v.setVisibility(View.GONE);

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
    }     // generate Icons for Direction last working 11/4 April


        private boolean generateIconsForDirection(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            LinearLayout container;
            int iconCount = 0;

            try {
                if (v == editTextACUp) {
                    container = upACContainer;
                    iconCount = parseEditTextValue(editTextACUp, 0);
                    if (iconCount == 0) return true; // 🛑 Skip if 0
                    generateIcons(container, iconCount, "UP");
                } else if (v == editTextACRight) {
                    container = rightACContainer;
                    iconCount = parseEditTextValue(editTextACRight, 0);
                    if (iconCount == 0) return true; // 🛑 Skip if 0
                    generateIcons(container, iconCount, "RIGHT");
                } else if (v == editTextACDown) {
                    container = downACContainer;
                    iconCount = parseEditTextValue(editTextACDown, 0);
                    if (iconCount == 0) return true; // 🛑 Skip if 0
                    generateIcons(container, iconCount, "DOWN");
                } else {
                    container = leftACContainer;
                    iconCount = parseEditTextValue(editTextACLeft, 0);
                    if (iconCount == 0) return true; // 🛑 Skip if 0
                    generateIcons(container, iconCount, "LEFT");
                }

                v.clearFocus();
                v.setVisibility(View.GONE);

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
    }


    */

    private boolean isValidACCount(int count, String direction) {
        if (count < 0 || count > 13) {
            Toast.makeText(this, " Please enter a number between 0 and 13", Toast.LENGTH_SHORT).show();
            //updateTerminal("❌ Invalid count: " + count + " for " + direction);
            return false;
        }
        return true;
    }

    private boolean generateIconsForDirection(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            LinearLayout container;
            int iconCount = 0;

            try {
                if (v == editTextACUp) {
                    container = upACContainer;
                    iconCount = parseEditTextValue(editTextACUp, 0);
                    if (!isValidACCount(iconCount, "UP")) return true; // 🆕 Range check
                    if (iconCount == 0) {
                        v.setVisibility(View.GONE); // 🆕 hide
                        updateTerminal("ℹ️ No ACs for UP — field hidden"); // 🆕 log
                        return true; // ✅ Prevent crash
                    }                    // 🛑 Skip if 0 instead hide it
                    generateIcons(container, iconCount, "UP");

                } else if (v == editTextACRight) {
                    container = rightACContainer;
                    iconCount = parseEditTextValue(editTextACRight, 0);
                    if (!isValidACCount(iconCount, "RIGHT")) return true; // 🆕 Range check
                    if (iconCount == 0) {
                        v.setVisibility(View.GONE); // 🆕 hide
                        updateTerminal("ℹ️ No ACs for UP — field hidden"); // 🆕 log
                        return true; // ✅ Prevent crash
                    }                    // 🛑 Skip if 0
                    generateIcons(container, iconCount, "RIGHT");

                } else if (v == editTextACDown) {
                    container = downACContainer;
                    iconCount = parseEditTextValue(editTextACDown, 0);
                    if (!isValidACCount(iconCount, "DOWN")) return true; // 🆕 Range check
                    if (iconCount == 0) {
                        v.setVisibility(View.GONE); // 🆕 hide
                        updateTerminal("ℹ️ No ACs for UP — field hidden"); // 🆕 log
                        return true; // ✅ Prevent crash
                    }                    generateIcons(container, iconCount, "DOWN");

                } else {
                    container = leftACContainer;
                    iconCount = parseEditTextValue(editTextACLeft, 0);
                    if (!isValidACCount(iconCount, "LEFT")) return true; // 🆕 Range check
                    if (iconCount == 0) {
                        v.setVisibility(View.GONE); // 🆕 hide
                        updateTerminal("ℹ️ No ACs for UP — field hidden"); // 🆕 log
                        return true; // ✅ Prevent crash
                    }                    generateIcons(container, iconCount, "LEFT");
                }

                v.clearFocus();
                v.setVisibility(View.GONE);

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }

            return true;
        }
        return false;
    }


    private void generateIcons(LinearLayout container, int count, String direction) {
        container.removeAllViews(); // Clear the container before adding new icons
        int spacing = Math.max(5, 50 / count);
        for (int i = 0; i < count; i++) {
            addIconToContainer(container, direction, spacing);
        }
    }


    private int topCounter = 1;
    private int rightCounter = 0;
    private int bottomCounter = 0;
    private int leftCounter = 0;

    private void addIconToContainer(LinearLayout container, String direction, int spacing) {
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_ac_icon));
        icon.setColorFilter(ContextCompat.getColor(this, R.color.gray));

        // Determine the icon number based on the direction
        int iconNumber;
        switch (direction.toUpperCase()) {
            case "UP":
                iconNumber = topCounter++;
                break;
            case "RIGHT":
                if (rightCounter == 0) rightCounter = topCounter; // Initialize rightCounter after UP
                iconNumber = rightCounter++;
                break;
            case "DOWN":
                if (bottomCounter == 0) bottomCounter = rightCounter; // Initialize bottomCounter after RIGHT
                iconNumber = bottomCounter++;
                break;
            case "LEFT":
                if (leftCounter == 0) leftCounter = bottomCounter; // Initialize leftCounter after BOTTOM
                iconNumber = leftCounter++;
                break;
            default:
                throw new IllegalArgumentException("Invalid direction: " + direction);
        }

        // Set the tag for the icon
        icon.setTag("AC " + String.format("%03d", iconNumber));
        acID = iconNumber;

        // Set layout parameters
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(spacing, spacing, spacing, spacing);
        icon.setLayoutParams(params);

        icon.setOnClickListener(v -> selectACIcon(icon));
        container.addView(icon);
        acStatusMap.put(icon, new ACStatus(false, 0, "Default Brand", false, 1));

        // Build the JSON message body for AC initialization
        JSONObject messageJson = new JSONObject();
        try {
            messageJson.put("acID", icon.getTag().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String messageBody1 = "AcID"+icon.getTag().toString();


        // Convert the JSON object to a string
        String messageBody = messageJson.toString();
        // Message Type for setting is 2
        byte messageType = 0; // Setting message

        // Call TcpHelper to compose the structured message
        byte[] structuredMessage = TcpHelper.composeMessage(messageBody, acID, messageType);

        // Send the structured message via TCP
        TcpHelper.sendMessage(new String(structuredMessage, StandardCharsets.US_ASCII));
    }




    private void selectACIcon(ImageView icon) {
        if (selectedIcons.contains(icon)) {
            selectedIcons.remove(icon);
            ACStatus status = acStatusMap.get(icon);
            updateIconColor(icon, status.isPowerOn ? R.color.green : R.color.gray);
        } else {
            selectedIcons.add(icon);
            updateIconColor(icon, R.color.blue);
            displayACStatus(icon);
        }
        updateAllSelectedStatus(); // new line for Allselected bool
    }

    private void toggleSelectAllACIcons() {
        if (selectedIcons.size() == acStatusMap.size()) {
            selectedIcons.clear();
            for (ImageView icon : acStatusMap.keySet()) {
                ACStatus status = acStatusMap.get(icon);
                updateIconColor(icon, status.isPowerOn ? R.color.green : R.color.gray);
            }
        } else {
            selectedIcons.clear();
            selectedIcons.addAll(acStatusMap.keySet());
            for (ImageView icon : selectedIcons) {
                updateIconColor(icon, R.color.blue);
            }
        }
        updateAllSelectedStatus(); // new line for Allselected bool

    }

    private void updateAllSelectedStatus() {
        boolean newAllSelected = selectedIcons.size() == acStatusMap.size();
        if (newAllSelected != allSelected) {
            allSelected = newAllSelected;
            updateTerminal("AllSelected = " + allSelected);
        }
    }



    private void toggleTestMode() {
        isTestMode = !isTestMode;
        if (isTestMode) {
            testModeIndicator.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Test Mode Activated", Toast.LENGTH_SHORT).show(); // Inform user
        } else {
            testModeIndicator.setVisibility(View.GONE);
            Toast.makeText(this, "Test Mode Deactivated", Toast.LENGTH_SHORT).show(); // Inform user
        }
    }


    private void updateTemperature(boolean increase) {
        for (ImageView icon : selectedIcons) {
            ACStatus status = acStatusMap.get(icon);
            //if (status.isPowerOn) { start of if
              // int newTemp status.temperature += increase ? 1 : -1; Last working code 4/11 April
            int newTemp = status.temperature + (increase ? 1 : -1);

            // 🛡 Clamp temp between 16 and 30
            if (newTemp < 16) newTemp = 16;
            if (newTemp > 30) newTemp = 30;

            //  If no change, don't send or update
            if (newTemp == status.temperature) {
                updateTerminal("Temp already at limit " + newTemp + "°C for " + icon.getTag());
                continue;
            }

            status.temperature = newTemp;

                // Update BOTH the main temperature TextView and the central display
                tvCurrentTemp.setText("Temprature: " + status.temperature + "°C");
                tempDisplay.setText(status.temperature + "°C"); // Update number between buttons

                // Send control message via TCP
                JSONObject messageJson = new JSONObject();
                try {
                    messageJson.put("temperature", status.temperature);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                byte messageType = 1;  // Control Command
                byte[] structuredMessage = TcpHelper.composeMessage(messageJson.toString(), acID, messageType);
                TcpHelper.sendMessage(new String(structuredMessage, StandardCharsets.US_ASCII));

                updateTerminal("Temperature set to " + status.temperature + "°C for " + icon.getTag());
            //} if power ON ends
        }
    }

    private void updateIconColor(ImageView icon, int colorId) {
        icon.setColorFilter(ContextCompat.getColor(this, colorId));
    }



    private void displayACStatus(ImageView icon) {
        ACStatus status = acStatusMap.get(icon);
        String statusMessage = icon.getTag() + " " + status.brand + ": " +
                (status.isPowerOn ? "ON, " + status.temperature + "°C, Fan " + status.fanLevel + (status.swing ? ", Swing" : "")
                        : "OFF");
        Toast.makeText(this, statusMessage, Toast.LENGTH_SHORT).show();
    }


    public static MainActivity getInstance(){
        return instance;
    }


    private class ACStatus {
        boolean isPowerOn;
        int temperature;
        String brand;
        boolean swing;
        int fanLevel;

        ACStatus(boolean isPowerOn, int temperature, String brand, boolean swing, int fanLevel) {
            this.isPowerOn = isPowerOn;
            this.temperature = temperature;
            this.brand = brand;
            this.swing = swing;
            this.fanLevel = fanLevel;
        }
    }

    private void updateConnectionStatusUI() {
        boolean isWifi = TcpMainActivity.getInstance() != null &&
                TcpMainActivity.getInstance().isWifiConnected1();
        boolean isTcp = TcpMainActivity.getInstance() != null &&
                TcpMainActivity.getInstance().isTcpConnected();

        if (wifiStatus != null && tcpStatus != null) {
            wifiStatus.setColorFilter(ContextCompat.getColor(this, isWifi ? R.color.green : R.color.red));
            tcpStatus.setColorFilter(ContextCompat.getColor(this, isTcp ? R.color.green : R.color.red));
        }
    }

   /* private void startEspClockTicker(final long espClockStartMillis) {
        // 🟡 Create a handler to update the clock every second
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 🟢 Calculate the elapsed time since the ESP start time
                long currentTimeMillis = System.currentTimeMillis();
                long elapsedMillis = currentTimeMillis - espClockStartMillis;

                // 🟡 Format the updated time (HH:mm:ss) based on the elapsed time
                Date updatedTime = new Date(espClockStartMillis + elapsedMillis);  // Update the time
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                String formattedUpdatedTime = sdf.format(updatedTime);  // Format to HH:mm:ss

                // 🟢 Update the TextView every second with the new time
                if (espTimeTextView != null) {
                    espTimeTextView.setText("ESP Time: " + formattedUpdatedTime);
                }

                // 🟡 Re-run the handler to update the time every second
                handler.postDelayed(this, 1000); // Tick every second
            }
        }, 1000); // Start ticking after 1 second
    }    private String getEspTimeFromMessage(String espMessage) {
        // Check if the ESP message length is valid (should be 14 digits as per your format)
        if (espMessage != null && espMessage.length() == 14) {
            // Extract the time part from the message (YYYYMMDDHHMMSS)
            String hour = espMessage.substring(8, 10); // Get HH
            String minute = espMessage.substring(10, 12); // Get MM
            String second = espMessage.substring(12, 14); // Get SS

            // Format and return as HH:mm:ss
            return hour + ":" + minute + ":" + second;
        }
        return "Invalid ESP Time";  // In case of error
    }  */
   private void startEspClockTicker(final long espClockStartMillis) {
       final Handler handler = new Handler();
       handler.postDelayed(new Runnable() {
           @Override
           public void run() {
               // 🟢 Calculate the elapsed time since the ESP start time
               long currentTimeMillis = System.currentTimeMillis();
               long elapsedMillis = currentTimeMillis - espClockStartMillis;  // Correct time calculation

               // 🟡 Format the updated time (hh:mm:ss) based on the elapsed time
               Date updatedTime = new Date(elapsedMillis);  // Update the time based on elapsedMillis
               SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
               String formattedUpdatedTime = sdf.format(updatedTime);  // Format it to hh:mm:ss

               // 🟢 Update the TextView every second with the new time
               if (espTimeTextView != null) {
                   espTimeTextView.setText("ESP Time: " + formattedUpdatedTime);  // Update dynamic ESP time
               }

               // 🟡 Re-run the handler to update the time every second
               handler.postDelayed(this, 1000); // Tick every second
           }
       }, 1000); // Start ticking after 1 second
   }


} // class MainActivitiy
