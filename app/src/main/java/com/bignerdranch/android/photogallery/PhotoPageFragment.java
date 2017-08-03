package com.bignerdranch.android.photogallery;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

/*Fragment defining the WebView generated when an image is clicked/expanded*/
public class PhotoPageFragment extends VisibleFragment {

    private static final String ARG_URI = "photo_page_url";

    private Uri mUri;
    private WebView mWebView;

    public static PhotoPageFragment newInstance(Uri uri) {
        //Create bundle; place given URI into it
        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, uri);

        //New PhotoPageFragment; associate with above bundle
        PhotoPageFragment fragment = new PhotoPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Return the arguments supplied to the fragment when it was created
        mUri = getArguments().getParcelable(ARG_URI);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_page, container, false);

        mWebView = (WebView) v.findViewById(R.id.fragment_photo_page_web_view);

        return v;
    }

}