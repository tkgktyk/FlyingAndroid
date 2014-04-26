package jp.tkgktyk.flyingandroid;

import java.util.Collections;
import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

public class FA {

	public static void logD(String text) {
		if (BuildConfig.DEBUG) {
			XposedBridge.log("FA [DEBUG]: " + text);
		}
	}

	public static void logE(String text) {
		XposedBridge.log("FA: " + text);
	}

	public static void logE(Throwable t) {
		XposedBridge.log(t);
	}

	public static final int TAKEOFF_POSITION_CENTER = 0;
	public static final int TAKEOFF_POSITION_BOTTOM = 1;
	public static final int TAKEOFF_POSITION_LOWER_LEFT = 2;
	public static final int TAKEOFF_POSITION_LOWER_RIGHT = 3;

	public static final int PIN_POSITION_CENTER_LEFT = 1;
	public static final int PIN_POSITION_CENTER_RIGHT = 2;
	public static final int PIN_POSITION_LOWER_LEFT = 3;
	public static final int PIN_POSITION_LOWER_RIGHT = 4;

	public static final int AUTO_PIN_DISABLE = 0;
	public static final int AUTO_PIN_WHEN_TAKEOFF = 1;
	public static final int AUTO_PIN_AFTER_MOVING = 2;

	public static class Settings implements Cloneable {
		// for flying
		public float speed;
		public int takeoffPosition;
		public boolean notifyFlying;
		public boolean flyingDialog;
		public Set<String> blackSet;
		// for pinning
		public boolean usePin;
		public int pinXp;
		public int pinYp;
		private int autoPinSelection;
		public Set<String> whiteSet;
		private boolean flyingStatusBar;
		public boolean resetWhenCollapsed;

		public Settings(XSharedPreferences pref) {
			pref.reload();
			// for flying
			speed = Float.parseFloat(pref.getString("pref_key_speed", "1.5"));
			takeoffPosition = Integer.parseInt(pref.getString(
					"pref_key_takeoff_position", "0"));
			notifyFlying = pref.getBoolean("pref_key_notify_flying", true);
			flyingDialog = pref.getBoolean("pref_key_flying_dialog", false);
			blackSet = pref.getStringSet("pref_key_black_list",
					Collections.<String> emptySet());
			// for pinning
			usePin = pref.getBoolean("pref_key_use_pin", false);
			pinXp = pref.getInt("pref_key_pin_x_percent",
					PinPosition.DEFAULT_X_PERCENT);
			pinYp = pref.getInt("pref_key_pin_y_percent",
					PinPosition.DEFAULT_Y_PERCENT);
			autoPinSelection = Integer.parseInt(pref.getString(
					"pref_key_auto_pin_selection", "3"));
			whiteSet = pref.getStringSet("pref_key_white_list",
					Collections.<String> emptySet());
			flyingStatusBar = pref.getBoolean("pref_key_flying_status_bar",
					false);
			resetWhenCollapsed = pref.getBoolean(
					"pref_key_reset_when_collapsed", false);
		}

		public void overwriteUsePinByWhiteList(String packageName) {
			if (usePin) {
				boolean newUsePin = whiteSet.isEmpty()
						|| whiteSet.contains(packageName);
				usePin = newUsePin;
			}
		}

		public boolean autoPin(int selection) {
			return usePin && autoPinSelection == selection;
		}

		public boolean useFlyingStatusBar() {
			return usePin && flyingStatusBar;
		}

		@Override
		public Settings clone() {
			Settings ret = null;
			try {
				ret = (Settings) super.clone();
			} catch (CloneNotSupportedException e) {
				XposedBridge.log(e);
			}
			return ret;
		}
	}
}
