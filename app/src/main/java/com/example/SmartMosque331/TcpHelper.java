package com.example.SmartMosque331;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.zip.CRC32;

public class TcpHelper {

    private static int messageId = 1;
    private static int messageCounter = 0; // Message ID as an integer (00-99)





    // Send message to be used in other classes
    public static void sendMessage(String message) {
        TcpMainActivity tcpMainActivity = TcpMainActivity.getInstance();
        if (tcpMainActivity != null) {
            tcpMainActivity.sendMessageExternally(message);
        } else {
            Log.e("TcpHelper", "TCP connection is not established.");
        }
    }

    // Handle incoming messages from ESP32
    /*public static String receiveMessages() {
        TcpMainActivity tcpMainActivity = TcpMainActivity.getInstance();
        String message = tcpMainActivity.listenForServerMessagesExternally(); // Route message to processFeedback()

        if (tcpMainActivity != null) {
            return message;
        } else {
            Log.e("TcpHelper", "TCP connection is not established.");
        } return null;
    } */

    public static byte[] composeMessage(String messageBody, int acID, byte messageType) {
        // Start building the message
        StringBuilder message = new StringBuilder();

        // 1. "MOSQ" Identifier (4 bytes)
        message.append("MOSQ");

        // 2. Protocol Version (1 byte) - Always '1'
        message.append('1');

        // 3. Real-Time Clock (YYYYMMDDHHMMSS) - 14 characters
        String currentTime = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        message.append(currentTime);

        // 4. Message Type (1 byte)
        message.append((char) (messageType + '0')); // Convert to char digit

        // 5. Message ID (2 characters: 00-99 then loops back)
        message.append(String.format("%02d", messageCounter)); // Convert int to 2-char string
        messageCounter = (messageCounter + 1) % 100; // Increment and loop after 99

        // 6. Destination ID (3 characters: Broadcast 000, or ID from 001+)
        String formattedACID = String.format("%03d", acID);
        message.append(formattedACID);

        // 7. Message Length (5 characters: every character adds 00001)
        String formattedLength = String.format("%05d", messageBody.length());
        message.append(formattedLength);

        // 8. Message Body (Actual Command or Data)
        message.append(messageBody);

        // Convert message to byte array
        return message.toString().getBytes(StandardCharsets.US_ASCII);
    }


    // listenForServerMessages method end
/* Last working method
//send message to be used in other classes
    public static void sendMessage(String message) {
        TcpMainActivity tcpMainActivity = TcpMainActivity.getInstance();
        if (tcpMainActivity != null) {
            tcpMainActivity.sendMessageExternally(message);
        } else {
            Log.e("TcpHelper", "TCP connection is not established.");
        }
    }

    */

    /*
    // Method to create a structured message
    public static byte[] composeMessage(String messageBody, int acID, byte messageType) {
        // Start by creating a byte array to hold the message
        StringBuilder message = new StringBuilder();

        // 1. "MOSQ" Identifier
        message.append("MOSQ");

        // 2. Protocol Version (1 byte)
        //message.append((char) 1);

        message.append((byte) 1);  // Directly appending byte 1 for version


        // 3. Real-Time (HH:MM:SS) (Format the time)
        String currentTime = new java.text.SimpleDateFormat("HH:mm:ss").format(new Date());
        message.append(currentTime);

        // 4. Message Type (1 byte) - Passed as parameter
        message.append((char) messageType);  // Now messageType is dynamic

        // 5. Message ID (1 byte) - Counter that starts from 1 and resets at 255
        message.append((char) messageId);

        // 6. Destination (AC ID) - Use acTag (this is unique for each AC)
        message.append(acID);

        // 7. Message Length (2 bytes) - Length of the message body
        int bodyLength = messageBody.length();
        message.append((char) (bodyLength & 0xFF));  // Low byte
        message.append((char) ((bodyLength >> 8) & 0xFF));  // High byte

        // 8. Message Body - Your control message (Temperature, Power, etc.)
        message.append(messageBody);


        // Increment message ID and reset if it exceeds 255
        messageId = (messageId % 99) + 1;

        // Convert the full message to byte array and return it
        return message.toString().getBytes(StandardCharsets.US_ASCII);
    }

    */


    /* Last method we have working ON
    public static byte[] composeMessage(String messageBody, int acID, byte messageType) {
        // Start building the message
        StringBuilder message = new StringBuilder();

        // 1. "MOSQ" Identifier (4 bytes)
        message.append("MOSQ");

        // 2. Protocol Version (1 byte) - Ensure it's a character
        message.append('1');  // Fixed to match ESP32 format

        // 3. Real-Time Clock (HH:MM:SS)
        String currentTime = new java.text.SimpleDateFormat("HH:mm:ss").format(new Date());
        message.append(currentTime);

        // 4. Message Type (1 byte)
        message.append((char) (messageType + '0'));  // Convert to char digit

        // 5. Destination ID (1 byte) - AC ID
        message.append((char) (acID + '0'));  // Convert to char digit

        // 6. Message Length (3-digit string)
        String formattedLength = String.format("%03d", messageBody.length());
        message.append(formattedLength);

        // 7. Message Body (Actual ON/OFF Command)
        message.append(messageBody);

        // Convert message to byte array
        return message.toString().getBytes(StandardCharsets.US_ASCII);
    }

    */



    // Method to calculate CRC32 checksum
    private static long calculateCRC(String data) {
        CRC32 crc = new CRC32();
        crc.update(data.getBytes(StandardCharsets.US_ASCII));
        return crc.getValue();
    }


}