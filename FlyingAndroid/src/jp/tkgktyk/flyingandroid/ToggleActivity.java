package jp.tkgktyk.flyingandroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ToggleActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		finish();
		sendBroadcast(new Intent("jp.tkgktyk.flyingandroid.ACTION_TOGGLE"));
	}
}