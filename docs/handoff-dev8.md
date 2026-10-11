# Aquamirae & Nouveau 0.1.0-dev.8

用户要求所有魔符自身等级默认统一一级。将 CreatureAuraGlyph 和 EelBiteGlyph 的 defaultTier 改为 SpellTier.ONE，覆盖鳗鱼咆哮、船长旋流、吸血咬击；其他魔符原本已为一级。Neo 13 项、Forge 12 项现在全部默认一级。AGENTS.md 写入同样约定，后续新增魔符沿用；README 更新当前版本与等级。

工作树：C:/Users/57991/Documents/MinecraftDev/parallel-workspaces/aquamirae-first-abilities/Aquamirae-Nouveau，分支 agent/aquamirae-first-abilities。固定 Core 仍为 ../Ars-Conflux-allies 的 b24f18d72e64a902ff6c966040b0b83ea3cc6226（0.1.16），受控文件无变化。平台和实际原模组依赖沿用 [dev.7 交接](handoff-dev7.md)。本次只调整默认等级，不改状态等级、增强收益、费用、配方和行为。

## 验证与产物

根目录 Java 21、forge/ Java 17 分别运行 `./gradlew.bat :jar :sourcesJar --offline --console=plain`，均通过；Forge 完成 reobfJar。日志 build/dev8-neo.log、forge/build/dev8-forge.log。根目录 `python tools/verify_release.py` 与 `--forge` 均通过。

对最终两版 JAR 逐一使用 javap 检查全部具体魔符实现的 defaultTier 字节码：Neo 7 个实现类、Forge 6 个实现类均直接返回 SpellTier.ONE，分别覆盖全部 13／12 项魔符；证据 build/dev8-tier-proof.json。改动仅默认值，未重跑 dev.7 的服务端行为测试；本次未启动客户端，实际画面未人工验证。

路径相对于上述工作树：

| JAR | SHA-256 |
| --- | --- |
| build/libs/aquamirae-nouveau-0.1.0-dev.8-mc1.21.1-neoforge.jar | a2a815b45543f5cebc26c8ef143b153cacf628a4e70e163276419fd4cb69ae60 |
| build/libs/aquamirae-nouveau-0.1.0-dev.8-sources.jar | 6c5f8a5f9ce682fec6cbab4b6ec3e7feafa18fb151e58799d327179ab38e3037 |
| forge/build/libs/aquamirae-nouveau-0.1.0-dev.8-mc1.20.1-forge.jar | 7eda6ebf913912f25d3b1eefbd45693f0adee4e2a67e409403903a82685f3b69 |
| forge/build/libs/aquamirae-nouveau-0.1.0-dev.8-sources.jar | 7485446b14b069dbf8b41bed5dfab129ed6c0a264633487a83f4fdab2ffef543 |

按已有授权，仅将本附属 Neo dev.7 替换为 dev.8，部署 C:/Users/57991/Desktop/.minecraft/versions/1.21.1-万法归一/mods。部署后只有一个本附属 modid，哈希与上表一致；其他 161 个 JAR 哈希不变，Core 仍为其他任务部署的 0.1.17。旧附属备份 build/deployment-backup/dev8，证据 build/deployment-proof-dev8.json。未改共享配置，Ars 已有自定义等级配置仍以其配置值为准。

发行包保持附属 MIT 与 THIRD_PARTY_NOTICES；无原生代码／素材复制。未修改其他仓库，未推送或发布，未部署 Forge。

> 在本独立附属工作树继续，先读 AGENTS.md 与固定 Core 工作流程。所有魔符默认 SpellTier.ONE；Core 源码只读，暂不新增食物状态，不做船长生命掠夺。只对新改动或明确疑点补必要验证。部署授权仅涵盖指定 Neo mods 目录里的本附属，不替换其他模组或共享配置。
