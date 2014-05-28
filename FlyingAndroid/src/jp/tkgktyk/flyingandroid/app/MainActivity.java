package jp.tkgktyk.flyingandroid.app;

import java.util.List;

import jp.tkgktyk.flyingandroid.FA;
import jp.tkgktyk.flyingandroid.R;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class MainActivity extends PreferenceActivity {

	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.preference_headers, target);
	}

	@Override
	protected boolean isValidFragment(String fragmentName) {
		return FlyingFragment.class.getName().equals(fragmentName)
				|| PinningFragment.class.getName().equals(fragmentName)
				|| TroubleshootingFragment.class.getName().equals(fragmentName);
	}

	@Override
	public void onHeaderClick(Header header, int position) {
		super.onHeaderClick(header, position);

		if (header.id == R.id.donate) {
			PreferenceManager
					.getDefaultSharedPreferences(this)
					.edit()
					.putBoolean(getString(R.string.pref_key_feeling_to_donate),
							true).apply();
		}
	}

	public static class SettingFragment extends PreferenceFragment {

		@SuppressWarnings("deprecation")
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			getPreferenceManager().setSharedPreferencesMode(
					PreferenceActivity.MODE_WORLD_READABLE);
		}

		protected Preference findPreference(int id) {
			return findPreference(getString(id));
		}

		private final OnPreferenceChangeListener mListChangeListener = new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				setListSummary((ListPreference) preference, (String) newValue);
				return true;
			}
		};

		protected void showListSummary(int id) {
			ListPreference list = (ListPreference) findPreference(id);
			list.setOnPreferenceChangeListener(mListChangeListener);
			setListSummary(list, getSharedString(id));
		}

		private String getSharedString(int keyId) {
			return PreferenceManager.getDefaultSharedPreferences(getActivity())
					.getString(getString(keyId), null);
		}

		private void setListSummary(ListPreference pref, String value) {
			int index = pref.findIndexOfValue(value);
			CharSequence entry = null;
			if (index != -1) {
				entry = pref.getEntries()[index];
			} else {
				entry = "default";
			}
			pref.setSummary(getString(R.string.current_s1, entry));
		}

		protected void showTextSummary(int id) {
			EditTextPreference et = (EditTextPreference) findPreference(id);
			et.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference,
						Object newValue) {
					preference.setSummary(getString(R.string.current_s1,
							(String) newValue));
					return true;
				}
			});
			et.getOnPreferenceChangeListener().onPreferenceChange(et,
					et.getText());
		}

		protected void openSelectorOnClick(int id, final int textId) {
			Preference pref = findPreference(id);
			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Intent activity = new Intent(preference.getContext(),
							AppSelectActivity.class);
					activity.putExtra(AppSelectActivity.EXTRA_PREF_KEY_STRING,
							preference.getKey());
					activity.putExtra(AppSelectActivity.EXTRA_ONLY_TEXT_ID,
							textId);
					startActivity(activity);
					return true;
				}
			});
		}

		protected void openActivity(int id, final Class<?> cls) {
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

	public static class FlyingFragment extends SettingFragment {

		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.preference_flying);

			// scroll speed
			showTextSummary(R.string.pref_key_speed);
			// initial position
			openActivity(R.string.pref_key_initial_position,
					InitialPositionActivity.class);
			// use notification toggle
			setupNotificationToggle();
		}

		private void setupNotificationToggle() {
			CheckBoxPreference notificationPref = (CheckBoxPreference) findPreference(R.string.pref_key_use_notification);
			notificationPref
					.setOnPreferenceClickListener(new OnPreferenceClickListener() {
						@Override
						public boolean onPreferenceClick(Preference preference) {
							CheckBoxPreference cbPref = (CheckBoxPreference) preference;
							FA.showToggleNotification(cbPref.getContext(),
									cbPref.isChecked());
							return false;
						}
					});
		}
	}

	public static class PinningFragment extends SettingFragment {

		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.preference_pinning);

			// pin position
			openActivity(R.string.pref_key_pin_position, MovePinActivity.class);
			// auto pin
			showListSummary(R.string.pref_key_auto_pin_selection);
			// white list
			openSelectorOnClick(R.string.pref_key_white_list,
					R.string.show_only_white);
			// niwatori button
			setupNiwatoriButton();
		}

		private void setupNiwatoriButton() {
			// unlock
			if (getPreferenceManager().getSharedPreferences().getBoolean(
					getString(R.string.pref_key_feeling_to_donate), false)) {
				Preference niwatori = findPreference(R.string.pref_key_use_niwatori_button);
				niwatori.setEnabled(true);
			}
		}
	}

	public static class TroubleshootingFragment extends SettingFragment {

		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.preference_troubleshooting);

			// force set black background
			openSelectorOnClick(R.string.pref_key_force_set_black_background,
					R.string.show_only_checked);
			// black list
			openSelectorOnClick(R.string.pref_key_black_list,
					R.string.show_only_black);
		}
	}
}
