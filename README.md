# Liquid Glass Gallery

一个用 **Kotlin + Jetpack Compose** 写的安卓相册 App 示例，视觉上模拟 iOS 风格的 Liquid Glass：半透明玻璃岛、折射高光、柔和边缘、浮动导航和暗色动态背景。

## 主要特性

- 纯 Compose UI，无需任何图片/icon 素材即可编译。
- 已内置 `@mipmap/ic_launcher` 和 `@mipmap/ic_launcher_round` 的自适应矢量图标 XML，不会因为缺少 icon 资源报错。
- 支持读取系统相册最近图片；未授权或无图片时显示内置演示卡片。
- 顶部玻璃工具栏、底部液态玻璃导航、玻璃照片预览卡。
- 已包含 GitHub Actions 工作流，可直接构建 Debug APK 并上传 artifact。

## 本地构建

```bash
./gradlew :app:assembleDebug
```

如果本机没有 Gradle，仓库内的 `gradlew` 会尝试下载 Gradle 8.10.2；Windows 用户可安装 Gradle 后运行：

```bat
gradle :app:assembleDebug
```

## GitHub Actions 构建

把整个项目推到 GitHub 后，Actions 会在 push / pull request / 手动触发时运行：

```bash
gradle :app:assembleDebug --stacktrace
```

构建产物路径：

```text
app/build/outputs/apk/debug/app-debug.apk
```

Actions 页面会上传名为 `liquid-glass-gallery-debug-apk` 的 artifact。

## 项目结构

```text
LiquidGlassGallery/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/liquidglassgallery/MainActivity.kt
│       └── res/
│           ├── drawable/
│           ├── mipmap-anydpi-v26/
│           └── values/
├── .github/workflows/android-build.yml
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew
```
