/*
 * Copyright (C) 2016 Verizon. All Rights Reserved.
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

/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2016 Verizon
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
 * See the License for the specific language governing permissions an
 * limitations under the License.
 */

package android.omadm;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;


public class OmadmStateListener {

    /**
     *  Listen for remove omadm fumo plugin UI.
     *
     */
    public static final int LISTEN_OMADM_FUMO_PLUGIN_REMOVE_UI = 0x00000001;

    /**
     *  Listen for changes size of download update package omadm fumo plugin.
     *
     */
    public static final int LISTEN_OMADMFUMO_PLUGIN_DOWNLOAD_PROGRESS = 0x00000002;

    /**
     *  Listen for information about latest system update from omadm fumo plugin.
     *
     */
    public static final int LISTEN_OMADMFUMO_PLUGIN_SYSTEM_UPDATE_INFO = 0x00000003;

    /**
     *  Listen for remove omadm controller UI.
     *
     */
    public static final int LISTEN_OMADM_CONTROLLER_REMOVE_UI = 0x00000004;

    private final Handler mHandler;

    public OmadmStateListener() {
        this(Looper.myLooper());
    }

    public OmadmStateListener(Looper looper) {
        mHandler = new Handler(looper) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case LISTEN_OMADM_FUMO_PLUGIN_REMOVE_UI:
                        OmadmStateListener.this.onOmadmFumoPluginRemoveUI((int)msg.obj);
                        break;
                    case LISTEN_OMADMFUMO_PLUGIN_DOWNLOAD_PROGRESS:
                        OmadmStateListener.this.onOmadmFumoPluginDownloadProgressChanged((int)msg.obj);
                        break;
                    case LISTEN_OMADMFUMO_PLUGIN_SYSTEM_UPDATE_INFO:
                        OmadmStateListener.this.onOmadmFumoPluginSystemUpdateInfo((FumoGuiSystemUpdateInfo)msg.obj);
                        break;
                    case LISTEN_OMADM_CONTROLLER_REMOVE_UI:
                        OmadmStateListener.this.onOmadmControllerRemoveUI((int)msg.obj);
                        break;
                }
            }
        };
    }

    /**
     * Callback invoked when omadm fumo plugin remove UI
     * for current state.
     */
    public void onOmadmFumoPluginRemoveUI(int state) {
    }

    /**
     * Callback invoked when omadm fumo plugin changes size of
     * downloaded update package
     */
    public void onOmadmFumoPluginDownloadProgressChanged(int percent) {
        // default implementation empty
    }

    public void onOmadmFumoPluginSystemUpdateInfo(FumoGuiSystemUpdateInfo fupi) {
        // default implementation empty
    }

    /**
     * Callback invoked when omadm controller remove UI
     * for current state.
     */
    public void onOmadmControllerRemoveUI(int type) {
    }

    IOmadmStateListener callback = new IOmadmStateListener.Stub() {

        public void onOmadmFumoPluginRemoveUI(int state) {
            Message.obtain(mHandler, LISTEN_OMADM_FUMO_PLUGIN_REMOVE_UI, 0, 0, state).sendToTarget();
        }

        public void onOmadmFumoPluginDownloadProgressChanged(int percent) {
            Message.obtain(mHandler, LISTEN_OMADMFUMO_PLUGIN_DOWNLOAD_PROGRESS, 0, 0, percent).sendToTarget();
        }

        public void onOmadmFumoPluginSystemUpdateInfo(FumoGuiSystemUpdateInfo fupi) {
            Message.obtain(mHandler, LISTEN_OMADMFUMO_PLUGIN_SYSTEM_UPDATE_INFO, 0, 0, fupi).sendToTarget();
        }

        public void onOmadmControllerRemoveUI(int type) {
            Message.obtain(mHandler, LISTEN_OMADM_CONTROLLER_REMOVE_UI, 0, 0, type).sendToTarget();
        }
    };

}
