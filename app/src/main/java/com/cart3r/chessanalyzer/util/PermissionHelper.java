package com.cart3r.chessanalyzer.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

public final class PermissionHelper {
    private PermissionHelper() {}
    public static boolean canDrawOverlays(Context c) {
        return Settings.canDrawOverlays(c);
    }
    public static void requestOverlayPermission(Context c) {
        Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + c.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
    }
}
