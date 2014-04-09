package jp.tkgktyk.flyingandroid;

import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class MainActivity extends PreferenceActivity {

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesMode(
				PreferenceActivity.MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.settings_preference);

		// scroll speed
		showListSummary(R.string.pref_key_speed);
		// takeoff position
		showListSummary(R.string.pref_key_takeoff_position);
		// black list
		openSelectorOnClick(R.string.pref_key_black_list,
				R.string.Show_only_black);
		// pin position
		showListSummary(R.string.pref_key_pin_position);
		// white list
		openSelectorOnClick(R.string.pref_key_white_list,
				R.string.Show_only_white);
	}

	@SuppressWarnings("deprecation")
	protected Preference findPreference(int id) {
		return findPreference(getString(id));
	}

	private String getSharedString(int keyId) {
		return PreferenceManager.getDefaultSharedPreferences(this).getString(
				getString(keyId), null);
	}

	private final OnPreferenceChangeListener mListChangeListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			setListSummary((ListPreference) preference, (String) newValue);
			return true;
		}
	};

	private void showListSummary(int id) {
		ListPreference list = (ListPreference) findPreference(id);
		list.setOnPreferenceChangeListener(mListChangeListener);
		setListSummary(list, getSharedString(id));
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

	private void openSelectorOnClick(int id, final int textId) {
		Preference pref = findPreference(id);
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent activity = new Intent(preference.getContext(),
						SelectableListActivity.class);
				activity.putExtra(SelectableListActivity.EXTRA_PREF_KEY_STRING,
						preference.getKey());
				activity.putExtra(SelectableListActivity.EXTRA_ONLY_TEXT_ID,
						textId);
				startActivity(activity);
				return true;
			}
		});
	}
}
