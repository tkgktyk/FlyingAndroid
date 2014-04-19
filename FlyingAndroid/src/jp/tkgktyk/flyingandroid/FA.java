package jp.tkgktyk.flyingandroid;

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
}
