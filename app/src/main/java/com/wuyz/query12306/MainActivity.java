package com.wuyz.query12306;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.wuyz.query12306.model.TrainInfo;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity implements View.OnClickListener, TextToSpeech.OnInitListener, CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "MainActivity";
    private static final String ACTION_QUERY = "com.wuyz.query12306.ACTION_QUERY";

    private Button queryButton;
    private Button timerQueryButton;
    private Button exchangeButton;
    private AutoCompleteTextView startStationView;
    private AutoCompleteTextView endStationView;
    private Spinner timerSpinner;
    private TextView startDateText;
    private TextView startTimeText;
    private TextView endDateText;
    private TextView endTimeText;
    private CheckBox firstSeatCheck;
    private CheckBox secondSeatCheck;
    private CheckBox noSeatCheck;

    private List<TrainInfo> data = new ArrayList<>();
    private List<TrainInfo> filteredData = new ArrayList<>();
    private Map<String, String> stationMap;
//    private List<StationInfo> stations;
    private MyAdapter adapter;
    private SharedPreferences preferences;
    private Calendar startDate = Calendar.getInstance();
    private Calendar endDate = Calendar.getInstance();
    private Calendar startTime = Calendar.getInstance();
    private Calendar endTime = Calendar.getInstance();
    private String[] timerKeys = new String[] {"5秒", "10秒", "30秒", "1分钟", "10分钟", "30分钟", "60分钟"};
    private int[] timerValues = new int[] {5, 10, 30, 60, 600, 1800, 3600};
    private AlarmManager alarmManager;
    private boolean isInTimerQuery = false;
    private PendingIntent pendingIntent;
    private Vibrator vibrator;
    private String startCode;
    private String endCode;

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            tryQueryTicket();
        }
    };

    private Comparator<TrainInfo> comparator = new Comparator<TrainInfo>() {
        @Override
        public int compare(TrainInfo o1, TrainInfo o2) {
            int ret = Boolean.compare(TrainInfo.isSellOut(o1.getZe_num()), TrainInfo.isSellOut(o2.getZe_num()));
            if (ret != 0) return ret;
            ret = Boolean.compare(TrainInfo.isSellOut(o1.getWz_num()), TrainInfo.isSellOut(o2.getWz_num()));
            if (ret != 0) return ret;
            ret = Boolean.compare(TrainInfo.isSellOut(o1.getZy_num()), TrainInfo.isSellOut(o2.getZy_num()));
            if (ret != 0) return ret;
            return (o1.getStart_train_date() + o1.getStart_time()).compareTo(o2.getStart_train_date() + o2.getStart_time());
        }
    };

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            tryQueryTicket();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0,
                new Intent(ACTION_QUERY), PendingIntent.FLAG_UPDATE_CURRENT);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        stationMap = Utils.readStations(this);
        initViews();
        restorePrefs();
        registerReceiver(receiver, new IntentFilter(ACTION_QUERY));
    }

    private void initViews() {
        queryButton = (Button) findViewById(R.id.query_button);
        timerQueryButton = (Button) findViewById(R.id.timer_query_button);
        startStationView = (AutoCompleteTextView) findViewById(R.id.start_station_view);
        endStationView = (AutoCompleteTextView) findViewById(R.id.end_station_view);
        timerSpinner = (Spinner) findViewById(R.id.timer_spinner);
        startDateText = (TextView) findViewById(R.id.start_date_view);
        endDateText = (TextView) findViewById(R.id.end_date_view);
        startTimeText = (TextView) findViewById(R.id.start_time_view);
        endTimeText = (TextView) findViewById(R.id.end_time_view);
        firstSeatCheck = (CheckBox) findViewById(R.id.first_seat_check);
        secondSeatCheck = (CheckBox) findViewById(R.id.second_seat_check);
        noSeatCheck = (CheckBox) findViewById(R.id.no_seat_check);
        exchangeButton = (Button) findViewById(R.id.exchange_button);

        firstSeatCheck.setOnCheckedChangeListener(this);
        secondSeatCheck.setOnCheckedChangeListener(this);
        noSeatCheck.setOnCheckedChangeListener(this);

        queryButton.setOnClickListener(this);
        timerQueryButton.setOnClickListener(this);
        startDateText.setOnClickListener(this);
        endDateText.setOnClickListener(this);
        startTimeText.setOnClickListener(this);
        endTimeText.setOnClickListener(this);
        exchangeButton.setOnClickListener(this);
        findViewById(R.id.error_button).setOnClickListener(this);

        ListView listView = (ListView) findViewById(R.id.list1);
        adapter = new MyAdapter();
        listView.setAdapter(adapter);

        List<String> keys = new ArrayList<>(stationMap.size());
        Set<String> set = stationMap.keySet();
        for (String k : set) {
            keys.add(k);
        }
        ArrayAdapter<String> startStationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
                keys);
        startStationView.setAdapter(startStationAdapter);

        ArrayAdapter<String> endStationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
                keys);
        endStationView.setAdapter(endStationAdapter);

        timerSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, timerKeys));
    }

    private void restorePrefs() {
        String s = preferences.getString("startStation", "");
        if (s.isEmpty()) s = "厦门北";
        startStationView.setText(s);

        s = preferences.getString("endStation", "");
        if (s.isEmpty()) s = "铜陵";
        endStationView.setText(s);

        int i = preferences.getInt("timer", 0);
        if (i  >= 0 && i < timerSpinner.getAdapter().getCount()) {
            timerSpinner.setSelection(i);
        }

        firstSeatCheck.setChecked(preferences.getBoolean("firstSeat", false));
        secondSeatCheck.setChecked(preferences.getBoolean("secondSeat", true));
        noSeatCheck.setChecked(preferences.getBoolean("noSeat", false));

        Calendar curTime = Calendar.getInstance();

        s = preferences.getString("startDate", "");
        try {
            startDate.setTime(Utils.dateFormat.parse(s));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (startDate.before(curTime))
            startDate.setTimeInMillis(curTime.getTimeInMillis());
        startDateText.setText(Utils.dateFormat.format(startDate.getTime()));

        s = preferences.getString("endDate", "");
        try {
            endDate.setTime(Utils.dateFormat.parse(s));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (endDate.before(curTime))
            endDate.setTimeInMillis(curTime.getTimeInMillis());
        endDateText.setText(Utils.dateFormat.format(endDate.getTime()));

        s = preferences.getString("startTime", "00:00");
        try {
            startTime.setTime(Utils.timeFormat.parse(s));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        startTimeText.setText(Utils.timeFormat.format(startTime.getTime()));

        s = preferences.getString("endTime", "23:59");
        try {
            endTime.setTime(Utils.timeFormat.parse(s));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        endTimeText.setText(Utils.timeFormat.format(endTime.getTime()));
    }

    private void savePrefs() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("startStation", startStationView.getText().toString());
        editor.putString("endStation", endStationView.getText().toString());
        editor.putInt("timer", timerSpinner.getSelectedItemPosition());
        editor.putBoolean("firstSeat", firstSeatCheck.isChecked());
        editor.putBoolean("secondSeat", secondSeatCheck.isChecked());
        editor.putBoolean("noSeat", noSeatCheck.isChecked());
        editor.putString("startDate", Utils.dateFormat.format(startDate.getTime()));
        editor.putString("endDate", Utils.dateFormat.format(endDate.getTime()));
        editor.putString("startTime", Utils.timeFormat.format(startTime.getTime()));
        editor.putString("endTime", Utils.timeFormat.format(endTime.getTime()));
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        unregisterReceiver(receiver);
        savePrefs();
        super.onDestroy();
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.query_button:
                if (!Utils.isNetworkConnected(this)) {
                    Toast.makeText(this, "网络未连接，请检查！", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!getStationCode()) {
                    Toast.makeText(this, "站点有误！", Toast.LENGTH_SHORT).show();
                    return;
                }
                savePrefs();
                queryButton.setEnabled(false);
                tryQueryTicket();
                break;
            case R.id.timer_query_button:
                if (isInTimerQuery) {
                    stopTimer();
                    return;
                }
                if (!getStationCode()) {
                    Toast.makeText(this, "站点有误！", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!Utils.isNetworkConnected(this)) {
                    Toast.makeText(this, "网络未连接，请检查！", Toast.LENGTH_SHORT).show();
                    return;
                }
                savePrefs();
                isInTimerQuery = true;
                enableViews(false);
                tryQueryTicket();
                break;
            case R.id.start_date_view:
                DatePickerDialog dialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        startDate.set(Calendar.YEAR, year);
                        startDate.set(Calendar.MONTH, month);
                        startDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        startDate.set(Calendar.HOUR_OF_DAY, 0);
                        startDate.set(Calendar.MINUTE, 0);
                        startDateText.setText(Utils.dateFormat.format(startDate.getTime()));
                    }
                }, startDate.get(Calendar.YEAR), startDate.get(Calendar.MONTH), startDate.get(Calendar.DAY_OF_MONTH));
                dialog.getDatePicker().setMinDate(Math.min(startDate.getTimeInMillis(), System.currentTimeMillis()) - 1000);
                dialog.show();
                break;
            case R.id.end_date_view:
                dialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        endDate.set(Calendar.YEAR, year);
                        endDate.set(Calendar.MONTH, month);
                        endDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        endDate.set(Calendar.HOUR_OF_DAY, 23);
                        endDate.set(Calendar.MINUTE, 59);
                        endDateText.setText(Utils.dateFormat.format(endDate.getTime()));
                    }
                }, endDate.get(Calendar.YEAR), endDate.get(Calendar.MONTH), endDate.get(Calendar.DAY_OF_MONTH));
                dialog.getDatePicker().setMinDate(Math.min(endDate.getTimeInMillis(), System.currentTimeMillis()) - 1000);
                dialog.show();
                break;
            case R.id.start_time_view:
                TimePickerDialog dialog2 = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        startTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        startTime.set(Calendar.MINUTE, minute);
                        startTimeText.setText(Utils.timeFormat.format(startTime.getTime()));
                    }
                }, startTime.get(Calendar.HOUR_OF_DAY), startTime.get(Calendar.MINUTE), true);
                dialog2.show();
                break;
            case R.id.end_time_view:
                dialog2 = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        endTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        endTime.set(Calendar.MINUTE, minute);
                        endTimeText.setText(Utils.timeFormat.format(endTime.getTime()));
                    }
                }, endTime.get(Calendar.HOUR_OF_DAY), endTime.get(Calendar.MINUTE), true);
                dialog2.show();
                break;
            case R.id.exchange_button:
                String s1 = startStationView.getText().toString();
                String s2 = endStationView.getText().toString();
                startStationView.setText(s2);
                endStationView.setText(s1);
                break;
            case R.id.error_button:
                File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "error.txt");
                if (file.exists() && file.isFile()) {
                    Intent intent = new Intent(this, MessageActivity.class);
                    intent.setData(Uri.fromFile(file));
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "no error log", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private boolean getStationCode() {
//        startCode = null;
//        endCode = null;
        String startName = startStationView.getText().toString();
        String endName = endStationView.getText().toString();
//        for (StationInfo info : stations) {
//            if (startCode == null && info.name.equals(startName)) {
//                startCode = info.code;
//            }
//            if (endCode == null && info.name.equals(endName)) {
//                endCode = info.code;
//            }
//            if (startCode != null && endCode != null)
//                break;
//        }
        startCode = stationMap.get(startName);
        endCode = stationMap.get(endName);
        return startCode != null && endCode != null;
    }

    private void tryQueryTicket() {
        startDate.set(Calendar.HOUR_OF_DAY, 0);
        startDate.set(Calendar.MINUTE, 0);
        endDate.set(Calendar.HOUR_OF_DAY, 23);
        endDate.set(Calendar.MINUTE, 59);
        final String time1 = Utils.timeFormat.format(startTime.getTime());
        final String time2 = Utils.timeFormat.format(endTime.getTime());
        Log2.d(TAG, "tryQueryTicket %s - %s, %s - %s",
                Utils.dateFormat.format(startDate.getTime()),
                Utils.dateFormat.format(endDate.getTime()),
                time1, time2);

        ThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                data.clear();
                if (Utils.isNetworkConnected(MainActivity.this)) {
                    Calendar calendar = (Calendar) startDate.clone();
                    while (calendar.getTimeInMillis() <= endDate.getTimeInMillis()) {
                        String content = Utils.queryLeftTickets( startCode, endCode,
                                Utils.dateFormat.format(calendar.getTime()));
                        List<TrainInfo> list = Utils.parseAvailableTrains(content, time1, time2);
                        if (list != null)
                            data.addAll(list);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        calendar.add(Calendar.DAY_OF_YEAR, 1);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            filterTrans();

                            if (isInTimerQuery) {
                                if (!data.isEmpty()) {
                                    for (TrainInfo info : data) {
                                        if (info.isMatch(firstSeatCheck.isChecked(), secondSeatCheck.isChecked(), noSeatCheck.isChecked())) {
                                            stopTimer();
                                            notifyUser();
                                            return;
                                        }
                                    }
                                }

                                int dealtTime = timerValues[timerSpinner.getSelectedItemPosition()] * 1000;
                                if (dealtTime < 300000) {
                                    handler.removeCallbacks(runnable);
                                    handler.postDelayed(runnable, dealtTime);
                                } else {
                                    alarmManager.cancel(pendingIntent);
                                    alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + dealtTime, pendingIntent);
                                }
                            } else {
                                enableViews(true);
                            }
                        }
                    });
                } else {
                    if (isInTimerQuery) {
                        int dealtTime = timerValues[timerSpinner.getSelectedItemPosition()] * 1000;
                        if (dealtTime < 300000) {
                            handler.removeCallbacks(runnable);
                            handler.postDelayed(runnable, dealtTime);
                        } else {
                            alarmManager.cancel(pendingIntent);
                            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + dealtTime, pendingIntent);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (isInTimerQuery) {
            moveTaskToBack(true);
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
            Log2.e(TAG, "failed to init text to speech");
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        filterTrans();
    }

    private void filterTrans() {
        filteredData.clear();
        boolean all = !firstSeatCheck.isChecked() && !secondSeatCheck.isChecked() && !noSeatCheck.isChecked();
        for (TrainInfo info : data) {
            if (all || info.isMatch(firstSeatCheck.isChecked(), secondSeatCheck.isChecked(), noSeatCheck.isChecked()))
                filteredData.add(info);
        }
        Collections.sort(filteredData, comparator);
        adapter.notifyDataSetChanged();
    }

    private static class MyViewHolder {

        TextView number, date, fromStation, toStation, startTime, endTime, spendTime, firstSeat, secondSeat, noSeat;
//        View mask;


        MyViewHolder(View itemView) {
            itemView.setTag(this);
            number = (TextView) itemView.findViewById(R.id.number);
            date = (TextView) itemView.findViewById(R.id.date);
            fromStation = (TextView) itemView.findViewById(R.id.from_station);
            toStation = (TextView) itemView.findViewById(R.id.to_station);
            startTime = (TextView) itemView.findViewById(R.id.start_time);
            endTime = (TextView) itemView.findViewById(R.id.end_time);
            spendTime = (TextView) itemView.findViewById(R.id.spend_time);
            firstSeat = (TextView) itemView.findViewById(R.id.first_seat);
            secondSeat = (TextView) itemView.findViewById(R.id.second_seat);
            noSeat = (TextView) itemView.findViewById(R.id.no_seat);
//            mask = itemView.findViewById(R.id.mask);
        }

        void clear() {
            number.setText("");
            date.setText("");
            fromStation.setText("");
            toStation.setText("");
            startTime.setText("");
            endTime.setText("");
            spendTime.setText("");
            firstSeat.setText("");
            secondSeat.setText("");
            noSeat.setText("");
//            mask.setVisibility(View.VISIBLE);
        }

        void setInfo(TrainInfo info) {
            if (info != null) {
                number.setText(info.getStation_train_code());
                date.setText(info.getStart_train_date());
                if (info.getFrom_station_name().equals(info.getStart_station_name())) {
                    fromStation.setText(info.getFrom_station_name());
                } else {
                    fromStation.setText(info.getFrom_station_name() + "("
                            + info.getStart_station_name() + ")");
                }
                toStation.setText(info.getTo_station_name());
                startTime.setText(info.getStart_time());
                endTime.setText(info.getArrive_time());
                spendTime.setText(info.getLishi());
                firstSeat.setText("一等:" + info.getZy_num());
                secondSeat.setText("二等:" + info.getZe_num());
                noSeat.setText("无座:" + info.getWz_num());

                // mask.setVisibility(info.canBuy() ? View.GONE : View.VISIBLE);
            } else {
                clear();
            }
        }
    }

    private class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return filteredData == null ? 0 : filteredData.size();
        }

        @Override
        public Object getItem(int position) {
            return filteredData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MyViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
                viewHolder = new MyViewHolder(convertView);
            } else {
                viewHolder = (MyViewHolder) convertView.getTag();
            }
            TrainInfo info = filteredData.get(position);
            viewHolder.setInfo(info);
            convertView.setBackgroundColor(info.canBuy() ? Color.WHITE : Color.LTGRAY);
            return convertView;
        }
    }

    private void enableViews(boolean enable) {
        queryButton.setEnabled(enable);
        timerQueryButton.setText(enable ? "定时查询" : "停止查询");
        startStationView.setEnabled(enable);
        endStationView.setEnabled(enable);
        timerSpinner.setEnabled(enable);
        startDateText.setEnabled(enable);
        startTimeText.setEnabled(enable);
        endDateText.setEnabled(enable);
        endTimeText.setEnabled(enable);
//        firstSeatCheck.setEnabled(enable);
//        secondSeatCheck.setEnabled(enable);
//        noSeatCheck.setEnabled(enable);
        exchangeButton.setEnabled(enable);
    }

    private void stopTimer() {
        alarmManager.cancel(pendingIntent);
        isInTimerQuery = false;
        enableViews(true);
    }

    private void notifyUser() {
        vibrator.cancel();
        vibrator.vibrate(new long[]{100, 200, 100, 200}, 3);
        Notification.Builder builder = new Notification.Builder(this);
        builder.setTicker("12306");
        builder.setContentText("发现火车票");
        builder.setContentTitle("发现火车票");
        builder.setSmallIcon(R.mipmap.ic_launcher);
        Notification notification = builder.build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }
}
