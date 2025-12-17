# 用户接口 User API（v1）

## 概览
- **Base URL**：`/api/v1/user`，实现位于 `cn.sztu.questioncloud.web.rest.v1.user.UserController`。
- **统一响应**：除头像下载外，所有接口均返回 `ResultVO<T>`（`code` `message` `data`）。
- **HTTP 状态**：除文件流接口外 HTTP 始终为 200；成功与失败通过 `ResultVO.code` 判断。
- **鉴权与会话**：项目使用 Sa-Token Cookie 会话，Cookie 名 `satoken`（`StpUtil.getTokenName()`）。浏览器同域自动携带；跨域请求需 `withCredentials=true`，同时后端需允许 credentials 且 `Access-Control-Allow-Origin` 不能为 `*`。
- **tokenName/tokenValue**：登录响应返回该字段，仅方便调试或非浏览器客户端；Web 前端不要手动拼 Header，而是依赖浏览器 Cookie。
- **登录要求**：需要登录的接口限定为 `POST /logout`、`POST /upload-avatar`、`GET /basicInfo`，未登录访问时返回 `code=401000`（HTTP 仍为 200）。
- **头像接口**：`GET /api/v1/user/avatar/{userId}` 公开访问且始终返回图片流（缺失时回退默认头像），不依赖登录 Cookie。

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
| tokenName | string | Sa-Token 名称（默认 `satoken`） |
| tokenValue | string | 登录凭证，仅供调试/非浏览器客户端设置 Cookie；浏览器自动处理 |

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

**备注**：密码错误与账号不存在均返回 `404000`；登录成功后 Sa-Token 会生成 Cookie 响应头。

### POST /logout — 退出登录
- **Content-Type**：`application/json`
- **鉴权**：需要登录（未登录返回 `code=401000`）
- **请求体**：无
- **响应**：`ResultVO<Void>`，data 恒为 `null`。

**请求示例**
```http
POST /api/v1/user/logout HTTP/1.1
Cookie: satoken=b5b1...
```

**成功示例**
```json
{"code":"200000","message":"请求成功","data":null}
```

### POST /upload-avatar — 上传头像
- **Content-Type**：`multipart/form-data`
- **鉴权**：需要登录（未登录返回 `code=401000`）
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

### GET /avatar/{userId} — 获取头像
- **Accept**：默认 `image/*`
- **鉴权**：无需登录
- **路径参数**

| 名称 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| userId | long | 是 | 目标用户 ID |

**请求示例**
```http
GET /api/v1/user/avatar/123 HTTP/1.1
Host: localhost:8080
```

**响应体**：`ResponseEntity<Resource>`，始终返回图片（二进制资源流），当用户未上传头像或文件缺失时由服务端返回默认头像。

**HTTP 头部示例**
```
HTTP/1.1 200 OK
Content-Type: image/png
Cache-Control: public, max-age=2592000
```

**HTML 用法示例**
```html
<img src="/api/v1/user/avatar/123" alt="user avatar" />
```


### GET /basicInfo — 获取用户基础信息
- **Content-Type**：`application/json`
- **鉴权**：需要登录（未登录返回 `code=401000`）
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

## 特殊说明
- 登录成功后浏览器会自动写入 `satoken` Cookie；跨域联调务必启用 `withCredentials` 并在服务端开放 `allowCredentials=true` 且按域名配置 `Access-Control-Allow-Origin`。
- 非浏览器客户端若需调试，可直接设置 `Cookie: satoken=<tokenValue>`，无需构造自定义 Header。
- 头像下载接口永远返回图片（二进制流 + 默认头像兜底），客户端可依赖 `Cache-Control: public, max-age=30 days` 做缓存；若需要强制刷新可追加时间戳 Query。
