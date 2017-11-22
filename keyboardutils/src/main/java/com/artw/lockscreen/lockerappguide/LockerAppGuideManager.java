package com.artw.lockscreen.lockerappguide;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import com.ihs.app.analytics.HSAnalytics;
import com.ihs.app.framework.HSApplication;
import com.ihs.app.utils.HSMarketUtils;
import com.ihs.commons.config.HSConfig;
import com.ihs.keyboardutils.R;
import com.ihs.keyboardutils.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by jixiang on 17/11/17.
 */

public class LockerAppGuideManager {
    public static final String FLURRY_ALERT_OPEN_APP = "alertOpenApp";
    public static final String FLURRY_ALERT_WALL_PAPER = "alertWallpaper";
    public static final String FLURRY_ALERT_UNLOCK = "alertUnlock";
    public static final String FLURRY_ALERT_FROM_LOCKER = "alertFromLocker";
    public static final String FLURRY_SET_AS_LOCK_SCREEN = "setAsLockScreenButton";

    private static final LockerAppGuideManager ourInstance = new LockerAppGuideManager();
    private String lockerAppPkgName = "";
    private boolean shouldGuideToLockerApp = false;
    private String lockerAppInstalledFrom = "";

    public static LockerAppGuideManager getInstance() {
        return ourInstance;
    }

    private boolean isLockerInstall = false;
    private List<ILockerInstallStatusChangeListener> lockerInstallStatusChangeListeners;


    private LockerAppGuideManager() {
    }

    public void init(String pkgName, boolean shouldGuideToLockerApp) {
        isLockerInstall = CommonUtils.isPackageInstalled(pkgName);
        if (!isLockerInstall) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
            intentFilter.addDataScheme("package");

            PackageInstallReceiver packageInstallReceiver = new PackageInstallReceiver(pkgName);
            HSApplication.getContext().registerReceiver(packageInstallReceiver, intentFilter);

            intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_USER_PRESENT);

            UnlockScreenReceiver unlockScreenReceiver = new UnlockScreenReceiver();
            HSApplication.getContext().registerReceiver(unlockScreenReceiver, intentFilter);
        }
        lockerAppPkgName = pkgName;
        this.shouldGuideToLockerApp = shouldGuideToLockerApp;
    }

    public void addLockerInstallStatusChangeListener(ILockerInstallStatusChangeListener lockerInstallStatusChangeListener) {
        if (lockerInstallStatusChangeListeners == null) {
            lockerInstallStatusChangeListeners = new ArrayList<>();
        }
        lockerInstallStatusChangeListeners.add(lockerInstallStatusChangeListener);
    }

    public void removeLockerInstallStatusChangeListener(ILockerInstallStatusChangeListener lockerInstallStatusChangeListener) {
        if (lockerInstallStatusChangeListeners != null) {
            lockerInstallStatusChangeListeners.remove(lockerInstallStatusChangeListener);
        }
    }

    private void setLockerInstall() {
        isLockerInstall = true;
        if (lockerInstallStatusChangeListeners != null) {
            for (ILockerInstallStatusChangeListener lockerInstallStatusChangeListener : lockerInstallStatusChangeListeners) {
                lockerInstallStatusChangeListener.onLockerInstallStatusChange();
            }
        }
        HSAnalytics.logEvent("googlePlay_smartLocker_installed", "googlePlay_smartLocker_installed", lockerAppInstalledFrom);
    }

    public boolean isLockerInstall() {
        return isLockerInstall;
    }

    public boolean isShouldGuideToLockerApp() {
        return shouldGuideToLockerApp;
    }

    public boolean shouldGuideToDownloadLocker() {
        return !isLockerInstall && shouldGuideToLockerApp;
    }


    public void downloadOrRedirectToLockerApp(String from) {
        if (isLockerInstall) {
            openApp(lockerAppPkgName);
        } else {
            HSMarketUtils.browseAPP(lockerAppPkgName);
            lockerAppInstalledFrom = from;
        }
    }

    public void showDownloadLockerAlert(Context context, String from) {
        Map<String, ?> alertMap = null;
        switch (from) {
            case FLURRY_ALERT_FROM_LOCKER:
                alertMap = HSConfig.getMap("Application", "DownloadScreenLocker", "UnlockScreen");
                break;
            case FLURRY_ALERT_OPEN_APP:
                alertMap = HSConfig.getMap("Application", "DownloadScreenLocker", "AppOpen");
                break;
            case FLURRY_ALERT_UNLOCK:
                alertMap = HSConfig.getMap("Application", "DownloadScreenLocker", "UnlockForFree");
                break;
            case FLURRY_ALERT_WALL_PAPER:
                alertMap = HSConfig.getMap("Application", "DownloadScreenLocker", "Wallpaper");
                break;
        }
        if (alertMap.size() < 1) {
            return;
        }
        LockerGuideAlertBean bean = new LockerGuideAlertBean((String) alertMap.get("title"), (String) alertMap.get("body"), (String) alertMap.get("button"));
        CustomDesignAlert lockerDialog = new CustomDesignAlert(context);
        lockerDialog.setTitle(bean.getTitle());
        lockerDialog.setMessage(bean.getBody());
        lockerDialog.setImageResource(R.drawable.enable_tripple_alert_top_image);//locker image
        lockerDialog.setCancelable(true);
        lockerDialog.setPositiveButton(bean.getButton(), view -> {
            HSMarketUtils.browseAPP(lockerAppPkgName);
            lockerAppInstalledFrom = from;
            HSAnalytics.logEvent("app_lockerAlert_button_clicked", "app_lockerAlert_button_clicked", from);
        });
        lockerDialog.show();
        HSAnalytics.logEvent("app_lockerAlert_show", "app_lockerAlert_show", from);
    }

    private static class PackageInstallReceiver extends BroadcastReceiver {
        private String pkgName;

        private PackageInstallReceiver(String pkgName) {
            this.pkgName = pkgName;
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final String packageName = intent.getData().getEncodedSchemeSpecificPart();
            if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                if (pkgName.endsWith(packageName)) {
                    LockerAppGuideManager.getInstance().setLockerInstall();
                }
            }
        }
    }

    private static class UnlockScreenReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!LockerAppGuideManager.getInstance().isLockerInstall) {
                Context ctx = HSApplication.getContext();
                boolean downloadLockerAlert = HSConfig.optBoolean(false, "Application", "DownloadScreenLocker", "UnlockScreen", "ShowUnlockScreenAlert");
                if (downloadLockerAlert) {
                    int alertIntervalInHour = HSConfig.optInteger(24, "Application", "DownloadScreenLocker", "UnlockScreen", "AlertIntervalInHour");
                    long lastShowDownloadLockerAlertTime = PreferenceManager.getDefaultSharedPreferences(ctx).getLong("lastShowDownloadLockerAlertTime", 0);
                    if (System.currentTimeMillis() - lastShowDownloadLockerAlertTime > alertIntervalInHour * 60 * 60 * 1000) {
                        PreferenceManager.getDefaultSharedPreferences(ctx).edit().putLong("lastShowDownloadLockerAlertTime", System.currentTimeMillis()).apply();
                        LockerAppGuideManager.getInstance().showDownloadLockerAlert(ctx, FLURRY_ALERT_FROM_LOCKER);
                    }
                }
            }
        }
    }

    public interface ILockerInstallStatusChangeListener {
        void onLockerInstallStatusChange();
    }

    public static boolean openApp(String packageName) {
        PackageManager manager = HSApplication.getContext().getPackageManager();
        try {
            Intent i = manager.getLaunchIntentForPackage(packageName);
            if (i == null) {
                return false;
                //throw new ActivityNotFoundException();
            }
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            HSApplication.getContext().startActivity(i);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }


}