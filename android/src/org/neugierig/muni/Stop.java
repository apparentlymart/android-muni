package org.neugierig.muni;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.util.Log;

public class Stop extends Activity implements AsyncBackend.Delegate,
                                              View.OnClickListener {
  // Intent extra data on the stop name.
  public static final String KEY_NAME = "name";
  public static final int STOP_SELECT_REQUEST = 1;

  private MuniAPI.Stop mStop = null;
  private String mRoute;
  private String mDirection;
  private AsyncBackend mBackend;
  private StarDBAdapter mStarDB;
  private CheckBox mStarView;

  private class StopQuery implements AsyncBackend.Query {
    final String mQuery;
    final boolean mReload;
    StopQuery(String query, boolean reload) {
      mQuery = query;
      mReload = reload;
    }
    public Object runQuery(Backend backend) throws Exception {
      return backend.fetchStop(mQuery, mReload);
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.stop);

    mStarDB = new StarDBAdapter(this);

    setViewParams(getIntent());
  }

  private void setViewParams(Intent intent) {
    Bundle extras = intent.getExtras();
    if (extras != null) {
      mRoute = extras.getString(Route.KEY_ROUTE);
      mDirection = extras.getString(Route.KEY_DIRECTION);
      mStop = new MuniAPI.Stop(extras.getString(KEY_NAME),
                               extras.getString(Backend.KEY_QUERY));
      setIntent(intent);

      TextView title = (TextView) findViewById(R.id.title);
      title.setText(mStop.name);
      TextView subtitle = (TextView) findViewById(R.id.subtitle);
      subtitle.setText(mRoute + ": " + mDirection);

      mStarView = (CheckBox) findViewById(R.id.star);
      mStarView.setOnClickListener(this);
      mStarView.setChecked(mStarDB.getStarred(mStop.url));

      // Empty out the list while we load the new data
      // so we don't confuse the user.
      ListView list = (ListView) findViewById(R.id.list);
      ListAdapter adapter = new ArrayAdapter<String>(
          this,
          android.R.layout.simple_list_item_1,
          new String[0]
      );
      list.setAdapter(adapter);

      mBackend = new AsyncBackend(this, this);
      mBackend.start(new StopQuery(mStop.url, false));

    }
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    return mBackend.onCreateDialog(id);
  }

  @Override
  public void onAsyncResult(Object data) {
    mStop.times = (MuniAPI.Stop.Time[]) data;

    ListView list = (ListView) findViewById(R.id.list);
    ListAdapter adapter;
    if (mStop.times.length > 0) {
      adapter = new ArrayAdapter<MuniAPI.Stop.Time>(
          this,
          android.R.layout.simple_list_item_1,
          mStop.times);
    } else {
      adapter = new ArrayAdapter<String>(
          this,
          android.R.layout.simple_list_item_1,
          new String[] {"(no arrivals predicted)"});
    }
    list.setAdapter(adapter);
  }

  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.star:
        if (mStarView.isChecked())
          mStarDB.setStarred(mStop, mRoute, mDirection);
        else
          mStarDB.unStar(mStop);
        break;
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.stop_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.refresh:
      mBackend.start(new StopQuery(mStop.url, true));
      return true;
    case R.id.change_stop:
      Intent intent = new Intent(this, Stops.class);
      intent.putExtra(Route.KEY_ROUTE, mRoute);
      intent.putExtra(Route.KEY_DIRECTION, mDirection);
      // FIXME: Don't hard-code this. Instead, do it based on the current route and direction.
      // But since the current route and direction are baked into the query we can't actually
      // get at them individually here.
      intent.putExtra(Backend.KEY_QUERY, "a=sf-muni&r=F&d=F__OBCTRO");
      startActivityForResult(intent, STOP_SELECT_REQUEST);
      return true;
    case R.id.change_route:
      Intent intent2 = new Intent(this, Stops.class);
      // In this case we pass no route information, causing
      // the stop picker to delegate to the route picker
      // to select a route.
      startActivityForResult(intent2, STOP_SELECT_REQUEST);
      return true;
    }
    return false;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
    case STOP_SELECT_REQUEST:
      if (resultCode == RESULT_OK) {
        setViewParams(data);
      }
    }
  }

}
