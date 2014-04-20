package jp.tkgktyk.flyingandroid.app;

import jp.tkgktyk.flyingandroid.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class NiwatoriActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		View niwatori = new View(this);
		niwatori.setBackgroundResource(R.drawable.ic_launcher);
		setContentView(niwatori);
	}
}
