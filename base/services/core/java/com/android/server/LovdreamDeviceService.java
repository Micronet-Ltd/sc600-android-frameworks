/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.server;

import android.R.drawable;
import android.content.Context;

import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import com.lovdream.ILovdreamDevice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.IBinder;

import java.lang.reflect.Method;
import java.util.List;

import com.lovdream.LovdreamDeviceManager;

import android.os.storage.StorageVolume;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.util.Log;
import android.os.Binder;

public final class LovdreamDeviceService extends ILovdreamDevice.Stub {

	private static final String TAG = "LovdreamDeviceService";
	private Context mContext;
	public static boolean isDebug = true;

	private static volatile LovdreamDeviceService instance = null;

	public static LovdreamDeviceService getInstance(Context context) {
		synchronized (LovdreamDeviceService.class) {
			if (instance == null) {
				instance = new LovdreamDeviceService(context);
			}
		}
		return instance;
	}

	private LovdreamDeviceService(Context context) {
		mContext = context;
	}
	private Process mTestProcess;

        public void runProcess(String cmds){
            String rc = cmds.replace("*"," ");
            String[] cmd = rc.split(" ");
            for(String s : cmd){
                Log.d("fy",s);
            }
            try {
                mTestProcess = Runtime.getRuntime().exec(cmd);
                mTestProcess.waitFor();
            } catch (Exception e) {}

        }
        @Override
        public String readToFile(String path){
        	if(path.equals("0")){
        		startPlayFm();
        		return null;
        	}else if(path.equals("1")){
        		stopPlayFm();
        		return null;
        	}
			//Binder.clearCallingIdentity();
			File file = new File(path);
			String str = "";
			try {

				FileReader reader = new FileReader(file);
				BufferedReader bReader = new BufferedReader(reader);
				StringBuilder sb = new StringBuilder();
				String s = "";
				while ((s = bReader.readLine()) != null) {
					sb.append(s);
				}
				bReader.close();
				str = sb.toString();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return str.equals("") ? "0" : str;

		}
	@Override
	public void writeToFile(String path, String flag) {
		//Binder.clearCallingIdentity();
		boolean res = true;
		 try {
			File file = new File(path);
			File dir = new File(file.getParent());
			if (!dir.exists())
			dir.mkdirs();
			FileWriter mFileWriter = new FileWriter(file, false);
			mFileWriter.write(flag);
			mFileWriter.close();
			} catch (IOException e2) {
			e2.printStackTrace();
			android.util.Log.d(TAG, "e------>" + (e2));
			res = false;
			}
	}
	
	Light mThreeColorLight;
	Light mButtonBackLight;

	public void setThreeLightColor(int color) {
		try {
			// 闪光灯;
			if (mThreeColorLight == null)
				mThreeColorLight = LocalServices.getService(LightsManager.class)
						.getLight(LightsManager.LIGHT_ID_NOTIFICATIONS);
			// mLight.setFlashing(current, Light.LIGHT_FLASH_TIMED,1000, 3000);
			if(color!=-1)mThreeColorLight.setColor(Integer.valueOf(color));
			else mThreeColorLight.turnOff();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
    public void setButtonBackLight(boolean light){
    	try {
    		if (mButtonBackLight == null)
    			mButtonBackLight = LocalServices.getService(LightsManager.class)
						.getLight(LightsManager.LIGHT_ID_BUTTONS);
    		
    		if(light)mButtonBackLight.setBrightness(255);
    		else mButtonBackLight.turnOff();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

	/*
	 * return G;not M or KB;
	 */

	@Override
	public long getMemorySize(int flag) {
		String sd_path = getExternalSDCardPath();
		File path = null;
		boolean isHasRemoveSd = !(LovdreamDeviceManager.FLAG_NO_SD_CARD
				.equals(sd_path));
		switch (flag) {
		case LovdreamDeviceManager.FLAG_READ_EXTERNAL_ALL:
		case LovdreamDeviceManager.FLAG_READ_EXTERNAL_AVAILABLE:
			if (isHasRemoveSd) {
				path = new File(sd_path);
			}
			break;
		case LovdreamDeviceManager.FLAG_READ_INTERNAL_ALL:
		case LovdreamDeviceManager.FLAG_READ_INTERNAL_AVAILABLE:
			path = Environment.getDataDirectory();
			break;
		default:
			break;
		}
		if (path == null) {
			return 0L;
		}
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSizeLong();
		long availableBlocks = stat.getAvailableBlocksLong();
		long totalBlocks = stat.getBlockCountLong();
		return (flag == LovdreamDeviceManager.FLAG_READ_EXTERNAL_ALL || flag == LovdreamDeviceManager.FLAG_READ_INTERNAL_ALL) ? (totalBlocks * blockSize)
				/ ((1024 * 1024))
				: (availableBlocks * blockSize) / (1024 * 1024);
	}

	/*
	 * add by xxf; get ecternal sd path;(if no sd card return "");
	 */
	private String getExternalSDCardPath() {
		try {
			StorageManager storageManager = (StorageManager) mContext
					.getSystemService(Context.STORAGE_SERVICE);
			List<StorageVolume> storageVolumes = storageManager
					.getStorageVolumes();
			Class<?> volumeClass = Class
					.forName("android.os.storage.StorageVolume");
			Method getPath = volumeClass.getDeclaredMethod("getPath");
			Method isRemovable = volumeClass.getDeclaredMethod("isRemovable");
			getPath.setAccessible(true);
			isRemovable.setAccessible(true);
			for (int i = 0; i < storageVolumes.size(); i++) {
				StorageVolume storageVolume = storageVolumes.get(i);
				String mPath = (String) getPath.invoke(storageVolume);
				Boolean isRemove = (Boolean) isRemovable.invoke(storageVolume);
				if (isRemove) {
					return mPath;
				}
				if (isDebug)
					Log.d(TAG, "mPath is ===> " + mPath
							+ "===>isRemoveble ==> " + isRemove);
			}
		} catch (Exception e) {
			Log.d(TAG, "e ==> " + e.getMessage());
		}
		return LovdreamDeviceManager.FLAG_NO_SD_CARD;
	}
	
	private AudioTrack mTrack;
	private static final int SAMPLE_RATE = 48000;
	private static final int BUFFER_SIZE = 1024;
	private byte[] mBuffer = new byte[BUFFER_SIZE];
	private static final String RECORD_FILE = "/data/vendor/audio/ftm_pcm_record.wav";
	private  boolean running =false;

	public void startPlayFm(){
		try {
			running=true;
			mTrack = new AudioTrack(AudioManager.STREAM_MUSIC,SAMPLE_RATE,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,BUFFER_SIZE,AudioTrack.MODE_STREAM);
			mTrack.setPlaybackRate(SAMPLE_RATE);
			mTrack.play();
			FileInputStream fin = new FileInputStream(RECORD_FILE);
			int readSize = 0;
			while(((readSize = fin.read(mBuffer)) != -1) && running){
				mTrack.write(mBuffer,0,readSize);
			}
			fin.close();
		} catch (Exception e) {
			Log.d(TAG,"e--->"+e);
		}
		
	}

	public void stopPlayFm(){
		running=false;
		if(mTrack!=null){
			mTrack.stop();
			mTrack.release();
			mTrack = null;
		}
	}
	
	@Override
	public void setSystemProp(String prop,String value){
		SystemProperties.set(prop, value);
	}
	@Override
	public String getSystemProp(String prop){
		return SystemProperties.get(prop);
	}
}
