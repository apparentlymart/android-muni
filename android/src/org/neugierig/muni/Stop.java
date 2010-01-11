package org.neugierig.muni;

import android.app.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.util.Log;

public class Stop extends Activity implements AsyncBackend.Delegate,
                                              View.OnClickListener {
  // Intent extra data on the stop name.
  public static final String KEY_NAME = "name";

  private String mRouteId;
  private String mRouteName;
  private String mRunId;
  private String mRunName;
  private String mStopId;
  private String mStopName;
  private ProximoBus.Prediction[] mPredictions;

  private AsyncBackend mBackend;
  private StarDBAdapter mStarDB;
  private CheckBox mStarView;

  private class PredictionsForStopQuery implements AsyncBackend.Query {
    final String mStopId;
    final String mRouteId;
    final boolean mForceRefresh;
    PredictionsForStopQuery(String routeId, String stopId, boolean forceRefresh) {
      mRouteId = routeId;
      mStopId = stopId;
      mForceRefresh = forceRefresh;
    }
    public Object runQuery(Backend backend) throws Exception {
      return backend.fetchPredictionsForRouteAtStop(mRouteId, mStopId, mForceRefresh);
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.stop);

    mStarDB = new StarDBAdapter(this);

    Bundle extras = getIntent().getExtras();
    mRouteId = extras.getString(ViewState.ROUTE_ID_KEY);
    mRouteName = extras.getString(ViewState.ROUTE_NAME_KEY);
    mRunId = extras.getString(ViewState.RUN_ID_KEY);
    mRunName = extras.getString(ViewState.RUN_NAME_KEY);
    mStopId = extras.getString(ViewState.STOP_ID_KEY);
    mStopName = extras.getString(ViewState.STOP_NAME_KEY);

    TextView title = (TextView) findViewById(R.id.title);
    title.setText(mStopName);
    TextView subtitle = (TextView) findViewById(R.id.subtitle);
    subtitle.setText(mRouteName + ": " + mRunName);

    mStarView = (CheckBox) findViewById(R.id.star);
    mStarView.setOnClickListener(this);
    // FIXME: Make Favorites work with the ProximoBus backend
    //mStarView.setChecked(mStarDB.getStarred(mStop.url));

    mBackend = new AsyncBackend(this, this);
    mBackend.start(new PredictionsForStopQuery(mRouteId, mStopId, false));
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    return mBackend.onCreateDialog(id);
  }

  @Override
  public void onAsyncResult(Object data) {
    mPredictions = (ProximoBus.Prediction[]) data;

    ListView list = (ListView) findViewById(R.id.list);
    ListAdapter adapter;
    if (mPredictions.length > 0) {
      adapter = new ArrayAdapter<ProximoBus.Prediction>(
          this,
          android.R.layout.simple_list_item_1,
          mPredictions);
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
    /*switch (view.getId()) {
      case R.id.star:
        if (mStarView.isChecked())
          mStarDB.setStarred(mStop, mRoute, mDirection);
        else
          mStarDB.unStar(mStop);
        break;
        }*/
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
      mBackend.start(new PredictionsForStopQuery(mRouteId, mStopId, true));
      return true;
    }
    return false;
  }
}
