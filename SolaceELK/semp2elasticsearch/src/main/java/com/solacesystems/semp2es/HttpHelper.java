package com.solacesystems.semp2es;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by koverton on 11/28/2014.
 */
class HttpHelper {

    static Result<String> doHead(final URL getURL) {
        int responseCode = -1;
        String responseMsg = null;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) getURL.openConnection();
            connection.setRequestMethod("HEAD");
            responseCode = connection.getResponseCode();
            responseMsg  = connection.getResponseMessage();

        }
        catch (IOException exception) {
            exception.printStackTrace();
        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return new Result(responseCode, responseMsg, null);
    }

    static Result<String> doPost(final URL postURL, final String data) {
        String payload = null;
        int responseCode = -1;
        String responseMsg = null;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) postURL.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);

            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            out.write(data);
            out.close();

            payload = getResponseString(connection.getInputStream());
            responseCode = connection.getResponseCode();
            if (!httpSuccess(responseCode)) {
                responseMsg = connection.getResponseMessage();
                System.out.println("Error: " + responseCode + " " + responseMsg);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return new Result(responseCode, responseMsg, payload);
    }

    static Result<String> doPut(final URL putUrl) {
        String payload = null;
        int responseCode = -1;
        String responseMsg = null;
        HttpURLConnection connection = null;
        try {
            // Send the request
            connection = (HttpURLConnection) putUrl.openConnection();
            connection.setRequestMethod("PUT");
            connection.setDoInput(true);
            connection.setDoOutput(false);
            // Gather the response
            responseCode = connection.getResponseCode();
            if (httpSuccess(responseCode)) {
                payload = getResponseString(connection.getInputStream());
            }
            else {
                responseMsg = connection.getResponseMessage();
                System.out.println("Error: " + responseCode + " " + responseMsg);
            }
        }
        catch (IOException exception) {
            exception.printStackTrace();
        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return new Result(responseCode, responseMsg, payload);
    }

    static boolean httpSuccess(final int status) {
        return status >= 200 && status < 300;
    }

    private static String getResponseString(final InputStream input) {
        byte[] buffer = new byte[1000];
        boolean end = false;
        String dataString = "";
        try
        {
            DataInputStream in = new DataInputStream(input);
            while(!end)
            {
                int bytesRead = in.read(buffer);
                if (bytesRead == -1) end = true;
                else dataString += new String(buffer, 0, bytesRead);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return dataString;
    }
}
