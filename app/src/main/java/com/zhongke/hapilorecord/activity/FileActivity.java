package com.zhongke.hapilorecord.activity;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

import com.zhongke.hapilorecord.Bean.FileBean;
import com.zhongke.hapilorecord.R;
import com.zhongke.hapilorecord.adapter.FileAdapter;
import com.zhongke.hapilorecord.base.BaseActivity;
import com.zhongke.hapilorecord.utils.FileUtils;
import com.zhongke.hapilorecord.utils.TimeUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 录音文件列表界面
 */
public class FileActivity extends BaseActivity implements View.OnClickListener, FileAdapter.PlayListeners {
    private ListView listFileListView;
    private ImageView ivBack;
    private FileAdapter adapter;
    private List<FileBean> list = new ArrayList<>();
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private static final String TAG = "FileActivity";
    private int currentPosition = -1;
    /**
     * 是否在播放标志
     */
    private boolean isPlaying;
    /**
     * 播放时间
     */
    private int playTime;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_file;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        ivBack = (ImageView) findViewById(R.id.iv_file_back);
        listFileListView = (ListView) findViewById(R.id.list_file);
        adapter = new FileAdapter(list, FileActivity.this);
        listFileListView.setAdapter(adapter);
        startQueryThread();
        setListener();
    }

    /**
     * 开启查询录音文件的线程
     */
    private void startQueryThread() {

        File file = new File(getExternalCacheDir().getAbsolutePath());
        File[] files = file.listFiles();
        for (int i = 0; i < files.length; i++) {
            FileBean fb = new FileBean();
            fb.setRunning(false);
            fb.setName(files[i].getName());
            try {
                fb.setTotal(TimeUtils.getDate(FileUtils.getAmrDuration(files[i])));
            } catch (IOException e) {
                e.printStackTrace();
            }
            fb.setPlayTime("00:00:00");
            fb.setPath(files[i].getPath());
            list.add(fb);
        }
        // 倒序排列
        Collections.reverse(list);
        handler.sendEmptyMessage(1);
    }

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 1) {
                adapter.notifyDataSetChanged();
            }
            return false;
        }
    });

    private void setListener() {
        ivBack.setOnClickListener(this);
        adapter.setListener(FileActivity.this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_file_back) {
            finish();
        }
    }

    @Override
    public void playOrPause(final int position, View view) {
        stopChronographThread();
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setRunning(false);
            if (position != i) {
                list.get(i).setPlayTime("00:00:00");
            }
        }
        list.get(position).setRunning(true);

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                isPlaying = false;
                playTime = 0;
//                stopPlaying();
                list.get(position).setPlayTime("00:00:00");
                list.get(position).setRunning(false);
                adapter.notifyDataSetChanged();
            }
        });
        if (position != currentPosition) {
            mediaPlayer.reset();
            startPlaying(list.get(position).getPath());
            if (currentPosition != -1) {
                list.get(currentPosition).setPlayTime("00:00:00");
            }
            playTime = 0;
            isPlaying = true;
            startChronographThread(position);
        } else {
            if (mediaPlayer.isPlaying()) {
                pausePlaying();
                isPlaying = false;
                list.get(position).setRunning(false);
            } else {
                startChronographThread(position);
                isPlaying = true;
                list.get(position).setPlayTime(TimeUtils.getDate(playTime));
                list.get(position).setRunning(true);
                mediaPlayer.seekTo(playTime);
                mediaPlayer.start();
            }
        }
        currentPosition = position;
        adapter.notifyDataSetChanged();
    }

    private Thread currentThread;

    /**
     * 停止计时线程
     */
    private void stopChronographThread() {
        try {
            isPlaying = false;
            if (currentThread != null) {
                currentThread.interrupt();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 一秒
     */
    private  final int millis=1000;
    /**
     * 开启记时线程
     *
     * @param position
     */
    private void startChronographThread(final int position) {
        currentThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isPlaying) {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                list.get(position).setPlayTime(TimeUtils.getDate(playTime));
                                adapter.notifyDataSetChanged();
                            }
                        });
                        playTime += millis;
                        Thread.sleep(millis);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        currentThread.start();
    }


    /**
     * 开始播放录音
     */
    private void startPlaying(String file1) {
        try {
            String s = String.valueOf(file1);
            mediaPlayer.setDataSource(s);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * 停止播放录音
     */
    private void stopPlaying() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    /**
     * 暂停播放录音
     */
    private void pausePlaying() {
        mediaPlayer.pause();
    }

    @Override
    protected void onStop() {
        super.onStop();
           stopPlaying();
    }
}
