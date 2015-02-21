#!/bin/bash

ANDROID_NDK=~/Library/Android/android-ndk-r10d
JAVA_HOME=/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home

JAVAC=javac

# Clean everything first
rm -f mobi_seacat_client_internal_*.h
rm -fr ../obj ../libs/armeabi-v7a ../libs/armeabi ../libs/x86

# Prepare compiled class
mkdir -p ../bin/classes
${JAVAC} -d ../bin/classes -classpath ~/Android/sdk/platforms/android-19/android.jar:../src/ ../src/mobi/seacat/client/core/seacatcc.java

# Prepare header file
javah -d . -classpath ~/Android/sdk/platforms/android-19/android.jar:../bin/classes mobi.seacat.client.core.seacatcc

# Compile Android JNI
${ANDROID_NDK}/ndk-build
