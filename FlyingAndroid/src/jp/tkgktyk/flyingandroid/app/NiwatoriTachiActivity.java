package jp.tkgktyk.flyingandroid.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jp.tkgktyk.flyingandroid.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class NiwatoriTachiActivity extends Activity {

	private class Adapter extends ArrayAdapter<Boolean> {
		private Drawable niwatori;

		public Adapter(Context context, List<Boolean> objects) {
			super(context, android.R.id.empty, objects);
			niwatori = getResources().getDrawable(R.drawable.ic_launcher);
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			ImageView icon = null;
			if (view == null) {
				icon = new ImageView(NiwatoriTachiActivity.this);
				icon.setImageDrawable(niwatori);
			} else {
				icon = (ImageView) view;
			}
			Boolean exist = getItem(position);
			if (exist) {
				icon.setVisibility(View.VISIBLE);
			} else {
				icon.setVisibility(View.INVISIBLE);
			}

			return icon;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final GridView niwatoriTachi = new GridView(this);
		niwatoriTachi.setHorizontalScrollBarEnabled(false);
		niwatoriTachi.setVerticalScrollBarEnabled(false);
		final int iconSize = getResources().getDimensionPixelSize(
				android.R.dimen.app_icon_size);
		niwatoriTachi.setColumnWidth(iconSize);
		niwatoriTachi.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);

		niwatoriTachi.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() {
					@SuppressWarnings("deprecation")
					@SuppressLint("NewApi")
					@Override
					public void onGlobalLayout() {
						int w = niwatoriTachi.getWidth();
						int h = niwatoriTachi.getHeight();
						int columns = w / iconSize;
						int rows = h / iconSize;
						int n = columns * rows;
						List<Boolean> entries = new ArrayList<Boolean>(n);
						Random r = new Random();
						for (int i = 0; i < n; ++i) {
							entries.add(r.nextBoolean());
						}
						if (r.nextBoolean()) {
							niwatoriTachi.setNumColumns(columns);
						}
						niwatoriTachi.setAdapter(new Adapter(
								NiwatoriTachiActivity.this, entries));

						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
							niwatoriTachi.getViewTreeObserver()
									.removeOnGlobalLayoutListener(this);
						} else {
							niwatoriTachi.getViewTreeObserver()
									.removeGlobalOnLayoutListener(this);
						}
					}
				});
		setContentView(niwatoriTachi);
	}
}
