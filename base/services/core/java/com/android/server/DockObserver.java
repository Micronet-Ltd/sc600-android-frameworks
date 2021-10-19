/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;

import com.android.internal.util.DumpUtils;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;

/**
 * DockObserver monitors for a docking station.
 */
final class DockObserver extends SystemService {
    private static final String TAG = "DockObserver";

    private static final String DOCK_UEVENT_MATCH = "DEVPATH=/devices/virtual/switch_dock/dock";
    private static final String DOCK_STATE_PATH = "/sys/class/switch_dock/dock/state";

    private static final int MSG_DOCK_STATE_CHANGED = 0;
    private static final int IN_CRADLE = 1;
	private static final int IGNITION_ON = 2;
	private static final int SMART_CRADLE = 4;
	private static final int OBC_CRADLE = 8;

    private final PowerManager mPowerManager;
    private final PowerManager.WakeLock mWakeLock;

    private final Object mLock = new Object();

    private boolean mSystemReady;

    private int mActualDockState = Intent.EXTRA_DOCK_STATE_UNDOCKED;

    private int mReportedDockState = Intent.EXTRA_DOCK_STATE_UNDOCKED;
    private int mPreviousDockState = Intent.EXTRA_DOCK_STATE_UNDOCKED;

    private boolean mUpdatesStopped;

    private final boolean mAllowTheaterModeWakeFromDock;
    
    private static final String SHUTDOWN_ACTION_REQ = "com.android.server.SHUTDOWN_ACTION_REQ";
    private int mShutdownTimeout = -1;
    private static final int MIN_SHUTDOWN_TIMEOUT = 15000;

    private SettingsObserver mSettingsObserver;
    private BroadcastReceiver mShutdownTimeoutListener = null;
    private McuFileObserver mMcuFileObserver;
 

    public DockObserver(Context context) {
        super(context);

        mPowerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mAllowTheaterModeWakeFromDock = context.getResources().getBoolean(
                com.android.internal.R.bool.config_allowTheaterModeWakeFromDock);

        init();  // set initial status

        mObserver.startObserving(DOCK_UEVENT_MATCH);
    }

    @Override
    public void onStart() {
        publishBinderService(TAG, new BinderService());
        if(Build.MODEL.contains("MSCAM")){
            mMcuFileObserver = new McuFileObserver();
            mMcuFileObserver.startWatching();        
        } 
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_ACTIVITY_MANAGER_READY) {
            synchronized (mLock) {
                mSystemReady = true;

                if(Build.MODEL.contains("MSCAM")){
                    readAndUpdateMcuFpgaVersions();
                } else {
                    SystemProperties.set("hw.build.version.mcu", "unknown");
                    SystemProperties.set("hw.build.version.fpga", "unknown");
                }
                // don't bother broadcasting undocked here
                if (mReportedDockState != Intent.EXTRA_DOCK_STATE_UNDOCKED) {
                    updateLocked();
                }
                startContentObserver();
                registerShutdownTimeout();
                startShutdownTimeout(0 == (IGNITION_ON & mReportedDockState));//1st time on SystemReady
            }
        }
    }

    private void init() {
        synchronized (mLock) {
            try {
                char[] buffer = new char[1024];
                FileReader file = new FileReader(DOCK_STATE_PATH);
                try {
                    int len = file.read(buffer, 0, 1024);
                    setActualDockStateLocked(Integer.parseInt((new String(buffer, 0, len)).trim()));
                    mPreviousDockState = mActualDockState;
                } finally {
                    file.close();
                }
            } catch (FileNotFoundException e) {
                Slog.w(TAG, "This kernel does not have dock station support");
            } catch (Exception e) {
                Slog.e(TAG, "" , e);
            }
        }
    }

    private void setActualDockStateLocked(int newState) {
        mActualDockState = newState;
        if (!mUpdatesStopped) {
            setDockStateLocked(newState);
        }
    }

    private void setDockStateLocked(int newState) {
        if (newState != mReportedDockState) {
            mReportedDockState = newState;
            if (mSystemReady) {
                // Wake up immediately when docked or undocked except in theater mode.
                if (mAllowTheaterModeWakeFromDock
                        || Settings.Global.getInt(getContext().getContentResolver(),
                            Settings.Global.THEATER_MODE_ON, 0) == 0) {
                    mPowerManager.wakeUp(SystemClock.uptimeMillis(),
                            "android.server:DOCK");
                }
                updateLocked();
            }
        }
    }

    private void updateLocked() {
        mWakeLock.acquire();
        mHandler.sendEmptyMessage(MSG_DOCK_STATE_CHANGED);
    }

    private void handleDockStateChange() {
        synchronized (mLock) {
            Slog.i(TAG, "Dock state changed from " + mPreviousDockState + " to "
                    + mReportedDockState);
            final int previousDockState = mPreviousDockState;
            mPreviousDockState = mReportedDockState;

            // Skip the dock intent if not yet provisioned.
            final ContentResolver cr = getContext().getContentResolver();
            if (Settings.Global.getInt(cr,
                    Settings.Global.DEVICE_PROVISIONED, 0) == 0) {
                Slog.i(TAG, "Device not provisioned, skipping dock broadcast");
                return;
            }

            // Pack up the values and broadcast them to everyone
            Intent intent = new Intent(Intent.ACTION_DOCK_EVENT);
            intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            intent.putExtra(Intent.EXTRA_DOCK_STATE, convertDockState(mReportedDockState));
            intent.putExtra("DockValue", mReportedDockState);
            if (Build.MODEL.contains("MSCAM")){
                readAndUpdateMcuFpgaVersions();
            }

            boolean dockSoundsEnabled = Settings.Global.getInt(cr,
                    Settings.Global.DOCK_SOUNDS_ENABLED, 1) == 1;
            boolean dockSoundsEnabledWhenAccessibility = Settings.Global.getInt(cr,
                    Settings.Global.DOCK_SOUNDS_ENABLED_WHEN_ACCESSIBILITY, 1) == 1;
            boolean accessibilityEnabled = Settings.Secure.getInt(cr,
                    Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1;

            // Play a sound to provide feedback to confirm dock connection.
            // Particularly useful for flaky contact pins...
            if ((dockSoundsEnabled) ||
                   (accessibilityEnabled && dockSoundsEnabledWhenAccessibility)) {
                String whichSound = null;
                if (mReportedDockState == Intent.EXTRA_DOCK_STATE_UNDOCKED) {
                    if ((previousDockState == Intent.EXTRA_DOCK_STATE_DESK) ||
                        (previousDockState == Intent.EXTRA_DOCK_STATE_LE_DESK) ||
                        (previousDockState == Intent.EXTRA_DOCK_STATE_HE_DESK)) {
                        whichSound = Settings.Global.DESK_UNDOCK_SOUND;
                    } else if (previousDockState == Intent.EXTRA_DOCK_STATE_CAR) {
                        whichSound = Settings.Global.CAR_UNDOCK_SOUND;
                    }
                } else {
                    if ((mReportedDockState == Intent.EXTRA_DOCK_STATE_DESK) ||
                        (mReportedDockState == Intent.EXTRA_DOCK_STATE_LE_DESK) ||
                        (mReportedDockState == Intent.EXTRA_DOCK_STATE_HE_DESK)) {
                        whichSound = Settings.Global.DESK_DOCK_SOUND;
                    } else if (mReportedDockState == Intent.EXTRA_DOCK_STATE_CAR) {
                        whichSound = Settings.Global.CAR_DOCK_SOUND;
                    }
                }

                if (whichSound != null) {
                    final String soundPath = Settings.Global.getString(cr, whichSound);
                    if (soundPath != null) {
                        final Uri soundUri = Uri.parse("file://" + soundPath);
                        if (soundUri != null) {
                            final Ringtone sfx = RingtoneManager.getRingtone(
                                    getContext(), soundUri);
                            if (sfx != null) {
                                sfx.setStreamType(AudioManager.STREAM_SYSTEM);
                                sfx.play();
                            }
                        }
                    }
                }
            }

            // Send the dock event intent.
            // There are many components in the system watching for this so as to
            // adjust audio routing, screen orientation, etc.
            getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    private final Handler mHandler = new Handler(true /*async*/) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DOCK_STATE_CHANGED:
                    handleDockStateChange();
                    mWakeLock.release();
                    break;
            }
        }
    };
    
    /* 
	 *  Convert the Docking state received from kernel
	 *  to the expected Intent.EXTRA_DOCK_STATE_? values
	 *  received from kernel:
	 *  IN_CRADLE | IGNITION_ON
	 *	send in Intent:
	 *  Intent.EXTRA_DOCK_STATE_UNDOCKED, Intent.EXTRA_DOCK_STATE_DESK
	 *  	or Intent.EXTRA_DOCK_STATE_CAR
	 */

    private final int convertDockState(int receivedDockStat) {
		if ((receivedDockStat & IGNITION_ON) == IGNITION_ON) {
			// IGNITION_ON can be detected only if in cradle
			// so no need to check if IN_CRADLE
			return Intent.EXTRA_DOCK_STATE_CAR;
		} else if ((receivedDockStat & OBC_CRADLE) == OBC_CRADLE) {
			return Intent.EXTRA_DOCK_STATE_DESK;
		} else if ((receivedDockStat & SMART_CRADLE) == SMART_CRADLE) {
			return Intent.EXTRA_DOCK_STATE_HE_DESK;
		} else if ((receivedDockStat & IN_CRADLE) == IN_CRADLE) {
			return Intent.EXTRA_DOCK_STATE_LE_DESK;
		} else {
			return Intent.EXTRA_DOCK_STATE_UNDOCKED;
		}
	}

    private final UEventObserver mObserver = new UEventObserver() {
        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Slog.v(TAG, "Dock UEVENT: " + event.toString());
            }

            int newState = Integer.parseInt(event.get("SWITCH_STATE"));
            try {
                synchronized (mLock) {
                    setActualDockStateLocked(newState);
                    startShutdownTimeout(0 == (IGNITION_ON & newState));
                }
            } catch (NumberFormatException e) {
                Slog.e(TAG, "Could not parse switch state from event " + event);
            }
        }
    };

    private final class BinderService extends Binder {
        @Override
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (!DumpUtils.checkDumpPermission(getContext(), TAG, pw)) return;
            final long ident = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    if (args == null || args.length == 0 || "-a".equals(args[0])) {
                        pw.println("Current Dock Observer Service state:");
                        if (mUpdatesStopped) {
                            pw.println("  (UPDATES STOPPED -- use 'reset' to restart)");
                        }
                        pw.println("  reported state: " + mReportedDockState);
                        pw.println("  previous state: " + mPreviousDockState);
                        pw.println("  actual state: " + mActualDockState);
                    } else if (args.length == 3 && "set".equals(args[0])) {
                        String key = args[1];
                        String value = args[2];
                        try {
                            if ("state".equals(key)) {
                                mUpdatesStopped = true;
                                setDockStateLocked(Integer.parseInt(value));
                            } else {
                                pw.println("Unknown set option: " + key);
                            }
                        } catch (NumberFormatException ex) {
                            pw.println("Bad value: " + value);
                        }
                    } else if (args.length == 1 && "reset".equals(args[0])) {
                        mUpdatesStopped = false;
                        setDockStateLocked(mActualDockState);
                    } else {
                        pw.println("Dump current dock state, or:");
                        pw.println("  set state <value>");
                        pw.println("  reset");
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }
    
    private void setAlarmShutdown (long duration) {
        AlarmManager am = (AlarmManager)getContext().getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(SHUTDOWN_ACTION_REQ);

        PendingIntent pi = PendingIntent.getBroadcast(getContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        Slog.d(TAG, "set alarm after " + duration);
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + duration, pi);
    }
    private void cancelAlarmShutdown() {
        AlarmManager am = (AlarmManager)getContext().getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(SHUTDOWN_ACTION_REQ);

        PendingIntent pi = PendingIntent.getBroadcast(getContext(), 0, i, PendingIntent.FLAG_NO_CREATE);
        if(null != pi)
        	am.cancel(pi);
        Slog.d(TAG, "shutdown canceled");
    }
    public void registerShutdownTimeout() {
    	mShutdownTimeout = getShutdownTimeoutDuration();
        if (mShutdownTimeoutListener == null) {
        	mShutdownTimeoutListener = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Slog.d(TAG, "ShutdownTimeout expired");
                    if(0 == (IGNITION_ON & mReportedDockState))//maybe changed
                    	powerOff();
                }
            };
            IntentFilter intentFilter = new IntentFilter(SHUTDOWN_ACTION_REQ);
            getContext().registerReceiver(mShutdownTimeoutListener, intentFilter);
        }
    }
    private void powerOff() {
        SystemProperties.set("sys.powerctl", "shutdown");
    }
    private void startShutdownTimeout(boolean start) {
    	Slog.d(TAG, "startShutdownTimeout: start " + start + " mShutdownTimeout " + mShutdownTimeout);
    	
        if(start) {
        	int dur = mShutdownTimeout;
        	if(-1 != dur)
        		setAlarmShutdown(dur);
        	else
            	cancelAlarmShutdown();
        }
        else {		            	
        	cancelAlarmShutdown();
        }    	
    }
    private int getShutdownTimeoutDuration() {
        ContentResolver resolver = getContext().getContentResolver();

        int ShutdownTimeout = Settings.System.getInt(resolver, Settings.System.SHUTDOWN_TIMEOUT, -1);

        if(0 < ShutdownTimeout && Integer.MAX_VALUE > ShutdownTimeout) {
        	ShutdownTimeout = (MIN_SHUTDOWN_TIMEOUT > ShutdownTimeout) ? MIN_SHUTDOWN_TIMEOUT : ShutdownTimeout;
        	return ShutdownTimeout;
        }
        return -1; 	
    }

    private void startContentObserver() {
        mSettingsObserver = new SettingsObserver(new Handler());

        final ContentResolver resolver = getContext().getContentResolver();
        
        resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SHUTDOWN_TIMEOUT),
                false, mSettingsObserver, UserHandle.USER_ALL);

    }

    private void handleSettingsChangedLocked() {
        int ShutdownTimeout = getShutdownTimeoutDuration();
        Slog.d(TAG, "updated shutdown_timeout " + ShutdownTimeout);
        
        if(ShutdownTimeout != mShutdownTimeout) {
        	mShutdownTimeout = ShutdownTimeout;
           	startShutdownTimeout(0 == (IGNITION_ON & mActualDockState));
        }
    }
    
    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            synchronized (mLock) {
                handleSettingsChangedLocked();
            }
        }
    }
    
    private void readAndUpdateMcuFpgaVersions(){
        SystemProperties.set("hw.build.version.mcu", getVersion("/proc/mcu_version"));
        SystemProperties.set("hw.build.version.fpga", getVersion("/proc/fpga_version"));
    }
    
    private String getVersion(String path){
        String version = "unknown";
        String line = null;
        try {
            FileReader fileReader = new FileReader(path);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            if ((line = bufferedReader.readLine()) != null) {
                version = line;
            }
            bufferedReader.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return version;
    }
        
    private class McuFileObserver extends FileObserver {
        public McuFileObserver() {
            super("/proc", FileObserver.CLOSE_WRITE);
        }

        @Override
        public void onEvent(int event, String path) {
            if(path.equals("mcu_version")){
                readAndUpdateMcuFpgaVersions();
            }
        }
    }
}
