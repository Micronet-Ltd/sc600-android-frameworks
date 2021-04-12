
package com.lovdream;

interface ILovdreamDevice{
	void writeToFile(String path,String flag);
	String readToFile(String path);
	void runProcess(String cmds);
	
	/*
	*add by xxf
	*@param int flag; 
	*                            
	*/
	long getMemorySize(int flag);
	
	void setThreeLightColor(int color);
	
	void setButtonBackLight(boolean light);
	
	
	void startPlayFm();
	void stopPlayFm();
	
	void setSystemProp(String prop,String value);
	String getSystemProp(String prop);
}
