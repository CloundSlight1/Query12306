package com.wuyz.query12306;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

/**
 * Created by wuyz on 2016/10/8.
 *
 */

public class MessageActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri uri = getIntent().getData();
        if (uri != null) {
            File file = new File(uri.getPath());
            if (file.exists() && file.isFile()) {
                String content = null;
                try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
                     ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    int b;
                    while ((b = inputStream.read()) != -1) {
                        outputStream.write(b);
                    }
                    content = outputStream.toString();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (content != null && !content.isEmpty()) {
                    setContentView(R.layout.activity_message);
                    TextView textView = (TextView) findViewById(R.id.content);
                    textView.setText(content);
                    return;
                }
            }
        }
        finish();
    }
}
