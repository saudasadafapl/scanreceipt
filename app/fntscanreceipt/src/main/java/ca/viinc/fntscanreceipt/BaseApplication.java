package ca.viinc.fntscanreceipt;

import android.app.Application;


/**
 * Created by Goutham Iyyappan on 20/05/19.
 */
public class BaseApplication extends Application {

    //eReceiptPRo stripe creds
    public static final String STRIPE_TEST_API_KEY = "pk_test_Jvf7hp8pJZLuwEAnopTcbaFK0068YMrMeb";
    public static final String STRIPE_LIVE_API_KEY = "pk_live_";

    public static final String PREF_FILE_CONFIG = "Config";
    public static final String PREF_FILE_PERSISTENT_CONFIG = "Persistent State Config";
    public static final String EXTRA_KEY_DEVICE = "device";
    public static final String EXTRA_KEY_USER = "user";
    public static final String EXTRA_KEY_USER_INTENT = "User_Intent";
    public static final String EXTRA_KEY_API_TOKEN = "api_token";
    public static final String EXTRA_KEY_USER_ROLE = "userRole";

    public static final String BASE_URL = "";
    public static final String PHP_BASE_URL ="";

    public static final String REGISTER_ACCOUNT_URL = BASE_URL + "auth/signup";
    public static final String LOGIN_URL = BASE_URL + "auth/login";
    public static final String LOG_OUT_URL = BASE_URL + "auth/logout";

    public static final String GET_USER_URL = BASE_URL + "account/profile";


    public static final String PHP_REGISTER_ACCOUNT_URL = PHP_BASE_URL + "register";
    public static final String PHP_SEND_ACTIVATION_EMAIL_URL = PHP_BASE_URL + "send-activation-email";
    public static final String PHP_LOGIN_URL = PHP_BASE_URL + "login";
    public static final String PHP_SEND_OTP_RESET_PWD_URL = PHP_BASE_URL + "send-reset-password-otp";
    public static final String PHP_RESET_PWD_URL = PHP_BASE_URL + "reset-password";
    public static final String PHP_LOG_OUT_URL = PHP_BASE_URL + "logout";

    public static final String PHP_GET_USER_URL = PHP_BASE_URL + "profile";
    public static final String PHP_RECEIPTS_BASE_URL = PHP_BASE_URL + "receipts";
    public static final String PHP_GET_ALL_RECEIPTS_URL = PHP_RECEIPTS_BASE_URL;
    public static final String PHP_UPLOAD_RECEIPT_URL = PHP_RECEIPTS_BASE_URL + "/upload";
    public static final String PHP_PROCESS_RECEIPT_URL = PHP_RECEIPTS_BASE_URL + "/process";
    public static final String PHP_ANALYSE_RECEIPT_URL = PHP_RECEIPTS_BASE_URL + "/analyze";
    public static final String PHP_DELETE_RECEIPT_URL = PHP_RECEIPTS_BASE_URL + "/delete";
    public static final String PHP_GET_EXPENSE_STATEMENT_URL = PHP_BASE_URL + "expense-statement";
    public static final String PHP_GET_EXPENSE_ANALYTICS_URL = PHP_BASE_URL + "expense-analytics";
    public static final String PHP_DOWNLOAD_RECEIPTS_URL = PHP_BASE_URL + "download-receipts";
    public static final String PHP_GET_SUBSCRIPTION_PLANS_URL = PHP_BASE_URL + "subscriptions/plans";

    public static final String PHP_CREATE_SUBSCRIPTION_PLAN_URL = PHP_BASE_URL + "subscriptions/create";
    public static final String PHP_CHANGE_SUBSCRIPTION_PLAN_URL = PHP_BASE_URL + "subscriptions/change-plan";
    public static final String PHP_FETCH_MY_SUBSCRIPTION_PLAN_URL = PHP_BASE_URL + "subscriptions/fetch";
    public static final String PHP_CANCEL_SUBSCRIPTION_PLAN_URL = PHP_BASE_URL + "subscriptions/cancel";
    public static final String PHP_RESUME_SUBSCRIPTION_PLAN_URL = PHP_BASE_URL + "subscriptions/resume";

    public static final String ADD_CARD_URL = PHP_BASE_URL + "cards/add";
    public static final String UPDATE_CARD_URL = PHP_BASE_URL + "cards/update";
    public static final String GET_CARDS_URL = PHP_BASE_URL + "cards/fetch";
    public static final String DELETE_CARD_URL = PHP_BASE_URL + "cards/delete";
    public static final String UPDATE_DEFAULT_CARD_URL = PHP_BASE_URL + "subscriptions/change-card";

    public static final String SEARCH_ITEMS_URL = PHP_BASE_URL + "search-item";
    public static final String GET_EXPENSE_ANALYTICS_FOR_ITEM_URL = PHP_BASE_URL + "item-expense-analytics";

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
    public static BaseApplication getInstance() {
        return instance;
    }

    private  static BaseApplication instance;

}
