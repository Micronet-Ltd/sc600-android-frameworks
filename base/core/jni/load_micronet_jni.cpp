/*
 * Copyright (C) 2009 The Android Open Source Project
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

#define LOG_TAG "LoadMicronetJNI"

#include "jni.h"
#include <nativehelper/JNIHelp.h>
#include "android_runtime/AndroidRuntime.h"
#include "com_android_server_lights_Light.h"
#include "com_android_server_serial_canbus.h"
//#include "com_android_server_vinputs_Vinputs.h"
//#include "load_micronet_jni.h"


#include <android/hardware/light/2.0/ILight.h>
#include <android/hardware/light/2.0/types.h>
#include <utils/misc.h>
#include <utils/Log.h>
#include <map>
#include <stdio.h>

using namespace android;


// ---------------------------------------------------------------------------

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = nullptr;
    jint result = -1;

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        ALOGE("ERROR: GetEnv failed\n");
        goto bail;
    }
    assert(env != nullptr);

    if (registerFuncsLight(env) < 0) {
        ALOGE("ERROR: registerFuncsLight native registration failed\n");
        goto bail;
    }
    
    if (registerFuncsCanbus(env) < 0) {
        ALOGE("ERROR: registerFuncsCanbus native registration failed\n");
        goto bail;
    }

//  if (registerFuncsVinputs(env) < 0) {
//      ALOGE("ERROR: registerFuncsVinputs native registration failed\n");
//      goto bail;
//  }
    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

bail:
    return result;
}
