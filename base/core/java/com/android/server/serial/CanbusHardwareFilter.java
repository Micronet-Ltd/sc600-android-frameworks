package com.android.server.serial;

/**
 * Sets Canbus hardware module to pass certain packet ids.
 * Filtering is done using both Mask and Frame Id to achieve maximum flexibility.
 */

public class CanbusHardwareFilter {
	private int[] mIds;
	private int[] mMask;
	private int[] filterType={EXTENDED};

	/**
	 * Defines standard filter type.
	 */
	static public int STANDARD=0;
	/**
	 * Defines extended filter type.
	 */
	static public int EXTENDED=1;
	/**
	 * Defines standard remote filter type.
	 */
	static public int STANDARD_REMOTE=2;
	/**
	 * Defines extended remote filter type.
	 */
	static public int EXTENDED_REMOTE=3;


	/**
	 * Creates filter with frame ids, mask and types.
	 *
	 *
	 *
	 * @param ids Register frame ids.
	 * @param mask Filter mask to be used in conjunction with frame ids.
	 * @param type Standard / Extended frame.
	 */
	public CanbusHardwareFilter(int[] ids, int[] mask, int[] type){
		mIds = ids;
		mMask = mask;
		filterType = type;
	}

	/**
	 * Creates filter with frame ids only.
	 * @param ids register frame ids to pass.
	 * @param filtType Standard / Extended frame.
	 */
	public CanbusHardwareFilter(int[] ids, int[] filtType){
		mIds = ids;
		filterType = filtType;
	}
	
	/**
	 * Sets the mask for filtering.
	 */
	public void setMask(int[] mask) {
		mMask = mask;
	}
	
	/**
	 * Returns the masks being used for filtering.
	 */
	public int[] getMask() {
		return mMask;
	}

	/**
	 *  Sets the filters ids.
	 */
	public void setIds(int[] ids) {
		mIds = ids;
	}
	
	/**
	 *  Returns the filter ids being used.
	 */
	public int[] getIds() {
		return mIds;
	}

	/**
	 * Returns filter type
	 */
	public int[] getFilterMaskType() {return filterType;}
}
