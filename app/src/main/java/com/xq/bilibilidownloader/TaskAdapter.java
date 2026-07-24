package com.xq.bilibilidownloader;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private List<DownloadTask> tasks = new ArrayList<>();
    private OnCancelListener cancelListener;

    public interface OnCancelListener {
        void onCancel(DownloadTask task);
    }

    public void setOnCancelListener(OnCancelListener listener) {
        this.cancelListener = listener;
    }

    public void updateTasks(List<DownloadTask> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DownloadTask task = tasks.get(position);

        holder.title.setText(task.getDisplayTitle());
        holder.url.setText(task.getUrl());

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
        holder.status.setText(statusText);
        holder.status.setTextColor(statusColor);

        boolean canCancel = task.getStatus() == DownloadTask.Status.PENDING
                || task.getStatus() == DownloadTask.Status.PARSING
                || task.getStatus() == DownloadTask.Status.DOWNLOADING
                || task.getStatus() == DownloadTask.Status.MERGING;
        holder.btnCancel.setVisibility(canCancel ? View.VISIBLE : View.GONE);

        holder.btnCancel.setOnClickListener(v -> {
            if (cancelListener != null) {
                cancelListener.onCancel(task);
            }
        });

        if (task.getStatus() == DownloadTask.Status.DOWNLOADING
                || task.getStatus() == DownloadTask.Status.MERGING
                || task.getStatus() == DownloadTask.Status.PARSING) {
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.progressText.setVisibility(View.VISIBLE);
            holder.progressBar.setProgress(task.getProgress());
            String pText = task.getProgress() + "%";
            if (!task.getSpeedText().isEmpty()) {
                pText += " · " + task.getSpeedText();
            }
            holder.progressText.setText(pText);
        } else {
            holder.progressBar.setVisibility(View.GONE);
            holder.progressText.setVisibility(View.GONE);
        }

        if (task.getStatus() == DownloadTask.Status.ERROR && task.getErrorMsg() != null) {
            holder.errorText.setVisibility(View.VISIBLE);
            holder.errorText.setText(task.getErrorMsg());
        } else {
            holder.errorText.setVisibility(View.GONE);
        }

        if (task.getStatus() == DownloadTask.Status.COMPLETED && task.getSavedFilename() != null) {
            holder.filenameText.setVisibility(View.VISIBLE);
            holder.filenameText.setText("已保存: " + task.getSavedFilename());
        } else {
            holder.filenameText.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, url, status, progressText, errorText, filenameText;
        ProgressBar progressBar;
        Button btnCancel;

        ViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.taskTitle);
            url = v.findViewById(R.id.taskUrl);
            status = v.findViewById(R.id.taskStatus);
            progressText = v.findViewById(R.id.taskProgress);
            progressBar = v.findViewById(R.id.taskProgressBar);
            errorText = v.findViewById(R.id.taskError);
            filenameText = v.findViewById(R.id.taskFilename);
            btnCancel = v.findViewById(R.id.btnCancel);
        }
    }
}
