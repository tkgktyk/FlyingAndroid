package jp.tkgktyk.flyingandroid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jp.tkgktyk.flyingandroid.util.forGB;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListView;

public class BlackListFragment extends ListFragment implements
		LoaderManager.LoaderCallbacks<List<BlackListFragment.Entry>> {

	public static class Entry {
		public final Drawable icon;
		public final String appName;
		public final String packageName;

		public boolean black = false;

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
			CheckedTextView text;
		}

		private View createView(ViewGroup parent) {
			View view = mInflater.inflate(R.layout.view_selectable_app, parent,
					false);
			ViewHolder holder = new ViewHolder();
			holder.icon = (ImageView) view.findViewById(android.R.id.icon);
			holder.text = (CheckedTextView) view
					.findViewById(android.R.id.text1);
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
			holder.icon.setImageDrawable(entry.icon);
			holder.text.setText(entry.appName);
			holder.text.setChecked(entry.black);

			return view;
		}
	}

	private boolean mShowOnlyBlack;
	private boolean mSave = false;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public BlackListFragment() {
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		setEmptyText(getString(R.string.No_applications));
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Adapter adapter = (Adapter) getListAdapter();
		Entry entry = adapter.getItem(position);
		entry.black = !entry.black;
		adapter.notifyDataSetChanged();

		mSave = true;
	}

	public void setShowOnlyBlack(boolean only) {
		if (only != mShowOnlyBlack) {
			saveBlackList();
			mShowOnlyBlack = only;
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

		Set<String> blackSet = forGB.getStringSet(getActivity(),
				R.string.pref_key_black_list);
		for (Entry entry : entries) {
			entry.black = blackSet.contains(entry.packageName);
			if (!mShowOnlyBlack || entry.black) {
				deliverer.add(entry);
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
		Adapter adapter = (Adapter) getListAdapter();
		for (int i = 0; i < adapter.getCount(); ++i) {
			Entry entry = adapter.getItem(i);
			if (entry.black) {
				blackSet.add(entry.packageName);
			}
		}
		forGB.putStringSet(getActivity(), R.string.pref_key_black_list,
				blackSet);
		mSave = false;
	}
}
