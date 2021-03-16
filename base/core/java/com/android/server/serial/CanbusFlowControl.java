package com.android.server.serial;

/*
* Set auto respond flow control messages.
* Configured when you need the firmware to automatically respond to a message with 1st data byte as <10>
* The number of available flow control codes is 8.
*
*/

public class CanbusFlowControl {

    private int searchId;
    private int responseId;
    private int flowDataLength;
    private byte[] dataBytes=new byte[8];
    private int idType = EXTENDED;

    private int n = 0;
    int length=0;

    static public int STANDARD=0;
    static public int EXTENDED=1;

    /**
     * Sets Canbus Flow Control module to respond to certain IDS with specific response ids and data bytes.
     * Supports Upto 8 Flow codes per can instance
     *
     * @param searchIdCode Search Id of the flow code
     * @param responseIdCode Response ID that will be used respond to a message with a search id.
     * @param type An array of Standard / Extended frames.
     * @param dataLength The number of data byte pairs that will be used to respond
     * @param responseDataBytes The firmware responds with these data bytes with its respective response ID.
     */
    public CanbusFlowControl(int searchIdCode, int responseIdCode, int type, int dataLength, byte[] responseDataBytes) {

        searchId = searchIdCode;
        responseId = responseIdCode;
        idType = type;
        flowDataLength = dataLength;
        length = responseDataBytes.length;
        dataBytes = responseDataBytes;
    }

    /**
     * Returns searchIds
     */
    public int getSearchId() {
        return searchId;
    }

    /**
     * Returns responseIds
     */
    public int getResponseId() {
        return responseId;
    }

    /**
     * Returns dataLength
     */
    public int getFlowDataLength() {
        return flowDataLength;
    }

    /**
     * Returns getIdType
     */
    public int getFlowMessageType() {
        return idType;
    }

    /**
     * Returns an array of data bytes for the first pair of search and response ids
     */
    public byte[] getDataBytes() {
        return dataBytes;
    }

}

