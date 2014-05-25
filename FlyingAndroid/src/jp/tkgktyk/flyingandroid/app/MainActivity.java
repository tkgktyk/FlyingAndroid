package jp.tkgktyk.flyingandroid.app;

import jp.tkgktyk.flyingandroid.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class MainActivity extends Activity {

	private final static int NOTIFICATION_ID = 1234567;
	
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
			
			CheckBoxPreference notificationPref = (CheckBoxPreference) findPreference(R.string.pref_key_use_notification);
			
			setToggleNotification(notificationPref.isChecked());

			// scroll speed
			showListSummary(R.string.pref_key_speed);
			// initial position
			openActivity(R.string.pref_key_initial_position,
					InitialPositionActivity.class);
			// force set black background
			openSelectorOnClick(R.string.pref_key_force_set_black_background,
					R.string.Show_only_checked);
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
			// niwatori button
			if (getPreferenceManager().getSharedPreferences().getBoolean(
					getString(R.string.pref_key_feeling_to_donate), false)) {
				Preference niwatori = findPreference(R.string.pref_key_use_niwatori_button);
				niwatori.setEnabled(true);
			}
			Preference donate = findPreference(R.string.pref_key_feeling_to_donate);
			donate.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					getPreferenceManager()
							.getSharedPreferences()
							.edit()
							.putBoolean(
									getString(R.string.pref_key_feeling_to_donate),
									true).apply();
					Preference niwatori = findPreference(R.string.pref_key_use_niwatori_button);
					niwatori.setEnabled(true);
					return false;
				}
			});
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
					activity.putExtra(AppSelectActivity.EXTRA_PREF_KEY_STRING,
							preference.getKey());
					activity.putExtra(AppSelectActivity.EXTRA_ONLY_TEXT_ID,
							textId);
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
		
		@SuppressWarnings("deprecation")
		@SuppressLint("NewApi")
		private void setToggleNotification(boolean showNotification){
			
			NotificationManager nm = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
			
			if(showNotification){
				
				Notification.Builder notBuilder = new Notification.Builder(getActivity());
				
				notBuilder.setOngoing(true);
				notBuilder.setContentTitle("Toggle Flying Android");
				
				notBuilder.setSmallIcon(R.drawable.ic_launcher);
				
				notBuilder.setWhen(System.currentTimeMillis());
				
				PendingIntent toggleBroadcast = PendingIntent.getBroadcast(getActivity(), 0, new Intent("jp.tkgktyk.flyingandroid.ACTION_TOGGLE"), 0);
				
				notBuilder.setContentIntent(toggleBroadcast);
				
				if(android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
					nm.notify(NOTIFICATION_ID, notBuilder.getNotification());
				}
				else{
					nm.notify(NOTIFICATION_ID, notBuilder.build());
				}
			}
			else{
				nm.cancel(NOTIFICATION_ID);
			}
		}
		
		@Override
		public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference){
			
			if(preference.getKey().equals(getString(R.string.pref_key_use_notification))){
				
				CheckBoxPreference cbPref = (CheckBoxPreference)preference;
				
				setToggleNotification(cbPref.isChecked());				
				
				return true;
			}
			
			return super.onPreferenceTreeClick(preferenceScreen, preference);
		}
	}
}
