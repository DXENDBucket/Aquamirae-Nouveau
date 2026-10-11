# Aquamirae & Nouveau 0.1.0-dev.4

用户要求碎晶不锁定或碰撞友军，并明确接入 Conflux 新友军 API。本批只修改海灵附属，仍仅获准部署本附属至指定 NeoForge 人工测试目录。

## 范围与固定基线

- 附属工作树：C:/Users/57991/Documents/MinecraftDev/parallel-workspaces/aquamirae-first-abilities/Aquamirae-Nouveau；分支 agent/aquamirae-first-abilities。
- 实现提交：b97e346a9908043bc0ee596f27b078c18a131abd；后续仅补本交接提交信息，不改变发行包。
- 新 Core 构建副本：C:/Users/57991/Documents/MinecraftDev/parallel-workspaces/aquamirae-first-abilities/Ars-Conflux-allies；detached 固定 b24f18d72e64a902ff6c966040b0b83ea3cc6226（0.1.16）。
- 用户确认新 API 已本地提交后才创建该副本，两平台 settings.gradle 指向此副本。旧 3eccd65 副本保留，workspace.json 记录迁移理由和原基线；两个副本受版本控制文件均未改变。
- NeoForge 1.21.1 / Java 21 / Aquamirae 7.2.11 / Ars 5.13.3；Forge 1.20.1 / Java 17 / Aquamirae 7.1.14 / Ars 4.12.7。
- Neo 运行时最低 Core 改为 0.1.16；Forge 不包含碎晶，四个状态魔符仍允许 0.1.15。没有修改 Core、其他附属、原模组或共享配置。

## 最终实现

- SpellAbyssalShard 使用 `Allies.areAllies(originalCaster, candidate)`，排除友军追踪与直接碰撞。原施法者独立于 native owner / damageOwner，未把友军目标变成所有者或付款者。
- Conflux 负责队伍、所有权及扩展提供者；附属不复制这些规则。中立和被动生物仍可被直接击中。伤害继续走本体 `10 × scale` 和原生虚弱。
- ShardSplitMixin 仅匹配 SpellAbyssalShard 父实体，替换本体 onBreak 调用的子实体工厂，传递原施法者。数量、随机运动、尺寸、声音、粒子仍由本体 onBreak 处理，普通 Aquamirae 碎晶仍调用原工厂。
- 三个尺寸 0.5 的子碎晶同样排除友军，并保留 5 点原生基础伤害、虚弱和 200 tick 生命周期；不续接 Ars、不再次扣费。它们使用同一附属 EntityType 和原生 renderer。原施法者失效或重新加载丢失运行时上下文时移除。
- ShardCarriers 仅登记驱动本附属载荷的离线 Ars 载体，使用弱引用；Flight.close 清除登记。ShardCarrierMixin 排除其友军碰撞，普通 Ars 弹射物不登记。命中回调另做防御性筛选，友军不触发载荷、续接或重复形态费用。
- 新增钩子只在 Neo 注册；Forge 不包含这些类或 Mixin 配置。verify_release.py 验证 API 最低版本、钩子打包及 Forge 排除。

本体分裂工厂和 Ars 碰撞入口均根据实际本地发布 JAR 的 javap 字节码核对，检查输出位于忽略的 build/native-shard-bytecode.txt、build/ars-projectile-bytecode.txt 和 build/native-homing-bytecode.txt。没有复制本体伤害、追踪、分裂算法或资源。

## 验证证据

| 平台 | 在本工作树执行的命令 | 结果 |
| --- | --- | --- |
| Neo 根目录，Java 21 | `:runGameTestServer :jar :sourcesJar --offline --console=plain` | 20/20，build/feedback-dev4-neo.log |
| Forge 目录，Java 17 | 同上，jar 完成后执行 reobfJar | 6/6，forge/build/feedback-dev4-forge.log |
| 根目录发行检查 | `python tools/verify_release.py`；加 `--forge` | 双平台二进制／源码包通过 |

四项新增场景覆盖：队伍友军怪物的追踪与穿透、原施法者宠物在伤害所有者不同情况下免伤、API 外部提供者对分裂子碎晶的保护与子碎晶原生载荷、受控 Ars 实际碰撞跳过友军且不扣费或续接。普通 Aquamirae 原生分裂产生原类型的三个子碎晶；普通 Ars 未登记载体保持原样。

原有裸碎晶史莱姆／岩浆怪伤害、史莱姆追踪、被动目标续接、原生生命期、真实环绕、重复计费、空蓝和失效清理全部通过。

首次测试编译错误是新用例的 CarrierProfile 参数顺序，已按固定 API 签名修正。随后两项友军用例失败；隔离并发追踪场景、给友军僵尸戴头盔以排除日照伤害后全部通过，筛选实现没有因这两项失败更改。首次运行日志保留为 build/feedback-dev4-neo-first.log。

两份 Core 副本 `git status --porcelain --untracked-files=no` 为空。测试服务器已退出，没有本任务残留的非 Gradle Java 进程。未启动客户端；新子碎晶显示、多人画面和实际 Goety 仆从类型仍需人工确认，API 扩展测试不代表 Goety 全矩阵实测。

## 产物与部署

以下路径相对于附属工作树，均为最终发行包或源码包：

| JAR | SHA-256 |
| --- | --- |
| build/libs/aquamirae-nouveau-0.1.0-dev.4-mc1.21.1-neoforge.jar | 7929bb9897a1c5cadbad0dea99e2e0e129820abe304e63737ff2ec2db94e4c8c |
| build/libs/aquamirae-nouveau-0.1.0-dev.4-sources.jar | 47bf796587371e1efc1b16597fd7fb1aacc604beb1195890fc039dc6da5bdf0e |
| forge/build/libs/aquamirae-nouveau-0.1.0-dev.4-mc1.20.1-forge.jar | d6b4cced0afacd78d438e20ca359e919271be488c72a1ec41d1e28e7469e9739 |
| forge/build/libs/aquamirae-nouveau-0.1.0-dev.4-sources.jar | 6e5ef60cda80841d4d08515cecb7e21a10b95ecd9c90234a5f126e39ae5b61de |

Neo dev.4 已部署至 C:/Users/57991/Desktop/.minecraft/versions/1.21.1-万法归一/mods，仅替换本附属 dev.3。备份 build/deployment-backup/dev4；证据 build/deployment-proof-dev4.json。其他 161 个 JAR 部署前后哈希一致。

部署前检查测试目录已经由 Core 任务安装的 Conflux 0.1.16，含实际 Allies.class，哈希 2aad5052a2ed162408dd18f712aabb0c37e15f76f236c2473be69ce7eeea39ab；本任务未替换它。未部署 Forge、未推送或公开发布。客户端需完整重启复测。

只打包附属原创适配代码与资源路径引用，MIT / THIRD_PARTY_NOTICES 保留；无原模组代码、图片、声音、依赖 JAR 或测试数据。dev.3 历史交接补齐在 handoff-dev3.md。

> 在上述附属工作树继续任务。先核对 AGENTS.md、固定 Core b24f18d72e64a902ff6c966040b0b83ea3cc6226 和 workspace.json。不要改 Core 受版本控制文件或其他附属，不重跑已通过矩阵；只对新增改动或明确疑点补验证。部署授权仅涵盖本附属指定 Neo mods 目录，其他模组和配置不能修改。
