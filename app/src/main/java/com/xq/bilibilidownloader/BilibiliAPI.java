package com.xq.bilibilidownloader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BilibiliAPI {

    public static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    public static final String REFERER = "https://www.bilibili.com";

    private final OkHttpClient client;

    public BilibiliAPI() {
        client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .build();
    }

    public static String extractBvid(String url) {
        if (url == null) return null;
        Pattern p = Pattern.compile("(BV[a-zA-Z0-9]{10})");
        Matcher m = p.matcher(url);
        if (m.find()) return m.group(1);
        return null;
    }

    public static int getQualityId(String label) {
        if (label == null) return 64;
        if (label.startsWith("360")) return 16;
        if (label.startsWith("480")) return 32;
        if (label.startsWith("720")) return 64;
        if (label.startsWith("1080P+")) return 112;
        if (label.startsWith("1080")) return 80;
        if (label.startsWith("4K")) return 120;
        return 64;
    }

    public static String getQualityName(int qn) {
        switch (qn) {
            case 16: return "360P";
            case 32: return "480P";
            case 64: return "720P";
            case 80: return "1080P";
            case 112: return "1080P+";
            case 120: return "4K";
            default: return qn + "P";
        }
    }

    public VideoInfo getVideoInfo(String bvid, String sessdata) throws Exception {
        String url = "https://api.bilibili.com/x/web-interface/view?bvid=" + bvid;
        Request.Builder rb = new Request.Builder().url(url)
                .header("User-Agent", UA)
                .header("Referer", REFERER);
        if (sessdata != null && !sessdata.isEmpty()) {
            rb.header("Cookie", "SESSDATA=" + sessdata);
        }

        try (Response response = client.newCall(rb.build()).execute()) {
            String body = response.body() != null ? response.body().string() : "{}";
            JSONObject json = new JSONObject(body);

            if (json.getInt("code") != 0) {
                throw new Exception(json.optString("message", "获取视频信息失败"));
            }

            JSONObject data = json.getJSONObject("data");
            VideoInfo info = new VideoInfo();
            info.bvid = bvid;
            info.aid = data.getLong("aid");
            info.cid = data.getLong("cid");
            info.title = data.getString("title");
            info.cover = data.optString("pic", "");

            JSONObject owner = data.optJSONObject("owner");
            if (owner != null) {
                info.upName = owner.optString("name", "");
            }

            return info;
        }
    }

    public PlayUrlInfo getPlayUrl(String bvid, long cid, int qn, String sessdata) throws Exception {
        String url = "https://api.bilibili.com/x/player/playurl?bvid=" + bvid
                + "&cid=" + cid + "&qn=" + qn + "&fnval=4048&fourk=1";
        Request.Builder rb = new Request.Builder().url(url)
                .header("User-Agent", UA)
                .header("Referer", REFERER);
        if (sessdata != null && !sessdata.isEmpty()) {
            rb.header("Cookie", "SESSDATA=" + sessdata);
        }

        try (Response response = client.newCall(rb.build()).execute()) {
            String body = response.body() != null ? response.body().string() : "{}";
            JSONObject json = new JSONObject(body);

            if (json.getInt("code") != 0) {
                throw new Exception(json.optString("message", "获取播放地址失败"));
            }

            JSONObject data = json.getJSONObject("data");
            PlayUrlInfo info = new PlayUrlInfo();
            info.quality = data.optInt("quality", qn);

            JSONObject dash = data.optJSONObject("dash");
            if (dash != null) {
                JSONArray videos = dash.getJSONArray("video");
                JSONArray audios = dash.optJSONArray("audio");

                for (int i = 0; i < videos.length(); i++) {
                    JSONObject v = videos.getJSONObject(i);
                    if (v.getInt("id") == info.quality) {
                        info.videoUrl = v.optString("baseUrl", v.optString("base_url", ""));
                        if (info.videoUrl.isEmpty()) {
                            info.videoUrl = v.optString("backupUrl", v.optString("backup_url", ""));
                        }
                        break;
                    }
                }
                if (info.videoUrl == null && videos.length() > 0) {
                    JSONObject v = videos.getJSONObject(0);
                    info.videoUrl = v.optString("baseUrl", v.optString("base_url", ""));
                    if (info.videoUrl.isEmpty()) {
                        info.videoUrl = v.optString("backupUrl", v.optString("backup_url", ""));
                    }
                }

                if (audios != null && audios.length() > 0) {
                    JSONObject a = audios.getJSONObject(0);
                    info.audioUrl = a.optString("baseUrl", a.optString("base_url", ""));
                    if (info.audioUrl.isEmpty()) {
                        info.audioUrl = a.optString("backupUrl", a.optString("backup_url", ""));
                    }
                }
            } else {
                JSONArray durls = data.optJSONArray("durl");
                if (durls != null && durls.length() > 0) {
                    info.videoUrl = durls.getJSONObject(0).optString("url", "");
                }
            }

            if (info.videoUrl == null || info.videoUrl.isEmpty()) {
                throw new Exception("未获取到视频流地址");
            }

            return info;
        }
    }

    public static class PlayUrlInfo {
        public int quality;
        public String videoUrl;
        public String audioUrl;

        public boolean isDash() {
            return audioUrl != null && !audioUrl.isEmpty();
        }
    }
}
