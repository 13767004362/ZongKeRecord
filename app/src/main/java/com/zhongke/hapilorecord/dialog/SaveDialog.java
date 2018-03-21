package com.zhongke.hapilorecord.dialog;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.zhongke.hapilorecord.R;

/**
 * Created by ${tanlei} on 2017/10/24.
 */

public class SaveDialog extends Dialog {
    private TextView tvSure;
    private EditText etInput;
    private ClickListener listener;

    public SaveDialog(@NonNull Context context) {
        this(context, 0);
    }

    public void setListener(ClickListener listener) {
        this.listener = listener;
    }

    public SaveDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, R.style.DialogTheme_no_title);
        View view = LayoutInflater.from(context).inflate(R.layout.save_dialog, null);
        this.setContentView(view);
        this.setCanceledOnTouchOutside(false);
        tvSure = view.findViewById(R.id.tv_sure);
        etInput = view.findViewById(R.id.et_name);
        tvSure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.clickSure(etInput.getText().toString());
                SaveDialog.this.dismiss();
            }
        });
    }

    public void setName(String str) {
        etInput.setText(str);
    }

    public interface ClickListener {
        void clickSure(String name);
    }
}
