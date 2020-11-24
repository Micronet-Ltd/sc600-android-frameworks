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
 * This class describe Omadm fumo plugin gui message descriptor
 * This structure should be in sync with PAL definition
 * pal_fumo_gui_message_descriptor
 *
 */
public class FumoGuiMessageDescriptor implements Parcelable {
    public static int mState;
    public int mMessageType;
    public int mMessageMode;
    public int mIconType;
    public String mHeaderText;
    public String mMessageText;
    public String mHyperLinkCaption;
    public String mHyperLink;
    public String mProgressBarCaption;
    public String mInstallParam;
    public int mButtonType;
    public long mRequiredSpaceForUpdate;
    public long mRequiredSpaceForDelete;
    public String mSeverity;

    public FumoGuiMessageDescriptor(
            int state, int message_type, int message_mode, int icon_type,
            String header_text, String message_text, String hyper_link_caption,
            String hyper_link, String progress_bar_caption, String install_param, int button_type,
            long required_space_for_update, long required_space_for_delete, String severity) {
        this.mState = state;
        this.mMessageType = message_type;
        this.mMessageMode = message_mode;
        this.mIconType = icon_type;
        this.mHeaderText = header_text;
        this.mMessageText = message_text;
        this.mHyperLinkCaption = hyper_link_caption;
        this.mHyperLink = hyper_link;
        this.mProgressBarCaption = progress_bar_caption;
        this.mInstallParam = install_param;
        this.mButtonType = button_type;
        this.mRequiredSpaceForUpdate = required_space_for_update;
        this.mRequiredSpaceForDelete = required_space_for_delete;
        this.mSeverity = severity;
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
    public static final Parcelable.Creator<FumoGuiMessageDescriptor> CREATOR
            = new Parcelable.Creator<FumoGuiMessageDescriptor>() {
        @Override
        public FumoGuiMessageDescriptor createFromParcel(Parcel in) {
            return new FumoGuiMessageDescriptor(in);
        }

        @Override
        public FumoGuiMessageDescriptor[] newArray(int size) {
            return new FumoGuiMessageDescriptor[size];
        }
    };

    /**
     * Supports Parcelable
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mState);
        dest.writeInt(mMessageType);
        dest.writeInt(mMessageMode);
        dest.writeInt(mIconType);
        dest.writeString(mHeaderText);
        dest.writeString(mMessageText);
        dest.writeString(mHyperLinkCaption);
        dest.writeString(mHyperLink);
        dest.writeString(mProgressBarCaption);
        dest.writeString(mInstallParam);
        dest.writeInt(mButtonType);
        dest.writeLong(mRequiredSpaceForUpdate);
        dest.writeLong(mRequiredSpaceForDelete);
        dest.writeString(mSeverity);
    }

    /**
     * Supports Parcelable
     */
    public FumoGuiMessageDescriptor(Parcel in) {
        mState = in.readInt();
        mMessageType = in.readInt();
        mMessageMode = in.readInt();
        mIconType = in.readInt();
        mHeaderText = in.readString();
        mMessageText = in.readString();
        mHyperLinkCaption = in.readString();
        mHyperLink = in.readString();
        mProgressBarCaption = in.readString();
        mInstallParam = in.readString();
        mButtonType = in.readInt();
        mRequiredSpaceForUpdate = in.readLong();
        mRequiredSpaceForDelete = in.readLong();
        mSeverity = in.readString();
    }
}
