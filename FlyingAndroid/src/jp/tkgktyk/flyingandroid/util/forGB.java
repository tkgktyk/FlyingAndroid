package jp.tkgktyk.flyingandroid.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

public class forGB {
	private static final String TAG = forGB.class.getSimpleName();

	public static Set<String> getStringSet(Context context, int keyId) {
		String key = context.getString(keyId);
		return getStringSet(context, key);
	}

	@SuppressWarnings("unchecked")
	@SuppressLint("NewApi")
	public static Set<String> getStringSet(Context context, String key) {
		Set<String> result = null;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			result = PreferenceManager.getDefaultSharedPreferences(context)
					.getStringSet(key, Collections.<String> emptySet());
		} else {
			try {
				FileInputStream fis = context.openFileInput(key);
				ObjectInputStream ois = new ObjectInputStream(fis);
				result = (Set<String>) ois.readObject();
				ois.close();
				fis.close();
			} catch (FileNotFoundException e) {
				Log.w(TAG, "a local file is not found: " + key);
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG, e.toString());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				Log.e(TAG, e.toString());
			}
			if (result == null)
				result = Collections.emptySet();
		}
		return result;
	}

	public static void putStringSet(Context context, int keyId,
			Set<String> value) {
		String key = context.getString(keyId);
		putStringSet(context, key, value);
	}

	@SuppressLint("NewApi")
	public static void putStringSet(Context context, String key,
			Set<String> value) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			PreferenceManager.getDefaultSharedPreferences(context).edit()
					.putStringSet(key, value).apply();
		} else {
			try {
				FileOutputStream fos = context.openFileOutput(key,
						Context.MODE_PRIVATE);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(value);
				oos.close();
				fos.close();
			} catch (FileNotFoundException e) {
				Log.w(TAG, "a local file is not found: " + key);
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG, e.toString());
			}
		}
	}
}
