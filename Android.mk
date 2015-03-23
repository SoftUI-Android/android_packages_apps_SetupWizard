LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# Allow to include device specific pages
ifneq ($(BOARD_SETUP_WIZARD_CLASS),)
    $(foreach bcp,$(BOARD_SETUP_WIZARD_CLASS), \
        $(eval LOCAL_SRC_FILES += $(call all-java-files-under, ../../../$(bcp))))
endif

BASE_SRC_FILES += $(call all-java-files-under, src/)

unique_specific_classes :=
    $(foreach cf,$(LOCAL_SRC_FILES), \
        $(eval unique_specific_classes += $(notdir $(cf))))

default_classes :=
$(foreach cf,$(BASE_SRC_FILES), \
    $(if $(filter $(unique_specific_classes),$(notdir $(cf))),,\
        $(eval default_classes += $(cf))))

LOCAL_SRC_FILES += $(default_classes)

LOCAL_MODULE_TAGS := optional

LOCAL_PACKAGE_NAME := NamelessSetupWizard
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v4 \
    android-support-v13 \
    play \
    libphonenumber

LOCAL_JAVA_LIBRARIES := \
    telephony-common

# Include res dir from chips
google_play_dir := ../../../external/google/google_play_services/libproject/google-play-services_lib/res
res_dir := $(google_play_dir) res

ifneq ($(BOARD_SETUP_WIZARD_RESOURCES),)
    res_dir += ../../../$(BOARD_SETUP_WIZARD_RESOURCES)
endif

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dir))
LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages com.google.android.gms

include $(BUILD_PACKAGE)
