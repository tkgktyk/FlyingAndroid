package jp.tkgktyk.flyingandroid;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class MainActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_preference);

		OnPreferenceChangeListener listChangeListener = new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				setListSummary((ListPreference) preference, (String) newValue);
				return true;
			}
		};
		// scroll speed
		ListPreference scrollSpeed = (ListPreference) findPreference(R.string.pref_key_speed);
		scrollSpeed.setOnPreferenceChangeListener(listChangeListener);
		setListSummary(scrollSpeed, getSharedString(R.string.pref_key_speed));
		// takeoff position
		ListPreference takeoff = (ListPreference) findPreference(R.string.pref_key_takeoff_position);
		takeoff.setOnPreferenceChangeListener(listChangeListener);
		setListSummary(takeoff,
				getSharedString(R.string.pref_key_takeoff_position));
		// black list
		Preference blackList = findPreference(R.string.pref_key_black_list);
		blackList.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent activity = new Intent(preference.getContext(),
						BlackListActivity.class);
				startActivity(activity);
				return true;
			}
		});
	}

	private String getSharedString(int keyId) {
		return PreferenceManager.getDefaultSharedPreferences(this).getString(
				getString(keyId), null);
	}

	private void setListSummary(ListPreference pref, String value) {
		int index = pref.findIndexOfValue(value);
		CharSequence entry = null;
		if (index != -1) {
			entry = pref.getEntries()[index];
		} else {
			entry = "default";
		}
		pref.setSummary(getString(R.string.Current_s1, entry));
	}

	// @Override
	// protected void onDestroy() {
	// super.onDestroy();
	//
	// FlyingAndroid.reloadPreferences();
	// }

	protected Preference findPreference(int id) {
		return findPreference(getString(id));
	}
}
