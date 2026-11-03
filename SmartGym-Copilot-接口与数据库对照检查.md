# 后续范围变更：移除健身房领域

项目已调整为面向通用用户的健身软件。健身房、用户会员关系和健身房器械实例不再属于当前产品范围；历史审查章节中的 gym 结论仅保留作变更记录。当前器械目录使用 equipment 表，训练计划不再关联 gym_id。数据库通过 V5__remove_gym_domain.sql 清理历史表。

# SmartGym Copilot 接口与数据库对照检查

- 检查日期：2026-11-02（Asia/Shanghai）
- 性质：源码与文档静态审查；不是数据库运行验收、接口联调报告或全量代码质量审计。
- 原接口基线：`SmartGym-Copilot-接口文档.md` v1.0；当前接口契约已更新为 v1.2。
- 产品背景：`SmartGym-Copilot-项目完整方案.md`，只作需求背景，不把规划内容自动升级为首版接口。
- SQL 参考：`smart-gym-schema.sql`；实际迁移文件：`smart_gym/src/main/resources/db/migration/V1__init_auth_and_gym.sql`。
- 实现依据：现有 Controller、DTO、Service、实体、Mapper、Security、异常处理、trace 和 application.yml；前端已有 API/类型用于交叉核对，不反向扩大需求。
- 本文其余 gym 相关段落是范围变更前的历史审查记录，不再代表当前产品范围；当前版本已移除健身房、会员关系和健身房器械实例。

## 1. 总体结论与范围边界

1. 原接口文档共有 **76 个不同的方法＋路径**，相同路径的不同查询参数按一个接口计。
2. 其中 72 个为 `/api/v1` 接口，4 个为健康检查、指标、OpenAPI、Swagger 入口。
3. 当前自定义业务 Controller 只实现 5 个认证接口；其余 67 个 `/api/v1` 接口尚未发现实现。框架端点的启用/暴露情况必须启动后验证，不归入“67 个未实现业务接口”。
4. 完整 SQL 有 **57 张表**；Flyway 当前只有 V1，定义 **6 张表**。不能把完整 SQL 中的表描述成已经完成迁移，也不能把没有实现的模块直接认定为代码错误。
5. V1 的 6 张表和 V2 的 9 张健康档案表均已具备 `id`、`created_at`、`updated_at`。完整 SQL 中其余尚未迁移的业务表，需在各自迁移落地前继续统一公共字段。
6. 当前版本已移除 gym 范围；个人档案、训练和饮食接口只按当前用户身份与各自资源归属授权。
7. `MEMBER` 是系统角色；`gym_user` 是场馆关系；教练访问他人数据需要单独的业务授权。三个概念不能替代。
8. 后续开发以接口文档 §12.5 为顺序：认证及健康档案 → 动作/器械/训练 → 饮食 → 汇总/管理 → Agent → RAG。健身房不再插入主线。

状态含义：

- **已实现待验收**：发现源码实现，不代表已经通过真实 MySQL/HTTP 验证。
- **已设计未迁移**：完整 SQL 有结构，但 Flyway 尚无相应迁移。
- **字段缺项**：契约字段缺少明确持久化位置。
- **契约待明确**：有数据结构，但身份、规则、返回值或聚合口径不够明确，不能直接凭猜测实现。
- **规划保留**：已有方案/SQL 包含的内容，当前无独立接口，不主动新增 CRUD。

## 2. 当前认证及通用实现的核对结果

### C01：Trace ID 链路【已修正】

- `TraceIdFilter` 已注册为全局 `@Component`，并以高优先级运行在认证链路之前。
- 请求头、响应头、响应体 `traceId` 和日志 MDC 复用同一个值；未传或非法 `X-Request-Id` 时服务端生成。
- 异步 SSE 的 MDC 传播仍属于后续 SSE 专项验收，不影响当前同步认证接口。

### C02：时间语义【已修正（认证范围）】

- 数据库连接与应用时钟统一使用 UTC；Refresh Token、最后登录时间使用注入的 UTC `Clock`。
- `/auth/me` 的 `createdAt` 改为 `OffsetDateTime`，返回带 UTC 偏移的 ISO 8601 值。
- 后续业务模块继续遵守数据库 UTC 存储、按用户时区解释业务日期的约定。

### C03：非法请求状态码【已修正】

- JSON 解析失败返回 `400 MALFORMED_REQUEST`。
- 缺失参数或类型转换失败返回 `400 INVALID_REQUEST`；不支持的方法返回 405，不支持的媒体类型返回 415。
- Bean Validation 仍返回 422 `VALIDATION_ERROR`；未分类异常才进入 500 `INTERNAL_ERROR`。

### C04：认证接口幂等边界【已明确】

- 注册：账号唯一约束和并发兜底保证重复账号返回 409，不缓存包含密码的请求响应。
- 登录：每次成功创建新的 refresh token，不做响应重放。
- 刷新：成功后旧 refresh token 立即失效，重复使用返回 401。
- 退出：撤销当前用户全部未撤销 refresh token，可重复调用；认证接口当前不强制 `Idempotency-Key`。业务写接口和 Agent 确认接口仍需按接口文档实现幂等。

### C05：退出登录范围【已明确】

- `POST /api/v1/auth/logout` 撤销当前用户的全部 refresh token，会同时结束其他设备的 refresh 会话。
- 已签发的 Access Token 不主动加入黑名单，直到其自然过期；客户端应立即丢弃本地 Access Token。

### C06：账号规范化【已修正】

- 注册和登录在校验后均对账号执行首尾空白去除和 `Locale.ROOT` 小写化；密码不 trim。
- 注册昵称在校验前去除首尾空白，再按 2～30 个字符校验；实际存储值与校验值一致。
- 首版只限制账号长度和非空，不强制邮箱/手机号格式；该规则已写入接口文档。

### C07：认证正常字段大体对齐，示例值不是强制常量【记录】

- 注册：`userId/account/displayName` 对应用户表；默认 `MEMBER`、密码编码和事务已有实现。
- 登录：`accessToken/refreshToken/expiresIn/user` 对应当前 DTO；Refresh Token 使用哈希保存，有效期/撤销字段齐备。
- 刷新：旧令牌条件撤销与新令牌插入由外层事务包裹；角色重新查询。未做真实数据库并发验收。
- 配置 `expiresIn=900`、文档示例 `7200`；这是配置/示例一致性问题，不应把示例数值当成接口常量。令牌字符串 `rt_...` 是示例，不应误认随机令牌必须使用此前缀。
- `ACCOUNT_ALREADY_EXISTS`、`ACCOUNT_DISABLED`、`DEFAULT_ROLE_NOT_FOUND`、`DATA_CONFLICT`、`MALFORMED_REQUEST` 等错误码已补充到接口文档，前端按 `code` 判断，不依赖 message。
- `AUTH_TOKEN_EXPIRED` 已列在文档，当前统一使用 `AUTH_TOKEN_INVALID`；若继续保持统一策略，应同步说明。

### C08：环境访问控制与验收状态【发布前处理，不扩大功能】

- Security 的 Swagger/OpenAPI 放行没有环境条件；文档 §11 要求生产访问控制。`/actuator/metrics` 只有“需登录”的通用规则，端点是否暴露及允许主体都要明确。
- 现有测试仅发现 `contextLoads`，不能据此宣称认证闭环、事务、轮换并发、Flyway 已验收。
- 已移除 application.yml 中与 MyBatis/Flyway 无关的 JPA `ddl-auto` 配置，避免产生 Hibernate 自动改表的误解。
- 本次没有运行构建或连接 MySQL。先前混合本地 jar 的直接 javac 检查不等价于按 pom.xml 解析依赖的 Maven 构建，更不等价于数据库验收。

## 3. 健康档案：下一开发阶段前必须对齐的字段

以下为映射方案与缺项说明。健康档案相关表已在 Flyway V2 落地，不改变现有 URL；业务实体、Mapper、Service 和 Controller 仍待实现。

V2 为所有健康档案表统一增加 `id`、`created_at`、`updated_at`。`food_preference.food_id` 暂不建立外键，因为食物目录表尚未进入迁移；待食物目录迁移后再补充外键约束。

| 接口字段 | 现有表/列 | 结论与约束 |
|---|---|---|
| profile.userId | member_profile.user_id | 从登录上下文取；一用户一档案；未来新增独立 id 时保留 user_id 唯一约束 |
| gender/birthDate | member_profile.gender/birth_date | 枚举按文档；birthDate 是日期，不转成时间戳 |
| heightCm/weightKg | member_profile.height_cm/weight_kg | Decimal 可支持；范围、是否允许 null 尚需明确，不直接沿用前端自行设定的范围 |
| trainingExperience | member_profile.training_experience | 枚举按文档 |
| weeklyTrainingDays/maxSessionMinutes | member_profile.weekly_training_days/max_session_minutes | 已有；范围需明确 |
| goals[] | training_goal.goal_type，关联 user_id/status | 可聚合；PUT 全量替换还是更新活动目标需明确；不能每次请求重复追加 |
| updatedAt | member_profile.updated_at | 目标变更也属于档案变更，应统一更新时间；响应按 C02 处理 |
| bodyMetric.id/measuredAt/weightKg/bodyFatPercent/waistCm/note | body_metric 同名 snake_case 列 | 已有；分页、日期含义、排序及并列时间稳定排序需固定 |
| constraints[].type | health_constraint.constraint_type | 名称不同，需显式映射，不能依赖普通驼峰映射 |
| constraints[].bodyPart/severity/description/active | health_constraint 同名 snake_case 列 | 已有；PUT 无必填 id，不能强迫前端传数据库主键；明确全量替换/清空语义 |
| medicalNotice | 无明确专用列 | 字段缺项；建议附属于 member_profile 的 medical_notice，可独立于 constraints 保存，不能复制到每一条限制中；本次仅建议 |
| dietType | nutrition_profile.diet_type | 已有 |
| dailyWaterMlTarget | nutrition_profile.daily_water_target_ml | 含词序差异，需显式映射；不会自动映射为同名驼峰属性 |
| allergies[] | food_allergy → allergen.code | 接口传 code，服务端解析 ID；校验未知 code；字典种子当前没有在迁移中落地 |
| restrictions[] | dietary_restriction.restriction_code | 已有；固定按文档/后续枚举说明校验，不随意扩展 |
| dislikedFoods[] | food_preference | 候选：preference_type=DISLIKED_FOOD、preference_value=名称；需明确类型约定、去重、全量替换与来源保留 |
| preferredCuisines[] | food_preference | 候选：preference_type=PREFERRED_CUISINE、preference_value=菜系 code；同上；不能误报为“整个字段完全无表可存” |

必须补充的现有接口细节：

- GET 未建档时返回 404 还是空结构；前端当前将 profile 的 404 当作尚未建档，原文档没有明确该约定。
- PUT 中字段省略、null、空数组的语义；profile、健康限制、饮食档案分别是否允许独立创建。
- `member_profile.weight_kg` 与 `body_metric` 最新体重的关系。不能擅自假定更新档案等于新增测量、或新增测量必定覆盖档案。
- goals、过敏/限制/偏好更新的事务与去重；涉及聚合读取时保持数据一致。
- 不要求用户先加入健身房才能创建个人档案。目标 user_id 只能从当前身份取。
- 只迁移当前阶段所需结构。`food_preference.food_id` 外键引用尚未迁移的 food；不为建健康档案顺手迁移整套食物业务。可在本阶段保留所需偏好列、后续按实际依赖补充可选引用，或显式安排依赖迁移，需在迁移设计中选定。

## 4. 后续模块的数据库与契约问题（先登记，到对应阶段修正）

### D01：动作字段缺项与筛选映射

- `exercise` 缺 `default_rest_seconds` 与 `thumbnail_url` 的明确存储列；接口要求 `defaultRestSeconds`、`thumbnailUrl`。
- `primaryMuscles[]` 可由 exercise_muscle(muscle_role=PRIMARY) → muscle_group.code 组装；equipment[] 由 exercise_equipment → equipment 组装。
- `muscleGroup=LEGS` 与响应 `QUADRICEPS/GLUTES` 的层级关系可用 muscle_group.parent_id 表达，但筛选是否含子级需要明确。
- `contraindications[]` 对应 contraindications_json，不需要额外新建表。

### D02：器械类型与场馆实例的 ID/状态歧义

- equipment 是类型目录（status 默认 ACTIVE）；gym_equipment 是场馆实例（status 默认 AVAILABLE）。
- 文档响应同时返回 `id/name/category/status/gymId/remark`，未明确 id 指类型还是实例；路径参数 equipmentId 与动作 equipmentId 可能被混用。
- 必须明确每个 ID 的来源、多场馆/多实例返回方式和场馆上下文来源；不能任取一个场馆或把类型 ACTIVE 直接伪装成实例 AVAILABLE。
- remark 可候选映射到 gym_equipment.note，但需要明确；保持现有外部字段名，不能为方便数据库实现擅自改契约。

### D03：训练计划与执行规则

- 计划主字段、warnings JSON、计划日与动作明细均有表可存。
- `targetReps` 由 target_reps_min/max 格式化，是派生字段，不是缺列。
- 创建请求没有 day_no，数据库却为 NOT NULL，可按数组顺序生成，需明确并稳定保存；sort_order 同理。
- 请求无 startsOn，today 接口却按日期查；起始日期默认、周期计算、时区、无训练日响应、同一用户多个 ACTIVE 计划的处理需明确。
- activate、PATCH 支持字段/状态转换、删除已有关联记录的处理和响应结构未完整说明；不能从 SQL 的默认 DRAFT 推断所有业务状态规则。
- durationMinutes 根据 started_at/ended_at 派生；totalVolumeKg 可以持久化，但计算口径需明确。接口例子只有 1 组 20kg×12 次，却返回 720kg，示例自身不一致，不能照抄。
- plan_id/plan_day_id 都有外键，但外键不能证明计划属于当前用户，也不能证明 planDay 属于 plan；业务层必须同时校验。
- feedback 与 session 都存 fatigue/pain，需明确是否同步和查询优先级。

### D04：食物与营养换算

- food、food_nutrition、food_serving、food_allergen、allergen、food_category 能支撑多数响应字段。
- `defaultServingAmount/defaultServingUnit` 未定义从哪一条 food_serving 选取（无默认标识），或是否是独立的默认值；不能把每 100g 的营养基准直接当作“默认份量”。
- food_nutrition 允许多个 source_version/份量；接口未明确当前使用版本；source/sourceVersion 还同时存在于 food，需定义选取与追溯规则。
- 营养响应的 `protein/carbohydrate/fat/fiber/sugar/sodium` 需映射 `_g`/`_mg`，sodium 单位需明确。
- 请求支持 ml；food_serving.gram_weight 可为 NULL，不能无依据把 ml 当 g。缺少换算时必须按明确的校验规则处理。

### D05：营养目标、记录、计划与汇总

- nutrition_target.effective_date 非空，而 current 请求没有日期；需按明确时区/日期规则赋值；GET current 查当日还是最近生效记录未明确。
- 饮食记录的快照、明细确认标记、置信度、总营养均有存储位置；不能仅凭 foodId 动态重算历史记录。
- 文档 meal-record 响应列出的单个明细为 143kcal，总计却是 310kcal，与示例省略明细的处理不明确；先修正文档示例或标明省略，不能当验收真值。
- meal_plan 请求 date 映射到 meal_plan_day.plan_date；meal time 对应 meal_plan_meal.meal_time。SQL 能存多天，不能据此擅自要求前端改成多天请求。
- 替换接口依赖 itemId，原文档未完整列出计划响应，需明确返回明细 ID；替换项必须属于路径计划和当前用户。
- completionRate 可从 consumed/target 派生，需约定 target=0/null 情形；recordCompleteness 虽有列，但计算公式未定义。
- PATCH/activate/replace-item 的响应和状态规则尚需补齐。记录、计划和汇总的事务/重算归属应固定，避免重复累加。

### D06：首页与趋势是聚合，不增加无依据的表

- 体重来自 body_metric；训练来自 workout_plan/day、workout_session/set_record；营养来自 nutrition_target、meal_record/item 或 daily_nutrition_summary。
- 今日计划、周完成率分母、7 天体重差缺测处理、一天多次测量取值和 NUTRITION 趋势指标/单位都需定义。
- quickQuestions 可为配置/派生内容，无需仅为响应字段新增业务表。

### D07：Agent 及确认接口

- 会话/消息/运行使用 VARCHAR(64) 字符串 ID，与文档 conv_/msg_/run_ 一致；现有 Long AUTO 的 BaseEntity 不能原样套到这些实体上。
- SSE status/tool_call/source/plan_preview/token/error/done 的载荷应遵守现有文档；run/step/tool_call、draft_json/draft_expires_at、output_json/content_json 可承载运行/草稿/来源等数据，JSON 内部映射与必要的持久化范围需明确。
- context.timezone/date 的运行时解释与持久化位置未明确；保留原请求字段，不另造用户时区接口。
- clientMessageId 唯一约束已有；与 Idempotency-Key 的关系、SSE 重连顺序字段、重放方式尚未完整定义。
- confirm 的 accepted=false 含“拒绝/要求修改”两种描述；状态转移、过期处理、结果资源复查与重复确认规则需明确。
- confirmed_run_id 当前是普通索引；确认状态条件更新、唯一业务保证与幂等记录必须共同防止重复生成资源，不能只依赖普通索引。
- feedback 的 (user_id,message_id) 唯一约束支持防重；需校验 message 属于 conversation 和当前用户，评分 1～5 由业务校验，重复更新语义需写清。

### D08：知识库与管理端

- knowledge_document 的 content_hash、knowledge_source.source_type 由服务端生成/推导；不应强迫前端提交原契约没有的字段。
- chunkCount 可聚合 knowledge_chunk，不是缺列；文档 patch/delete/reindex/search 的响应与状态规则仍不完整。
- knowledge_document 去重以 (content_hash,version) 为准；是否允许相同内容多文档、多版本以及重复创建的响应未明确。
- 用户禁用 reason 可落 audit_log.detail_json，不需要另造用户状态历史表；当前尚未迁移/实现审计。
- 角色更新按文档为 PATCH；仅 `/admin` 角色检查不能自动满足文档所述“审计权限”，具体范围需明确，不能先造一套未经要求的 RBAC 菜单/权限表。
- prompt_version 已有 (prompt_name,version) 唯一索引，但 (prompt_name,enabled) 只是普通索引，不能保证“同名同时仅一个启用版本”。需在启用事务与并发策略中落实规则。
- 管理端列表/详情等多处仅列 URL，未给响应字段；前端自行定义的类型只能作补充草案，需先回写契约再实现。

## 5. gym 范围的处理

用户已确认保留：gym、gym_user 以及此前讨论的健身房基础信息/会员关系范围。V1 与实体/Mapper 继续保留。

下列是此前讨论过的路径，仅作为待补契约清单，不计入原文档 76 个接口，不表示本次已经正式落地：

- GET /api/v1/gyms
- GET /api/v1/gyms/{gymId}
- POST /api/v1/gyms/{gymId}/members
- DELETE /api/v1/gyms/{gymId}/members/me
- GET /api/v1/users/me/gyms
- GET /api/v1/gyms/{gymId}/members

管理员创建/变更健身房与会员状态此前也被讨论，但路径与输入输出尚无完整约定，先记录这一缺口，不在本报告自行发明 URL。

实现前需补充：

1. members POST 是自己加入还是管理员添加他人；不同主体的参数和权限必须明确。
2. 可否加入多个健身房、退出后如何重新加入；目前 (gym_id,user_id) 唯一，重复加入不能直接重复插入。
3. 会员状态值、状态转换、停用健身房对关系的影响；当前默认 ACTIVE 不能代替完整规则。
4. 会员列表可见主体、返回字段和脱敏；普通场馆成员不应自动获得他人健康数据访问权。
5. 管理员是当前全局 ADMIN；不自动扩展为场馆管理员、收费会员或多租户权限系统。
6. 不因存在 membership_status 而把场馆关系当作教练的个人数据授权。

## 6. 迁移与基础字段核对

- V1 已含 6 张表及 MEMBER/TRAINER/ADMIN 种子；原始全量 SQL 的同名表结构与 V1 不再完全相同，尤其关系表主键和时间字段，不能把原 SQL 原样当作新 V1 使用。
- 完整 SQL 尚有 51 张表未迁移；其中 31 张不满足统一公共字段约定。第 10 节列出每张表的实际缺项。
- 后续迁移按模块落地，不执行完整参考 SQL。已经应用的 V1 后续变更走新版本；本次没有假设数据库已清空，也没有修改 V1。
- 给关系表增加自增 id 时，保留原业务组合唯一键；给 member_profile/nutrition_profile 增加 id 时保留 user_id 唯一键。
- 字符串 Agent 主键保留契约类型；统一存在 id 不等于所有实体都必须继承 Long AUTO 的 BaseEntity。只在对应模块实现时适配基类，不预先重构全项目。
- 聚合/转换字段无需机械加列，例如 goals、targetReps、durationMinutes、completionRate、chunkCount。
- 外键和索引不等于资源所有权检查。写入计划/明细/反馈时校验完整所属关系。

## 7. 修正顺序与验收清单

| 顺序 | 工作 | 完成条件 |
|---|---|---|
| 1 | 修正既有认证/通用差异 C01～C06 | trace 全链一致；非法请求为正确 4xx；时间有 offset 且存储语义一致；退出范围/账号规则/幂等边界明确 |
| 2 | 将本报告中已存在接口的缺失规则补回契约 | 字段必填/空值、枚举、错误码、响应、状态转移可用于验收；gym 单列补充，不改变现有字段名/路径 |
| 3 | 健康档案迁移与接口 | 对应第 3 节字段全覆盖；不因食物外键依赖而顺手实现食物全模块；公共字段、唯一键、事务与所有权校验齐备 |
| 4 | 后续按原 §12.5 开发 | 每个模块先核对本报告对应问题，再编写迁移/实体/Mapper/业务与测试；没有契约的新功能不实现 |

当前认证真实验收用例（本次未执行）：

- 成功注册、默认 MEMBER、密码不明文；重复及并发同账号注册不产生重复用户/孤立角色关系。
- 正确/错误密码、禁用/删除用户；登录、me、刷新响应字段符合契约，无 passwordHash 泄露。
- 刷新旧令牌失效；并发刷新仅一个成功；插入新令牌/签发失败时事务保持一致；过期令牌不再可用。
- 退出后的全部或当前会话范围按最终契约验证；退出与刷新并发场景；不承诺已签发 JWT 立即失效。
- 无请求 ID、合法/非法 ID、401/403/422/500 的 header/body/log 一致；MDC 在线程复用后不串号。
- 事件时间有 offset；UTC 存取与不同时区日期边界一致。
- JSON 解析错误、字段越界、未知角色/枚举、幂等键冲突返回规定的 HTTP/code/data。

健康档案验收用例（下一阶段）：

- 未建档 GET、首次 PUT、重复 PUT、空数组/null/省略字段按契约运行。
- medicalNotice 独立保存，即使 constraints 为空仍可往返。
- goals、allergies、restrictions、dislikedFoods、preferredCuisines 往返不丢失、不重复；失败时事务回滚。
- 一用户一份基础/饮食档案；用户 A 不能读取/修改用户 B；未加入 gym 的用户仍可维护个人档案。
- UTC 日界线、分页边界、排序稳定性和体重同步规则通过真实数据库验收。

## 8. 字段映射/约束补充说明

- 各接口完整字段以原文档对应节的 JSON 为准；第 9 节逐项列请求摘要、响应摘要、关联表、权限和缺口编号，不将摘要视为重新定义接口。
- 常规同名映射用 camelCase ↔ snake_case；第 3～4 节列出不能直接自动映射的字段、派生字段和聚合字段。
- 通用响应统一 ApiResponse；分页统一 items/page/pageSize/total/totalPages。原文档未说明响应时，第 9 节明确标“未定义”，不编造响应。
- 原文档没有完整列出所有字段必填/数值上下界/枚举/默认值。JSON 示例不等于完整校验规则，SQL NULL/默认值也不等于客户端协议。
- 时间、幂等、所有权、校验码属于所有模块的共同约束；没有为这些约束新建无依据的业务需求。

## 9. 全量接口对照清单

权限缩写：匿名=原契约免 Bearer；本人=当前登录用户及资源归属；登录=已登录目录查询；管理=ADMIN（审计等额外权限见 D08）；环境=按运行环境控制。所有写操作还需解决 C04 幂等边界。


### 原文 §2. 认证与当前用户

| 编号 / 方法＋路径（原文行号） | 请求摘要 | 响应摘要 | 涉及参考表 | 权限 | 状态 / 约束位置 |
|---|---|---|---|---|---|
| 01 `POST /api/v1/auth/register`（L134） | account/password/displayName | 201 userId/account/displayName | sys_user,sys_role,sys_user_role | 匿名 | 已实现待验收；C03/C04/C06 |
| 02 `POST /api/v1/auth/login`（L175） | account/password | accessToken/refreshToken/expiresIn/user{id,account,displayName,roles} | sys_user,sys_role,sys_user_role,sys_refresh_token | 匿名 | 已实现待验收；C02/C04/C07 |
| 03 `POST /api/v1/auth/refresh`（L213） | refreshToken | accessToken/refreshToken/expiresIn | sys_refresh_token,sys_user,sys_role,sys_user_role | 匿名，刷新凭据校验 | 已实现待验收；C02/C04/C05/C07 |
| 04 `POST /api/v1/auth/logout`（L229） | 空体，Bearer | 当前为 ApiResponse 空 data；文档未具体列出响应 | sys_refresh_token | 本人 | 已实现待验收；C04/C05 |
| 05 `GET /api/v1/auth/me`（L237） | Bearer，无 userId 入参 | id/account/displayName/avatarUrl/roles/createdAt | sys_user,sys_role,sys_user_role | 本人 | 已实现待验收；C01/C02 |

### 原文 §3. 会员健康档案

| 编号 / 方法＋路径（原文行号） | 请求摘要 | 响应摘要 | 涉及参考表 | 权限 | 状态 / 约束位置 |
|---|---|---|---|---|---|
| 06 `GET /api/v1/member/profile`（L267） | 无 | userId/gender/birthDate/heightCm/weightKg/trainingExperience/weeklyTrainingDays/maxSessionMinutes/goals[]/updatedAt | member_profile,training_goal | 本人 | 未实现/相关表未迁移；第3节 |
| 07 `PUT /api/v1/member/profile`（L297） | gender/birthDate/heightCm/weightKg/trainingExperience/weeklyTrainingDays/maxSessionMinutes/goals[] | 完整健康档案 | member_profile,training_goal | 本人 | 未实现/相关表未迁移；第3节 |
| 08 `GET /api/v1/member/body-metrics`（L326） | from/to/page/pageSize | 分页身体指标{id,measuredAt,weightKg,bodyFatPercent,waistCm,note} | body_metric | 本人 | 未实现/相关表未迁移；第3节/C02 |
| 09 `POST /api/v1/member/body-metrics`（L327） | measuredAt/weightKg/bodyFatPercent/waistCm/note | 身体指标数据 | body_metric | 本人 | 未实现/相关表未迁移；第3节/C02 |
| 10 `GET /api/v1/member/health-constraints`（L358） | 无 | GET 响应未独立定义；应补充 constraints/medicalNotice 返回约定 | health_constraint；medicalNotice 待落位 | 本人 | 未实现/相关表未迁移；第3节 |
| 11 `PUT /api/v1/member/health-constraints`（L359） | constraints[]{type,bodyPart,severity,description,active}/medicalNotice | 未定义 | health_constraint；medicalNotice 待落位 | 本人 | 未实现/相关表未迁移；第3节 |
| 12 `GET /api/v1/member/nutrition-profile`（L360） | 无 | GET 响应未独立定义；应补充完整饮食档案返回约定 | nutrition_profile,food_allergy,allergen,dietary_restriction,food_preference | 本人 | 未实现/相关表未迁移；第3节 |
| 13 `PUT /api/v1/member/nutrition-profile`（L361） | dietType/allergies[]/restrictions[]/dislikedFoods[]/preferredCuisines[]/dailyWaterMlTarget | 未定义 | nutrition_profile,food_allergy,allergen,dietary_restriction,food_preference | 本人 | 未实现/相关表未迁移；第3节 |

### 原文 §4. 动作、肌群和器械

| 编号 / 方法＋路径（原文行号） | 请求摘要 | 响应摘要 | 涉及参考表 | 权限 | 状态 / 约束位置 |
|---|---|---|---|---|---|
| 14 `GET /api/v1/exercises`（L403） | keyword/muscleGroup/equipmentId/page/pageSize | 分页动作{id,name,englishName,primaryMuscles,equipment,difficulty,defaultRestSeconds,contraindications,thumbnailUrl} | exercise,exercise_muscle,muscle_group,exercise_equipment,equipment | 登录 | 未实现/相关表未迁移；D01 |
| 15 `GET /api/v1/exercises/{exerciseId}`（L404） | exerciseId | 详情未独立定义；有动作列表项可参考 | exercise,exercise_muscle,muscle_group,exercise_equipment,equipment | 登录 | 未实现/相关表未迁移；D01 |
| 16 `GET /api/v1/equipment`（L426） | status/page/pageSize | 分页器械{id,name,category,status,gymId,remark} | equipment,gym_equipment,gym | 登录；场馆上下文待明确 | 未实现/相关表未迁移；D02 |
| 17 `GET /api/v1/equipment/{equipmentId}`（L427） | equipmentId | 器械响应项；ID 含义待明确 | equipment,gym_equipment,gym | 登录；场馆上下文待明确 | 未实现/相关表未迁移；D02 |

### 原文 §5. 训练计划与训练记录

| 编号 / 方法＋路径（原文行号） | 请求摘要 | 响应摘要 | 涉及参考表 | 权限 | 状态 / 约束位置 |
|---|---|---|---|---|---|
| 18 `GET /api/v1/workout-plans`（L450） | status/page/pageSize | 分页计划；name/source/status/goal/durationWeeks/trainingDaysPerWeek/days/warnings/createdAt | workout_plan,workout_plan_day,workout_plan_exercise,exercise | 本人 | 未实现/相关表未迁移；D03 |
| 19 `GET /api/v1/workout-plans/today`（L451） | date | 今日响应是否完整计划/当日计划项、空值形态未独立定义 | workout_plan,workout_plan_day,workout_plan_exercise | 本人 | 未实现/相关表未迁移；D03 |
| 20 `GET /api/v1/workout-plans/{planId}`（L452） | planId | 完整计划及 days/exercises | workout_plan,workout_plan_day,workout_plan_exercise,exercise | 本人 | 未实现/相关表未迁移；D03 |
| 21 `POST /api/v1/workout-plans`（L493） | name/goal/durationWeeks/trainingDaysPerWeek/days[]{dayOfWeek,title,estimatedMinutes,exercises[]{exerciseId,sets,targetRepsMin,targetRepsMax,restSeconds,notes}} | 201 完整计划 | workout_plan,workout_plan_day,workout_plan_exercise,exercise | 本人 | 未实现/相关表未迁移；D03 |
| 22 `PATCH /api/v1/workout-plans/{planId}`（L529） | planId；示例 name/status | 未定义 | workout_plan；是否修改子项需明确 | 本人 | 未实现/相关表未迁移；D03 |
| 23 `POST /api/v1/workout-plans/{planId}/activate`（L530） | planId；请求体未说明 | 未定义 | workout_plan | 本人 | 未实现/相关表未迁移；D03 |
| 24 `DELETE /api/v1/workout-plans/{planId}`（L531） | planId | 未具体定义；通用删除规则提及 204 | workout_plan；关联执行记录 | 本人 | 未实现/相关表未迁移；D03 |
| 25 `POST /api/v1/workout-sessions`（L548） | planId/planDayId/startedAt/endedAt/status/sets[]{exerciseId,setIndex,weightKg,reps,completed,restSeconds}/fatigueLevel/painBodyPart/painLevel/note | id/planId/status/durationMinutes/totalVolumeKg/fatigueLevel/painLevel/createdAt | workout_session,workout_set_record,workout_plan,workout_plan_day | 本人，校验计划及日归属 | 未实现/相关表未迁移；D03 |
| 26 `GET /api/v1/workout-sessions`（L595） | from/to/status/page/pageSize | 分页；列表项未独立定义 | workout_session,workout_set_record,workout_feedback | 本人 | 未实现/相关表未迁移；D03 |
| 27 `GET /api/v1/workout-sessions/{sessionId}`（L596） | sessionId | 详情未独立定义 | workout_session,workout_set_record,workout_feedback | 本人 | 未实现/相关表未迁移；D03 |
| 28 `POST /api/v1/workout-sessions/{sessionId}/feedback`（L597） | sessionId；fatigueLevel/painBodyPart/painLevel/feeling/shouldAdjustNextPlan | 未定义 | workout_feedback,workout_session | 本人 | 未实现/相关表未迁移；D03 |

### 原文 §6. 食物、饮食计划与饮食记录

| 编号 / 方法＋路径（原文行号） | 请求摘要 | 响应摘要 | 涉及参考表 | 权限 | 状态 / 约束位置 |
|---|---|---|---|---|---|
| 29 `GET /api/v1/foods`（L619） | keyword/category/excludeAllergens/page/pageSize | 分页食物{id,name,category,defaultServingAmount,defaultServingUnit,nutritionPer100g,allergens,source,sourceVersion} | food,food_category,food_nutrition,food_serving,food_allergen,allergen | 登录 | 未实现/相关表未迁移；D04 |
| 30 `GET /api/v1/foods/{foodId}`（L620） | foodId | 食物详情未独立定义；有列表项可参考 | food,food_category,food_nutrition,food_serving,food_allergen,allergen | 登录 | 未实现/相关表未迁移；D04 |
| 31 `GET /api/v1/nutrition-targets/current`（L650） | 无 | 未定义 | nutrition_target | 本人 | 未实现/相关表未迁移；D05 |
| 32 `PUT /api/v1/nutrition-targets/current`（L651） | calories/proteinG/carbohydrateG/fatG/fiberG/source | 未定义 | nutrition_target | 本人 | 未实现/相关表未迁移；D05 |
| 33 `POST /api/v1/meal-records`（L670） | eatenAt/mealType/items[]{foodId,amount,unit,confirmedByUser}/inputSource/note | id/eatenAt/mealType/items[]{foodId,foodNameSnapshot,amount,unit,estimatedNutrition,confidence}/totalNutrition/createdAt | meal_record,meal_record_item,food,food_nutrition,food_serving | 本人 | 未实现/相关表未迁移；D04/D05 |
| 34 `GET /api/v1/meal-records`（L733） | date/mealType/page/pageSize | 分页；列表项未独立定义 | meal_record,meal_record_item | 本人 | 未实现/相关表未迁移；D05 |
| 35 `GET /api/v1/nutrition/daily-summary`（L734） | date | date/target/consumed/completionRate/mealCount/recordCompleteness | nutrition_target,meal_record,meal_record_item,daily_nutrition_summary | 本人 | 未实现/相关表未迁移；D05 |
| 36 `GET /api/v1/nutrition/trends`（L735） | from/to/metric | 未定义 | daily_nutrition_summary；或记录/目标聚合 | 本人 | 未实现/相关表未迁移；D05/D06 |
| 37 `GET /api/v1/meal-plans`（L769） | status/page/pageSize | 分页；列表项未定义 | meal_plan,meal_plan_day,meal_plan_meal,meal_plan_item | 本人 | 未实现/相关表未迁移；D05 |
| 38 `GET /api/v1/meal-plans/{planId}`（L770） | planId | 未定义；replace-item 需要 itemId，应补返回 | meal_plan,meal_plan_day,meal_plan_meal,meal_plan_item | 本人 | 未实现/相关表未迁移；D05 |
| 39 `POST /api/v1/meal-plans`（L771） | name/date/goal/meals[]{mealType,time,items[]{foodId,amount,unit}}/assumptions[] | 未定义 | meal_plan,meal_plan_day,meal_plan_meal,meal_plan_item；食物营养及过敏关联表 | 本人 | 未实现/相关表未迁移；D04/D05 |
| 40 `PATCH /api/v1/meal-plans/{planId}`（L772） | planId；请求字段未定义 | 未定义 | meal_plan 及子表，修改范围待明确 | 本人 | 未实现/相关表未迁移；D05 |
| 41 `POST /api/v1/meal-plans/{planId}/activate`（L773） | planId；请求体未说明 | 未定义 | meal_plan | 本人 | 未实现/相关表未迁移；D05 |
| 42 `POST /api/v1/meal-plans/{planId}/replace-item`（L774） | planId；itemId/replacementFoodId/reason | 未定义；要求重算总营养并检查过敏原 | meal_plan 及子表；food/food_nutrition/food_allergen/food_allergy/allergen | 本人，校验项属计划 | 未实现/相关表未迁移；D04/D05 |
| 43 `POST /api/v1/water-records`（L816） | recordedAt/amountMl | 未定义 | water_record | 本人 | 未实现/相关表未迁移；C02/D05 |
| 44 `GET /api/v1/water-records`（L817） | date | 未定义，集合/汇总形态待明确 | water_record | 本人 | 未实现/相关表未迁移；C02/D05 |

### 原文 §7. 首页与趋势看板

| 编号 / 方法＋路径（原文行号） | 请求摘要 | 响应摘要 | 涉及参考表 | 权限 | 状态 / 约束位置 |
|---|---|---|---|---|---|
| 45 `GET /api/v1/dashboard/overview`（L836） | date | todayWorkout/nutrition/weeklyWorkoutCompletionRate/latestWeightKg/weightChangeKg7d/quickQuestions | 训练计划/执行、营养目标/记录/汇总、body_metric | 本人 | 未实现/相关表未迁移；D06 |
| 46 `GET /api/v1/dashboard/trends`（L866） | metric/from/to；三种 metric 示例共用同接口 | metric/unit/points[]{date,value} | body_metric、workout_session、营养记录/汇总 | 本人 | 未实现/相关表未迁移；D06 |

### 原文 §8. Agent 对话与智能计划

| 编号 / 方法＋路径（原文行号） | 请求摘要 | 响应摘要 | 涉及参考表 | 权限 | 状态 / 约束位置 |
|---|---|---|---|---|---|
| 47 `POST /api/v1/agent/conversations`（L891） | title | 201 id/title/status/createdAt | agent_conversation | 本人 | 未实现/相关表未迁移；D07 |
| 48 `GET /api/v1/agent/conversations`（L916） | page/pageSize | 分页；列表项未独立定义 | agent_conversation | 本人 | 未实现/相关表未迁移；D07 |
| 49 `GET /api/v1/agent/conversations/{conversationId}`（L917） | conversationId | 详情未独立定义 | agent_conversation | 本人 | 未实现/相关表未迁移；D07 |
| 50 `GET /api/v1/agent/conversations/{conversationId}/messages`（L918） | conversationId/page/pageSize | 分页；消息字段未完整定义 | agent_message,agent_conversation | 本人 | 未实现/相关表未迁移；D07 |
| 51 `POST /api/v1/agent/conversations/{conversationId}/messages`（L924） | conversationId；content/clientMessageId/context{date,timezone} | 200 SSE：status/tool_call/source/plan_preview/token/error/done；各负载按原 §8.3 | agent_message,agent_run,agent_step,agent_tool_call；只读业务与知识表 | 本人；消息入口不保存正式计划 | 未实现/相关表未迁移；D07 |
| 52 `GET /api/v1/agent/runs/{runId}`（L1051） | runId | runId/conversationId/status/intent/steps/draftType/draft/createdAt | agent_run,agent_step,agent_conversation | 本人 | 未实现/相关表未迁移；D07 |
| 53 `POST /api/v1/agent/runs/{runId}/confirm`（L1079） | runId；accepted/draftType/comment；幂等键 | runId/status/createdResources[]{type,id} | agent_run,idempotency_record,audit_log；按草稿类型写计划/记录相关表 | 本人；重新校验草稿/权限/过敏/规则 | 未实现/相关表未迁移；D07/C04 |
| 54 `POST /api/v1/agent/conversations/{conversationId}/feedback`（L1127） | conversationId；messageId/rating/tags[]/comment | 未定义；同一用户同消息只提交一次/幂等更新 | agent_feedback,agent_message,agent_conversation | 本人 | 未实现/相关表未迁移；D07 |

### 原文 §9. 知识库接口（管理员）

| 编号 / 方法＋路径（原文行号） | 请求摘要 | 响应摘要 | 涉及参考表 | 权限 | 状态 / 约束位置 |
|---|---|---|---|---|---|
| 55 `GET /api/v1/admin/knowledge/documents`（L1150） | page/pageSize/documentType | 分页；列表字段未独立定义 | knowledge_document,knowledge_source,knowledge_chunk | 管理 | 未实现/相关表未迁移；D08 |
| 56 `POST /api/v1/admin/knowledge/documents`（L1151） | title/documentType/content/sourceName/sourceUrl/version/publishedAt | id/title/documentType/version/status/chunkCount/createdAt | knowledge_document,knowledge_source,knowledge_chunk | 管理 | 未实现/相关表未迁移；D08 |
| 57 `GET /api/v1/admin/knowledge/documents/{documentId}`（L1152） | documentId | 详情未独立定义 | knowledge_document,knowledge_source,knowledge_chunk | 管理 | 未实现/相关表未迁移；D08 |
| 58 `PATCH /api/v1/admin/knowledge/documents/{documentId}`（L1153） | documentId；更新字段未定义 | 未定义 | knowledge_document；索引更新规则待明确 | 管理 | 未实现/相关表未迁移；D08 |
| 59 `DELETE /api/v1/admin/knowledge/documents/{documentId}`（L1154） | documentId | 未具体定义 | knowledge_document,knowledge_chunk；引用处理待明确 | 管理 | 未实现/相关表未迁移；D08 |
| 60 `POST /api/v1/admin/knowledge/documents/{documentId}/reindex`（L1155） | documentId；请求体未说明 | 未定义 | knowledge_document,knowledge_chunk | 管理 | 未实现/相关表未迁移；D08 |
| 61 `GET /api/v1/admin/knowledge/search`（L1189） | q/documentType/topK | 未定义 | knowledge_document,knowledge_chunk,knowledge_source | 管理，仅检索调试 | 未实现/相关表未迁移；D08 |

### 原文 §10. 管理端接口

| 编号 / 方法＋路径（原文行号） | 请求摘要 | 响应摘要 | 涉及参考表 | 权限 | 状态 / 约束位置 |
|---|---|---|---|---|---|
| 62 `GET /api/v1/admin/users`（L1201） | keyword/role/status/page/pageSize | 分页；用户字段未定义 | sys_user,sys_user_role,sys_role | 管理 | 未实现；核心身份表已迁移，audit_log 未迁移；D08 |
| 63 `GET /api/v1/admin/users/{userId}`（L1202） | userId | 未定义；不得暴露 password_hash/token_hash | sys_user,sys_user_role,sys_role | 管理 | 未实现；核心身份表已迁移，audit_log 未迁移；D08 |
| 64 `PATCH /api/v1/admin/users/{userId}/status`（L1203） | userId；status/reason | 未定义 | sys_user,audit_log；刷新令牌处置待约定 | 管理 | 未实现；核心身份表已迁移，audit_log 未迁移；D08 |
| 65 `PATCH /api/v1/admin/users/{userId}/roles`（L1204） | userId；roles[] | 未定义 | sys_role,sys_user_role,audit_log | 管理 | 未实现；核心身份表已迁移，audit_log 未迁移；D08 |
| 66 `GET /api/v1/admin/agent/runs`（L1227） | userId/status/from/to/page/pageSize | 分页；审计字段/脱敏边界未定义 | agent_run,agent_step | 管理＋审计权限待明确 | 未实现/相关表未迁移；D08 |
| 67 `GET /api/v1/admin/agent/runs/{runId}`（L1228） | runId | 未定义 | agent_run,agent_step,agent_tool_call,agent_message | 管理＋审计权限待明确 | 未实现/相关表未迁移；D08 |
| 68 `GET /api/v1/admin/agent/tool-calls`（L1229） | toolName/page/pageSize | 分页；字段/脱敏边界未定义 | agent_tool_call,agent_run | 管理＋审计权限待明确 | 未实现/相关表未迁移；D08 |
| 69 `GET /api/v1/admin/prompts`（L1237） | promptName；分页参数未完整列出 | 未定义 | prompt_version | 管理 | 未实现/相关表未迁移；D08 |
| 70 `POST /api/v1/admin/prompts`（L1238） | promptName/version/content/modelName/temperature | 未定义 | prompt_version | 管理 | 未实现/相关表未迁移；D08 |
| 71 `PATCH /api/v1/admin/prompts/{promptId}`（L1239） | promptId；更新字段未定义 | 未定义 | prompt_version | 管理 | 未实现/相关表未迁移；D08 |
| 72 `POST /api/v1/admin/prompts/{promptId}/enable`（L1240） | promptId；请求体未说明 | 未定义；同名仅一个启用版本 | prompt_version | 管理 | 未实现/相关表未迁移；D08 |

### 原文 §11. 系统健康与配置

| 编号 / 方法＋路径（原文行号） | 请求摘要 | 响应摘要 | 涉及参考表 | 权限 | 状态 / 约束位置 |
|---|---|---|---|---|---|
| 73 `GET /actuator/health`（L1262） | 无 | 框架健康响应，不假定 ApiResponse | 不直接对应业务表 | 环境；当前允许匿名 | 框架端点待启动验证；C08 |
| 74 `GET /actuator/metrics`（L1263） | 框架参数 | 框架指标响应，需实际启用验证 | 不直接对应业务表 | 环境；当前通用认证规则 | 框架端点待启动验证；C08 |
| 75 `GET /v3/api-docs`（L1264） | 无 | OpenAPI 文档 | 不直接对应业务表 | 环境；当前无条件 permitAll | 框架端点待启动验证；C08 |
| 76 `GET /swagger-ui/index.html`（L1265） | 无 | 文档页面 | 不直接对应业务表 | 环境；当前无条件 permitAll | 框架端点待启动验证；C08 |

## 10. 全量表的契约归属与迁移状态

下表只确认源码中的定义，不证明运行数据库中存在表。公共字段缺项以完整参考 SQL 为准，V1 修正单独注明。

| 表 | 归属/接口依据 | Flyway 状态 | 参考 SQL 公共字段缺项 |
|---|---|---|---|
| `sys_user` | §2 认证 / §10.1 用户管理 | V1 已定义，3 个公共字段齐备 | 无 |
| `sys_role` | §2 认证 / §10.1 用户管理 | V1 已定义，3 个公共字段齐备 | updated_at（V1 已修正） |
| `sys_user_role` | §2 认证 / §10.1 用户管理 | V1 已定义，3 个公共字段齐备 | id, updated_at（V1 已修正） |
| `sys_refresh_token` | §2 认证 / §10.1 用户管理 | V1 已定义，3 个公共字段齐备 | updated_at（V1 已修正） |
| `gym` | 用户本轮确认保留；gym 待补契约 | V1 已定义，3 个公共字段齐备 | 无 |
| `gym_user` | 用户本轮确认保留；gym 待补契约 | V1 已定义，3 个公共字段齐备 | id, created_at, updated_at（V1 已修正） |
| `member_profile` | §3 健康/饮食档案；部分复用于 §6 | 未迁移 | id |
| `body_metric` | §3 健康/饮食档案；部分复用于 §6 | 未迁移 | updated_at |
| `health_constraint` | §3 健康/饮食档案；部分复用于 §6 | 未迁移 | 无 |
| `training_goal` | §3 健康/饮食档案；部分复用于 §6 | 未迁移 | 无 |
| `nutrition_profile` | §3 健康/饮食档案；部分复用于 §6 | 未迁移 | id |
| `allergen` | §3 健康/饮食档案；部分复用于 §6 | 未迁移 | created_at, updated_at |
| `food_category` | §6 食物/饮食计算 | 未迁移 | created_at, updated_at |
| `food` | §6 食物/饮食计算 | 未迁移 | 无 |
| `food_nutrition` | §6 食物/饮食计算 | 未迁移 | 无 |
| `food_serving` | §6 食物/饮食计算 | 未迁移 | created_at, updated_at |
| `food_allergen` | §6 食物/饮食计算 | 未迁移 | id, created_at, updated_at |
| `food_preference` | §3 健康/饮食档案；部分复用于 §6 | 未迁移 | 无 |
| `food_allergy` | §3 健康/饮食档案；部分复用于 §6 | 未迁移 | 无 |
| `dietary_restriction` | §3 健康/饮食档案；部分复用于 §6 | 未迁移 | 无 |
| `muscle_group` | §4 动作/器械；gym_equipment 非独立新增接口 | 未迁移 | created_at, updated_at |
| `exercise` | §4 动作/器械；gym_equipment 非独立新增接口 | 未迁移 | 无 |
| `exercise_muscle` | §4 动作/器械；gym_equipment 非独立新增接口 | 未迁移 | id, created_at, updated_at |
| `equipment` | §4 动作/器械；gym_equipment 非独立新增接口 | 未迁移 | 无 |
| `gym_equipment` | §4 动作/器械；gym_equipment 非独立新增接口 | 未迁移 | 无 |
| `exercise_equipment` | §4 动作/器械；gym_equipment 非独立新增接口 | 未迁移 | id, created_at, updated_at |
| `workout_plan` | §5 训练；§7 汇总；§8 确认 | 未迁移 | 无 |
| `workout_plan_day` | §5 训练；§7 汇总；§8 确认 | 未迁移 | created_at, updated_at |
| `workout_plan_exercise` | §5 训练；§7 汇总；§8 确认 | 未迁移 | created_at, updated_at |
| `workout_session` | §5 训练；§7 汇总；§8 确认 | 未迁移 | 无 |
| `workout_set_record` | §5 训练；§7 汇总；§8 确认 | 未迁移 | updated_at |
| `workout_feedback` | §5 训练；§7 汇总；§8 确认 | 未迁移 | updated_at |
| `nutrition_target` | §6 饮食；§7 汇总；§8 确认 | 未迁移 | 无 |
| `recipe` | 原项目方案 §15.3 已有食谱设计；当前无独立接口，规划保留，不补 CRUD | 未迁移 | 无 |
| `recipe_ingredient` | 原项目方案 §15.3 已有食谱设计；当前无独立接口，规划保留，不补 CRUD | 未迁移 | created_at, updated_at |
| `meal_plan` | §6 饮食；§7 汇总；§8 确认 | 未迁移 | 无 |
| `meal_plan_day` | §6 饮食；§7 汇总；§8 确认 | 未迁移 | created_at, updated_at |
| `meal_plan_meal` | §6 饮食；§7 汇总；§8 确认 | 未迁移 | created_at, updated_at |
| `meal_plan_item` | §6 饮食；§7 汇总；§8 确认 | 未迁移 | created_at, updated_at |
| `meal_record` | §6 饮食；§7 汇总；§8 确认 | 未迁移 | 无 |
| `meal_record_item` | §6 饮食；§7 汇总；§8 确认 | 未迁移 | updated_at |
| `daily_nutrition_summary` | §6 饮食；§7 汇总；§8 确认 | 未迁移 | created_at, updated_at |
| `water_record` | §6 饮食；§7 汇总；§8 确认 | 未迁移 | updated_at |
| `knowledge_source` | §9 知识库；§8 来源引用 | 未迁移 | updated_at |
| `knowledge_document` | §9 知识库；§8 来源引用 | 未迁移 | 无 |
| `knowledge_chunk` | §9 知识库；§8 来源引用 | 未迁移 | updated_at |
| `prompt_version` | §10.3 Prompt；§8 运行关联 | 未迁移 | updated_at |
| `agent_conversation` | §8 Agent；§10.2 审计 | 未迁移 | 无 |
| `agent_message` | §8 Agent；§10.2 审计 | 未迁移 | updated_at |
| `agent_run` | §8 Agent；§10.2 审计 | 未迁移 | updated_at |
| `agent_step` | §8 Agent；§10.2 审计 | 未迁移 | created_at, updated_at |
| `agent_tool_call` | §8 Agent；§10.2 审计 | 未迁移 | updated_at |
| `agent_feedback` | §8 Agent；§10.2 审计 | 未迁移 | updated_at |
| `agent_memory` | 原项目方案 §14 记忆；内部支持，无独立接口，不补 CRUD | 未迁移 | 无 |
| `chat_memory_message` | 原项目方案 §14 记忆；内部支持，无独立接口，不补 CRUD | 未迁移 | updated_at |
| `idempotency_record` | §1.1 / §8.5 幂等要求 | 未迁移 | 无 |
| `audit_log` | §8.5 / §10 管理审计支持 | 未迁移 | updated_at |

## 11. 检查完成记录与后续使用方式

- 已按原文档逐项登记 76 个不同方法＋路径；方法和路径从原文提取，见第 9 节原文行号；查询参数合并但保留在请求摘要。
- 已盘点完整 SQL 的 57 张表和 V1 的 6 张表，公共字段缺项逐表列出；没有把规划表当作已迁移表。
- 已检查当前 5 个认证接口的 DTO/业务/响应、Security、异常、trace、时间和测试文件；原契约未说明之处标为待明确，不按前端猜测值补成事实。
- 已区分直接字段、聚合/计算字段、真实缺列、规则不完整、未实现和已授权 gym 范围。
- 本轮只新增此检查文档。原接口文档、参考 SQL、V1、业务代码、前端均未由本轮改动；未运行数据库迁移、构建、HTTP 测试或部署。
- 下一步首先处理第 7 节顺序 1～2，再进入健康档案迁移设计；本报告中的“建议/候选/待明确”不是已批准的契约变更。
