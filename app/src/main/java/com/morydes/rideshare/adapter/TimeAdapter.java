package com.morydes.rideshare.adapter;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.morydes.rideshare.MainActivity;
import com.morydes.rideshare.R;
import com.morydes.rideshare.Utility.CommonMethods;
import com.morydes.rideshare.alertbanner.AlertDialogForAnything;
import com.morydes.rideshare.appdata.GlobalAppAccess;
import com.morydes.rideshare.appdata.MydApplication;
import com.morydes.rideshare.model.Time;
import com.morydes.rideshare.model.TimeLocation;
import com.morydes.rideshare.receiver.AlarmReceiver;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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

    private PopupWindow popupwindow_obj;

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

        final TimeLocation timeLocation = Displayedplaces.get(MainActivity.selectedTimePosition);
        final Time event = timeLocation.getTimes().get(position);
        holder.tv_time.setText(CommonMethods.changeFormat(event.getTime(),"dd/MM/yyyy HH:mm:ss.SSS", "MM/dd/yyyy hh:mm a"));
        if (MydApplication.deviceImieNumber.equals(event.getDeviceId())) {
            holder.btn_delete.setVisibility(View.VISIBLE);
        } else {
            holder.btn_delete.setVisibility(View.GONE);
        }

        holder.btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sentRequestToDeleteTime(GlobalAppAccess.URL_DELETE_TIME, event.getId(), position);
            }
        });

        holder.btn_set_alarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showReminderDialog(event.getTime(),timeLocation.getLat(), timeLocation.getLang());
            }
        });

    }

    private void showReminderDialog(final String time_of_the_marker, final double lat, final double lang) {
        final Dialog dialog_start = new Dialog(activity,
                android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog_start.setCancelable(true);
        dialog_start.setContentView(R.layout.dialog_set_reminder_2);

        final Spinner sp_time = (Spinner) dialog_start.findViewById(R.id.sp_time);
        Button btn_submit = (Button) dialog_start.findViewById(R.id.btn_submit);
        ImageView img_close_dialog = (ImageView) dialog_start.findViewById(R.id.img_close_dialog);

        ArrayAdapter<String> dataAdapter_pillars_name = new ArrayAdapter<String>
                (activity, R.layout.spinner_item, GlobalAppAccess.reminder_time_options);
        sp_time.setAdapter(dataAdapter_pillars_name);




        final Calendar calendar = Calendar.getInstance();
        calendar.clear();

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
        Date date_of_the_marker = null;
        try {
             date_of_the_marker = formatter.parse(time_of_the_marker);
            calendar.setTime(date_of_the_marker);
        } catch (ParseException e) {
            e.printStackTrace();
        }


        final Date finalDate_of_the_marker = date_of_the_marker;
        sp_time.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(finalDate_of_the_marker == null)return;
                calendar.clear();
                calendar.setTime(finalDate_of_the_marker);
                if(i == 0){

                }else if(i == 1){
                    calendar.add(Calendar.MINUTE, -3);
                }else if(i == 2){
                    calendar.add(Calendar.MINUTE, -15);
                }else if(i == 3){
                    calendar.add(Calendar.MINUTE, -30);
                }else if(i == 4){
                    calendar.add(Calendar.HOUR, -1);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });



        btn_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Date date_of_the_alarm = calendar.getTime();

                Date currentDate = new Date();

                if(currentDate.after(date_of_the_alarm)){
                    Toast.makeText(activity, "The current is very near to the set time. You cant set this time!", Toast.LENGTH_LONG).show();
                    return;
                }

                setAlarm(calendar, lat, lang);
                dialog_start.dismiss();

            }
        });

        img_close_dialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog_start.dismiss();
            }
        });

        dialog_start.show();

    }

    private void setAlarm(Calendar targetCal, double lat, double lang) {

        Date date = targetCal.getTime();
        String time = CommonMethods.formatDate(date, "yyyy-MM-dd HH:mm:ss");


        Intent intent = new Intent(activity, AlarmReceiver.class);
        intent.putExtra("lat", lat);
        intent.putExtra("lang", lang);
        intent.putExtra("time", time);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(activity, 101, intent, 0);
        AlarmManager alarmManager = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, targetCal.getTimeInMillis(), pendingIntent);

        int num_of_time_user_set_alarm = MydApplication.getInstance().getPrefManger().getNumberOfTimeUserSetAlarm();

        num_of_time_user_set_alarm = num_of_time_user_set_alarm + 1;

        if(num_of_time_user_set_alarm == 3){
            ((MainActivity) activity).showInterstitialAds();
            num_of_time_user_set_alarm = 0;
        }


        MydApplication.getInstance().getPrefManger().setNumberOfTimeUserSetAlarm(num_of_time_user_set_alarm);



        Toast.makeText(activity, "Successfully added reminder.", Toast.LENGTH_SHORT).show();
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
                                if (num_of_time_set <= 0) {
                                    ((MainActivity) activity).removeMarkerAndSetAsUserClickedMarker();
                                } else {
                                    ((MainActivity) activity).updateBottomSheetUi(num_of_time_set);
                                    notifyDataSetChanged();
                                }
                                ((MainActivity) activity).changeUserPinFlagToFalse();

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
                        if (num_of_time_set <= 0) {
                            ((MainActivity) activity).removeMarkerAndSetAsUserClickedMarker();
                        } else {
                            ((MainActivity) activity).updateBottomSheetUi(num_of_time_set);
                            notifyDataSetChanged();
                        }

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }) {
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
        if (MainActivity.selectedTimePosition == -1) return 0;
        else if (Displayedplaces.size() <= 0) return Displayedplaces.size();
        return Displayedplaces.get(MainActivity.selectedTimePosition).getTimes().size();
    }

    private String getDummyDeleteResponse() {
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