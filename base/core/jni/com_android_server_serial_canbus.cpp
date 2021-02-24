#include <unistd.h>
#include <sys/prctl.h>
#include <pthread.h>
#include <stdlib.h>
#include <stdio.h>
#include <jni.h>
#include <stdint.h>
#include <errno.h>
#include <termios.h>
#include <string.h>
#include <sys/poll.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <linux/netlink.h>
#include <android/log.h>
#include <utils/Log.h>
#include "android_runtime/AndroidRuntime.h"
#include <nativehelper/JNIHelp.h>
#include <utils/misc.h>

#include "com_android_server_serial_canbus.h"

#define MAX_PACKET_SIZE 256

#define CAN1_TTY    "/dev/ttyCAN0" //"/dev/ttyACM2"
#define CAN2_TTY    "/dev/ttyCAN1" //"/dev/ttyACM3"
#define CAN_OK_RESPONSE 	0x0D
#define CAN_ERROR_RESPONSE	0x07
#define DWORD uint32_t
#define WORD uint16_t
#define BYTE uint8_t
#define BOOL int
#define SYSTEM_ERROR -1
#define STANDARD 0
#define EXTENDED 1
#define STANDARD_REMOTE 2
#define EXTENDED_REMOTE 3
#define MAX_FLEXCAN_CAN_FILTERS 24
#define MAX_FlexCAN_Flowcontrol_CAN 8
#define MAX_MASK_FILTER_SIZE 11

static int CAN1_TTY_NUMBER= 1;
static int CAN2_TTY_NUMBER= 2;

static int fd_CAN1 = -1;
static int fd_CAN2 = -1;

struct FLEXCAN_Flow_Control{
    __u32 search_id;
    __u32 response_id;
    __u8 flow_msg_type;
    __u8 flow_msg_data_length;
    BYTE response_data_bytes[MAX_FlexCAN_Flowcontrol_CAN];
};

struct FLEXCAN_filter_mask {
    __u32 mask_id[MAX_FLEXCAN_CAN_FILTERS];
    __u32 mask_count;

    __u8 filter_mask_type[MAX_FLEXCAN_CAN_FILTERS];
    __u8 filter_mask_type_count;

    __u32 filter_id[MAX_FLEXCAN_CAN_FILTERS];
    __u32 filter_count;

};


int initTerminalInterface(int fd, speed_t interfaceBaud, uint8_t readMinChar);
void setFilterAndMasks(FLEXCAN_filter_mask *filter_array, int numfilter, int port_fd);
void configureFlowControl( struct FLEXCAN_Flow_Control *configuration_array, int numFlowCodes, int port_fd);

char* getPortName(int portNumber){
    if (portNumber==1){
        return const_cast<char *>(CAN1_TTY);
    }
    else if (portNumber ==2){
        return const_cast<char *>(CAN2_TTY);}
    else {
        return const_cast<char *>("-1");
    }
}

static void throwException(JNIEnv *env, const char *message, const char* add) {
    char msg[128];
    sprintf(msg, message, add);
    jclass cls = env->FindClass("java/lang/IllegalArgumentException");
    if (cls == 0) {
        return;
    }
    env->ThrowNew(cls, msg);
}

int serial_set_nonblocking(int fd){
    int flags;

    if(-1 == (flags = fcntl(fd, F_GETFL))) {
        ALOGE("CAN jni: %s:%d fnctl: %s\n", __FILE__, __LINE__, strerror(errno));
        return -1;
    }

    if(-1 == fcntl(fd, F_SETFL, flags | O_NONBLOCK))
    {
        ALOGE("CAN jni: %s:%d: fcntl: %s\n", __FILE__, __LINE__, strerror(errno));
        return -1;
    }
    return 0;
}

int serial_init(char *portName){
    // Open corresponding ttyCANx
    //
    if (0 == strcmp(portName, CAN1_TTY)) {
        if (-1 == fd_CAN1) {
            if ((fd_CAN1 = open(portName, O_RDWR | O_NOCTTY | O_NONBLOCK)) < 0) {
                fd_CAN1 = -1;
                perror(portName);
                ALOGE("CAN jni: failed to open %s, error: %s", portName, strerror(errno));
                return -1;
            }
            serial_set_nonblocking(fd_CAN1);
            ALOGD("CAN jni: opened port: '%s', fd=%d", CAN1_TTY, fd_CAN1);
            initTerminalInterface(fd_CAN1, B115200, 1);
        }

        return fd_CAN1;
    } else if (0 == strcmp(portName,CAN2_TTY)) {
        if (-1 == fd_CAN2) {
            if ((fd_CAN2 = open(portName, O_RDWR | O_NOCTTY | O_NONBLOCK)) < 0) {
                fd_CAN2 = -1;
                perror(portName);
    			ALOGE("CAN jni: failed to open %s, error: %s", portName, strerror(errno));
                return -1;
            }
            serial_set_nonblocking(fd_CAN2);
            ALOGD("CAN jni: opened port: '%s', fd=%d", CAN2_TTY, fd_CAN2);
            initTerminalInterface(fd_CAN2, B115200,1);
        }
        return fd_CAN2;
    }

    return -1;
}

int initTerminalInterface(int fd, speed_t interfaceBaud, uint8_t readMinChar) {
    if (isatty(fd)) {
        ALOGD("The port that is being init is a tty port");
    } else {
        ALOGD("The port that is being init is not a ttyport");
    }
    struct termios ios;

    tcgetattr(fd, &ios);
    bzero(&ios, sizeof(ios));

    cfmakeraw(&ios);
    cfsetospeed(&ios, interfaceBaud);
    cfsetispeed(&ios, interfaceBaud);
    ios.c_cflag = (interfaceBaud | CS8 | CLOCAL | CREAD) & ~(CRTSCTS | CSTOPB | PARENB);
    ios.c_iflag = 0;
    ios.c_oflag = 0;
    ios.c_lflag = 0;        /* disable ECHO, ICANON, etc... */
    ios.c_cc[VTIME] = 10;   /* unit: 1/10 second. */
    ios.c_cc[VMIN] = readMinChar;     /* minimal characters for reading */

    tcsetattr(fd, TCSANOW, &ios);
    tcflush(fd, TCIOFLUSH);

    return 0;
}

int closeCAN(int close_fd) {
    // first always close the CAN module (bug #250)
    // http://192.168.1.234/redmine/issues/250
    // Vladimir:
    // TODO: the command format depend on previous open
    // Should be fixed
    //
    char buf[256];
    sprintf(buf, "C\r");
    if (-1 == write(close_fd, buf, strlen(buf))) {
        ALOGE("CAN jni: write %s command failed\n", buf);
    }
    close(close_fd);
    if (fd_CAN1 == close_fd) {
        fd_CAN1 =-1;
    } else {
        fd_CAN2 =-1;
    }
    ALOGD("Closed CAN channel ");
    return 0;
}

/**
 * Returns the file descriptor associated with the tty port
 **/
int getFd(int portNumber){
    if (portNumber == CAN1_TTY_NUMBER) {
        return fd_CAN1;
    } else if (portNumber == CAN2_TTY_NUMBER) {
        return fd_CAN2;
    }
    else return -1; 
}

/**
 * sendMessage(fileDescriptorToSendTo, Message)
 * This function writes the entire message to the port associated with the file descriptor.
 * */
int sendMessage(int fd_port, const char * message) {
    char buf[256]={0};
    sprintf(buf, "%s", message);
    printf("Send %s\n", buf);
    if (-1 == write(fd_port, buf, strlen(buf))) {
        ALOGE("CAN jni: %s command coudln't be written! \n", buf );
        closeCAN(fd_port);
        return -1;
    }
    return 0;
}
/**
 * setFlowControlMessage(FlowMessageType, RequestId, ResponseId, NumberOfResponseDataBytes, ResponseDataBytes, fd)
 * Constructs a Flow control message and sends teh flow control command to the CAN_TTY ports.
 * The user can set up to 8 flow codes for each can instance.
 * */
void setFlowControlMessage(char type,char *searchID,char *responseID, int dataLength, BYTE* dataBytes, int port_fd){

    char *flowControlMessage = new char[36];
    memset(flowControlMessage,'\0',sizeof(char));
    int i = 0, j = 0, k=0;
    int standardMessageLength=0, extendedMessageLength=0;
    uint8_t tmp1=0;

    flowControlMessage[i++]='M';

    if(type=='T') {
        flowControlMessage[i++]='F';
        //Add search ID
        for (i = 2; i<=9; i++) {
            flowControlMessage[i] = searchID[j];
            j++;
        }
        //Add response ID
        for(i=10; i<=17; i++){
            flowControlMessage[i]=responseID[k];
            k++;
        }
        //Add data length

        flowControlMessage[i++]=dataLength + '0';

        //Add response data bytes
        for(i=19;i<((2*dataLength)+18);i++){
            for (int ind=0; ind < dataLength; ind++){
                tmp1 = (dataBytes[ind] >> 4) & 0xF;
                if (tmp1 > 9)
                    flowControlMessage[i] = tmp1 - 10 + 'A';
                else
                    flowControlMessage[i] = tmp1 + '0';
                i++;
                tmp1 = dataBytes[ind] & 0xF;
                if (tmp1 > 9)
                    flowControlMessage[i] = tmp1 - 10 + 'A';
                else
                    flowControlMessage[i] = tmp1 + '0';
                i++;
            }
        }
        i--;
        flowControlMessage[i++]=CAN_OK_RESPONSE;
        extendedMessageLength=i;
        flowControlMessage[i]= 0;
    }

    else if (type=='t'){
        flowControlMessage[i++]='f';

        //Add search ID
        for (i = 2; i<=4; i++) {
            flowControlMessage[i] = searchID[j];
            j++;
        }
        //Add response ID
        for(i=5; i<=7; i++){
            flowControlMessage[i]=responseID[k];
            k++;
        }
        //Add data length
        flowControlMessage[i++]= dataLength + '0';

        //Add response data bytes
        for(i=9;i<((2*dataLength)+8);i++){
            for (int ind=0; ind < dataLength; ind++){
                tmp1 = (dataBytes[ind] >> 4) & 0xF;
                if (tmp1 > 9)
                    flowControlMessage[i] = tmp1 - 10 + 'A';
                else
                    flowControlMessage[i] = tmp1 + '0';
                i++;
                tmp1 = dataBytes[ind] & 0xF;
                if (tmp1 > 9)
                    flowControlMessage[i] = tmp1 - 10 + 'A';
                else
                    flowControlMessage[i] = tmp1 + '0';
                i++;
            }
        }
        i--;
        //Add CAN_OK_RESPONSE Character
        flowControlMessage[i++]=CAN_OK_RESPONSE;
        standardMessageLength=i;
        flowControlMessage[i]= 0;
    }

    if(type == 't'){
        ALOGD("Start printing Message; Length = %d", standardMessageLength );
        for(i = 0; i < standardMessageLength; i++){
            ALOGD("flowControlMessage[%d] = %c", i, flowControlMessage[i] );
        }
    }
    else {
        ALOGD("Start printing Message; Length = %d", extendedMessageLength );
        for(i = 0; i < extendedMessageLength; i++){
            ALOGD("flowControlMessage[%d] = %c", i, flowControlMessage[i] );
        }
    }

        //Check for valid extended and standard flow command based on its length
    if((flowControlMessage[1]=='F' &&  flowControlMessage[extendedMessageLength-1]==CAN_OK_RESPONSE) ||
       (flowControlMessage[1]=='f' &&  flowControlMessage[standardMessageLength-1]==CAN_OK_RESPONSE)){
        if (-1 == sendMessage(port_fd, flowControlMessage)) {
            ALOGE("CAN jni: !!!!Error configuring flow message: %s for Flow code: !!!!", searchID);
        }
        ALOGD("Flow message SET: %s", flowControlMessage);
    } else
        ALOGE("CAN jni: Flow control command coundn't be sent! Message: %s, Extended Message size=%d or StandardMessageSize=%d", flowControlMessage, extendedMessageLength,standardMessageLength);
}

/**
 * setMasks(mask, maskType, fd)
 * Builds a mask command to set acceptable masks for receive.
 * The masks are stated as global per CAN instance.
 * A user can set up to 16 Masks per can instance.
 * */
int setMasks(char *mask, char type, int port_fd) {
    char maskString[16];
    char maskCommand[16];
    char standardFormat[5]={'0','0','0','0','0'};
    int maskLength;
    int i = 0, j = 0, x=0;


    if(mask!=NULL) {
        maskString[i] = 'm';
        i++;
        maskString[i++] = type;

        if ((type == 'T') || (type == 'R')) {
            for (i = 2; i < 10; i++) {
                maskString[i] = mask[j];
                j++;
            }
        } else if ((type == 't') || (type == 'r')) {
            for (i = 2; i < 7; i++) {
                maskString[i] = standardFormat[x];
            }
            for (i = 7; i < 10; i++) {
                maskString[i] = mask[j];
                j++;
            }
        }
        maskString[i++] = CAN_OK_RESPONSE;
        maskLength = i;
        maskString[i++]= 0;

        //send Mask string
        memcpy(maskCommand, maskString,maskLength);
        if (-1 == sendMessage(port_fd, maskString)) {
            ALOGE("!!!!Error sending Mask message: %s for Filter: !!!!", maskString);
        }
        ALOGD("Mask set SET %s", mask);
    }
    else ALOGE("!!!MASK NOT SET - NULL/Empty MASK PASSED!!!");
	return 0;
}

/**
 * setFilters(filter, filterType, fd)
 * Builds a filter command to set acceptable filters/codes for receive.
 * The filters are stated as global per CAN instance.
 * A user can set up to 24 Filters per can instance.
 * */
int setFilters(char *filter, char type, int port_fd) {
    char filterCommand[16]={0};
    char filterString[16]={0};
    char standardFormat[5]={'0','0','0','0','0'};
    int filters_length;
    int i = 0, j = 0, x=0;

    if(filter!=NULL) {
        filterString[i++] = 'M';
        filterString[i++] = type;

        if ((type == 'T') || (type == 'R')) {
            for (i = 2; i < 10; i++) {
                filterString[i] = filter[j];
                j++;
            }
        } else if ((type == 't') || (type == 'r')) {
            for (i = 2; i < 7; i++) {
                filterString[i] =standardFormat[x];
                x++;
            }
            for (i = 7; i < 10; i++) {
                filterString[i] = filter[j];
                j++;
            }
        }
        filterString[i++] = CAN_OK_RESPONSE;
        filters_length = i;
        filterString[i++]= 0;

        memcpy(filterCommand, filterString,filters_length);
        if (-1 == sendMessage(port_fd, filterCommand)) {
            ALOGE("!!!!Error sending Filter message: %s for Filter: !!!!", filterString);
        }
        ALOGD("Filter SET: Filter- %s", filter);
    }
    else ALOGE("!!!! NULL FILTER PASSED !!!");
	return 0;
}

int setBitrate(int fd, int speed) {
    int baud;
    switch(speed) {
        case 10000:
            baud = 0;
            break;
        case 20000:
            baud = 1;
            break;
        case 33330:
            baud = 2;
            break;
        case 50000:
            baud = 3;
            break;
        case 100000:
            baud = 4;
            break;
        case 125000:
            baud = 5;
            break;
        case 250000:
            baud = 6;
            break;
        case 500000:
            baud = 7;
            break;
        case 800000:
            baud = 8;
            break;
        case 1000000:
            baud = 9;
            break;
        default:
            baud = 6;
            break;
    }
    char buf[256];
    sprintf(buf, "S%d\r", baud);
    if (-1 == write(fd, buf, strlen(buf))) {
        ALOGE("CAN jni: Write Failed! Command - %s \n", buf);
        //closeCAN(fd);
        return -1;
    }
    return 0;
}

int openCANandSetTerm(int fd, bool term) {
    int termination = term ? 1 : 0;

    char buf[256];
    sprintf(buf, "O%d\r", termination);
    if (-1 == write(fd, buf, strlen(buf))) {
        ALOGE("CAN jni: Write Failed! Command - %s \n", buf);
        return -1;
    }
    ALOGD("CAN jni: Opened CAN channel with termination set to = %d ",termination);
    return 0;
}

int setListeningModeandTerm(int fd, bool term) {
    int termination = term ? 1 : 0;
    char buf[256];
    sprintf(buf, "L%d\r", termination);
    if (-1 == write(fd, buf, strlen(buf))) {
        ALOGE("CAN jni: Couldn't Open Channel in Listening Mode: Write Failed! Command - %s \n", buf);
        return -1;
    }
    return 0;
}

int sendReadStatusCommand(int fd) {
    char buf[256];
    sprintf(buf, "F\r");
    if (-1 == write(fd, buf, strlen(buf))) {
        ALOGE("CAN jni: Read Status Flag Config Failed: Write Failed! Command - %s \n", buf);
        closeCAN(fd);
        return -1;
    }
    return 0;
}

int serial_send_data(BYTE *mydata, DWORD bytes_to_write, int fd) {
    DWORD numwr = 0;
    if(fd != -1){
        numwr = write(fd, mydata, bytes_to_write);
        if(numwr == bytes_to_write){
            ALOGD("CAN jni: Frame sent sucessfully! Number of bytes=%d, frame=%s, fd=%d",bytes_to_write, mydata,fd);
            return 0;
        } else{
            //TODO: this may not be an error
            ALOGD("CAN jni: Alert - Frame sent to port! but the number of bytes written were not equal to what it should have written. numwr=%d, bytes_to_write=%d", numwr, bytes_to_write);
            //LOGD("Check me! Number of bytes=%d, frame=%s, fd=%d",bytes_to_write, mydata,fd);
            return 0;
        }
    }
    else {
        ALOGE("CAN jni: Alert - FileDescriptor = %d!", fd);
        closeCAN(fd);
        return -1 ;
    }
}

/**
 * Creates and configures a Can Interface for communication
 * This configures the tty ports does all the following:
 *     a. Sets Filters and Masks, Max filters = 24, Max masks = 16.
 *     b. Set upto 8 flow control codes
 *     c. Sets the can baud rate
 *     d. Configures can termination [Enable/Disable]
 *     e. Configures the control mode [Listening?]
 *     f. Opens Can channel for communication
 */
int can_config_and_open(bool listeningModeEnable, int bitrate, int termination, FLEXCAN_filter_mask* filter_array,int numfilter, char *portName, FLEXCAN_Flow_Control flexcan_flow_control[],int numOfFlowMessages)
{
    int ret=-1;
    char *port = portName;
    int fd= -1;

    fd = ret = serial_init(port);

    if (fd == -1){
        return -1;
    }

    if (initTerminalInterface(fd, B9600,1) == -1) {
        return -1;
    }

    /**
    * First always close1939Port the CAN module (bug #250)
    * http://192.168.1.234/redmine/issues/250/
    */
    // Vladimir:
    // Such stupid, two days debugging. Close interface should destruct as CAN as file handle but here requiers CAN only
    // 
    #if 0
        if (closeCAN(fd) == -1) {
            return -1;
        }
    #else
        // Vladimir:
        // TODO: the command format depend on previous open
        // Should be fixed
        //
        char buf[8];
        sprintf(buf, "C\r");
        if (-1 == write(fd, buf, strlen(buf))) {
            ALOGE("CAN jni: write %s command failed\n", buf);
        }
    #endif

    /**
     * The firmware has a 100ms delay after closing the port.
     */
    usleep(100000);

    /**
     * Configuring CAN Baud rate
     * The baud rate must be set after closing the CAN module.
     */
    if(setBitrate(fd, bitrate) == -1) {
        return -1;
    }

    /**
     * Configuring Filter List
     */
    setFilterAndMasks(filter_array, numfilter,fd);

    /**
     * The firmware has a 25ms delay between configuring filters and flow control codes
     */
    usleep(25000);

    /**
     * Configure Flow Control if the user has set any flow control codes
     */
    if(numOfFlowMessages > 0){
        configureFlowControl(flexcan_flow_control, numOfFlowMessages,fd);
    }


    /**
     * Open CAN interface for communication and enable/disable termination
     */
    if(listeningModeEnable){
        if(setListeningModeandTerm(fd, termination) == -1){
            return -1;
        }
    } else {
        if(openCANandSetTerm(fd, termination) == -1){
            return -1;
        }
    }

    /**
     * 100ms delay after CAN channel is opened.
     */
    usleep(100000);

   /**
    * TODO: Research more about me.
    * if(sendReadStatusCommand(fd) == -1) {
    *    return -1;
    * }
    */
    return ret;
}

/**
 * Converts the Canbus frame to a string build building a message that can be sent to the mcu.
 * Message Formats
 * -------------------------------------------------------------------------------------------
 * Extended Frame: TiiiiiiiiLdd..[CR]
 * iiiiiiii - Identifier,  IdRange = [0x00000000 - 0x1FFFFFFF]
 * L - Number of data bytes [length], Range = [0-8]
 * dd - Byte value in Hex, Max number of dd pairs = 8
 * Note: The number of dd pairs must match the number of dd pairs.
 * -------------------------------------------------------------------------------------------
 * Standard Frame: tiiildd..[CR]
 * iii - IdRange = [0x000 - 0x7FF]
 * L - Number of data bytes [length], Range = [0-8]
 * dd - Byte value in Hex, Max number of dd pairs = 8
 * Note: The number of dd pairs must match the number of dd pairs.
 * -------------------------------------------------------------------------------------------
 * Extended Remote: Riiiiiiii[CR]
 * iiiiiiii - Identifier,  IdRange = [0x00000000 - 0x1FFFFFFF]
 * -------------------------------------------------------------------------------------------
 * Standard Remote: riii[CR]
 * iii - IdRange = [0x000 - 0x7FF]
 * ------------------------------------------------------------------------------------------
 * This returns the length of the formatted string
 *
 */
int32_t ParseCanMessToString(int msg_type, int id, int data_len, BYTE * data, uint8_t * pDestBuff){

    uint8_t tmp1, ind, *pmsg_str, curr_msg_len = 0;

    if (NULL == data || NULL == pDestBuff) {
        ALOGD("CAN jni: %s: Error wrong params\n", __func__);
        return -1;
    }

    pmsg_str = (uint8_t*)pDestBuff;

    //The first byte defines the message type
    // T - Extended CAN Frame
    // t - Standard CAN Frame
    // R - Remote Extended Frame
    // r - Standard Remote Frame

    if(msg_type==EXTENDED){
        *pmsg_str = 'T';
    }
    else if(msg_type==STANDARD){
        *pmsg_str = 't';
    }
    else if (msg_type==EXTENDED_REMOTE){
        *pmsg_str = 'R';
    }
    else if (msg_type==STANDARD_REMOTE){
        *pmsg_str = 'r';
    }

    pmsg_str++;
    curr_msg_len++;

    //Append the CAN Id to the string [Hex values as characters]
    if ((msg_type == EXTENDED) | (msg_type == EXTENDED_REMOTE)) {
        // Extended / Extended Remote Identifier
        sprintf ( (char*)pmsg_str, "%08x", id);
        pmsg_str += 8;
        curr_msg_len += 8;
    } else if ((msg_type == STANDARD) || (msg_type == STANDARD_REMOTE)){
        // Standard / Standard Remote Identifier
        sprintf ( (char*)pmsg_str, "%03x", id);
        pmsg_str += 3;
        curr_msg_len += 3;
    }

    //Append the Message length
    *pmsg_str = data_len + '0';
    pmsg_str++;
    curr_msg_len ++;

    // Append the data bytes to the frame
    if ((msg_type==EXTENDED) || (msg_type==STANDARD)) {
        for (ind = 0; ind < data_len; ind++) {
            tmp1 = (data[ind] >> 4) & 0xF;
            if (tmp1 > 9)
                *pmsg_str = tmp1 - 10 + 'A';
            else
                *pmsg_str = tmp1 + '0';

            pmsg_str++;
            curr_msg_len++;

            tmp1 = data[ind] & 0xF;
            if (tmp1 > 9)
                *pmsg_str = tmp1 - 10 + 'A';
            else
                *pmsg_str = tmp1 + '0';

            pmsg_str++;
            curr_msg_len++;
        }
    }

    //Add a CAN_OK_RESPONSE character
    *pmsg_str = CAN_OK_RESPONSE;

    curr_msg_len++;

    return (int32_t)curr_msg_len;
}

/**
 * Sends a Canbus frame to the Can ports.
 * */
void FlexCAN_send_can_packet(BYTE type, DWORD id, int data_len, BYTE *data, int portNumber) {
    int fd=-1;
    uint8_t canPacketToTx[MAX_PACKET_SIZE] = {0};
    int msgLength = 0;
    fd = getFd(portNumber);

    msgLength = ParseCanMessToString(type, id, data_len, data, canPacketToTx);

    if( serial_send_data(canPacketToTx, msgLength, fd) < 0){
        ALOGE("CAN jni: !!!!!!!!!!!!!!! Couldn't send FLEXCAN CAN message !!!!!!!!!!!!!!!!!");
        return;
    }
}

char getCharType(uint8_t type){
    char typeChar=0;
    if(type==0){
        typeChar='t';
    } else if(type==1){
        typeChar='T';
    } else if(type==2){
        typeChar='r';
    } else if(type==3){
        typeChar='R';
    }
    return typeChar;
}
/**
 * Cofigures upto 24 filters and 16 masks for the Can ports
 * Filter and Mask Format:
 * ----------------------------------------------------------------------------------
 * Filters
 * M<t/T/r/R>iiiiiiii[CR]
 * iiiiiiii - Acceptable code for receive.
 * Accepted IDs Range in Hex = [0x00000000 - 0x1FFFFFFF]
 * T - Extended Filter
 * t - Standard Filter
 * R - Extended Remote Filter
 * r - Remote Standard Filter
 * ----------------------------------------------------------------------------------
 * Masks
 * m<t/T/r/R>iiiiiiii[CR]
 * iiiiiiii - Acceptable mask for receive.
 * Accepted IDs Range in Hex = [0x00000000 - 0x1FFFFFFF]
 * T - Extended Mask
 * t - Standard Mask
 * R - Extended Remote Mask
 * r - Remote Standard Mask
 * ---------------------------------------------------------------------------------
 *
 * */
void setFilterAndMasks(FLEXCAN_filter_mask *filter_array, int numfilter, int port_fd){

    uint32_t filterId=0;
    char filterIdString[MAX_MASK_FILTER_SIZE]={0};
    uint8_t filterSetCount=0;
    uint8_t maskSetCount=0;

    uint8_t filterMaskType=0;
    char filterMaskTypeChar=0;

    uint32_t maskId=0;
    char maskIdString[MAX_MASK_FILTER_SIZE]={0};

    struct FLEXCAN_filter_mask tmp_filter={ .mask_id = {0},
											.mask_count = 0,
											.filter_mask_type = {0},
											.filter_mask_type_count = 0,
											.filter_id = {0},
											.filter_count = 0
   										   };

        tmp_filter = *filter_array;

        for (uint8_t index=0; index < tmp_filter.filter_count; index++) {

            filterMaskType = tmp_filter.filter_mask_type[index];
            filterMaskTypeChar = getCharType(filterMaskType);

            filterId = tmp_filter.filter_id[index];
            if(filterMaskTypeChar == 'T'){
                sprintf ((char*)filterIdString, "%08x", filterId);
            }
            else if(filterMaskTypeChar=='t'){
                sprintf ((char*)filterIdString, "%03x", filterId);
            }

            if(filterSetCount < tmp_filter.filter_count || filterSetCount <= 24){
                setFilters(filterIdString, filterMaskTypeChar,port_fd);
                usleep(5000);
                filterSetCount++;
                ALOGD("Filter Set, No of filters set = %d", filterSetCount);
            }

            maskId = tmp_filter.mask_id[index];
            if(filterMaskTypeChar == 'T'){
                sprintf ( (char*)maskIdString, "%08x", maskId);
            }
            else if(filterMaskTypeChar=='t'){
                sprintf ((char*)maskIdString, "%03x", maskId);
            }
            if(maskSetCount<tmp_filter.mask_count && maskSetCount<=16){
                setMasks(maskIdString, filterMaskTypeChar,port_fd);
                maskSetCount++;
                usleep(5000);
                ALOGD("Mask Set, No of masks set = %d", maskSetCount);
            }
        }
    filterMaskTypeChar=0;
}

void configureFlowControl( struct FLEXCAN_Flow_Control *configuration_array, int numFlowCodes, int port_fd){

    int count = 0;
    int flowCodeSetCount=0;
    char flowMessageTypeChar='\0';
    char searchIdString[MAX_FlexCAN_Flowcontrol_CAN + 2]; //Additional space for NULL terminator of sprintf
    char responseIdString[MAX_FlexCAN_Flowcontrol_CAN + 2]; //Additional space for NULL terminator of sprintf
    BYTE dataBytes[8]={0,0,0,0,0,0,0,0};

    for(count = 0; count < numFlowCodes; count++ ){
        ALOGD("CAN jni: configureFlowControl: count=%d, numFlowCodes=%d, messageType=%d", count,  numFlowCodes, configuration_array[count].flow_msg_type);
        flowMessageTypeChar = getCharType(configuration_array[count].flow_msg_type);

        if(flowMessageTypeChar=='T'){
            // Parse Search Id
            sprintf ((char*)searchIdString, "%08x", configuration_array[count].search_id);
            //Parse Response Id
            sprintf ((char*)responseIdString, "%08x", configuration_array[count].response_id);
        } else if(flowMessageTypeChar=='t'){
            // Parse Search Id
            sprintf ((char*)searchIdString, "%03x", configuration_array[count].search_id);
            //Parse Response Id
            sprintf ((char*)responseIdString, "%03x", configuration_array[count].response_id);
        }

        for(int j = 0; j < 8; j++){
           // LOGD("Flow message %d, Structure dataBytes[%d] = %x", count , j, configuration_array[count].response_data_bytes[j]);
        }

        //Workaround to fix compile time error
        dataBytes[0] = 0;
        for(int index = 0; index < configuration_array[count].flow_msg_data_length; index++){
            //LOGD("Flow message %d, Old data bytes %x to data bytes from structure = %x", index, dataBytes[index], configuration_array[count].response_data_bytes[index] );
            dataBytes[index] = configuration_array[count].response_data_bytes[index];
            //LOGD("Flow message %d, dataBytes[%d] = %x", count , index, dataBytes[index]);
        }

        if((flowCodeSetCount <= numFlowCodes) && (flowCodeSetCount<=8)) {
            setFlowControlMessage(flowMessageTypeChar, searchIdString, responseIdString, configuration_array[count].flow_msg_data_length,dataBytes, port_fd);
            memset(dataBytes,0, sizeof dataBytes);
            usleep(5000);
            flowCodeSetCount++;
            ALOGD("Flow control message set, No of filters set = %d", flowCodeSetCount);
        }
    }
}

// JNI functions
int close_native(JNIEnv* *env, jobject instance, jint port){
    return closeCAN(getFd(port));
}

int readStatusCommand(JNIEnv* *env, jobject instance, jint fd){
    return sendReadStatusCommand(fd);
}
/**
 * Configure CAN Interface
 * Begin with initialising everything
 * 1. Start working with hardware filters. Get Filter, Mask and Ids and store them in a structure
 * 2. Derive the TTY port that needs to be configured
 * 3. Get the flow control codes
 * 4. Create a new interface based on the port number
 * 5. Set the right variables - file descriptors
 * */
int configureCanAndOpen(JNIEnv *env, jobject instance, jboolean listeningModeEnable, jint bitrate, jboolean termination,jobjectArray  hardwarefilter, jint port_number,jobjectArray flowControl){
    // /Initialisation
    static char *port=NULL;
    //int ttyport_number=0;
    int i=0,f=0,m=0,fmt=0;
    int total_masks = 0, total_filters = 0, total_filter_mask_types=0;
    struct FLEXCAN_Flow_Control flowControlMessageArray[8];
    struct FLEXCAN_filter_mask filter_array[24];
    int numfilter = 0;
    int numFlowControlMessages = 0;
    int x =0 , flowMesgCount = 0;

    if (hardwarefilter != NULL){
        numfilter = env->GetArrayLength (hardwarefilter);
    }

    if (flowControl != NULL){
        numFlowControlMessages = env->GetArrayLength (flowControl);
    }

    ALOGD("Flow Control Messages in JNI = %d", numFlowControlMessages);

    //jclass clazz = env->FindClass("com/micronet/canbus/FlexCANVehicleInterfaceBridge");

    //Get Filters and Masks from the Object
    for (i = 0; i < numfilter; i++){

        jobject element = env->GetObjectArrayElement(hardwarefilter, i);

        //get filter ids array
        jclass cls = env->GetObjectClass(element);
        jmethodID methodId = env->GetMethodID(cls, "getIds", "()[I");
        jintArray ids = (jintArray)env->CallObjectMethod(element, methodId);
        jint* ints = env->GetIntArrayElements(ids, NULL);
        jsize lengthOfArray = env->GetArrayLength(ids);

        //get masks array
        jmethodID methodMaskId = env->GetMethodID(cls, "getMask", "()[I");
        jintArray masks = (jintArray)env->CallObjectMethod(element, methodMaskId);
        jint* maskInts = env->GetIntArrayElements(masks, NULL);
        jsize lengthOfMaskArray = env->GetArrayLength(masks);

        //Get filter and Mask type array
        jmethodID methodFilterType = env->GetMethodID(cls, "getFilterMaskType", "()[I");
        jintArray FilterType = (jintArray)env->CallObjectMethod(element, methodFilterType);
        jint* filterMaskTypeInts = env->GetIntArrayElements(FilterType, NULL);
        jsize lengthOfFilterMaskTypeArray = env->GetArrayLength(FilterType);

        //Saving Filter ids
        filter_array[i].filter_count = lengthOfArray;
        for (f = 0; f < lengthOfArray; f++) {
            filter_array[i].filter_id[f] = ints[f];
            total_filters++;
        }

        // Saving the mask ids
        filter_array[i].mask_count = lengthOfMaskArray;
        for (m = 0; m < lengthOfMaskArray; m++) {
            filter_array[i].mask_id[m] = maskInts[m];
            total_masks++;
        }

        //Saving Filter and Mask types
        filter_array[i].filter_mask_type_count = lengthOfFilterMaskTypeArray;
        for (fmt= 0; fmt < lengthOfFilterMaskTypeArray; fmt++) {
            if (fmt < MAX_FLEXCAN_CAN_FILTERS)
            {
                filter_array[i].filter_mask_type[fmt] = filterMaskTypeInts[fmt];
                total_filter_mask_types++;
            }
            else
            {
                //throwException(env, "Hardware Filter: %s tried to pass array index of filter_mask_type", "err");
            }
            filter_array[i].filter_mask_type[fmt] = filterMaskTypeInts[fmt];
            total_filter_mask_types++;
        }
    }

    //The maximum number of filters that can be added are 24; Otherwise the MCU overwrites the existing filters.
    if (total_filters > 24){
        char str_filters [20];
        snprintf(str_filters, sizeof(str_filters), "%d", i);
        throwException(env, "Hardware Filter: Too many filter ids (%s). Max allowed - 24", str_filters);
    }

    //The maximum number of masks that can be set are 16.
    if (total_masks > 16){
        char str_masks [20];
        snprintf(str_masks, sizeof(str_masks), "%d", total_masks);
        throwException(env, "Hardware Filter: Too many mask ids (%s). Max allowed - 16", str_masks);
    }

    //Set the correct port number
    if(port_number == 1 || port_number == 2){
        port = getPortName(port_number);
    } else{
        throwException(env, "Entered an incorrect port number: %d ", "err");

    }

    //Get Flow control codes
    if(flowControl != NULL){

        for(x = 0; x < numFlowControlMessages; x++){

            jobject  flowElement = env->GetObjectArrayElement(flowControl, x);
            jclass flowClass = env->GetObjectClass(flowElement);

            jmethodID methodSearchId = env->GetMethodID(flowClass, "getSearchId", "()I");
            uint32_t searchId = (uint32_t) env->CallIntMethod(flowElement,methodSearchId);
            flowControlMessageArray[x].search_id = searchId;

            jmethodID methodResponseId = env->GetMethodID(flowClass, "getResponseId", "()I");
            uint32_t responseId = (uint32_t) env->CallIntMethod(flowElement,methodResponseId);
            flowControlMessageArray[x].response_id = responseId;

            jmethodID methodMesgType = env->GetMethodID(flowClass, "getFlowMessageType", "()I");
            uint8_t mesgType = (uint8_t) env->CallIntMethod(flowElement,methodMesgType);
            flowControlMessageArray[x].flow_msg_type = mesgType;

            jmethodID methodDataResponseLength = env->GetMethodID(flowClass, "getFlowDataLength", "()I");
            uint8_t mesgLength = (uint8_t) env->CallIntMethod(flowElement,methodDataResponseLength);
            flowControlMessageArray[x].flow_msg_data_length = mesgLength;

            jmethodID methodResponseDataBytes=env->GetMethodID(flowClass,"getDataBytes","()[B");
            jbyteArray responseDataBytes=(jbyteArray)env->CallObjectMethod(flowElement, methodResponseDataBytes);
            jbyte *bufferPtr = env->GetByteArrayElements(responseDataBytes,NULL);
            
            for(int i = 0; i < flowControlMessageArray[x].flow_msg_data_length; i ++){
                flowControlMessageArray[i].response_data_bytes[i]= (uint8_t) bufferPtr[i];
                ALOGD("Response data bytes from Java: %d ----> Response data bytes stored: %d", (uint8_t) bufferPtr[i],flowControlMessageArray[i].response_data_bytes[i]);
            }

            //TODO: Set Codes
            flowMesgCount ++;
            ALOGD("Flow Message %d stored", flowMesgCount);
        }

        if(flowMesgCount > 8){
            char str [20];
            snprintf(str, sizeof(str), "%d", i);
            throwException(env, "Flow Messages: Too many flow codes (%s) set. Max allowed - 8", str);
        }
    }

    //Create a interface
    if (port_number==1) {
        jint fdCanPort1 = can_config_and_open(listeningModeEnable, bitrate, termination, filter_array, numfilter, port, flowControlMessageArray, numFlowControlMessages);
        //jfieldID fd_id;
        //fd_id = env->GetFieldID(clazz, "fdCanPort1", "I");
       // env->SetIntField(instance, fd_id, fdCanPort1);
        if (fdCanPort1 < 0){
          return SYSTEM_ERROR;
        }
    }
    else if (port_number==2){
        jint fdCanPort2 = can_config_and_open(listeningModeEnable, bitrate, termination, filter_array, numfilter, port, flowControlMessageArray, numFlowControlMessages);
        //jfieldID fd_id;
        //fd_id = env->GetFieldID(clazz, "fdCanPort2", "I");
        //env->SetIntField(instance, fd_id, fdCanPort2);
        if (fdCanPort2 < 0){
          return SYSTEM_ERROR;
        }
    }


    return 0;
}

int sendFrameToCan(JNIEnv *env, jobject obj, jobject canbusFrameObj, jint canNumber) {

    int id, type;

    jclass cls = env->GetObjectClass(canbusFrameObj);
    jmethodID methodId = env->GetMethodID(cls, "getData", "()[B");
    jbyteArray data = (jbyteArray)env->CallObjectMethod(canbusFrameObj, methodId);

    methodId = env->GetMethodID(cls, "getId", "()I");
    id = env->CallIntMethod(canbusFrameObj, methodId);

    jbyte* bufferPtr = env->GetByteArrayElements(data, NULL);
    jsize lengthOfArray = env->GetArrayLength(data);
    methodId = env->GetMethodID(cls, "getType", "()I");
    type = env->CallIntMethod(canbusFrameObj, methodId);

    FlexCAN_send_can_packet((BYTE)type,id,lengthOfArray, (BYTE*) bufferPtr, canNumber);

    env->ReleaseByteArrayElements(data, bufferPtr, JNI_ABORT);

    return 0;
}


static const char *classPathName = "com/android/server/serial/CanbusService";

static const JNINativeMethod methods[] = {
{"close_native",                         "(I)I",                                     (int*)close_native },
{"sendFrameToCan",                         "(Lcom/android/server/serial/CanbusFrame;I)I",                                     (int*)sendFrameToCan },
{"configureCanAndOpen",                         "(ZIZ[Lcom/android/server/serial/CanbusHardwareFilter;I[Lcom/android/server/serial/CanbusFlowControl;)I",                                     (int*)configureCanAndOpen },
};

int registerFuncsCanbus (JNIEnv *_env){
    return android::AndroidRuntime::registerNativeMethods(
            _env, classPathName, methods, NELEM(methods));
}
