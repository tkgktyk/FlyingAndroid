package jp.tkgktyk.flyingandroid;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class MainActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_preference);
		// scroll speed
		ListPreference scrollSpeed = (ListPreference) findPreference(R.string.pref_key_speed);
		scrollSpeed
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						float speed = Float.parseFloat((String) newValue);
						preference.setSummary(getString(
								R.string.Current_speed_f1, speed));
						return true;
					}
				});
		scrollSpeed.setSummary(getString(
				R.string.Current_speed_f1,
				Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(
						this).getString(getString(R.string.pref_key_speed),
						"1.5"))));
	}

	protected Preference findPreference(int id) {
		return this.findPreference(this.getString(id));
	}
}
