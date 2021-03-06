package org.adaway.model.git;

import androidx.annotation.Nullable;

import org.adaway.util.Constants;
import org.adaway.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * This class is an utility class to get information from GitHub gist hosting.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
class GistHostsSource extends GitHostsSource {
    /**
     * The gist identifier.
     */
    private final String gistIdentifier;

    /**
     * Constructor.
     *
     * @param url The hosts file URL hosted on GitHub gist.
     * @throws MalformedURLException If the URl is not a gist URL.
     */
    GistHostsSource(String url) throws MalformedURLException {
        // Check URL path
        URL parsedUrl = new URL(url);
        String path = parsedUrl.getPath();
        String[] pathParts = path.split("/");
        if (pathParts.length < 2) {
            throw new MalformedURLException("The GitHub gist URL " + url + " is not valid.");
        }
        // Extract gist identifier from path
        gistIdentifier = pathParts[2];
    }

    @Override
    @Nullable
    public Date getLastUpdate() {
        // Create commit API request URL
        String commitApiUrl = "https://api.github.com/gists/" + gistIdentifier;
        // Create client and request
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(commitApiUrl).build();
        try (Response execute = client.newCall(request).execute()) {
            ResponseBody body = execute.body();
            if (body == null) {
                throw new IOException("Empty body content for URL:" + commitApiUrl);
            }
            return parseJsonBody(body.string());
        } catch (IOException | JSONException exception) {
            Log.e(Constants.TAG, "Unable to get commits from API.", exception);
            // Return failed
            return null;
        }
    }

    @Nullable
    private Date parseJsonBody(String body) throws JSONException {
        JSONObject gistObject = new JSONObject(body);
        String dateString = gistObject.getString("updated_at");
        Date date = null;
        try {
            date = this.dateFormat.parse(dateString);
        } catch (ParseException exception) {
            Log.w(Constants.TAG, "Failed to parse commit date: " + dateString + ".", exception);
        }
        return date;
    }
}
