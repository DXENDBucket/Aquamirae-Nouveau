# Aquamirae & Nouveau 0.1.0-dev.3

本批修复「史莱姆，弹射物→碎晶」无法命中的反馈，并参考 Cataclysm / BOMD 附属缩短中英法术书描述。

- 工作树：C:/Users/57991/Documents/MinecraftDev/parallel-workspaces/aquamirae-first-abilities/Aquamirae-Nouveau；分支 agent/aquamirae-first-abilities。
- 实现提交：6290aafa76095de9e5cbbba1024f30335e263d36。
- 当时固定 Core：3eccd65fc569cd43ef960ef3eee77963ef201ac8，独立 ../Ars-Conflux；受版本控制文件未修改。
- NeoForge 1.21.1 / Java 21，Forge 1.20.1 / Java 17；原模组和 Ars 基线与 dev.2 相同。

## 最终行为与依据

实际 Aquamirae 7.2.11 JAR 的 AbstractHomingShard.canHitEntity 同时使用追踪条件筛选物理碰撞。AbyssalShard 的 Monster 检查排除了实现 Enemy、却不继承 Monster 的史莱姆和岩浆怪。

附属主碎晶把碰撞与追踪筛选分开：普通生物可被直接命中；追踪保留原生敌对目标，并补入 Enemy。原生伤害仍为 10 × 尺寸，未要求附加 Ars 伤害魔符。史莱姆无甲时受到 10 点伤害，岩浆怪的原生护甲会减伤。虚弱、声音、粒子、追踪运动与分裂仍调用本体。

描述改为一至两句，去除长篇来源和计费解释。来源仍可在配方、图标及 native-audit.md 查阅。

## 验证

- Neo 根目录：`:runGameTestServer :jar :sourcesJar --offline --console=plain`，16/16；日志 build/feedback-dev3-neo.log。
- 新增实际裸碎晶伤害测试覆盖闲置史莱姆、岩浆怪，另覆盖偏离原发射线的史莱姆追踪、牛和村民的实际碰撞与一次续接。
- Forge 仅重混淆并打包；本批没有重新执行 Forge GameTest。最近一次 Forge 服务端验证是 dev.2 的 6/6。
- 两版 verify_release.py 通过，未将资源路径检查标为客户端外观实测。

## 产物及部署

相对本工作树路径与 SHA-256：

| JAR | SHA-256 |
| --- | --- |
| build/libs/aquamirae-nouveau-0.1.0-dev.3-mc1.21.1-neoforge.jar | ef12e5b606788ffff5a76b8dae8ab3caf924f4cb7e625b37ba2f757ec588eee5 |
| build/libs/aquamirae-nouveau-0.1.0-dev.3-sources.jar | 074d13d3be9d5fab28a5d74ecc791938ad12df2f6ec783b09ee7cec22faeddbf |
| forge/build/libs/aquamirae-nouveau-0.1.0-dev.3-mc1.20.1-forge.jar | d45b257a6d701259149673325704a4f7c9c585f51bd23e35d05e934e7a51494a |
| forge/build/libs/aquamirae-nouveau-0.1.0-dev.3-sources.jar | e6af6b95055323cc9d6f79f67e19f350707eb809402edc9706788729b3ef8315 |

按用户授权，仅 Neo 本附属部署至 C:/Users/57991/Desktop/.minecraft/versions/1.21.1-万法归一/mods。旧 dev.2 备份于 build/deployment-backup/dev3，证据 build/deployment-proof-dev3.json；其余 161 个 JAR 部署前后哈希一致。未替换 Core 或其他模组，未部署 Forge、未推送。客户端需重启复测，agent 未启动或人工验证客户端。

后续用户新增友军过滤要求，单独在 dev.4 接入 Conflux Allies API；本记录只描述 dev.3 已交付行为。
