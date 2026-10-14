# AI 截屏待办助手（MyApplication2）

一款基于 **Android 无障碍截屏 + OCR + 大模型** 的智能待办应用：截屏或截图后，自动识别屏幕上的文字，用 **DeepSeek** 提取出「待办任务」与「截止时间」，生成任务卡片，并通过悬浮窗和通知提醒你。

## 核心流程

```
手机截屏 / 相册截图
      ↓
屏幕文字识别（ML Kit 中文 OCR）
      ↓
DeepSeek AI 提取任务与截止时间
      ↓
任务存入本地数据库（Room）
      ↓
悬浮窗卡片 + 到期通知提醒
```

## 功能特性

- 📸 **无障碍自动截屏**：借助无障碍服务截取当前屏幕，并监听系统截图事件
- 🧠 **AI 任务识别**：调用 DeepSeek（`deepseek-chat`）从 OCR 文本中提取任务内容与截止时间，输出结构化 JSON
- 🗂️ **本地任务管理**：Room 数据库持久化，支持任务列表 / 详情查看、完成状态标记
- 🪟 **悬浮窗卡片**：在桌面或其他应用上方显示任务卡片，随时可见
- 🔔 **到期提醒**：任务到期通过通知提醒，常驻前台服务保障提醒可靠
- ⏱️ **截止时间解析**：AI 结合当前时间推断截止时间（如"明天下午 6 点前交周报"→ ISO8601）

## 技术栈

| 类别 | 技术 |
| --- | --- |
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 + Navigation |
| 数据库 | Room（KSP） |
| OCR | ML Kit 中文文字识别 |
| AI | DeepSeek API（OpenAI 兼容接口） |
| 后台 | WorkManager + 前台服务（specialUse） |
| 系统能力 | 无障碍服务、悬浮窗、通知 |
| 构建 | Gradle（Kotlin DSL），minSdk 36 / targetSdk 37 |

## 目录结构

```
app/src/main/java/com/example/myapplication/
├── MainActivity.kt                 # 入口
├── ai/DeepSeekClient.kt            # DeepSeek 调用与任务提取
├── capture/                        # 截屏与截图监听（ScreenshotWatcher）
├── ocr/OcrEngine.kt                # ML Kit 文字识别
├── parse/                          # AI 返回结果解析
├── data/                           # Room 实体与数据库（Task / AppDatabase）
├── service/ScreenCaptureAccessibilityService.kt  # 无障碍截屏服务
├── status/TaskStatusService.kt     # 常驻状态前台服务
├── overlay/                        # 悬浮窗任务卡片
└── ui/                             # Compose 界面（任务列表 / 详情）
```

## 运行准备

1. **Android Studio** 打开项目，等待 Gradle 同步；
2. **DeepSeek API Key**：在应用设置中填入（`https://platform.deepseek.com` 获取），用于 AI 识别；
3. **开启无障碍服务**：系统设置 → 无障碍 → 本应用（截屏能力）；
4. **授予悬浮窗权限**（任务卡片显示）与 **通知权限**（到期提醒）；
5. 连接设备（Android 16+，minSdk 36）运行。

## 隐私说明

- 截图文字会发送至 DeepSeek 进行任务提取，请勿在截图包含敏感信息时使用；
- 任务数据仅保存在本机 Room 数据库，不包含任何云端同步。

## 下一步计划

- [ ] 多任务批量提取与去重
- [ ] 截止时间自定义编辑
- [ ] 任务分类 / 标签
