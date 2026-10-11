# Aquamirae & Nouveau 0.1.0-dev.9

用户接受平衡审计中的费用、吟唱和长冷却调整，并要求继续复核弹射物。随后明确拒绝限制同一施法者的控制源数量：本版保留同时给多个友军授予同种控制增益，逐份收费。

## 范围与固定基线

- 附属工作树：C:/Users/57991/Documents/MinecraftDev/parallel-workspaces/aquamirae-first-abilities/Aquamirae-Nouveau；分支 agent/aquamirae-first-abilities。
- 实现提交 3ea2eb63775422f5e1afec00db8bb27b74be3465；配置工具 Windows 输出修正 e5decb15b64a72701307336be477cdb4358d9ccf。本交接单独提交，不影响 JAR。
- Core 固定 b24f18d72e64a902ff6c966040b0b83ea3cc6226，0.1.16，独立 ../Ars-Conflux-allies；受控文件无变化，忽略构建输出可写。
- NeoForge 1.21.1／21.1.250／Java 21／Ars 5.13.3／Aquamirae 7.2.11；Forge 1.20.1／47.4.0／Java 17／Ars 4.12.7／Aquamirae 7.1.14；两版 Fragmentum 5.2.0。实际 JAR 哈希仍由依赖锁固定。
- 只改本附属；不改 Core、其他附属或本体，不推送、发布、合并主工作树或部署 Forge。用户已授权指定 Neo mods 目录里的本附属；本次落地同时同步本附属自己的旧默认配置值，没有部署其他配置。

## 最终平衡

费用是附属载荷价，Ars 形态、增强和后续效果另算；全部魔符默认一级。

| 魔符 | dev.9 Mana | 默认主动 CD | 新增前摇 |
| --- | --- | --- | --- |
| 鳗鱼咆哮 | 初费 80，续费 12／秒 | 至少 30 秒 | 13 tick／0.65 秒 |
| 船长旋流 | 初费 120，续费 16／秒 | 至少 45 秒 | 20 tick／1 秒 |
| 吸血咬击 | 100 | 5 秒 | 0 |
| 碎甲迸裂／碎命迸裂 | 20／15 | 3 秒 | 0 |
| 可怖环刃／迷宫玫瑰 | 40／50 | 3／5 秒 | 0 |
| 水母蜇刺 | 保持 40 | 5 秒 | 0 |
| 其他既有魔符 | 保持 | 保持 | 0 |

咆哮／旋流单次默认最大时长 300／400 tick，包含延时。默认 5／10 秒完整载荷价 128／264，简单自身编排完整价 138／274；延时到上限后载荷 248／424。普通生物动作、伤害、药水等级、控制速度、原生资源和来源配方保持既有实现。

AquamiraeCooldowns 使用原有 Core ActiveCastProvider。释放登记主动 CD，控制真正授予时延长截止时间，避免飞行消耗 CD；结束后补主动恢复期，咆哮 20 秒（其中 10 秒覆盖残留缓慢／黑暗），旋流 15 秒。活动会话以最晚到期时间计算下一次新主动施放的恢复窗口，死亡重生复制本附属时间戳。可以关闭 CD 或独立关闭某个魔符 CD。

没有按原施法者计数的并行数量上限，也没有按 CD 拒绝旧载体的命中／授予；散射、环绕、传播和延迟续接可以分别给多个友军建立会话，逐份支付初费和续费。原有同执行者同项状态去重保留，不叠加、不重复付款。结束一份不取消其他份；友军过滤、空蓝、净化、失效与重载清理保持。

主动准备由 Core 聚合，独立 chanting 开关，默认 MAXIMUM，也支持 Core SUM；普通魔符保持即时释放。AquamiraePreparation 使用原版 ServerBossEvent 显示顶部剩余进度并逐 tick 更新，取消关闭并提示中断，服务器停止清理；没有自定义客户端渲染器、图片、数据包或对其他附属的引用。命中、续费和自动 resolver 不重新吟唱。配置新参数见 README。

## 弹射物复核

完整依据见 [projectile-balance-dev9.md](projectile-balance-dev9.md)。两版实际 Ars 一份 Split 增加一发，总数两发；环刃／玫瑰普通发射按载荷生成一次计费，穿透／回返接触可以多次产生原生伤害与续接。散射逐发收费，资源预算缩减数量；环绕等重复形态每次有效接触支付整份报价。空蓝与友军不产生免费载荷。碎晶保留 80 Mana，主晶基础 10、子晶各 5，分裂额外收益不是保证命中总量。

补齐独立海灵环境中的碎晶增强编排校验：原先只扩展了形态对替换增强的兼容列表，未允许 Amplify；本版扩展形态兼容并通过已有 Core SpellValidators 的 AquamiraePayloadRules 收窄适用组合，普通 Ars 和不支持伤害增强的原生环刃不获得无效增强。其他附属的 ProjectileForm 委托其自身规则。两端复用同一接线；Ars 5 的形态说明引用已有魔符描述，Ars 4 没有该接口。无新增 Mixin 或 Core 修改。

## 验证

| 目录／平台 | 实际命令 | 结果／证据 |
| --- | --- | --- |
| 附属根目录，Java 21 | ./gradlew.bat :runGameTestServer :jar :sourcesJar --offline --console=plain | Neo 46/46；build/dev9-neo.log |
| forge/，Java 17 | 同上 | Forge 31/31，含 reobfJar；forge/build/dev9-forge.log |
| 附属根目录发行检查 | python tools/verify_release.py 与 --forge | 双版二进制／源码包通过 |
| 配置迁移针对性检查 | tools/update_balance_config.py 在工作树忽略目录的缩进 CRLF 样本 | 自定义 77 Mana、9 秒 CD 保留，其余旧默认迁移，第二次无改动；build/dev9-migration-check.json |

新增 Neo 7／Forge 6 项，覆盖准备取消不付费／不登记 CD、真实准备结束只释放一次、混合 MAXIMUM 一次释放、多个友军同能力分别收费和维护／单份净化、时长上限和结束后恢复／重生时间戳、普通环刃多目标和回返只付一次、真实散射两发逐发付款／仅够一发的预算；Neo 另验证原生书内碎晶增强兼容以及普通 Ars／原生武器的无效增强排除。旧 Neo 39／Forge 25 项保留并按新报价更新结算预期。

环刃多接触测试使用真正原生载荷和控制器模拟两个目标及回返顺序，验证计费／伤害／续接，不声称测到了自然轨迹的群体命中率；既有真实飞行、回收与环绕测试仍通过。首次新散射测试误按三发断言，核对两版实际 JAR 后改为原生两发，未改变生产端散射算法，失败日志 build/dev9-neo-first.log。没有无理由重复已通过矩阵。

固定 Core git status --porcelain --untracked-files=no 为空；本任务测试服务已自然退出，无本任务非 Gradle JVM 残留。未启动客户端、未运行完整共享环境；顶部进度条、声音、粒子、运动手感和装备／共鸣实战平衡仍待人工验收。服务器通过不代表外观已验证。

## JAR 与部署

路径相对上述附属工作树，Forge 为最终重混淆包：

| JAR | SHA-256 |
| --- | --- |
| build/libs/aquamirae-nouveau-0.1.0-dev.9-mc1.21.1-neoforge.jar | ec946f5aa639e0edeb99dcfac525da5f5dad4ce6e4052fd5c0adb1836ca220a4 |
| build/libs/aquamirae-nouveau-0.1.0-dev.9-sources.jar | c51e809f28ce8b01c7a520bf7128692c21145c6cfe7768f0120416ba3db5cf9f |
| forge/build/libs/aquamirae-nouveau-0.1.0-dev.9-mc1.20.1-forge.jar | 2f2492a3fe7f41e1d081ae126891c5a105ff463bf2aa9146479cac1d1d28564d |
| forge/build/libs/aquamirae-nouveau-0.1.0-dev.9-sources.jar | 200c595431ac7d24c0a7815bed574c1c6c49524c01cbd153947cb77a1e1a0df0 |

Neo dev.9 已部署 C:/Users/57991/Desktop/.minecraft/versions/1.21.1-万法归一/mods，只替换本附属 dev.8；旧 JAR 与本附属旧配置备份在 build/deployment-backup/dev9。同步 config/aquamirae_nouveau-server.toml 中旧默认费用／CD／续费，保留自定义值；其余 161 个 JAR、941 个配置文件在最终部署前后哈希一致，证据 build/deployment-proof-dev9.json、build/deployment-config-proof-dev9.json。单个本附属 modid／最终二进制哈希正确，未替换 Core 0.1.17（6dde2ad9c72984c3bd3115db161b9c7e8f22884c524c6b09c05649e8152a5b46）。

初次配置子进程的中文路径 JSON 被 Windows 旧编码输出，主进程解码失败；已从核对过的备份只恢复本附属 JAR／配置，改用 ASCII 安全 JSON，再完成部署并记录前后哈希。没有修改其他文件来修复它。

随后观察到实例仍在运行旧版本，logs/latest.log 的 FileWatcher 针对本附属提示配置不正确并自动纠正：旧规范删除了 dev.9 的新增前摇／时长／恢复键，已存在的费用、CD 与续费新值仍正确。因此必须完整退出并重启客户端，让 dev.9 加载并依据新规范补齐默认设置，不能仅靠当前游戏内文件热加载验收。未替用户关闭游戏。

完整候选配置另保存在工作树 build/dev9-prepared-config/aquamirae_nouveau-server.toml，SHA-256 8f50130f91721a792abfbfc71b5f21a07d4e76c00576efaaf4b2098e3fb40ba9；没有部署到其他配置目录。若重启后仍需同步，可在游戏关闭时使用上述迁移工具或此完整文件，但先保留后来新增的自定义设置。部署证据已明确标记 file_watcher_reverted_new_fields 与 client_restart_required，未把当前旧进程称为新版本已加载。

发行边界保持附属 MIT／THIRD_PARTY_NOTICES，原生代码、媒体、依赖 JAR 与 GameTest 数据均未打包；资源仅引用原生路径。

> 在此独立工作树继续，Core b24f18d72e64a902ff6c966040b0b83ea3cc6226 源码只读。先读 AGENTS.md、README 与本交接。用户要求全部魔符默认一级，并明确允许多个友军同时承载同项控制，不重新增加按施法者计数的控制源上限。长 CD 只门控新主动施法，原有弹射物／环绕／传播和持续命中仍可逐份付费。优先让人工客户端完整重启，验证新前摇显示和配置，然后观察装备、群体和运动目标下的实际强度；只对新疑点补必要测试。部署仅限本附属和它自己的配置，不替换其他模组或配置。
