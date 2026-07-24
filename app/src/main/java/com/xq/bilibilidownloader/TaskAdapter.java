package com.xq.bilibilidownloader;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

public class TaskAdapter {

    private LinearLayout container;
    private List<DownloadTask> tasks;
    private OnCancelListener cancelListener;
    private OnPauseListener pauseListener;

    public interface OnCancelListener {
        void onCancel(DownloadTask task);
    }

    public interface OnPauseListener {
        void onPause(DownloadTask task);
    }

    public void setContainer(LinearLayout container) {
        this.container = container;
    }

    public void setOnCancelListener(OnCancelListener listener) {
        this.cancelListener = listener;
    }

    public void setOnPauseListener(OnPauseListener listener) {
        this.pauseListener = listener;
    }

    public void updateTasks(List<DownloadTask> newTasks) {
        this.tasks = newTasks;

        if (container.getChildCount() != tasks.size()) {
            container.removeAllViews();
            for (int i = 0; i < tasks.size(); i++) {
                View v = LayoutInflater.from(container.getContext())
                        .inflate(R.layout.item_task, container, false);
                container.addView(v);
            }
        }

        for (int i = 0; i < tasks.size(); i++) {
            bindView(container.getChildAt(i), tasks.get(i));
        }
    }

    private void bindView(View v, DownloadTask task) {
        TextView title = v.findViewById(R.id.taskTitle);
        TextView url = v.findViewById(R.id.taskUrl);
        TextView status = v.findViewById(R.id.taskStatus);
        TextView progressText = v.findViewById(R.id.taskProgress);
        ProgressBar progressBar = v.findViewById(R.id.taskProgressBar);
        TextView errorText = v.findViewById(R.id.taskError);
        TextView filenameText = v.findViewById(R.id.taskFilename);
        Button btnCancel = v.findViewById(R.id.btnCancel);
        Button btnPause = v.findViewById(R.id.btnPause);

        title.setText(task.getDisplayTitle());
        url.setText(task.getUrl());

        String statusText;
        int statusColor;
        switch (task.getStatus()) {
            case PENDING:
                statusText = "等待中";
                statusColor = 0xFF888888;
                break;
            case PARSING:
                statusText = "解析中";
                statusColor = 0xFF2196F3;
                break;
            case DOWNLOADING:
                statusText = "下载中";
                statusColor = 0xFF4CAF50;
                break;
            case PAUSED:
                statusText = "已暂停";
                statusColor = 0xFFFF9800;
                break;
            case MERGING:
                statusText = "合并中";
                statusColor = 0xFFFF9800;
                break;
            case COMPLETED:
                statusText = "已完成";
                statusColor = 0xFF4CAF50;
                break;
            case ERROR:
                statusText = "失败";
                statusColor = 0xFFF44336;
                break;
            default:
                statusText = task.getStatus().name();
                statusColor = 0xFF888888;
        }

        if (!task.getQualityText().isEmpty()
                && (task.getStatus() == DownloadTask.Status.DOWNLOADING
                || task.getStatus() == DownloadTask.Status.MERGING
                || task.getStatus() == DownloadTask.Status.COMPLETED)) {
            statusText += " · " + task.getQualityText();
        }
        status.setText(statusText);
        status.setTextColor(statusColor);

        boolean canCancel = task.getStatus() == DownloadTask.Status.PENDING
                || task.getStatus() == DownloadTask.Status.PARSING
                || task.getStatus() == DownloadTask.Status.DOWNLOADING
                || task.getStatus() == DownloadTask.Status.PAUSED
                || task.getStatus() == DownloadTask.Status.MERGING;
        btnCancel.setVisibility(canCancel ? View.VISIBLE : View.GONE);

        btnCancel.setOnClickListener(btn -> {
            if (cancelListener != null) {
                cancelListener.onCancel(task);
            }
        });

        boolean canPause = task.getStatus() == DownloadTask.Status.DOWNLOADING
                || task.getStatus() == DownloadTask.Status.PAUSED;
        btnPause.setVisibility(canPause ? View.VISIBLE : View.GONE);
        btnPause.setText(task.getStatus() == DownloadTask.Status.PAUSED ? "继续" : "暂停");

        btnPause.setOnClickListener(btn -> {
            if (pauseListener != null) {
                pauseListener.onPause(task);
            }
        });

        if (task.getStatus() == DownloadTask.Status.DOWNLOADING
                || task.getStatus() == DownloadTask.Status.MERGING
                || task.getStatus() == DownloadTask.Status.PARSING
                || task.getStatus() == DownloadTask.Status.PAUSED) {
            progressBar.setVisibility(View.VISIBLE);
            progressText.setVisibility(View.VISIBLE);
            progressBar.setProgress(task.getProgress());
            String pText = task.getProgress() + "%";
            if (!task.getSpeedText().isEmpty()) {
                pText += " · " + task.getSpeedText();
            }
            progressText.setText(pText);
        } else {
            progressBar.setVisibility(View.GONE);
            progressText.setVisibility(View.GONE);
        }

        if (task.getStatus() == DownloadTask.Status.ERROR && task.getErrorMsg() != null) {
            errorText.setVisibility(View.VISIBLE);
            errorText.setText(task.getErrorMsg());
        } else {
            errorText.setVisibility(View.GONE);
        }

        if (task.getStatus() == DownloadTask.Status.COMPLETED && task.getSavedFilename() != null) {
            filenameText.setVisibility(View.VISIBLE);
            filenameText.setText("已保存: " + task.getSavedFilename());
        } else {
            filenameText.setVisibility(View.GONE);
        }
    }
}
