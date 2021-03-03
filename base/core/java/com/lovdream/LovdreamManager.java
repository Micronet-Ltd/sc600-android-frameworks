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
import com.lovdream.ILovdream;
import android.os.ServiceManager;
import android.annotation.SystemService;
import android.os.IBinder;

/**
 * AudioManager provides access to volume and ringer mode control.
 */
@SystemService(Context.LOVDREAM_SERVICE)
public class LovdreamManager {
	
	    private Context mContext;
	   //add by xxf for getMemorySize;
	    
	    public static final String FLAG_NO_SD_CARD ="no_sd_card";
	    
		public static final int FLAG_READ_EXTERNAL_ALL =0;
		public static final int FLAG_READ_EXTERNAL_AVAILABLE =1;
		public static final int FLAG_READ_INTERNAL_ALL =2; 
		public static final int FLAG_READ_INTERNAL_AVAILABLE =3;
		//add by xxf for getMemorySize;

    private static ILovdream sService;
	   private static  LovdreamManager instance=null;
	    public static LovdreamManager getInstance(Context context){
	            synchronized(LovdreamManager.class){
	                if(instance==null){
	                    instance=new LovdreamManager(context);
	                }
	            }
	        return instance;
	    }

    private LovdreamManager(Context context){
	mContext = context;
	}
    
    private  static ILovdream getService()
    {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(Context.LOVDREAM_SERVICE);
        sService = ILovdream.Stub.asInterface(b);
        return sService;
    }
   
}
