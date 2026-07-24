package com.xq.bilibilidownloader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadManager {

    private final List<DownloadTask> tasks = new ArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private Runnable onUpdateCallback;

    public void setOnUpdate(Runnable callback) {
        this.onUpdateCallback = callback;
    }

    public void submit(List<String> urls, String quality, String sessdata, File outputDir) {
        for (String url : urls) {
            String trimmed = url.trim();
            if (trimmed.isEmpty()) continue;

            DownloadTask task = new DownloadTask(trimmed, quality, sessdata, outputDir);
            task.setOnUpdate(() -> {
                if (onUpdateCallback != null) onUpdateCallback.run();
            });
            tasks.add(task);
            executor.submit(task::execute);
        }
        if (onUpdateCallback != null) onUpdateCallback.run();
    }

    public List<DownloadTask> getTasks() {
        return tasks;
    }

    public void clearCompleted() {
        tasks.removeIf(t -> t.getStatus() == DownloadTask.Status.COMPLETED
                || t.getStatus() == DownloadTask.Status.ERROR);
        if (onUpdateCallback != null) onUpdateCallback.run();
    }
}
