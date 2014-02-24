package jp.tkgktyk.flyingandroid;

import android.content.Context;
import android.util.AttributeSet;

public class FlyingView1 extends FlyingView {

	public FlyingView1(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public FlyingView1(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public FlyingView1(Context context) {
		super(context);
	}

	private int mOffsetX;
	private int mOffsetY;

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		/*
		 * not work correctly because padding doesn't have negative value.
		 */
		setPadding(getPaddingLeft() + mOffsetX, getPaddingTop() + mOffsetY,
				getPaddingRight() + mOffsetX, getPaddingBottom() + mOffsetY);
		/*
		 * not work correctly because FrameLayout doesn't use onLayout's
		 * arguments (layouts on upper left).
		 */
		super.onLayout(true, left, top, right, bottom);

		setPadding(getPaddingLeft() - mOffsetX, getPaddingTop() - mOffsetY,
				getPaddingRight() - mOffsetX, getPaddingBottom() - mOffsetY);
	}

	@Override
	public void move(int deltaX, int deltaY) {
		int hLimit = getWidth() - getHorizontalPadding();
		int vLimit = getHeight() - getVerticalPadding();
		mOffsetX = clamp(mOffsetX + deltaX, hLimit);
		mOffsetY = clamp(mOffsetY + deltaY, vLimit);

		requestLayout();
	}

	@Override
	public void returnToHome() {
		mOffsetX = 0;
		mOffsetY = 0;

		requestLayout();
	}

	@Override
	public void rotate() {
	}
}