package jp.tkgktyk.flyingandroid.app;

import jp.tkgktyk.flyinglayout.FlyingLayoutF;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

public class GridLinedFlyingLayout extends FlyingLayoutF {

	private Paint mLinePaint;

	public GridLinedFlyingLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public GridLinedFlyingLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public GridLinedFlyingLayout(Context context) {
		super(context);
		init();
	}

	private void init() {
		setWillNotDraw(false);
		mLinePaint = new Paint();
		mLinePaint.setColor(getResources().getColor(
				android.R.color.holo_blue_light));
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		for (int i = 0; i <= 10; ++i) {
			float x = getWidth() * i / 10f;
			float y = getHeight() * i / 10f;
			canvas.drawLine(x, 0, x, getHeight(), mLinePaint);
			canvas.drawLine(0, y, getWidth(), y, mLinePaint);
		}
	}
}
