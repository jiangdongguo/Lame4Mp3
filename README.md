# Lame4Mp3
** 使用lame库封装mp3文件 **

**1. 添加依赖**  

(1) 在工程build.gradle中添加
```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
}
```
  
(2) 在module的gradle中添加
```
dependencies {
	  compile 'com.github.jiangdongguo:Lame4Mp3:v1.0.0'
}
```  
**2. 使用方法**  

(1) 配置参数
```  
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
   //  Mp3Recorder.MODE_AAC 仅编码得到AAC数据流
   //  Mp3Recorder.MODE_MP3 仅编码得到Mp3文件
   //  Mp3Recorder.MODE_BOTH 同时编码
   mMp3Recorder.setMode(Mp3Recorder.MODE_BOTH);
```  
(2) 开始编码
```
   mMp3Recorder.start(filePath, fileName, new Mp3Recorder.OnAACStreamResultListener() {
       @Override
       public void onEncodeResult(byte[] data, int offset, int length, long timestamp) {
              Log.i("MainActivity","acc数据流长度："+data.length);
          }
       });
```  
(3) 停止编码
```
    mMp3Recorder.stop();
```
