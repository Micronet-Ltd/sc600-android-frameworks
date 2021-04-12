
package com.lovdream;

interface ILovdream{

//add by xxf
//wifi;
boolean setWlanPolicies(int mode);
int getWlanPolicies();

//dataNet;
boolean setDataConnectivityPolicies(int mode);
int getDataConnectivityPolicies();

//bt
boolean setBluetoothPolicies(int mode, String bluetoothInfoList);
String getBluetoothPolicies();


//nfc
boolean setNfcPolicies(int mode);
int getNfcPolicies();

//red led
boolean setIrPolicies(int mode);
int getIrPolicies();

//shengwu
boolean setBiometricRecognitionPolicies(int mode);
int getBiometricRecognitionPolicies();

//location
boolean setGpsPolicies(int mode);
int getGpsPolicies();


//usb
boolean setUsbDataPolicies(int mode);
int getUsbDataPolicies();


//sd
boolean setExternalStoragePolicies(int mode);
int getExternalStoragePolicies();

//mic
boolean setMicrophonePolicies(int mode);
int getMicrophonePolicies();

//speaker;
boolean setSpeakerPolicies(int mode);
int getSpeakerPolicies();

//camera
boolean setCameraPolicies(int mode);
int getCameraPolicies();


//flash
boolean setFlashPolicies(int mode);
int getFlashPolicies();


//wai she
boolean setPeripheralPolicies(int mode);
int getPeripheralPolicies();


//add by wh
//device info
boolean allowGetMEIDEnable(boolean enable);

//application running
boolean allowStartOnBootEnable(boolean enable);
boolean allowRunBackgroundEnable(boolean enable);

//phone and mms
boolean allowTelephonePolicies(String packageName,boolean enable);
boolean allowReadCallRecord(boolean enable);
boolean allowModifyCallRecord(boolean enable);
boolean allowRemoveCallRecord(boolean enable);
boolean allowSmsPolicies(boolean enable);
boolean allowReadSms(boolean enable);
boolean allowMmsPolocies(boolean enable);
boolean allowReadMms(boolean enable);
boolean allowReadContacts(boolean enable);
boolean allowWriteContacts(boolean enable);

//net and location
boolean allowDataOpen(boolean enable);
boolean allowDataConnectivityPolicies(boolean enable);
boolean allowWifiOpen(boolean enable);
boolean allowWifiConnectivity(boolean enable);
boolean allowBluttoothOpen(boolean enable);
boolean allowNfcOpen(boolean enable);
boolean allowGpsConnectivity(boolean enable);

//Multi-Media 
boolean allowSoundrecorder(boolean enable);
boolean allowRecordingscreen(boolean enable);
boolean allowCamera(boolean enable);
boolean allowVideoCamera(boolean enable);










//add by xxf
int exist();
int writeDevice (String b, int len);
int openBeidou(String device);
int closeBeidou();
//add by xxf














}
