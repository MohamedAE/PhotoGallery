package com.bignerdranch.android.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/*Class that will handle networking with Flickr*/
public class FlickrFetchr {

    private static final String TAG = "FlickrFetchr";
    private static final String API_KEY = "3868d93169de7166e9772d0e46aa22fb";

    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    //A "template" URI
    private static final Uri ENDPOINT = Uri
            .parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();

    /*Fetch raw data from a String; returns array of bytes*/
    public byte[] getUrlBytes(String urlSpec) throws IOException {
        //Create URL object
        URL url = new URL(urlSpec);
        //Create connection object to URL
        //Cast to HTTP to use given interfaces for request/response/streaming utility
        //Not connected to endpoint until getInputStream() is called
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() +
                        ": with " +
                        urlSpec);
            }

            int bytesRead = 0;
            //A 1 megabyte buffer; read into buffer
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    /*Convert results from getUrlBytes(...) to String*/
    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    /*Fetch recent photos from Flickr*/
    public List<GalleryItem> fetchRecentPhotos() {
        String url = buildUrl(FETCH_RECENTS_METHOD, null);
        return downloadGalleryItems(url);
    }

    /*Fetch photos by given search parameter from Flickr*/
    public List<GalleryItem> searchPhotos(String query) {
        String url = buildUrl(SEARCH_METHOD, query);
        return downloadGalleryItems(url);
    }

    /*Construct query string
    * Call parseItems(...) to fill with return from query
    * Return assembled collection of GalleryItems*/
    private List<GalleryItem> downloadGalleryItems(String url) {
        //Arraylist of gallery items to be filled
        List<GalleryItem> items = new ArrayList<>();

        try {
            /*Use Uri.Builder to build complete URL for Flickr API request
            String url = Uri.parse("https://api.flickr.com/services/rest/")
                    //Construct new builder; copy attributes from this uri
                    .buildUpon()
                    //Invoke GetRecent method
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    //Include API_KEY
                    .appendQueryParameter("api_key", API_KEY)
                    //Format of returned URL
                    .appendQueryParameter("format", "json")
                    //Exclude method name and parentheses from response
                    .appendQueryParameter("nojsoncallback", "1")
                    //Include URL for small version of photo, if available
                    .appendQueryParameter("extras", "url_s")
                    .build().toString();
            */

            /*Final String query*/
            String jsonString = getUrlString(url);

            Log.i(TAG, "Received JSON:" + jsonString);

            /*Produce object with JSON hierarchy mapped to original JSON text
            * A set of name/value pairs
            * -JSONObject
            * --photos
            * ---array
            * ----photo metadata*/
            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(items, jsonBody);
        } catch (JSONException je) {
            Log.i(TAG, "Failed to parse JSON", je);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items", ioe);
        }

        //Pass (by reference) the ArrayList of GalleryItems (model objects)
        return items;
    }

    /*Appends necessary parameters to the template URI*/
    private String buildUrl(String method, String query) {
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter("method", method);

        //If query is a search, append text parameter
        if (method.equals(SEARCH_METHOD)) {
            uriBuilder.appendQueryParameter("text", query);
        }

        return uriBuilder.build().toString();
    }

    /*Parse data from JSONObject into collection*/
    private void parseItems(List<GalleryItem> items, JSONObject jsonBody) throws IOException, JSONException {
        //Get from the JSON hierarchy value mapped to "photos"
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        //Get from above JSONObject a JSONArray mapped to "photo"
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");

        for (int i = 0; i < photoJsonArray.length(); i++) {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);

            GalleryItem item = new GalleryItem();
            item.setId(photoJsonObject.getString("id"));
            item.setCaption(photoJsonObject.getString("title"));

            if (!photoJsonObject.has("url_s")) {
                continue;
            }

            item.setUrl(photoJsonObject.getString("url_s"));
            items.add(item);
        }
    }

}