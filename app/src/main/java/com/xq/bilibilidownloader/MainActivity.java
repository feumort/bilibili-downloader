package com.xq.bilibilidownloader;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private EditText urlInput;
    private Spinner qualitySpinner;
    private Button downloadBtn;
    private Button clearBtn;
    private RecyclerView taskList;
    private TextView emptyText;
    private TextView savePathText;
    private ScrollView scrollView;

    private DownloadManager downloadManager;
    private TaskAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        urlInput = findViewById(R.id.urlInput);
        qualitySpinner = findViewById(R.id.qualitySpinner);
        downloadBtn = findViewById(R.id.downloadBtn);
        clearBtn = findViewById(R.id.clearBtn);
        taskList = findViewById(R.id.taskList);
        emptyText = findViewById(R.id.emptyText);
        savePathText = findViewById(R.id.savePathText);
        scrollView = findViewById(R.id.scrollView);

        String[] qualities = {"360P 流畅", "480P 标清", "720P 高清", "1080P 超清"};
        ArrayAdapter<String> qAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, qualities);
        qualitySpinner.setAdapter(qAdapter);
        qualitySpinner.setSelection(2);

        taskList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter();
        taskList.setAdapter(adapter);
        taskList.setNestedScrollingEnabled(false);
        adapter.setOnCancelListener(task -> downloadManager.cancelTask(task));

        downloadManager = new DownloadManager();
        downloadManager.setOnUpdate(this::refreshTaskList);

        File saveDir = getSaveDir();
        savePathText.setText("保存位置: " + saveDir.getAbsolutePath());

        downloadBtn.setOnClickListener(v -> startDownload());
        clearBtn.setOnClickListener(v -> downloadManager.clearCompleted());

        if (!checkPermission()) {
            requestPermission();
        }
    }

    private File getSaveDir() {
        return new File(Environment.getExternalStorageDirectory(), "b站下载视频");
    }

    private void startDownload() {
        String urls = urlInput.getText().toString().trim();
        if (urls.isEmpty()) {
            Toast.makeText(this, "请输入视频链接", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!checkPermission()) {
            requestPermission();
            return;
        }

        String quality = qualitySpinner.getSelectedItem().toString();
        String sessdata = BilibiliAPI.DEFAULT_SESSDATA;

        List<String> urlList = Arrays.asList(urls.split("\n"));
        int validCount = 0;
        for (String u : urlList) {
            if (!u.trim().isEmpty()) validCount++;
        }

        if (validCount == 1) {
            String singleUrl = null;
            for (String u : urlList) {
                if (!u.trim().isEmpty()) {
                    singleUrl = u.trim();
                    break;
                }
            }
            parseAndDownload(singleUrl, quality, sessdata);
        } else {
            downloadManager.submit(urlList, quality, sessdata, getSaveDir());
            urlInput.setText("");
            Toast.makeText(this, "已添加 " + validCount + " 个下载任务", Toast.LENGTH_SHORT).show();
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        }
    }

    private void parseAndDownload(String url, String quality, String sessdata) {
        downloadBtn.setEnabled(false);
        downloadBtn.setText("解析中...");

        new Thread(() -> {
            try {
                BilibiliAPI api = new BilibiliAPI();
                String bvid = api.resolveBvid(url);
                if (bvid == null) {
                    runOnUiThread(() -> {
                        downloadBtn.setEnabled(true);
                        downloadBtn.setText("开始下载");
                        Toast.makeText(this, "无法解析视频链接，请检查URL", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                VideoInfo info = api.getVideoInfo(bvid, sessdata);

                runOnUiThread(() -> {
                    downloadBtn.setEnabled(true);
                    downloadBtn.setText("开始下载");
                    if (info.pages.size() > 1) {
                        showPageSelectionDialog(url, quality, sessdata, info);
                    } else {
                        downloadManager.submit(Arrays.asList(url), quality, sessdata, getSaveDir());
                        urlInput.setText("");
                        Toast.makeText(this, "已添加下载任务", Toast.LENGTH_SHORT).show();
                        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    downloadBtn.setEnabled(true);
                    downloadBtn.setText("开始下载");
                    Toast.makeText(this, "解析失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void showPageSelectionDialog(String url, String quality, String sessdata, VideoInfo info) {
        boolean[] checked = new boolean[info.pages.size()];
        for (int i = 0; i < checked.length; i++) checked[i] = false;

        String[] labels = new String[info.pages.size()];
        for (int i = 0; i < info.pages.size(); i++) {
            VideoInfo.Page p = info.pages.get(i);
            labels[i] = "P" + p.page + " " + p.part;
        }

        ScrollView dialogScroll = new ScrollView(this);
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(50, 30, 50, 30);

        List<CheckBox> checkboxes = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            CheckBox cb = new CheckBox(this);
            cb.setText(labels[i]);
            cb.setChecked(false);
            cb.setTextSize(14);
            final int idx = i;
            cb.setOnCheckedChangeListener((button, isChecked) -> checked[idx] = isChecked);
            checkboxes.add(cb);
            dialogLayout.addView(cb);
        }

        dialogScroll.addView(dialogLayout);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(info.title + "（共" + info.pages.size() + "P）");
        builder.setView(dialogScroll);

        builder.setNeutralButton("全选", null);
        builder.setPositiveButton("下载选中", null);
        builder.setNegativeButton("取消", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            boolean allChecked = true;
            for (CheckBox cb : checkboxes) {
                if (!cb.isChecked()) {
                    allChecked = false;
                    break;
                }
            }
            for (int i = 0; i < checkboxes.size(); i++) {
                checkboxes.get(i).setChecked(!allChecked);
                checked[i] = !allChecked;
            }
            ((Button) v).setText(allChecked ? "全选" : "取消全选");
        });

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int selectedCount = 0;
            for (boolean b : checked) if (b) selectedCount++;

            if (selectedCount == 0) {
                Toast.makeText(this, "请至少选择一个分P", Toast.LENGTH_SHORT).show();
                return;
            }

            for (int i = 0; i < info.pages.size(); i++) {
                if (checked[i]) {
                    VideoInfo.Page p = info.pages.get(i);
                    downloadManager.submitPage(url, quality, sessdata, getSaveDir(), p.cid, p.part);
                }
            }

            urlInput.setText("");
            Toast.makeText(this, "已添加 " + selectedCount + " 个下载任务", Toast.LENGTH_SHORT).show();
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
            dialog.dismiss();
        });
    }

    private void refreshTaskList() {
        List<DownloadTask> tasks = downloadManager.getTasks();
        adapter.updateTasks(tasks);

        if (tasks.isEmpty()) {
            taskList.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
            clearBtn.setVisibility(View.GONE);
        } else {
            taskList.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
            clearBtn.setVisibility(View.VISIBLE);
        }
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "需要存储权限才能保存视频", Toast.LENGTH_LONG).show();
            }
        }
    }
}
