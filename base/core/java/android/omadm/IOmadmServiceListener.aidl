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

package android.omadm;

/** @hide */
oneway interface IOmadmServiceListener
{
    /**
     * Keep up-to-date with vendor/verizon/.../service_listener.h
     */

    /**
     * Retrieve information for OMADM_FUMO_USER_REPLY
     * omadm callback
     */
    void omadmFumoPluginDispachUserReply(
        int state, long defered_update_time, boolean wifi_requred,
        boolean automatic_update_enable, int button_id);

    /**
     * Retrieve information for OMADM_CONTROLLER_USER_REPLY
     * omadm callback
     */
    void omadmControllerDispachUserReply(
        int type, String input_text, int selected_choices_count,
        in String selected_choices, int button_id);

    /**
     * Retrieve information for OMADM_ADMIN_NETWORK_STATUS
     * omadm callback
     */
    void omadmControllerDispachAdminNetStatus(int status, int netId);

    /**
     * Retrieve information for OMADM_NET_MANAGER_REPLY
     * omadm callback
     */
    void omadmNetManagerReply(int net_feature, boolean enabled);

    /**
     * Retrieve information for OMADM_SCOMO_GET_BATTERY_STATUS
     * omadm callback
     */
    void omadmScomoPluginDispachBatteryState(
        int battery_status, int battery_level);

    /**
     * Retrieve information for OMADM_FUMO_GET_BATTERY_STATUS
     * omadm callback
     */
    void omadmFumoPluginDispachBatteryState(
        int battery_status, int battery_level);

    /**
     * Retrieve information for OMADM_FUMO_CHECK_FOR_UPDATE
     * omadm callback
     */
    void omadmFumoPluginDispachCheckForUpdate();
}

