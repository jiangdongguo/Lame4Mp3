package com.jiangdg.lametomp3;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;

/** 音频录制
 * Created by jiandpngguo on 2017/6/11.
 */

public class Mp3Recorder {
    private final String TAG = "Mp3Recorder";
    private static final String ROOTPATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    private boolean isRecording = false;
    // AudioRecord配置参数
    private AudioRecord mAudioRecord;
    private int audioSource = MediaRecorder.AudioSource.MIC; // 音频源 麦克风
    private int sampleRateInHz = 8000;  //采样率8000
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;  // 单声道
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT; // 采样精度 16位
    private int bufferSizeInBytes;
    // lameMp3参数配置
    private int outChannel = 1; // 单声道
    private int bitRate = 32;   // 比特率 32kbps
    private int qaulityDegree = 7; // 0，最差最慢，9最好最快


    private void initAudioRecord(){
        if(mAudioRecord != null){
            mAudioRecord.release();
            mAudioRecord = null;
        }
        // 设置录制优先级
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        // 设置录制缓存
        bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz,channelConfig,audioFormat);
        // 初始化AudioRecorder
        mAudioRecord = new AudioRecord(audioSource,sampleRateInHz,channelConfig,audioFormat,bufferSizeInBytes*2);
        mAudioRecord.startRecording();
        isRecording = true;
    }

    private void initLameMp3(){
        // 初始化lame引擎
        LameMp3.lameInit(sampleRateInHz,outChannel,sampleRateInHz,bitRate,qaulityDegree);
    }

    public void startMp3Record(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(!isRecording){
                        Log.d(TAG,"初始化");
                        initLameMp3();
                        initAudioRecord();
                    }
                    // 5秒缓存
                    int readBytes = 0;
                    short[] audioData = new short[sampleRateInHz * (16 /8) * 1 *5];
                    byte[] mp3Buffer = new byte[(int)(7200 + audioData.length * 2 * 1.25)];
//                    byte[] audioData = new byte[1024];
//                    byte[] mp3Buffer = new byte[(int)(7200 + audioData.length * 2 * 1.25)];
                    FileOutputStream fops = null;
                    try {
                        fops = new FileOutputStream(new File(ROOTPATH+File.separator+System.currentTimeMillis()+".mp3"));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    try {
                        while(isRecording){
                            readBytes = mAudioRecord.read(audioData,0,bufferSizeInBytes);
                            Log.d(TAG,"读取pcm数据流，大小为："+readBytes);
                            if(readBytes >0 ){
                                // 编码
//                                short[] data = bytes2Shorts(audioData);
//                                int encResult = LameMp3.lameEncode(data,data,data.length,mp3Buffer);
                                int encResult = LameMp3.lameEncode(audioData,audioData,readBytes,mp3Buffer);
                                Log.d(TAG,"lame编码，大小为："+encResult);
                                if(encResult != 0){
                                    try {
                                        fops.write(mp3Buffer,0,encResult);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        // 录音完毕
                        int flushResult =  LameMp3.lameFlush(mp3Buffer);
                        Log.d(TAG,"录制完毕，大小为："+flushResult);
                        if(flushResult != 0){
                            try {
                                fops.write(mp3Buffer,0,flushResult);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        try {
                            fops.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }finally {
                        Log.d(TAG,"释放AudioRecorder资源");
                        stopAudioRecorder();

                    }
                }finally {
                    Log.d(TAG,"释放Lame库资源");
                    stopLameMp3();
                }
            }
        }).start();
    }

    private void stopAudioRecorder(){
        if(mAudioRecord != null){
            mAudioRecord.stop();;
            mAudioRecord.release();
        }
        isRecording = false;
    }

    private void stopLameMp3(){
        LameMp3.lameClose();
    }

    public void stopMp3Record(){
        isRecording = false;
    }

    public boolean isRecording(){
        return isRecording;
    }
}
