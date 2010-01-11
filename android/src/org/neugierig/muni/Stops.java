package org.neugierig.muni;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.widget.*;
import android.view.*;

public class Stops extends ListActivity implements AsyncBackend.Delegate {
  public static final int DIRECTION_SELECT_REQUEST = 1;

  private MuniAPI.Stop[] mStops;

  private String mRoute = null;
  private String mDirection = null;
  private String mQuery = null;
  private AsyncBackend mBackend;

  private class StopsQuery implements AsyncBackend.Query {
    final String mQuery;
    StopsQuery(String query) {
      mQuery = query;
    }
    public Object runQuery(Backend backend) throws Exception {
      return backend.fetchStops(mQuery);
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    super.onCreate(savedInstanceState);

    setViewParams(getIntent());

    // If the caller didn't tell us what direction to work with,
    // ask the user.
    if (mDirection == null) {
      Intent intent = new Intent(this, Route.class);
      startActivityForResult(intent, DIRECTION_SELECT_REQUEST);
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
      mQuery = extras.getString(Backend.KEY_QUERY);
      mRoute = extras.getString(Route.KEY_ROUTE);
      mDirection = extras.getString(Route.KEY_DIRECTION);

      mBackend = new AsyncBackend(this, this);
      mBackend.start(new StopsQuery(mQuery));
    }
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    return mBackend.onCreateDialog(id);
  }

  @Override
  public void onAsyncResult(Object data) {
    mStops = (MuniAPI.Stop[]) data;
    ListAdapter adapter = new ArrayAdapter<MuniAPI.Stop>(
        this,
        android.R.layout.simple_list_item_1,
        mStops);
    setListAdapter(adapter);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    MuniAPI.Stop stop = mStops[position];
    Intent intent = new Intent(this, Stop.class);
    intent.putExtra(Route.KEY_ROUTE, mRoute);
    intent.putExtra(Route.KEY_DIRECTION, mDirection);
    intent.putExtra(Stop.KEY_NAME, stop.name);
    intent.putExtra(Backend.KEY_QUERY, stop.url);
    setResult(RESULT_OK, intent);
    finish();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
    case DIRECTION_SELECT_REQUEST:
      if (resultCode == RESULT_OK) {
        setViewParams(data);
      }
      else {
        // If the user cancelled chosing a direction, then that implicitly
        // cancels selecting a stop too.
        setResult(RESULT_CANCELED);
        finish();
      }
    }
  }

}
