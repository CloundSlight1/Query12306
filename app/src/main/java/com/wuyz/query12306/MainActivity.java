package com.wuyz.query12306;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.wuyz.query12306.model.TrainInfo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity implements View.OnClickListener, TextToSpeech.OnInitListener {
    private static final String TAG = "MainActivity";
    private static final String ACTION_QUERY = "com.wuyz.query12306.ACTION_QUERY";

    private Button queryButton;
    private Button timerQueryButton;
    private ListView listView;
    private Spinner startStationSpinner;
    private Spinner endStationSpinner;
    private Spinner timerSpinner;
    private TextView startDateText;
    private TextView startTimeText;
    private TextView endDateText;
    private TextView endTimeText;
    private CheckBox firstSeatCheck;
    private CheckBox secondSeatCheck;
    private CheckBox noSeatCheck;

    private List<TrainInfo> data = new ArrayList<>();
    private Map<String, String> stationMap = new HashMap<>(2500);
    private List<String> stationNames = new ArrayList<>(stationMap.size());
    private MyAdapter adapter;
    private SharedPreferences preferences;
    private Calendar startTime;
    private Calendar endTime;
    private ArrayAdapter<String> startStationAdapter;
    private ArrayAdapter<String> endStationAdapter;
    private String[] timerKeys = new String[] {"5秒", "30秒", "1分钟", "10分钟", "30分钟", "60分钟"};
    private int[] timerValues = new int[] {5, 30, 60, 600, 1800, 3600};
    private AlarmManager alarmManager;
    private boolean isInTimerQuery = false;
    private PendingIntent pendingIntent;
    private Vibrator vibrator;
    private TextToSpeech textToSpeech;

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
        textToSpeech = new TextToSpeech(this, this);

        Utils.readStations(this, stationMap, stationNames);
        initViews();
        restorePrefs();
        registerReceiver(receiver, new IntentFilter(ACTION_QUERY));
    }

    private void initViews() {
        queryButton = (Button) findViewById(R.id.query_button);
        timerQueryButton = (Button) findViewById(R.id.timer_query_button);
        startStationSpinner = (Spinner) findViewById(R.id.start_station_spinner);
        endStationSpinner = (Spinner) findViewById(R.id.end_station_spinner);
        timerSpinner = (Spinner) findViewById(R.id.timer_spinner);
        startDateText = (TextView) findViewById(R.id.start_date_view);
        endDateText = (TextView) findViewById(R.id.end_date_view);
        startTimeText = (TextView) findViewById(R.id.start_time_view);
        endTimeText = (TextView) findViewById(R.id.end_time_view);
        firstSeatCheck = (CheckBox) findViewById(R.id.first_seat_check);
        secondSeatCheck = (CheckBox) findViewById(R.id.second_seat_check);
        noSeatCheck = (CheckBox) findViewById(R.id.no_seat_check);

        queryButton.setOnClickListener(this);
        timerQueryButton.setOnClickListener(this);
        startDateText.setOnClickListener(this);
        endDateText.setOnClickListener(this);
        startTimeText.setOnClickListener(this);
        endTimeText.setOnClickListener(this);

        listView = (ListView) findViewById(R.id.list1);
        adapter = new MyAdapter();
        listView.setAdapter(adapter);

        startStationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                stationNames);
        startStationSpinner.setAdapter(startStationAdapter);

        endStationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                stationNames);
        endStationSpinner.setAdapter(endStationAdapter);

        timerSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, timerKeys));
    }

    private void restorePrefs() {
        int i = preferences.getInt("startStation", 0);
        if (i  >= 0 && i < stationNames.size()) {
            startStationSpinner.setSelection(i);
        }
        i = preferences.getInt("endStation", 0);
        if (i  >= 0 && i < stationNames.size()) {
            endStationSpinner.setSelection(i);
        }
        i = preferences.getInt("timer", 0);
        if (i  >= 0 && i < timerSpinner.getAdapter().getCount()) {
            timerSpinner.setSelection(i);
        }

        firstSeatCheck.setChecked(preferences.getBoolean("firstSeat", false));
        secondSeatCheck.setChecked(preferences.getBoolean("secondSeat", true));
        noSeatCheck.setChecked(preferences.getBoolean("noSeat", false));

        long curTime = System.currentTimeMillis();
        long time = preferences.getLong("startTime", curTime);
        time = Math.max(time, curTime);
        startTime = Calendar.getInstance();
        startTime.setTime(new Date(time));
        startDateText.setText(Utils.dateFormat2.format(startTime.getTime()));
        startTimeText.setText(Utils.dateFormat3.format(startTime.getTime()));

        time = preferences.getLong("endTime", curTime + 24 * 3600000L);
        time = Math.max(time, curTime);
        endTime = Calendar.getInstance();
        endTime.setTime(new Date(time));
        endDateText.setText(Utils.dateFormat2.format(endTime.getTime()));
        endTimeText.setText(Utils.dateFormat3.format(endTime.getTime()));
    }

    private void savePrefs() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("startStation", startStationSpinner.getSelectedItemPosition());
        editor.putInt("endStation", endStationSpinner.getSelectedItemPosition());
        editor.putInt("timer", timerSpinner.getSelectedItemPosition());
        editor.putBoolean("firstSeat", firstSeatCheck.isChecked());
        editor.putBoolean("secondSeat", secondSeatCheck.isChecked());
        editor.putBoolean("noSeat", noSeatCheck.isChecked());
        editor.putLong("startTime", startTime.getTimeInMillis());
        editor.putLong("endTime", endTime.getTimeInMillis());
        editor.apply();
    }

    @Override
    protected void onDestroy() {
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
                queryButton.setEnabled(false);
                tryQueryTicket();
                break;
            case R.id.timer_query_button:
                if (isInTimerQuery) {
                    stopTimer();
                    return;
                }
                if (!Utils.isNetworkConnected(this)) {
                    Toast.makeText(this, "网络未连接，请检查！", Toast.LENGTH_SHORT).show();
                    return;
                }
                isInTimerQuery = true;
                enableViews(false);
                tryQueryTicket();
                break;
            case R.id.start_date_view:
                DatePickerDialog dialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        startTime.set(Calendar.YEAR, year);
                        startTime.set(Calendar.MONTH, month);
                        startTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        startTime.set(Calendar.HOUR_OF_DAY, 0);
                        startDateText.setText(Utils.dateFormat2.format(startTime.getTime()));
                    }
                }, startTime.get(Calendar.YEAR), startTime.get(Calendar.MONTH), startTime.get(Calendar.DAY_OF_MONTH));
                dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 24 * 3600000L);
                dialog.show();
                break;
            case R.id.end_date_view:
                dialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        endTime.set(Calendar.YEAR, year);
                        endTime.set(Calendar.MONTH, month);
                        endTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        endDateText.setText(Utils.dateFormat2.format(endTime.getTime()));
                        endTimeText.setText(Utils.dateFormat3.format(endTime.getTime()));
                    }
                }, endTime.get(Calendar.YEAR), endTime.get(Calendar.MONTH), endTime.get(Calendar.DAY_OF_MONTH));
                dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 24 * 3600000L);
                dialog.show();
                break;
            case R.id.start_time_view:
                TimePickerDialog dialog2 = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        startTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        startTime.set(Calendar.MINUTE, minute);
                        startTimeText.setText(Utils.dateFormat3.format(startTime.getTime()));
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
                        endTimeText.setText(Utils.dateFormat3.format(endTime.getTime()));
                    }
                }, endTime.get(Calendar.HOUR_OF_DAY), endTime.get(Calendar.MINUTE), true);
                dialog2.show();
                break;
        }
    }

    private void tryQueryTicket() {
        Log2.d(TAG, "tryQueryTicket %s - %s", Utils.dateFormat.format(startTime.getTime()),
                Utils.dateFormat.format(endTime.getTime()));
        ThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                data.clear();
                if (Utils.isNetworkConnected(MainActivity.this)) {
                    Calendar calendar = (Calendar) startTime.clone();
                    while (calendar.getTimeInMillis() <= endTime.getTimeInMillis()) {
                        String content = Utils.queryLeftTickets(
                                stationMap.get((String) startStationSpinner.getSelectedItem()),
                                stationMap.get((String) endStationSpinner.getSelectedItem()),
                                Utils.dateFormat2.format(calendar.getTime()));
                        List<TrainInfo> list = Utils.parseAvailableTrains(content,
                                firstSeatCheck.isChecked(), secondSeatCheck.isChecked(), noSeatCheck.isChecked());
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
                            adapter.notifyDataSetChanged();

                            if (isInTimerQuery) {
                                if (!data.isEmpty()) {
                                    stopTimer();
                                    notifyUser();
                                    return;
                                }

                                int dealtTime = timerValues[timerSpinner.getSelectedItemPosition()] * 1000;
                                alarmManager.cancel(pendingIntent);
                                alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + dealtTime, pendingIntent);
                            } else {
                                enableViews(true);
                            }
                        }
                    });
                } else {
                    if (isInTimerQuery) {
                        int dealtTime = timerValues[timerSpinner.getSelectedItemPosition()] * 1000;
                        alarmManager.cancel(pendingIntent);
                        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + dealtTime, pendingIntent);
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
            textToSpeech = null;
        }
    }

    private static class MyViewHolder {

        public TextView number, date, fromStation, toStation, starTtime, endTime, spendTime, firstSeat, secondSeat, noSeat;


        public MyViewHolder(View itemView) {
            itemView.setTag(this);
            number = (TextView) itemView.findViewById(R.id.number);
            date = (TextView) itemView.findViewById(R.id.date);
            fromStation = (TextView) itemView.findViewById(R.id.from_station);
            toStation = (TextView) itemView.findViewById(R.id.to_station);
            starTtime = (TextView) itemView.findViewById(R.id.start_time);
            endTime = (TextView) itemView.findViewById(R.id.end_time);
            spendTime = (TextView) itemView.findViewById(R.id.spend_time);
            firstSeat = (TextView) itemView.findViewById(R.id.first_seat);
            secondSeat = (TextView) itemView.findViewById(R.id.second_seat);
            noSeat = (TextView) itemView.findViewById(R.id.no_seat);
        }

        void clear() {
            number.setText("");
            date.setText("");
            fromStation.setText("");
            toStation.setText("");
            starTtime.setText("");
            endTime.setText("");
            spendTime.setText("");
            firstSeat.setText("");
            secondSeat.setText("");
            noSeat.setText("");
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
                starTtime.setText(info.getStart_time());
                endTime.setText(info.getArrive_time());
                spendTime.setText(info.getLishi());
                firstSeat.setText("一等:" + info.getZy_num());
                secondSeat.setText("二等:" + info.getZe_num());
                noSeat.setText("无座:" + info.getWz_num());
            } else {
                clear();
            }
        }
    }

    private class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return data == null ? 0 : data.size();
        }

        @Override
        public Object getItem(int position) {
            return data.get(position);
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
            TrainInfo info = data.get(position);
            viewHolder.setInfo(info);
            return convertView;
        }
    }

    private void enableViews(boolean enable) {
        queryButton.setEnabled(enable);
        timerQueryButton.setText(enable ? "定时查询" : "停止查询");
        startStationSpinner.setEnabled(enable);
        endStationSpinner.setEnabled(enable);
        timerSpinner.setEnabled(enable);
        startDateText.setEnabled(enable);
        startTimeText.setEnabled(enable);
        endDateText.setEnabled(enable);
        endTimeText.setEnabled(enable);
        firstSeatCheck.setEnabled(enable);
        secondSeatCheck.setEnabled(enable);
        noSeatCheck.setEnabled(enable);
    }

    private void stopTimer() {
        alarmManager.cancel(pendingIntent);
        isInTimerQuery = false;
        enableViews(true);
    }

    private void notifyUser() {
        vibrator.cancel();
        vibrator.vibrate(new long[] {500, 500}, 4);
        if (textToSpeech != null) {
            textToSpeech.speak("find ticket", TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
}
