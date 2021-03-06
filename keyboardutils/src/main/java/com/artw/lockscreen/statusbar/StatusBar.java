package com.artw.lockscreen.statusbar;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.artw.lockscreen.LockerActivity;
import com.artw.lockscreen.LockerUtils;
import com.ihs.keyboardutils.R;
import com.ihs.feature.common.ScreenStatusReceiver;
import com.artw.lockscreen.common.SystemSettingsManager;
import com.ihs.charging.HSChargingManager;
import com.ihs.commons.notificationcenter.HSGlobalNotificationCenter;
import com.ihs.commons.notificationcenter.INotificationObserver;
import com.ihs.commons.utils.HSBundle;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class StatusBar extends RelativeLayout implements SystemSettingsManager.ISystemSettingsListener
        , INotificationObserver {

    boolean shouldShowWifi = false;
    private boolean isListeningPhoneState = false;

    private TextView tvTime;
    private TextView tvBattery;
    private ImageView ivWifi;
    private TextView tvMobileData;
    private ImageView ivMobileStrength;
    private ImageView ivAirPlane;
    private ImageView ivSound;
    private ImageView ivBatteryCharging;
    private StatusBarBatteryIndicator batteryIndicator;
    private int[] resWifiImages = new int[]{
            R.drawable.status_wifi_strength_1,
            R.drawable.status_wifi_strength_2,
            R.drawable.status_wifi_strength_3
    };
    private int[] resMobileStrengthImages = new int[]{
            R.drawable.status_signal_disable,
            R.drawable.status_signal_strength_1,
            R.drawable.status_signal_strength_2,
            R.drawable.status_signal_strength_3,
            R.drawable.status_signal_strength_4
    };

    private SystemSettingsManager mSystemSettingsManager;

    private HSChargingManager.IChargingListener chargingListener = new HSChargingManager.IChargingListener() {

        @Override public void onBatteryLevelChanged(int i, int i1) {
            updateBattery();
        }

        @Override
        public void onChargingStateChanged(HSChargingManager.HSChargingState preChargingState, HSChargingManager.HSChargingState curChargingState) {
            updateBattery();
        }

        @Override public void onChargingRemainingTimeChanged(int i) {

        }

        @Override public void onBatteryTemperatureChanged(float v, float v1) {

        }
    };

    protected Handler handler = new Handler(Looper.getMainLooper());
    private boolean isReceiverRegistered = false;

    private BroadcastReceiver datetimeUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateTime();
        }
    };

    private ContentObserver dateFormatObserver = new ContentObserver(handler) {
        @Override
        public void onChange(boolean selfChange) {
            updateTime();
        }
    };

    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (!LockerUtils.hasPermission(Manifest.permission.ACCESS_WIFI_STATE)
                    || !LockerUtils.hasPermission(Manifest.permission.CHANGE_WIFI_STATE)) {
                return;
            }
            final WifiManager wManager = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            List<ScanResult> wifiList = wManager.getScanResults();
            if (wifiList == null) {
                return;
            }
            boolean hasConnected = false;
            for (int i = 0; i < wifiList.size(); i++) {
                ScanResult wifi = wifiList.get(i);
                if (wManager.getConnectionInfo() != null && wifi != null && wifi.BSSID != null) {
                    if (wifi.BSSID.equals(wManager.getConnectionInfo().getBSSID())) {
                        hasConnected = true;
                        int signalLevel = WifiManager.calculateSignalLevel(wifi.level, 3);
                        if (signalLevel < 0) {
                            signalLevel = 0;
                        } else if (signalLevel > 2) {
                            signalLevel = 2;
                        }
                        if (shouldShowWifi) {
                            ivWifi.setVisibility(View.VISIBLE);
                            ivWifi.setImageResource(resWifiImages[signalLevel]);
                        }
                    }
                }
            }
            if (!hasConnected) {
                ivWifi.setVisibility(View.GONE);
            }
            if (ScreenStatusReceiver.isScreenOn()) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        wManager.startScan();
                    }
                }, 5 * 1000);
            }
        }
    };

    private PhoneStateListener phoneStateListener = new PhoneStateListener() {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                HSBundle bundle = new HSBundle();
                bundle.putBoolean(LockerActivity.EXTRA_SHOULD_DISMISS_KEYGUARD, false);
                HSGlobalNotificationCenter.sendNotification(LockerActivity.EVENT_FINISH_SELF, bundle);
            }
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            int asu = signalStrength.getGsmSignalStrength();
            if (asu <= 2 || asu == 99)
                ivMobileStrength.setImageResource(resMobileStrengthImages[0]);
            else if (asu >= 12) ivMobileStrength.setImageResource(resMobileStrengthImages[4]);
            else if (asu >= 8) ivMobileStrength.setImageResource(resMobileStrengthImages[3]);
            else if (asu >= 5) ivMobileStrength.setImageResource(resMobileStrengthImages[2]);
            else ivMobileStrength.setImageResource(resMobileStrengthImages[1]);
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            switch (networkType) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    tvMobileData.setText(R.string.locker_2g);
                    break;
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    tvMobileData.setText(R.string.locker_3g);
                    break;
                case TelephonyManager.NETWORK_TYPE_LTE:
                    tvMobileData.setText(R.string.locker_4g);
                    break;
                default:
                    tvMobileData.setText(R.string.locker_unkown);
                    break;
            }
        }
    };

    public StatusBar(Context context, AttributeSet set) {
        super(context, set);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        tvTime = (TextView) findViewById(R.id.tv_time);
        tvBattery = (TextView) findViewById(R.id.tv_battery);
        ivWifi = (ImageView) findViewById(R.id.iv_wifi);
        tvMobileData = (TextView) findViewById(R.id.tv_mobile_data);
        ivMobileStrength = (ImageView) findViewById(R.id.iv_mobile_strength);
        ivAirPlane = (ImageView) findViewById(R.id.iv_airplane);
        ivSound = (ImageView) findViewById(R.id.iv_sound);
        ivBatteryCharging = (ImageView) findViewById(R.id.iv_battery_charging);
        batteryIndicator = (StatusBarBatteryIndicator) findViewById(R.id.battery_indicator);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

//        registerDatetimeReceiver();
        startListenPhoneState();
        HSGlobalNotificationCenter.addObserver(ScreenStatusReceiver.NOTIFICATION_SCREEN_ON, this);
        HSGlobalNotificationCenter.addObserver(ScreenStatusReceiver.NOTIFICATION_SCREEN_OFF, this);

        mSystemSettingsManager = new SystemSettingsManager(getContext());
        mSystemSettingsManager.register(this);

        updateTime();
        updatePlaneOn();
        updateSound();
        updateWifi();
        updateBattery();
        HSChargingManager.getInstance().addChargingListener(chargingListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

//        unregisterDatetimeReceiver();
        stopListenPhoneState();
        HSGlobalNotificationCenter.removeObserver(this);
        if (mSystemSettingsManager != null) {
            mSystemSettingsManager.unRegister();
            mSystemSettingsManager = null;
        }
        HSChargingManager.getInstance().removeChargingListener(chargingListener);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    private void registerDatetimeReceiver() {
        if (!isReceiverRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_TIME_TICK);
            intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
            intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            getContext().registerReceiver(datetimeUpdateReceiver, intentFilter);
            getContext().getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, dateFormatObserver);

            startScanWifi();
            isReceiverRegistered = true;
        }
    }

    private void unregisterDatetimeReceiver() {
        if (isReceiverRegistered) {
            getContext().unregisterReceiver(datetimeUpdateReceiver);
            getContext().getContentResolver().unregisterContentObserver(dateFormatObserver);

            stopScanWifi();
            isReceiverRegistered = false;
        }
    }

    private void updateTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat();
        if (DateFormat.is24HourFormat(getContext())) {
            dateFormat.applyPattern("HH:mm");
        } else {
            dateFormat.applyPattern("hh:mm");
        }
        tvTime.setText(dateFormat.format(calendar.getTime()));
    }

    private void updatePlaneOn() {
        if (mSystemSettingsManager.getSystemSettingsItemState(SystemSettingsManager.SettingsItem.AIRPLANE_MODE) == 1) {
            ivAirPlane.setVisibility(View.VISIBLE);
            ivMobileStrength.setVisibility(View.GONE);
            tvMobileData.setVisibility(View.GONE);
            updateWifi();
        } else {
            ivAirPlane.setVisibility(View.GONE);
            ivMobileStrength.setVisibility(View.VISIBLE);
            updateWifi();
        }
    }

    private void updateSound() {
        if (mSystemSettingsManager.getSystemSettingsItemState(SystemSettingsManager.SettingsItem.RINGMODE) == 0) {
            ivSound.setVisibility(View.VISIBLE);
            ivSound.setImageResource(R.drawable.status_silence);
        } else if (mSystemSettingsManager.getSystemSettingsItemState(SystemSettingsManager.SettingsItem.RINGMODE) == 1) {
            ivSound.setVisibility(View.VISIBLE);
            ivSound.setImageResource(R.drawable.status_vibrate);
        } else {
            ivSound.setVisibility(View.GONE);
        }
    }

    private void updateWifi() {
        if (mSystemSettingsManager.getSystemSettingsItemState(SystemSettingsManager.SettingsItem.AIRPLANE_MODE) == 1) {
            shouldShowWifi = false;
            ivWifi.setVisibility(View.GONE);
        } else {
            int state = mSystemSettingsManager.getSystemSettingsItemState(SystemSettingsManager.SettingsItem.WIFI);
            if (state > 0) {
                shouldShowWifi = true;
                ivWifi.setVisibility(View.VISIBLE);
            } else {
                shouldShowWifi = false;
                ivWifi.setVisibility(View.GONE);
            }
        }
        updateMobileData();
    }

    private void updateBattery() {
        int batteryPercentage = HSChargingManager.getInstance().getBatteryRemainingPercent();
        batteryIndicator.setPercentage(batteryPercentage);
        String batteryText;
        if (batteryPercentage == 100) {
            batteryText = "100";
        } else if (batteryPercentage < 10) {
            batteryText = "  " + batteryPercentage + "% ";
        } else {
            batteryText = batteryPercentage + "% ";
        }
        tvBattery.setText(batteryText);
        if (HSChargingManager.getInstance().isCharging()) {
            ivBatteryCharging.setVisibility(View.VISIBLE);
        } else {
            ivBatteryCharging.setVisibility(View.GONE);
        }
    }

    private void updateMobileData() {
        if (mSystemSettingsManager.getSystemSettingsItemState(SystemSettingsManager.SettingsItem.AIRPLANE_MODE) == 0
                && mSystemSettingsManager.getSystemSettingsItemState(SystemSettingsManager.SettingsItem.MOBILE_DATA) == 1
                && mSystemSettingsManager.getSystemSettingsItemState(SystemSettingsManager.SettingsItem.WIFI) == 0) {
            tvMobileData.setVisibility(View.VISIBLE);
        } else {
            tvMobileData.setVisibility(View.GONE);
        }
    }

    private void startScanWifi() {
        WifiManager wManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wManager.startScan();
        getContext().registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    private void stopScanWifi() {
        getContext().unregisterReceiver(wifiReceiver);
    }

    private void startListenPhoneState() {
        if (!isListeningPhoneState) {
            TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
            isListeningPhoneState = true;
        }
    }

    private void stopListenPhoneState() {
        if (isListeningPhoneState) {
            TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            isListeningPhoneState = false;
        }
    }

    @Override
    public void onSystemSettingsStateChanged(SystemSettingsManager.SettingsItem item, int state) {
        switch (item) {
            case RINGMODE:
                updateSound();
                break;
            case WIFI:
                updateWifi();
                break;
            case AIRPLANE_MODE:
                updatePlaneOn();
                break;
            case MOBILE_DATA:
                updateMobileData();
                break;
            default:
                break;
        }
    }

    @Override
    public void onReceive(String s, HSBundle hsBundle) {
        switch (s) {
            case ScreenStatusReceiver.NOTIFICATION_SCREEN_ON:
                startListenPhoneState();
                break;
            case ScreenStatusReceiver.NOTIFICATION_SCREEN_OFF:
                stopListenPhoneState();
                break;
            default:
                break;
        }
    }
}
