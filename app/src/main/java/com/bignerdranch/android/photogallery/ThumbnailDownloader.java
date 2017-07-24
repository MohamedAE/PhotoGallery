package com.bignerdranch.android.photogallery;

import android.os.HandlerThread;
import android.util.Log;

/*Class to handle background downloading of thumbnails
* HandlerThread
* - Used for starting a new thread that has a looper*/
public class ThumbnailDownloader<T> extends HandlerThread {

	private static final String TAG = "ThumbnailDownloader";

	public ThumbnailDownloader() {
		super(TAG);
	}

	public void queueThumbnail(T target, String url) {
		Log.i(TAG, "Got a URL: " + url);
	}

}