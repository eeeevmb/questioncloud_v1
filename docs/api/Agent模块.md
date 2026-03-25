# Agent模块 API

## 创建会话
### 请求路径
```http
POST /api/v1/agent
```

### 简要说明
> 创建一个新的 Agent 聊天会话，需要登录（Sa-Token）
>

### 请求头
| **名称** | **示例值** | **必选** | **说明** |
| --- | --- | --- | --- |
| Cookie | satoken=86e8... | 是 | 登录态凭证 |
| Content-Type | application/json | 是 | 请求体格式(json) |


### 请求体
| **名称** | **类型** | **必选** | **说明** |
| --- | --- | --- | --- |
| agentName | string | 是 | 智能体名称 |


### 路径参数
无

### 查询参数
无

### 请求示例
```json
POST /api/v1/agent HTTP/1.1
Cookie: satoken=86e8bcaa-29da-4d27-a78a-d0df4362741d
Content-Type: application/json

{
  "agentName": "题库小助手"
}
```

### 成功响应示例
```json
{
  "code": "200000",
  "message": "请求成功",
  "data": {
    "sessionId": "161019495270318081",
    "agentName": "题库小助手",
    "userId": "158317222236782592",
    "title": null,
    "metadata": null,
    "createdAt": "2026-03-25T07:53:59.269154",
    "updatedAt": "2026-03-25T07:53:59.269154"
  }
}
```

### 失败响应示例（智能体不存在）
```json
{
  "code": "A00001",
  "message": "智能体不存在",
  "data": null
}
```

## 获取会话列表
### 请求路径
```http
GET /api/v1/agent
```

### 简要说明
> 获取当前登录用户的 Agent 会话列表
>

### 请求头
| **名称** | **示例值** | **必选** | **说明** |
| --- | --- | --- | --- |
| Cookie | satoken=86e8... | 是 | 登录态凭证 |
| Content-Type | application/json | 否 | 本接口无请求体，仅为统一规范可携带 |


### 请求体
无

### 路径参数
无

### 查询参数
无

### 请求示例
```json
GET /api/v1/agent HTTP/1.1
Cookie: satoken=86e8bcaa-29da-4d27-a78a-d0df4362741d
```

### 成功响应示例
```json
{
  "code": "200000",
  "message": "请求成功",
  "data": [
    {
      "sessionId": "158317350439878657",
      "agentName": "题库小助手",
      "userId": "158317222236782592",
      "title": "你好",
      "metadata": null,
      "createdAt": "2026-03-17T20:56:38",
      "updatedAt": "2026-03-17T20:56:38"
    },
    {
      "sessionId": "161019495270318081",
      "agentName": "题库小助手",
      "userId": "158317222236782592",
      "title": null,
      "metadata": null,
      "createdAt": "2026-03-25T07:53:59",
      "updatedAt": "2026-03-25T07:53:59"
    }
  ]
}
```

## 更新会话标题
### 请求路径
```http
PUT /api/v1/agent/sessions/{sessionId}/title
```

### 简要说明
> 修改会话标题（单个会话），需要登录（Sa-Token）
>

### 请求头
| **名称** | **示例值** | **必选** | **说明** |
| --- | --- | --- | --- |
| Cookie | satoken=86e8... | 是 | 登录态凭证 |
| Content-Type | application/json | 是 | 请求体格式(json) |


### 请求体
| **名称** | **类型** | **必选** | **说明** |
| --- | --- | --- | --- |
| title | string | 是 | 新标题，长度不超过20个字符 |


### 路径参数
| **名称** | **类型** | **必选** | **说明** |
| --- | --- | --- | --- |
| sessionId | string/number | 是 | 会话ID |


### 查询参数
无

### 请求示例
```json
PUT /api/v1/agent/sessions/161019495270318081/title HTTP/1.1
Cookie: satoken=86e8bcaa-29da-4d27-a78a-d0df4362741d
Content-Type: application/json

{
  "title": "文档联调会话"
}
```

### 成功响应示例
```json
{
  "code": "200000",
  "message": "请求成功",
  "data": null
}
```

### 失败响应示例（参数校验失败）
```json
{
  "code": "400001",
  "message": "参数验证失败",
  "data": {
    "title": "会话标题长度不能超过20个字符"
  }
}
```

## 删除会话
### 请求路径
```http
DELETE /api/v1/agent/sessions/{sessionId}
```

### 简要说明
> 硬删除单个会话，需要登录（Sa-Token）
>

### 请求头
| **名称** | **示例值** | **必选** | **说明** |
| --- | --- | --- | --- |
| Cookie | satoken=86e8... | 是 | 登录态凭证 |
| Content-Type | application/json | 否 | 本接口无请求体，仅为统一规范可携带 |


### 请求体
无

### 路径参数
| **名称** | **类型** | **必选** | **说明** |
| --- | --- | --- | --- |
| sessionId | string/number | 是 | 会话ID |


### 查询参数
无

### 请求示例
```json
DELETE /api/v1/agent/sessions/161019495270318081 HTTP/1.1
Cookie: satoken=86e8bcaa-29da-4d27-a78a-d0df4362741d
```

### 成功响应示例
```json
{
  "code": "200000",
  "message": "请求成功",
  "data": null
}
```

## 获取会话消息历史
### 请求路径
```http
GET /api/v1/agent/sessions/{sessionId}/message
```

### 简要说明
> 获取指定会话的消息历史，需要登录（Sa-Token）
>

### 请求头
| **名称** | **示例值** | **必选** | **说明** |
| --- | --- | --- | --- |
| Cookie | satoken=86e8... | 是 | 登录态凭证 |
| Content-Type | application/json | 否 | 本接口无请求体，仅为统一规范可携带 |


### 请求体
无

### 路径参数
| **名称** | **类型** | **必选** | **说明** |
| --- | --- | --- | --- |
| sessionId | string/number | 是 | 会话ID |


### 查询参数
无

### 请求示例
```json
GET /api/v1/agent/sessions/161019495270318081/message HTTP/1.1
Cookie: satoken=86e8bcaa-29da-4d27-a78a-d0df4362741d
```

### 成功响应示例
```json
{
  "code": "200000",
  "message": "请求成功",
  "data": []
}
```

### 失败响应示例（会话不存在）
```json
{
  "code": "A00002",
  "message": "聊天会话已过期或不存在",
  "data": null
}
```

## 流式聊天
### 请求路径
```http
POST /api/v1/agent/chat
```

### 简要说明
> 与 Agent 进行流式聊天，返回 SSE 分片数据；结束时返回 done 事件
>

### 请求头
| **名称** | **示例值** | **必选** | **说明** |
| --- | --- | --- | --- |
| Cookie | satoken=86e8... | 是 | 登录态凭证 |
| Content-Type | application/json | 是 | 请求体格式(json) |
| Accept | text/event-stream | 否 | 建议携带，声明期望SSE流 |


### 请求体
| **名称** | **类型** | **必选** | **说明** |
| --- | --- | --- | --- |
| sessionId | string/number | 是 | 会话ID |
| message | string | 是 | 用户输入 |
| agentName | string | 是 | 智能体名称 |
| context | object | 是 | 业务上下文 |
| context.collectionIds | array<number> | 是 | 当前题集范围 |
| context.selectedQuestionIds | array<number> | 是 | 当前选中题目 |


### 路径参数
无

### 查询参数
无

### 请求示例
```json
POST /api/v1/agent/chat HTTP/1.1
Cookie: satoken=86e8bcaa-29da-4d27-a78a-d0df4362741d
Content-Type: application/json
Accept: text/event-stream

{
  "sessionId": 161019495270318081,
  "message": "你好",
  "agentName": "题库小助手",
  "context": {
    "collectionIds": [160229847568744450],
    "selectedQuestionIds": []
  }
}
```

### 成功响应示例
```text
event:message
data:你好

event:message
data:！我是

event:message
data:题

event:message
data:库小助手

event:done
data:[DONE]
```

## 生成题目草稿
### 请求路径
```http
POST /api/v1/agent/question-draft/generate
```

### 简要说明
> 根据用户描述生成题目草稿，需要登录（Sa-Token）
>

### 请求头
| **名称** | **示例值** | **必选** | **说明** |
| --- | --- | --- | --- |
| Cookie | satoken=86e8... | 是 | 登录态凭证 |
| Content-Type | application/json | 是 | 请求体格式(json) |


### 请求体
| **名称** | **类型** | **必选** | **说明** |
| --- | --- | --- | --- |
| topic | string | 否 | 知识点 / 出题方向 |
| typeCode | string | 否 | 题型：single-choice / multiple-choice / true-false / fill-in / short-answer |
| scene | string | 否 | 场景描述 |
| extraRequirements | string | 否 | 额外要求 |


### 路径参数
无

### 查询参数
无

### 请求示例
```json
POST /api/v1/agent/question-draft/generate HTTP/1.1
Cookie: satoken=86e8bcaa-29da-4d27-a78a-d0df4362741d
Content-Type: application/json

{
  "topic": "Redis缓存",
  "typeCode": "single-choice",
  "scene": "课堂练习",
  "extraRequirements": "难度中等"
}
```

### 成功响应示例
```json
{
  "code": "200000",
  "message": "请求成功",
  "data": {
    "typeCode": "single-choice",
    "title": "Redis 缓存基础",
    "stem": "关于 Redis 缓存，以下描述正确的是？",
    "options": [
      {
        "key": "A",
        "content": "Redis 是一种关系型数据库，支持 SQL 查询。"
      },
      {
        "key": "B",
        "content": "Redis 是一种基于内存的键值存储系统，常用于缓存场景。"
      },
      {
        "key": "C",
        "content": "Redis 只支持字符串类型的数据结构。"
      },
      {
        "key": "D",
        "content": "Redis 无法实现数据持久化。"
      }
    ],
    "answer": "",
    "correctOptions": [
      "B"
    ],
    "judgeAnswer": null,
    "solution": "Redis 是一个开源的、基于内存的键值存储系统，支持多种数据结构（如字符串、哈希、列表、集合等），并提供 RDB 和 AOF 两种持久化机制。它广泛应用于缓存、消息队列等场景。",
    "difficulty": 0.50
  }
}
```

## 生成组卷候选清单
### 请求路径
```http
POST /api/v1/agent/paper-draft/generate
```

### 简要说明
> 根据自然语言描述和题型约束，在指定题集范围内返回候选题列表，需要登录（Sa-Token）
>

### 请求头
| **名称** | **示例值** | **必选** | **说明** |
| --- | --- | --- | --- |
| Cookie | satoken=86e8... | 是 | 登录态凭证 |
| Content-Type | application/json | 是 | 请求体格式(json) |


### 请求体
| **名称** | **类型** | **必选** | **说明** |
| --- | --- | --- | --- |
| message | string | 否 | 组卷描述 |
| collectionIds | array<number> | 否 | 题集范围 |
| constrains | array<object> | 否 | 题型约束列表 |
| constrains[].typeCode | string | 是 | 题型编码 |
| constrains[].count | number | 是 | 该题型题目数量 |
| constrains[].difficultyMin | number | 否 | 难度下限 |
| constrains[].difficultyMax | number | 否 | 难度上限 |


### 路径参数
无

### 查询参数
无

### 请求示例
```json
POST /api/v1/agent/paper-draft/generate HTTP/1.1
Cookie: satoken=86e8bcaa-29da-4d27-a78a-d0df4362741d
Content-Type: application/json

{
  "message": "高等数学复习卷，重点极限与导数",
  "collectionIds": [160229847568744450],
  "constrains": [
    {
      "typeCode": "single-choice",
      "count": 2,
      "difficultyMin": 0.3,
      "difficultyMax": 0.7
    },
    {
      "typeCode": "fill-in",
      "count": 1,
      "difficultyMin": 0.3,
      "difficultyMax": 0.7
    }
  ]
}
```

### 成功响应示例
```json
{
  "code": "200000",
  "message": "请求成功",
  "data": {
    "reason": "单选聚焦极限与导数的概念辨析和性质判断，填空侧重基础计算与公式应用，覆盖复习重点的核心技能点。",
    "candidateQuestions": [
      {
        "questionId": "160229913784221696",
        "questionVersionId": "160229913784221697",
        "title": "极限与连续-单选题-01",
        "typeCode": "single-choice",
        "difficulty": 0.42
      },
      {
        "questionId": "160229914069434368",
        "questionVersionId": "160229914069434369",
        "title": "导数应用：单调性、极值、最值-单选题-04",
        "typeCode": "single-choice",
        "difficulty": 0.5
      },
      {
        "questionId": "160229914010714114",
        "questionVersionId": "160229914010714115",
        "title": "导数的定义与计算-填空题-01",
        "typeCode": "fill-in",
        "difficulty": 0.4
      }
    ]
  }
}
```

### 失败响应示例（题目数量不足）
```json
{
  "code": "A00003",
  "message": "题目数量不足，请调整组卷条件或补充题库题目",
  "data": null
}
```
