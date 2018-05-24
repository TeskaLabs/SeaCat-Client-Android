package com.teskalabs.seacat.android.client.auth;

public class SeaCatUserNotAuthenticatedException extends Exception {

	public SeaCatUserNotAuthenticatedException() { super(); }

	public SeaCatUserNotAuthenticatedException(Throwable e) { super(e); }

	public SeaCatUserNotAuthenticatedException(String message) {
		super(message);
	}

	public SeaCatUserNotAuthenticatedException(String message, Throwable cause) { super(message, cause); }

}
