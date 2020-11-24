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

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import android.omadm.OmadmStateListener;
/**
 * This class gives you control of the omadm activities.
 *
 */
public final class OmadmManager {
    private static final String TAG = "OmadmManager";

    // This string delimiter should be in sync with definition on PAL side
    public static final String OMADM_DELIMITER = "<omadm>";

    final IOmadmManager mService;

    // Broadcast action for process omadm controller ui.
    public static final String ACTION_OMADM_CONTROLLER_SHOW_UI = "com.verizon.dmclientupdate.Controller.SHOW_UI";
    public static final String OMADM_CONTROLLER_MESSAGE_DESCRIPTOR = "omadm_controller_message_descriptor";

    // Broadcast action for process omadm fumo ui.
    public static final String ACTION_OMADM_FUMO_SHOW_UI = "com.verizon.dmclientupdate.Fumo.SHOW_UI";
    public static final String FUMO_MESSAGE_DESCRIPTOR = "fumo_message_descriptor";
    public static final String ACTION_OMADM_FUMO_SHOW_PROGRESS = "com.verizon.dmclientupdate.Fumo.SHOW_PROGRESS";
    public static final String FUMO_PROGRESS_PERCENT = "fumo_progress_percent";
    public static final String ACTION_OMADM_FUMO_REMOVE_UI = "com.verizon.dmclientupdate.Fumo.REMOVE_UI";
    public static final String FUMO_REMOVE_UI_STATE = "fumo_remove_ui_state";

    /**
     * {@hide}
     */
    public OmadmManager(IOmadmManager service) {
        mService = service;
    }

    /**
     * Registers the listener object to receive notification of changes
     * omadm activities.
     *
     * @param listener that is to be registered.
     */
    public void addOmadmChangedListener(OmadmStateListener listener) {
        try {
            mService.addOmadmChangedListener(listener.callback);
        } catch (RemoteException ex) {
            // system process dead
        } catch (NullPointerException ex) {
            // system process dead
        }
    }

    /**
     * Unregister the listener object to receive notification of changes
     * omadm activities.
     *
     * @param listener that is to be unregistered.
     */
    public void removeOmadmChangedListener(OmadmStateListener listener) {
        try {
            mService.removeOmadmChangedListener(listener.callback);
        } catch (RemoteException ex) {
            // system process dead
        } catch (NullPointerException ex) {
            // system process dead
        }
    }

    /**
     * Notify omadm client that the listener object to receive notification of changes
     * omadm activities.
     *
     * @param listener that is to be unregistered.
     */
    public void omadmCheckForUpdate() {
        try {
            mService.handleCheckForUpdate(true);
        } catch (RemoteException ex) {
            // system process dead
        } catch (NullPointerException ex) {
            // system process dead
        }
    }

    /**
     * Notify omadm fumo plugin that we have user response.
     * @param state fumo plugun state for which we have user response.
     * @param defered_update_time defered time for start
     * update process.
     * @param wifi_requred flag which determine that update
     * process should be start only if wifi enabled and connected.
     * @param automatic_update_enable flag which determine that update
     * process should be start whithout any user reactio.
     * @param button_id gui button identificator
     * which was pressed by user.
     */
    public void omadmFumoPluginDispachUserReply(
        int state, long defered_update_time, boolean wifi_requred,
        boolean automatic_update_enable, int button_id) {
        try {
            mService.omadmFumoPluginDispachUserReply(
                state, defered_update_time, wifi_requred,
                automatic_update_enable, button_id);
        } catch (RemoteException ex) {
            // system process dead
        } catch (NullPointerException ex) {
            // system process dead
        }
    }

    /**
     * Notify omadm controller that we have user response.
     * @param type type of omadm controller UI.
     * @param input_text text which was entered.
     * @param selected_choices choices which was done by user.
     * @param button_id button which was pressed.
     */
    public void omadmControllerDispachUserReply(
        int type, String input_text, int selected_choices_count,
        String selected_choices, int button_id) {
        try {
            mService.omadmControllerDispachUserReply(
                type, input_text, selected_choices_count, selected_choices, button_id);
        } catch (RemoteException ex) {
            // system process dead
        } catch (NullPointerException ex) {
            // system process dead
        }
    }

    /**
     * Opens protected file.
     * Service is used here as ContentProvider.
     * @param path path to the protected file
     * @return ParcelFileDescriptor to read content
     */
    public ParcelFileDescriptor inputStream(String path) {
        ParcelFileDescriptor pfd = null;
        try {
            pfd = mService.inputStream(path);
        } catch (Exception e) {
            // TODO: handle exception
        }
        return pfd;
    }

}
