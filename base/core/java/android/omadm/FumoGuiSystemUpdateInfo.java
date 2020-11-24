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
 * See the License for the specific language governing permissions an
 * limitations under the License.
 */

package android.omadm;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class describe Omadm fumo system update information
 * This structure should be in sync with PAL definition
 * pal_fumo_gui_system_update_info
 *
 */
public class FumoGuiSystemUpdateInfo implements Parcelable {
    public String mSoftwareVerion;
    public String mConfigurationVersion;
    public long mLatestSystemUpdateTime;
    public String mStatusMessage;
    public String mHyperLink;

    public FumoGuiSystemUpdateInfo(
            String software_verion, String configuration_version,
            long latest_system_update_time, String status_message,
            String hyper_link) {
        this.mSoftwareVerion = software_verion;
        this.mConfigurationVersion = configuration_version;
        this.mLatestSystemUpdateTime = latest_system_update_time;
        this.mStatusMessage = status_message;
        this.mHyperLink = hyper_link;
    }

    /**
     * Supports Parcelable
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Supports Parcelable
     */
    public static final Parcelable.Creator<FumoGuiSystemUpdateInfo> CREATOR
            = new Parcelable.Creator<FumoGuiSystemUpdateInfo>() {
        @Override
        public FumoGuiSystemUpdateInfo createFromParcel(Parcel in) {
            return new FumoGuiSystemUpdateInfo(in);
        }

        @Override
        public FumoGuiSystemUpdateInfo[] newArray(int size) {
            return new FumoGuiSystemUpdateInfo[size];
        }
    };

    /**
     * Supports Parcelable
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSoftwareVerion);
        dest.writeString(mConfigurationVersion);
        dest.writeLong(mLatestSystemUpdateTime);
        dest.writeString(mStatusMessage);
        dest.writeString(mHyperLink);
    }

    /**
     * Supports Parcelable
     */
    public FumoGuiSystemUpdateInfo(Parcel in) {
        mSoftwareVerion = in.readString();
        mConfigurationVersion = in.readString();
        mLatestSystemUpdateTime = in.readLong();
        mStatusMessage = in.readString();
        mHyperLink = in.readString();
    }
}
