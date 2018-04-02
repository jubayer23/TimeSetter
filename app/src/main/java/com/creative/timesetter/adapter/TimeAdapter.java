package com.creative.timesetter.adapter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.creative.timesetter.MainActivity;
import com.creative.timesetter.R;
import com.creative.timesetter.Utility.CommonMethods;
import com.creative.timesetter.alertbanner.AlertDialogForAnything;
import com.creative.timesetter.appdata.GlobalAppAccess;
import com.creative.timesetter.appdata.MydApplication;
import com.creative.timesetter.model.Time;
import com.creative.timesetter.model.TimeInfo;
import com.creative.timesetter.model.TimeLocation;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TimeAdapter extends RecyclerView.Adapter<TimeAdapter.MyViewHolder> {

    public static final String KEY_EVENT = "key_event";
    private List<TimeLocation> Displayedplaces;
    private List<TimeLocation> Originalplaces;
    private LayoutInflater inflater;
    @SuppressWarnings("unused")
    private Activity activity;
    private String call_from;

    private  PopupWindow popupwindow_obj;

    private int lastPosition = -1;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tv_time;
        Button btn_set_alarm, btn_delete;

        public MyViewHolder(View view) {
            super(view);
            tv_time = (TextView) view.findViewById(R.id.tv_time);
            btn_set_alarm = (Button) view.findViewById(R.id.btn_set_alarm);
            btn_delete = (Button) view.findViewById(R.id.btn_delete);
        }
    }


    public TimeAdapter(Activity activity, List<TimeLocation> attendees) {
        this.activity = activity;
        this.Displayedplaces = attendees;
        this.Originalplaces = attendees;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_time_list, parent, false);



        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        final Time event = Displayedplaces.get(MainActivity.selectedTimePosition).getTimes().get(position);
        holder.tv_time.setText(CommonMethods.changeFormat(event.getTime(),"MM/dd/yyyy HH:mm:ss"));
        if(MydApplication.deviceImieNumber.equals(event.getDeviceId())){
            holder.btn_delete.setVisibility(View.VISIBLE);
        }else{
            holder.btn_delete.setVisibility(View.GONE);
        }

        holder.btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sentRequestToDeleteTime(GlobalAppAccess.URL_DELETE_TIME,event.getId(),position);
            }
        });

    }


    public void sentRequestToDeleteTime(String url, final int id, final int position) {

        //url = url + "?" + "email=" + email + "&password=" + password;
        // TODO Auto-generated method stub
        showProgressDialog("Loading..", true, false);

        final StringRequest req = new StringRequest(Request.Method.POST, url,
                new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Log.d("DEBUG",response);


                        dismissProgressDialog();


                        try {

                            JSONObject jsonObject = new JSONObject(response);
                            boolean result = jsonObject.getBoolean("Result");

                            if (result) {
                               Displayedplaces.get(MainActivity.selectedTimePosition).getTimes().remove(position);
                                int num_of_time_set = Displayedplaces.get(MainActivity.selectedTimePosition).getTimes().size();
                                if(num_of_time_set <= 0){
                                    ((MainActivity)activity).removeMarkerAndSetAsUserClickedMarker();
                                }else{
                                    ((MainActivity)activity).updateBottomSheetUi(num_of_time_set);
                                    notifyDataSetChanged();
                                }


                            } else {
                                AlertDialogForAnything.showAlertDialogWhenComplte(activity, "Error", "Server problem while loading the timeLocations!", false);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissProgressDialog();

                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(getDummyDeleteResponse());
                    boolean result = jsonObject.getBoolean("Result");

                    if (result) {
                        Displayedplaces.get(MainActivity.selectedTimePosition).getTimes().remove(position);
                        int num_of_time_set = Displayedplaces.get(MainActivity.selectedTimePosition).getTimes().size();
                        if(num_of_time_set <= 0){
                            ((MainActivity)activity).removeMarkerAndSetAsUserClickedMarker();
                        }else{
                            ((MainActivity)activity).updateBottomSheetUi(num_of_time_set);
                            notifyDataSetChanged();
                        }

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }){
            @Override
            protected Map<String, String> getParams() {
                //userId=XXX&routeId=XXX&selected=XXX
                Map<String, String> params = new HashMap<String, String>();
                params.put("id", String.valueOf(id));
                return params;
            }
        };

        req.setRetryPolicy(new DefaultRetryPolicy(60000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        // TODO Auto-generated method stub
        MydApplication.getInstance().addToRequestQueue(req);
    }

    @Override
    public int getItemCount() {
        if(MainActivity.selectedTimePosition == -1) return 0;
        else if(Displayedplaces.size() <= 0) return Displayedplaces.size();
        return Displayedplaces.get(MainActivity.selectedTimePosition).getTimes().size();
    }

    private String getDummyDeleteResponse(){
        return "{\n" +
                "\t\"Result\": true\n" +
                "}";
    }

    private ProgressDialog progressDialog;
    public void showProgressDialog(String message, boolean isIntermidiate, boolean isCancelable) {
       /**/
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(activity);
        }
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog.setIndeterminate(isIntermidiate);
        progressDialog.setCancelable(isCancelable);
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    public void dismissProgressDialog() {
        if (progressDialog == null) {
            return;
        }
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

}