# Aquamirae-Nouveau 工作约定

先读本仓库 README.md、VERSION、构建配置和固定 Conflux 副本的 docs/development-workflow.md。隔离工作区由 workspace.json 记录。

- 本任务只修改自己的附属工作树；Conflux 受版本控制文件只读。需要新 API 时记录具体需求，交给独立 Core 任务。
- common/ 放两版共享逻辑，平台入口和差异分别保留在根项目及 forge/。两版共享 VERSION。
- 所有魔符默认等级统一为 SpellTier.ONE；后续新增魔符同样遵守。
- 用户明确保留同时给多个友军建立同种控制源的组合能力，不增加按原施法者计数的控制源上限；每份独立付费，主动冷却不拦截已派发的命中与持续续接。
- 核对实际发布 JAR 的原生入口、两端表现和生命周期，优先调用完整原生实现。只运行时引用 Aquamirae，不复制其代码、贴图、模型、声音或 JAR 到发行包。
- 按实际变更收拢后执行最小必要验证；服务端通过不代表客户端外观已人工验证。
- 本次用户允许只向指定人工测试目录部署 Aquamirae-Nouveau 本身；禁止替换 Conflux、原模组或其他模组。无独立授权不推送或发布。
- 交接按 Core 的 docs/agent-handoff-template.md，记录固定 Core SHA、工作树、验证、JAR 哈希与未验证项。
