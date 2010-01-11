package org.neugierig.muni;

import android.app.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.util.Log;
import java.util.HashMap;

public class Stop extends Activity implements AsyncBackend.Delegate,
                                              View.OnClickListener {

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

  private HashMap<String, String> mRouteNames = new HashMap<String, String>();
  private HashMap<String, String> mRunNames = new HashMap<String, String>();

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
      return backend.fetchPredictionsForStop(mStopId, mForceRefresh);
    }
  }

  private class RouteQuery implements AsyncBackend.Query {
    final String mRouteId;
    RouteQuery(String routeId) {
      mRouteId = routeId;
    }
    public Object runQuery(Backend backend) throws Exception {
      return backend.fetchRoute(mRouteId);
    }
  }

  private class RunQuery implements AsyncBackend.Query {
    final String mRouteId;
    final String mRunId;
    RunQuery(String routeId, String runId) {
      mRouteId = routeId;
      mRunId = runId;
    }
    public Object runQuery(Backend backend) throws Exception {
      return backend.fetchRun(mRouteId, mRunId);
    }
  }

  private class PredictionsListAdapter extends ArrayAdapter<ProximoBus.Prediction> {

    public PredictionsListAdapter(Stop context, ProximoBus.Prediction[] objects) {
      super(context, android.R.layout.simple_list_item_1, objects);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      View ret;

      if (convertView != null) {
        ret = convertView;
      }
      else {
        ret = View.inflate(this.getContext(), R.layout.prediction_item, null);
      }

      ProximoBus.Prediction prediction = getItem(position);

      String routeDisplayName;
      String runDisplayName;

      if (mRouteNames.containsKey(prediction.routeId)) {
        routeDisplayName = mRouteNames.get(prediction.routeId);
      }
      else {
        // Use the routeId as a placeholder for now,
        // and start a query to find the real name.
        routeDisplayName = prediction.routeId;
        mBackend.start(new RouteQuery(prediction.routeId));
      }

      if (mRunNames.containsKey(prediction.runId)) {
        runDisplayName = mRunNames.get(prediction.runId);
      }
      else {
        // Leave this blank for now, and start a query
        // to find the run name.
        runDisplayName = "";
        mBackend.start(new RunQuery(prediction.routeId, prediction.runId));
      }

      TextView routeNameView = (TextView) ret.findViewById(R.id.route_name);
      routeNameView.setText(routeDisplayName);

      TextView runNameView = (TextView) ret.findViewById(R.id.run_name);
      runNameView.setText(runDisplayName);
      
      TextView predictedTimeView = (TextView) ret.findViewById(R.id.predicted_time);
      predictedTimeView.setText(prediction.predictedTimeForDisplay());
      

      return ret;
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

    mRouteNames.put(mRouteId, mRouteName);
    mRunNames.put(mRunId, mRunName);

    TextView title = (TextView) findViewById(R.id.title);
    title.setText(mStopName);

    mStarView = (CheckBox) findViewById(R.id.star);
    mStarView.setOnClickListener(this);
    mStarView.setChecked(mStarDB.isStopAFavorite(mRouteId, mStopId));

    mBackend = new AsyncBackend(this, this);
    mBackend.start(new PredictionsForStopQuery(mRouteId, mStopId, false));
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    return mBackend.onCreateDialog(id);
  }

  @Override
  public void onAsyncResult(Object data, AsyncBackend.Query query) {
    if (query instanceof PredictionsForStopQuery) {
      mPredictions = (ProximoBus.Prediction[]) data;

      ListView list = (ListView) findViewById(R.id.list);
      ListAdapter adapter;
      if (mPredictions.length > 0) {
        adapter = new PredictionsListAdapter(
          this,
          mPredictions);
      } else {
        adapter = new ArrayAdapter<String>(
          this,
          android.R.layout.simple_list_item_1,
          new String[] {"(no arrivals predicted)"});
      }
      list.setAdapter(adapter);
    }
    else if (query instanceof RouteQuery) {
      ProximoBus.Route route = (ProximoBus.Route) data;
      mRouteNames.put(route.id, route.displayName);

      ListView list = (ListView) findViewById(R.id.list);
      ListAdapter adapter = list.getAdapter();
      if (adapter instanceof PredictionsListAdapter) {
        PredictionsListAdapter pladapter = (PredictionsListAdapter) adapter;
        pladapter.notifyDataSetChanged();
      }
    }
    else if (query instanceof RunQuery) {
      ProximoBus.Run run = (ProximoBus.Run) data;
      mRunNames.put(run.id, run.displayName);

      ListView list = (ListView) findViewById(R.id.list);
      ListAdapter adapter = list.getAdapter();
      if (adapter instanceof PredictionsListAdapter) {
        PredictionsListAdapter pladapter = (PredictionsListAdapter) adapter;
        pladapter.notifyDataSetChanged();
      }
    }
  }

  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.star:
        if (mStarView.isChecked())
          mStarDB.addStopAsFavorite(mRouteId, mRouteName, mRunId, mRunName, mStopId, mStopName);
        else
          mStarDB.removeStopAsFavorite(mRouteId, mStopId);
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
      mBackend.start(new PredictionsForStopQuery(mRouteId, mStopId, true));
      return true;
    }
    return false;
  }
}
