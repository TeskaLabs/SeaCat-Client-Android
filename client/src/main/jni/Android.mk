SEACAT_CLIENT_CCORE_PATH := ./seacat-ccore
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := ssl
LOCAL_SRC_FILES := ./seacat-ccore/openssl/$(TARGET_ARCH_ABI)/lib/libssl.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := crypto
LOCAL_SRC_FILES := ./seacat-ccore/openssl/$(TARGET_ARCH_ABI)/lib/libcrypto.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := ccore
LOCAL_SRC_FILES := ./seacat-ccore/libs/$(TARGET_ARCH_ABI)/libseacatcc.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_C_INCLUDES := ./seacat-ccore/include
LOCAL_LDLIBS := -llog -lz
LOCAL_MODULE    := seacatjni
LOCAL_SRC_FILES := seacatjni.c logging.c

LOCAL_STATIC_LIBRARIES := ccore ssl crypto

include $(BUILD_SHARED_LIBRARY)
