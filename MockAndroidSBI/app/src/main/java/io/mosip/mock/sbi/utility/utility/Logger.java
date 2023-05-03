package io.mosip.mock.sbi.utility.utility;

import android.util.Log;

/**
 * @author NPrime Technologies
 */

public class Logger {

    private static final int LOG_ASSERT = 1;
    private static final int LOG_ERROR = 2;
    private static final int LOG_WARN = 3;
    private static final int LOG_INFO = 4;
    private static final int LOG_DEBUG = 5;

    private static final int LOG_LEVEL = LOG_INFO;

    public static void d(String logTag, String message)
    {
        if (LOG_LEVEL >= LOG_DEBUG)
        {
            Log.d(logTag, message);
        }
    }

    public static void i(String logTag, String message)
    {
        if (LOG_LEVEL >= LOG_INFO)
        {
            Log.i(logTag, message);
        }
    }
    public static void w(String logTag, String message)
    {
        if (LOG_LEVEL >= LOG_WARN)
        {
            Log.w(logTag, message);
        }
    }
    public static void e(String logTag, String message)
    {
        if (LOG_LEVEL >= LOG_ERROR)
        {
            Log.e(logTag, message);
        }
    }

    public static void v(String logTag, String message)
    {
        if (LOG_LEVEL > LOG_DEBUG)
        {
            Log.v(logTag, message);
        }
    }
}
