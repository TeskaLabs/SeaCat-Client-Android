package com.teskalabs.seacat.android.client;

import android.content.Intent;

/**
 * This class is for internal SeaCat client use only.
 * It is not  part of public API.
 */
public class SeaCatInternals
{
    final public static String L = "SeaCat";

    final public static String SeaCatPreferences = "seacat_preferences";

    final static public Intent createIntent(String action)
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

    public static String applicationIdSuffix = null;

    public static boolean logDebug = false;
}
