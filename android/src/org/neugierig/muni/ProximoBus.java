/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

package org.neugierig.muni;

import java.net.*;
import java.io.*;
import org.json.*;

class ProximoBus {
  private static final String API_URL = "http://proximobus.appspot.com/agencies/sf-muni/";

  public static class Displayable {
    public String displayName;
        
    public String toString() {
      return displayName;
    }
  }

  public static class Route extends Displayable {
    static final String RUNS_PATH_FORMAT = "routes/%s/runs.json";

    public String id;

    public String getRunsPath() {
      return String.format(RUNS_PATH_FORMAT, URLEncoder.encode(this.id));
    }

    public static Route fromJsonObject(JSONObject obj) throws JSONException {
      Route route = new Route();
      route.displayName = obj.getString("displayName");
      route.id = obj.getString("id");
      return route;
    }
  }

  public static class Run extends Displayable {
    static final String STOPS_PATH_FORMAT = "routes/%s/runs/%s/stops.json";
    public String id;
    public String routeId;

    public String getStopsPath() {
      return String.format(STOPS_PATH_FORMAT, this.id, URLEncoder.encode(routeId));
    }
  }

  public static class Stop extends Displayable {
    static final String PREDICTIONS_PATH_FORMAT = "stops/%s/predictions.json";
    static final String PREDICTIONS_BY_ROUTE_PATH_FORMAT = "stops/%s/predictions/by-route/%s.json";

    public String id;

    public String getPredictionsPath() {
      return String.format(PREDICTIONS_PATH_FORMAT, this.id);
    }

    public String getPredictionsByRoutePath(String routeId) {
      return String.format(PREDICTIONS_BY_ROUTE_PATH_FORMAT, this.id, URLEncoder.encode(routeId));
    }
  }

  static Route[] parseRoutes(String data) throws JSONException {
    JSONArray array = new JSONArray(data);
    Route[] routes = new Route[array.length()];
    for (int i = 0; i < array.length(); ++i) {
      JSONObject entry = array.getJSONObject(i);
      routes[i] = Route.fromJsonObject(entry);
    }
    return routes;
  }

  static String queryNetwork(String path)
    throws MalformedURLException, IOException
  {
    return fetchURL(new URL(API_URL + path));
  }

  // It's pretty unbelievable there's no simpler way to do this.
  static String fetchURL(URL url) throws IOException {
    InputStream input = url.openStream();
    StringBuffer buffer = new StringBuffer(8 << 10);

    int byte_read;
    while ((byte_read = input.read()) != -1) {
      // This is incorrect for non-ASCII, but we don't have any of that.
      buffer.appendCodePoint(byte_read);
    }

    return buffer.toString();
  }
    
}
