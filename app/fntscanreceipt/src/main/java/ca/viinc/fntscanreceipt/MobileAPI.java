package ca.viinc.fntscanreceipt;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ca.viinc.fntscanreceipt.utils.Utils;

import static ca.viinc.fntscanreceipt.BaseActivity.debugLogs;

//import ca.viinc.ereceiptpro.GlobalConstants;
//import ca.viinc.ereceiptpro.model.*;
//import ca.viinc.ereceiptpro.utils.CacheData;
//import ca.viinc.ereceiptpro.utils.Utils;

/**
 * Created by Goutham Iyyappan on 20/05/19.
 */
public class MobileAPI {
    Context context;
    UBarberCallbacks eReceiptCallbacks;
    String tag_json_obj = "json_obj_req";
//    public static boolean debugLogs = GlobalConstants.debugLogs;
    private HashMap<String, ReceiptAPIRequest> uBarberRequestQueue;

    public MobileAPI(Context context) {
        this(context, null);
    }

    public MobileAPI(Context context, UBarberCallbacks eReceiptCallbacks) {
        this.context = context;
        this.eReceiptCallbacks = eReceiptCallbacks;
        if (eReceiptCallbacks != null)
            tag_json_obj = eReceiptCallbacks.getClass().getSimpleName();
        else
            tag_json_obj = context.getClass().getSimpleName();
        uBarberRequestQueue = new HashMap<>();
    }

    public void cancelAll() {
//        AppController.getInstance().cancelPendingRequests(tag_json_obj);
        uBarberRequestQueue.clear();
    }

    public void makeAPICall(final String apiURL, HashMap<String, Object> extraParams, final OnSuccessCallback successCallback) {
        makeAPICall(apiURL, extraParams, successCallback, null);
    }

    public class ReceiptAPIRequest {
        public String apiURL;
        public HashMap<String, Object> extraParams;
        public OnSuccessCallback successCallback;
        public OnErrorCallback errorCallback;
        public boolean retryRequest;
        public int requestMethod;
        public boolean isHeaderRequired;

        public ReceiptAPIRequest(int requestMethod, String apiURL, HashMap<String, Object> extraParams,
                                 OnSuccessCallback successCallback, OnErrorCallback errorCallback,
                                 boolean retryRequest, boolean isHeaderRequired) {
            this.requestMethod = requestMethod;
            this.apiURL = apiURL;
            this.extraParams = extraParams;
            this.successCallback = successCallback;
            this.errorCallback = errorCallback;
            this.retryRequest = retryRequest;
            this.isHeaderRequired = isHeaderRequired;
        }

        public ReceiptAPIRequest(String apiURL, HashMap<String, Object> extraParams, OnSuccessCallback successCallback, OnErrorCallback errorCallback) {
            this(-1, apiURL, extraParams, successCallback, errorCallback, false, true);
        }

        public ReceiptAPIRequest(String apiURL, HashMap<String, Object> extraParams, OnSuccessCallback successCallback) {
            this(-1, apiURL, extraParams, successCallback, null, false, true);
        }

        public ReceiptAPIRequest(String apiURL, HashMap<String, Object> extraParams, OnSuccessCallback successCallback, boolean retryRequest) {
            this(-1, apiURL, extraParams, successCallback, null, retryRequest, true);
        }

        public ReceiptAPIRequest(String apiURL, HashMap<String, Object> extraParams, OnSuccessCallback successCallback, OnErrorCallback errorCallback, boolean retryRequest) {
            this(-1, apiURL, extraParams, successCallback, errorCallback, retryRequest, true);
        }

        public ReceiptAPIRequest(int requestMethod, String apiURL, HashMap<String, Object> extraParams, OnSuccessCallback successCallback, OnErrorCallback errorCallback) {
            this(requestMethod, apiURL, extraParams, successCallback, errorCallback, false, true);
        }
    }

    public void makeAPICall(final String apiURL, HashMap<String, Object> extraParams,
                            final OnSuccessCallback successCallback, final OnErrorCallback errorCallback) {
        ReceiptAPIRequest ReceiptAPIRequest = new ReceiptAPIRequest(apiURL, extraParams,
                successCallback, errorCallback);
        makeAPICall(ReceiptAPIRequest);

    }

    public void makeAPICall(final String apiURL, HashMap<String, Object> extraParams,
                            final OnSuccessCallback successCallback, final OnErrorCallback errorCallback, boolean retryRequest) {
        ReceiptAPIRequest ReceiptAPIRequest = new ReceiptAPIRequest(apiURL, extraParams,
                successCallback, errorCallback, retryRequest);
        makeAPICall(ReceiptAPIRequest);
    }

    public void makeAPICall(int requestMethod, final String apiURL, HashMap<String, Object> extraParams,
                            final OnSuccessCallback successCallback, final OnErrorCallback errorCallback) {
        ReceiptAPIRequest ReceiptAPIRequest = new ReceiptAPIRequest(requestMethod, apiURL, extraParams,
                successCallback, errorCallback);
        makeAPICall(ReceiptAPIRequest);
    }

    public void makeAPICall(int requestMethod, final String apiURL, HashMap<String, Object> extraParams,
                            final OnSuccessCallback successCallback, final OnErrorCallback errorCallback, boolean retryRequest, boolean isHeaderRequired) {
        ReceiptAPIRequest ReceiptAPIRequest = new ReceiptAPIRequest(requestMethod, apiURL, extraParams,
                successCallback, errorCallback, retryRequest, isHeaderRequired);
        makeAPICall(ReceiptAPIRequest);

    }

    public String getBaseURL() {
//        return BaseApplication.BASE_URL;
        return "";
    }

    public HashMap<String, Object> getCommonParams(String apiURL) {

        final HashMap<String, Object> params = new HashMap<>();

//        User user = User.load(context);
//        DeviceDetails savedDevice = DeviceDetails.getInstance(context);
//        params.put("vendor_id", savedDevice.getVendor_id());
//
//        if (user != null && user.getUser_id() != null && !user.getUser_id().isEmpty())
//            params.put("user_id", user.getUser_id());

        return params;
    }

    public void makeAPICall(final ReceiptAPIRequest receiptAPIRequest) {
        final String apiURL = receiptAPIRequest.apiURL;
        HashMap<String, Object> extraParams = receiptAPIRequest.extraParams;
        final OnSuccessCallback successCallback = receiptAPIRequest.successCallback;
        final OnErrorCallback errorCallback = receiptAPIRequest.errorCallback;
        boolean retryRequest = receiptAPIRequest.retryRequest;

        uBarberRequestQueue.remove(apiURL);

        final String apiName = apiURL.replace(getBaseURL(), "");
        Log.e("Debug", "API" + "Calling API :" + apiName);
        if (eReceiptCallbacks != null) eReceiptCallbacks.beforeAPICall(new MobileAPIStatus(apiURL));
        int requestMethod = receiptAPIRequest.requestMethod;

        JSONObject jsonRequest = new JSONObject();
        if (extraParams != null) {
            jsonRequest = new JSONObject(extraParams);
        } else {
            Log.d("MobileApi", "no params to send to api");
        }

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(requestMethod,
                apiURL, jsonRequest,
                new com.android.volley.Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (debugLogs)
                            Utils.Log(Log.DEBUG, "API- ", apiName + ": " + response.toString());
                        if (eReceiptCallbacks != null)
                            eReceiptCallbacks.afterAPICall(new MobileAPIStatus(apiURL));

                        if (checkResponse(response, apiURL)) {
//                            Response was successful
                            if (successCallback != null) successCallback.onSuccess(response);
                        } else {
                            //Response was a failure
                            if (errorCallback != null)
                                errorCallback.onError(response);
                            else if (eReceiptCallbacks != null)
                                eReceiptCallbacks.onAPIError(response);
                        }
                    }
                }, new com.android.volley.Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                //VolleyUtils.Log(Log.DEBUG"TAG", "Error: " + error.getMessage());
                NetworkResponse networkResponse = error.networkResponse;
                String responseString;
                if (networkResponse != null && networkResponse.data != null) {
                    responseString = new String(networkResponse.data);
                    Utils.Log(Log.ERROR, "API Error", "Php Error: " + responseString);
                } else {
                    responseString = "";
                }
                if (eReceiptCallbacks != null)
                    eReceiptCallbacks.afterAPICall(new MobileAPIStatus(apiURL));

                JSONObject response = null;
                try {
                    response = new JSONObject(responseString);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (response != null && errorCallback != null && networkResponse != null &&
                        isClientError(networkResponse.statusCode)) {
                    errorCallback.onError(response);
                }

                if (eReceiptCallbacks != null && networkResponse != null &&
                        isServerError(networkResponse.statusCode))
                    eReceiptCallbacks.onAPIError(response);

                if (debugLogs) Utils.Log(Log.DEBUG, "API", apiName + ": " + error.getMessage());
                if (Utils.isNetworkProblem(error)) {
                    if (debugLogs) Utils.Log(Log.DEBUG, "API", apiName + ": Internet Error");
                    if (eReceiptCallbacks != null) eReceiptCallbacks.onInternetError(apiURL);
                    if (receiptAPIRequest.retryRequest) {
                        uBarberRequestQueue.put(apiURL, receiptAPIRequest);
                    }
                } else if (Utils.isTimeoutError(error)) {
                    if (debugLogs)
                        Utils.Log(Log.DEBUG, "API", apiName + ": Internet Timeout Error");
                    if (eReceiptCallbacks != null) eReceiptCallbacks.onInternetError(apiURL);
                    if (receiptAPIRequest.retryRequest) {
                        uBarberRequestQueue.put(apiURL, receiptAPIRequest);
                    }
                }
            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json";
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getRequestHeaders(apiURL, receiptAPIRequest);
            }
        };

        jsonObjReq.setRetryPolicy(new DefaultRetryPolicy(30000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        // Adding request to request queue
//        AppController.getInstance().addToRequestQueue(jsonObjReq, tag_json_obj);
    }

    public boolean isClientError(int statusCode) {
        return statusCode > 399 && statusCode < 500;
    }

    public boolean isServerError(int statusCode) {
        return statusCode > 499;
    }

    public boolean checkResponse(JSONObject response, String apiURL) {
        try {
            boolean isVerifyNeeded = false;
            if (apiURL.equals(BaseApplication.PHP_LOGIN_URL)) {
                isVerifyNeeded = response.getInt("code") == 503;
            }
            return response.getBoolean("status") || isVerifyNeeded;

        } catch (JSONException e) {
            Utils.Log(Log.ERROR, "TAG", e.toString());
            return false;
        }
    }

    public void continueQueue() {
        Iterator it = uBarberRequestQueue.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry entry = (HashMap.Entry) it.next();
            ReceiptAPIRequest ReceiptAPIRequest = (ReceiptAPIRequest) entry.getValue();
            it.remove();
            makeAPICall(ReceiptAPIRequest);
        }
    }

    private String getAPIMessage(String apiURL) {
        String apiMessage;
        switch (apiURL) {
            default:
                apiMessage = "Requesting...";
        }
        return apiMessage;
    }

    public HashMap<String, String> getRequestHeaders(String apiURL, ReceiptAPIRequest request) {
        HashMap<String, String> requestHeader = new HashMap<>();

        if (request.isHeaderRequired)
            requestHeader.put("Content-Type", "application/json");
        requestHeader.put("Accept", "application/json");


        if (!apiURL.equals(BaseApplication.REGISTER_ACCOUNT_URL) &&
                !apiURL.equals(BaseApplication.PHP_REGISTER_ACCOUNT_URL) &&
                !apiURL.equals(BaseApplication.LOGIN_URL) &&
                !apiURL.equals(BaseApplication.PHP_LOGIN_URL)) {

            SharedPreferences prefs = context.getSharedPreferences(BaseApplication.PREF_FILE_CONFIG,
                    Context.MODE_PRIVATE);
            String api_token = prefs.getString(BaseApplication.EXTRA_KEY_API_TOKEN, "");
            requestHeader.put("Authorization", "Bearer " + api_token);
        }

        return requestHeader;
    }

    public interface UBarberCallbacks {
        void beforeAPICall(MobileAPIStatus apiStatus);

        void afterAPICall(MobileAPIStatus apiStatus);

        void onAPIError(JSONObject response);

        void onInternetError(String apiURL);
    }

    public interface OnSuccessCallback {
        void onSuccess(JSONObject response);
    }

    public interface OnErrorCallback {
        void onError(JSONObject response);
    }

    public class MobileAPIStatus {
        public String apiURL;
        public String apiMessage;

        public MobileAPIStatus(String apiURL, String apiMessage) {
            this.apiURL = apiURL;
            this.apiMessage = apiMessage;
        }

        public MobileAPIStatus(String apiURL) {
            this(apiURL, getAPIMessage(apiURL));
        }

        public String getApiMessage() {
            return apiMessage;
        }

        public String getApiURL() {
            return apiURL;
        }
    }

    public interface OnAuthenticationCallback {
        void onSignUpSuccess();

        void onSignInSuccess(JSONObject response);

        void onLogoutSuccess(String message);

        void onVerifySuccess(String message);

        void onFailure(String message);
    }

    public void signUp(final HashMap<String, Object> extraParams, final OnAuthenticationCallback onAuthenticationCallback) {
        makeAPICall(Request.Method.POST, BaseApplication.PHP_REGISTER_ACCOUNT_URL, extraParams, new OnSuccessCallback() {
            @Override
            public void onSuccess(JSONObject response) {
//                    String api_token = response.getJSONObject("data").getString("api_token");
                if (onAuthenticationCallback != null)
                    onAuthenticationCallback.onSignUpSuccess();
            }
        }, response -> {
            try {
                boolean status = response.getBoolean("status");
                String message = response.getString("message");

                if (onAuthenticationCallback != null) {
                    if (status)
                        onAuthenticationCallback.onVerifySuccess(message);
                    else
                        onAuthenticationCallback.onFailure(message);
                }


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, true, true);
    }


    public void sendActivationEmail(@NotNull String mVerifyEmail, final OnAuthenticationCallback onAuthenticationCallback) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("email", mVerifyEmail);
        makeAPICall(Request.Method.POST, BaseApplication.PHP_SEND_ACTIVATION_EMAIL_URL, params, response -> {
            try {
                String message = response.getString("message");
                if (onAuthenticationCallback != null)
                    onAuthenticationCallback.onVerifySuccess(message);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }, response -> {
            try {
                String message = response.getString("message");
                if (onAuthenticationCallback != null)
                    onAuthenticationCallback.onFailure(message);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, true, true);
    }


    public void sendOTPForPasswordReset(@NotNull String mVerifyEmail, final OnAuthenticationCallback onAuthenticationCallback) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("email", mVerifyEmail);
        makeAPICall(Request.Method.POST, BaseApplication.PHP_SEND_OTP_RESET_PWD_URL, params, response -> {
            try {
                String message = response.getString("message");
                if (onAuthenticationCallback != null)
                    onAuthenticationCallback.onVerifySuccess(message);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }, response -> {
            try {
                String message = response.getString("message");
                if (onAuthenticationCallback != null)
                    onAuthenticationCallback.onFailure(message);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, true, true);
    }

    public void resetPassword(HashMap<String, Object> extraParams, final OnAuthenticationCallback onAuthenticationCallback) {
        makeAPICall(Request.Method.POST, BaseApplication.PHP_RESET_PWD_URL, extraParams, response -> {
            try {
                String message = response.getString("message");
                if (onAuthenticationCallback != null)
                    onAuthenticationCallback.onVerifySuccess(message);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }, response -> {
            try {
                String message = response.getString("message");
                if (onAuthenticationCallback != null)
                    onAuthenticationCallback.onFailure(message);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, true, true);
    }

    public void performSignIn(HashMap<String, Object> extraParams, final OnAuthenticationCallback onAuthenticationCallback) {
        makeAPICall(Request.Method.POST, BaseApplication.PHP_LOGIN_URL, extraParams, response -> {
            if (onAuthenticationCallback != null)
                onAuthenticationCallback.onSignInSuccess(response);

        }, response -> {
            try {
                String message = response.getString("message");
                if (onAuthenticationCallback != null)
                    onAuthenticationCallback.onFailure(message);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, true, true);
    }

    public void logout(final OnAuthenticationCallback onAuthenticationCallback) {
        makeAPICall(Request.Method.POST, BaseApplication.PHP_LOG_OUT_URL, null, response -> {
            try {
                boolean status = response.getBoolean("status");
                String message = response.getString("message");
                if (status && onAuthenticationCallback != null)
                    onAuthenticationCallback.onLogoutSuccess(message);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, response -> {
            try {
                String message = response.getString("description");
                if (onAuthenticationCallback != null)
                    onAuthenticationCallback.onFailure(message);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, true, false);
    }


//    public interface OnGetUserCallback {
//        void onGetUserSuccess(User user);
//
//        void onGetUserFailure(String message, boolean approvalPending);
//    }

//    public void getUser(final Context context, final OnGetUserCallback callback) {
//        makeAPICall(Request.Method.POST, BaseApplication.PHP_GET_USER_URL, null, new OnSuccessCallback() {
//            @Override
//            public void onSuccess(JSONObject response) {
//                try {
//                    JSONObject dataObj = response.getJSONObject("data");
//
//                    User user = new Gson().fromJson(dataObj.getJSONObject("user_details").toString(), User.class);
//                    CacheData.saveObject(context, user);
//                    if (callback != null)
//                        callback.onGetUserSuccess(user);
//
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//        }, new OnErrorCallback() {
//            @Override
//            public void onError(JSONObject response) {
//                try {
//                    int statusCode = response.getInt("code");
//                    String message = response.getString("description");
//
//                    boolean approvalPending = false;
//                    if (statusCode == 401 && message.contains("Not Approved")) {
//                        approvalPending = true;
//                    }
//
//                    if (callback != null)
//                        callback.onGetUserFailure(message, approvalPending);
//
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//        }, true, true);
//    }

//    public interface OnReceiptsCallback {
//        void onGetUserSuccess(ArrayList<ReceiptFile> receipts);
//
//        void onGetUserFailure(String message);
//    }

//    public void getReceipts(HashMap<String, Object> params, final Context context, final OnReceiptsCallback callback) {
//        makeAPICall(Request.Method.POST, BaseApplication.PHP_GET_ALL_RECEIPTS_URL, params, new OnSuccessCallback() {
//            @Override
//            public void onSuccess(JSONObject response) {
//                try {
//                    String userType = response.getJSONObject("data").getString("user_type");
//                    CacheData.saveObject(context, userType);
//
//                    ReceiptFile[] receiptArr = new Gson().fromJson(response.getJSONObject("data")
//                            .getJSONArray("receipts").toString(), ReceiptFile[].class);
//                    CacheData.saveObjects(context, receiptArr);
//                    ArrayList<ReceiptFile> receipts = new ArrayList<>();
//                    Collections.addAll(receipts, receiptArr);
//
//                    if (callback != null)
//                        callback.onGetUserSuccess(receipts);
//
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//        }, new OnErrorCallback() {
//            @Override
//            public void onError(JSONObject response) {
//                try {
//                    String message = response.getString("message");
//
//                    if (callback != null)
//                        callback.onGetUserFailure(message);
//
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//        }, true, true);
//    }

    public void deletesReceipt(int receipt_id, final OnProcessReceiptCallback callback) {
        HashMap<String, Object> param = new HashMap<>();
        param.put("receipt_id", receipt_id);
        makeAPICall(Request.Method.POST, BaseApplication.PHP_DELETE_RECEIPT_URL, param, response -> {
            try {
                String message = response.getString("message");
                if (response.getBoolean("status")) {
                    if (callback != null)
                        callback.onProcessSuccess(message);
                } else {
                    if (callback != null)
                        callback.onProcessFailure(message);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, response -> {
            try {
                String message = response.getString("message");

                if (callback != null)
                    callback.onProcessFailure(message);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, true, true);
    }

    public interface OnProcessReceiptCallback {
        void onProcessSuccess(String message);

        void onProcessFailure(String message);
    }

    public void processReceipt(int receipt_id, final Context context, final OnProcessReceiptCallback callback) {
        HashMap<String, Object> param = new HashMap<>();
        param.put("receipt_id", receipt_id);
        makeAPICall(Request.Method.POST, BaseApplication.PHP_PROCESS_RECEIPT_URL, param, new OnSuccessCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    String message = response.getString("message");
                    if (response.getBoolean("status")) {
                        if (callback != null)
                            callback.onProcessSuccess(message);
                    } else {
                        if (callback != null)
                            callback.onProcessFailure(message);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new OnErrorCallback() {
            @Override
            public void onError(JSONObject response) {
                try {
                    String message = response.getString("message");

                    if (callback != null)
                        callback.onProcessFailure(message);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, true, true);
    }

//    public interface OnAnalyseReceiptCallback {
//        void onAnalyseSuccess(AnalysedReceipt analysedReceipt);
//
//        void onAnalyseFailure(String message);
//    }

//    public void analyseReceipt(int receipt_id, final Context context, final OnAnalyseReceiptCallback callback) {
//        HashMap<String, Object> param = new HashMap<>();
//        param.put("receipt_id", receipt_id);
//        makeAPICall(Request.Method.POST, BaseApplication.PHP_ANALYSE_RECEIPT_URL, param, new OnSuccessCallback() {
//            @Override
//            public void onSuccess(JSONObject response) {
//                try {
//                    String message = response.getString("message");
//                    if (response.getBoolean("status")) {
//
//                        JSONObject dataObj = response.getJSONObject("data");
//                        AnalysedReceipt receipt = new Gson().fromJson(dataObj.getJSONObject("analyzed_data").toString(), AnalysedReceipt.class);
////                        CacheData.saveObject(context, receipt);
//
//                        if (callback != null)
//                            callback.onAnalyseSuccess(receipt);
//                    } else {
//                        if (callback != null)
//                            callback.onAnalyseFailure(message);
//                    }
//
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//        }, new OnErrorCallback() {
//            @Override
//            public void onError(JSONObject response) {
//                try {
//                    String message = response.getString("message");
//
//                    if (callback != null)
//                        callback.onAnalyseFailure(message);
//
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//        }, true, true);
//    }























}