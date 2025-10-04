# 文件存储模块 (File Storage Module)

## 1. 概述

本模块提供了一套完整的文件管理解决方案，旨在为应用程序提供可靠、安全、可扩展的文件上传、下载、删除和访问控制功能。目前主要实现了基于本地文件系统的存储策略。

## 2. 主要功能

- **文件上传**: 支持从 `MultipartFile` 或 `InputStream` 上传文件。
- **文件下载**: 提供安全的流式文件下载。
- **文件删除**: 同时删除物理文件和数据库中的元数据记录。
- **访问控制**:
    - **公开 (Public)**: 文件可通过其ID被任何人访问。
    - **私有 (Private)**: 文件默认只能被所有者访问。
- **权限共享**: 文件所有者可以将私有文件的访问权限授予其他指定用户。
- **配额管理**: 可配置每个用户每周的文件上传数量限制。
- **元数据管理**: 所有文件的信息（如所有者、大小、MIME类型、存储路径等）都存储在数据库中，便于管理和查询。

## 3. 核心服务接口

核心服务接口是 `FileStorageService`，它定义了所有文件操作。

```java
public interface FileStorageService {
    // 上传文件
    FileMetadata upload(MultipartFile file, String ownerIdentifier, AccessLevel accessLevel);

    FileMetadata upload(InputStream inputStream, String ownerIdentifier, ...);

    // 下载文件
    boolean download(Long fileId, String accessorIdentifier, OutputStream outputStream);

    // 获取文件元数据
    FileMetadata getMetadata(Long fileId);

    // 删除文件
    void delete(Long fileId, String ownerIdentifier);

    // 授权
    void grantAccess(Long fileId, String ownerIdentifier, String granteeIdentifier);

    // 取消授权
    void revokeAccess(Long fileId, String ownerIdentifier, String granteeIdentifier);
}
```

## 4. 配置

在您的 `application.yml` 或 `application.properties` 文件中配置以下参数：

```yaml
file:
  storage:
    # 本地存储配置
    local:
      # 文件存储的基础路径。可以是绝对路径 (e.g., /var/files) 或相对路径 (e.g., uploads)。
      # 相对路径将以项目运行目录为基准。
      base-path: "uploads"
    # 配额配置
    quota:
      # 是否启用配额检查
      enabled: true
      # 每个用户每周允许上传的文件数量
      files-per-week: 50
```

## 5. 使用教程

### 5.1 注入服务

在您的 Service 或 Controller 中注入 `FileStorageService`。

```java

@Service
public class YourService {
    private final FileStorageService fileStorageService;

    public YourService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }
    // ...
}
```

### 5.2 上传文件

```java
// 假设 file 是从 Controller 获取的 MultipartFile
// "user-001" 是当前操作用户的唯一标识
// AccessLevel.PRIVATE 表示这是一个私有文件
try{
FileMetadata metadata = fileStorageService.upload(file, "user-001", AccessLevel.PRIVATE);
    log.

info("文件上传成功，文件ID: {}",metadata.getFmId());
        // 保存 metadata.getFmId() 用于后续操作
        }catch(
InfrastructureException e){
        log.

error("文件上传失败: {}",e.getMessage());
        }
```

### 5.3 下载文件

```java
// "user-001" 是请求下载的用户的唯一标识
// response 是 HttpServletResponse 对象
try{
FileMetadata metadata = fileStorageService.getMetadata(fileId);
// 设置响应头，提示浏览器下载
    response.

setContentType(metadata.getFmMimeType());
        response.

setHeader("Content-Disposition","attachment; filename=\""+metadata.getFmOriginalFilename() +"\"");

boolean success = fileStorageService.download(fileId, "user-001", response.getOutputStream());
    if(!success){
        // 处理下载失败的情况
        }
        }catch(
InfrastructureException e){
        // 处理异常，例如：文件不存在、无权访问等
        response.

setStatus(HttpServletResponse.SC_NOT_FOUND);
}
```

### 5.4 删除文件

只有文件所有者才能删除文件。

```java
// "user-001" 是文件所有者的标识
try{
        fileStorageService.delete(fileId, "user-001");
    log.

info("文件 {} 已被删除",fileId);
}catch(
InfrastructureException e){
        // 如果 "user-001" 不是所有者，会抛出 OWNER_MISMATCH 异常
        log.

error("文件删除失败: {}",e.getMessage());
        }
```

### 5.5 共享私有文件

文件所有者可以将私有文件的访问权限授予其他用户。

```java
// "user-001" 是文件所有者
// "user-002" 是被授权的用户
try{
        fileStorageService.grantAccess(fileId, "user-001","user-002");
    log.

info("已将文件 {} 的访问权限授予 user-002",fileId);
}catch(
InfrastructureException e){
        log.

error("授权失败: {}",e.getMessage());
        }
```

之后，`user-002` 就可以下载这个文件了。

### 5.6 取消共享

```java
try{
        fileStorageService.revokeAccess(fileId, "user-001","user-002");
    log.

info("已取消 user-002 对文件 {} 的访问权限",fileId);
}catch(
InfrastructureException e){
        log.

error("取消授权失败: {}",e.getMessage());
        }
```

## 6. 异常处理

本模块中的所有业务异常都将包装为 `InfrastructureException` 抛出。该异常包含一个 `FileResultCodeEnum`
类型的错误码，便于上层应用进行统一的异常处理和国际化。

**常见的错误码:**

- `QUOTA_EXCEEDED`: 超出上传配额
- `FILE_NOT_FOUND`: 文件元数据或物理文件不存在
- `ACCESS_DENIED`: 无权访问该文件
- `OWNER_MISMATCH`: 操作者不是文件所有者（如删除、授权操作）
- `STORAGE_ERROR`: 文件读写时发生 I/O 错误
