# Quietly

一个适用于 Fabric `1.21.1 ~ 1.21.11` 的无声开箱模组。

## 功能

- 潜行并长按右键时，无声打开箱子、陷阱箱、木桶和末影箱
- 使用 Bossbar 显示开启进度
- 开启耗时取决于容器内的物品种类数量
- 保留容器开启动画
- 不播放原版开箱音效
- 不触发原版 `CONTAINER_OPEN / CONTAINER_CLOSE` 声感事件

## 使用方式

- 对准支持的容器
- 按住 `Shift`
- 持续按住鼠标右键直到 Bossbar 读满
- 读满后容器会以静默方式打开

## 支持容器

- 普通箱子
- 陷阱箱
- 木桶
- 末影箱

## 当前规则

- 只有 `Shift + 主手长按右键` 会触发静默开箱
- 开启速度公式为：`12 + 物品种类数 * 4` tick
- 物品种类数按容器内非空物品的 `Item` 去重计算，不按堆叠数量计算
- 当前 Bossbar 已提供中英文本地化

## 开发环境

- Minecraft: `1.21.1`
- Fabric Loader: `0.19.2`
- Fabric API: `0.116.12+1.21.1`
- Java: `21`

## 构建

```bash
./gradlew.bat build
```

构建完成后的模组文件位于：

```text
build/libs/quietly-1.0.0.jar
```

## 说明

- `fabric.mod.json` 中声明的兼容范围为 `1.21.1 ~ 1.21.11`
- 若需要进一步扩展，可继续添加配置文件、自定义时长、更多提示文本或发布页说明

## License

This project is available under the CC0 license.
