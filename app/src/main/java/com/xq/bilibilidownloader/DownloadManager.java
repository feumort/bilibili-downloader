package com.xq.bilibilidownloader;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadManager {

    private static DownloadManager instance;

    public static synchronized DownloadManager getInstance() {
        if (instance == null) {
            instance = new DownloadManager();
        }
        return instance;
    }

    private final List<DownloadTask> tasks = new ArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final CopyOnWriteArrayList<Runnable> callbacks = new CopyOnWriteArrayList<>();

    private DownloadManager() {}

    public void addCallback(Runnable r) {
        callbacks.add(r);
    }

    public void removeCallback(Runnable r) {
        callbacks.remove(r);
    }

    void notifyCallbacks() {
        for (Runnable r : callbacks) {
            r.run();
        }
    }

    public void submit(List<String> urls, String quality, String sessdata, File outputDir) {
        for (String url : urls) {
            String trimmed = url.trim();
            if (trimmed.isEmpty()) continue;

            DownloadTask task = new DownloadTask(trimmed, quality, sessdata, outputDir);
            task.setOnUpdate(this::notifyCallbacks);
            tasks.add(task);
            executor.submit(task::execute);
        }
        notifyCallbacks();
    }

    public void submitPage(String url, String quality, String sessdata, File outputDir, long cid, String part) {
        DownloadTask task = new DownloadTask(url, quality, sessdata, outputDir, cid, part);
        task.setOnUpdate(this::notifyCallbacks);
        tasks.add(task);
        executor.submit(task::execute);
        notifyCallbacks();
    }

    private void autoCleanup() {
        int completedCount = 0;
        for (DownloadTask t : tasks) {
            DownloadTask.Status s = t.getStatus();
            if (s == DownloadTask.Status.COMPLETED || s == DownloadTask.Status.ERROR) {
                completedCount++;
            }
        }
        if (completedCount > 20) {
            int toRemove = completedCount - 20;
            Iterator<DownloadTask> it = tasks.iterator();
            while (it.hasNext() && toRemove > 0) {
                DownloadTask t = it.next();
                DownloadTask.Status s = t.getStatus();
                if (s == DownloadTask.Status.COMPLETED || s == DownloadTask.Status.ERROR) {
                    it.remove();
                    toRemove--;
                }
            }
        }
    }

    public List<DownloadTask> getTasks() {
        autoCleanup();
        return tasks;
    }

    public boolean hasActiveTasks() {
        for (DownloadTask t : tasks) {
            DownloadTask.Status s = t.getStatus();
            if (s == DownloadTask.Status.PENDING || s == DownloadTask.Status.PARSING
                    || s == DownloadTask.Status.DOWNLOADING || s == DownloadTask.Status.PAUSED
                    || s == DownloadTask.Status.MERGING) {
                return true;
            }
        }
        return false;
    }

    public int getActiveCount() {
        int count = 0;
        for (DownloadTask t : tasks) {
            DownloadTask.Status s = t.getStatus();
            if (s == DownloadTask.Status.PENDING || s == DownloadTask.Status.PARSING
                    || s == DownloadTask.Status.DOWNLOADING || s == DownloadTask.Status.PAUSED
                    || s == DownloadTask.Status.MERGING) {
                count++;
            }
        }
        return count;
    }

    public void cancelTask(DownloadTask task) {
        task.cancel();
        tasks.remove(task);
        notifyCallbacks();
    }

    public void cancelAll() {
        Iterator<DownloadTask> it = tasks.iterator();
        while (it.hasNext()) {
            DownloadTask t = it.next();
            t.cancel();
            it.remove();
        }
        notifyCallbacks();
    }

    public void clearCompleted() {
        tasks.removeIf(t -> t.getStatus() == DownloadTask.Status.COMPLETED
                || t.getStatus() == DownloadTask.Status.ERROR);
        notifyCallbacks();
    }
}
