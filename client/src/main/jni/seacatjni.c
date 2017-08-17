#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <math.h>
#include <sys/socket.h>

#include <seacatcc.h>

#include "com_teskalabs_seacat_android_client_core_seacatcc.h"

///

static JavaVM * g_java_vm = NULL;

static jmethodID g_buffer_limit_mid = 0;
static jmethodID g_buffer_position_mid = 0;


static jobject g_reactor_obj = NULL;
static jmethodID g_reactor_JNICALLBACK_write_ready_mid = 0;
static jmethodID g_reactor_JNICALLBACK_read_ready_mid = 0;
static jmethodID g_reactor_JNICALLBACK_frame_received_mid = 0;
static jmethodID g_reactor_JNICALLBACK_frame_return_mid = 0;
static jmethodID g_reactor_JNICALLBACK_worker_request_mid = 0;
static jmethodID g_reactor_JNICALLBACK_evloop_heartbeat_mid = 0;
static jmethodID g_reactor_JNICALLBACK_evloop_started_mid = 0;
static jmethodID g_reactor_JNICALLBACK_gwconn_reset_mid = 0;
static jmethodID g_reactor_JNICALLBACK_gwconn_connected_mid = 0;
static jmethodID g_reactor_JNICALLBACK_state_changed_mid = 0;
static jmethodID g_reactor_JNICALLBACK_clientid_changed_mid = 0;

static jobject g_write_buffer_obj = NULL;
static jobject g_read_buffer_obj = NULL;

///

static void JNICALLBACK_write_ready(void ** data, uint16_t * data_len);
static void JNICALLBACK_read_ready(void ** data, uint16_t * data_len);
static void JNICALLBACK_frame_received(void * data, uint16_t frame_len);
static void JNICALLBACK_frame_return(void * data);
static void JNICALLBACK_worker_request(char worker);
static double JNICALLBACK_evloop_heartbeat(double now);

static void JNICALLBACK_evloop_started(void);
static void JNICALLBACK_gwconn_connected(void);
static void JNICALLBACK_gwconn_reset(void);
static void JNICALLBACK_state_changed(void);
static void JNICALLBACK_clientid_changed(void);

///

#ifdef __ANDROID__
#define JNINATIVEINTERFACEPTR const struct JNINativeInterface ***
#else
#define JNINATIVEINTERFACEPTR void **
#endif

///

void seacatjni_log_fnct(char level, const char * message);

////

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
	JNIEnv* env;

	g_java_vm = vm;

	if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK)
	{
		return -1;
	}

	jclass cls = (*env)->FindClass(env, "java/nio/Buffer");
	
	g_buffer_limit_mid = (*env)->GetMethodID(env, cls, "limit", "()I");
	if (g_buffer_limit_mid == NULL) return -1;
	
	g_buffer_position_mid = (*env)->GetMethodID(env, cls, "position", "()I");
	if (g_buffer_position_mid == NULL) return -1;

	seacatcc_log_setfnct(seacatjni_log_fnct);

	return JNI_VERSION_1_6;
}

////

JNIEXPORT jint JNICALL Java_com_teskalabs_seacat_android_client_core_seacatcc_init(JNIEnv * env, jclass cls, jstring applicationId, jstring applicationIdSuffix, jstring varDir, jobject reactor)
{
	assert(g_reactor_obj == NULL);

	const char * appIdChar = (*env)->GetStringUTFChars(env, applicationId, 0);
	const char * appIdSuffixChar = (applicationIdSuffix != NULL) ? (*env)->GetStringUTFChars(env, applicationIdSuffix, 0) : NULL;
	const char * varDirChar = (*env)->GetStringUTFChars(env, varDir, 0);

	// convert local to global reference (local will die after this method call)
	g_reactor_obj = (*env)->NewGlobalRef(env, reactor);
	assert(g_reactor_obj != NULL);

	// save refs for callback
	jclass g_clazz = (*env)->GetObjectClass(env, g_reactor_obj);
	if (g_clazz == NULL) return SEACATCC_RC_E_GENERIC;

	g_reactor_JNICALLBACK_write_ready_mid = (*env)->GetMethodID(env, g_clazz, "JNICallbackWriteReady", "()Ljava/nio/ByteBuffer;");
	if (g_reactor_JNICALLBACK_write_ready_mid == NULL) return SEACATCC_RC_E_GENERIC;

	g_reactor_JNICALLBACK_read_ready_mid = (*env)->GetMethodID(env, g_clazz, "JNICallbackReadReady", "()Ljava/nio/ByteBuffer;");
	if (g_reactor_JNICALLBACK_read_ready_mid == NULL) return SEACATCC_RC_E_GENERIC;

	g_reactor_JNICALLBACK_frame_received_mid = (*env)->GetMethodID(env, g_clazz, "JNICallbackFrameReceived", "(Ljava/nio/ByteBuffer;I)V");
	if (g_reactor_JNICALLBACK_frame_received_mid == NULL) return SEACATCC_RC_E_GENERIC;

	g_reactor_JNICALLBACK_frame_return_mid = (*env)->GetMethodID(env, g_clazz, "JNICallbackFrameReturn", "(Ljava/nio/ByteBuffer;)V");
	if (g_reactor_JNICALLBACK_frame_return_mid == NULL) return SEACATCC_RC_E_GENERIC;

	g_reactor_JNICALLBACK_worker_request_mid = (*env)->GetMethodID(env, g_clazz, "JNICallbackWorkerRequest", "(C)V");
	if (g_reactor_JNICALLBACK_worker_request_mid == NULL) return SEACATCC_RC_E_GENERIC;

	g_reactor_JNICALLBACK_evloop_heartbeat_mid = (*env)->GetMethodID(env, g_clazz, "JNICallbackEvLoopHeartBeat", "(D)D");
	if (g_reactor_JNICALLBACK_evloop_heartbeat_mid == NULL) return SEACATCC_RC_E_GENERIC;

	g_reactor_JNICALLBACK_evloop_started_mid = (*env)->GetMethodID(env, g_clazz, "JNICallbackEvLoopStarted", "()V");
	if (g_reactor_JNICALLBACK_evloop_started_mid == NULL) return SEACATCC_RC_E_GENERIC;

	g_reactor_JNICALLBACK_gwconn_connected_mid = (*env)->GetMethodID(env, g_clazz, "JNICallbackGWConnConnected", "()V");
	if (g_reactor_JNICALLBACK_gwconn_connected_mid == NULL) return SEACATCC_RC_E_GENERIC;

	g_reactor_JNICALLBACK_gwconn_reset_mid = (*env)->GetMethodID(env, g_clazz, "JNICallbackGWConnReset", "()V");
	if (g_reactor_JNICALLBACK_gwconn_reset_mid == NULL) return SEACATCC_RC_E_GENERIC;

	g_reactor_JNICALLBACK_state_changed_mid = (*env)->GetMethodID(env, g_clazz, "JNICallbackStateChanged", "(Ljava/lang/String;)V");
	if (g_reactor_JNICALLBACK_state_changed_mid == NULL) return SEACATCC_RC_E_GENERIC;

	g_reactor_JNICALLBACK_clientid_changed_mid = (*env)->GetMethodID(env, g_clazz, "JNICallbackClientIdChanged", "(Ljava/lang/String;Ljava/lang/String;)V");
	if (g_reactor_JNICALLBACK_clientid_changed_mid == NULL) return SEACATCC_RC_E_GENERIC;

#ifdef __ANDROID__
	static const char * platform = "and";
#else
	static const char * platform = "jav";
#endif

	int rc = seacatcc_init(appIdChar, appIdSuffixChar, platform, varDirChar,
		JNICALLBACK_write_ready,
		JNICALLBACK_read_ready,
		JNICALLBACK_frame_received,
		JNICALLBACK_frame_return,
		JNICALLBACK_worker_request,
		JNICALLBACK_evloop_heartbeat
	);

	(*env)->ReleaseStringUTFChars(env, varDir, varDirChar);
	if (appIdSuffixChar != NULL) (*env)->ReleaseStringUTFChars(env, applicationIdSuffix, appIdSuffixChar);
	(*env)->ReleaseStringUTFChars(env, applicationId, appIdChar);

	if (rc != SEACATCC_RC_OK) return rc;

	// Register other hooks
	rc = seacatcc_hook_register('E', JNICALLBACK_evloop_started);
	assert(rc == SEACATCC_RC_OK);
	rc = seacatcc_hook_register('R', JNICALLBACK_gwconn_reset);
	assert(rc == SEACATCC_RC_OK);
	rc = seacatcc_hook_register('c', JNICALLBACK_gwconn_connected);
	assert(rc == SEACATCC_RC_OK);
	rc = seacatcc_hook_register('S', JNICALLBACK_state_changed);
	assert(rc == SEACATCC_RC_OK);
	rc = seacatcc_hook_register('i', JNICALLBACK_clientid_changed);
	assert(rc == SEACATCC_RC_OK);

	return rc;
}

////

static void JNICALLBACK_write_ready(void ** data, uint16_t * data_len)
{
	JNIEnv * g_env;
	assert(g_java_vm != NULL);
	assert(g_write_buffer_obj == NULL);

	int getEnvStat = (*g_java_vm)->GetEnv(g_java_vm, (void **)&g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if ((*g_java_vm)->AttachCurrentThread(g_java_vm, (JNINATIVEINTERFACEPTR) &g_env, NULL) != 0)
		{
			seacatcc_log('E', "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		seacatcc_log('E', "version not supported");
		return;
	}

	// Get buffer object
	jobject obj = (*g_env)->CallObjectMethod(g_env, g_reactor_obj, g_reactor_JNICALLBACK_write_ready_mid, NULL);
	if (obj != NULL)
	{
		g_write_buffer_obj = (*g_env)->NewGlobalRef(g_env, obj);
		assert(g_write_buffer_obj != NULL);

		(*g_env)->DeleteLocalRef(g_env, obj);
		obj = NULL;

		void * trg_data = (*g_env)->GetDirectBufferAddress(g_env, g_write_buffer_obj);
		jint pos = (*g_env)->CallIntMethod(g_env, g_write_buffer_obj, g_buffer_position_mid, NULL);
		jint limit = (*g_env)->CallIntMethod(g_env, g_write_buffer_obj, g_buffer_limit_mid, NULL);

		*data = trg_data + pos;
		*data_len = limit - pos;
	}

	if (getEnvStat == JNI_EDETACHED)
		(*g_java_vm)->DetachCurrentThread(g_java_vm);
}

static void JNICALLBACK_read_ready(void ** data, uint16_t * data_len)
{
	JNIEnv * g_env;
	assert(g_java_vm != NULL);
	assert(g_read_buffer_obj == NULL);

	int getEnvStat = (*g_java_vm)->GetEnv(g_java_vm, (void **)&g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if ((*g_java_vm)->AttachCurrentThread(g_java_vm, (JNINATIVEINTERFACEPTR) &g_env, NULL) != 0)
		{
			seacatcc_log('E', "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		seacatcc_log('E', "version not supported");
		return;
	}

	// Get buffer object
	jobject obj = (*g_env)->CallObjectMethod(g_env, g_reactor_obj, g_reactor_JNICALLBACK_read_ready_mid, NULL);
	if (obj != NULL)
	{
		g_read_buffer_obj = (*g_env)->NewGlobalRef(g_env, obj);
		assert(g_read_buffer_obj != NULL);
		
		(*g_env)->DeleteLocalRef(g_env, obj);
		obj = NULL;

		void * trg_data = (*g_env)->GetDirectBufferAddress(g_env, g_read_buffer_obj);
		jint pos = (*g_env)->CallIntMethod(g_env, g_read_buffer_obj, g_buffer_position_mid, NULL);
		jlong capacity = (*g_env)->GetDirectBufferCapacity(g_env, g_read_buffer_obj);

		//TODO: Position should be always 0 - check it
		*data = trg_data + pos;
		*data_len = capacity - pos;
	}

	else
	{
		seacatcc_log('E', "starvation");
	}

	if (getEnvStat == JNI_EDETACHED)
		(*g_java_vm)->DetachCurrentThread(g_java_vm);
}

static void JNICALLBACK_frame_received(void * data, uint16_t frame_len)
{
	JNIEnv * g_env;

	assert(g_java_vm != NULL);	
	assert(g_read_buffer_obj != NULL);

	int getEnvStat = (*g_java_vm)->GetEnv(g_java_vm, (void **)&g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if ((*g_java_vm)->AttachCurrentThread(g_java_vm, (JNINATIVEINTERFACEPTR) &g_env, NULL) != 0)
		{
			seacatcc_log('E', "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		seacatcc_log('E', "version not supported");
		return;
	}

	jint frame_len_jint = frame_len; // Be conservative, we deal with Java here
	(*g_env)->CallVoidMethod(g_env, g_reactor_obj, g_reactor_JNICALLBACK_frame_received_mid, g_read_buffer_obj, frame_len_jint, NULL);

	(*g_env)->DeleteGlobalRef(g_env, g_read_buffer_obj);
	g_read_buffer_obj = NULL;

	if (getEnvStat == JNI_EDETACHED)
		(*g_java_vm)->DetachCurrentThread(g_java_vm);

}

static void JNICALLBACK_frame_return(void * data)
{
	JNIEnv * g_env;
	assert(g_java_vm != NULL);

	int getEnvStat = (*g_java_vm)->GetEnv(g_java_vm, (void **)&g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if ((*g_java_vm)->AttachCurrentThread(g_java_vm, (JNINATIVEINTERFACEPTR) &g_env, NULL) != 0)
		{
			seacatcc_log('E', "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		seacatcc_log('E', "version not supported");
		return;
	}

	if ((g_read_buffer_obj != NULL) && ((*g_env)->GetDirectBufferAddress(g_env, g_read_buffer_obj) == data))
	{
		(*g_env)->CallVoidMethod(g_env, g_reactor_obj, g_reactor_JNICALLBACK_frame_return_mid, g_read_buffer_obj, NULL);
		(*g_env)->DeleteGlobalRef(g_env, g_read_buffer_obj);
		g_read_buffer_obj = NULL;
	}

	else if  ((g_write_buffer_obj != NULL) && ((*g_env)->GetDirectBufferAddress(g_env, g_write_buffer_obj) == data))
	{
		(*g_env)->CallVoidMethod(g_env, g_reactor_obj, g_reactor_JNICALLBACK_frame_return_mid, g_write_buffer_obj, NULL);
		(*g_env)->DeleteGlobalRef(g_env, g_write_buffer_obj);
		g_write_buffer_obj = NULL;
	}

	else
	{
		seacatcc_log('E', "unknown frame");
	}	


	if (getEnvStat == JNI_EDETACHED)
		(*g_java_vm)->DetachCurrentThread(g_java_vm);

}


static void JNICALLBACK_worker_request(char worker)
{
	JNIEnv * g_env;
	assert(g_java_vm != NULL);

	int getEnvStat = (*g_java_vm)->GetEnv(g_java_vm, (void **)&g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if ((*g_java_vm)->AttachCurrentThread(g_java_vm, (JNINATIVEINTERFACEPTR) &g_env, NULL) != 0)
		{
			seacatcc_log('E', "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		seacatcc_log('E', "version not supported");
		return;
	}

	jchar worker_jchar = worker;
	(*g_env)->CallVoidMethod(g_env, g_reactor_obj, g_reactor_JNICALLBACK_worker_request_mid, worker_jchar, NULL);

	if (getEnvStat == JNI_EDETACHED)
		(*g_java_vm)->DetachCurrentThread(g_java_vm);
}

////

double JNICALLBACK_evloop_heartbeat(double now)
{
	JNIEnv * g_env;
	assert(g_java_vm != NULL);

	int getEnvStat = (*g_java_vm)->GetEnv(g_java_vm, (void **)&g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if ((*g_java_vm)->AttachCurrentThread(g_java_vm, (JNINATIVEINTERFACEPTR) &g_env, NULL) != 0)
		{
			seacatcc_log('E', "AttachCurrentThread failed");
			return NAN;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		seacatcc_log('E', "version not supported");
		return NAN;
	}

	double ret = (*g_env)->CallDoubleMethod(g_env, g_reactor_obj, g_reactor_JNICALLBACK_evloop_heartbeat_mid, now, NULL);

	if (getEnvStat == JNI_EDETACHED)
		(*g_java_vm)->DetachCurrentThread(g_java_vm);

	return ret;
}

////

static void JNICALLBACK_evloop_started(void)
{
	JNIEnv * g_env;
	assert(g_java_vm != NULL);

	int getEnvStat = (*g_java_vm)->GetEnv(g_java_vm, (void **)&g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if ((*g_java_vm)->AttachCurrentThread(g_java_vm, (JNINATIVEINTERFACEPTR) &g_env, NULL) != 0)
		{
			seacatcc_log('E', "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		seacatcc_log('E', "version not supported");
		return;
	}

	(*g_env)->CallVoidMethod(g_env, g_reactor_obj, g_reactor_JNICALLBACK_evloop_started_mid, NULL);

	if (getEnvStat == JNI_EDETACHED)
		(*g_java_vm)->DetachCurrentThread(g_java_vm);
}


static void JNICALLBACK_gwconn_reset(void)
{
	JNIEnv * g_env;
	assert(g_java_vm != NULL);

	int getEnvStat = (*g_java_vm)->GetEnv(g_java_vm, (void **)&g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if ((*g_java_vm)->AttachCurrentThread(g_java_vm, (JNINATIVEINTERFACEPTR) &g_env, NULL) != 0)
		{
			seacatcc_log('E', "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		seacatcc_log('E', "version not supported");
		return;
	}

	(*g_env)->CallVoidMethod(g_env, g_reactor_obj, g_reactor_JNICALLBACK_gwconn_reset_mid, NULL);

	if (getEnvStat == JNI_EDETACHED)
		(*g_java_vm)->DetachCurrentThread(g_java_vm);
}

static void JNICALLBACK_gwconn_connected(void)
{
	JNIEnv * g_env;
	assert(g_java_vm != NULL);

	int getEnvStat = (*g_java_vm)->GetEnv(g_java_vm, (void **)&g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if ((*g_java_vm)->AttachCurrentThread(g_java_vm, (JNINATIVEINTERFACEPTR) &g_env, NULL) != 0)
		{
			seacatcc_log('E', "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		seacatcc_log('E', "version not supported");
		return;
	}

	(*g_env)->CallVoidMethod(g_env, g_reactor_obj, g_reactor_JNICALLBACK_gwconn_connected_mid, NULL);

	if (getEnvStat == JNI_EDETACHED)
		(*g_java_vm)->DetachCurrentThread(g_java_vm);
}

static void JNICALLBACK_state_changed(void)
{
	JNIEnv * g_env;
	assert(g_java_vm != NULL);

	int getEnvStat = (*g_java_vm)->GetEnv(g_java_vm, (void **)&g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if ((*g_java_vm)->AttachCurrentThread(g_java_vm, (JNINATIVEINTERFACEPTR) &g_env, NULL) != 0)
		{
			seacatcc_log('E', "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		seacatcc_log('E', "version not supported");
		return;
	}

	char state_buf[SEACATCC_STATE_BUF_SIZE];
	seacatcc_state(state_buf);

	jstring jstate_buf = (*g_env)->NewStringUTF(g_env, state_buf);
	(*g_env)->CallVoidMethod(g_env, g_reactor_obj, g_reactor_JNICALLBACK_state_changed_mid, jstate_buf, NULL);
	(*g_env)->DeleteLocalRef(g_env, jstate_buf);
	jstate_buf= NULL;

	if (getEnvStat == JNI_EDETACHED)
		(*g_java_vm)->DetachCurrentThread(g_java_vm);
}

static void JNICALLBACK_clientid_changed(void)
{
	JNIEnv * g_env;
	assert(g_java_vm != NULL);

	int getEnvStat = (*g_java_vm)->GetEnv(g_java_vm, (void **)&g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if ((*g_java_vm)->AttachCurrentThread(g_java_vm, (JNINATIVEINTERFACEPTR) &g_env, NULL) != 0)
		{
			seacatcc_log('E', "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		seacatcc_log('E', "version not supported");
		return;
	}

	jstring clientid_buf = (*g_env)->NewStringUTF(g_env, seacatcc_client_id());
	jstring clienttag_buf = (*g_env)->NewStringUTF(g_env, seacatcc_client_tag());
	(*g_env)->CallVoidMethod(g_env, g_reactor_obj, g_reactor_JNICALLBACK_clientid_changed_mid, clientid_buf, clienttag_buf, NULL);
	(*g_env)->DeleteLocalRef(g_env, clientid_buf);
	clientid_buf= NULL;
	(*g_env)->DeleteLocalRef(g_env, clienttag_buf);
	clienttag_buf= NULL;

	if (getEnvStat == JNI_EDETACHED)
		(*g_java_vm)->DetachCurrentThread(g_java_vm);	
}

////


JNIEXPORT jint JNICALL Java_com_teskalabs_seacat_android_client_core_seacatcc_run(JNIEnv * env, jclass cls)
{
	return seacatcc_run();
}


JNIEXPORT jint JNICALL Java_com_teskalabs_seacat_android_client_core_seacatcc_shutdown(JNIEnv * env, jclass cls)
{
	return seacatcc_shutdown();
}

JNIEXPORT jint JNICALL Java_com_teskalabs_seacat_android_client_core_seacatcc_yield(JNIEnv * env, jclass cls, jchar what)
{
	if (what > 0xFF) return SEACATCC_RC_E_GENERIC;
	return seacatcc_yield(what);
}

JNIEXPORT void JNICALL Java_com_teskalabs_seacat_android_client_core_seacatcc_ppkgen_1worker(JNIEnv * env, jclass cls)
{
	return seacatcc_ppkgen_worker();
}


JNIEXPORT jint JNICALL Java_com_teskalabs_seacat_android_client_core_seacatcc_csrgen_1worker(JNIEnv * env, jclass cls, jobjectArray params)
{

	int i, rc;
	int paramCount = (*env)->GetArrayLength(env, params);
	const char * csr_entries[paramCount+1];

	for (i=0; i<paramCount; i++)
	{
		jstring string = (jstring) (*env)->GetObjectArrayElement(env, params, i);
		csr_entries[i] = (*env)->GetStringUTFChars(env, string, 0);
	}
	csr_entries[paramCount] = NULL;

	rc = seacatcc_csrgen_worker(csr_entries);

	for (i=0; i<paramCount; i++)
	{
		jstring string = (jstring) (*env)->GetObjectArrayElement(env, params, i);
		(*env)->ReleaseStringUTFChars(env, string, csr_entries[i]);
	}

	return rc;
}


JNIEXPORT jdouble JNICALL Java_com_teskalabs_seacat_android_client_core_seacatcc_time(JNIEnv * env, jclass cls)
{
	return seacatcc_time();
}

JNIEXPORT jstring JNICALL Java_com_teskalabs_seacat_android_client_core_seacatcc_state(JNIEnv * env, jclass cls)
{
	char state_buf[SEACATCC_STATE_BUF_SIZE];
	seacatcc_state(state_buf);

	jstring result = (*env)->NewStringUTF(env, state_buf);
 	return result;
}

JNIEXPORT jstring JNICALL Java_com_teskalabs_seacat_android_client_core_seacatcc_client_1id(JNIEnv * env, jclass cls)
{
	jstring result = (*env)->NewStringUTF(env, seacatcc_client_id());
 	return result;
}

JNIEXPORT jstring JNICALL Java_com_teskalabs_seacat_android_client_core_seacatcc_client_1tag(JNIEnv * env, jclass cls)
{
	jstring result = (*env)->NewStringUTF(env, seacatcc_client_tag());
 	return result;
}

JNIEXPORT jint JNICALL Java_com_teskalabs_seacat_android_client_core_seacatcc_set_1proxy_1server_1worker(JNIEnv * env, jclass cls, jstring proxy_host, jstring proxy_port)
{
	const char * proxyHostChar = (*env)->GetStringUTFChars(env, proxy_host, 0);
	const char * proxyPortChar = (*env)->GetStringUTFChars(env, proxy_port, 0);

	int rc = seacatcc_set_proxy_server_worker(proxyHostChar, proxyPortChar);

	(*env)->ReleaseStringUTFChars(env, proxy_host, proxyHostChar);
	(*env)->ReleaseStringUTFChars(env, proxy_port, proxyPortChar);

	return rc;
}

JNIEXPORT jint JNICALL Java_com_teskalabs_seacat_android_client_core_seacatcc_log_1set_1mask(JNIEnv * env, jclass cls, jlong mask)
{
	union seacatcc_log_mask_u cc_mask = {.value = mask};
	return seacatcc_log_set_mask(cc_mask);
}

JNIEXPORT jint JNICALL Java_com_teskalabs_seacat_android_client_core_seacatcc_socket_1configure_1worker(JNIEnv * env, jclass cls, jint port, jchar domain, jchar sock_type, jint protocol, jstring peer_address, jstring peer_port)
{
	int domain_int = -1;
	switch (domain)
	{
		case 'u': domain_int = AF_UNIX; break;
		case '4': domain_int = AF_INET; break;
		case '6': domain_int = AF_INET6; break;
	};
	if (domain_int == -1)
	{
		seacatcc_log('E', "Unknown/invalid domain at socket_configure_worker: '%c'", domain);
		return SEACATCC_RC_E_INVALID_ARGS;
	}

	int sock_type_int = -1;
	switch (sock_type)
	{
		case 's': sock_type_int = SOCK_STREAM; break;
		case 'd': sock_type_int = SOCK_DGRAM; break;
	};
	if (sock_type_int == -1)
	{
		seacatcc_log('E', "Unknown/invalid type at socket_configure_worker: '%c'", sock_type);
		return SEACATCC_RC_E_INVALID_ARGS;
	}

	const char * peerAddressChar = (*env)->GetStringUTFChars(env, peer_address, 0);
	const char * peerPortChar = (*env)->GetStringUTFChars(env, peer_port, 0);
	int rc = seacatcc_socket_configure_worker(port, domain_int, sock_type_int, protocol, peerAddressChar, peerPortChar);
	(*env)->ReleaseStringUTFChars(env, peer_port, peerPortChar);
	(*env)->ReleaseStringUTFChars(env, peer_address, peerAddressChar);

	return rc;
}


JNIEXPORT jint JNICALL Java_com_teskalabs_seacat_android_client_core_seacatcc_characteristics_1store(JNIEnv * env, jclass cls, jobjectArray caparr)
{
	int capcnt = (*env)->GetArrayLength(env, caparr);

	const char * ccaparr[capcnt];
	jstring ccapstr[capcnt];

	for (int i=0; i<capcnt; i+=1)
	{
		ccapstr[i] = (jstring) ((*env)->GetObjectArrayElement(env, caparr, i));
		if (ccapstr[i] != NULL)
		{
			ccaparr[i] = (*env)->GetStringUTFChars(env, ccapstr[i], 0);
		}
		else
		{
			if (i != (capcnt-1)) seacatcc_log('E', "Received 'null' in a characteristics list");
			ccaparr[i] = NULL;
		}
    }

    int rc = seacatcc_characteristics_store(ccaparr);

	for (int i=0; i<capcnt; i+=1)
	{
		if (ccapstr[i] != NULL)
			(*env)->ReleaseStringUTFChars(env, ccapstr[i], ccaparr[i]);
    }

    return rc;
}
