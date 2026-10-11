# Aquamirae & Nouveau 首批交接

> 本文件为 0.1.0-dev.1 历史记录。当前弹射物替换、来源配方与交付见 [dev.2 交接](handoff-dev2.md)。

## 范围

- 用户目标：开发海灵物语附属，NeoForge 1.21.1 与 Forge 1.20.1；显示名称 **Aquamirae & Nouveau**。
- 用户后续授权：只部署本附属到指定 NeoForge 人工测试目录，其他模组与 Core 不替换；不推送或发布。
- 附属工作树：`C:/Users/57991/Documents/MinecraftDev/parallel-workspaces/aquamirae-first-abilities/Aquamirae-Nouveau`。
- 分支：`agent/aquamirae-first-abilities`。允许修改本附属工作树；原项目 `C:/Users/57991/Documents/MinecraftDev/Aquamirae-Nouveau` 只作为本地仓库和初始 main 基线，没有接管其他附属工作树。
- Conflux 固定完整提交：`3eccd65fc569cd43ef960ef3eee77963ef201ac8`。
- Core 构建副本：`C:/Users/57991/Documents/MinecraftDev/parallel-workspaces/aquamirae-first-abilities/Ars-Conflux`，detached、受版本控制文件只读。
- 隔离清单：上级 `workspace.json`；两个平台的 build/、run/ 与日志各自在本附属工作树，手动测试预留目录为上级 test-instance/mods。
- Java 21：`C:/Program Files/Java/jdk-21`；Java 17：`C:/Users/57991/Documents/MinecraftDev/toolchains/jdk-17.0.18+8`。
- 本体与 Ars 精确版本/依赖 SHA-256：根目录及 forge/dependencies.lock.json。NeoForge Aquamirae 7.2.11 / Ars 5.13.3，Forge Aquamirae 7.1.14 / Ars 4.12.7；Fragmentum 均为 5.2.0。

## 已完成

- 四个共享效果魔符：护甲碎裂、生命碎裂、深海之怒、深海韧性。原生状态、叠层/增强上限、延时、触发支付与主动释放冷却已接线。
- NeoForge 额外提供原生深渊碎晶，完整调用注册实体及生命周期。原生追踪、伤害、虚弱、分裂、同步、渲染、声音保留。自身从眼部发射；方块触发向表面外偏移。
- Forge 原模组没有深渊碎晶实体，不注册该魔符/配置/配方，不重写原模组能力。
- 两版独立入口、创意分类、中文/英文、原生抄写台获取配方、服务端配置、发行包和源码包。
- common/ 为共享实现；平台差异位于各自 platform/。Forge 构建从 NeoForge 配置模板生成小型 API 适配，并转换配方为 Ars 4 的嵌套 inputItems 格式。
- 原模组发布 JAR 的核对见 [native-audit.md](native-audit.md)。辅助 GitHub 检出 SHA `a468d77cc443a932d56a7aa34303f4e435eca3b6`，仅供阅读，未将其代码或素材打包。
- 没有 Core 扩展需求，没有修改 Core，没有跨附属硬依赖、Mixin 或额外静态实体状态。

## 验证证据

以下命令以工作树为当前目录；Forge 命令进入 forge/ 并设置 JAVA_HOME 为 Java 17，NeoForge 为 Java 21。

| 验证 | 实际命令 | 结果及记录 |
| --- | --- | --- |
| NeoForge 服务端与打包 | `./gradlew.bat :runGameTestServer :jar :sourcesJar --offline --console=plain` | 8/8 通过；build/verification-neo.log，run/gametest/logs/latest.log；最后一轮包含自身眼部发射修正 |
| Forge 服务端与打包 | `./gradlew.bat :runGameTestServer :jar :sourcesJar --offline --console=plain` | 6/6 通过；forge/build/verification-forge.log，forge/run/gametest/logs/latest.log；reobfJar 成功 |
| NeoForge 发行包检查 | `python tools/verify_release.py` | 二进制/源码均通过：依赖哈希、5 个配方、原材料、图标路径、许可、禁止打包的原模组内容 |
| Forge 发行包检查 | `python tools/verify_release.py --forge` | 二进制/源码均通过：4 个配方、Ars 4 格式、原材料、图标路径、许可、不暴露缺失的碎晶 |
| Git | `git diff --check` 与 `git diff --cached --check` | 通过 |
| 固定 Core | `git -C ../Ars-Conflux status --porcelain --untracked-files=no` | 空输出，受版本控制文件未变化；根 Core 仓库也未修改 |

服务端测试实际覆盖：原生护甲与生命属性、移除后的属性恢复、叠层上限、增强和延时、重复触发费用、无效目标与失败释放、空蓝阻止效果、创造模式免费、付款归属、原生 buff 不同级/同级处理、主动释放冷却与既有命中、克隆上下文归属、后续效果目标、原生配方加载，以及 NeoForge 碎晶所有者、眼部发射、尺寸伤害、原生实际命中虚弱、分裂和原生到期清理。

修正过的验证问题：Ars contextCanceled 是通知回调而非状态查询，改用 context.isCanceled；Forge API 和 LazyOptional 的签名适配；pack.mcmeta 与测试结构 namespace；Ars 4 实际嵌套 inputItems 配方。最终证据对应修正后的代码，不沿用失败结果。

未开启其他附属共存测试；最小依赖专用服务器可启动。本附属不修改本体实体/效果代码，原生普通能力未添加全局补丁；完整原模组玩法未做人工回归。
**客户端外观未人工验证**，共享整合包内的启动、图标、抄写台操作、碎晶画面/声音与法术组合仍需人工验收。GameTest 与资源路径检查不代表画面验证。

## 交付

- 版本：`0.1.0-dev.1`。
- 实现提交：`cc04a228c683837f64072325c8f224e56237620f`；本交接另作后续文档提交。
- 以下路径均相对本附属工作树；目录保留实际二进制与源码产物。

| 产物 | SHA-256 |
| --- | --- |
| `build/libs/aquamirae-nouveau-0.1.0-dev.1-mc1.21.1-neoforge.jar` | `70bee5ef71ddc74e0245f93e6f0d29eaf7452e61fa14e06d297222e42e3992c0` |
| `build/libs/aquamirae-nouveau-0.1.0-dev.1-sources.jar` | `44a69b8553e5e280b6ee965760e972341406f455e51b0aaf8981dc9d0c62fd14` |
| `forge/build/libs/aquamirae-nouveau-0.1.0-dev.1-mc1.20.1-forge.jar`（已重混淆） | `197f375f242a8e1d6854b5fc3ddeee65562172a79bb379a124315e0a6e83f930` |
| `forge/build/libs/aquamirae-nouveau-0.1.0-dev.1-sources.jar` | `552354efac2f6e5e194a5b4769a7621e923e419f9cd137b5202b73a0819df39b` |

已仅部署 NeoForge 本附属至：
`C:/Users/57991/Desktop/.minecraft/versions/1.21.1-万法归一/mods/aquamirae-nouveau-0.1.0-dev.1-mc1.21.1-neoforge.jar`。
最终复制后 SHA-256 与上表一致；部署前后其余 **159 个 JAR** 的 SHA-256 保持不变，包括 Conflux、Ars Nouveau、Aquamirae 与 Fragmentum。
部署证据：build/deployment-proof.json。Forge 产物未部署。共享整合包客户端未启动测试。

依赖、反编译缓存、日志和测试世界仅在忽略目录；发行包没有原模组素材/代码/JAR，素材均为运行时路径引用。许可见 THIRD_PARTY_NOTICES.md。
本次测试 JVM 已结束；没有停止其他任务的 Java 或 Gradle daemon。其他工作树（包括 cataclysm-ground-effects）未修改。

## 下一步

人工验收先检查独立创意分类与五个 NeoForge 魔符，抄写台材料/27 XP，`自身 → 深海之怒`、`弹射物 → 护甲碎裂 → 伤害`、`自身 → 深渊碎晶`。
特别观察碎晶自眼部发出、模型/声音、原生敌对目标选择和分裂。后续 Ars 效果仍在原触发位置，不附着到碎晶命中。
结晶化、毒素免疫和回旋镖的原生差异见核对文档，未承诺已适配；后续任务应先明确能力与平台差异。

> 在指定 Aquamirae-Nouveau 工作树继续。先读 AGENTS.md、README 与固定 Core 的共享流程，核对 workspace.json 和 Git 状态。Core 固定于 3eccd65fc569cd43ef960ef3eee77963ef201ac8，受版本控制文件只读。首批服务端已 8/8 NeoForge、6/6 Forge 通过，不无理由重复测试矩阵。NeoForge 附属已按用户授权单独部署，其他模组未替换，客户端仍需人工验收；新部署动作遵循新任务授权范围。
