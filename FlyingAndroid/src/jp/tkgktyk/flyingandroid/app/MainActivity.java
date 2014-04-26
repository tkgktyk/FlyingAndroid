package jp.tkgktyk.flyingandroid.app;

import jp.tkgktyk.flyingandroid.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new SettingsFragment()).commit();
	}

	public static class SettingsFragment extends PreferenceFragment {

		@SuppressWarnings("deprecation")
		public void onCreate(Bundle savedInstanceState) {
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
			openActivity(R.string.pref_key_pin_position, MovePinActivity.class);
			// auto pin
			showListSummary(R.string.pref_key_auto_pin_selection);
			// white list
			openSelectorOnClick(R.string.pref_key_white_list,
					R.string.Show_only_white);
		}

		protected Preference findPreference(int id) {
			return findPreference(getString(id));
		}

		private String getSharedString(int keyId) {
			return PreferenceManager.getDefaultSharedPreferences(getActivity())
					.getString(getString(keyId), null);
		}

		private final OnPreferenceChangeListener mListChangeListener = new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
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
							AppSelectActivity.class);
					activity.putExtra(
							AppSelectActivity.EXTRA_PREF_KEY_STRING,
							preference.getKey());
					activity.putExtra(
							AppSelectActivity.EXTRA_ONLY_TEXT_ID, textId);
					startActivity(activity);
					return true;
				}
			});
		}

		private void openActivity(int id, final Class<?> cls) {
			Preference pref = findPreference(id);
			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Intent activity = new Intent(preference.getContext(), cls);
					startActivity(activity);
					return true;
				}
			});
		}
	}
}
