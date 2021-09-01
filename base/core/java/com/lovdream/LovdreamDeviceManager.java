/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lovdream;


import android.content.Context;

import com.lovdream.ILovdreamDevice;

import android.os.ServiceManager;
import android.annotation.SystemService;
import android.os.IBinder;
import android.util.Log;

/**
 * AudioManager provides access to volume and ringer mode control.
 */
@SystemService(Context.LOVDREAMDEVICES_SERVICE)
public class LovdreamDeviceManager {
	
	    private Context mContext;
	   //add by xxf for getMemorySize;
	    
	    public static final String FLAG_NO_SD_CARD ="no_sd_card";
	    
		public static final int FLAG_READ_EXTERNAL_ALL =0;
		public static final int FLAG_READ_EXTERNAL_AVAILABLE =1;
		public static final int FLAG_READ_INTERNAL_ALL =2; 
		public static final int FLAG_READ_INTERNAL_AVAILABLE =3;
		//add by xxf for getMemorySize;

       private static ILovdreamDevice sService;
	   private static  LovdreamDeviceManager instance=null;
	    public static LovdreamDeviceManager getInstance(Context context){
	            synchronized(LovdreamDeviceManager.class){
	                if(instance==null){
	                    instance=new LovdreamDeviceManager(context);
	                }
	            }
	        return instance;
	    }

    private LovdreamDeviceManager(Context context){
	mContext = context;
	}
    
    private  static ILovdreamDevice getService()
    {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(Context.LOVDREAMDEVICES_SERVICE);
        sService = ILovdreamDevice.Stub.asInterface(b);
        return sService;
    }
    
    public void writeToFile(String path,String flag){
    	ILovdreamDevice mService = getService();
    	try {
    		mService.writeToFile(path,flag);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public void setButtonBackLight(boolean light){
    	ILovdreamDevice mService = getService();
    	try {
    		mService.setButtonBackLight(light);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    
    public  void setThreeLightColor(int color){
    	ILovdreamDevice mService = getService();
    	try {
    		mService.setThreeLightColor(color);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public void runProcess(String cmds){
        ILovdreamDevice mService = getService();
        try {
            mService.runProcess(cmds);
        } catch (Exception e) {}
    }
    
    
    public void startPlayFm(){
        ILovdreamDevice mService = getService();
        try {
            mService.startPlayFm();
        } catch (Exception e) {}
    }
    
    
    public void stopPlayFm(){
        ILovdreamDevice mService = getService();
        try {
            mService.stopPlayFm();
        } catch (Exception e) {}
    }
    
    public long getMemorySize(int flag) {
    	ILovdreamDevice mService = getService();
    	try {
    		return mService.getMemorySize(flag);
		} catch (Exception e) {
			return 0L;
		}
    }
   
}
