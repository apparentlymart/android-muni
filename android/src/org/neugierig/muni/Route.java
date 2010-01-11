package org.neugierig.muni;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.widget.*;
import android.view.*;

public class Route extends ListActivity implements AsyncBackend.Delegate {
  public static final String KEY_ROUTE = "route";
  public static final String KEY_DIRECTION = "direction";

  public static final int ROUTE_SELECT_REQUEST = 1;

  private String mRoute = null;
  private String mQuery = null;
  private MuniAPI.Direction[] mDirections;
  private AsyncBackend mBackend;

  private class RouteQuery implements AsyncBackend.Query {
    final String mQuery;
    RouteQuery(String query) {
      mQuery = query;
    }
    public Object runQuery(Backend backend) throws Exception {
      return backend.fetchRoute(mQuery);
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    super.onCreate(savedInstanceState);

    setViewParams(getIntent());

    if (mRoute == null) {
      // Call on the route selector so the user can choose a route.
      Intent intent = new Intent(this, Routes.class);
      startActivityForResult(intent, ROUTE_SELECT_REQUEST);
    }

  }

  private void setViewParams(Intent intent) {
    ListAdapter adapter = new ArrayAdapter<String>(
        this,
        android.R.layout.simple_list_item_1,
        new String[] {});
    setListAdapter(adapter);

    Bundle extras = intent.getExtras();
    if (extras != null) {
      mQuery = intent.getExtras().getString(Backend.KEY_QUERY);
      mRoute = intent.getExtras().getString(KEY_ROUTE);

      mBackend = new AsyncBackend(this, this);
      mBackend.start(new RouteQuery(mQuery));
      setIntent(intent);
    }
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    return mBackend.onCreateDialog(id);
  }

  @Override
  public void onAsyncResult(Object data) {
    mDirections = (MuniAPI.Direction[]) data;
    ListAdapter adapter = new ArrayAdapter<MuniAPI.Direction>(
        this,
        android.R.layout.simple_list_item_1,
        mDirections);
    setListAdapter(adapter);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    MuniAPI.Direction direction = mDirections[position];
    Intent intent = new Intent(this, Stops.class);
    intent.putExtra(Route.KEY_ROUTE, mRoute);
    intent.putExtra(Route.KEY_DIRECTION, direction.name);
    intent.putExtra(Backend.KEY_QUERY, direction.url);
    setResult(RESULT_OK, intent);
    finish();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
    case ROUTE_SELECT_REQUEST:
      if (resultCode == RESULT_OK) {
        setViewParams(data);
      }
      else {
        // If the user cancelled chosing a route, then that implicitly
        // cancels selecting a direction too.
        setResult(RESULT_CANCELED);
        finish();
      }
    }
  }

}
