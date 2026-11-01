# SmartGym Copilot 接口文档

- 文档版本：v1.0
- 接口前缀：`/api/v1`
- 适用范围：会员端、Agent 对话端、管理后台
- 数据格式：`application/json; charset=utf-8`
- 字段命名：JSON 使用 `camelCase`
- 时间格式：ISO 8601，例如 `2026-10-31T10:30:00+08:00`
- 日期格式：`yyyy-MM-dd`
- 金额和营养数值：使用 `number`，单位在字段名或说明中标明

本文档是前后端并行开发的接口契约。接口字段、枚举值和 SSE 事件名称完成评审后，应通过 OpenAPI 生成前端类型和后端接口骨架。

---

## 1. 通用约定

### 1.1 请求头

除注册、登录和刷新 Token 外，所有接口都需要：

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
X-Request-Id: <可选，客户端生成的请求标识>
```

涉及写入的接口还需要：

```http
Idempotency-Key: <uuid>
```

`Idempotency-Key` 在同一用户、同一路径下有效期建议为 24 小时。重复提交同一个 key 时，服务端返回第一次提交的结果，不得重复创建业务数据。

### 1.2 统一成功响应

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "tr_01J9ABCDEF",
  "timestamp": "2026-10-31T10:30:00+08:00"
}
```

### 1.3 统一失败响应

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "请求参数校验失败",
  "data": {
    "fieldErrors": [
      {
        "field": "heightCm",
        "reason": "must be between 50 and 250"
      }
    ]
  },
  "traceId": "tr_01J9ABCDEF",
  "timestamp": "2026-10-31T10:30:00+08:00"
}
```

### 1.4 分页响应

所有分页接口统一使用 `page` 和 `pageSize`，默认值分别为 `1` 和 `20`，最大 `100`。

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "items": [],
    "page": 1,
    "pageSize": 20,
    "total": 0,
    "totalPages": 0
  },
  "traceId": "tr_01J9ABCDEF",
  "timestamp": "2026-10-31T10:30:00+08:00"
}
```

### 1.5 常用 HTTP 状态码

| 状态码 | 使用场景 |
|---:|---|
| 200 | 查询、修改、确认成功 |
| 201 | 创建成功 |
| 204 | 删除成功且无响应体 |
| 400 | 请求格式错误或业务参数非法 |
| 401 | 未登录、Token 过期或无效 |
| 403 | 无权限访问资源 |
| 404 | 资源不存在 |
| 409 | 幂等冲突、状态冲突或重复操作 |
| 422 | 结构化数据或业务规则校验失败 |
| 429 | 访问频率超过限制 |
| 500 | 服务端异常 |
| 503 | 模型或依赖服务暂时不可用 |

### 1.6 错误码

| 错误码 | 含义 |
|---|---|
| `AUTH_INVALID_CREDENTIALS` | 用户名或密码错误 |
| `AUTH_TOKEN_INVALID` | Access Token 无效 |
| `AUTH_TOKEN_EXPIRED` | Access Token 已过期 |
| `FORBIDDEN` | 当前用户无权执行操作 |
| `RESOURCE_NOT_FOUND` | 资源不存在 |
| `VALIDATION_ERROR` | 参数校验失败 |
| `BUSINESS_RULE_VIOLATION` | 训练、营养或安全规则不通过 |
| `CONFIRMATION_REQUIRED` | 写操作需要用户确认 |
| `IDEMPOTENCY_CONFLICT` | 幂等键对应的请求内容不一致 |
| `AGENT_RUN_NOT_FOUND` | Agent 运行记录不存在 |
| `AGENT_OUTPUT_INVALID` | 模型结构化输出无法通过解析或校验 |
| `SAFETY_BLOCKED` | 高风险请求已被拦截 |
| `MODEL_UNAVAILABLE` | 模型服务不可用 |
| `RATE_LIMITED` | 请求频率过高 |
| `INTERNAL_ERROR` | 未分类服务端错误 |

---

## 2. 认证与当前用户

### 2.1 注册

```http
POST /api/v1/auth/register
```

请求：

```json
{
  "account": "zhangsan@example.com",
  "password": "Password@123",
  "displayName": "张三"
}
```

字段：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `account` | string | 是 | 邮箱或手机号，首版建议使用邮箱 |
| `password` | string | 是 | 8～64 位 |
| `displayName` | string | 是 | 2～30 个字符 |

响应 `201`：

```json
{
  "success": true,
  "code": "OK",
  "message": "注册成功",
  "data": {
    "userId": 10001,
    "account": "zhangsan@example.com",
    "displayName": "张三"
  },
  "traceId": "tr_01J9ABCDEF",
  "timestamp": "2026-10-31T10:30:00+08:00"
}
```

### 2.2 登录

```http
POST /api/v1/auth/login
```

请求：

```json
{
  "account": "zhangsan@example.com",
  "password": "Password@123"
}
```

响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "rt_01J9ABCDEF",
    "expiresIn": 7200,
    "user": {
      "id": 10001,
      "account": "zhangsan@example.com",
      "displayName": "张三",
      "roles": ["MEMBER"]
    }
  },
  "traceId": "tr_01J9ABCDEF",
  "timestamp": "2026-10-31T10:30:00+08:00"
}
```

### 2.3 刷新 Token

```http
POST /api/v1/auth/refresh
```

请求：

```json
{
  "refreshToken": "rt_01J9ABCDEF"
}
```

响应：与登录接口中的 Token 字段相同。

### 2.4 退出登录

```http
POST /api/v1/auth/logout
```

请求体为空。服务端使 refresh token 失效。

### 2.5 当前用户

```http
GET /api/v1/auth/me
```

响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "id": 10001,
    "account": "zhangsan@example.com",
    "displayName": "张三",
    "avatarUrl": null,
    "roles": ["MEMBER"],
    "createdAt": "2026-10-01T09:00:00+08:00"
  },
  "traceId": "tr_01J9ABCDEF",
  "timestamp": "2026-10-31T10:30:00+08:00"
}
```

---

## 3. 会员健康档案

### 3.1 查询健康档案

```http
GET /api/v1/member/profile
```

响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "userId": 10001,
    "gender": "MALE",
    "birthDate": "1995-05-20",
    "heightCm": 175.0,
    "weightKg": 72.5,
    "trainingExperience": "BEGINNER",
    "weeklyTrainingDays": 4,
    "maxSessionMinutes": 45,
    "goals": ["FAT_LOSS"],
    "updatedAt": "2026-10-31T10:30:00+08:00"
  },
  "traceId": "tr_01J9ABCDEF",
  "timestamp": "2026-10-31T10:30:00+08:00"
}
```

### 3.2 新建或更新健康档案

```http
PUT /api/v1/member/profile
```

请求：

```json
{
  "gender": "MALE",
  "birthDate": "1995-05-20",
  "heightCm": 175.0,
  "weightKg": 72.5,
  "trainingExperience": "BEGINNER",
  "weeklyTrainingDays": 4,
  "maxSessionMinutes": 45,
  "goals": ["FAT_LOSS"]
}
```

枚举：

- `gender`: `MALE`、`FEMALE`、`OTHER`、`UNKNOWN`
- `trainingExperience`: `BEGINNER`、`INTERMEDIATE`、`ADVANCED`
- `goals`: `MUSCLE_GAIN`、`FAT_LOSS`、`BODY_SHAPING`、`MAINTENANCE`

响应：返回完整健康档案。

### 3.3 身体指标记录

```http
GET /api/v1/member/body-metrics?from=2026-10-01&to=2026-10-31&page=1&pageSize=20
POST /api/v1/member/body-metrics
```

创建请求：

```json
{
  "measuredAt": "2026-10-31T08:00:00+08:00",
  "weightKg": 72.5,
  "bodyFatPercent": 19.8,
  "waistCm": 82.0,
  "note": "晨起空腹"
}
```

响应数据：

```json
{
  "id": 90001,
  "measuredAt": "2026-10-31T08:00:00+08:00",
  "weightKg": 72.5,
  "bodyFatPercent": 19.8,
  "waistCm": 82.0,
  "note": "晨起空腹"
}
```

### 3.4 身体限制、过敏和饮食偏好

```http
GET /api/v1/member/health-constraints
PUT /api/v1/member/health-constraints
GET /api/v1/member/nutrition-profile
PUT /api/v1/member/nutrition-profile
```

健康限制请求：

```json
{
  "constraints": [
    {
      "type": "PAIN",
      "bodyPart": "KNEE",
      "severity": "MILD",
      "description": "深蹲时膝盖不适",
      "active": true
    }
  ],
  "medicalNotice": "无已知慢性疾病"
}
```

饮食档案请求：

```json
{
  "dietType": "OMNIVORE",
  "allergies": ["PEANUT"],
  "restrictions": ["LOW_SUGAR"],
  "dislikedFoods": ["香菜"],
  "preferredCuisines": ["HOME_STYLE"],
  "dailyWaterMlTarget": 2000
}
```

`type` 可取：`PAIN`、`INJURY`、`CHRONIC_DISEASE`、`PREGNANCY`、`OTHER`。高风险健康信息只用于安全判断，不能被 Agent 当作医疗诊断依据。

---

## 4. 动作、肌群和器械

### 4.1 查询动作

```http
GET /api/v1/exercises?keyword=深蹲&muscleGroup=LEGS&equipmentId=101&page=1&pageSize=20
GET /api/v1/exercises/{exerciseId}
```

列表项：

```json
{
  "id": 201,
  "name": "高脚杯深蹲",
  "englishName": "Goblet Squat",
  "primaryMuscles": ["QUADRICEPS", "GLUTES"],
  "equipment": [{"id": 101, "name": "哑铃"}],
  "difficulty": "BEGINNER",
  "defaultRestSeconds": 90,
  "contraindications": ["严重膝关节疼痛"],
  "thumbnailUrl": null
}
```

### 4.2 查询器械

```http
GET /api/v1/equipment?status=AVAILABLE&page=1&pageSize=20
GET /api/v1/equipment/{equipmentId}
```

响应项：

```json
{
  "id": 101,
  "name": "哑铃",
  "category": "FREE_WEIGHT",
  "status": "AVAILABLE",
  "gymId": 1,
  "remark": "2.5kg～30kg"
}
```

---

## 5. 训练计划与训练记录

### 5.1 查询训练计划

```http
GET /api/v1/workout-plans?status=ACTIVE&page=1&pageSize=20
GET /api/v1/workout-plans/today?date=2026-10-31
GET /api/v1/workout-plans/{planId}
```

计划响应：

```json
{
  "id": 30001,
  "name": "减脂四日训练计划",
  "source": "AGENT",
  "status": "ACTIVE",
  "goal": "FAT_LOSS",
  "durationWeeks": 4,
  "trainingDaysPerWeek": 4,
  "days": [
    {
      "id": 30011,
      "dayOfWeek": 1,
      "title": "腿部训练",
      "estimatedMinutes": 45,
      "exercises": [
        {
          "exerciseId": 201,
          "exerciseName": "高脚杯深蹲",
          "sets": 3,
          "targetReps": "10-12",
          "restSeconds": 90,
          "tempo": null,
          "notes": "膝盖不适时降低深度"
        }
      ]
    }
  ],
  "warnings": ["存在膝盖不适，请根据疼痛情况调整"],
  "createdAt": "2026-10-31T10:30:00+08:00"
}
```

### 5.2 手动创建训练计划

```http
POST /api/v1/workout-plans
```

请求：

```json
{
  "name": "我的四周训练计划",
  "goal": "FAT_LOSS",
  "durationWeeks": 4,
  "trainingDaysPerWeek": 4,
  "days": [
    {
      "dayOfWeek": 1,
      "title": "腿部训练",
      "estimatedMinutes": 45,
      "exercises": [
        {
          "exerciseId": 201,
          "sets": 3,
          "targetRepsMin": 10,
          "targetRepsMax": 12,
          "restSeconds": 90,
          "notes": ""
        }
      ]
    }
  ]
}
```

响应 `201`：返回完整训练计划。

### 5.3 修改、激活和删除训练计划

```http
PATCH /api/v1/workout-plans/{planId}
POST /api/v1/workout-plans/{planId}/activate
DELETE /api/v1/workout-plans/{planId}
```

PATCH 示例：

```json
{
  "name": "减脂四日训练计划（调整版）",
  "status": "PAUSED"
}
```

删除和状态变更必须检查当前用户权限。已产生训练记录的计划建议采用逻辑删除或归档。

### 5.4 创建训练执行记录

```http
POST /api/v1/workout-sessions
```

请求：

```json
{
  "planId": 30001,
  "planDayId": 30011,
  "startedAt": "2026-10-31T19:00:00+08:00",
  "endedAt": "2026-10-31T19:43:00+08:00",
  "status": "COMPLETED",
  "sets": [
    {
      "exerciseId": 201,
      "setIndex": 1,
      "weightKg": 20.0,
      "reps": 12,
      "completed": true,
      "restSeconds": 90
    }
  ],
  "fatigueLevel": 6,
  "painBodyPart": null,
  "painLevel": 0,
  "note": "整体状态良好"
}
```

响应：

```json
{
  "id": 40001,
  "planId": 30001,
  "status": "COMPLETED",
  "durationMinutes": 43,
  "totalVolumeKg": 720.0,
  "fatigueLevel": 6,
  "painLevel": 0,
  "createdAt": "2026-10-31T19:45:00+08:00"
}
```

### 5.5 查询训练记录和反馈

```http
GET /api/v1/workout-sessions?from=2026-10-01&to=2026-10-31&status=COMPLETED&page=1&pageSize=20
GET /api/v1/workout-sessions/{sessionId}
POST /api/v1/workout-sessions/{sessionId}/feedback
```

反馈请求：

```json
{
  "fatigueLevel": 7,
  "painBodyPart": "KNEE",
  "painLevel": 3,
  "feeling": "右膝在下蹲时轻微不适",
  "shouldAdjustNextPlan": true
}
```

---

## 6. 食物、饮食计划与饮食记录

### 6.1 查询食物

```http
GET /api/v1/foods?keyword=鸡蛋&category=PROTEIN&excludeAllergens=PEANUT&page=1&pageSize=20
GET /api/v1/foods/{foodId}
```

响应项：

```json
{
  "id": 501,
  "name": "鸡蛋",
  "category": "PROTEIN",
  "defaultServingAmount": 50,
  "defaultServingUnit": "g",
  "nutritionPer100g": {
    "calories": 143,
    "protein": 12.6,
    "carbohydrate": 1.1,
    "fat": 9.5,
    "fiber": 0,
    "sugar": 1.1,
    "sodium": 142
  },
  "allergens": [],
  "source": "USDA",
  "sourceVersion": "2026-01"
}
```

### 6.2 查询和更新营养目标

```http
GET /api/v1/nutrition-targets/current
PUT /api/v1/nutrition-targets/current
```

请求：

```json
{
  "calories": 2200,
  "proteinG": 150,
  "carbohydrateG": 250,
  "fatG": 70,
  "fiberG": 30,
  "source": "USER_CONFIRMED"
}
```

### 6.3 创建饮食记录

```http
POST /api/v1/meal-records
```

请求：

```json
{
  "eatenAt": "2026-10-31T08:00:00+08:00",
  "mealType": "BREAKFAST",
  "items": [
    {
      "foodId": 501,
      "amount": 100,
      "unit": "g",
      "confirmedByUser": true
    },
    {
      "foodId": 502,
      "amount": 250,
      "unit": "ml",
      "confirmedByUser": true
    }
  ],
  "inputSource": "MANUAL",
  "note": ""
}
```

响应：

```json
{
  "id": 60001,
  "eatenAt": "2026-10-31T08:00:00+08:00",
  "mealType": "BREAKFAST",
  "items": [
    {
      "foodId": 501,
      "foodNameSnapshot": "鸡蛋",
      "amount": 100,
      "unit": "g",
      "estimatedNutrition": {
        "calories": 143,
        "proteinG": 12.6,
        "carbohydrateG": 1.1,
        "fatG": 9.5
      },
      "confidence": 1.0
    }
  ],
  "totalNutrition": {
    "calories": 310,
    "proteinG": 20.0,
    "carbohydrateG": 30.0,
    "fatG": 12.0
  },
  "createdAt": "2026-10-31T08:05:00+08:00"
}
```

### 6.4 查询饮食记录和每日汇总

```http
GET /api/v1/meal-records?date=2026-10-31&mealType=BREAKFAST&page=1&pageSize=20
GET /api/v1/nutrition/daily-summary?date=2026-10-31
GET /api/v1/nutrition/trends?from=2026-10-01&to=2026-10-31&metric=PROTEIN
```

每日汇总响应：

```json
{
  "date": "2026-10-31",
  "target": {
    "calories": 2200,
    "proteinG": 150,
    "carbohydrateG": 250,
    "fatG": 70
  },
  "consumed": {
    "calories": 1680,
    "proteinG": 122.5,
    "carbohydrateG": 180.0,
    "fatG": 48.0
  },
  "completionRate": {
    "calories": 0.76,
    "protein": 0.82,
    "carbohydrate": 0.72,
    "fat": 0.69
  },
  "mealCount": 3,
  "recordCompleteness": 0.75
}
```

### 6.5 饮食计划

```http
GET /api/v1/meal-plans?status=ACTIVE&page=1&pageSize=20
GET /api/v1/meal-plans/{planId}
POST /api/v1/meal-plans
PATCH /api/v1/meal-plans/{planId}
POST /api/v1/meal-plans/{planId}/activate
POST /api/v1/meal-plans/{planId}/replace-item
```

创建请求：

```json
{
  "name": "减脂训练日饮食计划",
  "date": "2026-10-31",
  "goal": "FAT_LOSS",
  "meals": [
    {
      "mealType": "LUNCH",
      "time": "12:00",
      "items": [
        {
          "foodId": 503,
          "amount": 150,
          "unit": "g"
        }
      ]
    }
  ],
  "assumptions": ["食物重量按可食用部分估算"]
}
```

替换食物请求：

```json
{
  "itemId": 70011,
  "replacementFoodId": 504,
  "reason": "不含花生且蛋白质接近"
}
```

服务端必须重新计算整份计划营养总量并执行过敏原检查。

### 6.6 饮水记录

```http
POST /api/v1/water-records
GET /api/v1/water-records?date=2026-10-31
```

请求：

```json
{
  "recordedAt": "2026-10-31T10:00:00+08:00",
  "amountMl": 300
}
```

---

## 7. 首页与趋势看板

### 7.1 首页概览

```http
GET /api/v1/dashboard/overview?date=2026-10-31
```

响应：

```json
{
  "date": "2026-10-31",
  "todayWorkout": {
    "planId": 30001,
    "title": "腿部训练",
    "estimatedMinutes": 45,
    "status": "NOT_STARTED"
  },
  "nutrition": {
    "caloriesConsumed": 1680,
    "caloriesTarget": 2200,
    "proteinConsumedG": 122.5,
    "proteinTargetG": 150
  },
  "weeklyWorkoutCompletionRate": 0.75,
  "latestWeightKg": 72.5,
  "weightChangeKg7d": -0.6,
  "quickQuestions": ["今天训练前应该吃什么？", "帮我调整今天的训练"]
}
```

### 7.2 趋势数据

```http
GET /api/v1/dashboard/trends?metric=WEIGHT&from=2026-10-01&to=2026-10-31
GET /api/v1/dashboard/trends?metric=TRAINING_VOLUME&from=2026-10-01&to=2026-10-31
GET /api/v1/dashboard/trends?metric=NUTRITION&from=2026-10-01&to=2026-10-31
```

响应：

```json
{
  "metric": "WEIGHT",
  "unit": "kg",
  "points": [
    {"date": "2026-10-01", "value": 73.1},
    {"date": "2026-10-31", "value": 72.5}
  ]
}
```

---

## 8. Agent 对话与智能计划

### 8.1 创建会话

```http
POST /api/v1/agent/conversations
```

请求：

```json
{
  "title": "我的训练和饮食咨询"
}
```

响应 `201`：

```json
{
  "id": "conv_01J9ABCDEF",
  "title": "我的训练和饮食咨询",
  "status": "ACTIVE",
  "createdAt": "2026-10-31T10:30:00+08:00"
}
```

### 8.2 查询会话和消息

```http
GET /api/v1/agent/conversations?page=1&pageSize=20
GET /api/v1/agent/conversations/{conversationId}
GET /api/v1/agent/conversations/{conversationId}/messages?page=1&pageSize=50
```

### 8.3 发送消息并获取 SSE 流

```http
POST /api/v1/agent/conversations/{conversationId}/messages
Accept: text/event-stream
Content-Type: application/json
```

请求：

```json
{
  "content": "今天晚上练腿，中午食堂有米饭、鸡腿、鱼、青菜、花生拌菜和炸鸡，我应该怎么选？晚上训练只有45分钟。",
  "clientMessageId": "msg_client_001",
  "context": {
    "date": "2026-10-31",
    "timezone": "Asia/Shanghai"
  }
}
```

服务端返回 `200 text/event-stream`。每条事件格式如下：

```text
event: status
data: {"runId":"run_01J9ABCDEF","node":"load_profile","message":"正在读取健康档案"}

```

事件定义：

#### `status`

```json
{
  "runId": "run_01J9ABCDEF",
  "node": "load_profile",
  "message": "正在读取健康档案"
}
```

`node` 示例：`intent_detection`、`load_profile`、`retrieve_knowledge`、`generate_plan`、`validate`、`waiting_confirmation`、`completed`。

#### `tool_call`

```json
{
  "runId": "run_01J9ABCDEF",
  "toolCallId": "tool_001",
  "tool": "getNutritionProfile",
  "status": "success",
  "durationMs": 86,
  "summary": "已读取饮食偏好和过敏信息"
}
```

`status`：`running`、`success`、`failed`。

#### `source`

```json
{
  "runId": "run_01J9ABCDEF",
  "sources": [
    {
      "documentId": 80001,
      "title": "训练日前后碳水安排",
      "version": "v2",
      "sourceUrl": null,
      "relevance": 0.89
    }
  ]
}
```

#### `plan_preview`

```json
{
  "runId": "run_01J9ABCDEF",
  "draftType": "WORKOUT_AND_MEAL_PLAN",
  "requiresConfirmation": true,
  "draft": {
    "workoutPlan": {},
    "mealPlan": {},
    "warnings": ["花生拌菜可能含有花生，已排除"],
    "assumptions": ["鸡腿重量按去骨可食用部分估算"]
  },
  "expiresAt": "2026-10-31T10:45:00+08:00"
}
```

#### `token`

```json
{
  "runId": "run_01J9ABCDEF",
  "content": "根据你的花生过敏信息，午餐建议优先选择鱼、米饭和青菜。"
}
```

#### `error`

```json
{
  "runId": "run_01J9ABCDEF",
  "code": "MODEL_UNAVAILABLE",
  "message": "当前智能服务暂时不可用，请稍后重试"
}
```

#### `done`

```json
{
  "runId": "run_01J9ABCDEF",
  "messageId": "msg_01J9ABCDEF",
  "status": "WAITING_CONFIRMATION",
  "usage": {
    "inputTokens": 1200,
    "outputTokens": 480
  }
}
```

Agent 写操作必须先返回 `plan_preview`，不得在发送消息接口中直接保存训练计划或饮食计划。

### 8.4 查询 Agent 运行详情

```http
GET /api/v1/agent/runs/{runId}
```

响应：

```json
{
  "runId": "run_01J9ABCDEF",
  "conversationId": "conv_01J9ABCDEF",
  "status": "WAITING_CONFIRMATION",
  "intent": "WORKOUT_AND_MEAL_PLAN",
  "steps": [
    {
      "node": "load_profile",
      "status": "SUCCESS",
      "startedAt": "2026-10-31T10:30:01+08:00",
      "durationMs": 86
    }
  ],
  "draftType": "WORKOUT_AND_MEAL_PLAN",
  "draft": {},
  "createdAt": "2026-10-31T10:30:00+08:00"
}
```

### 8.5 确认或拒绝 Agent 草稿

```http
POST /api/v1/agent/runs/{runId}/confirm
```

请求：

```json
{
  "accepted": true,
  "draftType": "WORKOUT_AND_MEAL_PLAN",
  "comment": "确认保存"
}
```

拒绝或要求修改：

```json
{
  "accepted": false,
  "draftType": "WORKOUT_AND_MEAL_PLAN",
  "comment": "把深蹲替换成膝盖压力小一点的动作"
}
```

响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "方案已保存",
  "data": {
    "runId": "run_01J9ABCDEF",
    "status": "CONFIRMED",
    "createdResources": [
      {"type": "WORKOUT_PLAN", "id": 30001},
      {"type": "MEAL_PLAN", "id": 70001}
    ]
  },
  "traceId": "tr_01J9ABCDEF",
  "timestamp": "2026-10-31T10:45:00+08:00"
}
```

确认接口必须携带 `Idempotency-Key`，服务端需要重新加载草稿、重新执行权限检查、过敏原检查和业务规则校验，然后在一个事务中保存。

### 8.6 Agent 反馈

```http
POST /api/v1/agent/conversations/{conversationId}/feedback
```

请求：

```json
{
  "messageId": "msg_01J9ABCDEF",
  "rating": 5,
  "tags": ["USEFUL", "ACCURATE"],
  "comment": "建议比较实用"
}
```

`rating` 范围为 `1～5`，同一用户对同一消息只能提交一次，可使用幂等键更新反馈。

---

## 9. 知识库接口（管理员）

### 9.1 文档管理

```http
GET /api/v1/admin/knowledge/documents?page=1&pageSize=20&documentType=NUTRITION
POST /api/v1/admin/knowledge/documents
GET /api/v1/admin/knowledge/documents/{documentId}
PATCH /api/v1/admin/knowledge/documents/{documentId}
DELETE /api/v1/admin/knowledge/documents/{documentId}
POST /api/v1/admin/knowledge/documents/{documentId}/reindex
```

创建请求：

```json
{
  "title": "训练日前后碳水安排",
  "documentType": "NUTRITION",
  "content": "训练日前后应根据训练强度和个人目标安排碳水摄入……",
  "sourceName": "系统营养知识库",
  "sourceUrl": null,
  "version": "v2",
  "publishedAt": "2026-10-01"
}
```

响应：

```json
{
  "id": 80001,
  "title": "训练日前后碳水安排",
  "documentType": "NUTRITION",
  "version": "v2",
  "status": "INDEXING",
  "chunkCount": 0,
  "createdAt": "2026-10-31T10:30:00+08:00"
}
```

### 9.2 知识检索测试

```http
GET /api/v1/admin/knowledge/search?q=训练前应该吃什么&documentType=NUTRITION&topK=5
```

该接口只用于管理员调试和 RAG 质量检查，不直接替代 Agent 的检索工具。

---

## 10. 管理端接口

### 10.1 用户管理

```http
GET /api/v1/admin/users?keyword=张三&role=MEMBER&status=ACTIVE&page=1&pageSize=20
GET /api/v1/admin/users/{userId}
PATCH /api/v1/admin/users/{userId}/status
PATCH /api/v1/admin/users/{userId}/roles
```

状态更新请求：

```json
{
  "status": "DISABLED",
  "reason": "管理员操作"
}
```

角色更新请求：

```json
{
  "roles": ["MEMBER"]
}
```

### 10.2 Agent 审计日志

```http
GET /api/v1/admin/agent/runs?userId=10001&status=FAILED&from=2026-10-01&to=2026-10-31&page=1&pageSize=20
GET /api/v1/admin/agent/runs/{runId}
GET /api/v1/admin/agent/tool-calls?toolName=validateTrainingPlan&page=1&pageSize=20
```

敏感字段默认脱敏，管理员只有在具备审计权限时才能查看受限内容。

### 10.3 Prompt 版本管理

```http
GET /api/v1/admin/prompts?promptName=workout-plan
POST /api/v1/admin/prompts
PATCH /api/v1/admin/prompts/{promptId}
POST /api/v1/admin/prompts/{promptId}/enable
```

创建请求：

```json
{
  "promptName": "workout-plan",
  "version": "v3",
  "content": "你是一名安全的健身计划助手……",
  "modelName": "deepseek-chat",
  "temperature": 0.2
}
```

同一个 `promptName` 同时只能有一个启用版本。

---

## 11. 系统健康与配置

```http
GET /actuator/health
GET /actuator/metrics
GET /v3/api-docs
GET /swagger-ui/index.html
```

`/actuator` 和 Swagger 生产环境必须进行访问控制，不能匿名暴露敏感指标或内部接口。

---

## 12. 前后端联调约定

### 12.1 前端状态处理

前端统一根据 `success`、HTTP 状态码和 `code` 处理结果，不根据 `message` 文案判断业务逻辑。枚举值由后端返回，前端展示文案自行维护。

### 12.2 Agent SSE 重连

- 客户端保存最近一次 `runId` 和事件顺序。
- 网络中断后，优先调用 `GET /agent/runs/{runId}` 查询最终状态。
- 对于已经收到 `done` 的运行，不重复提交确认。
- `token` 事件只用于展示文本，业务数据以 `plan_preview` 和确认接口响应为准。

### 12.3 时间和时区

- 前端提交带时区的时间戳。
- 业务日期字段以用户设置的时区计算。
- 服务端数据库统一保存带时区转换后的 UTC 时间，响应时转换为用户时区。

### 12.4 权限边界

- 会员只能访问自己的健康档案、训练、饮食和 Agent 会话。
- 教练只能访问授权会员的数据。
- 管理员接口统一使用 `/admin` 前缀，并检查管理员角色和操作权限。
- Agent 工具不接受前端传入的任意 `userId`，默认从登录上下文取得当前用户身份。

### 12.5 建议的联调顺序

1. 第一步：认证、当前用户和健康档案。
2. 第二步：动作、器械、训练计划和训练记录。
3. 第三步：食物、营养目标、饮食记录和每日汇总。
4. 第四步：首页、趋势图和管理端基础页面。
5. 第五步：Agent 会话、SSE、计划预览和确认。
6. 第六步：RAG、来源引用、记忆和训练饮食联动。

---

## 13. 首版暂不开放的接口

以下能力先保留在产品规划中，不进入 v1.0 接口契约：

- 多 Agent 之间的内部通信接口
- 摄像头动作识别接口
- 食物图片识别接口
- 智能手环和 IoT 设备接口
- 外卖平台菜单同步接口
- 教练端复杂排课和会员授权接口

后续新增接口必须保持 `/api/v1` 的兼容性；破坏性修改使用 `/api/v2`，或者通过新增字段实现向后兼容。
