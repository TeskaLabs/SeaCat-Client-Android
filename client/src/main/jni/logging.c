#include <jni.h>
#include <assert.h>
#include <android/log.h>

#include <seacatcc.h>

void seacatjni_log_fnct(char level, const char * message)
{
	static char * seacat_TAG = "SeaCat";
	int prio;

	switch (level)
	{
		case 'D': prio = ANDROID_LOG_DEBUG; break;
		case 'I': prio = ANDROID_LOG_INFO; break;
		case 'W': prio = ANDROID_LOG_WARN; break;
		case 'E': prio = ANDROID_LOG_ERROR; break;
		case 'F': prio = ANDROID_LOG_FATAL; break;
		default:  prio = ANDROID_LOG_DEBUG;
	}

	__android_log_write(prio, seacat_TAG, message);
}
