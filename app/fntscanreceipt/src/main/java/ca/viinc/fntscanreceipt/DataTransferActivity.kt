package ca.viinc.fntscanreceipt

//import kotlinx.android.synthetic.main.activity_main.*
import android.app.Activity
import android.app.Dialog
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.res.Configuration
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.*
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.barteksc.pdfviewer.PDFView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class DataTransferActivity : BaseActivity(), RadioGroup.OnCheckedChangeListener {

    private var mState = UART_PROFILE_DISCONNECTED
    private var mService: UartService? = null
    private var mDevice: BluetoothDevice? = null
    private var mBtAdapter: BluetoothAdapter? = null
    private var messageListView: ListView? = null
    private var listAdapter: ArrayAdapter<String>? = null
    private var btnConnectDisconnect: Button? = null
    private var btnSend: Button? = null
    private var btnPDF: Button? = null
    private var edtMessage: EditText? = null
    private var pdfView: PDFView? = null
    private lateinit var mLogLayout: LinearLayout
    private var isNfcSupported: Boolean = false
    private var nfcAdapter: NfcAdapter? = null

    //UART service connected/disconnected
    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, rawBinder: IBinder) {
            mService = (rawBinder as UartService.LocalBinder).service
            Log.d(TAG, "onServiceConnected mService= " + mService!!)
            if (!mService!!.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth")
                finish()
            }
        }

        override fun onServiceDisconnected(classname: ComponentName) {
            ////     mService.disconnect(mDevice);
            mService = null
        }
    }

    private val mHandler = object : Handler() {
        override//Handler events that received from UART service
        fun handleMessage(msg: Message) {
        }
    }

    private val UARTStatusChangeReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            val mIntent = intent
            var deviceName = "Goutham's Nordic BLE"
            //*********************//
            if (action == UartService.ACTION_GATT_CONNECTED) {
                runOnUiThread {
                    val currentDateTimeString = DateFormat.getTimeInstance().format(Date())
                    Log.d(TAG, "UART_CONNECT_MSG")
                    btnConnectDisconnect!!.tag = "Disconnect"
                    btnConnectDisconnect!!.text = "Disconnect From BLE Device"
                    edtMessage!!.isEnabled = true
                    btnSend!!.isEnabled = true

                    if (mDevice != null && mDevice!!.name != null) deviceName = mDevice!!.name
                    (findViewById<View>(R.id.deviceName) as TextView).text = "Connected to " + deviceName + " device"
                    listAdapter!!.add("[" + currentDateTimeString + "] Connected to: " + deviceName)
                    messageListView!!.smoothScrollToPosition(listAdapter!!.count - 1)
                    mState = UART_PROFILE_CONNECTED
                }
            }

            //*********************//
            if (action == UartService.ACTION_GATT_DISCONNECTED) {
                runOnUiThread {
                    val currentDateTimeString = DateFormat.getTimeInstance().format(Date())
                    Log.d(TAG, "UART_DISCONNECT_MSG")
                    btnConnectDisconnect!!.tag = "Connect"
                    btnConnectDisconnect!!.text = "Connect To BLE Device"
                    edtMessage!!.isEnabled = false
                    btnSend!!.isEnabled = false
                    (findViewById<View>(R.id.deviceName) as TextView).text = "Not Connected"
                    if (mDevice != null && mDevice!!.name != null) deviceName = mDevice!!.name
                    listAdapter!!.add("[" + currentDateTimeString + "] Disconnected to: " + deviceName)
                    mState = UART_PROFILE_DISCONNECTED
                    mService!!.close()
                    //setUiState();
                }
            }


            //*********************//
            if (action == UartService.ACTION_GATT_SERVICES_DISCOVERED) {
                mService!!.enableTXNotification()
            }
            //*********************//
            if (action == UartService.ACTION_DATA_AVAILABLE) {
                hasFileCreated = false
                val txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA)
                runOnUiThread {
                    try {
                        if (mBLEDataArray == null) {
                            mBLEDataArray = ByteArray(txValue.size)
                            mBLEDataArray = txValue
                        } else {
                            val newArray = combineByteArray(mBLEDataArray!!, txValue)
                            mBLEDataArray = ByteArray(newArray.size)
                            mBLEDataArray = newArray
                        }

                        val text = String(txValue, Charset.forName("UTF-8"))
                        val currentDateTimeString = DateFormat.getTimeInstance().format(Date())
                        listAdapter!!.add("[$currentDateTimeString] RX: $text")
                        messageListView!!.smoothScrollToPosition(listAdapter!!.count - 1)

                        val size = mBLEDataArray!!.size
                        var eofArray = mBLEDataArray!!.copyOfRange(size - 3, size)
                        if (eofArray.toString(Charset.defaultCharset()).toLowerCase().contains("eof")) {
                            if (!hasFileCreated)
                                createPDF(mBLEDataArray!!.dropLast(3).toByteArray())
                            showToast("File Transfer Complete...")
                            if (mService != null) mService!!.disconnect()
                            mBLEDataArray = null

                            btnPDF!!.isEnabled = true
                            btnPDF!!.isClickable = true
                        } else {
                            btnPDF!!.isEnabled = false
                            btnPDF!!.isClickable = false
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, e.toString())
                    }
                }
            }
            //*********************//
            if (action == UartService.DEVICE_DOES_NOT_SUPPORT_UART) {
                showMessage("Device doesn't support UART. Disconnecting")
                mService!!.disconnect()
            }
        }

    }
    private var mBLEDataArray: ByteArray? = null

    fun combineByteArray(array1: ByteArray, array2: ByteArray): ByteArray {
        val newArray = ByteArray(array1.size + array2.size)
        System.arraycopy(array1, 0, newArray, 0, array1.size)
        System.arraycopy(array2, 0, newArray, array1.size, array2.size)
        return newArray
    }

    fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }

//    private fun uploadImageToBackend(file: File) {
//        val serverURL = BaseApplication.PHP_UPLOAD_RECEIPT_URL
//        try {
//            val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
//                .addFormDataPart("file", file.getName(), file.toRequestBody("text/plain".toMediaTypeOrNull()))
//                .build()
//
//            val request = Request.Builder()
//                .url(serverURL)
//                .post(requestBody)
//                .build()
//
//            val token = getFromPreferences(BaseApplication.EXTRA_KEY_API_TOKEN)
//            if (token.isNullOrEmpty()) {
//                showToast("Invalid user login.")
//                startActivity(Intent(mContext, LoginActivity::class.java))
//                finish()
//                return
//            }
//            val client = OkHttpClient().newBuilder().addInterceptor(object : Interceptor {
//                override fun intercept(chain: Interceptor.Chain): Response {
//                    val request = chain.request().newBuilder()
//                        .addHeader("Authorization", "Bearer " + token)
//                        .build()
//                    return chain.proceed(request)
//                }
//            }).build()
//
//            client.newCall(request).enqueue(object : Callback {
//
//                override fun onFailure(call: Call, e: IOException) {
//                    Utils.printStackTrace(e)
//                    showToast(e.message)
//                }
//
//                override fun onResponse(call: Call, response: Response) {
//                    if (response.body != null) {
//                        try {
//                            val jsonResponse = JSONObject(response.body!!.string())
//                            if (jsonResponse.getBoolean("status")) {
//                                runOnUiThread {
//                                    showToast(jsonResponse.getString("message"))
//                                }
//                            } else {
//                                runOnUiThread {
//                                    showToast(jsonResponse.getString("message"))
//                                }
//                            }
//
//                        } catch (e: JsonParseException) {
//                            Utils.printStackTrace(e)
//                        }
//
//                    }
//                }
//            })
//
//        } catch (e: Exception) {
//            // Handle the error
//            Utils.printStackTrace(e)
//        }
//    }
//open fun setStatusBarColorSolid(): Unit {
//    val window = window
//    if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
//        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
//        window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimaryDark)
//    }
//}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        setStatusBarColorSolid()
        mBtAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show()
//            finish()
            return
        }
        messageListView = findViewById<View>(R.id.listMessage) as ListView
        listAdapter = ArrayAdapter(this, R.layout.message_detail)
        messageListView!!.adapter = listAdapter
        messageListView!!.divider = null
        btnConnectDisconnect = findViewById<View>(R.id.btn_select) as Button
        btnSend = findViewById<View>(R.id.sendButton) as Button
        btnPDF = findViewById<View>(R.id.btn_pdf) as Button
        edtMessage = findViewById<View>(R.id.sendText) as EditText
        pdfView = findViewById(R.id.pdfView) as PDFView
        mLogLayout = findViewById(R.id.linearLayout3)
        btnConnectDisconnect!!.tag = "Connect"
        service_init()

        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this)?.let { it }
        checkNFC()

        // Handle Disconnect & Connect button
        btnConnectDisconnect!!.setOnClickListener {
            if (!mBtAdapter!!.isEnabled) {
                Log.i(TAG, "onClick - BT not enabled yet")
                val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(
                    enableIntent,
                    REQUEST_ENABLE_BT
                )
            } else {
                if (btnConnectDisconnect!!.tag == "Connect") {

                    //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices
                    val newIntent = Intent(this@DataTransferActivity, DeviceListActivity::class.java)
                    startActivityForResult(
                        newIntent,
                        REQUEST_SELECT_DEVICE
                    )

                } else {
                    //Disconnect button pressed
//                    if (mDevice != null) {
                    mService!!.disconnect()
//                    }
                }
            }
        }

        btnPDF!!.setOnClickListener {
            if (mBLEDataArray != null) {
                pdfView!!.fromBytes(mBLEDataArray).load()
            }

            if (pdfView!!.visibility == View.VISIBLE) {
                togglePDFVisibility(false)
                btnPDF!!.text = "View PDF"
            } else {
                togglePDFVisibility(true)
                btnPDF!!.text = "Hide PDF"
            }
        }
    }

    fun checkNFC() {
        isNfcSupported = this.nfcAdapter != null

        if (!isNfcSupported) {
            Toast.makeText(this, "Nfc is not supported on this device", Toast.LENGTH_SHORT).show()
        }

        if (isNfcSupported && nfcAdapter != null && !nfcAdapter?.isEnabled!!) {
            Toast.makeText(
                this,
                "NFC disabled on this device. Turn on to proceed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun receiveMessageFromDevice(intent: Intent) {
        val action = intent.action
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action) {
            val parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            with(parcelables) {
                val inNdefMessage = this[0] as NdefMessage
                val inNdefRecords = inNdefMessage.records
                val ndefRecord_0 = inNdefRecords[0]

                val inMessage = String(ndefRecord_0.payload)
                val mNFCTagType =
                    "MIME type:" + ndefRecord_0.toMimeType() + "\n\n Payload Type(Type Name format): TNF " + ndefRecord_0.tnf
//                val mNFCTagInfo = inMessage

                var macAddrRev = inMessage.removeRange(0, 3).removeRange(17, 23)
                var one = macAddrRev.substring(15).trim()
                var two = macAddrRev.substring(12, 14)
                var three = macAddrRev.substring(9, 11)
                var four = macAddrRev.substring(6, 8)
                var five = macAddrRev.substring(3, 5)
                var six = macAddrRev.substring(0, 2)
                var macAddress = one + ":" + two + ":" + three + ":" + four + ":" + five + ":" + six

                if (mService != null) {
                    mDevice =
                        BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress.trim()) //"CC:A1:D7:E1:77:B9"
                    mService!!.connect(macAddress.trim())//"CC:A1:D7:E1:77:B9"
                }
//                showCustomDialog(mNFCTagType, mNFCTagInfo)
            }
        }
    }

    fun showCustomDialog(nfcTagType: String, nfcTagInfo: String) {

        val dialog = Dialog(this, R.style.DialogSlideAnim)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            dialog.requestWindowFeature(Window.FEATURE_SWIPE_TO_DISMISS)
        }
        dialog.setContentView(R.layout.dialog_nfc)

        val title = dialog.findViewById(R.id.dialog_title) as TextView

        val textMessage = dialog.findViewById(R.id.message) as TextView
        val mNFCTagInfo = dialog.findViewById(R.id.id_tag_info) as TextView
        val mNFCTagType = dialog.findViewById(R.id.id_tag_type) as TextView

        val button = dialog.findViewById(R.id.dialog_button) as Button
        var titleText = "NFC Tag Details"

        mNFCTagType.text = nfcTagType
        mNFCTagInfo.text = nfcTagInfo
        title.text = titleText

        //"CC:A1:D7:E1:77:B9"
        //"B9 77 E1 D7 A1 CC"
        // 01234567890123456

        var macAddrRev = nfcTagInfo.removeRange(0, 3).removeRange(17, 23)
        var one = macAddrRev.substring(15).trim()
        var two = macAddrRev.substring(12, 14)
        var three = macAddrRev.substring(9, 11)
        var four = macAddrRev.substring(6, 8)
        var five = macAddrRev.substring(3, 5)
        var six = macAddrRev.substring(0, 2)
        var macAddress = one + ":" + two + ":" + three + ":" + four + ":" + five + ":" + six

        mNFCTagInfo.text = nfcTagInfo.removeRange(0, 3) + "\n" + macAddress

        button.setOnClickListener {

            if (mService != null) {
                mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress.trim()) //"CC:A1:D7:E1:77:B9"
                mService!!.connect(macAddress.trim())//"CC:A1:D7:E1:77:B9"
            }
            dialog.dismiss()
        }

        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(true)
        dialog.show()

        val window = dialog.getWindow()
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onNewIntent(intent: Intent) {
        // also reading NFC message from here in case this activity is already started in order
        // not to start another instance of this activity
        super.onNewIntent(intent)
        receiveMessageFromDevice(intent)
    }

    private fun enableForegroundDispatch(activity: AppCompatActivity, adapter: NfcAdapter?) {

        // here we are setting up receiving activity for a foreground dispatch
        // thus if activity is already started it will take precedence over any other activity or app
        // with the same intent filters

        val intent = Intent(activity.applicationContext, activity.javaClass)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        val pendingIntent = PendingIntent.getActivity(activity.applicationContext, 0, intent, 0)

        val filters = arrayOfNulls<IntentFilter>(1)
        val techList = arrayOf<Array<String>>()

        filters[0] = IntentFilter()
        with(filters[0]) {
            this?.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED)
            this?.addCategory(Intent.CATEGORY_DEFAULT)
            try {
                this?.addDataType("*/*")
            } catch (ex: IntentFilter.MalformedMimeTypeException) {
                throw RuntimeException("Check your MIME type")
            }
        }

        adapter?.enableForegroundDispatch(activity, pendingIntent, filters, techList)
    }

    private fun disableForegroundDispatch(activity: AppCompatActivity, adapter: NfcAdapter?) {
        adapter?.disableForegroundDispatch(activity)
    }

    fun togglePDFVisibility(visible: Boolean) {
        if (visible) {
            pdfView!!.visibility = View.VISIBLE
            mLogLayout.visibility = View.GONE
        } else {
            pdfView!!.visibility = View.GONE
            mLogLayout.visibility = View.VISIBLE
        }
    }

    private var hasFileCreated = false
    fun createPDF(dataArray: ByteArray) {
        val pdfFile: File? = try {
            createNewFile()
        } catch (ex: IOException) {
            // Error occurred while creating the File
            showToast("Error occurred while creating the pdf File")
            null
        }
        // Continue only if the File was successfully created
        pdfFile?.also {
            try {
                //write the bytes in file
                val fos = FileOutputStream(pdfFile)
                fos.write(dataArray)
                fos.flush()
                fos.close()
                hasFileCreated = true

                //Upload File to Server
//                uploadImageToBackend(pdfFile)
                pdfFile.forEachLine {
                    println(it)
                }
                showToast("Creation of pdf File complete")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private var currentFilePath: String = ""
    @Throws(IOException::class)
    private fun createNewFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        return File.createTempFile(
            "PDF_${timeStamp}_", /* prefix */
            ".txt", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentFilePath = absolutePath
        }
    }

    private fun service_init() {
        val bindIntent = Intent(this, UartService::class.java)
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE)

        LocalBroadcastManager.getInstance(this).registerReceiver(
            UARTStatusChangeReceiver,
            makeGattUpdateIntentFilter()
        )
    }

    public override fun onStart() {
        super.onStart()
    }

    public override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy()")

        unbindService(mServiceConnection)
        mService!!.stopSelf()
        mService = null
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        super.onStop()

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver)
        } catch (ignore: Exception) {
            Log.e(TAG, ignore.toString())
        }
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
        if (nfcAdapter != null)
            disableForegroundDispatch(this, this.nfcAdapter)
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "onRestart")
    }

    public override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")

        if (nfcAdapter != null) {
            enableForegroundDispatch(this, this.nfcAdapter)
            receiveMessageFromDevice(intent)
        }

        if (!mBtAdapter!!.isEnabled) {
            Log.i(TAG, "onResume - BT not enabled yet")
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(
                enableIntent,
                REQUEST_ENABLE_BT
            )
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {

            REQUEST_SELECT_DEVICE ->
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE)
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress)

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService)
                    (findViewById<View>(R.id.deviceName) as TextView).text = mDevice!!.name + " - connecting"
                    mService!!.connect(deviceAddress)


                }
            REQUEST_ENABLE_BT ->
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show()
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled")
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show()
//                    finish()
                }
            else -> Log.e(TAG, "wrong request code")
        }
    }

    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {

    }


    private fun showMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    }

    override fun onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            val startMain = Intent(Intent.ACTION_MAIN)
            startMain.addCategory(Intent.CATEGORY_HOME)
            startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(startMain)
            showMessage("nRFUART's running in background.\n Disconnect to exit")
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private val REQUEST_SELECT_DEVICE = 1
        private val REQUEST_ENABLE_BT = 2
        private val UART_PROFILE_READY = 10
        val TAG = "nRFUART"
        private val UART_PROFILE_CONNECTED = 20
        private val UART_PROFILE_DISCONNECTED = 21
        private val STATE_OFF = 10

        private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(UartService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE)
            intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART)
            return intentFilter
        }
    }
}
