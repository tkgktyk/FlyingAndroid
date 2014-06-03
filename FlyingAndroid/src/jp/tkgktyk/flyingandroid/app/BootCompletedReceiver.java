package jp.tkgktyk.flyingandroid.app;

import jp.tkgktyk.flyingandroid.FA;
import jp.tkgktyk.flyingandroid.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootCompletedReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences pref = FA.getSharedPreferences(context);
		if (pref.getBoolean(
				context.getString(R.string.pref_key_use_notification), false)) {
			// do nothing when pref is false.
			FA.showToggleNotification(context, true);
		}
	}
}
