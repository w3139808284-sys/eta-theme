# Eta 主题版 · 项目说明书

> 精简权威版。接手前先读本文，不要凭记忆推断状态。​

## 1. 项目概要

Eta 个人主题增强版：在官方 Eta 源码上叠加壁纸、Liquid Glass 材质、双方头像、气泡样式、输入框玻璃质感等 UI 改动。

| 项 | 值 |
|---|---|
| 源码仓库 | `https://github.com/w3139808284-sys/eta-theme` |
| 发布资产仓库 | `https://github.com/w3139808284-sys/eta-release-vault`（Private） |
| 上游项目 | `https://github.com/Mangi-11/Eta` |
| 本地源码目录 | `D:\dsh\work\Eta-wp` |

## 2. 当前维护基线

| 项 | 值 |
|---|---|
| **维护分支** | `theme-on-3.3.5` |
| **上游基线版本** | v3.3.5 |
| **上游基线提交** | `6bedba981b78cb308a58c9f040a9f66a9a16595a` |
| 主题提交 HEAD | `a2a85ce0a952c9277ee1af3519ad9fe08de41c5a` |
| 主题提交数 | 22 |
| 分支总提交 | 462（含上游完整历史） |

**分支关系已验证**：`33c050f`（主题基线提交）的 parent 就是 `6bedba9`，无 `git replace`、无 graft，干净克隆可复现。

## 3. 当前版本

| 项 | 值 |
|---|---|
| versionName | `3.3.5-theme` |
| versionCode | `2026102802` |
| 包名 | `io.github.mangi.eta` |
| **APK SHA-256** | `F481D8C337A4998DA000A3D55151924E7FA1481DB81B0633CD9C01D226F0B95A` |
| 签名证书 DN | `CN=Eta Personal, O=Eta, OU=Custom, C=CN` |
| 签名证书 SHA-256 | `32f31f913be68cfb8ebc1f76d9e5ee1777a41c7447f92b319f24a01e959c44f2` |
| APK 归档位置 | vault `releases/v3.3.5-theme/Eta-v3.3.5-theme.apk` |
| R8 mapping | vault Release `v3.3.5-theme` 的 `mapping-3.3.5-theme.txt`（约 102 MB） |

## 4. 签名规则

- **后续所有版本继续使用同一把个人密钥**：`D:\dsh\work\Eta-keys\eta-personal-release.jks`
- 密钥的 AES-256 加密备份：vault `secure-backup/eta-personal-release.jks.7z`
- 解密后 SHA-256 应为：`B56B499059ED1B1C989CFD139065C938ADB64BA749B5CC57271830DD3E44950F`
- **密码通过独立安全渠道获取，任何文档、日志、命令行参数中都不得出现明文密码**
- 密钥与密码分处两地保存，不得同处一份备份

## 5. 最短工作流

### 构建

```powershell
$env:JAVA_HOME='D:\Eta-Migration-Handoff\toolchain\jdk'
$env:ANDROID_HOME='D:\Eta-Migration-Handoff\toolchain\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
# 签名环境变量从安全渠道取，不要写入文件
$env:ETA_RELEASE_STORE_FILE='<密钥路径>'
$env:ETA_RELEASE_STORE_PASSWORD='<从安全渠道取>'
$env:ETA_RELEASE_KEY_ALIAS='<alias>'
$env:ETA_RELEASE_KEY_PASSWORD='<从安全渠道取>'

cd D:\dsh\work\Eta-wp
D:\Eta-Migration-Handoff\toolchain\gradle\bin\gradle.bat :app:assembleRelease
```

### 安装验证

```bash
adb install -r app/build/outputs/apk/release/Eta-v3.3.5-theme.apk
adb shell dumpsys package io.github.mangi.eta | grep versionName
```

### 发布归档

1. APK 复制到 vault `releases/<版本>/`
2. mapping 作为 Release 资产上传（单文件 >100MB 不能进 Git）
3. 更新 vault `README.md` 的版本索引

## 6. 升级上游新版

```bash
git fetch upstream --tags
git rev-parse <当前基线提交>          # 记下旧基线
git checkout theme-on-3.3.5
git rebase --onto upstream/v3.6.0 <旧基线提交> theme-on-3.3.5
# 解决冲突 → git add → git rebase --continue
# 改 versionCode（必须 > 上游该版本）
# 构建 + 签名 + 安装验证 + 推送
```

**注意**：`--onto` 里的旧基线每次升级后都要更新，否则会重复重放提交。

## 7. 主题与官方的接缝（升级时重点检查）

| 接缝 | 文件 | 说明 |
|---|---|---|
| 主题档案投影 | `data/repository/AppearanceSettingsRepository.kt` | 官方新增 `AppearanceSettings` 字段时需同步 `ThemeProfile` 与两个投影函数 |
| 气泡渲染注入 | `ui/components/ChatMessageItem.kt` | 官方重写气泡绘制时需重新接入 `LocalBubbleStyles` / `BubbleMaterial` |
| 页面背景层 | `ui/components/PageBackground.kt`、`ui/app/AgentAppShell.kt`、`ui/components/MiuixScaffoldPage.kt` | 官方新增 Scaffold 骨架时需套用 `PageBackground` |
| 主题入口 | `ui/MainActivity.kt` | 保留 `ThemeProfileRepository` collect、`LocalBubbleStyles` 提供、投影调用 |
| 输入栏 | `ui/components/AgentChatInputBar.kt` | 官方改输入栏结构时保留玻璃叠色与草稿存储 |

## 8. 已知限制

- **LSPosed 第三方模块兼容性未验证**（上游 v3.3.5 大改了 ChatMessageItem、AgentChatBody、MainActivity，插件可能失效）
- **深色主题下输入框可读性未实测**
- **长列表滚动帧率未实测**
- 输入栏是「仿玻璃」视觉效果（半透明叠色 + 顶部高光 + 细描边），**不是真实背景模糊**
- 签名与上游官方不同：从官方包切换需先卸载（丢数据）；从旧个人版升级可直接覆盖

## 9. 保护规则

1. 不擅自删除唯一资产（密钥、唯一 APK、唯一 mapping、恢复资料）
2. 不泄露密钥或密码到 Git、文档、日志、命令行
3. 不重写 Git 历史、不强推、不删远端分支
4. 不为了省空间重新编译或修改当前有效 APK
5. 清理前先确认文件用途、是否重复、是否能重新生成

## 10. 恢复说明

见 vault `recovery/RESTORE-GUIDE.md`。​
