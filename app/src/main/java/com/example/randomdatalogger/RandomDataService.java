/*
 * Generates and logs random 'signal' measurements, or logs real signal strength values, taken at fixed time intervals.
 */

package com.example.randomdatalogger;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RandomDataService extends Service {

    private static final String TAG = "RandomDataService";

    private static final int SVC_NOTIF_ID = 1;

    private static final int ACT_PI_REQ_CODE = 1;
    private static final int MAX_SIGNAL = -95;
    private static final int MIN_SIGNAL= -105;
    private int MAX_CHART_DATA_POINTS;

    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    // private Handler handler = new Handler();
    private Handler handler;

    private SharedPreferences mSharedPrefs = null;
    private SQLiteDatabase mDb;
    private final ArrayList<ReadingDataPoint> mChartData = new ArrayList<>();

    private int logInterval;
    private int signalThreshold;

    private int toggleInterval;
    private long lastTimeSinceToggle;

    private boolean useRealData;


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Logging completed.", Toast.LENGTH_SHORT).show();
        // wakeLock.release();
        stopLogging();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Get a notification chanel ID
        createNotificationChannel();

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mDb = DatabaseHelper.getInstance(this).getWritableDatabase();

        MAX_CHART_DATA_POINTS = getResources().getInteger(R.integer.max_chart_data_points);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Logging starting...", Toast.LENGTH_SHORT).show();

        lastTimeSinceToggle = Calendar.getInstance().getTimeInMillis();

        startLogging();

        return START_NOT_STICKY;
    }

    // Create a notification channel.
    private NotificationChannel createNotificationChannel() {

        NotificationManager nm = getSystemService(NotificationManager.class);

        // Notification channel could have already been created...
        NotificationChannel nc =
                nm.getNotificationChannel(getString(R.string.notif_chan_id));

        // ... but if not, create it here.
        if (nc == null) {
            nc = new NotificationChannel(
                    getString(R.string.notif_chan_id),
                    getString(R.string.notif_chan_name),
                    NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(nc);
        }
        return nc;
    }

    // Show a non-dismissable notification while the service is running. Make it launch the main
    // activity if the user taps it; this lets them make changes or stop logging.
    private Notification buildForegroundNotification(String notText) {

        // Create notification and bring to foreground.
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent =
                PendingIntent.getActivity(this, ACT_PI_REQ_CODE, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(this,
                getString(R.string.notif_chan_id))
                .setSmallIcon(R.drawable.ic_cellsignal)
                .setTicker(getString(R.string.notif_ticker))
                .setContentTitle(getString(R.string.notif_title))
                .setContentIntent(contentIntent);
        if (notText != null) nb.setContentText(notText);

        return nb.build();
    }

    // Code to be executed periodically: get and log data after a fixed time interval.
    private Runnable logData = new Runnable() {
        @Override
        public void run() {
            // Included to prevent runnable from executing twice.
            handler.removeCallbacks(logData);

            // Get current time (as UTC milliseconds from the epoch).
            long currentTime = Calendar.getInstance().getTimeInMillis();

            // Get real signal data or generate random signal data.
            int data;
            if (useRealData) data = getCurrentDbm();
            else data = generateData();

            // Add data to SQLite database.
            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(DatabaseContract.Table1.COLUMN_TIME, currentTime);
            values.put(DatabaseContract.Table1.COLUMN_STRENGTH, data);

            Log.d(TAG, "time = " + currentTime + ", signal = " + data + " (" + useRealData + " data)");

            // Insert the new row, returning the primary key value of the new row
            long newRowId = mDb.insert(DatabaseContract.Table1.TABLE_NAME, null, values);

            // Send data to chart.
            ReadingDataPoint rdp = new ReadingDataPoint(
                    currentTime, data);
            mChartData.add(rdp);
            int overflow = mChartData.size() - MAX_CHART_DATA_POINTS;
            while (overflow-- > 0) {
                mChartData.remove(0);
            }
            Intent intent = new Intent();
            intent.setAction(getString(R.string.intent_category_chart_data_available));
            intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            intent.putParcelableArrayListExtra(getString(R.string.extra_chart_data_list), mChartData);
            // Send only to local process
            LocalBroadcastManager.getInstance(RandomDataService.this).sendBroadcast(intent);

            Log.d(TAG, "Sent chart data broadcast");

            // If signal below threshold and more than a toggle interval has elapsed, send to
            // receiver.
            if (data < signalThreshold &&
                    (currentTime - lastTimeSinceToggle) > toggleInterval * 1000) {

                lastTimeSinceToggle = currentTime;

                Intent intentThreshold = new Intent();
                intentThreshold.setAction(getString(R.string.intent_category_signal_below_threshold));
                intentThreshold.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                LocalBroadcastManager.getInstance(RandomDataService.this).sendBroadcast(intentThreshold);
            }

            // Repeat every logInterval.
            handler.postDelayed(logData, logInterval*1000 - SystemClock.elapsedRealtime()%1000);
        }
    };


    // Private helper method to randomly generate the result of a 'signal' check.
    private int generateData() {
        int randomData = MIN_SIGNAL + (int)(Math.random() * ((MAX_SIGNAL - MIN_SIGNAL) + 1));
        return randomData;
    }

    // Private helper method to obtain actual signal data.
    public int getCurrentDbm() {
        int currentDbm = -125;

        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        // CellInfo may only be returned if ACCESS_FINE_LOCATION permission is granted.
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {

            // Log string.
            String list = "";

            List<CellInfo> cellInfoList =
                    telephonyManager.getAllCellInfo();

            if (cellInfoList != null) {

                int i = 0;

                for (CellInfo cellInfo : cellInfoList) {

                    i++;

                    if (cellInfo instanceof CellInfoGsm) { // If GSM connection.

                        list += "Site_"+ i + " (GSM)\r\n";
                        list += "Registered: " + cellInfo.isRegistered() + "\r\n";

                        CellSignalStrengthGsm cellSignalStrengthGsm =
                                ((CellInfoGsm) cellInfo).getCellSignalStrength();
                        if (cellInfo.isRegistered())
                            currentDbm = cellSignalStrengthGsm.getDbm();

                        list += "dBm: " + currentDbm + "\r\n\r\n";

                    } else if (cellInfo instanceof CellInfoCdma) {  // If CDMA connection.

                        list += "Site_"+ i + " (CDMA)\r\n";
                        list += "Registered: " + cellInfo.isRegistered() + "\r\n";

                        CellSignalStrengthCdma cellSignalStrengthCdma =
                                ((CellInfoCdma) cellInfo).getCellSignalStrength();
                        if (cellInfo.isRegistered())
                            currentDbm = cellSignalStrengthCdma.getDbm();

                        list += "dBm: " + currentDbm + "\r\n\r\n";

                    } else if (cellInfo instanceof CellInfoLte) {  // If LTE connection.

                        list += "Site_"+ i + " (LTE)\r\n";
                        list += "Registered: " + cellInfo.isRegistered() + "\r\n";

                        CellSignalStrengthLte cellSignalStrengthLte =
                                ((CellInfoLte) cellInfo).getCellSignalStrength();
                        if (cellInfo.isRegistered())
                            currentDbm = cellSignalStrengthLte.getDbm();

                        list += "dBm: " + currentDbm + "\r\n\r\n";
                    }
                }
                Log.i("Info display", list);  // Display results in log.
            }
        }
        return currentDbm;
    }

    // Start logging the data.
    private void startLogging() {

        // Display the foreground notification.
        Notification notification = buildForegroundNotification(null);
        startForeground(SVC_NOTIF_ID, notification);

        // Initiate handler to perform periodic logs.
        handler = new Handler();

        // Get log frequency from preferences.
        String sLogInterval = mSharedPrefs.getString(getString(R.string.pref_key_log_freq), "1");
        logInterval = Integer.parseInt(sLogInterval);

        // Get signal threshold from preferences.
        String sSignalThreshold = mSharedPrefs.getString(getString(R.string.pref_key_threshold), "-200");
        signalThreshold = Integer.parseInt(sSignalThreshold);

        // Get toggle interval from preferences.
        String sToggleInterval = mSharedPrefs.getString(getString(R.string.pref_key_toggle_interval), "15");
        toggleInterval = Integer.parseInt(sToggleInterval);

        // Determine whether to use real or random data from shared preferences.
        useRealData = mSharedPrefs.getBoolean("use_real_data", false);

        // Doc says this call needs to be called from a worker thread since upgrading can be lengthy;
        // however, we know that the main form called this earlier, so we should be okay
        if (mDb == null || !mDb.isOpen()) {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
            mDb = dbHelper.getWritableDatabase();
        }

        logData.run();
    }

    // Called when user has opted to stop logging and the main app has stopped the service.
    // Called from onDestroy().
    private void stopLogging() {

        handler.removeCallbacks(logData);

        new Thread(new Runnable() {
            @Override
            public void run() {

                if (mSharedPrefs.getBoolean(getString(R.string.pref_key_log_enabled), true))
                    mSharedPrefs.edit()
                            .putBoolean(getString(R.string.pref_key_log_enabled), false)
                            .apply();

                // Clear the persistent logger notification
                NotificationManagerCompat.from(RandomDataService.this).cancel(SVC_NOTIF_ID);

                Log.d(TAG, "stopLogging");

            }
        }).start();
    }
}
