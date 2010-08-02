# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# As per the Apache license requirements, this file has been modified
# from its original state.
#
# Such modifications are Copyright (C) 2010 Ben Gruver, and are released
# under the original license

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    Main.c

LOCAL_C_INCLUDES := \
		dalvik/include \
		dalvik \
		dalvik/libdex \
		dalvik/vm \
		$(JNI_H_INCLUDE)

LOCAL_SHARED_LIBRARIES := \
		libdvm \
		libcutils

LOCAL_MODULE:= deodexerant

LOCAL_CFLAGS += -DANDROID_VER="$(PLATFORM_VERSION)"

include $(BUILD_EXECUTABLE)
