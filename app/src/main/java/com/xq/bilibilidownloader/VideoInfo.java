package com.xq.bilibilidownloader;

import java.util.ArrayList;
import java.util.List;

public class VideoInfo {
    public String bvid;
    public long aid;
    public long cid;
    public String title;
    public String upName;
    public String cover;
    public List<Page> pages = new ArrayList<>();

    public static class Page {
        public int page;
        public long cid;
        public String part;
    }
}
