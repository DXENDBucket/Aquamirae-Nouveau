# Aquamirae & Nouveau 0.1.0-dev.7

用户授权实现鳗鱼咆哮、吸血咬击与船长旋流，并明确不做船长生命掠夺。继续暂缓食物来源状态；不删除既有魔符。部署授权仅涵盖本附属指定 NeoForge mods 目录。

## 范围与固定基线

- 附属工作树：C:/Users/57991/Documents/MinecraftDev/parallel-workspaces/aquamirae-first-abilities/Aquamirae-Nouveau；分支 agent/aquamirae-first-abilities。
- 实现提交：bc178b842835cf3d26e640683c108102784b0dbd；本交接单独提交，不改变发行包。
- 固定 Core：b24f18d72e64a902ff6c966040b0b83ea3cc6226，版本 0.1.16，detached 副本 C:/Users/57991/Documents/MinecraftDev/parallel-workspaces/aquamirae-first-abilities/Ars-Conflux-allies。源码只读，构建目录独立。
- NeoForge 1.21.1 / 21.1.250 / Java 21 / Aquamirae 7.2.11 / Ars 5.13.3；Forge 1.20.1 / 47.4.0 / Java 17 / Aquamirae 7.1.14 / Ars 4.12.7；两版 Fragmentum 5.2.0。
- Aquamirae JAR 哈希与锁一致：Neo 5b8b2d04e16755d354adf3dfa77f906e33509722e647b1b5fdd5ccebabc8a35e；Forge 5439edc57af3075a1600effe0906ba322e89346077bcb80b761a4bff4cbff59b。
- 不修改 Core 受控文件、其他附属、原模组或共享配置；不推送、发布、合并主工作树或部署 Forge。

## 最终实现

新增三项 Tier 2 效果；Neo 共 13 个魔符／配方，Forge 共 12 个。中英描述保持简短，增强说明使用 Ars 实际翻译键。

| 魔符 | 默认行为 | 增强 | 费用／冷却 | 抄写来源 |
| --- | --- | --- | --- | --- |
| 鳗鱼咆哮 | 自身或友军获得 5 秒咆哮，前方 90 度、约 12.25 格可见敌人受到水平排斥、缓慢 II、黑暗与冻结累积 | 扩散每级范围 +2；延时每级 +10 秒 | 首费 40，后续每秒 4；冷却 5 秒 | 死海卷轴 dead_sea_scroll |
| 吸血咬击 | 命中敌方实体，基础伤害 8、中毒 III／5 秒，按实际生命减少两倍治疗伤害所有者 | 强化每级伤害 +2；延时每级中毒 +10 秒 | 70；冷却 3 秒 | 锋利骨头 sharp_bones |
| 船长旋流 | 自身或友军获得 10 秒旋流，附近敌人趋向半径 5 格的旋转位置，到期向外推；默认最大生命门槛 100 | 强化每级生命门槛 +50，最多 1000；扩散每级查询范围 +2，最多 32；延时每级 +10 秒 | 首费 80，后续每秒 8；冷却 10 秒 | 冰冻钥匙 frozen_key，船长掉落物刚需 |

全部配方消耗上述来源物品 ×1、魔源宝石 ×1、27 XP；魔符图标引用相同来源。增益状态图标通过 assets/minecraft/atlases/mob_effects.json 引用本体物品纹理，无 PNG 复制。强化另外支付既有配置的每级 10 Mana，延时／扩散的 Ars 费用由 Ars 处理。

CreatureAuraGlyph／CreatureAuras 使用 Core CastContext、Allies、ResourceCharge 和 ExecutionSession。选中实体只负责位置与朝向，不变成付款者或友军判断来源；原施法者账户保留，实际伤害所有者单独保留。首费预付首秒，之后每秒预付续费；默认完整咆哮载荷费用 56、旋流 152，未含形态与增强。重复施加同项不会叠加、刷新或再次付款。

增益标记可净化，状态移除、施法者或执行者失效、关系变为非友军、换维度、区块卸载、禁用和续费失败时关闭会话并清理状态。到期前保留一帧标记，避免实体 tick 顺序吞掉旋流最终排斥。实体加入世界时清理孤立标记；同 UUID 的新实体不会继承旧会话。执行会话不跨存档恢复。

EelBiteGlyph 经 Ars IDamageEffect.attemptDamage 处理属性、Pre/Post 事件与世界伤害，使用 damageOwner 构造来源。吸收护盾与过量伤害不计入回血；友军、事件取消、无敌帧拒绝和空蓝不治疗／付款。成功伤害后调用原生 AquamiraeHelper.doPoison，保留原生豁免。它是直接命中效果，无持续增益或隐藏攻击实体。

配置新增 creatureActions：biteDamage 默认 8、roarUpkeep 默认 4、vortexUpkeep 默认 8；既有配置提供三项初费、时长、冷却与启用开关。AquamiraeCosts 优先使用 CastContext 已有账户，避免选中的执行者被误当付款者。两个平台各注册自己的 AuraEffects，并在服务器实体加入时清理孤立标记；无新增实体或 Mixin。

## 原生依据与明确差异

实际两版 Eel 的回血位于 baseTick 动画攻击帧，doHurtTarget 仅负责近战与毒素；咆哮位于 baseTick 的当前目标处理。CaptainCorneliaBoss 旋流位于浮空阶段 tick，没有完整独立动作方法。原生依据与筛选记录见 native-audit.md，解编输出仅在忽略的 .upstream/。

本批是原创玩家法术适配，复用原生状态、毒素助手、声音和 GHOST 粒子，不复制 AI／动画代码，也不生成隐形 Boss。咆哮改为面向群体的持续增益，附属提供白色气流粒子；不覆盖敌人原有 AI 目标。旋流查询使用原生包围盒向各轴扩张形式，并添加视线、友军和存活筛选；不调用 rage()，不附送船长回血、浮空、阶段、闪电、破船者或遭遇事件。正常到期向外推，净化／失效取消直接停止。船长生命掠夺未实现。

## 验证证据

| 目录／平台 | 实际命令 | 结果／日志 |
| --- | --- | --- |
| 附属根目录，Java 21 | ./gradlew.bat :runGameTestServer :jar :sourcesJar --offline --console=plain | Neo 39/39；build/dev7-neo.log |
| 附属根目录，Java 21 | ./gradlew.bat :jar :sourcesJar --offline --console=plain | 最终包；build/dev7-package-neo.log |
| forge/，Java 17 | ./gradlew.bat :runGameTestServer :jar :sourcesJar --offline --console=plain | Forge 25/25；forge/build/dev7-forge.log，含 reobfJar |
| 附属根目录 | python tools/verify_release.py；加 --forge | 两版二进制／源码包全部通过 |

新增 8 项 GameTest：实际失血回血与原所有者／付款者、吸收／过量伤害／事件取消／友军／伤害拒绝、咬击增强与延时、友军执行者与咆哮朝向、持续扣费和净化停止、首秒预付后空蓝停止、旋流生命门槛／范围／时间、到期向外排斥与死亡清理、重复授予／孤立标记／同 UUID 重载清理。原有 Neo 31／Forge 17 项通过。

首轮 Neo 有两项失败：旧测试仍按旧魔符总数断言，吸收测试在 1.21.1 未同时添加吸收状态；修正测试后通过。另一次启动发现 Ars 构造期间会调用增强说明方法，不能访问尚未初始化的子类 ability 字段，已改为注册名。Forge 编译发现 Ars 4 没有 addAugmentDescriptions，移除这四处方法的 @Override，保持 Ars 5 隐式覆盖；仅注解／注释变化后重新生成 Neo 最终包，未无理由重跑相同服务端测试。

发行检查覆盖依赖锁、抄写配方／船长钥匙刚需、中英法术书键、来源图标和状态图集、许可、友军引用、平台 Mixin 类和 Forge refmap。未包含原生实现、图片、音频、依赖 JAR、测试代码或测试世界。

固定 Core 副本 git status --porcelain --untracked-files=no 为空，HEAD 正确。两次测试服务已自然退出；未关闭其他人的 JVM／Gradle。没有启动客户端，法术书、状态图标、声音、气流／幽灵粒子和旋流运动观感未人工验证；完整共享模组环境未运行。不把服务端与路径检查称为客户端外观已验证。既有普通 Aquamirae 实体没有新增全局行为钩子，本批未人工回归本体怪物 AI。

## 交付与部署

路径相对于上述附属工作树；Forge 二进制为最终重混淆包：

| JAR | SHA-256 |
| --- | --- |
| build/libs/aquamirae-nouveau-0.1.0-dev.7-mc1.21.1-neoforge.jar | dfb1d88d56a1b345db052bca68c35452fceb541d2e09322935e4d87c9fc7e34c |
| build/libs/aquamirae-nouveau-0.1.0-dev.7-sources.jar | 470fb0f8dddac6873a4ec6287afdabb45f86529d30e9d2bbc5c6cb95618efd7d |
| forge/build/libs/aquamirae-nouveau-0.1.0-dev.7-mc1.20.1-forge.jar | 66cb6b9faee541f98fadb354e30846fd6f771b2d6a2c903831e97fa8caf1a8df |
| forge/build/libs/aquamirae-nouveau-0.1.0-dev.7-sources.jar | 4be648195a79e5a4cf557c08a883fad21347139f443a1ac214555957b308fcfc |

Neo dev.7 已部署 C:/Users/57991/Desktop/.minecraft/versions/1.21.1-万法归一/mods，仅替换本附属 dev.6；旧包已核对备份到 build/deployment-backup/dev7。只存在一个本附属 modid，部署后哈希与上表一致。其余 161 个 JAR 哈希部署前后完全一致，证据 build/deployment-proof-dev7.json。

目录已有其他任务安装的 Core 0.1.17，SHA-256 6dde2ad9c72984c3bd3115db161b9c7e8f22884c524c6b09c05649e8152a5b46，本任务未替换。构建仍固定 0.1.16，最低依赖 [0.1.16,0.2)。源码和二进制许可仍为附属 MIT／THIRD_PARTY_NOTICES 原生引用边界。

下一步人工完整重启客户端，测试：自身 → 鳗鱼咆哮；弹射物 → 吸血咬击；自身 → 船长旋流，并分别加入强化、延时、扩散。旋流可比较最大生命 100 与 150 的目标；未增强不应牵动 150，增强一级可以。再检查友军承载、净化立即停发、续费不足停发与状态图标显示。

> 在上述独立附属工作树继续。先读 AGENTS.md、README、native-audit.md 与本交接，固定 Core b24f18d72e64a902ff6c966040b0b83ea3cc6226 的源码只读。暂不新增食物状态，不做船长生命掠夺；有常规装备来源的候选暂缓。只对新变更或明确疑点补必要验证。部署授权仅涵盖本附属到指定 Neo mods 目录，不替换 Core、其他模组或共享配置；无推送／发布授权。
