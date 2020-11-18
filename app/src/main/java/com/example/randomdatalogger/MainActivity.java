package com.example.randomdatalogger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.formatter.DefaultFillFormatter;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback, SharedPreferences.OnSharedPreferenceChangeListener {

    public final static String TAG = "MainActivity";
    private final static String[] PERMISSIONS_NEEDED =
            new String[] {Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CHANGE_NETWORK_STATE};
    private final static int REQ_PERMISSIONS_ON_STARTUP = 1;

    private static int signalThreshold = -200;
    private static int toggleInterval = 15;

    private static final int Y_AXIS_MIN = -120;
    private static final int Y_AXIS_MAX = -80;

    // View elements.
    private ToggleButton mBtnToggleLogging;
    private CombinedChart mSignalChart;
    private Button mBtnGetIP;
    private TextView mTextViewToggleInterval;
    private TextView mTextViewUseRealData;

    private SharedPreferences mSharedPrefs;

    private DatabaseHelper dbHelper;
    private SQLiteDatabase mDb;

    private Intent serviceIntent;

    // Track whether currently logging to avoid starting RandomDataService twice.
    private boolean _isLogging;



    // Inflate the menu file in the Main Activity.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }

    // Setup an explicit intent to open up the Settings Activity.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }

    private void setupSharedPreferences() {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSharedPrefs.registerOnSharedPreferenceChangeListener(this);
        Log.d(TAG, "Setting up shared preferences.");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        // Check if accessibility service is enabled; if not, redirect user to accessibility
        // settings.
        if (!checkAccess()) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.accessibility_redirect_message)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                            startActivity(intent);
                        }
                    })
                    .show();

        }

        setupSharedPreferences();

        // Request permissions.
        checkAndRequestPermissions(REQ_PERMISSIONS_ON_STARTUP);

        // Set signal threshold from preferences.
        String sThreshold =
                mSharedPrefs.getString(getString(R.string.pref_key_threshold),
                        "-200");
        if (sThreshold != null)
            signalThreshold = Integer.parseInt(sThreshold);

        // Get airplane mode toggle interval from preferences.
        String sToggleInterval = mSharedPrefs.getString(getString(R.string.pref_key_toggle_interval),
                "15");
        if (sToggleInterval != null)
            toggleInterval = Integer.parseInt(sToggleInterval);

        mTextViewToggleInterval = (TextView) findViewById(R.id.textViewToggleInterval);
        mTextViewToggleInterval.setText("Toggle airplane mode no more than every " + toggleInterval + " seconds.");

        // Get signal data source (real/random) from preferences.
        boolean useRealData = mSharedPrefs.getBoolean("use_real_data", false);
        mTextViewUseRealData = (TextView) findViewById(R.id.textViewUseRealData);
        if (useRealData)
            mTextViewUseRealData.setText("Currently using real signal data.");
        else
            mTextViewUseRealData.setText("Currently using random signal data.");

        // Chart initialization.
        // mSignalChart = (LineChart) findViewById(R.id.signalChart);
        mSignalChart = (CombinedChart) findViewById(R.id.signalChart);
        mSignalChart.setDrawGridBackground(false);
        mSignalChart.setDescription(null);
        mSignalChart.getAxisRight().setEnabled(false);
        XAxis xaxis = mSignalChart.getXAxis();
        xaxis.setEnabled(false);
        YAxis yaxis = mSignalChart.getAxisLeft();
        yaxis.setTextSize(12);
        yaxis.setLabelCount(20, true);
        yaxis.setAxisMaximum(-80f);
        yaxis.setAxisMinimum(-120f);
        yaxis.setDrawZeroLine(true);

        // Logging toggle button initialization.
        mBtnToggleLogging = (ToggleButton) findViewById(R.id.toggleButtonStartLogging);
        mBtnToggleLogging.setChecked(
                mSharedPrefs.getBoolean("logging_enabled", false)
        );

        // Listen to changes in logging status and apply changes to preference screen.
        mBtnToggleLogging.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !_isLogging) startLogging();
                else if (!isChecked && _isLogging) stopLogging();

                set_isLogging(isChecked);

                mSharedPrefs.edit()
                        .putBoolean("logging_enabled", isChecked)
                        .apply();
            }
        } );

        set_isLogging(mSharedPrefs.getBoolean("logging_enabled", false));

        // Get IP Address button initialization.
        mBtnGetIP = (Button) findViewById(R.id.buttonGetIP);
        mBtnGetIP.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), GetIPAddressActivity.class);
                startActivity(intent);
            }
        });

        // Intent to start / stop logging service.
        serviceIntent = new Intent(MainActivity.this, RandomDataService.class);

        // Create the SQLite database if needed.
        dbHelper = DatabaseHelper.getInstance(MainActivity.this);

        // Delete old table entries (UNINSTALL TO CLEAR DATA!)
        // dbHelper.deleteAll();
        dbHelper.clearTable();
        mDb = dbHelper.getWritableDatabase();

        /*
        (new Thread(new Runnable() {
            @Override
            public void run() {
                // DatabaseHelper dbHelper = new DatabaseHelper(MainActivity.this);
                mDb = dbHelper.getWritableDatabase();
            }
        })).start();

         */
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Start listening for chart data from the service.
        IntentFilter fltChart = new IntentFilter();
        fltChart.addAction(getString(R.string.intent_category_chart_data_available));
        LocalBroadcastManager.getInstance(this).registerReceiver(mChartDataReceiver, fltChart);

        // Start listening for signals below threshold from the service.
        IntentFilter fltThreshold = new IntentFilter();
        fltThreshold.addAction(getString(R.string.intent_category_signal_below_threshold));
        LocalBroadcastManager.getInstance(this).registerReceiver(mBelowThresholdReceiver, fltThreshold);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Stop listening for chart data.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mChartDataReceiver);

        // Stop listening for signals below threshold.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBelowThresholdReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // mDb.close();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_key_log_enabled))) {

            boolean loggingEnabled = mSharedPrefs.getBoolean(key, false);
            if (loggingEnabled && !_isLogging) startService(serviceIntent);
            else if (!loggingEnabled && _isLogging) stopService(serviceIntent);
            set_isLogging(loggingEnabled);

            Log.d(TAG, "_isLogging = " + _isLogging);
        } else if (key.equals(getString(R.string.pref_key_threshold))) {
            String sThreshold = mSharedPrefs.getString(key, "-200");
            if (sThreshold != null)
                signalThreshold = Integer.parseInt(sThreshold);

            Log.d(TAG, "threshold = " + signalThreshold);
        } else if (key.equals(getString(R.string.pref_key_toggle_interval))) {
            String sToggleInterval = mSharedPrefs.getString(key, "15");
            if (sToggleInterval != null) {
                toggleInterval = Integer.parseInt(sToggleInterval);
                mTextViewToggleInterval.setText("Toggle airplane mode no more than every " + toggleInterval + " seconds.");
            }
        }
    }

    // Set logging status.
    public void set_isLogging(boolean isLogging) {
        this._isLogging = isLogging;
        Log.d(TAG, "set _isLogging to " + _isLogging);
    }

    public void startLogging() {

        // Clear table before starting logging so that the table only contains data from the
        // subsequent logging session.
        dbHelper.clearTable();
        startService(serviceIntent);
        set_isLogging(true);
    }

    public void stopLogging() {
        stopService(serviceIntent);
        set_isLogging(false);

        // Print current values in SQLite database.
        String dbString = "";
        if (!mDb.isOpen()) mDb = dbHelper.getWritableDatabase();
        dbString = dbHelper.getTableAsString(mDb, DatabaseContract.Table1.TABLE_NAME);
        Log.d(TAG, dbString);
    }


    // Broadcast receiver to listen for signal strength below threshold.
    private BroadcastReceiver mBelowThresholdReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "onReceive below threshold");

            Toast.makeText(MainActivity.this,
                    "Signal below threshold! Toggling airplane mode...",
                    Toast.LENGTH_SHORT).show();

            // Go to wireless settings and wait for accessibility service to toggle airplane mode.
            Intent intentGoToWirelessSettings = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            startActivity(intentGoToWirelessSettings);
        }
    };

    // Broadcast receiver to listen for new chart data points.
    private BroadcastReceiver mChartDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<ReadingDataPoint> chartData =
                    intent.getParcelableArrayListExtra(getString(R.string.extra_chart_data_list));

            Log.d(TAG, "onReceive chart data (" + chartData.size() + " data points)");

            if (chartData != null && chartData.size() > 0) {
                mSignalChart.setVisibility(View.VISIBLE);

                // Update chart
                List<Entry> chartEntries = new ArrayList<>();
                List<Entry> chartEntriesAbove = new ArrayList<>();
                List<Entry> chartEntriesBelow = new ArrayList<>();
                List<Entry> chartEntriesToThreshold = new ArrayList<>();


                for (int i = 0; i < chartData.size(); i++) {

                    chartEntries.add(new Entry(i, chartData.get(i).get_signalStrength()));

                    if (chartData.get(i).get_signalStrength() <= signalThreshold) {
                        chartEntriesBelow.add(new Entry(i, chartData.get(i).get_signalStrength()));
                        chartEntriesToThreshold.add(new Entry(i, chartData.get(i).get_signalStrength()));
                    }
                    else {
                        chartEntriesAbove.add(new Entry(i, chartData.get(i).get_signalStrength()));
                        chartEntriesToThreshold.add(new Entry(i, signalThreshold));
                    }

                }

                LineDataSet dataSetLineAbove = new LineDataSet(chartEntries, "line above");
                LineDataSet dataSetLineBelow = new LineDataSet(chartEntriesToThreshold, "line above");

                ScatterDataSet dataSetScatterAbove = new ScatterDataSet(chartEntriesAbove, "Above");
                ScatterDataSet dataSetScatterBelow = new ScatterDataSet(chartEntriesBelow, "Below");


                // Signal data above threshold; fill up from signal with gray.
                dataSetLineAbove.setMode(LineDataSet.Mode.STEPPED);
                dataSetLineAbove.setColor(Color.BLACK);
                dataSetLineAbove.setDrawCircles(false);
                dataSetLineAbove.setDrawCircleHole(false);
                dataSetLineAbove.setDrawValues(false);
                dataSetLineAbove.setFillColor(Color.DKGRAY);
                dataSetLineAbove.setFillAlpha(65);
                dataSetLineAbove.setDrawFilled(true);

                // Signal data below threshold; fill up from signal to threshold with red.
                dataSetLineBelow.setMode(LineDataSet.Mode.STEPPED);
                dataSetLineBelow.setColor(Color.RED);
                dataSetLineBelow.setDrawCircles(false);
                dataSetLineBelow.setDrawCircleHole(false);
                dataSetLineBelow.setDrawValues(false);
                dataSetLineBelow.setFillColor(Color.RED);
                dataSetLineBelow.setFillAlpha(85);
                dataSetLineBelow.setDrawFilled(true);
                dataSetLineBelow.setFillFormatter(new DefaultFillFormatter() {
                    @Override
                    public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
                        return signalThreshold;// its value of middle Y line
                    }
                });

                // Scatter data of signals above threshold (black).
                dataSetScatterAbove.setScatterShapeSize(20f);
                dataSetScatterAbove.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
                dataSetScatterAbove.setColor(Color.BLACK);
                dataSetScatterAbove.setDrawValues(false);

                // Scatter data of signals below threshold (red).
                dataSetScatterBelow.setScatterShapeSize(20f);
                dataSetScatterBelow.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
                dataSetScatterBelow.setColor(Color.RED);
                dataSetScatterBelow.setDrawValues(false);

                LineData dataLine = new LineData();
                dataLine.addDataSet(dataSetLineAbove);
                dataLine.addDataSet(dataSetLineBelow);

                ScatterData dataScatter = new ScatterData();
                dataScatter.addDataSet(dataSetScatterAbove);
                dataScatter.addDataSet(dataSetScatterBelow);

                CombinedData dataAll = new CombinedData();
                dataAll.setData(dataLine);
                dataAll.setData(dataScatter);

                // Add signal threshold if in chart range.
                if (isThresholdInRange()) {
                    LimitLine limitLine = new LimitLine(signalThreshold); // Set where the line should be drawn.
                    limitLine.setLineColor(Color.RED);
                    limitLine.setLineWidth(3f);

                    YAxis leftAxis = mSignalChart.getAxisLeft();
                    leftAxis.removeAllLimitLines();
                    leftAxis.addLimitLine(limitLine);
                    // leftAxis.setDrawLimitLinesBehindData(true);
                }

                mSignalChart.setData(dataAll);
                mSignalChart.getLegend().setEnabled(false);
                mSignalChart.invalidate();
            }
        }
    };

    private boolean checkAndRequestPermissions(int req_code) {
        List<String> permissionsNeeded = new ArrayList<>();

        // Check which of the permissions still need to be granted.
        for (int i = 0; i < PERMISSIONS_NEEDED.length; i++) {
            if (ContextCompat.checkSelfPermission(this, PERMISSIONS_NEEDED[i]) !=
                    PackageManager.PERMISSION_GRANTED)
                permissionsNeeded.add(PERMISSIONS_NEEDED[i]);
        }

        int num = permissionsNeeded.size();

        if (num > 0)
            requestPermissions(permissionsNeeded.toArray(new String[num]), req_code);

        return (num == 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        boolean allPermissionsGranted = true;
        for (int perm : grantResults)
            if (perm != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }

        if (!allPermissionsGranted) {
            // Present rationale.
            AlertDialog dlg = new AlertDialog.Builder(this)
                    .setMessage("Location and phone signal strength permissions must be granted" +
                            " to allow logging to begin.")
                    .setTitle("Permission Needed")
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
            dlg.show();
        }
    }

    // If signal threshold is in chart range, plot on line chart.
    private boolean isThresholdInRange() {
        if (signalThreshold >= Y_AXIS_MIN && signalThreshold <= Y_AXIS_MAX) return true;
        else return false;
    }

    // Check for accessibility service access.
    protected boolean checkAccess() {
        String string = getString(R.string.accessibility_service_id);
        for (AccessibilityServiceInfo id : ((AccessibilityManager)
                getSystemService(Context.ACCESSIBILITY_SERVICE))
                .getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK)) {
            if (string.equals(id.getId())) {
                return true;
            }
        }
        return false;
    }
    
}