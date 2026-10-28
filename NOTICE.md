# NOTICE · 来源与改动声明

## 上游

本仓库是 **Eta** 的非官方改编版。

- **上游项目**：https://github.com/Mangi-11/Eta
- **上游作者**：Mangi-11
- **上游许可证**：PolyForm Noncommercial License 1.0.0（见 `LICENSE`）

本仓库所有上游原创代码与资源的版权归原作者所有。原始 `LICENSE` 文件未被修改，继续适用。

## 本仓库的定位

外观与主题系统增强分支，聚焦聊天界面的视觉层改造，不改变 Agent 核心逻辑。

## 相对上游的主要改动

| 提交 | 内容 |
|---|---|
| `d2c0620` | 基线快照（含第一轮视觉优化 + 气泡颜色崩溃修复） |
| `ddec6ed` … `6a14eed` | 双方头像：数据层、用户/AI 头像渲染、设置页分组、自写裁剪页 |
| `16a1e07` | 工作过程折叠为单行摘要（含耗时） |
| `d3b2e25` | 主题档案补齐 `blurEnabled` / `topBarBlurStyle`；交互项改全局键 |
| `21fd534` | AI 正文气泡接通 `backgroundArgb` / `cornerRadiusDp` / `padding*` / `material` |
| `55c4b0f` | 新增 `BubbleMaterial.LIQUID_GLASS` 与独立材质实现 |
| `4096946` | 修复气泡面板因材质标签缺失导致的 `ArrayIndexOutOfBounds` 闪退 |
| `b63f8d9` | backdrop 补 `drawContent()` 使壁纸真正录入；移除双重 clip |
| `2499cb6` | 采样点改 `clamp`，修复边缘带退化零位移 |
| `df20b71` | AGSL 像素 uniform 按 `downscaleFactor` 缩放 |
| `40acdf5` | SDF 改用气泡自身尺寸作基准 |
| `043087f` | 折射带高度改为气泡宽度比例（8%） |
| `984bf44` / `19984f1` | 输入框改为半透明玻璃质感（叠色 + 顶部高光 + 细描边） |

## 已知限制

- 输入框的玻璃效果为**视觉近似**，不是真实背景模糊
  （输入栏位于 `Scaffold.bottomBar` 槽位，无法采样 `PageBackground` 录制的壁纸层）
- 折射调参阶段已结束，当前效果为「可测量的连续折射带」，强度受模糊后背景平滑度限制

## 使用限制

**禁止商业使用**（PolyForm Noncommercial 1.0.0）。

个人学习、研究、自用修改不受限制，但需保留本文件与 `LICENSE`。
