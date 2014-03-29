package jp.tkgktyk.flyingandroid;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class FlyingView2 extends FlyingView {

	public FlyingView2(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public FlyingView2(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public FlyingView2(Context context) {
		super(context);
	}

	public void move(int deltaX, int deltaY) {
		int hLimit = getWidth() - getHorizontalPadding();
		int vLimit = getHeight() - getVerticalPadding();

		/*
		 * If move this view, the coordinates of touch is changed
		 * simultaneously. So we must move child views.
		 */
		// for (int i = 0; i < getChildCount(); ++i) {
		View child = getChildAt(0);
		ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child
				.getLayoutParams();
		lp.leftMargin = clamp(lp.leftMargin + deltaX, hLimit);
		lp.rightMargin = -lp.leftMargin;
		lp.topMargin = clamp(lp.topMargin + deltaY, vLimit);
		lp.bottomMargin = -lp.topMargin;
		child.setLayoutParams(lp);
		// }
	}

	public void returnToHome() {
		// for (int i = 0; i < getChildCount(); ++i) {
		View child = getChildAt(0);
		ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child
				.getLayoutParams();
		lp.leftMargin = 0;
		lp.rightMargin = 0;
		lp.topMargin = 0;
		lp.bottomMargin = 0;
		child.setLayoutParams(lp);
		// }
	}

	public void rotate() {
		// for (int i = 0; i < getChildCount(); ++i) {
		View child = getChildAt(0);
		ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child
				.getLayoutParams();
		lp.leftMargin = Math.round(lp.leftMargin * 1f / getWidth()
				* getHeight());
		lp.rightMargin = -lp.leftMargin;
		lp.topMargin = Math.round(lp.topMargin * 1f / getHeight() * getWidth());
		lp.bottomMargin = -lp.topMargin;
		child.setLayoutParams(lp);
		// }
	}
}