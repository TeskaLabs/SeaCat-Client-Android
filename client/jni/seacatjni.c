#include <jni.h>
#include <stdio.h>
#include <assert.h>
#include <seacatclcc.h>
#include "mobi_seacat_client_internal_JNI.h"

///

static JavaVM * g_java_vm = NULL;

static jmethodID g_buffer_limit_mid = NULL;
static jmethodID g_buffer_position_mid = NULL;
static jmethodID g_buffer_set_position_mid = NULL;

static jobject g_reactor_obj = NULL;
static jmethodID g_reactor_JNICALLBACK_write_ready_mid = NULL;
static jmethodID g_reactor_JNICALLBACK_read_ready_mid = NULL;
static jmethodID g_reactor_JNICALLBACK_frame_sent_mid = NULL;
static jmethodID g_reactor_JNICALLBACK_frame_received_mid = NULL;
static jmethodID g_reactor_JNICALLBACK_frame_stalled_mid = NULL;

static jobject g_write_buffer_obj = NULL;
static jobject g_read_buffer_obj = NULL;

///

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
	JNIEnv* env;

	g_java_vm = vm;

	int rc = seacat_init();
	if (rc != SEACAT_RC_OK) return -1;

	if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK)
	{
		return -1;
	}

	jclass cls = (*env)->FindClass(env, "java/nio/Buffer");
	
	g_buffer_limit_mid = (*env)->GetMethodID(env, cls, "limit", "()I");
	if (g_buffer_limit_mid == NULL) return -1;
	
	g_buffer_position_mid = (*env)->GetMethodID(env, cls, "position", "()I");
	if (g_buffer_position_mid == NULL) return -1;

	g_buffer_set_position_mid = (*env)->GetMethodID(env, cls, "position", "(I)Ljava/nio/Buffer;");
	if (g_buffer_set_position_mid == NULL) return -1;

	return JNI_VERSION_1_6;
}

////

static void JNICALLBACK_write_ready(void ** data, size_t * data_len)
{
	JNIEnv * g_env;
	assert(g_java_vm != NULL);
	assert(g_write_buffer_obj == NULL);

	int getEnvStat = (*g_java_vm)->GetEnv(g_java_vm, (void **)&g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if ((*g_java_vm)->AttachCurrentThread(g_java_vm, (void **) &g_env, NULL) != 0)
			return;
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		printf("GetEnv: version not supported\n");
		return;
	}

	// Get buffer object
	jobject obj = (*g_env)->CallObjectMethod(g_env, g_reactor_obj, g_reactor_JNICALLBACK_write_ready_mid);
	if (obj != NULL)
	{
		g_write_buffer_obj  = (*g_env)->NewGlobalRef(g_env, obj);

		void * trg_data = (*g_env)->GetDirectBufferAddress(g_env, g_write_buffer_obj);
		jint pos = (*g_env)->CallIntMethod(g_env, g_write_buffer_obj, g_buffer_position_mid, NULL);
		jint limit = (*g_env)->CallIntMethod(g_env, g_write_buffer_obj, g_buffer_limit_mid, NULL);

		*data = trg_data + pos;
		*data_len = limit - pos;
	}

	if (getEnvStat == JNI_EDETACHED)
		(*g_java_vm)->DetachCurrentThread(g_java_vm);
}

static void JNICALLBACK_read_ready(void ** data, size_t * data_len)
{
	JNIEnv * g_env;
	assert(g_java_vm != NULL);
	assert(g_read_buffer_obj == NULL);

	int getEnvStat = (*g_java_vm)->GetEnv(g_java_vm, (void **)&g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if ((*g_java_vm)->AttachCurrentThread(g_java_vm, (void **) &g_env, NULL) != 0)
			return;
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		printf("GetEnv: version not supported\n");
		return;
	}

	// Get buffer object
	jobject obj = (*g_env)->CallObjectMethod(g_env, g_reactor_obj, g_reactor_JNICALLBACK_read_ready_mid);
	if (obj != NULL)
	{
		g_read_buffer_obj  = (*g_env)->NewGlobalRef(g_env, obj);

		void * trg_data = (*g_env)->GetDirectBufferAddress(g_env, g_read_buffer_obj);
		jint pos = (*g_env)->CallIntMethod(g_env, g_read_buffer_obj, g_buffer_position_mid, NULL);
		jlong capacity = (*g_env)->GetDirectBufferCapacity(g_env, g_read_buffer_obj);

		*data = trg_data + pos;
		*data_len = capacity - pos;
	}

	else
	{
		printf("Starvation in JNICallbackReadReady()!\n");
	}

	if (getEnvStat == JNI_EDETACHED)
		(*g_java_vm)->DetachCurrentThread(g_java_vm);
}

static void JNICALLBACK_frame_sent(void * data)
{
	JNIEnv * g_env;
	assert(g_java_vm != NULL);
	assert(g_write_buffer_obj != NULL);

	int getEnvStat = (*g_java_vm)->GetEnv(g_java_vm, (void **)&g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if ((*g_java_vm)->AttachCurrentThread(g_java_vm, (void **) &g_env, NULL) != 0)
			return;
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		printf("GetEnv: version not supported\n");
		return;
	}

	(*g_env)->CallVoidMethod(g_env, g_reactor_obj, g_reactor_JNICALLBACK_frame_sent_mid, g_write_buffer_obj);

	(*g_env)->DeleteGlobalRef(g_env, g_write_buffer_obj);
	g_write_buffer_obj = NULL;

	if (getEnvStat == JNI_EDETACHED)
		(*g_java_vm)->DetachCurrentThread(g_java_vm);
}

static void JNICALLBACK_frame_received(void * data, size_t frame_len)
{
	JNIEnv * g_env;

	assert(g_java_vm != NULL);	
	assert(g_read_buffer_obj != NULL);

	int getEnvStat = (*g_java_vm)->GetEnv(g_java_vm, (void **)&g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if ((*g_java_vm)->AttachCurrentThread(g_java_vm, (void **) &g_env, NULL) != 0)
			return;
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		printf("GetEnv: version not supported\n");
		return;
	}

	jint pos = (*g_env)->CallIntMethod(g_env, g_read_buffer_obj, g_buffer_position_mid, NULL);
	(*g_env)->CallObjectMethod(g_env, g_read_buffer_obj, g_buffer_set_position_mid, pos + frame_len);
	(*g_env)->CallVoidMethod(g_env, g_reactor_obj, g_reactor_JNICALLBACK_frame_received_mid, g_read_buffer_obj);

	(*g_env)->DeleteGlobalRef(g_env, g_read_buffer_obj);
	g_read_buffer_obj = NULL;

	if (getEnvStat == JNI_EDETACHED)
		(*g_java_vm)->DetachCurrentThread(g_java_vm);

}


////

JNIEXPORT jint JNICALL Java_mobi_seacat_client_internal_JNI_seacat_1reactor_1init(JNIEnv * env, jclass cls, jobject obj)
{
	assert(g_reactor_obj == NULL);

	// convert local to global reference (local will die after this method call)
	g_reactor_obj = (*env)->NewGlobalRef(env, obj);

	// save refs for callback
	jclass g_clazz = (*env)->GetObjectClass(env, g_reactor_obj);
	if (g_clazz == NULL) return SEACAT_RC_E_GENERIC;

	g_reactor_JNICALLBACK_write_ready_mid = (*env)->GetMethodID(env, g_clazz, "JNICallbackWriteReady", "()Ljava/nio/ByteBuffer;");
	if (g_reactor_JNICALLBACK_write_ready_mid == NULL) return SEACAT_RC_E_GENERIC;

	g_reactor_JNICALLBACK_read_ready_mid = (*env)->GetMethodID(env, g_clazz, "JNICallbackReadReady", "()Ljava/nio/ByteBuffer;");
	if (g_reactor_JNICALLBACK_read_ready_mid == NULL) return SEACAT_RC_E_GENERIC;

	g_reactor_JNICALLBACK_frame_sent_mid = (*env)->GetMethodID(env, g_clazz, "JNICallbackFrameSent", "(Ljava/nio/ByteBuffer;)V");
	if (g_reactor_JNICALLBACK_frame_sent_mid == NULL) return SEACAT_RC_E_GENERIC;

	g_reactor_JNICALLBACK_frame_received_mid = (*env)->GetMethodID(env, g_clazz, "JNICallbackFrameReceived", "(Ljava/nio/ByteBuffer;)V");
	if (g_reactor_JNICALLBACK_frame_received_mid == NULL) return SEACAT_RC_E_GENERIC;

	g_reactor_JNICALLBACK_frame_stalled_mid = (*env)->GetMethodID(env, g_clazz, "JNICallbackFrameStalled", "(Ljava/nio/ByteBuffer;)V");
	if (g_reactor_JNICALLBACK_frame_stalled_mid == NULL) return SEACAT_RC_E_GENERIC;

	struct seacat_reactor_descr srd = 
	{
		.write_ready = JNICALLBACK_write_ready,
		.read_ready = JNICALLBACK_read_ready,
		.frame_sent = JNICALLBACK_frame_sent,
		.frame_received = JNICALLBACK_frame_received
	};

	return seacat_reactor_init(&srd);
}

JNIEXPORT jint JNICALL Java_mobi_seacat_client_internal_JNI_seacat_1reactor_1fini(JNIEnv * env, jclass cls)
{
	int rc;

	rc = seacat_reactor_fini();
	
	(*env)->DeleteGlobalRef(env, g_reactor_obj);
	g_reactor_obj = NULL;

	return rc;
}

JNIEXPORT jint JNICALL Java_mobi_seacat_client_internal_JNI_seacat_1reactor_1run(JNIEnv * env, jclass cls)
{
	int rc;
	rc = seacat_reactor_run();

	// Return stalled read buffer
	if (g_read_buffer_obj != NULL)
	{
		(*env)->CallVoidMethod(env, g_reactor_obj, g_reactor_JNICALLBACK_frame_stalled_mid, g_read_buffer_obj);
		(*env)->DeleteGlobalRef(env, g_read_buffer_obj);
		g_read_buffer_obj = NULL;
	}

	// Return stalled write buffer
	if (g_write_buffer_obj != NULL)
	{
		(*env)->CallVoidMethod(env, g_reactor_obj, g_reactor_JNICALLBACK_frame_stalled_mid, g_write_buffer_obj);
		(*env)->DeleteGlobalRef(env, g_write_buffer_obj);
		g_write_buffer_obj = NULL;
	}

	return rc;
}

JNIEXPORT jint JNICALL Java_mobi_seacat_client_internal_JNI_seacat_1reactor_1shutdown(JNIEnv * env, jclass cls)
{
	return seacat_reactor_shutdown();
}

JNIEXPORT jint JNICALL Java_mobi_seacat_client_internal_JNI_seacat_1reactor_1yield(JNIEnv * env, jclass cls)
{
	return seacat_reactor_yield();
}
