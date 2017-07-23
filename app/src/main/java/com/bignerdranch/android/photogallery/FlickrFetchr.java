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

    /*Convert incoming URL to String*/
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

    /*Convert incoming URL to String*/
    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    public List<GalleryItem> fetchItems() {
        //Arraylist of gallery items to be filled
        List<GalleryItem> items = new ArrayList<>();

        try {
            //Use Uri.Builder to build complete URL for Flickr API request
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
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON:" + jsonString);
            /*Produce object with JSON hierarchy mapped to original JSON text
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
        return items;
    }

    private void parseItems(List<GalleryItem> items, JSONObject jsonBody) throws IOException, JSONException {
        //JSONObject - Set of name/value pairs
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
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