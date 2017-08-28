package com.teligen.lametomp3demo;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.jiangdg.lametomp3.Mp3Recorder;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private Mp3Recorder mMp3Recorder;
    private boolean isStart = false;
    private Button mBtnRec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 初始化UI
        mBtnRec = (Button)findViewById(R.id.btn_record);
        mBtnRec.setOnClickListener(this);

        mMp3Recorder = Mp3Recorder.getInstance();
        // 配置AudioRecord参数
        mMp3Recorder.setAudioSource(Mp3Recorder.AUDIO_SOURCE_MIC);
        mMp3Recorder.setAudioSampleRare(Mp3Recorder.SMAPLE_RATE_8000HZ);
        mMp3Recorder.setAudioChannelConfig(Mp3Recorder.AUDIO_CHANNEL_MONO);
        mMp3Recorder.setAduioFormat(Mp3Recorder.AUDIO_FORMAT_16Bit);
        // 配置Lame参数
        mMp3Recorder.setLameBitRate(Mp3Recorder.LAME_BITRATE_32);
        mMp3Recorder.setLameOutChannel(Mp3Recorder.LAME_OUTCHANNEL_1);
        // 配置MediaCodec参数
        mMp3Recorder.setMediaCodecBitRate(Mp3Recorder.ENCODEC_BITRATE_1600HZ);
        mMp3Recorder.setMediaCodecSampleRate(Mp3Recorder.SMAPLE_RATE_8000HZ);
        // 设置模式
        mMp3Recorder.setMode(Mp3Recorder.MODE_BOTH);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btn_record){
            if(! isStart){
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
                String fileName = "audio"+System.currentTimeMillis();
                mMp3Recorder.start(filePath, fileName, new Mp3Recorder.OnAACStreamResultListener() {
                    @Override
                    public void onEncodeResult(byte[] data, int offset, int length, long timestamp) {
                        Log.i("MainActivity","acc数据流长度："+data.length);
                    }
                });

                mBtnRec.setText("停止");
            }else{
                mMp3Recorder.stop();

                mBtnRec.setText("开始");
            }
            isStart = ! isStart;
        }
    }
}
