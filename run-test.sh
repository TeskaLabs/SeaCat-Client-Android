#!/bin/bash

TEST_OUT=/tmp/mobi.seacat.client.android.test.out

while true
do
	rm -f ${TEST_OUT}
	~/Android/sdk/platform-tools/adb shell am instrument -w mobi.seacat.client.android.test/android.test.InstrumentationTestRunner | tee ${TEST_OUT}

	RESULT=`cat ${TEST_OUT} | grep -e "^OK " | cut -d" " -f1`
	if [ "$RESULT" != "OK" ];
	then
		break       	   #Abandon the loop.
	fi
done

