# Quietly

一个 Minecraft Spigot/Paper 插件，让玩家可以无声地打开容器。

## 功能特性

- **无声开启** - 潜行 + 双击右键容器，可以在不发出声音的情况下打开
- **进度显示** - 使用 Boss 血条显示开启进度
- **智能计时** - 容器内物品种类越多，开启时间越长
- **多语言支持** - 支持中文和英文

## 支持的版本

- **Minecraft**: 1.21.1 - 1.21.10+
- **Java**: 21+
- **服务端**: Spigot, Paper 及其衍生核心

## 使用方法

1. 潜行（按住 Shift）
2. 空手（主手不要拿任何物品）
3. 快速双击右键容器
4. 保持潜行直到进度条完成

### 支持的容器

- 箱子 (Chest)
- 木桶 (Barrel)
- 末影箱 (Ender Chest)
- 潜影盒 (Shulker Box)
- 工作台 (Crafting Table)
- 附魔台 (Enchanting Table)
- 其他容器

## 配置说明

### config.yml

```yaml
# 语言设置 (zh_cn / en_us)
language: zh_cn

# 双击时间窗口（毫秒）
double-click-window-ms: 350

# 基础开启时间（刻）
base-open-ticks: 12

# 每种物品类型增加的开启时间（刻）
ticks-per-item-kind: 4

# 交互范围（格）
interaction-range: 5.0

# 声音抑制半径（格）
sound-suppression-radius: 16.0
```

## 权限

| 权限 | 说明 | 默认 |
|------|------|------|
| quietly.use | 允许使用无声开启功能 | true |

## 安装方法

1. 下载 `Quietly-1.0.0.jar`
2. 将文件放入服务器的 `plugins` 文件夹
3. 重启服务器或使用 `/reload` 命令

## 构建方法

```bash
# 克隆仓库
git clone https://github.com/kafei520-CN/Quietly.git
cd Quietly

# 构建项目
mvn clean package

# 构建后的文件位于 target/Quietly-1.0.0.jar
```

## 项目信息

- **作者**: Kafei
- **许可证**: MIT
- **GitHub**: https://github.com/kafei520-CN/Quietly

## 更新日志

### v1.0.0
- 初始版本发布
- 支持无声开启容器
- 支持多语言
- 支持 1.21.1+ 版本
