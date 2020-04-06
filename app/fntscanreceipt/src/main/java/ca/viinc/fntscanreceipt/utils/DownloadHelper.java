package ca.viinc.fntscanreceipt.utils;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;

import ca.viinc.ereceiptpro.R;

public class DownloadHelper {

//    new DownloadFile().execute(fileUrl, fileName);
    String fname;
    String url;

    DownloadHelper(String url, String fname){
        this.url = url;
        this.fname = fname;
    }


     static class DownloadFile extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            String fileUrl = strings[0];   // -> http://maven.apache.org/maven-1.x/maven.pdf
            String fileName = strings[1];  // -> maven.pdf
            String extStorageDirectory = Environment.DIRECTORY_DOWNLOADS;
            File folder = new File(extStorageDirectory, "FNT");
            folder.mkdir();

            File pdfFile = new File(folder, fileName);

            try{
                pdfFile.createNewFile();
            }catch (IOException e){
                e.printStackTrace();
            }
//       FileDownloader.downloadFile(fileUrl, pdfFile);
            return null;
        }
    }
    public static String getFileDirPrivate(Context context) {

        File myDirectory = new File(context.getFilesDir(), context.getString(R.string.app_name).replace(" ", "_")); // // TODO: 4/9/2016 store file
        File file = null;

        if (!myDirectory.exists()) {
            myDirectory.mkdirs();
            try {
                 file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return myDirectory.getAbsolutePath() + "/";
    }
    public  static void copy(File src, File dst) throws IOException {
        FileInputStream inStream = new FileInputStream(src);
        FileOutputStream outStream = new FileOutputStream(dst);
        FileChannel inChannel = inStream.getChannel();
        FileChannel outChannel = outStream.getChannel();
        inChannel.transferTo(0, inChannel.size(), outChannel);
        inStream.close();
        outStream.close();
    }

    public static boolean isVideoFile(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("video");
    }
//    public static  boolean download(String fileUrl, File directory, Context baseContext){
//        Uri downloadUri = Uri.parse(fileUrl);
//
//        final DownloadManager.Request request = new DownloadManager.Request(downloadUri);
//        request.addRequestHeader(IPreferenceConstants.CDN_COOKIE, getCDNConfig());
//
//        request.setTitle(directory.getName());
//        request.setDescription("reciept");
//// request.allowScanningByMediaScanner();
//        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
////request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, coFile.getFileid());
//        request.setDestinationInExternalFilesDir(baseContext, getFileDirPrivate(baseContext), directory.getAbsolutePath());
//// request.setVisibleInDownloadsUi(false);
//        final DownloadManager manager = (DownloadManager) baseContext.getSystemService(Context.DOWNLOAD_SERVICE);
//        final long downloadId = manager.enqueue(request);
//    }



    public static class FileDownloader {
        private static final int  MEGABYTE = 1024 * 1024;

        public static boolean downloadFile(String fileUrl, String directory, Context baseContext, String receiptId){
            try {

                URL url = new URL(fileUrl);
//
                DownloadManager mgr = (DownloadManager)baseContext.getSystemService(Context.DOWNLOAD_SERVICE);

                Uri downloadUri = Uri.parse(fileUrl);
                DownloadManager.Request request = new DownloadManager.Request(downloadUri);
                File file = baseContext.getExternalFilesDir("/FNT");
                if(file!= null) {
                    File nFile = new File(file.getAbsolutePath() + "/"+receiptId+"reciept.pdf");
                    if (!nFile.exists()) {
                        request.setAllowedNetworkTypes(
                                DownloadManager.Request.NETWORK_WIFI
                                        | DownloadManager.Request.NETWORK_MOBILE)
                                .setAllowedOverRoaming(false).setTitle("Reciept")
                                .setDescription("Bill")
                                .setDestinationUri(Uri.fromFile(nFile));
//                            .setDestinationInExternalFilesDir(baseContext, "/FNT", "reciept.pdf");
//                        .setDestinationInExternalPublicDir("/dhaval_files", "lecture3.pdf");

                        mgr.enqueue(request);
                    }



                    return true;
                }else return false;
            } /*catch (FileNotFoundException e) {
                e.printStackTrace();
            }*/ catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }
}
