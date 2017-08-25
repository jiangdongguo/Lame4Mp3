package com.teligen.lametomp3demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.jiangdg.lametomp3.Mp3Recorder;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Mp3Recorder mp3Recorder = new Mp3Recorder();
        final Button mBtnRec = (Button)findViewById(R.id.btn_record);
        mBtnRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mp3Recorder.isRecording()){
                    mp3Recorder.startMp3Record();
                    mBtnRec.setText("停止录音");
                }else{
                    mp3Recorder.stopMp3Record();
                    mBtnRec.setText("开始录音");
                }
            }
        });
    }
}
