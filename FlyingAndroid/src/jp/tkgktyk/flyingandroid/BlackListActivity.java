package jp.tkgktyk.flyingandroid;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class BlackListActivity extends Activity {

	private class ViewHolder {
		CheckBox onlyExcluded;
		BlackListFragment blackList;
	}

	private ViewHolder mViewHolder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_black_list);

		mViewHolder = new ViewHolder();
		mViewHolder.onlyExcluded = (CheckBox) findViewById(R.id.only_black_check);
		mViewHolder.blackList = (BlackListFragment) getFragmentManager()
				.findFragmentById(R.id.black_list);

		mViewHolder.onlyExcluded
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						mViewHolder.blackList.setShowOnlyBlack(isChecked);
					}
				});
	}
}
