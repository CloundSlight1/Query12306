package com.wuyz.query12306;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.wuyz.query12306.model.TrainInfo;

import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView outputText;
    private Button queryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        outputText = (TextView) findViewById(R.id.output_text);
        queryButton = (Button) findViewById(R.id.query_button);
        queryButton.setOnClickListener(this);
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
        outputText.setText("");

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

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (list != null && !list.isEmpty()) {
                            for (TrainInfo info : list) {
                                outputText.append(info.toString() + "\n");
                            }
                        } else {
                            outputText.setText("done!");
                        }
                        queryButton.setEnabled(true);
                    }
                });
            }
        });
    }
}
