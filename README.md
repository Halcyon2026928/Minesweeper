# 扫雷 Minesweeper（Android）

一个用 Java 写的原生 Android 扫雷，包含沉浸式状态栏与一套偏"潮流"的视觉风格：渐变背景、圆角卡片、胶囊按钮、Material 圆角对话框，并完整支持深色模式。

- 包名：`com.example.Minesweeper`
- 版本：`2.2`（versionCode 3）
- 最低支持：Android 7.0（API 24），目标 API 34
- 语言 / 构建：Java + Gradle（AGP 8.5.2 / Gradle 8.7 / JDK 17）

## 玩法与功能

**三种难度**

| 难度 | 棋盘 | 地雷 |
|---|---|---|
| 初级 | 9 × 9 | 10 |
| 中级 | 16 × 16 | 40 |
| 高级 | 18 × 28 | 99 |

**操作**

- **单击**翻开格子；翻开空白格（周围 0 雷）时自动向四周扩散。
- **长按**（约 125 ms）标旗 / 取消标旗。
- **双击数字格**：周围已标旗数与数字相同时，翻开其余相邻格；数量不足时闪烁提示未标记的格子。
- 胜利条件是所有地雷都被正确标记，踩雷则立即失败并展示全部雷位。

**记录与存档**

- 按难度分别记录历史最佳用时与累计局数，主页三行战绩卡直接展示。
- 退出或返回主页自动保存当前棋盘，进度存在时主页「继续游戏」可用。
- 游戏页顶部两枚胶囊实时显示用时与剩余地雷数。

**设置与外观**

- 音效开关（翻开 / 爆炸 / 胜利音效）、深色模式开关、清除最佳记录、清除局数统计、打赏、关于。
- 深色模式基于 `AppCompatDelegate` + `values-night` 配色，切换即时生效。
- 沉浸式状态栏：内容延伸到系统栏下方，状态栏图标深浅随浅色/深色模式自动切换。

## 项目结构

```
app/src/main/java/com/example/Minesweeper/
├── MainActivity.java      主页：难度入口、战绩卡、继续游戏
├── GameActivity.java      游戏核心：棋盘构建、翻开/标旗/连锁/计时/胜负/存档
├── HelpActivity.java      操作说明
├── SettingsActivity.java  设置（浮动圆角卡片）
├── ImmersiveUtils.java    沉浸式状态栏工具类
└── SecurityCheck.java     启动安全检测（当前实现恒放行，可自行扩展）

app/src/main/res/
├── layout/                activity_main / activity_game / activity_help / activity_settings
├── drawable/              bg_gradient / bg_card / bg_board / bg_chip / bg_accent_bar /
│                          cell_unrevealed / cell_revealed / dot_accent / dot_amber / dot_sky
├── color/                 按钮着色与文字色 selector
├── values/                colors.xml / themes.xml / strings.xml
├── values-night/          colors.xml / themes.xml（深色配色）
└── raw/                   click.mp3 / explosion.mp3 / win.mp3
```

## 两个值得说明的实现细节

**1. 棋盘为什么用固定尺寸的行列 LinearLayout，而不是 GridLayout**

最初格子放在 `GridLayout` 里，点击后代码会把文字字号和内边距改掉，而 `Button` 的文本测量结果会参与单元格尺寸计算——于是出现数字/💣 的行列被撑大（实测空文本格 109 px、含文字格所在行变成 119 px），表现为"每点开一格，那一行的格子就被挤偏一点，间距越点越大"。

现在改成：每行一个横向 `LinearLayout`，每个格子用 `LinearLayout.LayoutParams(buttonSize, buttonSize)` 固定宽高，字号按像素一次性定死（`setTextSize(COMPLEX_UNIT_PX, buttonSize * 0.42f)`），点击后只改文字、文字颜色和背景，**不再触碰任何几何属性**。格子尺寸按棋盘区域的真实可用宽高取较小值计算，因此三档难度都不会溢出屏幕。

**2. 沉浸式状态栏**

`ImmersiveUtils.enableImmersiveStatusBar(Activity)` 在 `setContentView` 之前调用，做了这些事：

- 清除 `FLAG_FULLSCREEN` / `FLAG_TRANSLUCENT_STATUS`，改用 `FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS`；
- `setStatusBarColor(TRANSPARENT)`，API 29+ 关闭 `statusBarContrastEnforced`；
- `WindowCompat.setDecorFitsSystemWindows(window, false)` 让内容延伸到系统栏下方；
- 按当前 `uiMode` 用 `WindowInsetsControllerCompat.setAppearanceLightStatusBars()` 切换图标深浅；
- 给 `android.R.id.content` 补 `systemBars | displayCutout` 的内边距，避免内容被刘海/状态栏遮挡。

## 构建

准备 JDK 17 与 Android SDK（`platforms;android-34`、`build-tools;36.0.0`），然后在项目根目录建 `local.properties` 指向 SDK：

```properties
sdk.dir=D\:\\sdk
```

打包 Debug 包：

```bash
./gradlew assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`，安装：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

说明：

- 仓库里同时保留了 `build.gradle.kts` / `settings.gradle.kts` / `app/build.gradle.kts` 三份 Groovy 脚本的旧版，已改名为 `*.disabled`。Gradle 会优先解析 `.kts`，那套脚本内的 AGP 版本与依赖已过期，请保持停用状态。
- `local.properties` 已被 `.gitignore` 忽略，不要提交。
- 无网络依赖：全部数据存在本机 `SharedPreferences`（`MinesweeperRecords` / `MinesweeperPrefs` / `GameProgress`）。

## 更新日志

**v2.2（versionCode 3）**

- 新增沉浸式状态栏（`ImmersiveUtils`）。
- UI 全面重做为潮流风格：渐变背景、圆角卡片棋盘、胶囊按钮与状态胶囊、Material 圆角对话框、深色模式配色。
- 修复点击格子后棋盘间距/位置偏移的问题：棋盘改为固定尺寸行列布局，字号与内边距不再随状态变化。
- 构建链路调整：AGP 8.5.2 / Gradle 8.7、显式 `buildToolsVersion 36.0.0`、停用过期 `.kts` 脚本。

**v2.1（versionCode 2）** — 初始版本：三档难度、标旗、双击连锁、计时、战绩与进度存档、音效振动、设置页。
