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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.omadm;

import android.omadm.IOmadmServiceListener;
import android.omadm.IOmadmStateListener;
import android.omadm.IOmadmEventHandler;
import android.os.ParcelFileDescriptor;

/** @hide */
interface IOmadmManager {
    String getDeviceId();
    boolean sendNotification(String title, String message);

    int addOmadmListener(IOmadmServiceListener listener);
    int removeOmadmListener(IOmadmServiceListener listener);

    /*
     * Interface for event handler listener
     */
    int connectListener(IBinder listener, int api);
    int disconnectListener(int api);

    /*
     * Interface for pal_bluetooth functions
     */
    boolean setBluetoothEnabled();
    boolean setBluetoothDisabled();
    boolean setBluetoothDiscoverableEnabled();
    boolean setBluetoothDiscoverableDisabled();
    int getBluetoothDiscoverableState();
    String getBluetoothName();
    int getBluetoothState();
    int getBluetoothHdpState();
    int getBluetoothA2dpState();
    int getBluetoothHspState();

    /*
     * Interface for pal_nfc functions
     */
    boolean setNfcEnabled();
    boolean setNfcDisabled();
    int getNfcState();

    /*
     * Interface for pal_network functions
     */
    boolean setPreferredNetworkMode(int mode);
    boolean setMobileData(int enable);
    int getNetworkBaseStationId();
    int getNetworkSystemId();
    int getNetworkMcc();
    int getNetworkId();
    String getSupportedNetworkModes();
    int getNetworkPreferredMode();
    int nwCrntVoiceGet();
    int nwCrntDataGet();
    int nwMobileDataStateGet();
    int nwRssiGet(int reqNwType);
    int getNetworkMnc();
    int getCurrentNetworkType();
    int getCallState();
    int getCurrentConnectType();
    String getSimCountryIso();
    int getSimState();
    int nwGlobalDataRoamingGet();
    void nwGlobalDataRoamingSet(int enable);

    /*
     * Interface for pal_network mode functions
     */
    boolean setNetworkGlobalMode();
    boolean setNetworkLteCdmaMode();
    boolean setNetworkGsmUmtsMode();

    /*
     * Interface for pal_network_apn_class* functions
     */
    int nwApnGetId(int apnCls);
    String nwApnGetName(int apnCls);
    int nwApnSetName(int apnCls, String apnName);
    int nwApnGetIpvX(int apnCls);
    int nwApnSetIpvX(int apnCls, int apnIpV);
    int nwApnIsEnabled(int apnCls);
    int nwApnEnable(int apnCls, int enable);

    /*
     * Interface for pal_network_wifi* functions
     */
    int nwGetWifiState();
    int nwGetWifiSpeed();
    int nwGetWifiStatus();
    String nwGetWifiNetworks();
    int nwGetWifiSignal();
    String nwGetWifiSsid();
    int nwGetWifiHotspotState();
    String sysGetWifiMac();
    String nwGetWifiBssid();
    void nwSetWifiEnable();
    void nwSetWifiDisable();
    void nwSetWifiHotspotDisable();

    /*
     * Interface for pal_* functions volumes
     */
    int getRingToneVolume();
    int setRingToneVolume(int volumeLevel);
    int getNotificationVolume();
    int setNotificationVolume(int volumeLevel);
    int getAlarmVolume();
    int setAlarmVolume(int volumeLevel);
    int getMediaVolume();
    int setMediaVolume(int volumeLevel);
    int getBluetoothVolume();
    int setBluetoothVolume(int volumeLevel);

    /*
     * Interface for pal_* security functions
     */

    int getEncryptionState();
    int getVerifyAppState();
    void setVerifyAppEnable();

    /*
     * Interface for pal_network_ims_sms_over_ip_* functions
     */
    int nwImsSmsOverIpEnabled();
    void nwImsSmsOverIpEnable(int enable);

    /*
     * Interface for int pal_network_ims_* functions
     */
    String getImsDomain();
    int getImsSmsFormat();
    void setImsSmsFormat(int value);
    int getImsVlt();
    void setImsVlt(int value);
    int getImsLvcState();
    void setImsLvcState(int enable);
    int getImsVwfState();
    void setImsVwfState(int enable);
    int getImsEab();
    void setImsEab(int enable);

    /*
     * Interface for devDetail functions
     */
    String getDeviceOEM();
    String getFWV();
    String getSWV();
    String getHWV();
    String support_lrgobj_get();
    String getDate();
    String getTime();
    String getHostDeviceManu();
    String getHostDeviceModel();
    String getHostHWV();
    String getHostFWV();
    String getHostSWV();
    String getHostDateStamp();
    String getHostID();
    String getTyp();

    /*
     * Interface for DevInfo helper functions
     */
    String getManufacturer();
    String getModel();
    String getDmv();
    String getLanguage();
    String getIccid();
    String getExt();

    /*
     * Interface for Diagmon Gps functions
     */
     boolean GpsEnable();
     boolean GpsDisable();
     int getGpsStatus();
     int getSatellitesCount();
     float getSatellitesSnr();

    /*
     * Interface for OMADM Fumo plugin notifications
     */
    void notifyOmadmFumoPluginShowUI(
        int state, int message_type, int message_mode, int icon_type,
        String header_text, String message_text, String hyper_link_caption,
        String hyper_link, String progress_bar_caption, String install_param, int button_type,
        long required_space_for_update, long required_space_for_delete, String severity);
    void notifyOmadmFumoPluginRemoveUI(int state);
    void notifyOmadmFumoPluginDownloadProgress(int percent);
    void notifyOmadmFumoPluginSystemUpdateInfo(
        String software_verion, String configuration_version,
        long latest_system_update_time, String status_message,
        String hyper_link);

    /*
     * Interface for OMADM Controller notifications
     */
    void notifyOmadmControllerShowUI(
        int type , int min_disp, int max_disp, int max_resp_len,
        int input_type, int echo_type, String disp_msg, String dflt_resp,
        in String[] choices);
    void notifyOmadmControllerRemoveUI(int type);

    /*
     * Interface for pal_request_network functions
     */
    int requestAdminNetwork(boolean enable);

    /*
     * Interface for pal_system_storage_x_used_* functions
     */
    List<String> getAppsPaths();
    String[] getPictsPaths(int external);
    String[] getVideoPaths(int external);
    String[] getAudioPaths(int external);
    List<String> getCachesPaths(int external);
    String getExternalPath();
    String getDownloadsPath();
    long getDiskUsage(int type);

    /* Add here methods these are not used in the native omadm_service_api */

    /*
     * Interface for OMADM Fumo plugin callback
     */
    void omadmFumoPluginDispachUserReply(
        int state, long defered_update_time, boolean wifi_requred,
        boolean automatic_update_enable, int button_id);

    /*
     * Interface for OMADM Controller callback
     */
    void omadmControllerDispachUserReply(
        int type, String input_text, int selected_choices_count,
        in String selected_choices, int button_id);

    void handleCheckForUpdate(boolean update);
    void addOmadmChangedListener(IOmadmStateListener callback);
    void removeOmadmChangedListener(IOmadmStateListener callback);

    ParcelFileDescriptor inputStream(String path);
    int verifyUpdateImage(String path);
    int installUpdateImage(String path);
}
