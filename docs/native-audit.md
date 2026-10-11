# 首批原生能力核对

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
直接实例化注册的原生 EntityType，完整运行其 tick；没有隐藏 Boss、重写实体代码、复制渲染器或重复驱动位置。

- owner 为 Conflux CastContext.damageOwner，目标不转移归属；生成前要求所有者仍活着且处于执行世界。
- 原生追踪距离 16，针对 Monster 或正攻击所有者的 Mob；原生视线检测与创造/旁观者排除保持。
- 速度 0.8 是附属发射参数；每个增强增加 0.2 原生尺寸，上限 2。伤害直接走原生 `10 * scale`，虚弱 I 持续 50 tick。
- 尺寸大于 0.75 的碎晶破裂时生成三个尺寸 0.5 的原生子碎晶；子碎晶保持所有者，不再分裂。全程本体声音、物品粒子和 renderer，图像效果仍需客户端人工验证。
- 每个原生碎晶最大生存期 200 tick；继承原生保存/读取和区块卸载行为，不持有外部实体引用。死亡的所有者不会使已发出的原生碎晶提前取消。
- 发射是一个 Ars 效果，后续魔符保持原触发位置；不提供子碎晶 Ars 命中续接。散射、环绕等只会在各次实际触发产生各自付费的原生实体。
- Forge 7.1.14 的完整类清单没有 AbyssalShard / AbstractShard / AbstractHomingShard。未自行移植或伪装成双版功能。

## 暂未接入的已知差异

- Forge 7.1.14 `CrystallizationEffect.removeAttributeModifiers` 在原生效果结束时造成 `999999.0f` 点 crystallization 伤害；NeoForge 7.2.11 对应效果仅注册攻击伤害/速度修饰。无法视为同一个普通双版减益，本批不提供结晶化魔符。
- NeoForge 7.2.11 `PoisonImmunityEffect.applyEffectTick` 返回 `entity.removeEffect(POISON)`。没有毒素时返回 false，Minecraft 1.21.1 的效果更新会移除该效果；Forge 的对应 tick 方法无返回值。未宣称两版具有相同持续免疫行为，本批不提供该魔符或修补原生效果。
- DevouringCurse 是针对玩家食物/饥饿伤害的特殊本体机制；本批不将其伪装成通用生物持续伤害。
- 回旋镖的实体路径两版不同，而且原生没有通用最大生存期；在完整处理所有者失效与跨维度清理前，不在首批接入。

## 许可和边界

实际 NeoForge 发行 JAR 内的 LICENSE.md 为 OML 1.3，允许运行时兼容层和依赖；官方许可：https://github.com/ObscuriaLithium/Aquamirae/blob/1.21.1/LICENSE.md 。
本附属只包含原创适配代码、JSON 配方与资源路径引用，不分发原模组实现或素材；两个平台的二进制和源码包由 tools/verify_release.py 检查。
不新增对 Iron、Goety、BOMD 或 Cataclysm 附属的硬依赖，也不改 Conflux。许可证分别保留在原依赖中，附属 MIT 不改变它们。
