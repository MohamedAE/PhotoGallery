package com.bignerdranch.android.photogallery;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends VisibleFragment {

    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    //Handle background downloading
	private ThumbnailDownloader<PhotoHolder> mThumbNailDownloader;

    //Search box
    private MenuItem mSearchItem;
    //Reference to loading dialog
    private ProgressDialog mProgressDialog;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
		//Call to fetch items from source
        updateItems();

        //Automatically associate Handler with current (main) thread
        Handler responseHandler = new Handler();
        //Associate instance of ThumbnailDownloader with this thread's response handler
		mThumbNailDownloader = new ThumbnailDownloader<>(responseHandler);
        //Set listener for successful downloads
        mThumbNailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
                        //Get Drawable
                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        //Bind Drawable to photo holder
                        photoHolder.bindDrawable(drawable);
                    }
                }
        );

		//Start downloader thread
		mThumbNailDownloader.start();
		//Associate new thread with a Looper
		mThumbNailDownloader.getLooper();
		Log.i(TAG, "Background thread started");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        setupAdapter();

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbNailDownloader.clearQueue();
    }

    @Override
	public void onDestroy() {
		super.onDestroy();
		mThumbNailDownloader.quit();
		Log.i(TAG, "Background thread destroyed");
	}

	@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_photo_gallery, menu);

        //Reference to search box
        mSearchItem = menu.findItem(R.id.menu_item_search);
        //Reference to SearchView
        final SearchView searchView = (SearchView) mSearchItem.getActionView();

        //Set listener for SearchView text box
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG, "QueryTextSubmit: " + s);
                //Store query in shared preferences
                QueryPreferences.setStoredQuery(getActivity(), s);

                //Hide keyboard after submitting search query
                searchView.clearFocus();

                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(TAG, "QueryTextChange: " + s);
                return true;
            }
        });

        //Label toggle according to alarm state
        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }

        //Pre-populate search box with last search query
		searchView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String query = QueryPreferences.getStoredQuery(getActivity());
				searchView.setQuery(query, false);
			}
		});
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                //Passing null to setStoredQuery(...) clears search history
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                //Determine whether an alarm is already set; act accordingly
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                //Declare change in options menu, and update it
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*Update RecyclerView with new items
    * Ultimately creates a background thread to get recents/run search*/
    private void updateItems() {
        String query = QueryPreferences.getStoredQuery(getActivity());
		new FetchItemsTask(query).execute();
    }

    /*Configure GalleryItems arraylist with appropriate adapter*/
    private void setupAdapter() {
        /*AsyncTask is triggering callbacks from a background thread
        * Cannot assume that the fragment is attached to an activity (PhotoGalleryActivity)
        * If not, operations that rely on parent will fail
        *
        * Ultimately updates RecyclerView*/
        if (isAdded()) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    /*ViewHolder*/
    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ImageView mItemImageView;
        private GalleryItem mGalleryItem;

        public PhotoHolder(View itemView) {
            super(itemView);
            mItemImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);
            itemView.setOnClickListener(this);
        }

        //Action to take when holder is associated with an image
        public void bindDrawable(Drawable drawable) {
			mItemImageView.setImageDrawable(drawable);
		}

		//Action taken when holder is associated with a model object
		public void bindGalleryItem(GalleryItem galleryItem) {
            mGalleryItem = galleryItem;
        }

        //onClick listener for each item in the RecyclerView; launch WebView
        @Override
        public void onClick(View v) {
            Intent i = PhotoPageActivity.newIntent(getActivity(), mGalleryItem.getPhotoPageUri());
            startActivity(i);
        }

    }

    /*Adapter*/
    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(getActivity());
			View view = inflater.inflate(R.layout.gallery_item, viewGroup, false);
			return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            photoHolder.bindGalleryItem(galleryItem);
			Bitmap bitmap = mThumbNailDownloader.getCachedImage(galleryItem.getUrl());

            if (bitmap == null) {
                Drawable drawable = getResources().getDrawable(R.drawable.placeholder);
                photoHolder.bindDrawable(drawable);
                mThumbNailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());
            } else {
                Log.i(TAG, "Loaded image from cache");
                photoHolder.bindDrawable(new BitmapDrawable(getResources(), bitmap));
            }

            preloadAdjacentImages(position);
        }

        private void preloadAdjacentImages(int position) {
            //Number of images before and after position to cache
            final int imageBufferSize = 10;

            //Determine first/last indexes to load
            int startIndex = Math.max(position - imageBufferSize, 0);
            int endIndex = Math.max(position + imageBufferSize, 0);

            //Populate relevant gallery items, populating
            for (int i = startIndex; i < endIndex; i++) {
                //Current image is already loaded; do not load again
                if(i == position) continue;

                String url = mGalleryItems.get(i).getUrl();
                mThumbNailDownloader.preloadImage(url);
            }
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }

    }

    /*Creates background thread to run doInBackground(...) method
    * <Params,Progress,Result>
    * Params - Type of input parameters passed to execute()
    * Progress - Type for sending progress updates
    * Result - Type of result of background task*/
    private class FetchItemsTask extends AsyncTask<Void,Void,List<GalleryItem>> {

		//Stored incoming query
		private String mQuery;

		public FetchItemsTask(String query) {
			mQuery = query;
		}

        /*Pass ...(Void... params)
        * AsyncTask is a generic type; doInBackground() has no sense of passed data*/
        protected List<GalleryItem> doInBackground(Void... params) {
            if (mQuery == null) {
                return new FlickrFetchr().fetchRecentPhotos();
            } else {
                return new FlickrFetchr().searchPhotos(mQuery);
            }
        }

        //Display loading bar
        @Override
        public void onPreExecute() {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setCancelable(false);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.show();
        }

        /*onPostExecute(...) - Runs after doInBackground(...) is completed
        * FlickrFetcher from doInBackground(...) is passed here
        * mItems is updated with new collection
        * setupAdapter() to update RecyclerView
        * hide loading bar*/
        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems = items;
            setupAdapter();

            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
        }

    }

}