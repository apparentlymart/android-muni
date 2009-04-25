package org.neugierig.muni;

import android.content.*;
import android.util.Log;

import java.net.*;
import java.io.*;
import org.json.*;

public class Backend {
  private static final String TAG = "muni";
  public static final String KEY_QUERY = "query";

  public class QueryEntry {
    public String name;
    public String url;
    public QueryEntry(String name, String url) {
      this.name = name;
      this.url = url;
    }
    public QueryEntry() {}
    public String toString() { return this.name; }
  }

  public class Stop extends QueryEntry {
    Stop(String name, String url) { super(name, url); }
    class Time {
      public Time(int minutes) {
        this.minutes = minutes;
      }
      public int minutes;
      public String toString() {
        return "" + minutes + " minutes";
      }
    }
    public Time[] times;
  }

  public class Route extends QueryEntry {
    Route(String name, String url) { super(name, url); }
  }

  public class Direction extends QueryEntry {
    Direction(String name, String url) { super(name, url); }
  }

  Backend(Context context) {
    mDatabase = new Database(context);
  }

  Route[] fetchRoutes() {
    try {
      JSONArray array = new JSONArray(queryAPI(""));
      Route[] routes = new Route[array.length()];
      for (int i = 0; i < array.length(); ++i) {
        JSONObject entry = array.getJSONObject(i);
        routes[i] = new Route(entry.getString("name"),
                              entry.getString("url"));
      }
      return routes;
    } catch (JSONException e) {
      Log.e(TAG, "json", e);
      return null;
    }
  }

  Direction[] fetchRoute(String query) {
    try {
      JSONArray json = new JSONArray(queryAPI(query));
      Direction[] directions = new Direction[2];
      for (int i = 0; i < 2; ++i) {
        JSONObject json_dir = json.getJSONObject(i);
        directions[i] = new Direction(json_dir.getString("name"),
                                      json_dir.getString("url"));
      }
      return directions;
    } catch (JSONException e) {
      Log.e(TAG, "json", e);
      return null;
    }
  }

  Stop[] fetchStops(String query) {
    try {
      JSONArray json = new JSONArray(queryAPI(query));
      Stop[] stops = new Stop[json.length()];
      for (int i = 0; i < json.length(); ++i) {
        JSONObject json_stop = json.getJSONObject(i);
        stops[i] = new Stop(json_stop.getString("name"),
                            json_stop.getString("url"));
      }
      return stops;
    } catch (JSONException e) {
      Log.e(TAG, "json", e);
      return null;
    }
  }

  Stop fetchStop(String query) {
    try {
      JSONObject json = new JSONObject(queryAPI(query));

      Stop stop = new Stop();
      stop.direction = json.getString("direction");
      stop.name = json.getString("name");
      JSONArray times = json.getJSONArray("times");
      stop.times = new Stop.Time[times.length()];
      for (int i = 0; i < times.length(); ++i)
        stop.times[i] = stop.new Time(times.getInt(i));

      return stop;
    } catch (JSONException e) {
      Log.e(TAG, "json", e);
      return null;
    }
  }

  String queryAPI(String query) {
    try {
      String data = mDatabase.get(query);
      if (data == null) {
        data = fetchURL(new URL("http://10.0.2.2:8080/api/" + query));
        mDatabase.put(query, data);
      }
      Log.i(TAG, data);
      return data;
    } catch (MalformedURLException e) {
      Log.e(TAG, "url", e);
      return null;
    } catch (IOException e) {
      Log.e(TAG, "io", e);
      return null;
    }
  }

  // It's pretty unbelievable there's no simpler way to do this.
  String fetchURL(URL url) throws IOException {
    InputStream input = url.openStream();
    StringBuffer buffer = new StringBuffer(8 << 10);

    int byte_read;
    while ((byte_read = input.read()) != -1) {
      // This is incorrect for non-ASCII, but we don't have any of that.
      buffer.appendCodePoint(byte_read);
    }

    return buffer.toString();
  }

  private Database mDatabase;
}
