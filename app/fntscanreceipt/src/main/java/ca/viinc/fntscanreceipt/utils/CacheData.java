package ca.viinc.fntscanreceipt.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ca.viinc.ereceiptpro.framework.BaseApplication;

/**
 * Created by Goutham on 20/05/19.
 */
public class CacheData {
    public static String CACHE_KEY = "cache_key_";
    public static String CACHE_TIME_KEY = "cache_timestamp_key_";
    public static final long MS_IN_HOUR = 1000 * 60 * 60;
    public static long MAX_AGE = MS_IN_HOUR * 1;

    public static void saveObjects(Context context, Object[] objectList) {
        SharedPreferences preferences;
        SharedPreferences.Editor editor;
        String simpleClassName = objectList.getClass().getSimpleName();
        preferences = context.getSharedPreferences(BaseApplication.PREF_FILE_CONFIG, Context.MODE_PRIVATE);
        editor = preferences.edit();

        Gson gson = new Gson();
        String objectJson = gson.toJson(objectList);
        editor.putString(CACHE_KEY + simpleClassName, objectJson);
        editor.putLong(CACHE_TIME_KEY + simpleClassName, System.currentTimeMillis());
        editor.commit();
    }

    public static void saveObject(Context context, Object objectList) {
        SharedPreferences preferences;
        SharedPreferences.Editor editor;
        String simpleClassName = objectList.getClass().getSimpleName();
        preferences = context.getSharedPreferences(BaseApplication.PREF_FILE_CONFIG, Context.MODE_PRIVATE);
        editor = preferences.edit();

        Gson gson = new Gson();
        String objectJson = gson.toJson(objectList);
        editor.putString(CACHE_KEY + simpleClassName, objectJson);
        editor.putLong(CACHE_TIME_KEY + simpleClassName, System.currentTimeMillis());
        editor.commit();
    }

    public static Object getSavedObject(Context context, Class className) {
        SharedPreferences settings;
        Object savedObject = null;
        settings = context.getSharedPreferences(BaseApplication.PREF_FILE_CONFIG,
                Context.MODE_PRIVATE);
        long saveTimeStamp = settings.getLong(CACHE_TIME_KEY + className.getSimpleName(), 0);
        if (System.currentTimeMillis() - saveTimeStamp > MAX_AGE) {
            Utils.Log(Log.DEBUG, "TAG", "Data Stale for classname " + className.getSimpleName());
            return null;
        }

        try {
            if (settings.contains(CACHE_KEY + className.getSimpleName())) {
                String jsonString = settings.getString(CACHE_KEY + className.getSimpleName(), null);
                Gson gson = new Gson();
                savedObject = (Object) (gson.fromJson(jsonString, className));
            }
        } catch (Exception e) {
            Utils.printStackTrace(e);
        }
        return savedObject;
    }

    public static Object[] getSavedObjects(Context context, Class className) {
        SharedPreferences settings;
        Object[] savedObjects = null;
        List<Object> objectList = null;
        settings = context.getSharedPreferences(BaseApplication.PREF_FILE_CONFIG,
                Context.MODE_PRIVATE);
        long saveTimeStamp = settings.getLong(CACHE_TIME_KEY + className.getSimpleName(), 0);
        if (System.currentTimeMillis() - saveTimeStamp > MAX_AGE) {
            Utils.Log(Log.DEBUG, "TAG", "Data Stale for classname " + className.getSimpleName());
            return null;
        }

        try {
            if (settings.contains(CACHE_KEY + className.getSimpleName())) {
                String jsonString = settings.getString(CACHE_KEY + className.getSimpleName(), null);
                Gson gson = new Gson();
                savedObjects = (Object[]) (gson.fromJson(jsonString, className));
                objectList = Arrays.asList(savedObjects);
                objectList = new ArrayList<>(objectList);
            }
        } catch (Exception e) {
            Utils.printStackTrace(e);
        }
        return savedObjects;
    }
}
