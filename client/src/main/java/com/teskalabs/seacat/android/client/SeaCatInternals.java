package com.teskalabs.seacat.android.client;

import android.content.Intent;

import com.teskalabs.seacat.android.client.auth.KeyStoreAuth;

import java.security.Key;

/**
 * This class is for internal SeaCat client use only.
 * It is not  part of public API.
 */
public class SeaCatInternals
{
    public final static String L = "SeaCat";
    public final static String SeaCatHostSuffix = ".seacat";
    public final static String SeaCatPreferences = "seacat_preferences";

    public static String applicationIdSuffix = null;
    public static boolean logDebug = false;

    public static Intent createIntent(String action)
    {
        Intent Intent = new Intent(action);
        Intent.addCategory(SeaCatClient.CATEGORY_SEACAT);
        Intent.addFlags(android.content.Intent.FLAG_FROM_BACKGROUND);
        return Intent;
    }

    private static Runnable CSRWorker = null;
    public static void setCSRWorker(Runnable csrWorker)
    {
        CSRWorker = csrWorker;
    }
    public static Runnable getCSRWorker()
    {
        return CSRWorker;
    }

    private static KeyStoreAuth auth = null;
    public static void setAuth(KeyStoreAuth in_auth)
    {
        auth = in_auth;
    }
    public static KeyStoreAuth getAuth()
    {
        if (auth == null) auth = new KeyStoreAuth();
        return auth;
    }

}
