package com.bignerdranch.android.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.widget.Toast;

/*A dynamic broadcast receiver
* - registered in code, not in manifest file
* Responds to SHOW_NOTIFICATION broadcasts
* A generic fragment that hides foreground notifications*/
public abstract class VisibleFragment extends Fragment {

    private static final String TAG = "VisibleFragment";

    @Override
    public void onStart() {
        super.onStart();
        //An intent filter that listens for ACTION_SHOW_NOTIFICATION
        IntentFilter filter = new IntentFilter(PollService.ACTION_SHOW_NOTIFICATION);
        //Register this activity with the system as a receiver
        getActivity().registerReceiver(mOnShowNotification, filter, PollService.PERM_PRIVATE, null);
    }

    @Override
    public void onStop() {
        super.onStop();
        //Unregister this activity as a receiver
        getActivity().unregisterReceiver(mOnShowNotification);
    }

    //Generates this activity's broadcast receiver; overrides onReceive(...)
    private BroadcastReceiver mOnShowNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(getActivity(),
                    "Got a broadcast: " + intent.getAction(),
                    Toast.LENGTH_LONG)
                    .show();
        }
    };

}