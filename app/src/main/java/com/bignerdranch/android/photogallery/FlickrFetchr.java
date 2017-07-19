package com.bignerdranch.android.photogallery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/*Class that will handle networking with Flickr*/
public class FlickrFetchr {

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

}