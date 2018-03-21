package com.zhongke.hapilorecord.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.zhongke.hapilorecord.Bean.FileBean;
import com.zhongke.hapilorecord.R;

import java.util.List;

/**
 * Created by ${tanlei} on 2017/10/20.
 */

public class FileAdapter extends BaseAdapter {
    private List<FileBean> list;
    private Context context;
    private PlayListeners listener;

    public FileAdapter(List<FileBean> list, Context context) {
        this.list = list;
        this.context = context;
    }
    public void setListener(PlayListeners listener) {
        this.listener = listener;
    }
    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (null == convertView) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.adapter_file, null);
            holder.ivButton = convertView.findViewById(R.id.play_or_pause);
            holder.tvName = convertView.findViewById(R.id.tv_file_name);
            holder.tvPlay = convertView.findViewById(R.id.play_time);
            holder.tvTotal = convertView.findViewById(R.id.tote_time);
            holder.ivButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.playOrPause(position,v);
                }
            });
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        FileBean fb = (FileBean) getItem(position);
        holder.tvName.setText(fb.getName());
        holder.tvPlay.setText(fb.getPlayTime());
        holder.tvTotal.setText(fb.getTotal());
        if (fb.isRunning()) {
            holder.ivButton.setImageResource(R.mipmap.file_pause);
        } else {
            holder.ivButton.setImageResource(R.mipmap.file_play);
        }
        return convertView;
    }

    public static class ViewHolder {
        private ImageView ivButton;
        private TextView tvName, tvTotal, tvPlay;
    }

    /**
     * 播放的监听器
     */
    public interface PlayListeners {
        //播放或者暂停
        void playOrPause(int position,View view);
    }
}
