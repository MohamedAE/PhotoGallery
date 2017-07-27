package com.bignerdranch.android.photogallery;

import android.content.Context;
import android.preference.PreferenceManager;

/*An interface class used for reading and writing
* to and from the app's shared preferences
*
* Shared preferences
* - Files on the filesystem storing key-values
* - Backed up in persistent storage*/
public class QueryPreferences {

    private static final String PREF_SEARCH_QUERY = "searchQuery";

    /*Read stored queries from shared preferences*/
    public static String getStoredQuery(Context context) {
        //Get an instance of PreferenceManager unique to the given context
        return PreferenceManager.getDefaultSharedPreferences(context)
                /*getString(key, default)
                * Get value associated with given key or the given default*/
                .getString(PREF_SEARCH_QUERY, null);
    }

    /*Write queries to shared preferences*/
    public static void setStoredQuery(Context context, String query) {
        //Get an instance of PreferenceManager unique to the given context
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                //putString(key, value)
                .putString(PREF_SEARCH_QUERY, query)
                .apply();
    }

}