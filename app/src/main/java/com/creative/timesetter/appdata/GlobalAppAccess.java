package com.creative.timesetter.appdata;



public class GlobalAppAccess {


    public static final String APP_NAME = "TimeSetter";
    public static String BaseUrl = "http://204.12.241.178/";
    //public static String BaseUrl = "https://b5e99a4d.ngrok.io/bgb/";
    public static final String URL_GET_TIMES =  BaseUrl + "getTimes";
    public static final String URL_INSERT_TIME = BaseUrl +  "insertTime";
    public static final String URL_DELETE_TIME = BaseUrl +  "deleteTime";


    public static final  int SUCCESS = 1;
    public static  final  int ERROR = 0;



    // global topic to receive app wide push notifications
    public static final String TOPIC_GLOBAL = "global";

    // broadcast receiver intent filters
    public static final String REGISTRATION_COMPLETE = "registrationComplete";
    public static final String PUSH_NOTIFICATION = "pushNotification";

    // id to handle the notification in the notification tray
    public static final int NOTIFICATION_ID = 100;
    public static final int NOTIFICATION_ID_BIG_IMAGE = 101;


    public static final String KEY_CALL_FROM = "call_from";
    public static final String KEY_NOTIFICATION_ID = "notification_id";

    public static final String TAG_ALARM_RECEIVER = "alarm_receiver";

}
