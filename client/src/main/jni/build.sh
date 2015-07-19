#!/bin/bash

ANDROID_NDK=~/Library/Android/android-ndk-r10d
JAVA_HOME=/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home

JAVAC=javac

# Clean everything first
rm -f mobi_seacat_client_core_seacatcc.h
rm -fr ../obj ../libs/armeabi-v7a ../libs/armeabi ../libs/x86

# Prepare compiled class
mkdir -p ../../../build/jni/classes
${JAVAC} -d ../../../build/jni/classes -classpath ~/Library/Android/sdk/platforms/android-10/android.jar:../java ../java/mobi/seacat/client/core/seacatcc.java

# Prepare header file
javah -d . -classpath ~/Library/Android/sdk//platforms/android-10/android.jar:../../../build/jni/classes mobi.seacat.client.core.seacatcc

# Compile Android JNI
${ANDROID_NDK}/ndk-build
