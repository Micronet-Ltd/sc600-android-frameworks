package com.android.server.net;

import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

public class CanbusService{

    final private static String TAG =  "CanbusService";
    
    /**
    * Select operation mode: "normal" or "listen-only"
    */
    public int mode(String op_mode) {
        return select_op_mode_native(op_mode);
    }
    
    /**
    * Select bus bitrate 
    */
    public int bitrate(int br) {
       return select_bit_rate_native(br);
    }

    /**
    * Frames acceptance masking
    * @param m
    *   uint32_t array of length 2 dedicated to message assembly of
    *   mcp25x rx buffer 0/1, every element is maskasking standard
    *   or extended id. If standard id is choosen for filtering the
    *   mask should be in rage 000-7FF otherwise 00000000-1FFFFFFF
    * @param f 
    *   uint32_t array of length 6, 2 filters mcp25x rx buffer 0 and
    *   4 filters for mcp25x rx buffer 1 (000-7FF for standard or
    *   00000000-1FFFFFFF for extended including flags 0x80000000U
    *   extended, 
    * @return 
    *   0 if succeed and error otherwise 
    */

    public int mask(IntBuffer m, IntBuffer f) {
       return select_mask_native(m, f);
    }
	
    /**
    * Select /net/can0 link 
    */
    public int link(String up) {
       return select_link_native(up);
    }

    /**
    * Close the CAN socket 
    */
    public int close(int fd) {
       return close_native(fd);
    }
    
    /**
    * open the CAN socket 
    */
    public int open(String link) {
       return open_native(link);
    }
    
    /**
    * config CAN socket 
    */
    public int config(String link, IntBuffer ids, IntBuffer flt, int num, int err, int s, int echo_en, int join) {
       return config_native(link, ids, flt, num, err, s, echo_en, join);
    }
    
    /**
    * bind CAN socket to link 
    * returns link index 
    */
    public int bind(String link, int s) {
       return bind_native(link, s);
    }
    
    /**
    * Send the frame to canbus
    * @param s - previously opened socket
    * @param id (unsigned id (000-7FF for standard or 00000000-1FFFFFFF for extended including flags
    *           0x80000000U extended, 0x40000000U remote request,0x20000000U error frame)
    * @param payload length or dlc of remote request
    * @param payload (byte[]) bytes (from 0 to 8 bytes) 
    */

    public int send(int s, int id, int dlc, byte[] payload) {
       return send_native(s, id, dlc, payload);
    }
	
    /**
    * Receive the frame from canbus
    * @param s - previously opened socket 
    * @param idx - returned by bind 
    * @param pointer to store id (unsigned id (000-7FF for 
    *           standard or 00000000-1FFFFFFF for extended including
    *           flags 0x80000000U extended, 0x40000000U remote
    *           request,0x20000000U error frame)
    * @param pointer to store dlc of remote request 
    * @param pointer to store time stamp of frame 
    * @param pointer to store nuber of dropped of frames
    * @param payload (byte[]) bytes (from 0 to 8 bytes) 
    */

    public int recvmsg(int s, int idx, IntBuffer id, IntBuffer dlc, LongBuffer ts, IntBuffer dropped, ByteBuffer payload) {
       return recvmsg_native(s, idx, id, dlc, ts, dropped, payload);
    }

    static native int select_op_mode_native(String op_mode);
    static native int select_bit_rate_native(int br);
    static native int select_mask_native(IntBuffer m, IntBuffer f);
    static native int select_link_native(String up);
    static native int close_native(int socket);
    static native int open_native(String link);
    static native int config_native(String link, IntBuffer ids, IntBuffer flt, int num, int err, int s, int echo_en, int join);
    static native int bind_native(String link, int s);
    static native int send_native(int s, int id, int dlc, byte[] payload);
    static native int recvmsg_native(int s, int idx, IntBuffer id, IntBuffer dlc, LongBuffer ts, IntBuffer dropped, ByteBuffer payload);
}
