========================================
  B站视频下载器 - Android APK 构建指南
========================================

一、环境准备
-----------
1. 下载安装 Android Studio（免费）：
   https://developer.android.com/studio

2. 安装时勾选 Android SDK、Android SDK Platform-Tools
   （Android Studio 会自动安装）

3. 你的电脑已有 JDK 21，无需额外安装 Java


二、打开项目
-----------
1. 打开 Android Studio
2. 选择 "Open" → 找到 bilibili_apk 文件夹 → 确定
3. 等待 Gradle 同步完成（首次约 3-5 分钟，会自动下载依赖）


三、编译 APK
-----------
方法一：Debug APK（快速测试）
  菜单 Build → Build Bundle(s)/APK(s) → Build APK(s)
  生成路径：app/build/outputs/apk/debug/app-debug.apk

方法二：Release APK（正式版）
  菜单 Build → Generate Signed Bundle / APK → APK
  需要创建签名密钥（按向导操作即可）


四、安装到手机
-----------
方法一：USB 连接
  1. 手机开启"开发者选项"和"USB调试"
  2. USB 连接电脑
  3. 在 Android Studio 中直接点运行按钮

方法二：传输 APK 文件
  1. 把生成的 app-debug.apk 传到手机
  2. 手机上点击安装（需开启"允许未知来源应用"）


五、使用方法
-----------
1. 打开 App
2. 粘贴 B站视频链接（每行一个可批量下载）
3. 选择清晰度
4. 点"开始下载"
5. 视频保存在：手机存储/Download/BilibiliDownloader/


六、清晰度说明
-------------
360P / 480P / 720P —— 无需登录，直接下载
1080P / 4K         —— 需要填 SESSDATA

SESSDATA 获取方法：
  1. 电脑浏览器登录 bilibili.com
  2. 按 F12 → Application → Cookies → bilibili.com
  3. 找到 SESSDATA，复制值
  4. 粘贴到 App 的 SESSDATA 输入框


七、技术架构
-----------
- 下载引擎：直接调用 B站官方 API（不依赖 yt-dlp）
- 音视频合并：Android MediaMuxer（系统原生 API，无需 ffmpeg）
- HTTP 库：OkHttp 4.12
- UI 框架：Material Design + RecyclerView
- 最低系统：Android 7.0 (API 24)
- 目标系统：Android 10 (API 29)，兼容 Android 7-14


八、常见问题
-----------
Q: Gradle 同步失败？
A: 检查网络，确保能访问 google() 和 mavenCentral() 仓库；
   如需代理，在 Android Studio 设置中配置 HTTP Proxy

Q: 提示 SDK 缺失？
A: 在 Android Studio 的 SDK Manager 中安装 API 34 (Android 14)

Q: 下载失败提示 403？
A: B站 CDN 需要特定 Referer 头，代码已处理；
   如果仍然失败，可能是视频有区域限制

Q: 1080P 下载不了？
A: 1080P 需要登录，必须填写有效的 SESSDATA

Q: APK 安装被拦截？
A: 手机设置 → 安全 → 允许"未知来源"安装
