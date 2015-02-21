#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <assert.h>

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
static jmethodID g_reactor_JNICALLBACK_run_started_mid = 0;
static jmethodID g_reactor_JNICALLBACK_gwconn_reset_mid = 0;
static jmethodID g_reactor_JNICALLBACK_gwconn_connected_mid = 0;

static jobject g_write_buffer_obj = NULL;
static jobject g_read_buffer_obj = NULL;

///

static void JNICALLBACK_write_ready(void ** data, uint16_t * data_len);
static void JNICALLBACK_read_ready(void ** data, uint16_t * data_len);
static void JNICALLBACK_frame_received(void * data, uint16_t frame_len);
static void JNICALLBACK_frame_return(void * data);
static void JNICALLBACK_worker_request(char worker);

static void JNICALLBACK_run_started(void);
static void JNICALLBACK_gwconn_reset(void);
static void JNICALLBACK_gwconn_connected(void);

///

#ifdef __ANDROID__
#define JNINATIVEINTERFACEPTR const struct JNINativeInterface ***
#else
#define JNINATIVEINTERFACEPTR void **

#endif
///


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

	g_reactor_JNICALLBACK_run_started_mid = (*env)->GetMethodID(env, g_clazz, "JNICallbackRunStarted", "()V");
	if (g_reactor_JNICALLBACK_run_started_mid == NULL) return SEACATCC_RC_E_GENERIC;

	g_reactor_JNICALLBACK_gwconn_reset_mid = (*env)->GetMethodID(env, g_clazz, "JNICallbackGWConnReset", "()V");
	if (g_reactor_JNICALLBACK_gwconn_reset_mid == NULL) return SEACATCC_RC_E_GENERIC;

	g_reactor_JNICALLBACK_gwconn_connected_mid = (*env)->GetMethodID(env, g_clazz, "JNICallbackGWConnConnected", "()V");
	if (g_reactor_JNICALLBACK_gwconn_connected_mid == NULL) return SEACATCC_RC_E_GENERIC;


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
		JNICALLBACK_worker_request
	);

	(*env)->ReleaseStringUTFChars(env, varDir, varDirChar);
	(*env)->ReleaseStringUTFChars(env, applicationId, appIdChar);

	if (rc != SEACATCC_RC_OK) return rc;

	// Register other hooks
	rc = seacatcc_hook_register('E', JNICALLBACK_run_started);
	assert(rc == SEACATCC_RC_OK);
	rc = seacatcc_hook_register('R', JNICALLBACK_gwconn_reset);
	assert(rc == SEACATCC_RC_OK);
	rc = seacatcc_hook_register('C', JNICALLBACK_gwconn_connected);
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
		assert(obj != NULL);

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

static void JNICALLBACK_run_started(void)
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

	(*g_env)->CallVoidMethod(g_env, g_reactor_obj, g_reactor_JNICALLBACK_run_started_mid, NULL);

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

JNIEXPORT jint JNICALL Java_mobi_seacat_client_core_seacatcc_reset(JNIEnv * env, jclass cls)
{
	return seacatcc_reset();
}


JNIEXPORT void JNICALL Java_mobi_seacat_client_core_seacatcc_ppkgen_1worker(JNIEnv * env, jclass cls)
{
	return seacatcc_ppkgen_worker();
}


JNIEXPORT void JNICALL Java_mobi_seacat_client_core_seacatcc_csrgen_1worker(JNIEnv * env, jclass cls, 
	jstring country, jstring state, jstring locality, jstring organization, jstring organization_unit,
	jstring common_name,
	jstring surname, jstring given_name,
	jstring email,
	jstring san_email, jstring san_uri)
{

	const char * countryChar = country != NULL ? (*env)->GetStringUTFChars(env, country, 0) : NULL;
	const char * stateChar = (*env)->GetStringUTFChars(env, state, 0);
	const char * localityChar = (*env)->GetStringUTFChars(env, locality, 0);
	const char * organizationChar = (*env)->GetStringUTFChars(env, organization, 0);
	const char * organizationUnitChar = (*env)->GetStringUTFChars(env, organization_unit, 0);
	const char * commonNameChar = (*env)->GetStringUTFChars(env, common_name, 0);
	const char * surnameChar = (*env)->GetStringUTFChars(env, surname, 0);
	const char * givenNameChar = (*env)->GetStringUTFChars(env, given_name, 0);
	const char * emailChar = (*env)->GetStringUTFChars(env, email, 0);
	const char * sanEmailChar = san_email != NULL ? (*env)->GetStringUTFChars(env, san_email, 0) : NULL;
	const char * sanUriChar = san_uri != NULL ? (*env)->GetStringUTFChars(env, san_uri, 0) : NULL;

	seacatcc_csrgen_worker(
		countryChar, stateChar, localityChar, organizationChar, organizationUnitChar,
		commonNameChar,
		surnameChar, givenNameChar,
		emailChar,
		sanEmailChar, sanUriChar
	);

	if (sanUriChar != NULL) (*env)->ReleaseStringUTFChars(env, san_uri, sanUriChar);
	if (sanEmailChar != NULL) (*env)->ReleaseStringUTFChars(env, san_email, sanEmailChar);
	if (emailChar != NULL) (*env)->ReleaseStringUTFChars(env, email, emailChar);
	if (givenNameChar != NULL) (*env)->ReleaseStringUTFChars(env, given_name, givenNameChar);
	if (surnameChar != NULL) (*env)->ReleaseStringUTFChars(env, surname, surnameChar);
	if (commonNameChar != NULL) (*env)->ReleaseStringUTFChars(env, common_name, commonNameChar);
	if (organizationUnitChar != NULL) (*env)->ReleaseStringUTFChars(env, organization_unit, organizationUnitChar);
	if (organizationChar != NULL) (*env)->ReleaseStringUTFChars(env, organization, organizationChar);
	if (localityChar != NULL) (*env)->ReleaseStringUTFChars(env, locality, localityChar);
	if (stateChar != NULL) (*env)->ReleaseStringUTFChars(env, state, stateChar);
	if (countryChar != NULL) (*env)->ReleaseStringUTFChars(env, country, countryChar);
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
