package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PermissionInfo;
import android.util.Log;

final class PermissionService extends SystemService {

    private static final String TAG = "PermissionService";
    private PackageManagerInternal mServiceInternal;
      
    public PermissionService(Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        publishLocalService(PermissionService.class, this);
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_ACTIVITY_MANAGER_READY) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.GRANT_PERMISSION_ACTION");
            getContext().registerReceiver(mReceiver, intentFilter);
        }
    }
        
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            goAsync();
            String packageName = intent.getStringExtra("PackageName");
            PackageInfo packageInfo;
            String[] permissionsList;
                
            mServiceInternal = LocalServices.getService(PackageManagerInternal.class);
            
            try {
                packageInfo = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
                permissionsList = packageInfo.requestedPermissions;
                for(String p: permissionsList){
                    PermissionInfo permissionInfo = context.getPackageManager().getPermissionInfo(p,0);
                    int basePermissionType = permissionInfo.getProtection();
                    int permissionFlags = permissionInfo.getProtectionFlags();
                    
                    if(isRuntime(basePermissionType) || isDevelopment(basePermissionType, permissionFlags)){
                        Log.d(TAG, "Grant " + p + " permission to " + packageName);
                        mServiceInternal.grantRuntimePermission(packageName, p, 0, true);
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    };
    
    public boolean isRuntime(int basePermissionType) {
        return basePermissionType == PermissionInfo.PROTECTION_DANGEROUS;
    }
    public boolean isSignature(int basePermissionType) {
        return basePermissionType == PermissionInfo.PROTECTION_SIGNATURE;
    }

    public boolean isDevelopment(int basePermissionType, int permissionFlags) {
        return isSignature(basePermissionType) && (permissionFlags & PermissionInfo.PROTECTION_FLAG_DEVELOPMENT) != 0;
    }
    
}
