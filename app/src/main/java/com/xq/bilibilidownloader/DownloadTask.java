package com.xq.bilibilidownloader;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask {

    public enum Status { PENDING, PARSING, DOWNLOADING, MERGING, COMPLETED, ERROR }

    private final String url;
    private final String qualityLabel;
    private final String sessdata;
    private final File outputDir;
    private final long specifyCid;
    private final String specifyPart;

    private VideoInfo videoInfo;
    private Status status = Status.PENDING;
    private int progress = 0;
    private String speedText = "";
    private String errorMsg;
    private String savedFilename;
    private String qualityText = "";

    private final BilibiliAPI api;
    private final OkHttpClient downloadClient;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private Runnable onUpdateCallback;

    private volatile boolean cancelled = false;
    private long lastUpdateTime = 0;

    public DownloadTask(String url, String qualityLabel, String sessdata, File outputDir) {
        this(url, qualityLabel, sessdata, outputDir, 0, null);
    }

    public DownloadTask(String url, String qualityLabel, String sessdata, File outputDir, long cid, String part) {
        this.url = url;
        this.qualityLabel = qualityLabel;
        this.sessdata = sessdata;
        this.outputDir = outputDir;
        this.specifyCid = cid;
        this.specifyPart = part;

        this.api = new BilibiliAPI();
        this.downloadClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .followRedirects(true)
                .build();
    }

    public void setOnUpdate(Runnable callback) {
        this.onUpdateCallback = callback;
    }

    public void cancel() {
        cancelled = true;
    }

    public void execute() {
        try {
            status = Status.PARSING;
            progress = 5;
            notifyUpdate();

            String bvid = api.resolveBvid(url);
            if (bvid == null) {
                throw new Exception("无法解析视频链接，请检查URL");
            }

            videoInfo = api.getVideoInfo(bvid, sessdata);

            long cid = specifyCid > 0 ? specifyCid : videoInfo.cid;
            String partName = specifyPart != null ? specifyPart : videoInfo.title;

            if (videoInfo.pages.size() > 1 && specifyPart != null) {
                savedFilename = sanitizeFilename(videoInfo.title) + "_P" + findPageNumber(cid) + "_" + sanitizeFilename(partName) + ".mp4";
            } else {
                savedFilename = sanitizeFilename(videoInfo.title) + ".mp4";
            }
            progress = 10;
            notifyUpdate();

            int qn = BilibiliAPI.getQualityId(qualityLabel);
            BilibiliAPI.PlayUrlInfo playInfo = api.getPlayUrl(bvid, cid, qn, sessdata);
            qualityText = BilibiliAPI.getQualityName(playInfo.quality);
            progress = 15;
            notifyUpdate();

            status = Status.DOWNLOADING;
            notifyUpdate();

            outputDir.mkdirs();
            File tempDir = new File(outputDir, ".temp");
            tempDir.mkdirs();
            File outputFile = new File(outputDir, savedFilename);
            String tempSuffix = cid + "";
            File videoTemp = new File(tempDir, bvid + "_" + tempSuffix + "_video.m4s");
            File audioTemp = new File(tempDir, bvid + "_" + tempSuffix + "_audio.m4s");

            downloadFile(playInfo.videoUrl, videoTemp, 15, 55);

            if (playInfo.isDash()) {
                downloadFile(playInfo.audioUrl, audioTemp, 55, 85);
            }

            status = Status.MERGING;
            progress = 85;
            notifyUpdate();

            if (playInfo.isDash()) {
                mergeVideoAudio(videoTemp, audioTemp, outputFile);
            } else {
                copyFile(videoTemp, outputFile);
            }

            videoTemp.delete();
            audioTemp.delete();

            status = Status.COMPLETED;
            progress = 100;
            speedText = "";
            notifyUpdate();

        } catch (Exception e) {
            status = Status.ERROR;
            if (cancelled) {
                errorMsg = "已取消";
            } else {
                errorMsg = e.getMessage() != null ? e.getMessage() : "未知错误";
            }
            notifyUpdate();
        }
    }

    private int findPageNumber(long cid) {
        if (videoInfo != null && videoInfo.pages != null) {
            for (VideoInfo.Page p : videoInfo.pages) {
                if (p.cid == cid) return p.page;
            }
        }
        return 1;
    }

    private void downloadFile(String fileUrl, File dest, int progressStart, int progressEnd) throws Exception {
        Request request = new Request.Builder()
                .url(fileUrl)
                .header("User-Agent", BilibiliAPI.UA)
                .header("Referer", BilibiliAPI.REFERER)
                .build();

        try (Response response = downloadClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("下载失败: HTTP " + response.code());
            }

            long contentLength = response.body() != null ? response.body().contentLength() : -1;
            InputStream is = response.body() != null ? response.body().byteStream() : null;
            if (is == null) throw new Exception("下载失败: 无响应体");

            FileOutputStream fos = new FileOutputStream(dest);
            byte[] buffer = new byte[8192];
            long totalRead = 0;
            long startTime = System.currentTimeMillis();
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                if (cancelled) {
                    fos.close();
                    is.close();
                    throw new Exception("已取消");
                }
                fos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;

                long now = System.currentTimeMillis();
                if (now - lastUpdateTime > 400) {
                    lastUpdateTime = now;
                    if (contentLength > 0) {
                        progress = (int) (progressStart + (totalRead * 1.0 / contentLength) * (progressEnd - progressStart));
                    }
                    long elapsed = (now - startTime) / 1000;
                    if (elapsed > 0) {
                        speedText = formatSpeed(totalRead / elapsed);
                    }
                    notifyUpdate();
                }
            }

            fos.close();
            is.close();
        }
    }

    private void mergeVideoAudio(File videoFile, File audioFile, File outputFile) throws Exception {
        MediaExtractor videoExtractor = new MediaExtractor();
        videoExtractor.setDataSource(videoFile.getAbsolutePath());
        int videoTrack = getTrackIndex(videoExtractor, "video/");
        if (videoTrack < 0) throw new Exception("无法读取视频轨");
        videoExtractor.selectTrack(videoTrack);

        MediaExtractor audioExtractor = new MediaExtractor();
        audioExtractor.setDataSource(audioFile.getAbsolutePath());
        int audioTrack = getTrackIndex(audioExtractor, "audio/");
        if (audioTrack < 0) throw new Exception("无法读取音频轨");
        audioExtractor.selectTrack(audioTrack);

        MediaMuxer muxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        int muxerVideoTrack = muxer.addTrack(videoExtractor.getTrackFormat(videoTrack));
        int muxerAudioTrack = muxer.addTrack(audioExtractor.getTrackFormat(audioTrack));
        muxer.start();

        ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        while (!cancelled) {
            int sampleSize = videoExtractor.readSampleData(buffer, 0);
            if (sampleSize < 0) break;
            info.offset = 0;
            info.size = sampleSize;
            info.flags = videoExtractor.getSampleFlags();
            info.presentationTimeUs = videoExtractor.getSampleTime();
            muxer.writeSampleData(muxerVideoTrack, buffer, info);
            videoExtractor.advance();
        }

        while (!cancelled) {
            int sampleSize = audioExtractor.readSampleData(buffer, 0);
            if (sampleSize < 0) break;
            info.offset = 0;
            info.size = sampleSize;
            info.flags = audioExtractor.getSampleFlags();
            info.presentationTimeUs = audioExtractor.getSampleTime();
            muxer.writeSampleData(muxerAudioTrack, buffer, info);
            audioExtractor.advance();
        }

        muxer.stop();
        muxer.release();
        videoExtractor.release();
        audioExtractor.release();

        if (cancelled) throw new Exception("已取消");
    }

    private int getTrackIndex(MediaExtractor extractor, String mimePrefix) {
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith(mimePrefix)) {
                return i;
            }
        }
        return -1;
    }

    private void copyFile(File src, File dest) throws IOException {
        java.io.FileInputStream fis = new java.io.FileInputStream(src);
        java.io.FileOutputStream fos = new java.io.FileOutputStream(dest);
        byte[] buffer = new byte[8192];
        int len;
        while ((len = fis.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
        }
        fos.close();
        fis.close();
    }

    private String sanitizeFilename(String name) {
        if (name == null) return "video";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private String formatSpeed(long bytesPerSec) {
        if (bytesPerSec < 1024) return bytesPerSec + " B/s";
        if (bytesPerSec < 1024 * 1024) return String.format("%.1f KB/s", bytesPerSec / 1024.0);
        return String.format("%.1f MB/s", bytesPerSec / 1024.0 / 1024.0);
    }

    private void notifyUpdate() {
        if (onUpdateCallback != null) {
            uiHandler.post(onUpdateCallback);
        }
    }

    public String getUrl() { return url; }
    public Status getStatus() { return status; }
    public int getProgress() { return progress; }
    public String getSpeedText() { return speedText; }
    public String getErrorMsg() { return errorMsg; }
    public String getSavedFilename() { return savedFilename; }
    public String getQualityText() { return qualityText; }
    public VideoInfo getVideoInfo() { return videoInfo; }

    public String getDisplayTitle() {
        if (specifyPart != null && videoInfo != null && videoInfo.title != null) {
            return videoInfo.title + " - " + specifyPart;
        }
        if (videoInfo != null && videoInfo.title != null) {
            return videoInfo.title;
        }
        return url;
    }
}
