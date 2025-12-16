# 用户接口 User API（v1）

## 概览
- **Base URL**：`/api/v1/user`
- **版本**：v1，对应 `cn.sztu.questioncloud.web.rest.v1.user.UserController`
- **统一响应包装**：除头像下载外均返回 `ResultVO<T>`（字段：`code` `message` `data`，其中 `code` 为字符串，详见 `ResultVO`）。
- **错误处理**：`GlobalExceptionHandler` 捕获业务/权限/校验异常并返回 `ResultVO` 错误；错误码取自 `CommonResultCodeEnum` 或业务自定义。
- **鉴权**：项目使用 Sa-Token。根据 `StpUtil` 的调用方式推断 `/logout`、`/upload-avatar`、`/basicInfo`、`/avatar/{userId}` 需要已登录，其余接口公开（最终以线上拦截器配置为准）。

### 常见错误码
| code | message | 场景 |
| --- | --- | --- |
| 200000 | 请求成功 | 正常返回 |
| 400001 | 参数验证失败 | 请求体验证失败、文件校验失败、注册信息重复等 |
| 404000 | 请求的资源不存在 | 用户不存在、用户名或密码错误 |
| 401000 | 用户未登录 | 未登录访问需要鉴权接口 |
| 403000 | 无此操作权限 | 权限不足（如 Sa-Token 权限异常） |
| 500000 | 系统未知错误，请稍后重试 | 未知异常 |
| 500001 | 系统错误，请联系管理员 | 基础设施错误（文件存储等） |

### ResultVO 响应示例
```json
{
  "code": "200000",
  "message": "请求成功",
  "data": {
    "username": "alice",
    "tokenName": "satoken",
    "tokenValue": "xxxx"
  }
}
```

## 接口详情

### POST /register — 用户注册
- **Content-Type**：`application/json`
- **鉴权**：无需登录
- **请求体（RegisterReq）**
| 字段 | 类型 | 必填 | 约束 |
| --- | --- | --- | --- |
| username | string | 是 | 3-16 位，允许中文/字母/数字/_/- (`^[\p{IsHan}a-zA-Z0-9_-]{3,16}$`)
| email | string | 是 | 有效邮箱格式
| password | string | 是 | 8-20 位，至少包含两类（大写/小写/数字/特殊字符 `@$!%*?&#.`）

**请求示例**
```json
{
  "username": "alice_01",
  "email": "alice@example.com",
  "password": "Test@123"
}
```

**响应**：`ResultVO<Long>`（data 为注册成功的用户 ID）。

**成功示例**
```json
{"code":"200000","message":"请求成功","data":123456789012345678}
```

**失败示例**
```json
{"code":"400001","message":"该邮箱已被使用","data":null}
```

**备注**：命中校验/重复时抛 `ApplicationException`，对应 `400001`；其他异常返回 `500000` 或 `500001`。

### POST /login — 用户登录
- **Content-Type**：`application/json`
- **鉴权**：无需登录
- **请求体（LoginReq）**
| 字段 | 类型 | 必填 | 约束 |
| --- | --- | --- | --- |
| account | string | 是 | 支持用户名（3-16 位同上）或邮箱
| password | string | 是 | 同注册密码规则

**请求示例**
```json
{
  "account": "alice_01",
  "password": "Test@123"
}
```

**响应**：`ResultVO<LoginVO>`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| userId | long | 用户 ID |
| username | string | 用户名 |
| tokenName | string | Sa-Token 名称（如 `satoken`） |
| tokenValue | string | 登录凭证，需要放入后续请求头/Cookie |

**成功示例**
```json
{
  "code":"200000",
  "message":"请求成功",
  "data":{
    "userId":123,
    "username":"alice_01",
    "tokenName":"satoken",
    "tokenValue":"b5b1..."
  }
}
```

**失败示例**
```json
{"code":"404000","message":"用户名或密码错误","data":null}
```

**备注**：密码错误与账号不存在均返回 `404000`，避免泄露用户信息。

### POST /logout — 退出登录
- **Content-Type**：`application/json`
- **鉴权**：需要登录
- **请求体**：无
- **响应**：`ResultVO<Void>`，data 恒为 `null`。

**请求示例**
```http
POST /api/v1/user/logout HTTP/1.1
Authorization: satoken=b5b1...
```

**成功示例**
```json
{"code":"200000","message":"请求成功","data":null}
```

**失败示例**：未登录访问返回 `{"code":"401000","message":"用户未登录","data":null}`。

**备注**：实际登录态通过 Sa-Token 维护，客户端需携带 `tokenName=tokenValue`。

### POST /upload-avatar — 上传头像
- **Content-Type**：`multipart/form-data`
- **鉴权**：需要登录
- **表单字段**
| 名称 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| file | file | 是 | 图片文件，后端校验：不能为空、仅 jpg/png/jpeg、大小≤5MB、内容与扩展名需匹配。

**curl 示例**
```bash
curl -X POST https://host/api/v1/user/upload-avatar \
  -H "Cookie: satoken=b5b1..." \
  -F "file=@/path/avatar.png"
```

**响应**：`ResultVO<AvatarVO>`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| url | string | 存储后的相对路径，可配合文件服务访问 |

**成功示例**
```json
{"code":"200000","message":"请求成功","data":{"url":"/private/user/123/avatar.png"}}
```

**失败示例**
```json
{"code":"400001","message":"图片文件过大，最大支持 5MB","data":null}
```

**备注**：文件上传失败时 `InfrastructureException` 会被映射为参数错误或 `500001`（文件处理失败）。

### GET /avatar/{userId} — 获取头像
- **Accept**：由客户端指定，接口根据文件类型返回
- **鉴权**：需要登录
- **路径参数**
| 名称 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| userId | long | 是 | 目标用户 ID |

**请求示例**
```http
GET /api/v1/user/avatar/123 HTTP/1.1
Cookie: satoken=b5b1...
```

**响应**：`ResponseEntity<Resource>`，内容为头像文件流；若用户未上传则返回默认图片。

**HTTP 头部示例**
```
HTTP/1.1 200 OK
Content-Type: image/png
Cache-Control: max-age=2592000, public
```

**失败示例**（用户不存在）
```json
{"code":"404000","message":"用户不存在","data":null}
```

**备注**：头像 URL 的 Content-Type 由 `MediaTypeResolver` 推断；若文件缺失返回默认静态资源 `static/teacher.png`。

### GET /basicInfo — 获取用户基础信息
- **Content-Type**：`application/json`
- **鉴权**：需要登录
- **请求参数**：无
- **响应**：`ResultVO<UserBasicInfoVO>`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| userId | long | 用户 ID |
| username | string | 用户名 |
| email | string | 邮箱 |
| phone | string | 手机号（可能为空） |
| status | int | 用户状态（参考 `UserStatusEnum`，0=禁用/1=正常 等） |

**成功示例**
```json
{
  "code":"200000",
  "message":"请求成功",
  "data":{
    "userId":123,
    "username":"alice_01",
    "email":"alice@example.com",
    "phone":null,
    "status":1
  }
}
```

**失败示例**
```json
{"code":"404000","message":"用户不存在","data":null}
```

**备注**：内部通过 `StpUtil.getLoginIdAsLong()` 读取当前用户；未登录触发 `401000`。

## 特殊说明
- 登录成功后需将 `tokenName` 和 `tokenValue` 作为会话凭证；默认可通过 Cookie（`satoken=...`）或 Header 携带。
- 文件接口缓存策略：头像下载默认 `Cache-Control: public, max-age=30 days`，建议客户端配合 ETag 或定期刷新以同步更新。
- 若某接口的鉴权策略与推断不符，以 Sa-Token 拦截器和网关真实配置为准，并应在后续文档中补充说明。
