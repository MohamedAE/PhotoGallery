package com.bignerdranch.android.photogallery;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

public class PhotoGalleryActivity extends SingleFragmentActivity {

    /*Return an Intent instance used to start PhotoGalleryActivity.
    * Eventually, PollService will call this, wrap the result in a PendingIntent,
    * and set that PendingIntent on a notification*/
    public static Intent newIntent(Context context) {
        return new Intent(context, PhotoGalleryActivity.class);
    }

    @Override
    public Fragment createFragment() {
        return PhotoGalleryFragment.newInstance();
    }
}