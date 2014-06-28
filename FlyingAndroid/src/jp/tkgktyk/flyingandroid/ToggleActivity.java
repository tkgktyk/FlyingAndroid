package jp.tkgktyk.flyingandroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ToggleActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// ignore exit animation
		overridePendingTransition(0, 0);
		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		sendBroadcast(new Intent(FA.ACTION_TOGGLE));
	}
}