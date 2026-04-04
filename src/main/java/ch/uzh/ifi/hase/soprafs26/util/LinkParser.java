package ch.uzh.ifi.hase.soprafs26.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class LinkParser {

    // Sonarqube complains without this
    private LinkParser() {}

    // extract only code param
    public static String getLobbyCodeFromUrl(String url) {
        Map<String, String> params = parseQueryParams(url);
        return params.get("code");
    }

    // retreive the parameters from URL (helper function)
    public static Map<String, String> parseQueryParams(String url) {
        try {
            // retreive the query
            URI uri = new URI(url);
            String query = uri.getQuery();

            Map<String, String> params = new HashMap<>();

            // null when only /join, empty when invalid URL for some reason (safety)
            if (query == null || query.isEmpty()) {
                return params;
            }

            // split all the different params and save them
            String[] pairs = query.split("&");

            for (String pair : pairs) {
                String[] keyValue = pair.split("=");

                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }

            return params;

        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + url);
        }
    }
}
