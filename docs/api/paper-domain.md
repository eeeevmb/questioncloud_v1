# 试卷（paper）模块 API

## 概览
- Base URL：`/api/v1`，试卷接口前缀 `/paper`。
- 统一响应包装：所有接口均返回 `ResultVO<T>`（`code` `message` `data`），`code` 为字符串。
- HTTP 状态：所有接口 HTTP 恒为 200，成功或失败均以 `ResultVO.code` 判断。
- 鉴权与会话：全部需要登录，Sa-Token 使用 Cookie 会话，Cookie 名 `satoken`。浏览器同域自动携带；跨域联调需 `withCredentials=true`，服务端允许 credentials 且 `Access-Control-Allow-Origin` 不能为 `*`。未携带有效 Cookie 时返回 `code=401000`。
- tokenName/tokenValue：登录接口返回该字段仅供调试，非浏览器客户端可显式设置 `Cookie: satoken=<tokenValue>`；Web 前端以 Cookie 自动携带。
- 失败 message 结构：若抛出 `ApplicationException(ResultCodeEnum, "detail")`，`message` 会拼接为 `<默认文案>: <detail>`，如 `试卷不存在: 不在当前用户名下`；无附加信息则仅返回默认文案。
- 成功示例：`{"code":"200000","message":"请求成功","data":{...}}`

### 常见错误码
| code | message | 场景 |
| --- | --- | --- |
| 200000 | 请求成功 | 正常返回 |
| 400000 | 参数错误 | 关键词超长等 |
| 400001 | 参数验证失败 | 请求体验证失败 |
| 401000 | 用户未登录 | 缺少有效 Sa-Token |
| 403000 | 无此操作权限 | 访问他人试卷 |
| 404000 | 请求的资源不存在 | 公共 NOT_FOUND |
| P00001 | 试卷不存在 | 试卷校验失败或无权限 |
| P00002 | 保存试卷失败 | 基础信息或题目保存失败 |
| P00003 | 试卷标题重复 | 重名冲突 |
| P00004 | 试卷状态错误 | 非法状态（如已发布不可编辑） |
| 500000/500001 | 系统错误 | 未知或基础设施异常 |

### ResultVO 响应示例
```json
{
  "code": "200000",
  "message": "请求成功",
  "data": {
    "id": 60001,
    "title": "期末测试 A 卷"
  }
}
```

---

## 试卷接口（/api/v1/paper）

> PaperController 概览：
> - 自由组卷：创建空试卷、全量保存题目、获取详情、更新基础信息、硬删除试卷、清空题目
> - 随机组卷：随机预览（不落库）

### POST /api/v1/paper — 创建空试卷
- Content-Type：`application/json`
- 鉴权：需要登录
- 请求体（PaperSaveReq）

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| title | string | 是 | 试卷标题，`@NotBlank` |
| description | string | 否 | 试卷描述 |

请求示例
```json
{
  "title": "高数期末测试（A 卷）",
  "description": "2026 春季学期"
}
```

curl
```bash
curl -X POST https://host/api/v1/paper \
  -H "Content-Type: application/json" \
  -H "Cookie: satoken=xxxx" \
  -d '{"title":"高数期末测试（A 卷）","description":"2026 春季学期"}'
```

成功响应（PaperCreatedVO）
```json
{"code":"200000","message":"请求成功","data":{"paperId":60001}}
```

失败示例
```json
{"code":"P00003","message":"试卷标题重复","data":null}
```

---

### PUT /api/v1/paper/{paperId} — 修改试卷基础信息
- Content-Type：`application/json`
- 鉴权：需要登录（仅所有者可更新）
- 路径参数：`paperId` (long)
- 请求体（PaperSaveReq）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| title | string | 新标题 |
| description | string | 新描述 |

curl
```bash
curl -X PUT https://host/api/v1/paper/60001 \
  -H "Content-Type: application/json" \
  -H "Cookie: satoken=xxxx" \
  -d '{"title":"高数期末测试（A1 修订版）","description":"2026 春季学期 - 修订"}'
```

成功响应（PaperBasicVO）
```json
{"code":"200000","message":"请求成功","data":{"id":60001,"title":"高数期末测试（A1 修订版）","description":"2026 春季学期 - 修订"}}
```

失败示例（无权限或不存在）
```json
{"code":"P00001","message":"试卷不存在","data":null}
```

---

### GET /api/v1/paper/{paperId} — 获取试卷详情（含题目列表）
- 鉴权：需要登录（仅所有者可见）
- 路径参数：`paperId`
- 响应：`ResultVO<PaperDetailVO>`

字段说明
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | long | 试卷 ID |
| title | string | 标题 |
| description | string | 描述 |
| status | string | 状态（如 `DRAFT`/`PUBLISHED`） |
| totalScore | number | 总分 |
| itemCount | int | 题目数量 |
| items | PaperItemVO[] | 题目明细（见下） |
| createdAt / updatedAt | string | ISO-8601 时间戳 |
| ownerId | long | 所有者 |

PaperItemVO
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| questionId | long | 题目 ID |
| typeCode | string | 题型编码 |
| title | string | 题目标题（概要） |
| score | number | 分值 |
| ordinal | int | 在试卷中的顺序（从 1 开始） |

curl
```bash
curl https://host/api/v1/paper/60001 \
  -H "Cookie: satoken=xxxx"
```

成功响应
```json
{
  "code":"200000",
  "message":"请求成功",
  "data":{
    "id":60001,
    "title":"高数期末测试（A1 修订版）",
    "description":"2026 春季学期 - 修订",
    "status":"DRAFT",
    "totalScore":100,
    "itemCount":3,
    "items":[
      {"questionId":99001,"typeCode":"single-choice","title":"极限计算 1","score":5,"ordinal":1},
      {"questionId":99002,"typeCode":"true-false","title":"线性代数基础 1","score":5,"ordinal":2},
      {"questionId":99003,"typeCode":"short-answer","title":"二重积分 1","score":10,"ordinal":3}
    ],
    "ownerId":5001,
    "createdAt":"2026-01-01T10:00:00",
    "updatedAt":"2026-01-22T08:00:00"
  }
}
```

失败示例
```json
{"code":"P00001","message":"试卷不存在","data":null}
```

---

### POST /api/v1/paper/{paperId}/items — 全量保存试卷题目
- Content-Type：`application/json`
- 鉴权：需要登录（仅所有者可编辑）
- 路径参数：`paperId`
- 请求体：`List<PaperItemSaveReq>`（提交顺序即为试题顺序，服务端执行 delete+save 覆盖）

PaperItemSaveReq
| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| questionId | long | 是 | 题目 ID |
| score | number | 否 | 分值，缺省时由系统或题型默认 |
| ordinal | int | 否 | 指定顺序；不传则按数组索引顺序 |
| typeCode | string | 否 | 题型编码（校验用） |

请求示例
```json
[
  {"questionId":99001,"score":5,"ordinal":1},
  {"questionId":99002,"score":5,"ordinal":2},
  {"questionId":99003,"score":10,"ordinal":3}
]
```

curl
```bash
curl -X POST https://host/api/v1/paper/60001/items \
  -H "Content-Type: application/json" \
  -H "Cookie: satoken=xxxx" \
  -d '[{"questionId":99001,"score":5,"ordinal":1},{"questionId":99002,"score":5,"ordinal":2},{"questionId":99003,"score":10,"ordinal":3}]'
```

成功响应（List<PaperItemSaveVO>）
```json
{
  "code":"200000",
  "message":"请求成功",
  "data":[
    {"questionId":99001,"score":5,"ordinal":1},
    {"questionId":99002,"score":5,"ordinal":2},
    {"questionId":99003,"score":10,"ordinal":3}
  ]
}
```

失败示例
```json
{"code":"P00002","message":"保存试卷失败: 题目不存在或无权限","data":null}
```

---

### DELETE /api/v1/paper/{paperId} — 硬删除试卷
- 鉴权：需要登录（仅所有者可删除）
- 路径参数：`paperId`

curl
```bash
curl -X DELETE https://host/api/v1/paper/60001 \
  -H "Cookie: satoken=xxxx"
```

成功响应
```json
{"code":"200000","message":"请求成功","data":null}
```

失败示例
```json
{"code":"P00001","message":"试卷不存在","data":null}
```

---

### DELETE /api/v1/paper/{paperId}/items — 清空试卷题目
- 鉴权：需要登录（仅所有者可编辑）
- 路径参数：`paperId`

curl
```bash
curl -X DELETE https://host/api/v1/paper/60001/items \
  -H "Cookie: satoken=xxxx"
```

成功响应
```json
{"code":"200000","message":"请求成功","data":null}
```

失败示例
```json
{"code":"P00001","message":"试卷不存在","data":null}
```

---

### POST /api/v1/paper/{paperId}/random-preview — 随机组卷预览（不保存）
- Content-Type：`application/json`
- 鉴权：需要登录（仅所有者可预览）
- 路径参数：`paperId`
- 说明：仅返回随机生成的题目列表，不保存到数据库，不覆盖原有试卷。
- 请求体（RandomBuildReq）

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| totalCount | int | 否 | 总题数，`typeCounts` 未指定时使用 |
| typeCounts | object | 否 | 按题型数量，如 `{"single-choice":10,"true-false":5}` |
| collections | long[] | 否 | 题目来源的题集过滤 |
| levelMin | double | 否 | 难度下限（> levelMin） |
| levelMax | double | 否 | 难度上限（< levelMax） |
| excludeQuestionIds | long[] | 否 | 排除题目 ID |
| preferLowExposure | boolean | 否 | 优先选择低曝光题，默认 true |

请求示例
```json
{
  "typeCounts": {
    "single-choice": 6,
    "true-false": 4
  },
  "collections": [1288999001],
  "levelMin": 0.2,
  "levelMax": 0.7,
  "excludeQuestionIds": [99002],
  "preferLowExposure": true
}
```

curl
```bash
curl -X POST https://host/api/v1/paper/60001/random-preview \
  -H "Content-Type: application/json" \
  -H "Cookie: satoken=xxxx" \
  -d '{"typeCounts":{"single-choice":6,"true-false":4},"collections":[1288999001],"levelMin":0.2,"levelMax":0.7,"excludeQuestionIds":[99002],"preferLowExposure":true}'
```

成功响应（List<PaperItemSaveVO>；仅返回预览项）
```json
{
  "code":"200000",
  "message":"请求成功",
  "data":[
    {"questionId":99011,"score":5,"ordinal":1},
    {"questionId":99012,"score":5,"ordinal":2},
    {"questionId":99013,"score":5,"ordinal":3},
    {"questionId":99014,"score":5,"ordinal":4},
    {"questionId":99015,"score":5,"ordinal":5},
    {"questionId":99016,"score":5,"ordinal":6},
    {"questionId":99101,"score":5,"ordinal":7},
    {"questionId":99102,"score":5,"ordinal":8},
    {"questionId":99103,"score":5,"ordinal":9},
    {"questionId":99104,"score":5,"ordinal":10}
  ]
}
```

失败示例
```json
{"code":"P00002","message":"保存试卷失败: 随机参数非法","data":null}
```

---

## 业务备注
1. **自由组卷保存策略**：编辑页面点击“保存”时，会覆盖之前的题目列表（delete+save）；题目顺序以前端维护提交的数组顺序为准。
2. **权限与可见性**：仅试卷所有者可视、可编辑、可删除；跨用户访问返回 `403000`。
3. **删除策略**：硬删除试卷后，试卷本体及其题目关联均被清理；不触及题库中的题目实体。
4. **状态约束**：在某些状态（如 `PUBLISHED`）下可能限制编辑，触发 `P00004`。
5. **随机组卷**：随机预览不落库、不影响现有试卷；在支持的实现中可结合题目曝光系数（优先低曝光）进行采样，避免重复与热点题过度使用。
6. **错误消息拼接**：`ApplicationException(ResultCodeEnum, "detail")` 将以 `<默认文案>: <detail>` 返回；无 `detail` 时仅返回默认文案。