package com.xq.bilibilidownloader;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private EditText urlInput;
    private Spinner qualitySpinner;
    private EditText sessdataInput;
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
        sessdataInput = findViewById(R.id.sessdataInput);
        downloadBtn = findViewById(R.id.downloadBtn);
        clearBtn = findViewById(R.id.clearBtn);
        taskList = findViewById(R.id.taskList);
        emptyText = findViewById(R.id.emptyText);
        savePathText = findViewById(R.id.savePathText);
        scrollView = findViewById(R.id.scrollView);

        String[] qualities = {"360P 流畅", "480P 标清", "720P 高清", "1080P 超清(需登录)", "4K 超高清(需大会员)"};
        ArrayAdapter<String> qAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, qualities);
        qualitySpinner.setAdapter(qAdapter);
        qualitySpinner.setSelection(2);

        taskList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter();
        taskList.setAdapter(adapter);
        taskList.setNestedScrollingEnabled(false);

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
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "BilibiliDownloader");
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
        String sessdata = sessdataInput.getText().toString().trim();

        List<String> urlList = Arrays.asList(urls.split("\n"));
        File saveDir = getSaveDir();

        int count = 0;
        for (String u : urlList) {
            if (!u.trim().isEmpty()) count++;
        }

        downloadManager.submit(urlList, quality, sessdata, saveDir);

        urlInput.setText("");
        Toast.makeText(this, "已添加 " + count + " 个下载任务", Toast.LENGTH_SHORT).show();

        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
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
