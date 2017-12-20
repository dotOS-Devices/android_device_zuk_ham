LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CLANG_CFLAGS += \
        -Wno-error=unused-private-field \
        -Wno-error=strlcpy-strlcat-size \
        -Wno-error=gnu-designator \
        -Wno-error=unused-variable \
        -Wno-error=format \
        -Wno-error=sign-compare \
        -Wno-unused-parameter \
        -Wno-tautological-pointer-compare


LOCAL_SRC_FILES := \
        QCamera3Factory.cpp \
        QCamera3Hal.cpp \
        QCamera3HWI.cpp \
        QCamera3Mem.cpp \
        QCamera3Stream.cpp \
        QCamera3Channel.cpp \
        QCamera3PostProc.cpp \
        QCamera3VendorTags.cpp \
        ../util/QCameraCmdThread.cpp \
        ../util/QCameraQueue.cpp

LOCAL_CFLAGS := -Wall -Werror
LOCAL_CFLAGS += -DHAS_MULTIMEDIA_HINTS


LOCAL_C_INCLUDES := \
        $(LOCAL_PATH)/../stack/common \
        frameworks/native/include/media/openmax \
        frameworks/native/include \
        frameworks/av/include \
        $(call project-path-for,qcom-media)/libstagefrighthw \
        system/media/camera/include \
        $(LOCAL_PATH)/../../mm-image-codec/qexif \
        $(LOCAL_PATH)/../../mm-image-codec/qomx_core \
        $(LOCAL_PATH)/../util

LOCAL_C_INCLUDES += \
        $(call project-path-for,qcom-display)/libgralloc

LOCAL_C_INCLUDES += $(TARGET_OUT_INTERMEDIATES)/KERNEL_OBJ/usr/include
LOCAL_ADDITIONAL_DEPENDENCIES += $(TARGET_OUT_INTERMEDIATES)/KERNEL_OBJ/usr

LOCAL_SHARED_LIBRARIES := libcamera_client liblog libhardware libutils libcutils libdl libsensor
LOCAL_SHARED_LIBRARIES += libmmcamera_interface libmmjpeg_interface
LOCAL_SHARED_LIBRARIES += libhidltransport libsensor android.hidl.token@1.0-utils android.hardware.graphics.bufferqueue@1.0
LOCAL_STATIC_LIBRARIES := libarect

LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/hw
LOCAL_MODULE := camera.$(TARGET_DEVICE)
LOCAL_MODULE_TAGS := optional
LOCAL_PROPRIETARY_MODULE := true

include $(BUILD_SHARED_LIBRARY)

#include $(LOCAL_PATH)/test/Android.mk
