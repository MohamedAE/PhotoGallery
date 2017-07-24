package com.bignerdranch.android.photogallery;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
	private ThumbnailDownloader<PhotoHolder> mThumbNailDownloader;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
		//Call to fetch items from source
        new FetchItemsTask().execute();

		mThumbNailDownloader = new ThumbnailDownloader<>();
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
	public void onDestroy() {
		super.onDestroy();
		mThumbNailDownloader.quit();
		Log.i(TAG, "Background thread destroyed");
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
    private class PhotoHolder extends RecyclerView.ViewHolder {

        private ImageView mItemImageView;

        public PhotoHolder(View itemView) {
            super(itemView);
            mItemImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);
        }

        public void bindDrawable(Drawable drawable) {
			mItemImageView.setImageDrawable(drawable);
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
			Drawable placeholder = getResources().getDrawable(R.drawable.placeholder);
			photoHolder.bindDrawable(placeholder);
			//Pass this PhotoHolder to the download thread to be associated with an image
			mThumbNailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());
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

        /*Pass ...(Void... params)
        * AsyncTask is a generic type; doInBackground() has no sense of passed data*/
        protected List<GalleryItem> doInBackground(Void... params) {
            return new FlickrFetchr().fetchItems();
        }

        /*onPostExecute(...) - Runs after doInBackground(...) is completed
        * FlickrFetcher from doInBackground(...) is passed here
        * mItems is updated with new collection
        * setupAdapter() to update RecyclerView*/
        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems = items;
            setupAdapter();
        }

    }

}