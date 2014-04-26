package jp.tkgktyk.flyingandroid.app;

import jp.tkgktyk.flyingandroid.PinPosition;
import jp.tkgktyk.flyingandroid.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.View;
import android.view.View.OnDragListener;
import android.view.View.OnLongClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AbsoluteLayout;
import android.widget.ToggleButton;

@SuppressWarnings("deprecation")
public class MovePinActivity extends Activity {

	private ToggleButton mPin;
	private View mLineH;
	private View mLineV;
	private View mContainer;

	private PinPosition mPinPosition;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_move_pin);

		mPinPosition = new PinPosition(this);

		mContainer = findViewById(R.id.container);
		mLineH = findViewById(R.id.h_line);
		mLineV = findViewById(R.id.v_line);
		mPin = (ToggleButton) findViewById(R.id.pin);

		mPin.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				v.startDrag(null, new View.DragShadowBuilder(v), v, 0);
				return true;
			}
		});
		mContainer.setOnDragListener(new OnDragListener() {
			private void updatePosition(DragEvent event) {
				float x = event.getX();
				float y = event.getY();
				mPinPosition.setXp(mContainer, x);
				mPinPosition.setYp(mContainer, y);
				move();
			}

			@Override
			public boolean onDrag(View v, DragEvent event) {
				switch (event.getAction()) {
				case DragEvent.ACTION_DRAG_STARTED: {
					updatePosition(event);
					break;
				}
				case DragEvent.ACTION_DRAG_LOCATION: {
					updatePosition(event);
					break;
				}
				case DragEvent.ACTION_DROP: {
					updatePosition(event);
					mPinPosition.save(v.getContext());
					break;
				}
				default:
					break;
				}
				// must return true for dragging progress
				return true;
			}
		});

		mContainer.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() {
					@SuppressLint("NewApi")
					@Override
					public void onGlobalLayout() {
						move();

						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
							mContainer.getViewTreeObserver()
									.removeOnGlobalLayoutListener(this);
						} else {
							mContainer.getViewTreeObserver()
									.removeGlobalOnLayoutListener(this);
						}
					}
				});
	}

	private void move() {
		int x = mPinPosition.getX(mContainer);
		int y = mPinPosition.getY(mContainer);
		AbsoluteLayout.LayoutParams lp = (AbsoluteLayout.LayoutParams) mPin
				.getLayoutParams();
		lp.x = x - mPinPosition.getOffset();
		lp.y = y - mPinPosition.getOffset();
		mPin.setLayoutParams(lp);
		lp = (AbsoluteLayout.LayoutParams) mLineH.getLayoutParams();
		lp.y = y;
		mLineH.setLayoutParams(lp);
		lp = (AbsoluteLayout.LayoutParams) mLineV.getLayoutParams();
		lp.x = x;
		mLineV.setLayoutParams(lp);
	}
}
