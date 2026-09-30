# 会议室预约 Android 应用

基于 Java 和 Android SDK 的本地会议室预约示例项目。项目使用设备上的 SQLite 数据库，不需要部署服务端。

## 运行

1. 安装 Android Studio、JDK 21，并在 SDK Manager 中安装 Android SDK Platform 34。
2. 克隆仓库，用 Android Studio 打开仓库根目录，等待 Gradle 同步完成。
3. 连接 Android 7.0（API 24）或更高版本的设备或模拟器，运行 `app` 配置。

也可以在配置好 Android SDK 的命令行执行：

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleDebug
```

首次启动会创建本地示例数据。可以注册普通用户，也可以使用示例账号 `user001` / `123456`；管理界面示例账号为 `boss` / `123456`。这些账号仅供本地演示，不应当用于实际服务。

Gradle wrapper 和版本目录随仓库提供；Android SDK、Gradle 下载缓存、IDE 配置和构建产物由各自机器生成，不需提交。
