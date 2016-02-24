#ifndef _SEACATCC_H_
#define _SEACATCC_H_

#include <stddef.h>
#include <stdarg.h>
#include <stdint.h>

int seacatcc_init(
	const char * application_id,
	const char * application_id_suffix,
	const char * platform,
	const char * var_directory,
	void (* hook_write_ready)(void ** data, uint16_t * data_len),
	void (* hook_read_ready)(void ** data, uint16_t * data_len),
	void (* hook_frame_received)(void * data, uint16_t frame_len),
	void (* hook_frame_return)(void *data),
	void (* hook_worker_request)(char worker),
	double (* hook_evloop_heartbeat)(double now) // Return a interval (in seconds) after heartbeat should timeout (return Inf to disable)
);

int seacatcc_run(void);
int seacatcc_shutdown(void);

/*
Yieds:
	'd': Disconnect from gateway
	'r': Reset identity
	'n': Renew my certificate (aka trigger renewal process)
	'f': Recover from fatal state
	'W': We have data to send, call hook_write_ready ASAP
*/
int seacatcc_yield(char what);

const char * seacatcc_version(void);

/*
Workers:
	'C': CSR -> seacatcc_csrgen_worker()
	'P': PPK generator -> seacatcc_ppkgen_worker()
	'f': Recovery from fatal error worker, call seacatcc_yield('f') when done
	'n': Network not-reachable worker
	'R': OBSOLETED, NOT USED anylonger -> SeaCat CA download 
*/

// Workers (functions to be launched in 'other' thread than reactor)
void seacatcc_ppkgen_worker(void);
int seacatcc_csrgen_worker(const char * csr_entries[]);


// Hooks

/*
Hooks:
	'E': evloop_started
	'e': evloop_finished
	'R': gwconn_reset
	'C': gwconn_connected
	'S': state_changed
*/
typedef void (* seacatcc_hook)(void);
int seacatcc_hook_register(char code, seacatcc_hook hook);
int static inline seacatcc_hook_unregister(char code) { return seacatcc_hook_register(code, NULL); }


// Logging

/*
Log levels:
  'D': debug
  'I': info
  'W': warning
  'E': error
  'F': fatal
*/

void seacatcc_log(char level, const char * format, ...)  __attribute__ ((__format__ (__printf__, 2, 3)));
#define seacatcc_log_p(level, format, ...) seacatcc_log(level, "%s:%s:%d " format, __FILE__, __func__, __LINE__, ##__VA_ARGS__)

typedef void (* seacatcc_log_fnct)(char level, const char * message);
void seacatcc_log_setfnct(seacatcc_log_fnct log_fnct);


// State
#define SEACATCC_STATE_BUF_SIZE 7
void seacatcc_state(char * state_buf);


// Proxy
int seacatcc_set_proxy_server_worker(const char * proxy_host, const char * proxy_port);


// Misc
int seacatcc_network_reachable(void);
double seacatcc_time(void);


// Return codes

#define SEACATCC_RC_OK 0

#define SEACATCC_RC_W_ALREADY_INITIALIZED (-7901)
#define SEACATCC_RC_W_EVLOOP_NOT_RUNNING (7905)
#define SEACATCC_RC_W_WOULD_BLOCK (7906)
#define SEACATCC_RC_W_IGNORE (7910)

// -9000 ... 9399 are errno errors -> char * strerror(-(e-(SEACAT_RC_E_ERRNO_ERRORS)))
#define SEACATCC_RC_E_ERRNO_ERRORS (-9000)
#define SEACATCC_RC_E_ERRNO_GENERIC (-9399)

// -9600 ... 9799 are EIA errors -> const char * gai_strerror(-(e-(SEACAT_RC_E_EIA_ERRORS)))
#define SEACATCC_RC_E_EIA_ERRORS (-9600)
#define SEACATCC_RC_E_EIA_GENERIC (-9799)

// -9800 ... -9899 are OpenSSL errors -> char * ERR_error_string(-(e-(SEACAT_RC_E_SSL_ERRORS)), NULL);
#define SEACATCC_RC_E_SSL_ERRORS (-9800)
#define SEACATCC_RC_E_SSL_GENERIC (-9899)

#define SEACATCC_RC_E_INVALID_ARGS (-9901)
#define SEACATCC_RC_E_EVLOOP_ALREADY_RUNNING (-9902)
#define SEACATCC_RC_E_FRAME_TOO_SMALL (-9903)
#define SEACATCC_RC_E_INCORRECT_STATE (-9904)
#define SEACATCC_RC_E_NO_FRAMES (-9905)

#define SEACATCC_RC_E_DISCOVER_INVALID_SRV (-9921)
//Following error code means that there is no license (resp. related DNS SVR entry)
#define SEACATCC_RC_E_DISCOVER_NO_SRV (-9922)
#define SEACATCC_RC_E_DISCOVER_NO_AI (-9923)
#define SEACATCC_RC_E_DISCOVER_INVALID_REPLY (-9924)
#define SEACATCC_RC_E_DISCOVER_NO_NS (-9925)

#define SEACATCC_RC_E_SSL_GW_VERIFY_FAILED  (-9931)
#define SEACATCC_RC_E_PPK_NOT_FOUND (-9932)

#define SEACATCC_RC_E_MYCERT_NOT_FOUND (-9933)
#define SEACATCC_RC_E_MYCERT_INVALID (-9934)
#define SEACATCC_RC_E_MYCERT_NOT_VALID_YET (-9935)
#define SEACATCC_RC_E_MYCERT_EXPIRED (-9936)
#define SEACATCC_RC_E_CACERT_INVALID (-9937)

#define SEACATCC_RC_E_PERMA_INVALID (-9941)

#define SEACATCC_RC_E_PROXY_ERROR (-9951)

#define SEACATCC_RC_E_GENERIC (-9999)

#endif //_SEACATCC_H_
