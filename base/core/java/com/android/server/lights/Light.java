/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.server.lights;

import android.hardware.light.V2_0.Constants.Flash;
import android.hardware.light.V2_0.Constants.Brightness;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.Trace;
import android.provider.Settings;
import android.util.Slog;

public class Light {

    static final String TAG = "Light";
    static final boolean DEBUG = false;

    private int mId;
    private int mColor;
    private int mMode;
    private int mOnMS;
    private int mOffMS;
    private boolean mFlashing;
    private int mBrightnessMode;
    private int mLastBrightnessMode;
    private int mLastColor;
    private boolean mVrModeEnabled;
    private boolean mUseLowPersistenceForVR;
    private boolean mInitialized;
    private final Context mContext;


    public static final int LIGHT_FLASH_NONE = Flash.NONE;
    public static final int LIGHT_FLASH_TIMED = Flash.TIMED;
    public static final int LIGHT_FLASH_HARDWARE = Flash.HARDWARE;

    /**
     * Light brightness is managed by a user setting.
     */
    public static final int BRIGHTNESS_MODE_USER = Brightness.USER;

    /**
     * Light brightness is managed by a light sensor.
     */
    public static final int BRIGHTNESS_MODE_SENSOR = Brightness.SENSOR;

    /**
     * Low-persistence light mode.
     */
    public static final int BRIGHTNESS_MODE_LOW_PERSISTENCE = Brightness.LOW_PERSISTENCE;


    public Light(int id, Context context) {
        mId = id;
        mContext = context;
    }

     public void setIRLed(boolean on) {
     if(on){
        setBrightness(0xFFFFFFFF, BRIGHTNESS_MODE_USER);
     } else {
        setBrightness(0x80808080, BRIGHTNESS_MODE_USER);
        }
    }

    public void setBrightness(int brightness) {
        setBrightness(brightness, BRIGHTNESS_MODE_USER);
    }

    public void setBrightness(int brightness, int brightnessMode) {
        synchronized (this) {
            // LOW_PERSISTENCE cannot be manually set
            if (brightnessMode == BRIGHTNESS_MODE_LOW_PERSISTENCE) {
                Slog.w(TAG, "setBrightness with LOW_PERSISTENCE unexpected #" + mId +
                    ": brightness=0x" + Integer.toHexString(brightness));
                return;
            }
            //int color = brightness & 0x000000ff;
            //color = 0xff000000 | (color << 16) | (color << 8) | color;
            if (DEBUG) {
                Slog.w(TAG, "color: " + brightness + "hex: " + Integer.toHexString(brightness));
            }
            setLightLocked(brightness, LIGHT_FLASH_NONE, 0, 0, brightnessMode);
        }
    }

    public void setColor(int color) {
        synchronized (this) {
            setLightLocked(color, LIGHT_FLASH_NONE, 0, 0, 0);
        }
    }

    public void setFlashing(int color, int mode, int onMS, int offMS) {
        synchronized (this) {
            setLightLocked(color, mode, onMS, offMS, BRIGHTNESS_MODE_USER);
        }
    }

    public void pulse() {
        pulse(0x00ffffff, 7);
    }

    public void pulse(int color, int onMS) {
        synchronized (this) {
            if (mColor == 0 && !mFlashing) {
                setLightLocked(color, LIGHT_FLASH_HARDWARE, onMS, 1000,
                    BRIGHTNESS_MODE_USER);
                mColor = 0;
                mH.sendMessageDelayed(Message.obtain(mH, 1, this), onMS);
            }
        }
    }

    public void turnOff() {
        synchronized (this) {
            setLightLocked(0, LIGHT_FLASH_NONE, 0, 0, 0);
        }
    }

    public void setVrMode(boolean enabled) {
        synchronized (this) {
            if (mVrModeEnabled != enabled) {
                mVrModeEnabled = enabled;

                mUseLowPersistenceForVR =
                    (getVrDisplayMode() == Settings.Secure.VR_DISPLAY_MODE_LOW_PERSISTENCE);
                if (shouldBeInLowPersistenceMode()) {
                    mLastBrightnessMode = mBrightnessMode;
                }
            }
        }
    }

    public void setLightLocked(int color, int mode, int onMS, int offMS, int brightnessMode) {
        if (shouldBeInLowPersistenceMode()) {
            brightnessMode = BRIGHTNESS_MODE_LOW_PERSISTENCE;
        } else if (brightnessMode == BRIGHTNESS_MODE_LOW_PERSISTENCE) {
            brightnessMode = mLastBrightnessMode;
        }

        if (!mInitialized || color != mColor || mode != mMode || onMS != mOnMS ||
            offMS != mOffMS || mBrightnessMode != brightnessMode) {
            if (DEBUG) {
                Slog.v(TAG, "setLight #" + mId + ": color=#"
                    + Integer.toHexString(color) + ": brightnessMode=" + brightnessMode);
            }
            mInitialized = true;
            mLastColor = mColor;
            mColor = color;
            mMode = mode;
            mOnMS = onMS;
            mOffMS = offMS;
            mBrightnessMode = brightnessMode;
            Trace.traceBegin(Trace.TRACE_TAG_POWER, "setLight(" + mId + ", 0x"
                + Integer.toHexString(color) + ")");
            try {
                setLight_native(mId, color, mode, onMS, offMS, brightnessMode);
            } finally {
                Trace.traceEnd(Trace.TRACE_TAG_POWER);
            }
        }
    }

    public boolean shouldBeInLowPersistenceMode() {
        return mVrModeEnabled && mUseLowPersistenceForVR;
    }

    public int getVrDisplayMode() {
        int currentUser = ActivityManager.getCurrentUser();
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
            Settings.Secure.VR_DISPLAY_MODE,
            /*default*/Settings.Secure.VR_DISPLAY_MODE_LOW_PERSISTENCE,
            currentUser);
    }
    
    private Handler mH = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Light light = (Light)msg.obj;
            light.stopFlashing();
        }
    };
    
    private void stopFlashing() {
            synchronized (this) {
                setLightLocked(mColor, LIGHT_FLASH_NONE, 0, 0, BRIGHTNESS_MODE_USER);
            }
        }

    static native void setLight_native(int light, int color, int mode,
        int onMS, int offMS, int brightnessMode);

}
