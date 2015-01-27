SEACAT_CLIENT_CCORE_PATH := /Users/alex/Workspace/seacat/client-ccore
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := ssl
LOCAL_SRC_FILES := $(SEACAT_CLIENT_CCORE_PATH)/openssl/android/$(TARGET_ARCH_ABI)/lib/libssl.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := crypto
LOCAL_SRC_FILES := $(SEACAT_CLIENT_CCORE_PATH)/openssl/android/$(TARGET_ARCH_ABI)/lib/libcrypto.a
include $(PREBUILT_STATIC_LIBRARY)


include $(CLEAR_VARS)
LOCAL_MODULE := ccore
LOCAL_SRC_FILES := $(SEACAT_CLIENT_CCORE_PATH)/android/libs/$(TARGET_ARCH_ABI)/libseacatcc.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_C_INCLUDES := $(SEACAT_CLIENT_CCORE_PATH)/src
LOCAL_LDLIBS := -llog -lz
LOCAL_MODULE    := seacatjni
LOCAL_SRC_FILES := seacatjni.c

LOCAL_STATIC_LIBRARIES := ccore ssl crypto

include $(BUILD_SHARED_LIBRARY)
