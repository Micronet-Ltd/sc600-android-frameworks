#include <unistd.h>
#include <errno.h>
#include <inttypes.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <fcntl.h>
#include <jni.h>
#include <nativehelper/JNIHelp.h>
#include <sys/poll.h>
#include <sys/epoll.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <linux/netlink.h>
#include <linux/can.h>
#include <linux/can/raw.h>
#include <net/if.h>
#include <android/log.h>
#include <utils/Log.h>
#include <cutils/properties.h>
#include "android_runtime/AndroidRuntime.h"
#include "com_android_server_socket_canbus.h"

static void throwException(JNIEnv *env, const char *message, const char *add)
{
    char msg[128];

    sprintf(msg, message, add);
    jclass cls = env->FindClass("java/lang/IllegalArgumentException");
    if (!cls) {
        return;
    }
    env->ThrowNew(cls, msg);
}

int select_bit_rate(int br)
{
    char buf[PROPERTY_VALUE_MAX] = {0};
    int val;

    switch (br) {
        case 125000:
            break;
        case 250000:
            break;
        case 500000:
            break;
        case 800000:
            break;
        case 1000000:
            break;
        default:
            br = 125000;
            ALOGE("%s: incorrect bitrate, set default 125000\n", __func__);
            break;
    }

    property_get("vendor.can.set_bitrate", buf, "125000");
    val = atoi(buf);
    if (val == br) {
        return 0;
    }
    sprintf(buf, "%d", br);
    property_set("vendor.can.set_bitrate", buf);

    return 0;
}

int socket_can_mcp25x_mask(uint32_t *m, uint32_t *f)
{
    uint32_t rxm0[2] = {0};
    uint32_t rxm1[2] = {0};
    uint32_t rxf_s[6] = {0};
    uint32_t rxf_e[6] = {0};
    int i;
    char buf[PROPERTY_VALUE_MAX] = {0};

    for (i = 0; i < 6; i++, f++) {
        if (*f & CAN_EFF_FLAG) {
            rxf_e[i] = *f & CAN_EFF_MASK;
        } else {
            rxf_s[i] = *f & CAN_SFF_MASK; 
        }
    }

    for (i = 0; i < 2; i++) {
        if (rxf_e[i]) {
            rxm0[1] = *m;
        } else {
            rxm0[0] = *m;
        }
    }

    if (rxm0[0] && rxm0[1]) {
        rxm0[0] = 0;
        rxm0[1] &= CAN_EFF_MASK; 
    } else {
        if (rxm0[1]) {
            rxm0[1] &= CAN_EFF_MASK;
        } else {
            rxm0[0] &= CAN_SFF_MASK;
        }
    }

    for (i = 2; i < 6; i++) {
        if (rxf_e[i]) {
            rxm1[1] = *m;
        } else {
            rxm1[0] = *m;
        }
    }

    if (rxm1[0] && rxm1[1]) {
        rxm1[0] = 0;
        rxm1[1] &= CAN_EFF_MASK; 
    } else {
        if (rxm1[1]) {
            rxm1[1] &= CAN_EFF_MASK;
        } else {
            rxm1[0] &= CAN_SFF_MASK;
        }
    }

    sprintf(buf, "%x:%x,%x:%x", rxm0[0], rxm0[1], rxm1[0], rxm1[1]);
    property_set("vendor.can.masks", buf);

    sprintf(buf, "%x:%x-%d,%x:%x-%d,%x:%x-%d,%x:%x-%d,%x:%x-%d,%x:%x-%d\n",
                   rxf_s[0], rxf_e[0], (rxf_e[0])?1:0, rxf_s[1], rxf_e[1], (rxf_e[1])?1:0,
                   rxf_s[2], rxf_e[2], (rxf_e[2])?1:0, rxf_s[3], rxf_e[3], (rxf_e[3])?1:0,
                   rxf_s[4], rxf_e[4], (rxf_e[4])?1:0, rxf_s[5], rxf_e[5], (rxf_e[5])?1:0);
    property_set("vendor.can.filters", buf);

    return 0;
}

int select_op_mode(const char *op_mode)
{
    char buf[PROPERTY_VALUE_MAX] = {0};
    char mode[16] = {0};

    if (!op_mode) {
        return -1;
    }

    if (!strncmp(op_mode, "normal", strlen("normal"))) {
        sprintf(mode, "listen-only off");
    } else if (!strncmp(op_mode, "listen-only", strlen("listen-only"))) {
        sprintf(mode, "listen-only on");
    } else {
        return -1;
    }

    property_get("vendor.can.set_op_mode", buf, "listen-only off");
    if (!strncmp(mode, buf, strlen(mode))) {
        return 0;
    }
    property_set("vendor.can.set_op_mode", mode);

    return 0;
}

int select_link(const char *up)
{
    char buf[PROPERTY_VALUE_MAX] = {0};
    char link[16] = {0};

    if (!up) {
        return -1;
    }

    if (!strncmp(up, "up", strlen("up"))) {
        sprintf(link, "%s", up);
    } else if (!strncmp(up, "down", strlen("down"))) {
        sprintf(link, "%s", up);
    } else {
        return -1;
    }

    property_get("vendor.can", buf, "down");
    if (!strncmp(link, buf, strlen(link))) {
        return 0;
    }
    ALOGD("%s: link %s selected\n", __func__, link);
    property_set("vendor.can", link);

    return 0;
}

int socket_can_close(int fd)
{
    close(fd);
    ALOGD("%s: socket closeed\n", __func__);
    return 0;
}

int socket_can_open(const char *link_name)
{
    int s;

	if ((s = socket(PF_CAN, SOCK_RAW, CAN_RAW)) < 0) {
		ALOGD("%s: failure to create socket\n", __func__);
		return -1;
	}

    return s;
}

// accepted if rec_id & filter == ids & filter
// id and filters must include EFF_FLAG, RTR_FLAG ERR_FLAG
//

int socket_can_config(int s, int echo_en, uint32_t *ids, uint32_t *flts, int num , int errs, int join)
{
    const int timestamp_on = 1;
//    const int dropmonitor_on = 1;
    const int canfd_on = 0;
    int i;
    can_err_mask_t err_m = 0;
    struct can_filter *rflt;

    if (errs) {
        err_m = errs;
        if (err_m)
            setsockopt(s, SOL_CAN_RAW, CAN_RAW_ERR_FILTER, &err_m, sizeof(err_m));
    }

    if (join) {
        setsockopt(s, SOL_CAN_RAW, CAN_RAW_JOIN_FILTERS, &join, sizeof(join));
    }

//    setsockopt(s, SOL_CAN_RAW, CAN_RAW_FILTER, 0, 0); 
    if (num) {
        rflt = (struct can_filter *)malloc(sizeof(struct can_filter) * num); 
        if (!rflt) {
            ALOGD("%s: not enough memory for filters\n", __func__);
            return 1;
        }

        for (i = 0; i < num; i++) {
            rflt[i].can_id = ids[i];
            rflt[i].can_mask = flts[i]; 
        }
        setsockopt(s, SOL_CAN_RAW, CAN_RAW_FILTER, rflt, num * sizeof(struct can_filter));
        free((struct can_filter *)rflt);
    }

    setsockopt(s, SOL_CAN_RAW, CAN_RAW_LOOPBACK, &echo_en, sizeof(echo_en));
    setsockopt(s, SOL_CAN_RAW, CAN_RAW_FD_FRAMES, &canfd_on, sizeof(canfd_on));
    setsockopt(s, SOL_SOCKET, SO_TIMESTAMP, &timestamp_on, sizeof(timestamp_on));
//    setsockopt(s, SOL_SOCKET, SO_RXQ_OVFL, &dropmonitor_on, sizeof(dropmonitor_on));

    return 0;
}

int socket_can_bind(int s, const char *link)
{
    struct sockaddr_can addr;
    struct ifreq ifr;
    int link_l;

    link_l = strlen(link);
    if (link_l > IFNAMSIZ - 1) {
        link_l = IFNAMSIZ - 1;
    }
    strncpy(ifr.ifr_name, link, link_l); 

	ifr.ifr_name[link_l] = '\0';
	ifr.ifr_ifindex = if_nametoindex(ifr.ifr_name);
	if (!ifr.ifr_ifindex) {
        ALOGD("%s: invalid link[%s <-- %s, %d]\n", __func__, ifr.ifr_name, link, ifr.ifr_ifindex);
        ALOGD("%s: %s, %d]\n", __func__, strerror(errno), errno);
		return -1;
	}

	memset(&addr, 0, sizeof(addr));
	addr.can_family = AF_CAN;
	addr.can_ifindex = ifr.ifr_ifindex;
    if (bind(s, (struct sockaddr *)&addr, sizeof(addr)) < 0) {
        ALOGD("%s: failure to bind\n", __func__);
        return -1;
    }

    return addr.can_ifindex;
}

// can id
//  EFF_FLAG 0x80000000U extended id
//  RTR_FLAG 0x40000000U remote transmission request
//  ERR_FLAG 0x20000000U error message frame
//  EFF_MASK 0x1FFFFFFFU extended id
//  SFF_MASK 0x000007FFU standard id
//  ERR_MASK 0x1FFFFFFFU err id omit EFF, RTR, ERR
//
int socket_can_send(int s, int id, int dlc, unsigned char *payload)
{
    struct can_frame frame;

    if (!((id & (CAN_EFF_FLAG | CAN_RTR_FLAG | CAN_ERR_FLAG)) || payload)) {
        return -1;
    }
    frame.can_id = id;
    frame.can_dlc = dlc;
    if (payload) {
        memcpy(frame.data, payload, frame.can_dlc);
    }

    if (write(s, &frame, CAN_MTU) != CAN_MTU) {
        ALOGD("%s: failure to send frame\n", __func__);
        return 1;
    }

    return 0;
}

static inline uint64_t timestamp_ms(const struct timeval *tv)
{
		uint64_t tv2ms = tv->tv_sec * 1000;
        tv2ms += tv->tv_usec/1000;
        return tv2ms;
}

#define DEBUG_FRAMES 0

int socket_can_recvmsg(int s, int idx, int *id, int *dlc, uint64_t *ts_ms, int *dropped, unsigned char *payload)
{
    int epoll_fd, err;
//    int link_l;
	struct iovec iov;
	struct msghdr msg;
	struct cmsghdr *cmsg;
    struct can_frame frame;
    struct timeval tv;
    struct epoll_event events_pending;
    struct epoll_event event_setup = {
        .events = EPOLLIN,
    };
    struct sockaddr_can addr;
//	struct ifreq ifr;
    char ctrlmsg[CMSG_SPACE(sizeof(struct timeval)) +
             CMSG_SPACE(3 * sizeof(struct timespec)) +
             CMSG_SPACE(sizeof(__u32))];

//	signal(SIGTERM, sigterm);
//	signal(SIGHUP, sigterm);
//	signal(SIGINT, sigterm);

    if (dropped) {
        *dropped = 0;
    }

/*
    link_l = strlen(link);
    if (link_l > IFNAMSIZ - 1) {
        link_l = IFNAMSIZ - 1;
    }
    strncpy(ifr.ifr_name, link, link_l); 

    ifr.ifr_name[link_l] = '\0';
    ifr.ifr_ifindex = if_nametoindex(ifr.ifr_name);
    if (!ifr.ifr_ifindex) {
        ALOGD("%s: invalid link[%s <-- %s, %d]\n", __func__, ifr.ifr_name, link, ifr.ifr_ifindex);
        ALOGD("%s: %s, %d]\n", __func__, strerror(errno), errno);
        return -1;
    }
*/

    memset(&addr, 0, sizeof(addr));
    addr.can_family = AF_CAN;
    addr.can_ifindex = idx; //ifr.ifr_ifindex;

    epoll_fd = epoll_create(1);
	if (epoll_fd < 0) {
        ALOGD("%s: failure to create epoll\n", __func__);
		return -1;
	}

    event_setup.data.ptr = &s;
    if (epoll_ctl(epoll_fd, EPOLL_CTL_ADD, s, &event_setup)) {
        ALOGD("%s: failure to setup epoll\n", __func__);
        close(epoll_fd);
        return -1;
    }

    /* try to switch the socket into CAN FD mode */
    //setsockopt(s, SOL_CAN_RAW, CAN_RAW_FD_FRAMES, &canfd_on, sizeof(canfd_on));

	iov.iov_base = &frame;
	msg.msg_name = &addr;
	msg.msg_iov = &iov;
	msg.msg_iovlen = 1;
	msg.msg_control = &ctrlmsg;

    if (epoll_wait(epoll_fd, &events_pending, 1, -1) < 0) {
        if (errno != EINTR) {
            ALOGD("%s: %s, %d]\n", __func__, strerror(errno), errno);
            err = -1;
        } else {
            ALOGD("%s: %s, %d]\n", __func__, strerror(errno), errno);
            err = 1;
        }
    } else {
        /* these settings may be modified by recvmsg() */
        int rcvd;
        int sock = *(int *)events_pending.data.ptr;
        iov.iov_len = sizeof(frame);
        msg.msg_namelen = sizeof(addr);
        msg.msg_controllen = sizeof(ctrlmsg);
        msg.msg_flags = 0;

        rcvd = recvmsg(sock, &msg, 0);

        if (rcvd < 0) {
            if (errno == ENETDOWN) {
                ALOGD("%s: link down\n", __func__);
                err = 2;
            } else {
                ALOGD("%s: failure to retceive frame, probably socket closed\n", __func__);
                err = 1;
            }
        }

        uint32_t drop = 0;
        if ((size_t)rcvd != CAN_MTU) {
            ALOGD("%s: incomplete frame\n", __func__);
            if (dropped) {
                *dropped += drop;
            }
            err = 1;
        } else {
            for (cmsg = CMSG_FIRSTHDR(&msg); cmsg && (cmsg->cmsg_level == SOL_SOCKET); cmsg = CMSG_NXTHDR(&msg,cmsg)) {
                if (cmsg->cmsg_type == SO_TIMESTAMP) {
                    memcpy(&tv, CMSG_DATA(cmsg), sizeof(tv));
                } else if (cmsg->cmsg_type == SO_TIMESTAMPING) {
                    struct timespec *stamp = (struct timespec *)CMSG_DATA(cmsg);

                    /*
                     * stamp[0] is the software timestamp
                     * stamp[1] is deprecated
                     * stamp[2] is the raw hardware timestamp
                     * See chapter 2.1.2 Receive timestamps in
                     * linux/Documentation/networking/timestamping.txt
                     */
                    tv.tv_sec = stamp[2].tv_sec;
                    tv.tv_usec = stamp[2].tv_nsec/1000;
                } else if (cmsg->cmsg_type == SO_RXQ_OVFL) {
                    memcpy(&drop, CMSG_DATA(cmsg), sizeof(drop));
                    if (dropped) {
                        *dropped += drop;
                    }
                }
            }
        }
#if DEBUG_FRAMES
        ALOGD("%s: %" PRIu64 "\n", __func__, timestamp_ms(&tv));
        ALOGD("%s: id %s%s%s%x \n", __func__, (frame.can_id & CAN_EFF_FLAG)?"E":"S",
                                              (frame.can_id & CAN_RTR_FLAG)?"R":" ",
                                              (frame.can_id & CAN_ERR_FLAG)?"ER":"  ",
                                              frame.can_id & CAN_EFF_MASK);
        ALOGD("%s: length %d \n", __func__, frame.can_dlc);
        ALOGD("%s: ", __func__);
        for (err = 0; err < frame.can_dlc; err++) {
            ALOGD("0x%x ", (unsigned int)frame.data[err]);
        }
        ALOGD("\n");
#endif
        if (id) {
            *id = frame.can_id;
        }
        if (dlc) {
            *dlc = frame.can_dlc;
        }
        if (ts_ms) {
            *ts_ms = timestamp_ms(&tv);
        }
        if (payload) {
            memcpy(payload, frame.data, frame.can_dlc);
        }
        err = 0;
    }

	close(epoll_fd);

    return err;
}

// JNI functions
int select_bit_rate_native(JNIEnv *env, jobject instance, jint br)
{
    return select_bit_rate(br);
}

int select_mask_native(JNIEnv *env, jobject instance, jobject m, jobject f)
{
    if (!m || !f) {
        throwException(env, "Invalid %s", "rmx0..1, rfx0..5");
        return -1;
    }

    jint *rxm = (jint *)env->GetDirectBufferAddress(m);
    int ml = (int)env->GetDirectBufferCapacity(m);
    jint *rxf = (jint *)env->GetDirectBufferAddress(f);
    int fl = (int)env->GetDirectBufferCapacity(f);

    if (ml < 2) {
        ALOGD("%s: mask buffer too small\n", __func__);
        return -1;
    }

    if (fl < 6) {
        ALOGD("%s: filters buffer too small\n", __func__);
        return -1;
    }

    return socket_can_mcp25x_mask((uint32_t *)rxm, (uint32_t *)rxf);
}

int select_op_mode_native(JNIEnv *env, jobject instance, jstring op_mode)
{
    int res = -1;

    if (!op_mode) {
        throwException(env, "Invalid %s", "op_mode");
        return -1;
    }

    const char *mode = env->GetStringUTFChars(op_mode, 0);
    if (mode) {
        res = select_op_mode(mode);
        env->ReleaseStringUTFChars(op_mode, mode);
    }

    return res;
}

int select_link_native(JNIEnv *env, jobject instance, jstring op)
{
    int res = -1;

    if (!op) {
        throwException(env, "Invalid %s", "op");
        return -1;
    }

    const char *up = env->GetStringUTFChars(op, 0);
    if (up) {
        res = select_link(up);
        env->ReleaseStringUTFChars(op, up);
    } else {
        ALOGD("%s: invalid parameter\n", __func__);
    }

    return res;
}

int close_native(JNIEnv *env, jobject instance, jint s)
{
    return socket_can_close(s);
}

int open_native(JNIEnv *env, jobject instance, jstring link)
{
    int s = -1;

    if (!link) {
        throwException(env, "Invalid %s", "link");
        return -1;
    }

    const char *l = env->GetStringUTFChars(link, 0);
    if (l) {
        s = socket_can_open(l);
        env->ReleaseStringUTFChars(link, l);
    }

    return s;
}

int config_native(JNIEnv *env, jobject instance,
                  jstring link, jobject ids_m, jobject flt_m, jint num, jint err_m, jint s, jint echo_en, jint join)
{
    if (!link) {
        throwException(env, "Invalid %s", "link");
        return -1;
    }

    const char *l = env->GetStringUTFChars(link, 0);
    if (l) {
        env->ReleaseStringUTFChars(link, l);
    }

    jint *ids = 0;
    jint *flts = 0;

    if (num) {
        ids = (jint *)env->GetDirectBufferAddress(ids_m);
        flts = (jint *)env->GetDirectBufferAddress(flt_m);
    }

    return socket_can_config(s, echo_en, (uint32_t *)ids, (uint32_t *)flts, num , err_m, join);
}

int bind_native(JNIEnv *env, jobject instance, jstring link, jint s)
{
    int err = -1;

    if (!link || !s) {
        throwException(env, "Invalid %s", "link/socket");
        return -1;
    }

    const char *l = env->GetStringUTFChars(link, 0);
    if (l) {
        err = socket_can_bind(s, l);
        env->ReleaseStringUTFChars(link, l);
    }

    return err;
}

int send_native(JNIEnv *env, jobject instance, jint s, jint id, jint dlc, jbyteArray payload)
{
    int err = -1;

    if (!s) {
        throwException(env, "Invalid %s", "socket");
        return -1;
    }

    jbyte *p = 0;
    if (dlc) {
        p = env->GetByteArrayElements(payload, 0);
    }

    err = socket_can_send(s, id, dlc, (unsigned char *)p);

    if (p) {
        env->ReleaseByteArrayElements(payload, p, 0);
    }

    return err;
}

int recvmsg_native(JNIEnv *env, jobject instance, jint s, jint idx, jobject id, jobject dlc, jobject ts, jobject dropped, jobject payload)
{
    int err = -1;

    if (!s) {
        throwException(env, "Invalid %s", "socket");
        return -1;
    }

    jbyte *p = 0;
    jint *i = 0;
    jint *d = 0;
    jlong *t = 0;
    jint *dr = 0;

    i = (jint *)env->GetDirectBufferAddress(id);
    d = (jint *)env->GetDirectBufferAddress(dlc);
    t = (jlong *)env->GetDirectBufferAddress(ts);
    dr = (jint *)env->GetDirectBufferAddress(dropped);
    if (d) {
        /*unsigned char frm_pld[CAN_MAX_DLEN];
        p = frm_pld*/
        p = (jbyte *)env->GetDirectBufferAddress(payload);
    }

#if DEBUG_FRAMES
    ALOGD("%s: tsa %" PRIu64 "\n", __func__, (uint64_t)t);
    ALOGD("%s: ida %x \n", __func__, (unsigned int)i);
    ALOGD("%s: dlc %x[%d] \n", __func__, (unsigned int)d, (d)?(int)*d:-1);
    ALOGD("%s: pa %x \n", __func__, (unsigned int)p);
#endif

    err = socket_can_recvmsg(s, idx, i, d, (uint64_t *)t, dr, (unsigned char *)p);

    if (p) {
        /*
        env->SetByteArrayRegion(payload, 0, *d, frm_pld);
        */
    }

    return err;
}

static const char *classPathName = "com/android/server/net/CanbusService";

static const JNINativeMethod methods[] = {
    // bitrates 115000, 250000, 500000, 800000, 1000000
    {"select_bit_rate_native",  "(I)I",                             (int *)select_bit_rate_native},
    // mask m0, m1, filt
    {"select_mask_native",  "(Ljava/nio/IntBuffer;Ljava/nio/IntBuffer;)I",
                                                                    (int *)select_mask_native},
    // normal/listen-only
    {"select_op_mode_native",   "(Ljava/lang/String;)I",            (int *)select_op_mode_native},
    // up/down
    {"select_link_native",      "(Ljava/lang/String;)I",            (int *)select_link_native},
    // close socket
    {"close_native",            "(I)I",                             (int *)close_native},
    // return socket
    {"open_native",             "(Ljava/lang/String;)I",            (int *)open_native},
    // config socket, param en local echo and filters(idsMask array, filteMask array, errorMask array, filters caount)
    {"config_native",           "(Ljava/lang/String;Ljava/nio/IntBuffer;Ljava/nio/IntBuffer;IIIII)I",
                                                                    (int *)config_native},
    // bind link to socket
    {"bind_native",             "(Ljava/lang/String;I)I",           (int *)bind_native},
    // send canframe
    {"send_native",             "(III[B)I",                         (int *)send_native},
    //receive canframe
    {"recvmsg_native",          "(IILjava/nio/IntBuffer;Ljava/nio/IntBuffer;Ljava/nio/LongBuffer;Ljava/nio/IntBuffer;Ljava/nio/ByteBuffer;)I",
                                                                    (int *)recvmsg_native},
};

int registerFuncsCanbus (JNIEnv *_env){
    return android::AndroidRuntime::registerNativeMethods(_env, classPathName, methods, NELEM(methods));
}
