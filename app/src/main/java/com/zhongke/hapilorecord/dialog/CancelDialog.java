package com.zhongke.hapilorecord.dialog;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.zhongke.hapilorecord.R;

/**
 * Created by ${tanlei} on 2017/10/24.
 */

public class CancelDialog extends Dialog implements View.OnClickListener {
    private TextView tvYes, tvNo;
    public static final int CLICK_YES = 0;
    public static final int CLICK_NO = 1;
    private CancelListener listener;

    public void setListener(CancelListener listener) {
        this.listener = listener;
    }

    public CancelDialog(@NonNull Context context) {
        this(context, 0);
    }

    public CancelDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, R.style.DialogTheme_no_title);
        View view = LayoutInflater.from(context).inflate(R.layout.cancel_dialog, null);
        this.setContentView(view);
        this.setCanceledOnTouchOutside(false);
        tvYes = view.findViewById(R.id.tv_yes);
        tvNo = view.findViewById(R.id.tv_no);
        tvYes.setOnClickListener(this);
        tvNo.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_yes) {
            listener.clickCancel(CLICK_YES);
        } else {
            listener.clickCancel(CLICK_NO);
        }
    }

    public interface CancelListener {
        void clickCancel(int clickItem);
    }
}
