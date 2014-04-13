package jp.tkgktyk.flyingandroid;

import android.content.AsyncTaskLoader;
import android.content.Context;

public abstract class MyAsyncTaskLoader<D> extends AsyncTaskLoader<D> {
	
	private D mData = null;

	public MyAsyncTaskLoader(Context context) {
		super(context);
	}
	
    /* Runs on the UI thread */
    @Override
    public void deliverResult(D data) {
        if (isReset()) {
            // An async query came in while the loader is stopped
            return;
        }
        mData = data;
        super.deliverResult(data);
    }

    /**
     * Starts an asynchronous load of the contacts list data. When the result is ready the callbacks
     * will be called on the UI thread. If a previous load has been completed and is still valid
     * the result may be passed to the callbacks immediately.
     *
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading() {
        if (mData != null) {
            deliverResult(mData);
        }
        if (takeContentChanged() || mData == null) {
            forceLoad();
        }
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();
        
        // Ensure the loader is stopped
        onStopLoading();

        mData = null;
    }
}
