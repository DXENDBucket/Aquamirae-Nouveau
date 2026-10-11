# Aquamirae & Nouveau 0.1.0-dev.5

用户授权继续适配其他生物或物品的效果，弹射物优先替换，图标和制作材料尽量来自明确原生来源。本批完成双版水母蜇刺、可怖环刃和迷宫玫瑰，并仅部署本附属至用户指定目录。

## 范围与固定基线

- 附属工作树：C:/Users/57991/Documents/MinecraftDev/parallel-workspaces/aquamirae-first-abilities/Aquamirae-Nouveau；分支 agent/aquamirae-first-abilities。
- 实现提交：4ff2eaa1a17fa805eb1a9da498719313296a8a4e；本交接单独提交，不影响发行包。
- Core 副本：C:/Users/57991/Documents/MinecraftDev/parallel-workspaces/aquamirae-first-abilities/Ars-Conflux-allies；detached 固定 b24f18d72e64a902ff6c966040b0b83ea3cc6226（0.1.16）。
- 两版最低 Core 均为 0.1.16；未修改 Core 受版本控制文件。旧 3eccd65 副本也保持不变。
- NeoForge 1.21.1 / 21.1.250 / Java 21 / Aquamirae 7.2.11 / Ars 5.13.3；Forge 1.20.1 / 47.4.0 / Java 17 / Aquamirae 7.1.14 / Ars 4.12.7；Fragmentum 5.2.0。
- 仅允许修改本附属；不修改其他模组、原模组、共享配置，不推送、公开发布或部署 Forge。

## 最终行为与接线

| 能力 | 原生来源与实际行为 | 抄写材料／图标 | 默认 Mana／冷却 |
| --- | --- | --- | --- |
| 水母蜇刺 | 深海水母接触毒素：中毒 III、失明，默认 60 tick；调用 AquamiraeHelper.doPoison 并保留归属 | 水母果冻 jellyfish_jelly，实际水母掉落 | 40／3 秒 |
| 可怖环刃 | 继承 TerribleChakram；伤害 3，中毒 I / 60 tick，穿透并飞回原生所有者 | terrible_chakram | 60／3 秒 |
| 迷宫玫瑰 | 继承 MazeRose；伤害 3，浮空 I / 10 tick，保留怪物追踪与飞回 | maze_rose | 70／3 秒 |

三项均为 Tier 1。水母蜇刺是效果，增强提高中毒等级至 V，延时延长两个状态。保留本体对水母／鮟鱇鱼、创造／旁观玩家的豁免；排除友军。原生毒素助手接受施加时结算，即使同级效果没有变化；没有额外直接伤害。

两个武器是 ProjectileForm 替换增强，写作 `弹射物 → 可怖环刃／迷宫玫瑰 → 后续效果`，仅支持加速。common/projectile/ 的 ChakramGlyph、ChakramCasting、ChakramControl、ChakramPayload 共享上下文、结算与受控轨迹接线。两个约 34 行的原生实体子类保留原生方法，只切换驱动、转发命中和同步控制模式；Forge 构建生成包名和同步 API 桥接。两版注册保留本体 1.2 × 0.6 碰撞尺寸与 64 追踪范围，更新间隔 1 tick。

普通武器成功生成时只扣一次载荷费。每次原生 hurt 成功后续接 Ars 效果；无敌帧拒绝不付费、不续接、不销毁回旋镖。重复形态按成功 hurt 结算。方块接触保留原生处理，不续接 Ars。普通武器最长 120 tick；受控形态用 ArsTrajectory，依原载体时长，上限 1200 tick。所有者／原施法者失效、未加载区块、到期、取消、重复触发空蓝或重新载入丢失控制器时清理。

RoseHomingMixin 只过滤附属 opt-in 实体的原生候选列表，不复制或改写追踪算法。普通原生玫瑰保持原候选；原生 Monster 范围不含史莱姆，史莱姆仍可直接命中。AlliedCarriers / AlliedCarrierMixin 将旧碎晶载体筛选移入 common，继续只登记本附属载体；载荷碰撞和追踪均按原施法者调用 Allies.areAllies，独立于 damageOwner。

三项配方均消耗来源物品 ×1、魔源宝石 ×1、27 XP，武器会正常消耗。模型运行时引用本体贴图，中英法术书描述简短。Neo 共 8 个魔符，Forge 共 7 个；Forge 仍无深渊碎晶。

核对依据为两版实际发布 JAR，依赖哈希在 dependencies.lock.json。CFR 0.152 的原生实体、助手、注册输出仅位于忽略的 .upstream/dev5-neo/、.upstream/dev5-forge/。客户端直接注册原生泛型 renderer，无复制源码、模型、图片或声音。详细来源见 native-audit.md。

## 验证证据

| 平台／工作目录 | 命令 | 最终结果与日志 |
| --- | --- | --- |
| Neo 根目录，Java 21 | `./gradlew.bat :runGameTestServer :jar :sourcesJar --offline --console=plain` | 27/27，build/dev5-neo-final.log |
| Forge 目录，Java 17 | 同上，jar 执行 reobfJar | 13/13，forge/build/dev5-forge-final.log |
| 最后文字调整的打包 | 两版 `:jar :sourcesJar --offline --console=plain` | build/dev5-neo-packaging.log；forge/build/dev5-forge-packaging-final.log |
| 发行检查，根目录 | `python tools/verify_release.py` 与 `--forge` | 两版二进制与源码包通过 |

新增七个场景覆盖原生水母毒素与豁免、增强延时、真实 Ars 替换与尺寸／伤害／状态／续接、原生玫瑰候选与友军过滤、重复载体只按有效 hurt 扣费、飞回、停滞到期与重新载入孤儿、空蓝生成失败和所有者失效。原有 Neo 20 项／Forge 6 项均通过，包含旧碎晶伤害、分裂、友军和受控轨迹回归。

实施期间纠正了 AbstractAugment 兼容增强方法访问级别。Forge 测试适配将 Ars 5 getExpendedCost 换为实际 Ars 4 expendMana 使用的 getResolveCost，保留结算断言。首次 Forge 实际飞行场景位于地下，目标受窒息且原生视线被地形阻挡；加入详细断言定位后将场景移至 136 高度，隔离其他场景。发行复核补齐原生碰撞尺寸，并重跑两版必要验证。Forge sourcesJar 显式依赖生成资源，修正 Gradle 输出依赖检查失败。早期日志保留在 build/dev5-neo.log、forge/build/dev5-forge-first.log、forge/build/dev5-forge-diagnostic.log 等。

Forge 最终包的 refmap 包含 canHitEntity → m_5603_ 和 Level.getEntitiesOfClass → m_45976_ 两项 SRG 映射。GameTest 开发环境缺少输出目录中的 refmap 警告不代表发行包缺失；最终包已检查。末次 UI 文字调整只重新打包并做资源检查，未无理由重复 GameTest。

两份 Core 副本 `git status --porcelain --untracked-files=no` 均为空；本次测试服务器已退出，无本任务残留的非 Gradle Java 进程。未启动客户端，模型、法术书实际显示、多人与其他附属共存环境未人工验证；服务端和资源检查不等于客户端画面验证。

## 产物与部署

下列路径相对于本附属工作树，Forge 二进制已重混淆：

| JAR | SHA-256 |
| --- | --- |
| build/libs/aquamirae-nouveau-0.1.0-dev.5-mc1.21.1-neoforge.jar | 51e34b845fc6177554c383d27114686f308fdfc404bbd944d14c41fa6e0dcf78 |
| build/libs/aquamirae-nouveau-0.1.0-dev.5-sources.jar | f58ae8c02724289810ebc9bc81ff36bd116bec336a102438532c9745ccdefb4c |
| forge/build/libs/aquamirae-nouveau-0.1.0-dev.5-mc1.20.1-forge.jar | c3c67c47382b3adca3e6eb4f85e3761a03837f294a6e6b8d860c2e57d1d79fb5 |
| forge/build/libs/aquamirae-nouveau-0.1.0-dev.5-sources.jar | 2b273fe8c1cc48cf17323f86df32842d5e9aacdecaa63ebe7cc39a38132b2966 |

Neo dev.5 已部署至 C:/Users/57991/Desktop/.minecraft/versions/1.21.1-万法归一/mods，仅替换本附属 dev.4。旧包备份 build/deployment-backup/dev5；部署证据 build/deployment-proof-dev5.json；其余 161 个 JAR 前后哈希完全一致。原先安装的 Core 0.1.16（SHA-256 2aad5052a2ed162408dd18f712aabb0c37e15f76f236c2473be69ce7eeea39ab）未替换。共同客户端环境未启动；需完整重启游戏。

附属二进制和源码包含 MIT、THIRD_PARTY_NOTICES，没有依赖 JAR、测试数据或原生实现／素材。未推送、未公开发布、未合并回主工作树。

下一步优先根据人工反馈确认两种原生模型与法术书显示；可用裸 `弹射物 → 可怖环刃／迷宫玫瑰` 检查伤害，玫瑰对非友军怪物检查追踪，`触摸 → 水母蜇刺` 检查状态。暂缓的结晶化、毒素免疫和吞噬诅咒仍保留原生差异，见 native-audit.md。

> 在上述附属工作树继续任务。先读 AGENTS.md、README、交接与固定 Core 开发流程，核对固定 b24f18d72e64a902ff6c966040b0b83ea3cc6226。不要改 Core 受版本控制文件或其他模组；部署授权仅限本附属指定 Neo mods 目录。只对新改动或明确疑点补必要验证，不重复已通过的矩阵，不把服务端通过称为客户端已验证。
