package com.bignerdranch.android.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/*Class to handle background downloading of thumbnails
* HandlerThread
* - Used for starting a new thread that has a looper*/
public class ThumbnailDownloader<T> extends HandlerThread {

	private static final String TAG = "ThumbnailDownloader";
	//Used to identify messages and download requests
	private static final int MESSAGE_DOWNLOAD = 0;
	private static final int MESSAGE_PRELOAD = 1;

	//Reference to Handler of background thread
	private Handler mRequestHandler;
	//HashMap for storage/retrieval of URL associated with request
	private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();
	//Reference to Handler passed from the main thread
	private Handler mResponseHandler;
	private ThumbnailDownloadListener<T> mThumbnailDownloadListener;
	//Used to implement "last recently used" caching strategy
	private LruCache<String, Bitmap> mLruCache;

	/*Using an interface allows implementing classes to determine what to do with
	* the downloaded image (PhotoGalleryFragment)*/
	public interface ThumbnailDownloadListener<T> {
		void onThumbnailDownloaded(T target, Bitmap bitmap);
	}

	public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
		mThumbnailDownloadListener = listener;
	}

	public ThumbnailDownloader(Handler responseHandler) {
		super(TAG);
		mResponseHandler = responseHandler;
		mLruCache = new LruCache<String, Bitmap>(16384);
	}

	/*Method called before the first time the Looper checks for messages*/
	@Override
	protected void onLooperPrepared() {
		mRequestHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				//Check message type
				if (msg.what == MESSAGE_DOWNLOAD) {
					//Retrieve obj value
					T target = (T) msg.obj;
					Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
					handleRequest(target);
				}
			}
		};
	}

	/*target - reference to PhotoHolder from the RecyclerView
	* url	 - url to download*/
	public void queueThumbnail(T target, String url) {
		Log.i(TAG, "Got a URL: " + url);

		if (url == null) {
			mRequestMap.remove(target);
		} else {
			//Add PhotoHolder and URL to HashMap
			mRequestMap.put(target, url);
			//obtainMessage(...) - generates message from recycled messages
			mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
					.sendToTarget();
		}
	}

	public void preloadImage(String url) {
		mRequestHandler.obtainMessage(MESSAGE_PRELOAD, url).sendToTarget();
	}

	//Clear Handler of background thread
	public void clearQueue() {
		mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
	}

	public void clearCache() {
		mLruCache.evictAll();
	}

	public Bitmap getCachedImage(String url) {
		return mLruCache.get(url);
	}

	/*Where downloading happens*/
	private void handleRequest(final T target) {
		try {
			final String url = mRequestMap.get(target);

			if (url == null) {
				return;
			}

			//Get stream of bytes from URL
			byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
			//Decode stream; save to Bitmap
			final Bitmap bitmap = BitmapFactory
					.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
			Log.i(TAG, "Bitmap created");

			//Add Runnable to Message Queue of the main thread's Handler
			mResponseHandler.post(new Runnable() {
				public void run() {
					if (mRequestMap.get(target) != url) {
						return;
					}

					//Remove PhotoHolder/URL mapping; set Bitmap on PhotoHolder
					mRequestMap.remove(target);
					mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
				}
			});
		} catch (IOException ioe) {
			Log.e(TAG, "Error downloading image", ioe);
		}
	}

}