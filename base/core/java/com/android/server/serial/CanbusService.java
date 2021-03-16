package com.android.server.serial;

import android.util.Log;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class CanbusService{

    final private static String TAG =  "CanbusService";
    final private String can1 = "/dev/ttyCAN0";
    final private String can2 = "/dev/ttyCAN1";
    String command = "";
    String response = "";
    
    /**
    * Configure canbus and open
    *
    * @param listeningModeEnable - enable or disable listening mode
    * @param bitrate - speed: 10000, 20000, 33330, 50000, 100000, 125000, 250000, 500000, 800000, 1000000
    * @param termination - true enables termination, false - disable termination
    * @param hardwareFilters - Sets Canbus hardware module to pass certain packet ids.
    *        Filtering is done using both Mask and Frame Id to achieve maximum flexibility.
    *        @param mIds - int[] of ids
    *        @param mMask - int[] of masks
    *        @param filterType int[] of types: 1 (for extended) or 0 (for standard)
    * @param portNumber - 1 for ttyCAN0, 2 for ttyCAN1
    * @param flowControl - Sets Canbus Flow Control module to respond to certain IDS with specific response ids and data bytes.
    *        Supports Upto 8 Flow codes per can instance
    *        @param searchIdCode Search Id of the flow code (int)
    *        @param responseIdCode Response ID that will be used respond to a message with a search id. (int)
    *        @param type An array of Standard / Extended frames. (int)
    *        @param dataLength The number of data byte pairs that will be used to respond. (int)
    *        @param responseDataBytes The firmware responds with these data bytes with its respective response ID. (byte[])
    *
    */
    public int configureAndOpenCan(boolean listeningModeEnable, int bitrate, boolean termination, CanbusHardwareFilter[] hardwareFilters, int portNumber, CanbusFlowControl[] flowControl){
        return configureCanAndOpen(listeningModeEnable, bitrate, termination, hardwareFilters, portNumber, flowControl);
    }
    
    /**
    * Close the CAN channel, disable termination and power. 
    */
    public int closeCan(int portNumber){
       return close_native(portNumber);
    }
    
    /**
    * Read Status Flags. 
    */
    public int readStatusFlag(int portNumber){
       return readStatusCommand(portNumber);
    }
    
    /**
    * The get mask/filter/flow control config is used to get the mask config at a specific index ii.
    * @param portNumber - 1 for ttyCAN0, 2 for ttyCAN1
    * @param type 0 for mask, 1 for filter, 2 for flow control
    * @param index – index of the list. 
    * Values: 00-0F in hex for mask (0-15 in decimal), 00-18 (0-24 in decimal) in hex for filter, 00-07 (0-7 in decimal) in hex for auto flow command list.
    */
    public String getConfig (int portNumber, int type, int index){
        if(portNumber > 2 || portNumber < 1){
            Log.d(TAG, "invalid port number");
            return "-1";
        }
        if(type > 2 || type < 0){
            Log.d(TAG, "invalid type parameter");
            return "-1";
        }
        if((type == 0) && (index < 0 || index > 15)){
            Log.d(TAG, "invalid index parameter");
            return "-1";
        }
        if((type == 1) && (index < 0 || index > 24)){
            Log.d(TAG, "invalid index parameter");
            return "-1";
        }
        if((type == 2) && (index < 0 || index > 7)){
            Log.d(TAG, "invalid index parameter");
            return "-1";
        }
        String t = convertType(type);
        String hexIndex = Integer.toHexString(index);
        String fullHexString = "";
        if(hexIndex.length() == 1){
            fullHexString = "0" + hexIndex;
        }else {
            fullHexString = hexIndex;
        }
        String command = "G" + t + fullHexString;

        return runProcess(command);
    }
    
    /**
    * Send the frame to canbus
    * @param frame - frame of type CanbusFrame
    *        @param id (int) - id (values 000-7FF for standard frame, values 00000000-1FFFFFFF for extended frame)
    *        @param data (byte[]) bytes (from 0 to 8 bytes)
    *        @param type (int) 0 - standard  type, 1 - extended, 2 - remote standard, 3 - remote extended.
    * @param portNumber - 1 for ttyCAN0, 2 for ttyCAN1
    */
    public int sendFrame(CanbusFrame frame, int portNumber){
       return sendFrameToCan(frame, portNumber);
    }
	
	private static String runProcess(String cmd) {
        String output = "";
		try {
            Runtime rt = Runtime.getRuntime();
            String[] cmdline = { "sh", "-c", cmd }; 
            Process proc = rt.exec(cmdline);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                output = s;
            }

            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }

		} catch (IOException e) {
			e.printStackTrace();
		}
		return output;
	}
	
	private String convertType(int type){
        if(type == 0){
            return "m";
        } else if(type == 1){
            return "M";
        } 
        else return "f";
	}

	static native int configureCanAndOpen(boolean listeningModeEnable, int bitrate, boolean termination, CanbusHardwareFilter[] hardwareFilters, int portNumber, CanbusFlowControl[] flowControl);
	
	static native int close_native(int portNumber);
	
	static native int sendFrameToCan(CanbusFrame frame, int portNumber);
	
	static native int readStatusCommand(int portNumber);
	    
}
