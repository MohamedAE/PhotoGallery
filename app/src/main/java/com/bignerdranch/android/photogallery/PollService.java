package com.bignerdranch.android.photogallery;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.List;

/*A basic example of IntentService implementation
* Registered as a service with the system in the manifest file*/
public class PollService extends IntentService {

	private static final String TAG = "PollService";

	//15 minute poll interval
	private static final long POLL_INTERVAL = 1000;

	public static final String ACTION_SHOW_NOTIFICATION =
			"com.bignerdranch.android.photogallery.SHOW_NOTIFICATION";
	//Permission key; any application receiving broadcasts from this component
    //this permission key; also defined in manifest file
	public static final String PERM_PRIVATE = "com.bignerdranch.android.photogallery.PRIVATE";

    public static final String REQUEST_CODE = "REQUEST_CODE";
    public static final String NOTIFICATION = "NOTIFICATION";

	public static Intent newIntent(Context context) {
		return new Intent(context, PollService.class);
	}

	public PollService() {
		super(TAG);
	}

	/*Toggles a timer; state depends on given boolean
	* Static so other components can invoke (other controller, other fragments)*/
	public static void setServiceAlarm(Context context, boolean isOn) {
		//An explicit intent; a PollService is the Intent to be passed AlarmManager
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
			/*setInexactRepeating(...)
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

		//Place alarm state in SharedPreferences
		QueryPreferences.setAlarmOn(context, isOn);
	}

	/*Return boolean indicator of alarm state: true/false - alarm set/not set*/
	public static boolean isServiceAlarmOn(Context context) {
		Intent i = PollService.newIntent(context);
		PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_NO_CREATE);
		return pi != null;
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

			Resources resources = getResources();
			Intent i = PhotoGalleryActivity.newIntent(this);
			//Prepare a PendingIntent that will restart this activity
			PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

			//Create new notification object
			Notification notification = new NotificationCompat.Builder(this)
					//Configure ticker text
					.setTicker(resources.getString(R.string.new_pictures_title))
					//Configure small icon
					.setSmallIcon(android.R.drawable.ic_menu_report_image)
					//Configure notification title
					.setContentTitle(resources.getString(R.string.new_pictures_title))
					//Configure notification text
					.setContentText(resources.getString(R.string.new_pictures_text))
					//PendingIntent fired when user presses notification
					.setContentIntent(pi)
					//Set notification to cancel once clicked
					.setAutoCancel(true)
					.build();

			//Broadcast as an ordered broadcast
			showBackgroundNotification(0, notification);
		}

		//Store in SharedPreferences
		QueryPreferences.setLastResultId(this, resultId);
	}

	/*Constructs an ordered broadcast
	* Includes an explicit intent and a notification object*/
	private void showBackgroundNotification(int requestCode, Notification notification) {
        Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
        i.putExtra(REQUEST_CODE, requestCode);
        i.putExtra(NOTIFICATION, notification);

        /*sendOrderedBroadcast(...)
        * Version of sendBroadcast(...) that allows received data back from the broadcast
        * - intent
        * - receiver permission key
        * - result receiver
        * - handler
        * - initial code
        * - initial data
        * - initial extras*/
        sendOrderedBroadcast(
                i,
                PERM_PRIVATE,
                null,
                null,
                Activity.RESULT_OK,
                null,
                null
        );
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