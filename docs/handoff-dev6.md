# Aquamirae & Nouveau 0.1.0-dev.6

用户要求继续核对可适配能力，暂不做食物来源的状态效果。本批完成两版武器满层碎裂伤害，保留后续候选核对记录；部署授权仍仅涵盖本附属。

## 范围与基线

- 附属工作树：C:/Users/57991/Documents/MinecraftDev/parallel-workspaces/aquamirae-first-abilities/Aquamirae-Nouveau；分支 agent/aquamirae-first-abilities。
- 实现提交 fd6f9a450fecf00da711a978c20a511bafbdfa69；本交接单独提交，不改变发行包。
- 固定 Core：b24f18d72e64a902ff6c966040b0b83ea3cc6226，detached 副本 C:/Users/57991/Documents/MinecraftDev/parallel-workspaces/aquamirae-first-abilities/Ars-Conflux-allies，版本 0.1.16。受版本控制文件只读；构建输出隔离。
- NeoForge 1.21.1 / 21.1.250 / Java 21 / Aquamirae 7.2.11 / Ars 5.13.3；Forge 1.20.1 / 47.4.0 / Java 17 / Aquamirae 7.1.14 / Ars 4.12.7；Fragmentum 5.2.0。最低 Core 继续为 0.1.16。
- 不修改 Core、其他附属、原模组或共享配置，不推送、公开发布、合并主工作树或部署 Forge。

## 最终实现与原生依据

| 魔符 | 调用的实际能力 | 条件／基础伤害 | 默认费用／冷却 |
| --- | --- | --- | --- |
| 碎甲迸裂 Armor Burst | ShatterspikeFullAbility.SHARED.modifyOutgoingDamage | 护甲碎裂默认满 5 层；基础 6 点 | 60 Mana／3 秒 |
| 碎命迸裂 Health Burst | ShatterbladeFullAbility.SHARED.modifyOutgoingDamage | 生命碎裂默认满 10 层；基础 4 点 | 60 Mana／3 秒 |

两项是 Tier 1 效果，无增强／延时支持。shared ShatterBurstGlyph 使用 CastContext 的原施法者筛选与付款、damageOwner 计算原武器能力并构建 Ars 伤害来源。平台 NativeShatterBursts 以原生 DamageContext(0) 调用真实能力、取 result，不复制阈值、伤害变量或原生声音。Forge 构建只转换 DamageContext 包名。

原武器对既有攻击追加伤害；魔符将这段增量提取为独立条件效果，不要求手持来源武器，也不消耗或重置碎裂层数。未满层的 0 结果不会送入伤害 API，法术伤害加成不能绕过条件。满足条件后用实际 Ars IDamageEffect.attemptDamage，保留法术伤害属性、Pre/Post 事件、原生 hurt 返回和实际伤害所有者。世界装备能力、护甲和其他本体战斗规则继续生效，6／4 是基础值而非无条件最终伤害。

未满层、友军、无效目标、失效施法者、空蓝、无敌帧拒绝和 Ars 伤害事件取消时不扣载荷费。普通 Ars 形态费用独立。后续效果仍由 Ars 正常执行，迸裂失败不会人为取消整条法术。

图标与抄写材料分别引用裂解尖刺 shatterspike、裂解之刃 shatterblade；配方仍为来源武器 ×1、魔源宝石 ×1、27 XP，武器会被消耗。中英描述各一句，资源路径来自实际原生 JAR。Neo 共 10 个魔符／配方，Forge 共 9 个，仍仅 Neo 有深渊碎晶。

两版 JAR 的全部武器／护甲 Ability 与主要生物入口已用 CFR 0.152 核对，输出只在忽略的 .upstream/dev6-neo/、.upstream/dev6-forge/。后续候选和暂缓原因在 native-audit.md：水下深度增伤、缺失生命增伤、击杀中毒目标后的武器再生；不把船长特有回血写成普遍吸血，不按三叉戟名称编造电击，也不直接调用会随机缩减主手物品的贪婪能力。Boss 动作散布于 AI／baseTick／动画，不通过隐藏生成 Boss 或复制局部算法接入。暂不新增食品状态，现有魔符不在本批删除范围。

## 验证

| 目录／平台 | 实际命令 | 结果／证据 |
| --- | --- | --- |
| 本工作树根目录，Java 21 | `./gradlew.bat :runGameTestServer :jar :sourcesJar --offline --console=plain` | Neo 31/31；build/dev6-neo.log |
| forge/，Java 17 | 同上；jar 完成后 reobfJar | Forge 17/17；forge/build/dev6-forge.log |
| 根目录发行检查 | `python tools/verify_release.py` 与 `--forge` | 双版二进制／源码包通过 |

新增四项测试覆盖满层边界、原生 6／4 基础伤害、不消耗层数、成功支付和无敌帧拒绝；外部友军提供者、空蓝、失效施法者；真实 Ars 触摸编排先叠 5 层再迸裂并续接，付款者与 damageOwner 不同仍保留归属；实际 Ars Pre 伤害事件取消不扣费。原有 Neo 27／Forge 13 项均通过。

初次测试编译缺少测试 Marker 的兼容增强抽象方法；补齐后真实续接用例仍失败。详细断言确认伤害所有者正确，失败是未注册的测试 Marker 默认未启用，补上测试用 enabled／tier／cost 后通过，生产接线没有因此改变。失败日志保留 build/dev6-neo-compile-first.log、build/dev6-neo-first.log、build/dev6-neo-diagnostic.log。Forge 用既有生成桥接适配 Mana Optional、耗魔查询和事件总线，首次完整验证通过。

固定 Core 副本 `git status --porcelain --untracked-files=no` 为空，HEAD 正确。测试服务已退出，无本任务残留的非 Gradle Java 进程。未启动客户端；实际法术书显示、图标和声音未人工验证，未运行完整共享模组环境。原生代码检查与服务端通过不等于客户端画面验证。

## 交付与部署

路径相对于上述附属工作树，Forge 二进制为最终重混淆包：

| JAR | SHA-256 |
| --- | --- |
| build/libs/aquamirae-nouveau-0.1.0-dev.6-mc1.21.1-neoforge.jar | 90043a98a25921ef052cc9a74d3912304eca4f3f4e8ae7289349828201609d68 |
| build/libs/aquamirae-nouveau-0.1.0-dev.6-sources.jar | 5f314f02fe2bf48836d5e5937417545741a5d318963c74014b14febe02a693a9 |
| forge/build/libs/aquamirae-nouveau-0.1.0-dev.6-mc1.20.1-forge.jar | e0081a61bd2d514cd62bb599ea1e889f30b4663017d258813c0774f48e603660 |
| forge/build/libs/aquamirae-nouveau-0.1.0-dev.6-sources.jar | 6b4001b1fdfbdb16551f20addb20aee251391f26a94ae8b497c9fe2198594f32 |

Neo dev.6 已部署 C:/Users/57991/Desktop/.minecraft/versions/1.21.1-万法归一/mods，只替换本附属 dev.5。旧包备份 build/deployment-backup/dev6；证据 build/deployment-proof-dev6.json。其余 161 个 JAR 哈希部署前后完全一致。
部署时目录已有其他任务安装的 Core 0.1.17，含 Allies API，SHA-256 6dde2ad9c72984c3bd3115db161b9c7e8f22884c524c6b09c05649e8152a5b46；本任务未替换它，构建仍使用固定 0.1.16。共同客户端环境未启动，须完整重启游戏后测试。

发行包仅含附属原创兼容代码、JSON 与原生资源路径引用，保留 MIT／THIRD_PARTY_NOTICES；无原生实现、素材、依赖 JAR 或测试数据。没有新增实体或 Mixin。

下一步优先人工测试 `弹射物 → 护甲碎裂 → 碎甲迸裂`，重复命中同一目标，在 12 秒状态时长内累积到五层；生命组合阈值十层。其他候选先按 native-audit.md 的真实条件做设计，不扩大食品状态范围；是否需要新的通用伤害扩展 API 交独立 Core 任务。

> 在上述附属 worktree 继续。先读 AGENTS.md、README、native-audit 与固定 Core 开发流程，核对 b24f18d72e64a902ff6c966040b0b83ea3cc6226。Core 受版本控制文件只读。暂不新增食物来源状态，弹射物优先替换，图标／获取尽量引用来源。只对新改动或明确疑点补验证；部署授权仅涵盖本附属指定 Neo mods 目录，不改 Core／其他模组和配置。
