package com.lcw.people;


import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;
import android.view.MenuItem;

public class SettingsActivity extends AppCompatPreferenceActivity {

    public static final String KEY_PREF_SMS_MODE = "pref_sms_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.pref_general);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

            Preference smsPref = findPreference(KEY_PREF_SMS_MODE);
            smsPref.setSummary(sharedPreferences.getBoolean(KEY_PREF_SMS_MODE, true) ?
                    R.string.pref_summary_quick_message_on : R.string.pref_summary_quick_message_off);
            smsPref.setOnPreferenceChangeListener(this);

        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference instanceof SwitchPreference) {
                if (preference.getKey().equals(KEY_PREF_SMS_MODE)) {
                    preference.setSummary((Boolean) newValue ?
                            R.string.pref_summary_quick_message_on :
                            R.string.pref_summary_quick_message_off);

                    return true;
                }
            }

            return false;
        }
    }
}
