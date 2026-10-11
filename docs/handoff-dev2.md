# Aquamirae & Nouveau 0.1.0-dev.2

本批修复用户人工测试反馈：法术书缺少名称/描述、碎晶用途不明确，并按新增要求改为弹射物替换，图标与获取配方尽量采用明确原生来源。

- 工作树：C:/Users/57991/Documents/MinecraftDev/parallel-workspaces/aquamirae-first-abilities/Aquamirae-Nouveau。
- 分支：agent/aquamirae-first-abilities；仍未合并 main，无远程推送。
- Core 工作树：上级 Ars-Conflux；固定完整 SHA 3eccd65fc569cd43ef960ef3eee77963ef201ac8。受版本控制文件未修改，无 Core API 扩展。
- 平台/Java/依赖与首批一致：NeoForge 1.21.1 / Java 21 / Aquamirae 7.2.11 / Ars 5.13.3；Forge 1.20.1 / Java 17 / Aquamirae 7.1.14 / Ars 4.12.7。依赖精确锁定见各自 dependencies.lock.json。
- 授权仍仅部署本附属至用户的 NeoForge 人工测试 mods 目录，其他模组和实例配置不修改。

## 最终行为

- 读取实际 Ars 4/5 AbstractSpellPart 字节码确认 UI 的 glyph_name / glyph_desc 翻译键，补齐中英文；getBookDescription 同步使用标准键。发行检查对每个已获取魔符检查两组键。
- 四个状态魔符保留效果类别。中文深海之韧与本体一致；描述注明原生来源以及深海属性并非通用力量/抗性。
- 碎晶变为 NeoForge 专有 AbstractAugment / ProjectileForm，仍沿用 glyph_abyssal_shard ID。旧版“自身→碎晶”须改为“弹射物→碎晶→效果”；环绕写法“自身→环绕→碎晶→效果”。
- 原 Ars 弹射物不在世界生成。SpellAbyssalShard 继承本体 AbyssalShard，调用原生伤害、虚弱、分裂，直接引用 AbyssalShardRenderer；不添加 Mixin，不改普通本体实体。
- 普通形态使用本体 16 格敌对追踪和 200 tick 生命周期，主碎晶命中续接后续 Ars 效果。原生三个半尺寸子碎晶只保留原生载荷，不续接 Ars、不重复扣费；这是当前明确边界。
- 环绕等 Core 登记的受控形态使用 ArsTrajectory；自身两端停用原生运动/碰撞，控制模式由 SynchedEntityData 同步。重复形态按命中支付，每次续接，不碎裂；单次形态按生成付费。
- ExecutionSession 处理所有者失效、空蓝、取消、未加载区块和到期清理。未保存运行时 resolver，重新加载无控制器的附属实体会移除。
- 图标/配方采用同一来源：裂解尖刺、裂解之刃、鮟鱇鱼精华、熟棘鱼、破晓之惧。均通过抄写台，另消耗魔源宝石 ×1 和 27 XP；来源武器会被消耗。
- Forge 无原生碎晶，不链接或注册新增实体与增强；四个效果与来源配方同步更新。

## 实际验证

NeoForge JAVA_HOME=C:/Program Files/Java/jdk-21；Forge JAVA_HOME=C:/Users/57991/Documents/MinecraftDev/toolchains/jdk-17.0.18+8。

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| NeoForge 服务端、打包 | ./gradlew.bat :runGameTestServer :jar :sourcesJar --offline --console=plain | 12/12；build/feedback-neo.log |
| Forge 服务端、重混淆、打包 | forge/ 下相同任务 | 6/6；forge/build/feedback-forge.log；reobfJar 成功 |
| 最后文案更新后的打包 | 两版 :jar :sourcesJar --offline --console=plain | 成功；各自 build/feedback-package-*.log，未重复无关 GameTest |
| 发行检查 | python tools/verify_release.py；加 --forge | 两版二进制/源码包通过；含实际 Ars 翻译契约、语言键、图标/来源材料一致、依赖哈希、许可、无本体打包内容 |

新增集成测试覆盖真实 Ars 生成替换、普通 Ars 保持正常、费用和增强、原生真实碰撞伤害/虚弱、正确目标/归属/一次续接、原生分裂与到期、真实环绕运动、单驱动、重复命中收费、空蓝取消、所有者失效清理。服务端最小依赖环境启动通过，不将其说成客户端外观验证。

## 产物与部署

| 相对本工作树路径 | SHA-256 |
| --- | --- |
| build/libs/aquamirae-nouveau-0.1.0-dev.2-mc1.21.1-neoforge.jar | 6322d205922fb673c5219ca8d2e7d1a8751dc59ef98cc30c2156b2a6775e4683 |
| build/libs/aquamirae-nouveau-0.1.0-dev.2-sources.jar | 1e78d65af58cd6dc6294db89e65a568c68527223f770f1cd0b0e705bdc7ed7d7 |
| forge/build/libs/aquamirae-nouveau-0.1.0-dev.2-mc1.20.1-forge.jar | 55b4d6ac2b5676b64beb6cde99ec0e08e0f917dbfe4baba8fd5b8a0f961a4aa1 |
| forge/build/libs/aquamirae-nouveau-0.1.0-dev.2-sources.jar | 3b72f3323ccb780e3ebc316ed600e57c32342098a3230d109b23bcf324451e01 |

NeoForge 本附属 dev.2 已部署至 C:/Users/57991/Desktop/.minecraft/versions/1.21.1-万法归一/mods；按 JAR 内 modId 确认并移除本附属 dev.1，旧版备份在 build/deployment-backup/dev2。
复制后哈希相符，其余 **161 个 JAR** 在本次部署前后哈希完全一致；未替换 Core/原模组/其他附属，未部署 Forge。证据 build/deployment-proof-dev2.json。

客户端未由 agent 启动或人工验收。请完整重启客户端后确认法术书名称/描述、来源图标、抄写台材料与实际碎晶画面；数据检查不代表 UI 人工验证。注意需要重新编排旧版碎晶法术。

首批 dev.1 验证和历史见 handoff-first-abilities.md；原生来源与平台差异见 native-audit.md。
