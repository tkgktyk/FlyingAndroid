package jp.tkgktyk.flyingandroid.app;

import jp.tkgktyk.flyingandroid.FA;
import jp.tkgktyk.flyingandroid.InitialPosition;
import jp.tkgktyk.flyingandroid.R;
import jp.tkgktyk.flyinglayout.FlyingLayout;
import jp.tkgktyk.flyinglayout.FlyingLayout.OnFlyingEventListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

public class InitialPositionActivity extends Activity {

	private FlyingLayout mFlyingLayout;

	private InitialPosition mInitialPosition;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_initial_position);

		mInitialPosition = new InitialPosition(this);

		mFlyingLayout = (FlyingLayout) findViewById(R.id.flying);
		SharedPreferences pref = FA.getSharedPreferences(this);
		mFlyingLayout.setSpeed(Float.parseFloat(pref.getString(
				getString(R.string.pref_key_speed), "1.5f")));
		mFlyingLayout.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() {
					@SuppressWarnings("deprecation")
					@SuppressLint("NewApi")
					@Override
					public void onGlobalLayout() {
						mFlyingLayout.setOffsetX(mInitialPosition
								.getX(mFlyingLayout));
						mFlyingLayout.setOffsetY(mInitialPosition
								.getY(mFlyingLayout));
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
							mFlyingLayout.getViewTreeObserver()
									.removeOnGlobalLayoutListener(this);
						} else {
							mFlyingLayout.getViewTreeObserver()
									.removeGlobalOnLayoutListener(this);
						}
					}
				});
		mFlyingLayout.setOnFlyingEventListener(new OnFlyingEventListener() {
			@Override
			public void onOutsideClick(FlyingLayout v, int x, int y) {
				// doing nothing
			}

			@Override
			public void onDragStarted(FlyingLayout v) {
				// doing nothing
			}

			@Override
			public void onDragFinished(FlyingLayout v) {
				mInitialPosition.setXp(v, v.getOffsetX());
				mInitialPosition.setYp(v, v.getOffsetY());
				mInitialPosition.save(v.getContext());
				v.setOffsetX(mInitialPosition.getX(v));
				v.setOffsetY(mInitialPosition.getY(v));
				v.requestLayout();
			}
		});
	}
}
