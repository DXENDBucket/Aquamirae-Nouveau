# 原生能力核对

开发基线：NeoForge Aquamirae 7.2.11 / Forge 7.1.14，Fragmentum 5.2.0。
检查的是发行 JAR 实际字节码，辅助源代码 https://github.com/ObscuriaLithium/Aquamirae 的 1.21.1 分支不能代替发行版。
精确 JAR 哈希见根目录与 forge/dependencies.lock.json；反编译结果只保存在忽略的 .upstream/。

## 共享状态

两版 `dev.obscuria.aquamirae.registry.AquamiraeEffects` 实际字段为 `fragmentum.api.registry.Deferred<MobEffect>`，与当前 GitHub 上不同的 `DeferredMobEffect` 不能混用。
附属通过 Minecraft 实际注册表读取原生效果，NeoForge 使用 Holder，Forge 使用 MobEffect。
伤害、属性注册、状态同步、效果结束后的属性移除仍由本体与 Minecraft 管理。未添加全局 Mixin 或自定义状态实体。

- ARMOR_SHATTER：原生 -0.1 × 等级的 MULTIPLY_TOTAL / ADD_MULTIPLIED_TOTAL 护甲修饰；ShatterspikeBaseAbility 原生 12 秒、5 层上限。
- HEALTH_SHATTER：原生 -0.05 × 等级的生命上限修饰；ShatterbladeBaseAbility 原生 12 秒、10 层上限。Forge 使用原生 HealthBoostMobEffect，NeoForge 使用 MobEffect，健康值处理保持各自原生实现。
- FURY_OF_THE_DEPTHS / RESILIENCE_OF_THE_DEPTHS：原生深海属性 +0.1 × 等级的基础乘法修饰；附属持续时间默认 30 秒，上限 5 级。这两项的最终战斗语义由 AquamiraeCombatRules、装备能力和本体事件处理，附属不重新计算战斗伤害。
- 自身、触摸、弹射物目标走 Ars 原有 HitResult；四种状态均只对仍存活且位于当前执行世界的 LivingEntity 触发。没有适用目标、空蓝、原生 addEffect 返回 false 时不支付载荷费。
- 延时与增强是附属接线，不宣称原生武器本来就支持 Ars 增强。自然消退、移除、死亡和保存由原生 MobEffectInstance 管理，没有额外执行会话或静态实体引用。

## NeoForge 原生深渊碎晶

实际 JAR 中的 `common.entity.projectile.AbyssalShard`、`AbstractShard` 与 `AbstractHomingShard` 核对了构造参数、SCALE / HOMING_STRENGTH 同步数据、原生运动和命中方法、持久化字段及原生渲染器。
dev.2 使用附属注册的 SpellAbyssalShard 子类，继承本体完整载荷，并直接使用本体 AbyssalShardRenderer。普通本体 EntityType 和实现不修改，没有隐藏 Boss、复制渲染器或重复驱动位置。

- owner 为 Conflux CastContext.damageOwner，目标不转移归属；生成前要求所有者仍活着且处于执行世界。
- 原生追踪距离 16，原本针对 Monster 或正攻击所有者的 Mob；dev.3 为附属主碎晶补入 Enemy 接口（史莱姆、岩浆怪），并将碰撞筛选和追踪筛选分开。主碎晶可直接命中普通生物，但不会主动追踪被动生物；原生视线检测与创造/旁观者排除保持。
- 速度 0.8 是附属发射参数；每个增强增加 0.2 原生尺寸，上限 2。伤害直接走原生 `10 * scale`，虚弱 I 持续 50 tick。
- 尺寸大于 0.75 的碎晶破裂时生成三个尺寸 0.5 的原生子碎晶；子碎晶保持所有者，不再分裂。全程本体声音、物品粒子和 renderer，图像效果仍需客户端人工验证。
- 普通飞行碎晶原生最大生存期 200 tick；Ars 受控形态依其载体时长，上限 1200 tick。执行会话在所有者失效、空蓝、未加载区块等情况清理附属载荷；原生分裂子碎晶保留自己的 200 tick 生命周期。
- dev.2 是 Conflux ProjectileForm 替换增强。主碎晶命中续接 Ars 效果，子碎晶只保留原生伤害与虚弱。普通弹射物使用原生追踪运动；环绕等受控形态使用 ArsTrajectory，实体自身两端停止原生运动/碰撞。重复载体保留自身且按命中扣费，不在命中时分裂。
- Forge 7.1.14 的完整类清单没有 AbyssalShard / AbstractShard / AbstractHomingShard。未自行移植或伪装成双版功能。

## 明确来源与获取（dev.2 补核）

dev.4 补充：附属主碎晶及三个半尺寸子碎晶通过 Conflux 0.1.16 的 Allies.areAllies，以原施法者排除友军追踪和碰撞。分裂仍完整调用本体 onBreak，通过仅匹配 SpellAbyssalShard 的工厂钩子传递友军上下文；本体选择数量、随机速度与尺寸，普通原生碎晶仍走原工厂。子碎晶没有 Ars resolver、续接或额外扣费。受控 Ars 载体的碰撞钩子仅对本附属登记的载体生效；命中回调也防御性拒绝友军。本附属不复制友军规则、不硬依赖其他附属、不修改 Core。

补核两版实际发布 JAR 的 AquamiraeItems 注册和 NeoForge DreadwakeTier2Ability 字节码。反编译结果位于忽略的 .upstream/feedback-decompiled/；GitHub 源码只辅助阅读。

| 能力 | 原生来源 | 附属图标和抄写材料 |
| --- | --- | --- |
| 护甲碎裂 | ShatterspikeBaseAbility 命中叠层 | 裂解尖刺 shatterspike |
| 生命碎裂 | ShatterbladeBaseAbility 命中叠层 | 裂解之刃 shatterblade |
| 深海之怒 | 原生食品效果 | 鮟鱇鱼精华 esca |
| 深海之韧 | 原生食品效果 | 熟棘鱼 cooked_spinefish |
| 深渊碎晶 | 破晓之惧 DreadwakeTier2Ability：击杀后发出三个原生碎晶 | 破晓之惧 dreadwake，仅 NeoForge |

两版实际食品注册：esca 深海之怒 I/3600 tick；esca_skewer III/9600 tick；cooked_spinefish 深海之韧 II/3600 tick；sea_casserole III/9600 tick；sea_stew III/6000 tick；poseidon_breakfast 同时深海之怒 IV、深海之韧 IV/12000 tick。buff 的附属默认 30 秒是魔符参数，不是原料理时长。

NeoForge 本体 HookLivingEntity 按 affected_by_depths_fury / affected_by_depths_resilience 标签筛选最终战斗规则，发行数据默认引用 ice_maze_creatures。深海之怒改善对该标签生物的本体伤害倍率，并供武器能力计算；深海之韧在该标签攻击者造成的伤害中改善有效护甲，并降低本体暴击概率与倍率。不能将 +10% 属性写成所有伤害提高 10% 或统一减伤 10%。

所有配方仍用原生抄写台：来源物品 ×1、魔源宝石 ×1、27 XP；包含武器的来源物品会被正常消耗。模型只运行时引用同一来源物品纹理，没有复制资源。

## 双版水母蜇刺与回旋镖（dev.5）

再次反编译两版实际发布 JAR 中的 AquamiraeHelper、AbyssalScyphoid、AbstractChakram、TerribleChakram、MazeRose，并核对实体注册、物品、掉落表及客户端 renderer。CFR 0.152 输出位于忽略的 `.upstream/dev5-neo/` 和 `.upstream/dev5-forge/`，没有打包或复制原生实现。

| 魔符 | 原生入口与行为 | 图标／抄写材料 |
| --- | --- | --- |
| 水母蜇刺 | AbyssalScyphoid 接触攻击：doPoison 60 tick / amplifier 2，失明 60 tick / 0，SQUID_SQUIRT 声音 | 深海水母实际掉落水母果冻 jellyfish_jelly |
| 可怖环刃 | TerribleChakram，原生 3 点伤害；成功 hurt 后中毒 I / 60 tick | terrible_chakram |
| 迷宫玫瑰 | MazeRose，原生 3 点伤害；成功 hurt 后浮空 I / 10 tick 与原生声音 | maze_rose |

水母毒素调用两版公开的 `AquamiraeHelper.doPoison`，它排除创造／旁观玩家、添加中毒并设置 lastHurtByMob。水母原生入口另排除 AbyssalScyphoid 与 Anglerfish；附属保留这些豁免，并补接失明和声音，不生成隐形水母。助手对接受施加的生物返回 true，即使原生 addEffect 未改变效果；计费遵循这个返回值。中毒默认 III，增强最高 V，延时同时延长两个状态；没有额外直接伤害。通过 Allies.areAllies 排除原施法者的友军。

回旋镖两版类包名不同（Neo 为 common.entity.projectile，Forge 为 common.entity），但本批所用原生算法一致。共同基类调用 ProjectileUtil 碰撞，再 applyMovement(computeMovement)；不在命中后销毁，允许穿透。原生排除所有者并检查所有者视线。5 tick 后逐渐飞回所有者，回转时长分别 20 / 40 tick，靠近所有者 1 格时移除。原生没有统一最长存活期。

附属只继承完整原生类，通过短小的控制接线补入执行会话与 Ars 续接。两版实体注册均保留原生 1.2 × 0.6 碰撞尺寸和 64 追踪范围，更新间隔设为 1 tick 配合受控轨迹同步。普通飞行最多 120 tick；Ars 受控形态使用 ArsTrajectory，依载体时长并限制 1200 tick，两端停用原生驱动。同步模式字段，客户端注册原生泛型 renderer；没有复制模型、贴图、动画或声音。Forge 的实体同步字段写法由构建生成桥接，公共控制与魔符逻辑放 common/。

迷宫玫瑰原生私有 computeMonsterHoming 仅查询 10 格膨胀包围盒内的 Monster，选择最近候选并按原生时间变化调整运动。专用 Mixin 只对实现 ChakramPayload 且配置控制器的附属实体过滤友军与已死亡候选；普通 MazeRose 返回原候选列表。没有重写追踪算法或把史莱姆加入原生候选，史莱姆仍可直接命中。所有新载荷和受控 Ars 载体均用 Conflux 原施法者友军判断。

普通回旋镖成功生成时扣一次载荷费，穿透伤害不再扣费；重复形态只在原生 hurt 成功时结算并续接。受无敌帧拒绝的伤害不付费、不续接、不销毁武器。方块接触保留原生处理，不续接 Ars。仅支持加速，未把原生武器伤害或短状态包装成可增强数值。区块未加载、原施法者失效、到期、取消或重新加载缺失上下文时移除，不跨存档持有 resolver。

旧 ShardCarriers／ShardCarrierMixin 合并为 shared AlliedCarriers／AlliedCarrierMixin，登记只涉及本附属受控载体，普通 Ars 不受影响。Forge 两个钩子的 Minecraft 调用按 refmap 映射到 SRG；发行检查同时核对钩子类、refmap 与最低 Core 0.1.16。

## 武器完整能力与后续候选（dev.6）

本轮按用户要求暂不新增食物来源的状态效果。核对两版实际 JAR 的全部 weapon / armor Ability，以及 Eel、Maw、MazeMother、CaptainCorneliaBoss、Spinefish、TorturedSoul；CFR 输出位于忽略的 `.upstream/dev6-neo/`、`.upstream/dev6-forge/`。没有由名称、tooltip 或 GitHub 分支推测实现。

| 原生能力 | 两版实际行为与接入判断 |
| --- | --- |
| ShatterspikeFullAbility | 目标 ARMOR_SHATTER 达到 ShatterspikeBaseAbility 的 max_stacks（默认 5）后 DamageContext.add(bonus)，默认 6，并播放紫水晶破裂声；接入为碎甲迸裂 |
| ShatterbladeFullAbility | HEALTH_SHATTER 达到默认 10 层后增加默认 4 点，保留相同声音；接入为碎命迸裂 |
| TidepiercerAbility | 仅实际伤害所有者 isUnderWater 时按海平面以下深度每格 2% 提高伤害，上限额外 200%；可作为后续伤害增强候选，需要核对 Ars 伤害范围与原装备叠加 |
| TerribleFangBaseAbility | 缺失心数提高伤害，Neo 数值来自本体配置，Forge 为原生常量；满血目标是否翻倍取决于原生完整能力是否在装备集合激活，不能仅按“满血翻倍”无条件接入 |
| RemnantSaberAbility | 对 ice_maze_creatures 使用玩家本体长期击杀加成，最大值来自配置；回血仅 Captain Cornelia 实体，不能把它写成普遍吸血 |
| TempestTridentAbility | 虽声明 duration / shock_duration，实际 modifyOutgoingDamage 是水下乘以 1 + duration；未出现通用电击或闪电入口，暂缓，不能凭名称编造雷击 |
| DaggerOfGreedAbility | 概率产出绿宝石，同时有概率直接缩减 self 主手堆叠；不适合直接调用为魔符，可能损坏施法工具 |
| TerribleBladeFullAbility | 击杀已中毒目标后给 self 再生，来自装备而非食物；Neo 回调带 DamageSource，Forge 回调不带；可考虑后续击杀增强，需要实际击杀时序与结算设计 |
| AbyssalArmorFullSetWithHeaumeAbility | 32 格内把正在攻击其他玩家的 Mob 改为以 self 为目标；直接全域调用会覆盖友军与玩家队伍目标，需 opt-in 候选过滤后再适配 |
| AbyssalArmorFullSetWithTiaraAbility | 原生死亡取消、恢复满血、结晶化，涉及特殊伤害豁免与事件；沿用结晶化双版差异暂缓 |
| SalvagerArmorFullSetAbility | 此 Ability 类只有描述和变量集合，没有可直接调用的完整攻击入口，需查真实事件／装备实现 |
| 鳗鱼与船长攻击 | 特殊吸血、拉扯与群体动作分布于实体 baseTick、AI、动画和 Boss 状态；直接隐藏生成 Boss 或复制局部逻辑均不采用 |

新 shared ShatterBurstGlyph 只接适用且存活的实体，排除 Conflux 原施法者友军。平台 NativeShatterBursts 以原生 DamageContext(0) 调用实际完整能力并取 result；没有复制阈值或额外伤害公式，也不重放武器基础能力或消耗状态。两版唯一桥接差异为 DamageContext 包名。
原武器是对既有攻击追加伤害；魔符将这段增量作为独立条件伤害效果。未满层返回 0，甚至法术伤害加成也不能绕过条件。满层后使用实际 Ars IDamageEffect.attemptDamage 和 GENERIC_SPELL_DAMAGE 保留加成、Pre/Post 事件、原生伤害拒绝与伤害所有者；付款者仍为原施法者。资源触发只对伤害成功返回 true。原生装备能力继续由世界伤害管线处理，不抑制它们，不宣称基础 6／4 是无条件最终伤害。
两项魔符不支持增强或延时，默认 Tier 1、60 Mana／3 秒冷却，配置只增加费用与冷却条目。来源图标／抄写材料为对应原武器，仍是来源 ×1、魔源宝石 ×1、27 XP。没有新实体或 Mixin。

## 剩余弹射物与效果考察（2026-10-12，仅研究）

用户要求考察剩余弹射物替换或 effect，继续暂缓食物来源状态效果。本节是候选设计与接入判断，没有新增魔符、提高 VERSION、构建或部署。
沿用独立附属工作树 `parallel-workspaces/aquamirae-first-abilities/Aquamirae-Nouveau`，分支 `agent/aquamirae-first-abilities`，研究前 HEAD 为 `53543514d2ff8909febcef8d25a2b07d9b8dcbc9`。固定 Core 仍为独立 `Ars-Conflux-allies` 的 `b24f18d72e64a902ff6c966040b0b83ea3cc6226`，检查受版本控制文件无变化。

### 弹射物盘点

发行 JAR 的全部原生 projectile 类、EntityType 注册以及全部 common/item 类再次核对。两版共同的完整自定义载荷只有 TerribleChakram、MazeRose；Neo 另有 AbyssalShard，三者已经接入。AbstractChakram、AbstractShard、AbstractHomingShard 是基类，不算新的独立能力。

`tempest_trident` 和 `tidepiercer` 在 Neo 的 AquamiraeItems 注册为 SwordItem，在 Forge 注册为继承 SwordItem 的 AquamiraeSwordItem；没有原版 ThrownTrident 的发射入口。两版 TempestTridentAbility 的实际实现都是水下将伤害乘以 `1 + duration`（默认 duration 20），没有 tooltip 所说的超导状态、链式放电和电击入口。不能按武器名称制造“原生三叉戟替换”或把描述中的未实现能力补写成联动。
ShellHornItem 生成的是 Captain Cornelia Boss 及其遭遇事件，也不是弹射物。

结论：当前两版没有找到尚未接入、适合继续做完整原生替换的独立弹射物。后续优先 effect 与条件伤害增强。

### 候选优先级与类别

以下名称为附属候选名，资源与材料均指原模组真实来源；持续时间、费用和魔符等级尚未确定。

| 顺序 | 候选 / 来源 | 原生行为 | 建议类别与接入判断 |
| --- | --- | --- | --- |
| 1 | 挑衅 / 深海重盔 `abyssal_heaume` | AbyssalArmorFullSetWithHeaumeAbility：以 self 包围盒向各轴扩张 32 格，查询 Mob；仅把当前正在攻击其他玩家的 Mob 目标改为 self，不吸引闲置怪物 | effect；适合作为自身施法的队友保护能力。公开完整回调可调用，查询结果需仅在本附属调用期间过滤友军；没有实际目标改变时不计费。原生是伤害回调触发，改为主动 effect 是明确的附属设计 |
| 2 | 潜水守护 / 潜水头盔 `salvager_helmet`（原能力来自整套潜水装备） | 空气耗至 0 以下时补到 280 并播放 EVENT_AIR_SUPPLY；处于套装分支时跳过冰迷宫冷水每 tick 增加 4 的冻结累积 | 持续 effect；区别于单独的水下呼吸，包含本体冷水规则豁免。完整实现位于玩家 tick 的私有方法，需限定在本附属效果生效玩家上接通原生套装判断，不能只调用空的 SalvagerArmorFullSetAbility，也不应复制空气／冻结算法；中等接入成本 |
| 3 | 毒杀再生 / 可怖之刃 `terrible_blade` | TerribleBladeFullAbility：在伤害回调看到目标已死亡且仍中毒时给实际攻击者再生，默认 II、6 秒，数值两版均读原生配置 | 更适合作为伤害增强；也可评估接在伤害后执行的条件 effect。应只认可本次法术实际击杀，不能对已有尸体重复换取治疗；须核对毒素与死亡清理时序、原生装备重复触发和一次结算。不采用无条件再生 |
| 4 | 冷光气泡 / `luminescent_bubble` | 原生气泡是可放置的含水方块，光照 15；每 20 tick 查询方块包围盒向各轴扩张 8 格内的水中玩家，给予海豚的恩惠 II、80 tick；原生泡泡粒子 | 放置型 effect；原物品在使用者头顶的水中放置，与原生块表现可对应。原块永久且右键可收回物品，法术召唤需要独立临时实例、无掉落／无回收、到期还水、区块重载与权限处理。不能用可回收原块无限产出来源材料；不是补气能力 |
| 5 | 深潜增伤 / 潮汐穿刺者 `tidepiercer` | 实际攻击者必须 isUnderWater；海平面以下每格额外 2%，最大额外 200%，两版一致 | 伤害增强；原生公开 modifyOutgoingDamage 可复用，应该作用于已有伤害，不能以 DamageContext(0) 获得倍率伤害。限定当前法术的 Ars 伤害事件，核对原生装备已施加时的重复乘算；不另造固定伤害 effect |
| 6 | 残血增伤 / 可怖獠牙 `terrible_fang` | 每缺少完整一颗心默认额外 15%，最高额外 150%；Neo 可配置，Forge 使用常量。仅原生完整能力实际激活且目标满血时，额外加成再翻倍 | 伤害增强；公开完整入口可复用，优先保留基础残血规则。不能仅凭写了此增强就冒充原生完整装备能力，也不能把“加成翻倍”写成最终伤害翻倍；同样需排除原生装备重复乘算 |

若下一轮只实施一项，优先挑衅；若选择辅助路线，优先潜水守护。两者各有明确的原生行为与装备来源，两平台均存在。毒杀再生与现有水母蜇刺／可怖环刃有组合价值，但实现前需要击杀来源与事件时序验证。冷光气泡有原生可见表现，但临时方块生命周期比普通状态 effect 更复杂。纯增伤增强排在后面，避免重复提供与 Ars 普通伤害效果相似的独立攻击。

### 继续暂缓或不选

- 深海头饰 `abyssal_tiara` 的死亡延缓：两版公开 cancelDeath 都恢复满血并给予默认 300 秒结晶化，拒绝重复结晶化及对应致命伤害。现在确认两版移除结晶化都可触发致命伤害，参见下一节；作为保护 effect 有原生来源，但需专门设计预先施加保护、死亡取消、消耗与净化时序，暂不作为首选，也不把结晶化直接做普通攻击减益。
- 深海半套装备的负面状态转力量：原生每次有害效果加入后叠力量，最大 amplifier 2，即 III；适合未来响应类能力。不能伪造一次负面状态加入来调用，也不是随时可用的普通力量 effect。
- 可怖盔甲的受击反毒／水下海豚恩惠：真实装备来源，但与已有水母毒素及冷光气泡重叠，优先级较低。
- RemnantSaberAbility 的回血仅对 Captain Cornelia，不是通用吸血；DaggerOfGreedAbility 另有 1% 缩减主手堆叠，直接调用可能消耗法术工具，暂不选。
- Eel 吸血在动画攻击帧里按目标实际掉血的两倍回血；Maw、MazeMother、TorturedSoul 的普通攻击没有新的独立载荷。Captain Cornelia 拉扯、怒潮及视觉闪电依赖 Boss 动画／阶段／遭遇事件；没有找到可独立完整调用、适合这批 spell effect 的入口。不能通过隐形 Boss 或复制局部 AI 伪装适配。
- DreadwakeTier3Ability 声明了再生、伤害吸收和碎晶变量，但该类没有处理回调；不能将变量或 tooltip 当作完整可调用能力。
- DevouringCurse 实际食品来源为虚空鱿鱼，TorturedSoul 不施加该效果；MaelstromOmen 仅 Forge，来源为可饮用 NiveisTearItem，并涉及跨维度旅行。二者均不进入本轮非食物来源清单。原生持续毒免疫的问题仍保留，不通过附属修补。

### 本轮证据与未验证项

以 CFR 0.152 再次读取两版实际发行 JAR，新增输出仅放忽略目录 `.upstream/candidate-audit-neo/` 与 `.upstream/candidate-audit-forge/`，没有复制原生代码或素材到受控源码／发行包。关键入口为上述 Ability 类，Neo `HookPlayer.tickSalvagerArmor` / Forge `PlayerHandler.tickSalvagerArmor`、两版 `LuminescentBubbleBlock.tick`，以及 Neo 的结晶化移除事件调用链。
本轮计算 Aquamirae JAR SHA-256 与依赖锁一致：Neo `5b8b2d04e16755d354adf3dfa77f906e33509722e647b1b5fdd5ccebabc8a35e`；Forge `5439edc57af3075a1600effe0906ba322e89346077bcb80b761a4bff4cbff59b`。物品中文名来自各自 JAR 的 `assets/aquamirae/lang/zh_cn.json`，实现判断不依据 tooltip。
仅文档变化，不运行 Gradle 或 GameTest，不产生新 JAR，不向人工测试目录复制任何文件，不改变 Core 基线。候选的 Ars 组合、友军处理、扣费、生命周期与客户端表现均尚未实现或验证；上文优先级是基于原生实现的设计判断。

## 暂未接入的已知差异

- Forge 7.1.14 `CrystallizationEffect.removeAttributeModifiers` 在原生效果移除时直接造成 `999999.0f` 点 crystallization 伤害。补核 NeoForge 7.2.11 的完整调用链：`MixinLivingEntity.onEffectRemoved` → `HookLivingEntity.onEffectRemoved` → `ServerLevelHelper.triggerEvent` → `CrystallizationEvent.tick`，同样造成 `999999.0f` 点伤害，只是由服务器事件执行。此前仅检查 Neo 效果类、将其视作只有属性修饰的记录不完整。两版都不能作为普通可净化减益接入；死亡延缓候选需核对完整死亡与移除时序。
- NeoForge 7.2.11 `PoisonImmunityEffect.applyEffectTick` 返回 `entity.removeEffect(POISON)`。没有毒素时返回 false，Minecraft 1.21.1 的效果更新会移除该效果；Forge 的对应 tick 方法无返回值。未宣称两版具有相同持续免疫行为，本批不提供该魔符或修补原生效果。
- DevouringCurse 是针对玩家食物/饥饿伤害的特殊本体机制；实际物品来源是 `void_squid` 食品，两版 TorturedSoul 攻击均没有施加该诅咒。不列入本轮非食物来源候选，也不将其伪装成通用生物持续伤害。
- 首批暂缓的回旋镖已在 dev.5 接入；两版路径差异与最大生存期由上述平台桥接和执行会话处理。

## 许可和边界

实际 NeoForge 发行 JAR 内的 LICENSE.md 为 OML 1.3，允许运行时兼容层和依赖；官方许可：https://github.com/ObscuriaLithium/Aquamirae/blob/1.21.1/LICENSE.md 。
本附属只包含原创适配代码、JSON 配方与资源路径引用，不分发原模组实现或素材；两个平台的二进制和源码包由 tools/verify_release.py 检查。
不新增对 Iron、Goety、BOMD 或 Cataclysm 附属的硬依赖，也不改 Conflux。许可证分别保留在原依赖中，附属 MIT 不改变它们。
