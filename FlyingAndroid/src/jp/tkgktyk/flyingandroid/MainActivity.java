package jp.tkgktyk.flyingandroid;

import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

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
						setListSummary((ListPreference) preference,
								(String) newValue);
						return true;
					}
				});
		setListSummary(scrollSpeed, getSharedString(R.string.pref_key_speed));
		// width
		EditTextPreference width = (EditTextPreference) findPreference(R.string.pref_key_drag_area_width_dp);
		String w = getSharedString(R.string.pref_key_drag_area_width_dp);
		VerticalDragDetectorView dragView = new VerticalDragDetectorView(this);
		if (w == null) {
			w = String.valueOf(dragView.getDetectionWidthDp());
		}
		width.setSummary(getString(R.string.Current_s1, w + "dp"));
		final RelativeLayout dragAreaView = new RelativeLayout(this);
		dragView.setDetectionWidthDp(Integer.parseInt(w));
		resetDragAreaView(dragAreaView, dragView.getDetectionWidth());
		addContentView(dragAreaView, new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		width.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				String w = (String) newValue;
				preference.setSummary(getString(R.string.Current_s1, w + "dp"));
				VerticalDragDetectorView dragView = new VerticalDragDetectorView(
						MainActivity.this);
				dragView.setDetectionWidthDp(Integer.parseInt(w));
				resetDragAreaView(dragAreaView, dragView.getDetectionWidth());
				return true;
			}
		});
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

	private void resetDragAreaView(RelativeLayout container, int width) {
		container.removeAllViews();
		int background = this.getResources().getColor(R.color.drag_area);
		// add left
		final View left = new View(this);
		left.setBackgroundColor(background);
		final RelativeLayout.LayoutParams lpLeft = new RelativeLayout.LayoutParams(
				width, ViewGroup.LayoutParams.WRAP_CONTENT);
		lpLeft.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		lpLeft.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		left.setLayoutParams(lpLeft);
		container.addView(left);
		// add right
		final View right = new View(this);
		right.setBackgroundColor(background);
		final RelativeLayout.LayoutParams lpRight = new RelativeLayout.LayoutParams(
				width, ViewGroup.LayoutParams.WRAP_CONTENT);
		lpRight.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		lpRight.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		right.setLayoutParams(lpRight);
		container.addView(right);
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
