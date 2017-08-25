package com.jiangdg.lametomp3;

/** JNI调用lame库实现mp3文件封装
 * Created by Jiangdg on 2017/6/9.
 */

public class LameMp3 {
    static {
        System.loadLibrary("LameMp3");
    }

    /** 初始化lame库，配置相关信息
     *
     * @param inSampleRate pcm格式音频采样率
     * @param outChannel pcm格式音频通道数量
     * @param outSampleRate mp3格式音频采样率
     * @param outBitRate mp3格式音频比特率
     * @param quality mp3格式音频质量，0~9，最慢最差~最快最好
     */
    public native static void lameInit(int inSampleRate, int outChannel,int outSampleRate, int outBitRate, int quality);

    /** 编码pcm成mp3格式
     *
     * @param letftBuf  左pcm数据
     * @param rightBuf 右pcm数据，如果是单声道，则一致
     * @param sampleRate 读入的pcm字节大小
     * @param mp3Buf 存放mp3数据缓存
     * @return 编码数据字节长度
     */
    public native static int lameEncode(short[] letftBuf, short[] rightBuf,int sampleRate, byte[] mp3Buf);

    public native static int lameEncodeByByte(byte[] letftBuf, byte[] rightBuf,int sampleRate, byte[] mp3Buf);

    /** 保存mp3音频流到文件
     *
     * @param mp3buf mp3数据流
     * @return 数据流长度rty
     */
    public native static int lameFlush(byte[] mp3buf);

    /**
     * 释放lame库资源
     */
    public native static void lameClose();
}
