package ca.viinc.fntscanreceipt

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.app.Activity
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.graphics.Bitmap
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.forEach
import androidx.localbroadcastmanager.content.LocalBroadcastManager

import kotlinx.android.synthetic.main.activity_main.*

import java.io.*
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Created by Goutham Iyyappan on 20/05/2019 for Apeirogon Labs Pvt Ltd, India
 * Home screen that offers core features and base navigation structure.
 */
class MainViewActivity : BaseActivity(), BaseActivity.CustomHandler {


    companion object {
        const val MIME_TEXT_PLAIN = "*/*"

        val REQUEST_IMAGE_CAPTURE = 1
        private val REQUEST_ENABLE_BT = 2
        private val UART_PROFILE_CONNECTED = 20
        private val UART_PROFILE_DISCONNECTED = 21

        private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(UartService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE)
            intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART)
            return intentFilter
        }

        private fun byteArrayToString(ba: ByteArray): String {
            var hex = StringBuilder(ba.size * 2)

            for (b in ba)
                hex.append("${String.format("%02X", b)} ")

            return hex.toString()
        }


        class AdRecord {

            constructor(length: Int, type: Int, data: ByteArray) {
                var decodedRecord = ""

                try {
                    decodedRecord = String(data, Charset.forName("UTF-8"))

                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
                Log.d(
                    "DEBUG",
                    "Length: " + length + " Type : " + type + " Data : " + byteArrayToString(data)
                )
            }

            companion object {
                fun parseScanRecord(scanRecord: ByteArray): List<AdRecord> {
                    var records = ArrayList<AdRecord>()
                    var index = 0
                    var MSD_FLAG: Int = 0xFF
                    while (index < scanRecord.size) {
                        var length = scanRecord[index++].toInt() and 0xFF
                        if (length.equals(0)) break

                        var type = scanRecord[index].toInt() and 0xFF
                        if (type.equals(0)) break

                        if (type == MSD_FLAG) {
                            var data = Arrays.copyOfRange(scanRecord, index, length)
                            Log.d(
                                "DEBUG",
                                "MSD FLAG --- Length: " + length + " Type : " + type + " Data : " + byteArrayToString(
                                    data
                                )
                            )
                        }

                        var data = Arrays.copyOfRange(scanRecord, index + 1, index + length)
                        records.add(AdRecord(length, type, data))
                    }
                    return records
                }
            }

        }
    }

    private val TAG = MainViewActivity.javaClass.simpleName
    private lateinit var mBLEStatusTV: TextView
    private var nfcAdapter: NfcAdapter? = null
    private var currentPhotoPath: String = ""
    private var isNfcSupported: Boolean = false


    private var mService: UartService? = null
    private var mBtAdapter: BluetoothAdapter? = null
    private var mState = UART_PROFILE_DISCONNECTED
    private val mDeletionHandler: CustomHandler = this

    var mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, rawBinder: IBinder) {
            mService = (rawBinder as UartService.LocalBinder).service
            Log.d(TAG, "onServiceConnected mService= " + mService!!)
            if (!mService!!.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth")
                showToast("Unable to initialize Bluetooth")
            }
        }

        override fun onServiceDisconnected(classname: ComponentName) {
            ////     mService.disconnect(mDevice);
            mService = null
            Log.w(TAG, "Service disconnected: ${classname.className}")
        }
    }

    fun hideConnectivityStatus() {
        var listener = object : AnimatorListener {
            override fun onAnimationEnd(p0: Animator?) {
//                super.onAnimationEnd(p0)
                connection_status_layout.visibility = View.GONE
                progress_view.visibility = View.GONE
                id_progress_status.text = ""
            }

            override fun onAnimationRepeat(p0: Animator?) {
            }

            override fun onAnimationCancel(p0: Animator?) {

            }

            override fun onAnimationStart(p0: Animator?) {
            }
        }

        var transY = connection_status_layout.height.toFloat()

        connection_status_layout.animate()
            .alpha(0.0f)
            .translationY(-transY)
            .setDuration(2000)
            .setListener(listener)
    }

    fun showConnectivityStatus() {
        var listener = object : AnimatorListener {
            override fun onAnimationEnd(p0: Animator?) {
                progress_view.visibility = View.VISIBLE
                connection_status_layout.visibility = View.VISIBLE
            }

            override fun onAnimationRepeat(p0: Animator?) {
            }

            override fun onAnimationCancel(p0: Animator?) {
            }

            override fun onAnimationStart(p0: Animator?) {
            }
        }

        connection_status_layout.animate()
            .alpha(1.0f)
            .translationY(0f)
            .setListener(listener)
    }


    private var isFilePDF = false
    private var isFirstByteArray = true
    private val UARTStatusChangeReceiver = object : BroadcastReceiver() {
        private var mBLEDataArray: ByteArray? = null
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            //*********************//
            if (action == UartService.ACTION_GATT_CONNECTED) {
                runOnUiThread {
                    Log.d(TAG, "UART_CONNECT_MSG")
                    mState = UART_PROFILE_CONNECTED
                    showConnectivityStatus()
                        mBLEStatusTV.text = resources.getString(R.string.connected_home)
                        id_progress_status.text = resources.getString(R.string.waiting_for_pos)
                }
            }

            //*********************//
            if (action == UartService.ACTION_GATT_DISCONNECTED) {
                runOnUiThread {
                    Log.d(TAG, "UART_DISCONNECT_MSG")
                    mState = UART_PROFILE_DISCONNECTED
                    mService!!.close()
                    mBLEStatusTV.text = resources.getString(R.string.disconnected_home)
                    hideConnectivityStatus()
                }
            }

            //*********************//
            if (action == UartService.ACTION_GATT_SERVICES_DISCOVERED) {
                mService!!.enableTXNotification()
            }
            //*********************//
            if (action == UartService.ACTION_DATA_AVAILABLE) {
                id_progress_status.text = "Receiving receipt information..."
                hasFileCreated = false
                val txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA)
                if (txValue != null && txValue.size > 0 && isFirstByteArray) {
                    var filetypeStr = txValue
                    if (txValue.size > 8)
                        filetypeStr = txValue.copyOfRange(0, 8)

                    isFilePDF =
                        filetypeStr.toString(Charset.forName("UTF-8")).toLowerCase().contains("pdf")
                    isFirstByteArray = false
                }

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

                        val size = mBLEDataArray!!.size
                        val eofArray = mBLEDataArray!!.copyOfRange(size - 4, size)
                        if (eofArray.toString(Charset.forName("UTF-8")).toLowerCase().contains("eof")) {
                            if (!hasFileCreated) {
                                val finalFileDataArray = mBLEDataArray!!.dropLast(3).toByteArray()
                                if (finalFileDataArray.size > 5) {
                                    createPDF(mBLEDataArray!!.dropLast(3).toByteArray())
                                    showToast("File Transfer Complete...")
                                }
                            }
                            id_progress_status.text = "Receipt information Received Successfully..."
                            if (mService != null) mService!!.disconnect()
                            mBLEDataArray = null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, e.toString())
                        id_progress_status.text = "Receipt information parsing error..."
                        if (mService != null) mService!!.disconnect()
                        mBLEDataArray = null
                    }
                }
            }
            //*********************//
            if (action == UartService.DEVICE_DOES_NOT_SUPPORT_UART) {
                showToast("Device doesn't support UART. Disconnecting")
                mService!!.disconnect()
            }
        }
    }

    fun combineByteArray(array1: ByteArray, array2: ByteArray): ByteArray {
        val newArray = ByteArray(array1.size + array2.size)
        System.arraycopy(array1, 0, newArray, 0, array1.size)
        System.arraycopy(array2, 0, newArray, array1.size, array2.size)
        return newArray
    }

    private var hasFileCreated = false
    private fun createPDF(dataArray: ByteArray) {
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
                showToast("Creation of pdf File complete")

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    //    private var currentFilePath: String = ""
    @Throws(IOException::class)
    private fun createNewFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val fileType = if (isFilePDF) ".pdf" else ".txt"
        return File.createTempFile(
            "eReceipt_${timeStamp}_", /* prefix */
            fileType, /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            var currentFilePath = absolutePath
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














    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setStatusBarColorSolid()

        mBtAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBtAdapter == null) {
            showToast("Bluetooth is not available")
            return
        }

        mBLEStatusTV = findViewById(R.id.id_connect_status)

        mHandler = Handler()
        deviceList = ArrayList()
        devRssiValues = HashMap()

        service_init()

        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this)?.let { it }
        checkNFC()


        invalidateOptionsMenu()



    }

    fun checkNFC() {
        isNfcSupported = this.nfcAdapter != null

        if (!isNfcSupported) {
            Toast.makeText(this, "Nfc is not supported on this device", Toast.LENGTH_SHORT).show()
            fab.visibility = View.GONE
        }

        if (isNfcSupported && nfcAdapter != null && !nfcAdapter?.isEnabled!!) {
            showEnableNFCDialog()
        }
    }






    override fun onResume() {
        super.onResume()
        // foreground dispatch should be enabled here, as onResume is the guaranteed place where app
        // is in the foreground
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
        dismissProgress()
    }

    override fun onPause() {
        super.onPause()
        if (nfcAdapter != null)
            disableForegroundDispatch(this, this.nfcAdapter)
        scanLeDevice(false)
    }

    public override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy()")

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver)
        } catch (ignore: Exception) {
            Log.e(TAG, ignore.toString())
        }

        unbindService(mServiceConnection)
        mService!!.stopSelf()
        mService = null
    }

    var isBackPressed = false
    override fun onBackPressed() {
        val fragmentCount = supportFragmentManager.backStackEntryCount
        if (fragmentCount <= 1) {
            if (isBackPressed) {
                if (mState == UART_PROFILE_CONNECTED) {
                    val startMain = Intent(Intent.ACTION_MAIN)
                    startMain.addCategory(Intent.CATEGORY_HOME)
                    startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(startMain)
                    if (mService != null) mService!!.disconnect()
                } else {
                    finish()
                }
            } else {
                isBackPressed = true
                showToast("Press Back again to exit app.")
            }
        } else {
            // Activities or Fragments available in backstack
            super.onBackPressed()
        }
    }







    private val READ_REQUEST_CODE: Int = 42
    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    fun performFileSearch() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            // Filter to only show results that can be "opened", such as a
            // file (as opposed to a list of contacts or timezones)
            addCategory(Intent.CATEGORY_OPENABLE)

            // Filter to show only images, using the image MIME data type.
            // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
            // To search for all documents available via installed storage providers,
            // it would be "*/*".
            type = "*/*"
        }

        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when (requestCode) {
            REQUEST_IMAGE_CAPTURE ->
                if (resultCode == RESULT_OK) {
                    val file = File(currentPhotoPath)
                    val bitmap = MediaStore.Images.Media
                        .getBitmap(mContext.contentResolver, Uri.fromFile(file))
                    if (bitmap != null) {
                        val photoFile: File? = try {
                            createImageFile()
                        } catch (ex: IOException) {
                            // Error occurred while creating the File
                            null
                        }
                        // Continue only if the File was successfully created
                        photoFile?.also {
                            val bos = ByteArrayOutputStream()
                            bitmap.compress(
                                Bitmap.CompressFormat.JPEG,
                                70/*ignored for PNG*/,
                                bos
                            )
                            val bitmapdata = bos.toByteArray()
                            try {
                                //write the bytes in file
                                val fos = FileOutputStream(photoFile)
                                fos.write(bitmapdata)
                                fos.flush()
                                fos.close()
                                hasFileCreated = true

                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                    }
                } else {
                    super.onActivityResult(requestCode, resultCode, data)

                }

            REQUEST_ENABLE_BT ->
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show()

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled")
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show()
                }
            READ_REQUEST_CODE ->
                if (resultCode == Activity.RESULT_OK) {
                    // The document selected by the user won't be returned in the intent.
                    // Instead, a URI to that document will be contained in the return intent
                    // provided to this method as a parameter.
                    // Pull that URI using resultData.getData().
                    data?.data?.also { uri ->
                        Log.i(TAG, "Uri: $uri")
                        val mimetype = getMimeType(uri)
                        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                        var fileType: String = ".txt"
                        when (mimetype) {
                            "application/pdf" -> fileType = ".pdf"
                            "application/x-pdf" -> fileType = ".pdf"
                            "text/plain" -> fileType = ".txt"
                            "image/jpeg" -> fileType = ".jpg"
                            "image/png" -> fileType = ".png"
                            "image/tiff" -> fileType = ".tiff"
                        }

                        val file = File.createTempFile(
                            "eReceipt_${timeStamp}_", /* prefix */
                            fileType, /* suffix */
                            storageDir /* directory */
                        ).also {
                            try {
                                //write the bytes in file
                                val iStream = contentResolver.openInputStream(uri)
                                var byteArray = iStream?.readBytes()

                                val fos = FileOutputStream(it)
                                fos.write(byteArray)
                                fos.flush()
                                fos.close()
                                hasFileCreated = true

                                //Upload File to Server
//                                uploadImageToBackend(it)
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

            else -> Log.e(TAG, "wrong request code")

        }
    }

    fun getMimeType(uri: Uri): String? {
        var mimeType: String? = null
        if (!uri.scheme.isNullOrEmpty() && uri.scheme!!.equals(ContentResolver.SCHEME_CONTENT)) {
            val cr = mContext.contentResolver
            mimeType = cr.getType(uri)
        } else {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())

            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                fileExtension.toLowerCase()
            )
        }
        Log.i(TAG, "MimeType: $mimeType")
        return mimeType;
    }

//    private fun uploadImageToBackend(file: File) {
//        if(isVideoFile(file.path)){
//           return
//        }
//        showProgress("Uploading File...")
//
//        val serverURL = BaseApplication.PHP_UPLOAD_RECEIPT_URL
//        try {
//            val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
//                .addFormDataPart(
//                    "file",
//                    file.name,
//                    file.toRequestBody("application/pdf".toMediaTypeOrNull())
//                )
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
//                    dismissProgress()
//                }
//
//                override fun onResponse(call: Call, response: Response) {
//                    dismissProgress()
//                    if (response.body != null) {
//                        try {
//                            val jsonResponse = JSONObject(response.body!!.string())
//                            if (jsonResponse.getBoolean("status")) {
//                                hasFileCreated = false
//                                var receiptId = jsonResponse.getJSONObject("data")
//                                    .getJSONObject("receipt_details")
//                                    .getString("receipt_id")
//                                runOnUiThread {
//                                    id_progress_status.text = jsonResponse.getString("message")
//                                    showToast(jsonResponse.getString("message"))
//                                    getAllReceipts(null)
//                                }
//                            } else {
//                                runOnUiThread {
//                                    showToast(jsonResponse.getString("message"))
//                                }
//                            }
//                        } catch (e: JsonParseException) {
//                            hasFileCreated = false
//                            Utils.printStackTrace(e)
//                        }
//                    }
//                }
//            })
//
//        } catch (e: Exception) {
//            Utils.printStackTrace(e)
//            dismissProgress()
//        }
//    }

    var lastConnectedBLEMAC: String? = ""
    var mNFCManufacturerSpecificData: String? = ""
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

                //MF data: enB9 77 E1 D7 A1 CC 49 81
                //enB9 77 E1 D7 A1 CC 49 81 //enB4 87 2F 62 C9 1D 85 E7
                mNFCManufacturerSpecificData = inMessage.removeRange(0, 3).trim()
                var macAddrRev = inMessage.removeRange(0, 3).removeRange(17, 23)
                var one = macAddrRev.substring(15).trim()
                var two = macAddrRev.substring(12, 14)
                var three = macAddrRev.substring(9, 11)
                var four = macAddrRev.substring(6, 8)
                var five = macAddrRev.substring(3, 5)
                var six = macAddrRev.substring(0, 2)
                var macAddress = one + ":" + two + ":" + three + ":" + four + ":" + five + ":" + six

                lastConnectedBLEMAC = macAddress.trim()
                Log.w(TAG, "Device address from NFC: ${lastConnectedBLEMAC}")
                scanLeDevice(true)
                getIntent().setAction(null)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private val mScanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            Log.d(TAG, "onScanResult: ${result?.device?.address} - ${result?.device?.name}")

            val msd = result?.scanRecord?.manufacturerSpecificData
            Log.d(TAG, "onScanResult: ${result?.device?.address} - ${result?.device?.name}")

            var msdString = ""
            msd?.forEach { key, value ->
                msdString = byteArrayToString(value)
                Log.d("DEBUG", " manufacturerSpecificData : " + msdString)

                if (msdString.contains(mNFCManufacturerSpecificData!!)) {
                    manufacturerSpecificDataString = msdString
                    mDevice = result.device
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            Log.d(TAG, "onBatchScanResults:${results.toString()}")
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d(TAG, "onScanFailed: $errorCode")
        }
    }

    internal lateinit var deviceList: MutableList<BluetoothDevice>
    internal var mDevice: BluetoothDevice? = null
    internal lateinit var devRssiValues: MutableMap<String, Int>
    var manufacturerSpecificDataString = ""

    var itsaMatch = false
    fun connectToDevice() {
        if (manufacturerSpecificDataString.contains(mNFCManufacturerSpecificData!!) && mDevice != null) {
            itsaMatch = true
            if (mService != null) {
                var isConnected = mService!!.connect(mDevice!!.address)//"CC:A1:D7:E1:77:B9"
                if (!isConnected) {
                    id_progress_status.text = "Scan complete..."
                    mBLEStatusTV.text = "eReceipt BLE device connection failed"
                    hideConnectivityStatus()
                }
            } else {
                service_init()
                id_progress_status.text = "Starting BLE services..."
                mBLEStatusTV.text = "Initiating connection"

                if (mScanRepeats < 3)
                    scanLeDevice(true)
                else {
                    id_progress_status.text = "Multiple Scan complete..."
                    mBLEStatusTV.text = "Unable to re-start BLE service"
                    hideConnectivityStatus()
                }
            }
        } else {
            Log.w(TAG, "Device Address does not match.")
            Log.w(TAG, "MSD from NFC: ${mNFCManufacturerSpecificData}")
        }
        if (!itsaMatch) {
            id_progress_status.text = "No matching Device found..."
            mBLEStatusTV.text = "Unable to Find & connect to any nearby eReceipt BLE device"
            hideConnectivityStatus()
            mService!!.close()
            service_init()
            if (mScanRepeats < 3)
                scanLeDevice(true)
            else {
                id_progress_status.text = "Multiple scan complete..."
                mBLEStatusTV.text = "Unable to Find & connect to any nearby eReceipt BLE device"
                hideConnectivityStatus()
            }
        }
    }

    private var mHandler: Handler? = null
    private var mScanning: Boolean = false
    private var mScanRepeats: Int = 0

    private fun scanLeDevice(enable: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (enable) {
                // Stops scanning after a pre-defined scan period.
                mHandler!!.postDelayed({
                    mScanning = false

                    mBtAdapter!!.bluetoothLeScanner.stopScan(mScanCallback)
                    id_progress_status.text = "Scan Complete..."
                    mBLEStatusTV.text = "Connecting to eReceipt BLE Device."
                    connectToDevice()
                }, 3000)

                mScanning = true
                mScanRepeats++
                mBtAdapter!!.bluetoothLeScanner.startScan(mScanCallback)
                id_progress_status.text = "Scan In progress..."
                mBLEStatusTV.text = "Connecting to eReceipt BLE Device."
                showConnectivityStatus()
            } else {
                mScanning = false
                mBtAdapter!!.bluetoothLeScanner.stopScan(mScanCallback)
            }
        }
    }


    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    override fun onNewIntent(intent: Intent) {
        // also reading NFC message from here in case this activity is already started in order
        // not to start another instance of this activity
        super.onNewIntent(intent)
        setIntent(intent)
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
                this?.addDataType(MIME_TEXT_PLAIN)
            } catch (ex: IntentFilter.MalformedMimeTypeException) {
                throw RuntimeException("Check your MIME type")
            }
        }

        adapter?.enableForegroundDispatch(activity, pendingIntent, filters, techList)
    }

    private fun disableForegroundDispatch(activity: AppCompatActivity, adapter: NfcAdapter?) {
        adapter?.disableForegroundDispatch(activity)
    }




    var offset = 0











    val negativelogout = { dialog: DialogInterface, which: Int ->
        dialog.dismiss()
    }

    val positiveButtonClick = { dialog: DialogInterface, which: Int ->
        startActivity(Intent(android.provider.Settings.ACTION_NFC_SETTINGS))
    }
    val negativeButtonClick = { dialog: DialogInterface, which: Int ->
        Toast.makeText(
            applicationContext,
            android.R.string.no, Toast.LENGTH_SHORT
        ).show()
    }

    fun showEnableNFCDialog() {
        val builder = AlertDialog.Builder(this)
        with(builder)
        {
            setTitle("NFC Connection Alert")
            setMessage("Kindly Enable NFC feature from your settings to Tap & receive eReceipts.")
            setPositiveButton(
                android.R.string.yes,
                DialogInterface.OnClickListener(function = positiveButtonClick)
            )
            setNegativeButton(android.R.string.no, negativeButtonClick)
            show()
        }
    }

    override fun onSuccess() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onFailure(message: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}
