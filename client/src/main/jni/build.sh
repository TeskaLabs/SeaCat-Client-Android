#!/bin/bash -e

# To transfer fresh changes from SeaCat C-Core run this:
# rm -rvf seacat-ccore/libs sseacat-ccore/include && cp -vr ~/Workspace/seacat/client-ccore/build-android/ ./seacat-ccore/ && ./build.sh
#

. ./build.site

JAVAC=javac

# Clean everything first
rm -f mobi_seacat_client_core_seacatcc.h
rm -fr ../obj ../libs/armeabi-v7a ../libs/armeabi ../libs/x86

# Prepare compiled class
mkdir -p ../../../build/jni/classes
${JAVAC} -d ../../../build/jni/classes -classpath ~/Library/Android/sdk/platforms/android-25/android.jar:../java ../java/com/teskalabs/seacat/android/client/core/seacatcc.java

# Prepare header file
javah -d . -classpath ~/Library/Android/sdk/platforms/android-25/android.jar:../../../build/jni/classes com.teskalabs.seacat.android.client.core.seacatcc

# Compile Android JNI
${ANDROID_NDK}/ndk-build -B
