package jp.tkgktyk.flyingandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.LinearLayout;

public class PinPosition {
	public static int DEFAULT_X_PERCENT = 10;
	public static int DEFAULT_Y_PERCENT = 90;

	private int mXp;
	private int mYp;

	private final int mOffset;

	public PinPosition(Context context) {
		mOffset = Math.round(context.getResources().getDimensionPixelSize(
				android.R.dimen.app_icon_size) / 2f);
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		mXp = pref.getInt(context.getString(R.string.pref_key_pin_x_percent),
				DEFAULT_X_PERCENT);
		mYp = pref.getInt(context.getString(R.string.pref_key_pin_y_percent),
				DEFAULT_Y_PERCENT);
	}

	public PinPosition(Context context, int xp, int yp) {
		mOffset = Math.round(context.getResources().getDimensionPixelSize(
				android.R.dimen.app_icon_size) / 2f);
		mXp = xp;
		mYp = yp;
	}

	public int getOffset() {
		return mOffset;
	}

	public void save(Context context) {
		PreferenceManager
				.getDefaultSharedPreferences(context)
				.edit()
				.putInt(context.getString(R.string.pref_key_pin_x_percent), mXp)
				.putInt(context.getString(R.string.pref_key_pin_y_percent), mYp)
				.apply();
	}

	public int getX(View container) {
		return Math.round(container.getWidth() * mXp / 100f);
	}

	public int getY(View container) {
		return Math.round(container.getHeight() * mYp / 100f);
	}

	public int getXp(View continer) {
		return mXp;
	}

	public void setXp(View container, float x) {
		mXp = Math.round(x / container.getWidth() * 100f);
	}

	public void setXp(int xp) {
		mXp = xp;
	}

	public int getYp(View continer) {
		return mYp;
	}

	public void setYp(View container, float y) {
		mYp = Math.round(y / container.getHeight() * 100f);
	}

	public void setYp(int yp) {
		mYp = yp;
	}

	public void apply(View container) {
		{
			View left = container.findViewById(R.id.space_left);
			if (left != null) {
				LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) left
						.getLayoutParams();
				lp.weight = mXp;
				left.setLayoutParams(lp);
			}
		}
		{
			View right = container.findViewById(R.id.space_right);
			if (right != null) {
				LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) right
						.getLayoutParams();
				lp.weight = 100 - mXp;
				right.setLayoutParams(lp);
			}
		}
		{
			View top = container.findViewById(R.id.space_top);
			if (top != null) {
				LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) top
						.getLayoutParams();
				lp.weight = mYp;
				top.setLayoutParams(lp);
			}
		}
		{
			View bottom = container.findViewById(R.id.space_bottom);
			if (bottom != null) {
				LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) bottom
						.getLayoutParams();
				lp.weight = 100 - mYp;
				bottom.setLayoutParams(lp);
			}
		}
	}
}
