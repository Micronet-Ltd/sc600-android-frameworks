package com.android.server.vinputs;

import android.util.Log;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;


public class InputOutputService{
    final private static String TAG =  "InputOutputService";
    final private String in0 = "/sys/class/misc/vinputs/in0_input";
    final private String in1 = "/sys/class/misc/vinputs/in1_input";
    final private String in2 = "/sys/class/misc/vinputs/in2_input";
    final private String in3 = "/sys/class/misc/vinputs/in3_input";
    final private String in4 = "/sys/class/misc/vinputs/in4_input";
    final private String in5 = "/sys/class/misc/vinputs/in5_input";
    final private String in6 = "/sys/class/misc/vinputs/in6_input";
    final private String in7 = "/sys/class/misc/vinputs/in7_input";    
    final private String inAll = "/sys/class/misc/vinputs/in_all";
    final private String setOutput = "/sys/class/switch_dock/dock/outs_mask_set";
    final private String clearOutput = "/sys/class/switch_dock/dock/outs_mask_clr";
    final private String stateOutput = "/sys/class/switch_dock/dock/outs_mask_state";
    
    final public static int NUM_VINPUT = 8;
    
    public int readInput(int inputNum){
        if(inputNum < 0 || inputNum > 8){
            return -1;
        }
        return Integer.parseInt(readFromFile(mapInput(inputNum)));
    }
    
    public String readAllInputs(){
       return readFromFile(inAll); 
    }
    
    public void setMcuOutputs(int[] listOfOutputs, int value){
        if (value != 0 && value != 1){
            Log.e(TAG, "ERROR: wrong value");
            return;
        }
        if (listOfOutputs.length < 0 || listOfOutputs.length > 4){
             Log.e(TAG, "ERROR: wrong number of outputs. must be from 0 to 4");
            return;
        }
        
        String outputList = getOutputListValue(listOfOutputs);
        if (value == 1){
            //call to outs_mask_set
            String setOut = "echo " + outputList + " > " + setOutput;
            runProcess(setOut); 
        }
        else{
            //call to outs_mask_clr
            String clrOut = "echo " + outputList + " > " + clearOutput;
            runProcess(clrOut); 
        }
    }
    
    public String getMcuOutputsState(){
            return readFromFile(stateOutput);
    }
        
    private String readFromFile(String path) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(path), 256);
            return reader.readLine();
        } catch (Exception e){
            Log.e(TAG,"Cannot read virtual input at "+path);
            return "-1";
        } finally {
            try {
                reader.close();
            } catch (Exception e) {}
        }
    }
    
    private String mapInput(int inputNum){
        switch(inputNum){
            case 0:
                return in0;
            case 1:
                return in1;
            case 2:
                return in2;
            case 3:
                return in3;
            case 4:
                return in4;
            case 5:
                return in5;
            case 6:
                return in6;
            case 7:
                return in7;
            
            }
            return null;
    }
    
    private String getOutputListValue(int[] listOfOutputs){
        int len = listOfOutputs.length;
        int result = 0;
        String res;
        for (int i = 0; i < len; i++){
            result = (1<<listOfOutputs[i]) | result;
        }
        res = mapOutputs(result);
        return res;
    }
    
    private String mapOutputs(int res){
        if(res < 0 || res > 15){
            return "Error";
        }
        switch(res){
            case 0: return "0";
            case 1: return "1";
            case 2: return "2";
            case 3: return "3";
            case 4: return "4";
            case 5: return "5";
            case 6: return "6";
            case 7: return "7";
            case 8: return "8";
            case 9: return "9";
            case 10: return "a";
            case 11: return "b";
            case 12: return "c";
            case 13: return "d";
            case 14: return "e";
            case 15: return "f";
        }
        return "error";
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
