package com.android.server.permissions;

import android.content.Context;
import android.content.Intent;

public class PermissionsManager {

    public void getPermissions(String packageName, Context context) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.GRANT_PERMISSION_ACTION");
        intent.putExtra("PackageName", packageName ); 
        context.sendBroadcast(intent);
    }
}
