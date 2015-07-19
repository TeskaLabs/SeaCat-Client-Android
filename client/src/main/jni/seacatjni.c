#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <math.h>

#include <seacatcc.h>

#include "mobi_seacat_client_core_seacatcc.h"

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

JNIEXPORT jint JNICALL Java_mobi_seacat_client_core_seacatcc_init(JNIEnv * env, jclass cls, jstring applicationId, jstring varDir, jobject reactor)
{
	assert(g_reactor_obj == NULL);

	const char * appIdChar = (*env)->GetStringUTFChars(env, applicationId, 0);
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

#ifdef __ANDROID__
	static const char * platform = "and";
#else
	static const char * platform = "jav";
#endif

	int rc = seacatcc_init(appIdChar, platform, varDirChar,
		JNICALLBACK_write_ready,
		JNICALLBACK_read_ready,
		JNICALLBACK_frame_received,
		JNICALLBACK_frame_return,
		JNICALLBACK_worker_request,
		JNICALLBACK_evloop_heartbeat
	);

	(*env)->ReleaseStringUTFChars(env, varDir, varDirChar);
	(*env)->ReleaseStringUTFChars(env, applicationId, appIdChar);

	if (rc != SEACATCC_RC_OK) return rc;

	// Register other hooks
	rc = seacatcc_hook_register('E', JNICALLBACK_evloop_started);
	assert(rc == SEACATCC_RC_OK);
	rc = seacatcc_hook_register('R', JNICALLBACK_gwconn_reset);
	assert(rc == SEACATCC_RC_OK);
	rc = seacatcc_hook_register('C', JNICALLBACK_gwconn_connected);
	assert(rc == SEACATCC_RC_OK);
	rc = seacatcc_hook_register('S', JNICALLBACK_state_changed);
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
			seacatcc_log_p('E', "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		seacatcc_log_p('E', "version not supported");
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
			seacatcc_log_p('E', "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		seacatcc_log_p('E', "version not supported");
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

		*data = trg_data + pos;
		*data_len = capacity - pos;
	}

	else
	{
		seacatcc_log_p('E', "starvation");
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
			seacatcc_log_p('E', "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		seacatcc_log_p('E', "version not supported");
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
			seacatcc_log_p('E', "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		seacatcc_log_p('E', "version not supported");
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
		seacatcc_log_p('E', "unknown frame");
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
			seacatcc_log_p('E', "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		seacatcc_log_p('E', "version not supported");
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
			seacatcc_log_p('E', "AttachCurrentThread failed");
			return NAN;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		seacatcc_log_p('E', "version not supported");
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
			seacatcc_log_p('E', "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		seacatcc_log_p('E', "version not supported");
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
			seacatcc_log_p('E', "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		seacatcc_log_p('E', "version not supported");
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
			seacatcc_log_p('E', "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		seacatcc_log_p('E', "version not supported");
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
			seacatcc_log_p('E', "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		seacatcc_log_p('E', "version not supported");
		return;
	}

	char state_buf[SEACATCC_STATE_BUF_SIZE];
	seacatcc_get_state(state_buf);

	jstring jstate_buf = (*g_env)->NewStringUTF(g_env, state_buf);
	(*g_env)->CallVoidMethod(g_env, g_reactor_obj, g_reactor_JNICALLBACK_state_changed_mid, jstate_buf, NULL);

	if (getEnvStat == JNI_EDETACHED)
		(*g_java_vm)->DetachCurrentThread(g_java_vm);
}

////


JNIEXPORT jint JNICALL Java_mobi_seacat_client_core_seacatcc_run(JNIEnv * env, jclass cls)
{
	return seacatcc_run();
}


JNIEXPORT jint JNICALL Java_mobi_seacat_client_core_seacatcc_shutdown(JNIEnv * env, jclass cls)
{
	return seacatcc_shutdown();
}

JNIEXPORT jint JNICALL Java_mobi_seacat_client_core_seacatcc_yield(JNIEnv * env, jclass cls, jchar what)
{
	if (what > 0xFF) return SEACATCC_RC_E_GENERIC;
	return seacatcc_yield(what);
}

JNIEXPORT void JNICALL Java_mobi_seacat_client_core_seacatcc_ppkgen_1worker(JNIEnv * env, jclass cls)
{
	return seacatcc_ppkgen_worker();
}


JNIEXPORT jint JNICALL Java_mobi_seacat_client_core_seacatcc_csrgen_1worker(JNIEnv * env, jclass cls, jobjectArray params)
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

	rc = seacatcc_csrgen_worker((char * const*)csr_entries);

	for (i=0; i<paramCount; i++)
	{
		jstring string = (jstring) (*env)->GetObjectArrayElement(env, params, i);
		(*env)->ReleaseStringUTFChars(env, string, csr_entries[i]);
	}

	return rc;
}


JNIEXPORT jstring JNICALL Java_mobi_seacat_client_core_seacatcc_cacert_1url(JNIEnv * env, jclass cls)
{
	jstring result = (*env)->NewStringUTF(env, seacatcc_cacert_url()); 
 	return result;
}

JNIEXPORT void JNICALL Java_mobi_seacat_client_core_seacatcc_cacert_1worker(JNIEnv * env, jclass cls, jbyteArray cert)
{
	int len = (*env)->GetArrayLength(env, cert);
    jbyte *ptr = (jbyte *)(*env)->GetByteArrayElements(env, cert, NULL);

    seacatcc_cacert_worker((const char *)ptr, len);

    (*env)->ReleaseByteArrayElements(env, cert, ptr, 0);
}

JNIEXPORT jdouble JNICALL Java_mobi_seacat_client_core_seacatcc_time(JNIEnv * env, jclass cls)
{
	return seacatcc_time();
}
