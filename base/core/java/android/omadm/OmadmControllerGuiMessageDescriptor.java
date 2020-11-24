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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class describe Omadm controller gui message descriptor
 * This structure should be partially in sync with PAL definition
 * pal_omadmdm_controller_gui_message_descriptor
 *
 */
public class OmadmControllerGuiMessageDescriptor implements Parcelable {

    public int mType;
    public int mMinDisp;
    public int mMaxDisp;
    public int mMaxRespLen;
    public int mInputType;
    public int mEchoType;
    public String mDispMsg;
    public String mDfltResp;
    public String[] mChoices;

    public OmadmControllerGuiMessageDescriptor(
            int type , int min_disp, int max_disp, int max_resp_len,
            int input_type, int echo_type, String disp_msg, String dflt_resp,
            String[] choices) {
        this.mType = type;
        this.mMinDisp = min_disp;
        this.mMaxDisp = max_disp;
        this.mMaxRespLen = max_resp_len;
        this.mInputType = input_type;
        this.mEchoType = echo_type;
        this.mDispMsg = disp_msg;
        this.mDfltResp = dflt_resp;
        this.mChoices = choices;
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
    public static final Parcelable.Creator<OmadmControllerGuiMessageDescriptor> CREATOR
            = new Parcelable.Creator<OmadmControllerGuiMessageDescriptor>() {
        @Override
        public OmadmControllerGuiMessageDescriptor createFromParcel(Parcel in) {
            return new OmadmControllerGuiMessageDescriptor(in);
        }

        @Override
        public OmadmControllerGuiMessageDescriptor[] newArray(int size) {
            return new OmadmControllerGuiMessageDescriptor[size];
        }
    };

    /**
     * Supports Parcelable
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mType);
        dest.writeInt(mMinDisp);
        dest.writeInt(mMaxDisp);
        dest.writeInt(mMaxRespLen);
        dest.writeInt(mInputType);
        dest.writeInt(mEchoType);
        dest.writeString(mDispMsg);
        dest.writeString(mDfltResp);
        dest.writeStringArray(mChoices);
    }

    /**
     * Supports Parcelable
     */
    public OmadmControllerGuiMessageDescriptor(Parcel in) {
        mType = in.readInt();
        mMinDisp = in.readInt();
        mMaxDisp = in.readInt();
        mMaxRespLen = in.readInt();
        mInputType = in.readInt();
        mEchoType = in.readInt();
        mDispMsg = in.readString();
        mDfltResp = in.readString();
        mChoices = in.readStringArray();
    }
}
