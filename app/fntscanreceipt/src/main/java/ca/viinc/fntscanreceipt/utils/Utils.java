package ca.viinc.fntscanreceipt.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.util.Base64;
import android.widget.EditText;

import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.TimeoutError;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;


import ca.viinc.fntscanreceipt.BaseApplication;
import ca.viinc.fntscanreceipt.R;

/**
 * Created by root on 20/05/19.
 */
public class Utils {

    //Font work
    public static String regular_font_path = "font/OpenSans-Regular.ttf";
    public static String bold_font_path = "font/OpenSans-Bold.ttf";
    public static String semi_bold_font_path = "font/OpenSans-Semibold.ttf";

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static Typeface boldfont(Context ctx) {
        Typeface tf = Typeface.createFromAsset(ctx.getAssets(), bold_font_path);
        return tf;
    }

    public static Typeface regulerfont(Context ctx) {
        Typeface tf = Typeface.createFromAsset(ctx.getAssets(), regular_font_path);
        return tf;
    }


    public static Typeface semiboldfont(Context ctx) {
        Typeface tf = Typeface.createFromAsset(ctx.getAssets(), semi_bold_font_path);
        return tf;
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    public static boolean isValidEmailAddress(String emailID) {
        return (emailID != null && android.util.Patterns.EMAIL_ADDRESS.matcher(emailID).matches());
    }

    public static void setError(EditText editText, String errorMessage) {
        if (editText != null && errorMessage != null) {
            editText.setError(errorMessage);
        }
    }

    public static boolean isNetworkProblem(Object error) {
        return (error instanceof NetworkError) || (error instanceof NoConnectionError);
    }

    public static boolean isTimeoutError(Object error) {
        return (error instanceof TimeoutError);
    }


    public static String decodeBase64String(String base64String) {
        byte[] data = Base64.decode(base64String, Base64.DEFAULT);
        String text = null;
        try {
            text = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Utils.printStackTrace(e);
        }
        return text;
    }

    public static String toTitleCase(String inputText) {
        String[] words = inputText.split(" ");
        StringBuilder sb = new StringBuilder();
        if (words[0].length() > 0) {
            sb.append(Character.toUpperCase(words[0].charAt(0)) + words[0].subSequence(1, words[0].length()).toString().toLowerCase());
            for (int i = 1; i < words.length; i++) {
                if (words[i].length() == 1) {
                    sb.append(" ");
                    sb.append(Character.toUpperCase(words[i].charAt(0)));
                } else if (words[i].length() > 0) {
                    sb.append(" ");
                    sb.append(Character.toUpperCase(words[i].charAt(0)) + words[i].subSequence(1, words[i].length()).toString().toLowerCase());
                }

            }
        }
        String titleCaseValue = sb.toString();
        return titleCaseValue;
    }

    public static double round(double value, int places) { //formats double value (arg1: #value) to a given number of decimal places (arg: #places)
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static String formatDec(Double input) {
        String output = "";
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        output = nf.format(input);
        return output;
    }

    public static String formatDec(Float input) {
        return formatDec(new Double(input));
    }

    public static String formatDec(String input) {
        return formatDec(Double.parseDouble(input));
    }


    public static String getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    public static String getAppName(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    public static SharedPreferences getAppPreferences(Context context) {

        return context.getSharedPreferences(BaseApplication.PREF_FILE_CONFIG,
                Context.MODE_PRIVATE);
    }

    public static void Log(int priority, String tag, String message) {
        /*if (GlobalConstants.debugLogs) {
            if (GlobalConstants.CrashlyticsLogs) Crashlytics.log(priority, tag, message);
        } else {
            if (GlobalConstants.CrashlyticsLogs) Crashlytics.log(message);
        }*/
    }

    public static void printStackTrace(Exception e, boolean log) {
       /* if (log) {
            if (GlobalConstants.CrashlyticsLogs) Crashlytics.logException(e);
        }
        e.printStackTrace();*/
    }

    public static void printStackTrace(Exception e) {
        printStackTrace(e, false);
    }

    public static void printStackTrace(String customMessage) {
        printStackTrace(new GenericReportingException(customMessage), true);
    }


    public static class GenericReportingException extends Exception {
        public GenericReportingException() {
        }

        public GenericReportingException(String str) {
            super(str);
        }
    }

    public static DisplayImageOptions getDefaultDisplayOptions(Context context) {
        DisplayImageOptions options = new DisplayImageOptions.Builder().cacheInMemory(true)
                .resetViewBeforeLoading(true)
                .showImageForEmptyUri(context.getResources().getDrawable(R.mipmap.ic_pdf))
                .showImageOnFail(context.getResources().getDrawable(R.mipmap.ic_txt))
                .showImageOnLoading(context.getResources().getDrawable(R.mipmap.ic_jpg))
                .cacheOnDisk(true)
                .build();

        return options;
    }
    public static DisplayImageOptions getDefaultDisplayOptionsForTiff(Context context) {
        DisplayImageOptions options = new DisplayImageOptions.Builder().cacheInMemory(true)
                .resetViewBeforeLoading(true)
                .showImageOnLoading(context.getResources().getDrawable(R.mipmap.ic_jpg))
                .cacheOnDisk(true)
                .build();

        return options;
    }
}
