package com.zhongke.hapilorecord.activity;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.zhongke.hapilorecord.R;
import com.zhongke.hapilorecord.base.BaseActivity;
import com.zhongke.hapilorecord.dialog.CancelDialog;
import com.zhongke.hapilorecord.dialog.SaveDialog;
import com.zhongke.hapilorecord.utils.FileUtils;
import com.zhongke.hapilorecord.utils.TimeUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 录音主界面
 */
public class MainActivity extends BaseActivity implements View.OnClickListener, SaveDialog.ClickListener, CancelDialog.CancelListener {

    private static final int SAMPLE_RATE_IN_HZ = 8000;
    /**
     * AudioRecord最小的缓存size
     */
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
    private AudioRecord mAudioRecord;
    private boolean isGetVoiceRun;
    private Object mLock;

    /**
     * 记录需要合成的几段amr语音文件
     **/
    private List<String> list = new CopyOnWriteArrayList<>();
    private static final String TAG = "MainActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private String mFileName = null;

    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private MediaRecorder mRecorder = null;
    /**
     * 两个舵
     */
    private ImageView leftRudder, rightRudder;
    /**
     * 动画集合
     */
    private AnimatorSet animatorSet;
    /**
     * 按钮
     */
    private ImageView ivStart, ivPause, ivSave, ivCancel, ivFile;
    /**
     * 按钮下文本
     */
    private ImageView tvSave, tvPause, tvStart, tvCancel, tvFile;
    /**
     * 录音中...和时间
     */
    private TextView tvTitle, tvTime;
    /**
     * 录音时间
     */
    private long recordTime = 0;
    /**
     * 改变时间的线程
     */
    private Thread timeThread;
    /**
     * 线程运行标志
     */
    private boolean isRunning;
    /**
     * 点击了哪个按钮的标识
     */
    private static final int CLICK_SAVE = 1;
    private static final int CLICK_PAUSE = 2;
    private static final int CLICK_START = 3;
    private static final int CLICK_CANCEL = 4;
    private static final int CLICK_FILE = 5;
    /**
     * 是否开始录音的标志
     */
    private boolean isRecord;
    /**
     * 关闭按钮
     */
    private ImageView ivBack;
    /**
     * 左边的灯
     */
    private ImageView leftOne, leftTwo, leftThree, leftFour;
    /**
     * 右边的灯
     */
    private ImageView rightOne, rightTwo, rightThree, rightFour;
    private File file1;
    /**
     * 取消的对话框
     */
    private CancelDialog cancelDialog;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        mLock = new Object();

        initView();

//        //去除录音时发出的“滴”的声音
//        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
//        audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
//        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
//        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0);
//        audioManager.setStreamVolume(AudioManager.STREAM_DTMF, 0, 0);
//        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);
//        audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
    }


    /**
     * 设置三个线程的标志:开启或者暂停
     *
     * @param mark
     */
    public void setThreadMark(boolean mark) {
        this.isRecord = mark;
        this.isRunning = mark;
        this.isGetVoiceRun = mark;
    }


    /**
     * 通过AudioRecord来获取，声音分贝大小
     */
    public void startCalculateNoiseLevelThread() {
        if (isGetVoiceRun) {
            Log.e(TAG, "还在录着呢");
            return;
        }

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        if (mAudioRecord == null) {
            Log.e("sound", "mAudioRecord初始化失败");
        }
        isGetVoiceRun = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                mAudioRecord.startRecording();
                short[] buffer = new short[BUFFER_SIZE];
                while (isGetVoiceRun) {
                    //r是实际读取的数据长度，一般而言r会小于buffersize
                    int r = mAudioRecord.read(buffer, 0, BUFFER_SIZE);
                    long v = 0;
                    // 将 buffer 内容取出，进行平方和运算
                    for (int i = 0; i < buffer.length; i++) {
                        v += buffer[i] * buffer[i];
                    }
                    // 平方和除以数据总长度，得到音量大小。
                    double mean = v / (double) r;
                    final int volume = (int) (10 * Math.log10(mean));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateLightNumber(volume);
                            Log.e(TAG, volume + "");
                        }
                    });
                    // 大概一秒十次
                    synchronized (mLock) {
                        try {
                            mLock.wait(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            }
        }).start();
    }



    /**
     * 初始化控件
     */
    private void initView() {
        leftRudder = (ImageView) findViewById(R.id.left_rudder);
        rightRudder = (ImageView) findViewById(R.id.right_rudder);

        ivStart = (ImageView) findViewById(R.id.iv_start);
        ivPause = (ImageView) findViewById(R.id.iv_pause);
        ivSave = (ImageView) findViewById(R.id.iv_save);
        ivCancel = (ImageView) findViewById(R.id.iv_cancel);
        ivFile = (ImageView) findViewById(R.id.iv_file);

        tvSave = (ImageView) findViewById(R.id.tv_save);
        tvPause = (ImageView) findViewById(R.id.tv_pause);
        tvStart = (ImageView) findViewById(R.id.tv_start);
        tvCancel = (ImageView) findViewById(R.id.tv_cancel);
        tvFile = (ImageView) findViewById(R.id.tv_file);

        tvTitle = (TextView) findViewById(R.id.tv_title);
        tvTime = (TextView) findViewById(R.id.tv_time);

        ivBack = (ImageView) findViewById(R.id.iv_back);

        leftOne = (ImageView) findViewById(R.id.left_one);
        leftTwo = (ImageView) findViewById(R.id.left_two);
        leftThree = (ImageView) findViewById(R.id.left_three);
        leftFour = (ImageView) findViewById(R.id.left_four);

        rightOne = (ImageView) findViewById(R.id.right_one);
        rightTwo = (ImageView) findViewById(R.id.right_two);
        rightThree = (ImageView) findViewById(R.id.right_three);
        rightFour = (ImageView) findViewById(R.id.right_four);

        ivStart.setOnClickListener(this);
        ivPause.setOnClickListener(this);
        ivSave.setOnClickListener(this);
        ivCancel.setOnClickListener(this);
        ivFile.setOnClickListener(this);
        ivBack.setOnClickListener(this);
    }


    /**
     * 用于更新记录时间的UI操作
     */
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                tvTime.setText(TimeUtils.getDate(recordTime));
            }
        }
    };

    /**
     * 开始计时
     */
    private void startTimeThread() {
        isRunning = true;
        ivStart.setEnabled(false);
        timeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    try {
                        timeThread.sleep(1000);
                        recordTime += 1000;
                        runOnUiThread(runnable);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        timeThread.start();
    }

    /**
     * 停止计时
     */
    private void stopTime() {
        ivStart.setEnabled(true);
        light();
    }

    /**
     * 根据获取到的声音分贝数,更新点亮的灯光的数量
     *
     * @param decibel 分贝数
     */
    private void updateLightNumber(int decibel) {
        light();
        switch (decibel / 10) {
            case 0:
            case 1:
                light();
                break;
            case 2:
                rightOne.setVisibility(View.VISIBLE);
                leftOne.setVisibility(View.VISIBLE);
                break;
            case 3:
                rightTwo.setVisibility(View.VISIBLE);
                leftTwo.setVisibility(View.VISIBLE);
                break;
            case 4:
                rightThree.setVisibility(View.VISIBLE);
                leftThree.setVisibility(View.VISIBLE);
                break;
            case 5:
                rightFour.setVisibility(View.VISIBLE);
                leftFour.setVisibility(View.VISIBLE);
                break;
            case 6:
            case 7:
                rightFour.setVisibility(View.VISIBLE);
                leftFour.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    /**
     * 左右两边的灯光关闭
     */
    private void light() {

        leftOne.setVisibility(View.INVISIBLE);
        leftTwo.setVisibility(View.INVISIBLE);
        leftThree.setVisibility(View.INVISIBLE);
        leftFour.setVisibility(View.INVISIBLE);

        rightOne.setVisibility(View.INVISIBLE);
        rightTwo.setVisibility(View.INVISIBLE);
        rightThree.setVisibility(View.INVISIBLE);
        rightFour.setVisibility(View.INVISIBLE);
    }

    /**
     * 开始两个转盘旋转的动画
     */
    private void startAnimation() {
        tvTitle.setText("正在录音...");
        animatorSet = new AnimatorSet();
        ObjectAnimator leftAnima = ObjectAnimator.ofFloat(leftRudder, "rotation", 0f, 360f);
        ObjectAnimator rightAnima = ObjectAnimator.ofFloat(rightRudder, "rotation", 0f, 360f);
        leftAnima.setRepeatCount(ValueAnimator.INFINITE);
        rightAnima.setRepeatCount(ValueAnimator.INFINITE);
        animatorSet.playTogether(leftAnima, rightAnima);
        animatorSet.setDuration(1800);
        animatorSet.setInterpolator(new LinearInterpolator());
        animatorSet.start();
    }

    /**
     * 停止动画
     */
    private void stopAnimation() {
        if (animatorSet != null) {
            animatorSet.pause();
        }
    }


    /**
     * 开始录音
     */
    private void startMediaRecording() {
        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
        }
        tvTitle.setText("正在录音...");
        mRecorder.reset();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        mRecorder.start();
    }

    /**
     * 停止录音
     */
    private void stopMediaRecording() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }


    /**
     * 鉴权
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) finish();
    }

    /**
     * 生命周期 方法 释放资源
     */
    @Override
    public void onStop() {
        super.onStop();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

    }

    /**
     * 点击后改变控件的背景色，以及下方字体颜色
     *
     * @param current
     */
    private void clickButton(int current) {
        change();
        switch (current) {
            case CLICK_SAVE:
                ivSave.setImageResource(R.drawable.check_save);
                tvSave.setImageResource(R.drawable.check_text_save);
                break;
            case CLICK_PAUSE:
                ivPause.setImageResource(R.drawable.check_pause);
                tvPause.setImageResource(R.drawable.check_text_pause);
                break;
            case CLICK_START:
                ivStart.setImageResource(R.drawable.check_start);
                tvStart.setImageResource(R.drawable.check_text_start);
                break;
            case CLICK_CANCEL:
                ivCancel.setImageResource(R.drawable.check_cancel);
                tvCancel.setImageResource(R.drawable.check_text_cancel);
                break;
            case CLICK_FILE:
                ivFile.setImageResource(R.drawable.check_file);
                tvFile.setImageResource(R.drawable.check_text_file);
                break;
            default:
                break;
        }
    }

    /**
     * 全部未选中状态
     */
    private void change() {
        ivSave.setImageResource(R.drawable.uncheck_save);
        tvSave.setImageResource(R.drawable.uncheck_text_save);
        ivPause.setImageResource(R.drawable.uncheck_pause);
        tvPause.setImageResource(R.drawable.uncheck_text_pause);
        ivStart.setImageResource(R.drawable.uncheck_start);
        tvStart.setImageResource(R.drawable.uncheck_text_start);
        ivCancel.setImageResource(R.drawable.uncheck_cancel);
        tvCancel.setImageResource(R.drawable.uncheck_text_cancel);
        ivFile.setImageResource(R.drawable.uncheck_file);
        tvFile.setImageResource(R.drawable.uncheck_text_file);
    }

    /**
     * 点击事件
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_save://保存录音
                if (!isRecord) {
                    return;
                }
                setThreadMark(false);
                clickButton(CLICK_SAVE);
                SaveDialog saveDialog = new SaveDialog(this);
                setDialog(saveDialog);
                String mMinute1 = TimeUtils.setName(System.currentTimeMillis());
                saveDialog.setName(mMinute1);
                saveDialog.setListener(MainActivity.this);
                saveDialog.show();
                stopAnimation();
                tvTime.setText("00:00:00");
                ivStart.setEnabled(true);
                break;
            case R.id.iv_pause://暂停录音
                if (!isRecord) {
                    return;
                }
                setThreadMark(false);
                clickButton(CLICK_PAUSE);
                stopMediaRecording();
                stopAnimation();
                stopTime();
                break;
            case R.id.iv_start://开始录音
                clickButton(CLICK_START);
                mFileName = getExternalCacheDir().getAbsolutePath();
                mFileName += "/" + System.currentTimeMillis();
                list.add(mFileName);
                isRecord = true;
                //通过MediaRecorder开始录制
                startMediaRecording();
                //开启转动的动画
                startAnimation();
                //开启时间的记时
                startTimeThread();
                //根据AudioRecorder获取分贝值来更新亮灯数量
                startCalculateNoiseLevelThread();
                break;
            case R.id.iv_cancel://取消录音
                if (!isRecord) {
                    return;
                }
                // setThreadMark(false);
                clickButton(CLICK_CANCEL);
                cancelDialog = new CancelDialog(this);
                setDialog(cancelDialog);
                cancelDialog.show();
                cancelDialog.setListener(MainActivity.this);
                break;
            case R.id.iv_file://录音文件列表
                clickButton(CLICK_FILE);
                Intent intent = new Intent(MainActivity.this, FileActivity.class);
                startActivity(intent);
                break;
            case R.id.iv_back://关闭界面
                ivBack.setImageResource(R.drawable.check_shut_down);
                finish();
                break;
            default:
                break;
        }
    }

    /**
     * 将原本list中获取录音文件，合并成一个新的录音文件
     *
     * @param
     */
    public void mergeOutputFile(final List list, final String mMinute1) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 创建音频文件,合并的文件放这里
                file1 = new File(getExternalCacheDir().getAbsolutePath(), mMinute1);
                FileOutputStream fileOutputStream = null;

                if (!file1.exists()) {
                    try {
                        file1.createNewFile();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                try {
                    fileOutputStream = new FileOutputStream(file1);

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                //list里面为暂停录音 所产生的 几段录音文件的名字，中间几段文件的减去前面的6个字节头文件
                for (int i = 0; i < list.size(); i++) {
                    File file = new File((String) list.get(i));
                    Log.d("list的长度", list.size() + "");
                    try {
                        FileInputStream fileInputStream = new FileInputStream(file);
                        byte[] myByte = new byte[fileInputStream.available()];
                        //文件长度
                        int length = myByte.length;
                        //头文件
                        if (i == 0) {
                            while (fileInputStream.read(myByte) != -1) {
                                fileOutputStream.write(myByte, 0, length);
                            }
                        }
                        //之后的文件，去掉头文件就可以了
                        else {
                            while (fileInputStream.read(myByte) != -1) {

                                fileOutputStream.write(myByte, 6, length - 6);
                            }
                        }

                        fileOutputStream.flush();
                        fileInputStream.close();
                        System.out.println("合成文件长度：" + file1.length());

                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                //结束后关闭流
                try {
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                //删除原本的旧录音文件
                deleteListRecord();
            }
        }).start();
    }

    /**
     * 合成一个文件后，删除之前暂停录音所保存的零碎合成文件
     */
    private void deleteListRecord() {
        for (int i = 0; i < list.size(); i++) {
            FileUtils.deleteFileOrFileDirectory(list.get(i));
        }
        list.clear();
    }

    /**
     * 将对话框的大小按屏幕大小的百分比设置
     */
    private void setDialog(Dialog dialog) {
        Window dialogWindow = dialog.getWindow();
        WindowManager m = getWindowManager();
        Display d = m.getDefaultDisplay(); // 获取屏幕宽、高用
        WindowManager.LayoutParams p = dialogWindow.getAttributes(); // 获取对话框当前的参数值
        p.height = (int) (d.getHeight() * 0.7); // 高度设置为屏幕的0.6
        p.width = (int) (d.getWidth() * 0.65); // 宽度设置为屏幕的0.65
        dialogWindow.setAttributes(p);
    }

    /**
     * 回调传回来的录音文件的名称
     *
     * @param name
     */
    @Override
    public void clickSure(String name) {
        mergeOutputFile(list, TimeUtils.getTimeToLong(name));
        resetState();
    }

    /**
     * 重置状态
     */
    private void resetState() {
        recordTime = 0;
        tvTime.setText("00:00:00");
        tvTitle.setText("开始录音...");
        ivStart.setEnabled(true);
    }

    /**
     * 取消的dialog回调
     *
     * @param clickItem
     */
    @Override
    public void clickCancel(int clickItem) {

        if (clickItem == CancelDialog.CLICK_YES) {
            setThreadMark(false);
            deleteListRecord();
            stopAnimation();
            resetState();
        } else {
            if (isRunning) {
                clickButton(CLICK_START);
            } else {
                clickButton(CLICK_PAUSE);
            }
        }
        cancelDialog.dismiss();
    }


}
