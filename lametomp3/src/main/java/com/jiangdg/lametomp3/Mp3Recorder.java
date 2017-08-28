package com.jiangdg.lametomp3;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/** 音频录制
 * Created by jiandpngguo on 2017/8/28.
 */

public class Mp3Recorder {
    private static final int TIMES_OUT = 10000;
    private final String TAG = "Mp3Recorder";
    private boolean isRecording = false;
    // 模式
    private int mode = -1;
    public static final int MODE_MP3 = 0;
    public static final int MODE_AAC = 1;
    public static final int MODE_BOTH = 2;
    // AudioRecord参数
    public static final int AUDIO_SOURCE_MIC = MediaRecorder.AudioSource.MIC; // 音频源 麦克风
    public static final int AUDIO_SOURCE_DEFAULT = MediaRecorder.AudioSource.DEFAULT; // 音频源 默认
    public static final int AUDIO_SOURCE_COMMUNICATION = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
    public static final int SMAPLE_RATE_8000HZ = 8000;
    public static final int SMAPLE_RATE_32000HZ = 32000;
    public static final int SMAPLE_RATE_44100HZ = 44100;
    public static final int AUDIO_CHANNEL_MONO = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_STEREO;
    public static final int AUDIO_FORMAT_16Bit = AudioFormat.ENCODING_PCM_16BIT;
    public static final int AUDIO_FORMAT_8Bit = AudioFormat.ENCODING_PCM_8BIT;
    private AudioRecord mAudioRecord;
    private int audioSource;
    private int sampleRateInHz;
    private int channelConfig;
    private int audioFormat;
    private int bufferSizeInBytes;
    // lameMp3参数配置,声道、比特率(kbps)、质量(0，最差最慢，9最好最快)
    public static final int LAME_OUTCHANNEL_1 = 1;
    public static final int LAME_OUTCHANNEL_2 = 2;
    public static final int LAME_BITRATE_32 = 32;
    private int outChannel;
    private int bitRate ;
    private final int qaulityDegree = 7;

    // MediaCodec编码器参数
    private final String MIME_TYPE = "audio/mp4a-latm";
    private final int encodec_buffer_size = 1920;
    public static final int ENCODEC_BITRATE_1600HZ = 1600;
    public static final int ENCODEC_BITRATE_3200HZ = 3200;

    public static final int[] AUDIO_SAMPLING_RATES = { 96000, // 0
            88200, // 1
            64000, // 2
            48000, // 3
            44100, // 4
            32000, // 5
            24000, // 6
            22050, // 7
            16000, // 8
            12000, // 9
            11025, // 10
            8000, // 11
            7350, // 12
            -1, // 13
            -1, // 14
            -1, // 15
    };

    private OnAACStreamResultListener listener;
    private MediaCodec mAudioEncoder;
    private long prevPresentationTimes;
    private int mSamplingRateIndex = 0;
    private int encodec_bitrate;
    private int encodec_samplerate;


    public interface OnAACStreamResultListener{
        void onEncodeResult(byte[] data, int offset,
                            int length, long timestamp);
    }

    private static Mp3Recorder mMp3Recorder;

    private Mp3Recorder(){}

    public static Mp3Recorder getInstance(){
        if(mMp3Recorder == null){
            mMp3Recorder = new Mp3Recorder();
        }
        return mMp3Recorder;
    }

    private void initLameMp3(){
        // 初始化lame引擎
        LameMp3.lameInit(sampleRateInHz,outChannel,sampleRateInHz,bitRate,qaulityDegree);
    }

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

    public void start(final String filePath, final String fileName,final OnAACStreamResultListener listener){

        this.listener = listener;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(!isRecording){
                        Log.d(TAG,"初始化");
                        initLameMp3();
                        initAudioRecord();
                        initMediaCodec();
                    }
                    // 5秒缓存
                    int readBytes = 0;
                    byte[] audioBuffer = new byte[2048];
                    byte[] mp3Buffer = new byte[1024];

                    // 如果文件路径不存在，则创建
                    if(TextUtils.isEmpty(filePath) || TextUtils.isEmpty(fileName)){
                        Log.i(TAG,"文件路径或文件名为空");
                        return;
                    }
                    File file = new File(filePath);
                    if(! file.exists()){
                        file.mkdirs();
                    }
                    String mp3Path = file.getAbsoluteFile().toString()+File.separator+fileName+".mp3";
                    FileOutputStream fops = null;

                    try {
                        while(isRecording){
                            readBytes = mAudioRecord.read(audioBuffer,0,bufferSizeInBytes);
                            Log.i(TAG,"读取pcm数据流，大小为："+readBytes);
                            if(readBytes >0 ){
                                if(mode == MODE_AAC || mode == MODE_BOTH){
                                    // 将PCM编码为AAC
                                    encodeBytes(audioBuffer,readBytes);
                                }

                                if(mode == MODE_MP3 || mode == MODE_BOTH){
                                    // 打开mp3文件输出流
                                    if(fops == null){
                                        try {
                                            fops = new FileOutputStream(mp3Path);
                                        } catch (FileNotFoundException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    // 将byte[] 转换为 short[]
                                    // 将PCM编码为Mp3，并写入文件
                                    short[] data = transferByte2Short(audioBuffer,readBytes);
                                    int encResult = LameMp3.lameEncode(data,null,data.length,mp3Buffer);
                                    Log.i(TAG,"lame编码，大小为："+encResult);
                                    if(encResult != 0){
                                        try {
                                            fops.write(mp3Buffer,0,encResult);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                        // 录音完毕
                        if(fops != null){
                            int flushResult =  LameMp3.lameFlush(mp3Buffer);
                            Log.i(TAG,"录制完毕，大小为："+flushResult);
                            if(flushResult > 0){
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
                        }
                    }finally {
                        Log.i(TAG,"释放AudioRecorder资源");
                        stopAudioRecorder();
                        stopMediaCodec();

                    }
                }finally {
                    Log.i(TAG,"释放Lame库资源");
                    stopLameMp3();
                }
            }
        }).start();
    }

    @TargetApi(21)
    private void encodeBytes(byte[] audioBuf, int readBytes) {
        ByteBuffer[] inputBuffers = mAudioEncoder.getInputBuffers();
        ByteBuffer[] outputBuffers = mAudioEncoder.getOutputBuffers();
        //返回编码器的一个输入缓存区句柄，-1表示当前没有可用的输入缓存区
        int inputBufferIndex = mAudioEncoder.dequeueInputBuffer(TIMES_OUT);
        if(inputBufferIndex >= 0){
            // 绑定一个被空的、可写的输入缓存区inputBuffer到客户端
            ByteBuffer inputBuffer  = null;
            if(!isLollipop()){
                inputBuffer = inputBuffers[inputBufferIndex];
            }else{
                inputBuffer = mAudioEncoder.getInputBuffer(inputBufferIndex);
            }
            // 向输入缓存区写入有效原始数据，并提交到编码器中进行编码处理
            if(audioBuf==null || readBytes<=0){
                mAudioEncoder.queueInputBuffer(inputBufferIndex,0,0,getPTSUs(), MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            }else{
                inputBuffer.clear();
                inputBuffer.put(audioBuf);
                mAudioEncoder.queueInputBuffer(inputBufferIndex,0,readBytes,getPTSUs(),0);
            }
        }

        // 返回一个输出缓存区句柄，当为-1时表示当前没有可用的输出缓存区
        // mBufferInfo参数包含被编码好的数据，timesOut参数为超时等待的时间
        MediaCodec.BufferInfo  mBufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = -1;
        do{
            outputBufferIndex = mAudioEncoder.dequeueOutputBuffer(mBufferInfo,TIMES_OUT);
            if(outputBufferIndex == MediaCodec. INFO_TRY_AGAIN_LATER){
                Log.i(TAG,"获得编码器输出缓存区超时");
            }else if(outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
                // 如果API小于21，APP需要重新绑定编码器的输入缓存区；
                // 如果API大于21，则无需处理INFO_OUTPUT_BUFFERS_CHANGED
                if(!isLollipop()){
                    outputBuffers = mAudioEncoder.getOutputBuffers();
                }
            }else if(outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                // 编码器输出缓存区格式改变，通常在存储数据之前且只会改变一次
                // 这里设置混合器视频轨道，如果音频已经添加则启动混合器（保证音视频同步）
                Log.i(TAG,"编码器输出缓存区格式改变，添加视频轨道到混合器");
            }else{
                // 当flag属性置为BUFFER_FLAG_CODEC_CONFIG后，说明输出缓存区的数据已经被消费了
                if((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0){
                    Log.i(TAG,"编码数据被消费，BufferInfo的size属性置0");
                    mBufferInfo.size = 0;
                }
                // 数据流结束标志，结束本次循环
                if((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                    Log.i(TAG,"数据流结束，退出循环");
                    break;
                }
                // 获取一个只读的输出缓存区inputBuffer ，它包含被编码好的数据
                ByteBuffer mBuffer = ByteBuffer.allocate(10240);
                ByteBuffer outputBuffer = null;
                if(!isLollipop()){
                    outputBuffer  = outputBuffers[outputBufferIndex];
                }else{
                    outputBuffer  = mAudioEncoder.getOutputBuffer(outputBufferIndex);
                }
                if(mBufferInfo.size != 0){
                    // 获取输出缓存区失败，抛出异常
                    if(outputBuffer == null){
                        throw new RuntimeException("encodecOutputBuffer"+outputBufferIndex+"was null");
                    }
                    // AAC流添加ADTS头，缓存到mBuffer
                    mBuffer.clear();
                    outputBuffer.get(mBuffer.array(), 7, mBufferInfo.size);
                    outputBuffer.clear();
                    mBuffer.position(7 + mBufferInfo.size);
                    addADTStoPacket(mBuffer.array(), mBufferInfo.size + 7);
                    mBuffer.flip();
                    // 将AAC回调给MainModelImpl进行push
                    if(listener != null){
                        Log.i(TAG,"----->得到aac数据流<-----");
                        listener.onEncodeResult(mBuffer.array(),0, mBufferInfo.size + 7, mBufferInfo.presentationTimeUs / 1000);
                    }
                }
                // 处理结束，释放输出缓存区资源
                mAudioEncoder.releaseOutputBuffer(outputBufferIndex,false);
            }
        }while (outputBufferIndex >= 0);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void initMediaCodec(){
        Log.d(TAG,"AACEncodeConsumer-->开始编码音频");
        MediaCodecInfo mCodecInfo = selectSupportCodec(MIME_TYPE);
        if(mCodecInfo == null){
            Log.e(TAG,"编码器不支持"+MIME_TYPE+"类型");
            return;
        }
        try{
            mAudioEncoder = MediaCodec.createByCodecName(mCodecInfo.getName());
        }catch(IOException e){
            Log.e(TAG,"创建编码器失败"+e.getMessage());
            e.printStackTrace();
        }
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
        format.setInteger(MediaFormat.KEY_BIT_RATE, encodec_bitrate);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, encodec_samplerate);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, encodec_buffer_size);
        mAudioEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mAudioEncoder.start();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void stopMediaCodec() {
        Log.d(TAG,"AACEncodeConsumer-->停止编码音频");
        if(mAudioEncoder != null){
            mAudioEncoder.stop();
            mAudioEncoder.release();
            mAudioEncoder = null;
        }
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

    public void stop(){
        isRecording = false;
    }

    public boolean isRecording(){
        return isRecording;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private MediaCodecInfo selectSupportCodec(String mimeType){
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            // 判断是否为编码器，否则直接进入下一次循环
            if (!codecInfo.isEncoder()) {
                continue;
            }
            // 如果是编码器，判断是否支持Mime类型
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    private void addADTStoPacket(byte[] packet, int packetLen) {
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF1;
        packet[2] = (byte) (((2 - 1) << 6) + (mSamplingRateIndex << 2) + (1 >> 2));
        packet[3] = (byte) (((1 & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    private short[] transferByte2Short(byte[] data,int readBytes){
        // byte[] 转 short[]，数组长度缩减一半
        int shortLen = readBytes / 2;
        // 将byte[]数组装如ByteBuffer缓冲区
        ByteBuffer byteBuffer = ByteBuffer.wrap(data, 0, readBytes);
        // 将ByteBuffer转成小端并获取shortBuffer
        ShortBuffer shortBuffer = byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        short[] shortData = new short[shortLen];
        shortBuffer.get(shortData, 0, shortLen);
        return shortData;
    }

    // 设置音频源
    public void setAudioSource(int audioSource){
        this.audioSource = audioSource;
    }

    // 设置采样率
    public void setAudioSampleRare(int sampleRateInHz){
        this.sampleRateInHz = sampleRateInHz;
    }

    // 设置音频声道
    public void setAudioChannelConfig(int channelConfig){
        this.channelConfig = channelConfig;
    }

    // 采样精度
    public void setAduioFormat(int audioFormat){
        this.audioFormat = audioFormat;
    }

    //编码输出声道数量
    public void setLameOutChannel(int outChannel){
        this.outChannel = outChannel;
    }

    //编码比特率
    public void setLameBitRate(int bitRate){
        this.bitRate = bitRate;
    }

    // 编码器比特率
    public void setMediaCodecBitRate(int encodec_bitrate){
        this.encodec_bitrate = encodec_bitrate;
    }

    // 编码器音频采样率
    public void setMediaCodecSampleRate(int encodec_samplerate){
        this.encodec_samplerate = encodec_samplerate;
    }

    // 设置模式
    public void setMode(int mode){
        this.mode = mode;
    }

    // API>=21
    private boolean isLollipop(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    private long getPTSUs(){
        long result = System.nanoTime()/1000;
        if(result < prevPresentationTimes){
            result = (prevPresentationTimes  - result ) + result;
        }
        return result;
    }
}
