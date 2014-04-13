package jp.tkgktyk.flyingandroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class SelectableListActivity extends Activity {

	public static String EXTRA_PREF_KEY_STRING = "PREF_KEY_STRING";
	public static String EXTRA_ONLY_TEXT_ID = "ONLY_TEXT_ID";

	private class ViewHolder {
		CheckBox onlySelected;
		SelectableListFragment selectableList;
	}

	private ViewHolder mViewHolder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_selectable_list);

		String prefKey = null;
		int onlyTextId = 0;
		if (getIntent() != null) {
			Intent intent = getIntent();
			prefKey = intent.getStringExtra(EXTRA_PREF_KEY_STRING);
			onlyTextId = intent.getIntExtra(EXTRA_ONLY_TEXT_ID, 0);
		}

		mViewHolder = new ViewHolder();
		mViewHolder.onlySelected = (CheckBox) findViewById(R.id.only_selected_check);
		mViewHolder.selectableList = (SelectableListFragment) getFragmentManager()
				.findFragmentById(R.id.selectable_list);

		mViewHolder.onlySelected.setText(onlyTextId);
		mViewHolder.onlySelected
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						mViewHolder.selectableList
								.setShowOnlySelected(isChecked);
					}
				});

		mViewHolder.selectableList.setPrefKey(prefKey);
	}
}
