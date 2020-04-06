package ca.viinc.fntscanreceipt;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;

import ca.viinc.fntscanreceipt.utils.Utils;


/**
 * Created by Goutham Iyyappan on 20/05/2019 for Apeirogon Labs Pvt Ltd, India
 * Base Activity structure that is inherited by other activities of the project.
 */
public class BaseActivity extends AppCompatActivity implements MobileAPI.UBarberCallbacks {

    public static boolean debugLogs = true;
    private Toast activityToast = null;
    protected boolean checkedPermOnce = false;
    public static final int REQUEST_FINE_LOCATION = 0;
    private static final int REQUEST_CHECK_SETTINGS = 214;


    protected Context mContext;
    protected NotificationManager mNotificationManager;
    protected String CLASSNAME;
    protected MobileAPI mobileAPI;
    protected ProgressDialog apiProgressDialog = null;
    protected boolean enableAPIProgressDialog = true;
    protected boolean listenForUserChanges = false;
    private Location mLocation;
    private static final String TAG = BaseActivity.class.getSimpleName();

    public static final String KEY_REMIND = "Ocr";
    public static final String KEY_DATE = "date";

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CLASSNAME = this.getClass().getSimpleName();
        setStatusBarColorTransperant();
        mContext = this;
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mobileAPI = new MobileAPI(this, this);
        apiProgressDialog = new ProgressDialog(this);
        mLocation = new Location("Default");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    public void statusBarColor() {
        Window window = getWindow();
        if (window != null) {
            // clear FLAG_TRANSLUCENT_STATUS flag:
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            // finally change the color
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent context in AndroidManifest.xml.
        int id = item.getItemId();

        //TODO : had commented this
        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    public InputFilter mFilter = new InputFilter() {
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            for (int i = start; i < end; i++) {
                if (!Character.isLetterOrDigit(source.charAt(i))) {
                    return "";
                }
            }
            return null;
        }
    };

    public static HashMap<String, String> getRequestHeaders() {
        HashMap<String, String> requestHeader = new HashMap<String, String>();
        String requestUsername = "interactionone";
        String requestPassword = "mobi123";

        String creds = String.format("%s:%s", requestUsername, requestPassword);
        String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
        requestHeader.put("Authorization", auth);
        requestHeader.put("Content-Type", "application/json; charset=utf-8");
        return requestHeader;
    }

    public void saveToPreferences(String key, String value) {
        final SharedPreferences prefs = getSharedPreferences(BaseApplication.PREF_FILE_CONFIG,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public void saveOcr(String ocr, String date, Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(BaseApplication.PREF_FILE_CONFIG,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_REMIND, ocr);
        editor.putString(KEY_DATE, date);
        editor.commit();
    }

    public HashMap<String, String> getOcr(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(BaseApplication.PREF_FILE_CONFIG,
                Context.MODE_PRIVATE);
        HashMap<String, String> user = new HashMap<>();
        user.put(KEY_REMIND, prefs.getString(KEY_REMIND, null));
        user.put(KEY_DATE, prefs.getString(KEY_DATE, null));
        return user;
    }

    public void saveToPrefsInteger(String key, int value) {
        final SharedPreferences prefs = getSharedPreferences(BaseApplication.PREF_FILE_CONFIG,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public void saveToPreferences(String key, boolean value) {
        final SharedPreferences prefs = getSharedPreferences(BaseApplication.PREF_FILE_CONFIG,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        mayRequestLocation();
        displayLocationSettingsRequest(mContext);
    }

    private void displayLocationSettingsRequest(Context context) {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API).build();
        googleApiClient.connect();

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000 / 2);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.i(TAG, "All location settings are satisfied.");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to upgrade location settings ");

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(BaseActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        break;
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
//                        startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        break;
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    /*@Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestFineLocationAccess();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }*/

    /*private void requestFineLocationAccess() {
        if (!mayRequestLocation()) {
            return;
        }
    }

    private boolean mayRequestLocation() {
        String[] array = new String[]{ACCESS_FINE_LOCATION};
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION)) {
            *//*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                Snackbar.make(view, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE).setAction(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(array, 0);
                        }
                    }
                });*//*
            showToast("Enable Location Permissions to continue using Tap to transfer eReceipt feature.");
        } else {
            requestPermissions(array, 0);
        }
        return false;
    }*/


    @Override
    protected void onPause() {
        super.onPause();
    }

    public String getFromPreferences(String key) {
        final SharedPreferences prefs = getSharedPreferences(BaseApplication.PREF_FILE_CONFIG,
                Context.MODE_PRIVATE);
        String value = prefs.getString(key, "");
        if (!value.isEmpty()) {
            return value;
        }
        return "";
    }

    public int getIntegerPreferences(String key) {
        final SharedPreferences prefs = getSharedPreferences(BaseApplication.PREF_FILE_CONFIG,
                Context.MODE_PRIVATE);
        int value = prefs.getInt(key, -1);
        return value;
    }

    public boolean getBooleanPreferences(String key, boolean defaultValue) {
        final SharedPreferences prefs = getSharedPreferences(BaseApplication.PREF_FILE_CONFIG,
                Context.MODE_PRIVATE);
        boolean value = prefs.getBoolean(key, defaultValue);
        return value;
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    public void getFragment(Fragment fragment) {
//        int containerViewId = R.id.fragment_container;
//        getFragment(containerViewId, fragment);
    }

    public void getFragment(int containerViewId, Fragment fragment) {
        getFragment(containerViewId, fragment, null, null);
    }

    public void getFragment(int containerViewId, Fragment fragment, View sharedElement, String transitionName) {
        String backStateName = fragment.getClass().getName();
        String fragmentTag = backStateName;

        FragmentManager manager = getSupportFragmentManager();
        boolean fragmentPopped = manager.popBackStackImmediate(backStateName, 0);
        if (!fragmentPopped && manager.findFragmentByTag(fragmentTag) == null) {
            FragmentTransaction fragmentTransaction = manager.beginTransaction();
            if (sharedElement != null && transitionName != null)
                fragmentTransaction.addSharedElement(sharedElement, transitionName);
            fragmentTransaction.add(containerViewId, fragment, backStateName);
            fragmentTransaction.addToBackStack(backStateName);
            fragmentTransaction.commit();
        }
    }

    public void showProgress(String message) {
        if (apiProgressDialog == null)
            apiProgressDialog = new ProgressDialog(this);
        apiProgressDialog.setMessage(message);
        if (enableAPIProgressDialog) apiProgressDialog.show();
    }

    public void dismissProgress() {
        if (apiProgressDialog != null) apiProgressDialog.dismiss();
    }

    private BroadcastReceiver mRefreshUserReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onRefreshUser(context, intent);
        }
    };

    protected void onRefreshUser(Context context, Intent intent) {
//        User user = User.load(context);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    public void beforeAPICall(MobileAPI.MobileAPIStatus mobileAPIStatus) {
        if (!mobileAPIStatus.getApiURL().contains(BaseApplication.GET_USER_URL)) {
            showProgress(mobileAPIStatus.getApiMessage());
        }
    }

    @Override
    public void afterAPICall(MobileAPI.MobileAPIStatus mobileAPIStatus) {
        dismissProgress();
    }

    public void onAPIError(JSONObject response) {
        try {
            String errorMsg = response.getString("message");
            String errorCode = response.getString("code");

            /*if (errorCode.equals("204") && errorMsg.equalsIgnoreCase("User id missing")) {
                showToast("Login to Vcloud account to use more features");
                return;
            }*/
            appToast(errorMsg, Toast.LENGTH_LONG);
            /*Reset app to SignIn state when the device parameters don't match in the server*/
            if (errorMsg.equalsIgnoreCase("User id missing") ||
                    errorMsg.equalsIgnoreCase("Sorry ! Unauthorized access.") ||
                    errorMsg.equalsIgnoreCase("Device does not exist") ||
                    errorMsg.equalsIgnoreCase("Invalid user device combination.") ||
                    errorMsg.equalsIgnoreCase("Sorry!.You have been blocked from accessing Binge.")) {
            }
        } catch (JSONException e) {
            Utils.printStackTrace(e, true);
        }
    }

    public void onInternetError(String apiURL) {
        showToast("No Network Available");
        registerNetworkListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (apiProgressDialog != null) {
            dismissProgress();
            apiProgressDialog = null;
        }
        mobileAPI.cancelAll();
        if (listenForUserChanges) {
            mContext.unregisterReceiver(mRefreshUserReceiver);
        }
        unregisterNetworkListener();
    }

    /*Returns the layout params required for Overlay Guide*/
    public RelativeLayout.LayoutParams getLayoutParams() {

        RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lps.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        int margin = 20;
//        int margin = ((Number) (getResources().getDisplayMetrics().density * 12)).intValue();
        lps.setMargins(margin, margin, margin, 120);
        return lps;
    }

    public void showToast(String message) {
        appToast(message, Toast.LENGTH_SHORT);
    }

    public class NetworkReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (isNetworkAvailable()) {
                if (debugLogs) Utils.Log(Log.DEBUG, "TAG", "Reconnect Triggered");
                onNetReconnect();
            }
        }

    }

    public void onNetReconnect() {
        unregisterNetworkListener();
        mobileAPI.continueQueue();
    }

    NetworkReceiver networkReceiver;

    public void registerNetworkListener() {
        if (networkReceiver == null) {
            networkReceiver = new NetworkReceiver();
            registerReceiver(networkReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        }
    }

    public void unregisterNetworkListener() {
        if (networkReceiver != null) unregisterReceiver(networkReceiver);
        networkReceiver = null;
    }

    String lastToastMessage = "";

    public void appToast(String message, int duration) {
        try {
            if (activityToast == null) {
                activityToast = Toast.makeText(mContext, message, duration);
                activityToast.show();
                lastToastMessage = message;
            } else if (!message.equals(lastToastMessage)) {
                activityToast.cancel();
                activityToast = Toast.makeText(mContext, message, duration);
                activityToast.show();
            }
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    activityToast = null;

                }
            }, 2000);
        } catch (Exception e) {
            Utils.printStackTrace(e);
        }
    }

    public void setStatusBarColorTransperant() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();

            // clear FLAG_TRANSLUCENT_STATUS flag:
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            // Change the color
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    public void setStatusBarColorSolid() {
        Window window = getWindow();
        if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(mContext, R.color.colorPrimaryDark));
        }
    }

//    public String getUserAccountType() {
//        String accountType = "";
//        User user = (User) CacheData.getSavedObject(mContext, User.class);
//        if (user != null) {
//            accountType = user.getUser_type();
//        }
//        return accountType;
//    }

//    public void fetchMyPlan(MobileAPI.OnFetchMySubscriptionPlansCallback callback) {
//        mobileAPI.getMySubscriptionPlan(mContext, new MobileAPI.OnFetchMySubscriptionPlansCallback() {
//            @Override
//            public void onGetPlansSuccess(SubscriptionPlan plan) {
//                callback.onGetPlansSuccess(plan);
//            }
//
//            @Override
//            public void onGetPlansFailure(String message) {
//                callback.onGetPlansFailure(message);
//            }
//        });
//    }

    public void deleteReceipt(int id, CustomHandler handler) {
        showProgress("Deleting Receipt...");
        mobileAPI.deletesReceipt(id, new MobileAPI.OnProcessReceiptCallback() {
            @Override
            public void onProcessSuccess(String message) {
                showToast(message);
//                handler.onSuccess();
                finish();
                dismissProgress();
            }

            @Override
            public void onProcessFailure(String message) {
                showToast(message);
                dismissProgress();
//                handler.onFailure(message);
            }
        });
    }

    public void deleteReceiptDashboard(int id) {
        showProgress("Deleting Receipt...");
        mobileAPI.deletesReceipt(id, new MobileAPI.OnProcessReceiptCallback() {
            @Override
            public void onProcessSuccess(String message) {
                showToast(message);
//                handler.onSuccess();
//                finish();
                dismissProgress();
            }

            @Override
            public void onProcessFailure(String message) {
                showToast(message);
                dismissProgress();
//                handler.onFailure(message);
            }
        });
    }
    public interface CustomHandler extends Serializable {
        void onSuccess();

        void onFailure(String message);
    }

}
