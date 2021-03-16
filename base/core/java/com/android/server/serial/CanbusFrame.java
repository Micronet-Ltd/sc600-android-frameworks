package com.android.server.serial;

/**
 * Canbus frame encapsulator.
 * Includes byte array that representes the payload data within Canbus packet, 
 * id that holds both Standard &amp; Extended identifiers and packet type indicator (Standard/Extended).
 */
public class CanbusFrame{
	private int mId;
	private byte[] mData;
	private int mType = 0;
	
	/**
     * Constructs data frame with id,  data buffer and frame type: 0 - standart, 1 - extended, 2 - remote-standart, 3 - remote-extended.
     */
	public CanbusFrame(int id, byte[] data, int type)
	{
		mType = type;
		mData = data;
		mId = id;
	}
	
	/**
     * Constructs data frame with id and data buffer.
     * Frame type will be set to Standard by default.
     */
	public CanbusFrame(int id, byte[] data)
	{
		mData = data;
		mId = id;
	}
	
    /**
     * Returns the frame message data buffer.
     */
	public byte[] getData() {
		return mData;
	}
	
	/**
     * Sets the frame message data buffer.
     */
	public void setData(byte[] data) {
		this.mData = data;
	}
	
	/**
     * Returns the frame message identifier.
     */
	public int getId() {
		return mId;
	}
	
	/**
     * Sets the frame message identifier.
     */
	public void setId(int id) {
		this.mId = id;
	}

	/**
     * Gets the frame message type (Standard/Extended).
     */
	public int getType() {
		return mType;
	}

	/**
     * Sets the frame message type to be either Extended or Standard.
     */
	public void setType(int type) {mType = type;}
}
