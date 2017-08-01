package com.bignerdranch.android.photogallery;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.util.Log;

import java.util.List;

/*A basic example of IntentService implementation*/
public class PollService extends IntentService {

	private static final String TAG = "PollService";

	//60 second poll interval
	private static final int POLL_INTERVAL = 1000 * 60;

	public static Intent newIntent(Context context) {
		return new Intent(context, PollService.class);
	}

	public PollService() {
		super(TAG);
	}

	/*Toggles a timer; state depends on given boolean
	* Static so other components can invoke (other controller, other fragments)*/
	public static void setServiceAlarm(Context context, boolean isOn) {
		//Explicit intent
		Intent i = PollService.newIntent(context);

		/*getService(...)
		* Packages up an invocation of Context.startService(intent)
		* Returns an instance of PendingIntent whose parameters come from the
		* 	extras of the given intent
		* PARAMS
		* - context
		* - private request code for sender
		* - an explicit intent
		* - flags*/
		PendingIntent pi = PendingIntent.getService(context, 0, i, 0);

		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		if (isOn) {
			//Start alarm
			/*Params
			* - time basis for alarm
			* - time at which alarm starts
			* - interval at which to repeat alarm
			* - PendingIntent that will go off with alarm*/
			alarmManager.setInexactRepeating(
					AlarmManager.ELAPSED_REALTIME,
					SystemClock.elapsedRealtime(),
					POLL_INTERVAL,
					pi
			);
		} else {
			//Stop alarm
			alarmManager.cancel(pi);
			pi.cancel();
		}
	}

	/*Respond to intent
	* Called when the IntentService pulls a command off its queue to execute
	* Return prematurely if no network connection is found*/
	@Override
	protected void onHandleIntent(Intent intent) {
		if (!isNetworkAvailableAndConnected()) {
			return;
		}

		//Pull last query from SharedPreferences
		String query = QueryPreferences.getStoredQuery(this);
		//Get latest result set from current FlickrFetcher
		String lastResultId = QueryPreferences.getLastResultId(this);
		List<GalleryItem> items;

		//Populate FlickrFetcher accordingly
		if (query == null) {
			items = new FlickrFetchr().fetchRecentPhotos();
		} else {
			items = new FlickrFetchr().searchPhotos(query);
		}

		if (items.size() == 0) {
			return;
		}

		//If there are results, grab the first
		String resultId = items.get(0).getId();
		//If ID of first item matches last search query; If ID is different
		if (resultId.equals(lastResultId)) {
			Log.i(TAG, "Got an old result: " + resultId);
		} else {
			Log.i(TAG, "Got a new result: " + resultId);
		}

		//Store in SharedPreferences
		QueryPreferences.setLastResultId(this, resultId);
	}

	private boolean isNetworkAvailableAndConnected() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

		//getActiveNetworkInfo() - returns null is background data is disabled
		//Otherwise, returns an instance of android.net.NetworkInfo
		boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
		//Check if cm is fully connected; return result
		return isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();
	}

}