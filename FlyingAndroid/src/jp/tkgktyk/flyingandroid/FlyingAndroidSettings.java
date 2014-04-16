package jp.tkgktyk.flyingandroid;

import java.util.Collections;
import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

public class FlyingAndroidSettings implements Cloneable {
	public static final int TAKEOFF_POSITION_CENTER = 0;
	public static final int TAKEOFF_POSITION_BOTTOM = 1;
	public static final int TAKEOFF_POSITION_LOWER_LEFT = 2;
	public static final int TAKEOFF_POSITION_LOWER_RIGHT = 3;

	public static final int PIN_POSITION_CENTER_LEFT = 1;
	public static final int PIN_POSITION_CENTER_RIGHT = 2;
	public static final int PIN_POSITION_LOWER_LEFT = 3;
	public static final int PIN_POSITION_LOWER_RIGHT = 4;

	// for flying
	public float speed;
	public int takeoffPosition;
	public boolean notifyFlying;
	public boolean forceSetWindowBackground;
	public boolean flyingDialog;
	public Set<String> blackSet;
	// for pinning
	public boolean usePin;
	public int pinPosition;
	private boolean autoPin;
	public Set<String> whiteSet;
	private boolean flyingStatusBar;
	public boolean resetWhenCollapsed;

	public FlyingAndroidSettings(XSharedPreferences pref) {
		pref.reload();
		// for flying
		speed = Float.parseFloat(pref.getString("pref_key_speed", "1.5"));
		takeoffPosition = Integer.parseInt(pref.getString(
				"pref_key_takeoff_position", "0"));
		notifyFlying = pref.getBoolean("pref_key_notify_flying", true);
		forceSetWindowBackground = pref.getBoolean(
				"pref_key_force_set_window_background", false);
		flyingDialog = pref.getBoolean("pref_key_flying_dialog", false);
		blackSet = pref.getStringSet("pref_key_black_list",
				Collections.<String> emptySet());
		// for pinning
		usePin = pref.getBoolean("pref_key_use_pin", false);
		pinPosition = Integer.parseInt(pref.getString("pref_key_pin_position",
				"3"));
		autoPin = pref.getBoolean("pref_key_auto_pin", false);
		whiteSet = pref.getStringSet("pref_key_white_list",
				Collections.<String> emptySet());
		flyingStatusBar = pref.getBoolean("pref_key_flying_status_bar", false);
		resetWhenCollapsed = pref.getBoolean("pref_key_reset_when_collapsed",
				false);
	}

	public void overwriteUsePinByWhiteList(String packageName) {
		if (usePin) {
			boolean newUsePin = whiteSet.isEmpty()
					|| whiteSet.contains(packageName);
			usePin = newUsePin;
		}
	}

	public boolean autoPin() {
		return usePin && autoPin;
	}

	public boolean useFlyingStatusBar() {
		return usePin && flyingStatusBar;
	}

	@Override
	public FlyingAndroidSettings clone() {
		FlyingAndroidSettings ret = null;
		try {
			ret = (FlyingAndroidSettings) super.clone();
		} catch (CloneNotSupportedException e) {
			XposedBridge.log(e);
		}
		return ret;
	}
}
