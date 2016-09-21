package com.wuyz.query12306;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.wuyz.query12306.model.TrainInfo;

import java.util.List;
import java.util.Map;

public class MainActivity extends Activity implements View.OnClickListener {

    private Button queryButton;
    private Button timerQueryButton;
    private ListView listView;
    private Spinner startStationSpinner;
    private Spinner endStationSpinner;
    private Spinner startTimeSpinner;
    private Spinner endTimeSpinner;
    private Spinner timerSpinner;

    private List<TrainInfo> data;
    private Map<String, String> stationMap;
    private MyAdapter adapter;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stationMap = Utils.readStations(this);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        initViews();
    }

    private void initViews() {
        queryButton = (Button) findViewById(R.id.query_button);
        timerQueryButton = (Button) findViewById(R.id.timer_query_button);
        startStationSpinner = (Spinner) findViewById(R.id.start_station_spinner);
        endStationSpinner = (Spinner) findViewById(R.id.end_station_spinner);
        startTimeSpinner = (Spinner) findViewById(R.id.start_time_view);
        endTimeSpinner = (Spinner) findViewById(R.id.end_time_view);
        timerSpinner = (Spinner) findViewById(R.id.timer_spinner);

        queryButton.setOnClickListener(this);
        timerQueryButton.setOnClickListener(this);

        listView = (ListView) findViewById(R.id.list1);
        adapter = new MyAdapter();
        listView.setAdapter(adapter);

        startStationSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                stationMap.keySet().toArray()));
        endStationSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                stationMap.keySet().toArray()));
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.query_button:
                tryQueryTicket();
                break;
        }
    }

    private void tryQueryTicket() {

        queryButton.setEnabled(false);

        ThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                String content = Utils.queryLeftTickets("XKS", "ARH", "2016-09-30");
                final List<TrainInfo> list = Utils.parseAvailableTrains(content);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                content = Utils.queryLeftTickets("XKS", "ARH", "2016-10-01");
                List<TrainInfo> list2 = Utils.parseAvailableTrains(content);
                if (list != null && list2 != null)
                    list.addAll(list2);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                content = Utils.queryLeftTickets("XKS", "ARH", "2016-10-02");
                list2 = Utils.parseAvailableTrains(content);
                if (list != null && list2 != null)
                    list.addAll(list2);

                data = list;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                        queryButton.setEnabled(true);
                    }
                });
            }
        });
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
}
