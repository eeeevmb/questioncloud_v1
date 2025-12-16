# 题库领域 API

## 概览
- **Base URL**：`/api/v1`，题集接口前缀 `/collection`，题目前缀 `/question`。
- **统一响应包装**：除特别说明外均返回 `ResultVO<T>`，字段：`code`（String）、`message`（String）、`data`（任意 VO）。
- **成功示例**：`{"code":"200000","message":"请求成功","data":{...}}`
- **失败 message 结构**：若抛出 `ApplicationException(ResultCodeEnum, "detail")`，`message` 会拼接为 `<默认文案>: <detail>`，如 `题集不存在: 题集不存在`；无附加信息则只返回默认文案。
- **鉴权**：所有题库接口需登录（Service 层通过 `StpUtil.getLoginIdAsLong()` 校验），请在 Header 或 Cookie 携带 `satoken=<tokenValue>`（登录接口返回的 `tokenName/tokenValue`）。

### 常见错误码
| code | message | 场景 |
| --- | --- | --- |
| 200000 | 请求成功 | 正常返回 |
| 400000 | 参数错误 | BasePageQuery 关键词超长等 |
| 400001 | 参数验证失败 | 请求体验证失败、文件校验失败等 |
| 401000 | 用户未登录 | 缺少有效 Sa-Token |
| 403000 | 无此操作权限 | 访问他人题集/题目 |
| 404000 | 请求的资源不存在 | 公共 NOT_FOUND |
| Q10001 | 题集不存在 | Collection 校验失败或无权限 |
| Q00001 | 题目不存在 | Question 校验失败或无权限 |
| Q00002 | 保存题目失败 | 题型判分字段缺失 |
| Q00005 | 题型不存在 | `typeCode` 不受支持 |
| 500000/500001 | 系统错误 | 未知或基础设施异常 |

### ResultVO 响应示例
```json
{
  "code": "200000",
  "message": "请求成功",
  "data": {
    "id": 9001,
    "title": "示例"
  }
}
```

## 题集接口（/api/v1/collection）

### POST /api/v1/collection — 创建题集
- **Content-Type**：`application/json`
- **鉴权**：需要登录
- **请求体（CreateCollectionReq）**
| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| name | string | 是 | 题集名称，`@NotBlank` |
| description | string | 否 | 描述 |

**请求示例**
```json
{
  "name": "高等数学冲刺",
  "description": "期末强化套题"
}
```

**curl**
```bash
curl -X POST https://host/api/v1/collection \
  -H "Content-Type: application/json" \
  -H "Cookie: satoken=xxxx" \
  -d '{"name":"高等数学冲刺","description":"期末强化套题"}'
```

**成功响应**
```json
{"code":"200000","message":"请求成功","data":{"name":"高等数学冲刺","description":"期末强化套题"}}
```

**失败示例**
```json
{"code":"400001","message":"参数验证失败: 题集名不能为空","data":null}
```

### PUT /api/v1/collection/{collectionId} — 更新题集
- **Content-Type**：`application/json`
- **鉴权**：需要登录（仅所有者可更新）
- **路径参数**：`collectionId` (long)
- **请求体（UpdateCollectionReq）**
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| name | string | 新名称 |
| description | string | 新描述 |

**curl**
```bash
curl -X PUT https://host/api/v1/collection/1288999001 \
  -H "Content-Type: application/json" \
  -H "Cookie: satoken=xxxx" \
  -d '{"name":"2025 冲刺","description":"第3版"}'
```

**成功响应**
```json
{"code":"200000","message":"请求成功","data":{"name":"2025 冲刺","description":"第3版"}}
```

**失败示例**（无权限或不存在）
```json
{"code":"Q10001","message":"题集不存在","data":null}
```

### DELETE /api/v1/collection/{collectionId} — 删除题集
- **Content-Type**：`application/json`
- **鉴权**：需要登录
- **路径参数**：`collectionId`

**curl**
```bash
curl -X DELETE https://host/api/v1/collection/1288999001 \
  -H "Cookie: satoken=xxxx"
```

**成功响应**
```json
{"code":"200000","message":"请求成功","data":null}
```

**失败示例**
```json
{"code":"Q10001","message":"题集不存在","data":null}
```

### GET /api/v1/collection/{collectionId}/questions — 分页查询题目概要
- **鉴权**：需要登录
- **路径参数**：`collectionId`
- **Query（QuestionInCollectionPageQuery + BasePageQuery）**
| 参数 | 类型 | 说明 |
| --- | --- | --- |
| pageNum | int | 页码，默认 1 |
| pageSize | int | 每页条数，默认 20 |
| keyword | string | 标题模糊查询（≤50 字） |
| sortField | string | `createdAt` / `typeCode` / `correctRate` |
| sortDirection | string | `ASC` / `DESC`，默认 `DESC` |
| typeCode | string | 题型过滤（`single-choice` 等） |
| levelMin | double | 难度下限（> levelMin） |
| levelMax | double | 难度上限（< levelMax） |

**curl**
```bash
curl "https://host/api/v1/collection/1288999001/questions?pageNum=1&pageSize=10&typeCode=single-choice" \
  -H "Cookie: satoken=xxxx"
```

**成功响应**
```json
{
  "code":"200000",
  "message":"请求成功",
  "data":{
    "records":[
      {
        "id":9001,
        "currentVersionId":9101,
        "versionNo":2,
        "typeCode":"single-choice",
        "title":"极限计算",
        "difficulty":0.6,
        "correctRate":0.35,
        "createdAt":"2025-02-01T10:20:00",
        "updatedAt":"2025-03-28T08:00:00"
      }
    ],
    "total":36,
    "pageNum":1,
    "pageSize":10,
    "pages":4,
    "hasPrevious":false,
    "hasNext":true
  }
}
```

**失败示例**
```json
{"code":"403000","message":"无此操作权限","data":null}
```

## 题目接口（/api/v1/question）

### POST /api/v1/question — 创建题目
- **Content-Type**：`application/json`
- **鉴权**：需要登录
- **请求体（CreateQuestionReq）**
| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| typeCode | string | 是 | `single-choice` / `multiple-choice` / `true-false` / `fill-in` / `short-answer` |
| title | string | 是 | 题目标题 |
| stem | string | 是 | 题干 |
| options | QuestionOption[] | 选择题必填 | `{key, content}` |
| answer | string | 否 | 展示用答案 |
| correctOptions | string[] | 选择题必填 | 单选=1 项，多选≥1 项 |
| judgeAnswer | string | 判断题必填 | `T`/`F` |
| solution | string | 否 | 解析 |
| difficulty | decimal | 否 | 0.00-1.00 |
| collectionId | long | 是 | 归属题集 |
| assets | QuestionAsset[] | 否 | `{fileId, section (PRO/SOLU), ordinal}` |

**示例请求 JSON — 单选题**
```json
{
  "typeCode": "single-choice",
  "title": "数列极限",
  "stem": "已知 a_n = 1/n，求 lim a_n",
  "options": [
    {"key": "A", "content": "0"},
    {"key": "B", "content": "1"},
    {"key": "C", "content": "不存在"}
  ],
  "answer": "当 n→∞，a_n→0",
  "correctOptions": ["A"],
  "solution": "利用极限定义",
  "difficulty": 0.3,
  "collectionId": 1288999001,
  "assets": []
}
```

**示例请求 JSON — 判断题**
```json
{
  "typeCode": "true-false",
  "title": "线性代数基础",
  "stem": "若 A 可逆，则 det(A) ≠ 0。",
  "judgeAnswer": "T",
  "answer": "命题正确",
  "collectionId": 1288999001,
  "solution": "可逆矩阵行列式非零",
  "assets": []
}
```

**示例请求 JSON — 简答题（带附图）**
```json
{
  "typeCode": "short-answer",
  "title": "二重积分计算（带附图）",
  "stem": "计算二重积分 $\\displaystyle \\iint_D x^2 e^{-y^2}\\,dx\\,dy$，其中 $D$ 为三角形闭区域，顶点为 $(0,0)$、$(1,1)$、$(0,1)$。",
  "answer": null,
  "correctOptions": null,
  "judgeAnswer": null,
  "solution": "\\begin{align*}\nI&=\\int_{0}^{1}e^{-y^{2}}\\,dy\\int_{0}^{y}x^{2}\\,dx\n=\\frac{1}{3}\\int_{0}^{1}y^{3}e^{-y^{2}}\\,dy\n=\\frac{1}{6}-\\frac{1}{3e}\n\\end{align*}",
  "difficulty": 0.45,
  "collectionId": 122000000000000001,
  "assets": [
    {
      "fileId": 1,
      "section": "PRO",
      "ordinal": 1
    },
    {
      "fileId": 2,
      "section": "SOLU",
      "ordinal": 1
    }
  ]
}
```

**curl**
```bash
curl -X POST https://host/api/v1/question \
  -H "Content-Type: application/json" \
  -H "Cookie: satoken=xxxx" \
  -d '@create-question.json'
```

**成功响应**
```json
{"code":"200000","message":"请求成功","data":{"questionId":99001,"questionVersionId":88001,"collectionId":1288999001}}
```

**失败示例**
```json
{"code":"Q00005","message":"题型不存在","data":null}
```

**备注**：后端依据 `typeCode` 自动生成 `answerKey`，缺少必要字段会返回 `Q00002`。

### PUT /api/v1/question/{questionId} — 更新题目
- **Content-Type**：`application/json`
- **鉴权**：需要登录
- **路径参数**：`questionId`
- **请求体（UpdateQuestionReq）**
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| title | string | 新标题 |
| stem | string | **必填**，题干 |
| options | QuestionOption[] | 选择题选项 |
| answer | string | 展示答案 |
| correctOptions | string[] | 判分用选项 |
| judgeAnswer | string | 判断题答案（T/F） |
| solution | string | 解析 |
| assets | QuestionAsset[] | 附件 |

**示例请求 JSON — 简答题更新版**
```json
{
  "title": "三重积分计算（更新版）",
  "stem": "计算三重积分 $\\displaystyle \\iiint\\limits_{\\substack{\\Omega}} y\\sqrt{1 - x^{2}} \\, \\mathrm{d}v$，其中积分区域 $\\Omega$ 是由 $y = -\\sqrt{1 - x^{2} - z^{2}}$，$x^{2} + z^{2} = 1$ 及 $y = 1$ 围成的闭区域。",
  "solution": "\\begin{align*}\n\\iiint_{\\Omega} y\\sqrt{1-x^2}\\,dV \\\\\n&= \\int_{-1}^{1} \\int_{-\\sqrt{1-x^2}}^{\\sqrt{1-x^2}} \\int_{-\\sqrt{1-x^2-z^2}}^{1} y\\sqrt{1-x^2}\\,dy\\,dz\\,dx \\\\\n&= \\int_{-1}^{1} \\sqrt{1-x^2} \\left[ \\int_{-\\sqrt{1-x^2}}^{\\sqrt{1-x^2}} \\dfrac{x^2+z^2}{2} \\, dz \\right] dx \\\\\n&= \\dfrac{1}{2} \\int_{-1}^{1} \\sqrt{1-x^2} \\left[ \\int_{-\\sqrt{1-x^2}}^{\\sqrt{1-x^2}} (x^2+z^2) \\, dz \\right] dx \\\\\n&= \\int_{-1}^{1} \\left( x^2(1-x^2) + \\dfrac{1}{3}(1-x^2)^2 \\right) dx \\\\\n&= \\dfrac{28}{45}\n\\end{align*}",
  "assets": [
    {
      "fileId": 1,
      "section": "PRO",
      "ordinal": 1
    }
  ]
}
```

**示例请求 JSON — 判断题改为 F**
```json
{
  "stem": "设 A 为 3 阶方阵，|A|=2，则 |2A^{-1}|=4。",
  "judgeAnswer": "F",
  "answer": "命题错误，应为 |2A^{-1}| = |2I|^3 / |A| = 4",
  "solution": "利用 det(kA)=k^n det(A) 和 det(A^{-1})=1/det(A)",
  "assets": []
}
```

**curl**
```bash
curl -X PUT https://host/api/v1/question/99001 \
  -H "Content-Type: application/json" \
  -H "Cookie: satoken=xxxx" \
  -d '@update-question.json'
```

**成功响应**
```json
{"code":"200000","message":"请求成功","data":null}
```

**失败示例**
```json
{"code":"Q00002","message":"保存题目失败: 判断题答案不能为空","data":null}
```

**备注**：更新将创建新的 `question_version`（versionNo+1），同步更新 `question.current_version_id`、`question.updated_at` 与 `question_stats.version_id/updated_at`。

### DELETE /api/v1/question/{questionId} — 删除题目
- **鉴权**：需要登录
- **路径参数**：`questionId`

**curl**
```bash
curl -X DELETE https://host/api/v1/question/99001 \
  -H "Cookie: satoken=xxxx"
```

**成功响应**
```json
{"code":"200000","message":"请求成功","data":null}
```

**失败示例**
```json
{"code":"Q00001","message":"题目不存在","data":null}
```

**备注**：后端将硬删除 `question`、全部 `question_version`、`question_stats`，并从 `collection_item` 中移除该题（不重排 `ordinal`）。

### GET /api/v1/question/{questionId} — 查询题目详情
- **鉴权**：需要登录
- **路径参数**：`questionId`
- **响应**：`ResultVO<QuestionDetailVO>`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | long | 题目 ID |
| currentVersionId | long | 当前版本 ID |
| versionNo | int | 版本号 |
| typeCode | string | 题型编码 |
| title | string | 标题 |
| stem | string | 题干 |
| answer | string | 展示答案 |
| answerKey | string | 判分用答案（后端按题型生成） |
| solution | string | 解析 |
| assets | AssetVO[] | `{fileId, section, ordinal}` |
| attempts | int | 作答次数 |
| correctCount | int | 正确次数 |
| correctRate | double | 正确率 0-1 |
| difficulty | double | 难度系数 |
| exposureFactor | double | 题目曝光系数（见备注） |
| ownerId | long | 所有者 |
| createdAt / updatedAt | string | ISO-8601 时间戳 |

**curl**
```bash
curl https://host/api/v1/question/99001 \
  -H "Cookie: satoken=xxxx"
```

**成功响应**
```json
{
  "code":"200000",
  "message":"请求成功",
  "data":{
    "id":99001,
    "currentVersionId":88002,
    "versionNo":2,
    "typeCode":"single-choice",
    "title":"数列极限",
    "stem":"已知 a_n...",
    "answer":"a_n→0",
    "answerKey":"A",
    "solution":"利用极限定义",
    "assets":[],
    "attempts":120,
    "correctCount":42,
    "correctRate":0.35,
    "difficulty":0.3,
    "exposureFactor":0.62,
    "ownerId":5001,
    "createdAt":"2025-01-01T10:00:00",
    "updatedAt":"2025-03-28T08:00:00"
  }
}
```

**失败示例**
```json
{"code":"Q00001","message":"题目不存在","data":null}
```

## 业务备注
1. **题目版本化**：`PUT /question/{id}` 会创建新版本记录并更新 `question.current_version_id`、`question.updated_at` 与 `question_stats.version_id/updated_at`，旧版本仍保留。
2. **曝光系数**：`question_stats.exposure_factor` 保存 `last_exposed_at` 时刻的基准值；普通查询仅计算 `eff = exp * (1/2)^(Δt/7)` 不回写；确认组卷/下发时调用 `ExposureFactorUtil.renewExposureOnEvent` 回升并写回 `exposure_factor` 与 `last_exposed_at`（半衰期 7 天，α=0.3）。
3. **删除策略**：删除题目会同时移除 `question`、所有版本、统计记录，并清理 `collection_item` 中的引用但不重排顺序。
4. **answerKey 生成**：后端根据题型和请求字段生成判分答案——单选仅允许 1 个选项，多选去重排序，判断题仅接受 `T/F`，填空直接使用 `answer`；Update 请求不需要 `typeCode`，系统沿用现有题型。
