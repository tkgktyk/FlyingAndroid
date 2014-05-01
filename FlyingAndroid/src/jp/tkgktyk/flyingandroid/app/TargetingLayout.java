package jp.tkgktyk.flyingandroid.app;

import android.R.color;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

public class TargetingLayout extends FrameLayout {

	private static final String TAG = TargetingLayout.class.getSimpleName();

	private View mTarget;

	private Paint mLinePaint;

	public TargetingLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public TargetingLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TargetingLayout(Context context) {
		super(context);
		init();
	}

	private void init() {
		setWillNotDraw(false);
		mLinePaint = new Paint();
		mLinePaint.setColor(getResources().getColor(
				android.R.color.holo_orange_light));
	}

	public void setTarget(int id) {
		View v = findViewById(id);
		if (v != null) {
			mTarget = v;
		} else {
			Log.e(TAG, "target is not found in child views.");
		}
	}

	public int getTargetId() {
		return mTarget.getId();
	}

	public View getTargetView() {
		return mTarget;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		int left = 0;
		int top = 0;
		for (View v = mTarget; v != this; v = (View) v.getParent()) {
			left += v.getLeft();
			top += v.getTop();
		}
		left += mTarget.getWidth() / 2;
		top += mTarget.getHeight() / 2;
		canvas.drawLine(left, 0, left, getHeight(), mLinePaint);
		canvas.drawLine(0, top, getWidth(), top, mLinePaint);
	}
}
