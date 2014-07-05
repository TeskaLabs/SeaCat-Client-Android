#!/bin/bash

rm -f mobi_seacat_client_internal_*.h
rm -fr ../obj ../libs/armeabi-v7a ../libs/armeabi ../libs/x86

javah -d . -classpath ~/Android/sdk/platforms/android-19/android.jar:../bin/classes mobi.seacat.client.internal.JNI
~/Android/android-ndk-r9d/ndk-build
