package com.bignerdranch.android.photogallery;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

/*Fragment defining the WebView generated when an image is clicked/expanded*/
public class PhotoPageFragment extends VisibleFragment {

    private static final String ARG_URI = "photo_page_url";

    private Uri mUri;
    private WebView mWebView;
    private ProgressBar mProgressBar;

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

        mProgressBar = (ProgressBar) v.findViewById(R.id.fragment_photo_page_progress_bar);
        //WebChromeClient reports between 1-100
        mProgressBar.setMax(100);

        mWebView = (WebView) v.findViewById(R.id.fragment_photo_page_web_view);
        //Enable Javascript
        mWebView.getSettings().setJavaScriptEnabled(true);

        mWebView.setWebChromeClient(new WebChromeClient() {
            //Definitions of behaviour of WebView's progress bar
            public void onProgressChanged(WebView webView, int newProgress) {
                if (newProgress == 100) {
                    mProgressBar.setVisibility(View.GONE);
                } else {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mProgressBar.setProgress(newProgress);
                }
            }

            public void onReceivedTitle(WebView webView, String title) {
                AppCompatActivity activity = (AppCompatActivity) getActivity();
                activity.getSupportActionBar().setSubtitle(title);
            }
        });

        /*Configure the WebView; WebViewClient is an interface for responding to
        * events that should change elements of the Chrome browser*/
        mWebView.setWebViewClient(new WebViewClient() {
            //Method determines what happens when a URL is loaded in the WebView
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //False - handle URL auto; open in Chrome (external)
                //True; handle as specified in method
                return false;
            }
        });

        //Forward URL to the WebView
        mWebView.loadUrl(mUri.toString());

        return v;
    }

}