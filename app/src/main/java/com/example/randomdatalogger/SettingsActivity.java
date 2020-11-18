package com.example.randomdatalogger;

import android.Manifest;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsActivity extends AppCompatActivity {

    private final static String[] PERMISSIONS_NEEDED =
            new String[] {Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings_activity);

        /*
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        */

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }



        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            // Some EditTextPreference attributes can't be set in the xml file; set here instead.
            EditTextPreference prefFrequency = findPreference("logging_freq");
            EditTextPreference prefThreshold = findPreference("threshold");

            // Allow log frequency to take values between 0-600 only.
            if (prefFrequency != null) {
                prefFrequency.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
                    @Override
                    public void onBindEditText(@NonNull EditText editText) {
                        int maxLength = 3;
                        int minValue = 0;
                        int maxValue = 600;
                        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                        editText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(maxLength)});
                        editText.setFilters(new InputFilter[]{ new InputFilterMinMax(minValue, maxValue)});
                    }
                });
            }

            // Allow threshold to be a signed integer.
            if (prefThreshold != null) {
                prefThreshold.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
                    @Override
                    public void onBindEditText(@NonNull EditText editText) {
                        editText.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
                    }
                });
            }


            // Results in null pointer exception without if (pref != null) check.
            /*
            pref.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    int maxLength = 3;
                    int minValue = 0;
                    int maxValue = 600;
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    editText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(maxLength)});
                    editText.setFilters(new InputFilter[]{ new InputFilterMinMax(minValue, maxValue)});
                }
            });
            */
        }
    }

    // TODO: filter needs further debugging to handle non-zero min values; see https://stackoverflow.com/questions/14212518/is-there-a-way-to-define-a-min-and-max-value-for-edittext-in-android
    public static class InputFilterMinMax implements InputFilter {

        private int min, max;

        public InputFilterMinMax(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public InputFilterMinMax(String min, String max) {
            this.min = Integer.parseInt(min);
            this.max = Integer.parseInt(max);
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                                   int dstart, int dend) {
            try {
                // Remove the string out of destination that is to be replaced.
                String replacement = source.subSequence(start, end).toString();

                // Add the new string in.
                String newVal = dest.toString().substring(0, dstart) + replacement +
                        dest.toString().substring(dend);
                int input = Integer.parseInt(newVal);

                // Check if input is within range.
                if (isInRange(min, max, input))
                    return null;
            } catch (NumberFormatException nfe) { }
            return "";
        }

        private boolean isInRange(int min, int max, int input) {
            return max > min ? input >= min && input <= max : input >= max && input <= min;
        }
    }
}