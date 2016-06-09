package jta00.ucsb.edu.microsoftcognitivefrontend;

import android.os.AsyncTask;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by john on 3/14/16.
 */
public class ApiAccessor extends AsyncTask<StringUrlPair, Integer, String> {
    @Override
    protected String doInBackground(StringUrlPair... urls) {
        if (urls.length != 1)
            throw new IllegalStateException("ApiAccessor invoked with more than one URL.");
        URL postUrl = urls[0].url;
        String requestContent = urls[0].str;
        byte[] requestData = urls[0].data;
        String requestPassword = urls[0].password;
        String requestMethod = urls[0].method;

        HttpURLConnection postConnection = null;
        BufferedReader postIn = null;
        BufferedOutputStream postOut = null;
        PrintWriter outWriter = null;

        try {
            postConnection = (HttpURLConnection) postUrl.openConnection();

            postConnection.setRequestMethod(requestMethod);

            if(requestContent != null) {
                postOut = new BufferedOutputStream(postConnection.getOutputStream());
                outWriter = new PrintWriter(postOut, true);
                outWriter.println(requestContent);
                outWriter.close();
                outWriter = null;
            }
            else if(requestData != null){
                postOut = new BufferedOutputStream(postConnection.getOutputStream());
                postOut.write(requestData);
                postOut.close();
                postOut = null;
            }

            try {
                postIn = new BufferedReader(new InputStreamReader(postConnection.getInputStream()));
            } catch (IOException i) {
                postIn = new BufferedReader(new InputStreamReader(postConnection.getErrorStream()));
            }

            BufferedReader potentialError = new BufferedReader(postIn);
            StringBuilder builder = new StringBuilder();
            String aux;
            while ((aux = potentialError.readLine()) != null) {
                builder.append(aux);
            }
            String text = builder.toString();
            return text;
        } catch (IOException e) {
            return "ERROR: Could not access the internet.";

        } finally {
            try {
                if (postIn != null) postIn.close();
                if (postOut != null) postOut.close();
                if (postConnection != null) postConnection.disconnect();
                if (outWriter != null) outWriter.close();
            } catch (IOException e) {
            }
        }
    }
}
