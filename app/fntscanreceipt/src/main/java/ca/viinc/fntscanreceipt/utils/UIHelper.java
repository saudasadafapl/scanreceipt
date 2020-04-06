package ca.viinc.fntscanreceipt.utils;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import ca.viinc.fntscanreceipt.BaseActivity;


public class UIHelper {
    public static void hideKeyBoard(Context context){
        InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (((BaseActivity)context).getCurrentFocus()!=null)
            inputManager.hideSoftInputFromWindow(((BaseActivity)context).getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

    }
    public static void hideKeyboardFrom(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
