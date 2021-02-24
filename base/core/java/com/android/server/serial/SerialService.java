package com.android.server.serial;

import android.util.Log;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;


public class SerialService{
    final private static String TAG =  "SerialService";
    final private String rs485Enable = "/sys/devices/virtual/switch/dock/rs485_en";
    final private String j1708Enable = "/sys/devices/virtual/switch/dock/J1708_en";
    
    public void enableRs485(){
        enablePort(true, rs485Enable);
    }
    
    public void disableRs485(){
        enablePort(false, rs485Enable);
    }
    
    public void enableJ1708(){
        enablePort(true, j1708Enable);
    }
    
    public void disableJ1708(){
        enablePort(false, j1708Enable);
    }
    
    private void enablePort(boolean enable, String filePath){
        String setEnable = "";
        if(enable){
            setEnable = "echo 1 > " + filePath;
        } else {
            setEnable = "echo 0 > " + filePath;
        }    
        runProcess(setEnable); 
    }
    
    private static void runProcess(String cmd) {
		try {
            String[] cmdline = { "sh", "-c", cmd }; 
            Runtime.getRuntime().exec(cmdline);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
}
