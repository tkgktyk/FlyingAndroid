package jp.tkgktyk.flyingandroid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class BlackListFragment extends ListFragment implements
		LoaderManager.LoaderCallbacks<List<BlackListFragment.Entry>> {

	public static class Entry {
		Drawable icon;
		String appName;
		String packageName;

		public Entry(Drawable icon, String appName, String packageName) {
			this.icon = icon;
			this.appName = appName;
			this.packageName = packageName;
		}
	}

	private class Adapter extends ArrayAdapter<Entry> {
		private final LayoutInflater mInflater;

		public Adapter(Context context, List<Entry> entries) {
			super(context, android.R.id.empty, entries);
			mInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		class ViewHolder {
			ImageView icon;
			TextView text;
		}

		private View createView(ViewGroup parent) {
			View view = mInflater.inflate(
					android.R.layout.simple_list_item_multiple_choice, parent,
					false);
			ViewHolder holder = new ViewHolder();
			// holder.icon = (ImageView) view.findViewById(android.R.id.icon);
			holder.text = (TextView) view.findViewById(android.R.id.text1);
			view.setTag(holder);

			return view;
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			if (view == null) {
				view = createView(parent);
			}
			ViewHolder holder = (ViewHolder) view.getTag();

			Entry entry = getItem(position);
			//
			// holder.icon.setImageDrawable(entry.icon);
			holder.text.setText(entry.appName);
			holder.text.setCompoundDrawablesWithIntrinsicBounds(entry.icon,
					null, null, null);

			return view;
		}
	}

	private boolean mShowOnlyBlack;
	private boolean mSave;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public BlackListFragment() {
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		setEmptyText(getString(R.string.No_applications));
		getLoaderManager().initLoader(0, null, this);

		mSave = false;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		mSave = true;
	}

	public void setShowOnlyBlack(boolean only) {
		if (only != mShowOnlyBlack) {
			saveBlackList();
			mShowOnlyBlack = only;
			setListShown(false);
			getLoaderManager().initLoader(0, null, this);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		saveBlackList();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		getLoaderManager().destroyLoader(0);
	}

	@Override
	public Loader<List<Entry>> onCreateLoader(int id, Bundle bundle) {
		setListShown(false);
		return new BlackListLoader(getActivity());
	}

	@Override
	public void onLoadFinished(Loader<List<Entry>> loader, List<Entry> entries) {
		List<Entry> deliverer = new ArrayList<Entry>();
		Adapter adapter = new Adapter(getActivity(), deliverer);
		setListAdapter(adapter);
		Set<String> blackSet = PreferenceManager.getDefaultSharedPreferences(
				getActivity()).getStringSet(
				getString(R.string.pref_key_black_list), new HashSet<String>());
		for (Entry entry : entries) {
			boolean black = blackSet.contains(entry.packageName);
			if (!mShowOnlyBlack || black) {
				deliverer.add(entry);
				getListView().setItemChecked(deliverer.size() - 1, black);
			}
		}
		adapter.notifyDataSetChanged();

		setListShown(true);
	}

	@Override
	public void onLoaderReset(Loader<List<Entry>> loader) {
		// TODO do nothing?
	}

	public static class BlackListLoader extends MyAsyncTaskLoader<List<Entry>> {

		public BlackListLoader(Context context) {
			super(context);
		}

		@Override
		public List<Entry> loadInBackground() {
			List<Entry> ret = new ArrayList<Entry>();
			Context context = getContext();

			// get installed application's info
			PackageManager pm = context.getPackageManager();
			List<ApplicationInfo> apps = pm
					.getInstalledApplications(PackageManager.GET_META_DATA);
			Collections.sort(apps,
					new ApplicationInfo.DisplayNameComparator(pm));
			for (ApplicationInfo info : apps) {
				Drawable icon = pm.getApplicationIcon(info);
				String appName = (String) pm.getApplicationLabel(info);
				String packageName = info.packageName;
				ret.add(new Entry(icon, appName, packageName));
			}
			return ret;
		}
	}

	public void saveBlackList() {
		if (!mSave)
			return;
		Set<String> blackSet = new HashSet<String>();
		SparseBooleanArray checked = getListView().getCheckedItemPositions();
		Adapter adapter = (Adapter) getListAdapter();
		for (int i = 0; i < checked.size(); ++i) {
			if (checked.valueAt(i)) {
				Entry entry = adapter.getItem(checked.keyAt(i));
				blackSet.add(entry.packageName);
			}
		}
		PreferenceManager
				.getDefaultSharedPreferences(getActivity())
				.edit()
				.putStringSet(getString(R.string.pref_key_black_list), blackSet)
				.apply();
	}
}
