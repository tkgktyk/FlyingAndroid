package jp.tkgktyk.flyingandroid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

public class FlyingView1 extends FlyingView {
	private static int DEFAULT_CHILD_GRAVITY;

	static {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			DEFAULT_CHILD_GRAVITY = Gravity.TOP | Gravity.START;
		} else {
			DEFAULT_CHILD_GRAVITY = Gravity.TOP;
		}
	}

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
		layoutChildren(left, top, right, bottom, false /* no force left gravity */);
	}

	@SuppressLint("NewApi")
	void layoutChildren(int left, int top, int right, int bottom,
			boolean forceLeftGravity) {
		final int count = getChildCount();

		final int parentLeft = getPaddingLeft();
		final int parentRight = right - left - getPaddingRight();

		final int parentTop = getPaddingTop();
		final int parentBottom = bottom - top - getPaddingBottom();

		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != GONE) {
				final LayoutParams lp = (LayoutParams) child.getLayoutParams();

				final int width = child.getMeasuredWidth();
				final int height = child.getMeasuredHeight();

				int childLeft;
				int childTop;

				int gravity = lp.gravity;
				if (gravity == -1) {
					gravity = DEFAULT_CHILD_GRAVITY;
				}

				final int layoutDirection = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) ? getLayoutDirection()
						: 0;
				final int absoluteGravity = Gravity.getAbsoluteGravity(gravity,
						layoutDirection);
				final int verticalGravity = gravity
						& Gravity.VERTICAL_GRAVITY_MASK;

				switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
				case Gravity.CENTER_HORIZONTAL:
					childLeft = parentLeft + (parentRight - parentLeft - width)
							/ 2 + lp.leftMargin - lp.rightMargin;
					break;
				case Gravity.RIGHT:
					if (!forceLeftGravity) {
						childLeft = parentRight - width - lp.rightMargin;
						break;
					}
				case Gravity.LEFT:
				default:
					childLeft = parentLeft + lp.leftMargin;
				}

				switch (verticalGravity) {
				case Gravity.TOP:
					childTop = parentTop + lp.topMargin;
					break;
				case Gravity.CENTER_VERTICAL:
					childTop = parentTop + (parentBottom - parentTop - height)
							/ 2 + lp.topMargin - lp.bottomMargin;
					break;
				case Gravity.BOTTOM:
					childTop = parentBottom - height - lp.bottomMargin;
					break;
				default:
					childTop = parentTop + lp.topMargin;
				}

				if (i == 0) {
					child.layout(childLeft + mOffsetX, childTop + mOffsetY,
							childLeft + width + mOffsetX, childTop + height
									+ mOffsetY);
				} else {
					child.layout(childLeft, childTop, childLeft + width,
							childTop + height);
				}
			}
		}
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
		mOffsetX = Math.round(mOffsetX * 1f / getWidth() * getHeight());
		mOffsetY = Math.round(mOffsetY * 1f / getHeight() * getWidth());
	}
}