package ca.viinc.fntscanreceipt.utils;

import android.app.Activity;

import com.tbruyelle.rxpermissions2.RxPermissions;

public class PermissionHelper {
   static Boolean value= false;
     public static Boolean askPermission(String[] perm, Activity activity){
         RxPermissions rxPermissions = new RxPermissions(activity);
         if (perm != null && perm.length > 0) {
              rxPermissions
                     .request(perm)
                     .subscribe(permission -> {
                       if (permission) {
                           value = true;
                       } else {
                           value = false;
                       }
                   });
              return value;
         } else {
return true;
         }
     }
}
