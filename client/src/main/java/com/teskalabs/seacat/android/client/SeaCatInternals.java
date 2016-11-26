package com.teskalabs.seacat.android.client;

import android.content.Intent;

/**
 * This class is for internal SeaCat client use only.
 * It is not  part of public API.
 */
public class SeaCatInternals
{
    public final static String L = "SeaCat";
    public final static String SeaCatHostSuffix = ".seacat";
    public final static String SeaCatPreferences = "seacat_preferences";

    private static Runnable CSRWorker = null;
    public static String applicationIdSuffix = null;
    public static boolean logDebug = false;

    public static Intent createIntent(String action)
    {
        Intent Intent = new Intent(action);
        Intent.addCategory(SeaCatClient.CATEGORY_SEACAT);
        Intent.addFlags(android.content.Intent.FLAG_FROM_BACKGROUND);
        return Intent;
    }

    public static void setCSRWorker(Runnable csrWorker)
    {
        CSRWorker = csrWorker;
    }
    public static Runnable getCSRWorker()
    {
        return CSRWorker;
    }

}
