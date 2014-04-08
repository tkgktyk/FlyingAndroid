package jp.tkgktyk.flyingandroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ShortcutActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent shortcut = new Intent(this, ToggleActivity.class);

		Intent intent = new Intent();
		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcut);
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
				getString(R.string.toggle_app_name));
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
				Intent.ShortcutIconResource.fromContext(this,
						R.drawable.ic_launcher));

		setResult(RESULT_OK, intent);
		finish();
	}
}
