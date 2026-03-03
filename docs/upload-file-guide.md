# Sketchware Pro - 图片上传到远程服务器指南

## 概述

本文档介绍如何在 Sketchware Pro 用户项目中实现：**选择图片 → 预览 → 上传到远程服务器**。

利用内置的 `FilePicker` 组件选择文件，配合 `RequestNetwork` 组件的 `uploadFile()` 方法以 `multipart/form-data` 格式上传。

## 流程图

```
┌─────────────┐     ┌──────────────┐     ┌─────────────────┐     ┌──────────┐
│ 点击添加图片 │ ──→ │ FilePicker   │ ──→ │ onFilesPicked   │ ──→ │ 上传提交  │
│ 按钮        │     │ 打开文件管理  │     │ 获取图片路径     │     │ uploadFile│
└─────────────┘     └──────────────┘     └─────────────────┘     └──────────┘
```

## 前置准备

### 添加组件

在 Sketchware 项目中添加以下组件：

| 组件 | 变量名（示例） | 用途 |
|------|---------------|------|
| FilePicker | `filepicker1` | 打开系统文件选择器 |
| RequestNetwork | `requestnetwork1` | 发送 HTTP 请求 |

### 添加变量

| 变量名 | 类型 | 用途 |
|--------|------|------|
| `selectedImagePath` | String | 保存用户选择的图片路径 |

### 添加视图（示例）

| 视图 | 类型 | 用途 |
|------|------|------|
| `btnSelectImage` | Button | 选择图片按钮 |
| `btnSubmit` | Button | 提交按钮 |
| `imagePreview` | ImageView | 图片预览 |
| `editTitle` | EditText | 帖子标题输入 |
| `editContent` | EditText | 帖子内容输入 |

## 步骤详解

### 第 1 步：选择图片（Block 拖拽）

在 `btnSelectImage` 的 `onClick` 事件中：

```
filepicker1 setMimeType "image/*"
filepicker1 launch
```

> `setMimeType("image/*")` 限制只选择图片文件。

### 第 2 步：获取图片路径（Block 拖拽 + 自定义代码）

在 `filepicker1` 的 `onFilesPicked` 事件中：

```
// _filePath 是 ArrayList<String>，包含选中文件的绝对路径
selectedImagePath = _filePath.get(0)
imagePreview setImageBitmap (FileUtil.decodeSampleBitmapFromPath selectedImagePath, 1024)
```

> `_filePath` 由 `FileUtil.convertUriToFilePath()` 自动转换，是设备上的绝对路径。

### 第 3 步：提交上传（自定义代码块）

在 `btnSubmit` 的 `onClick` 事件中，使用**自定义代码块（addSourceDirectly）**：

```java
// 设置表单参数
HashMap<String, Object> params = new HashMap<>();
params.put("id", "你的帐号");
params.put("api", "你的API标识");
params.put("user", editUser.getText().toString());
params.put("password", editPassword.getText().toString());
params.put("title", editTitle.getText().toString());
params.put("content", editContent.getText().toString());
params.put("category", "默认分类");
requestnetwork1.setParams(params, RequestNetworkController.REQUEST_PARAM);

// 上传图片 + 表单参数
requestnetwork1.uploadFile(
    "http://dongming.fun/api/post.php",  // 服务器地址
    "image",                              // 文件参数名（由服务器 API 决定）
    selectedImagePath,                    // 图片文件路径
    "upload",                             // 请求标签（tag）
    _requestnetwork1_request_listener     // 回调监听器
);
```

### 第 4 步：处理服务器响应（Block 拖拽）

在 `requestnetwork1` 的 `onResponse` 事件中：

```
// tag = "upload", response = 服务器返回的 JSON 字符串
showMessage("发布成功: " + response)
```

在 `requestnetwork1` 的 `onErrorResponse` 事件中：

```
showMessage("上传失败: " + message)
```

## uploadFile 方法说明

### 方法签名

```java
requestnetwork.uploadFile(
    String url,           // 服务器 URL
    String fileKey,       // 文件在表单中的参数名
    String filePath,      // 本地文件的绝对路径
    String tag,           // 请求标签，用于在回调中区分不同请求
    RequestListener listener  // 回调监听器
);
```

### 工作原理

1. 将 `setParams()` 设置的所有参数作为 `multipart/form-data` 的文本字段发送
2. 将指定路径的文件作为 `multipart/form-data` 的文件字段发送
3. 自动根据文件扩展名设置 MIME 类型：

| 扩展名 | MIME 类型 |
|--------|-----------|
| `.jpg` / `.jpeg` | `image/jpeg` |
| `.png` | `image/png` |
| `.gif` | `image/gif` |
| `.webp` | `image/webp` |
| `.mp4` | `video/mp4` |
| `.mp3` | `audio/mpeg` |
| 其他 | `application/octet-stream` |

4. 回调复用已有的 `RequestListener` 接口（`onResponse` / `onErrorResponse`）

## 完整示例：发帖 + 上传图片

以下是一个完整的发帖流程，对应截图中的 API：

### API 信息

```
URL:      http://dongming.fun/api/post.php
方法:     POST
参数:     id, api, user, password, title, content, category
图片:     支持上传一张图片（multipart/form-data）
```

### Block 逻辑

```
===== onCreate =====
（无特殊初始化）

===== btnSelectImage onClick =====
filepicker1 setMimeType "image/*"
filepicker1 launch

===== filepicker1 onFilesPicked =====
selectedImagePath = _filePath.get(0)
imagePreview setImageBitmap (FileUtil.decodeSampleBitmapFromPath selectedImagePath, 1024)

===== filepicker1 onFilesPickedCancel =====
showMessage("已取消选择")

===== btnSubmit onClick =====
【自定义代码块 - 见第 3 步】

===== requestnetwork1 onResponse =====
showMessage("发布成功")

===== requestnetwork1 onErrorResponse =====
showMessage("发布失败: " + message)
```

## 注意事项

1. **权限**：FilePicker 组件会自动添加 `READ_EXTERNAL_STORAGE` 权限
2. **文件大小**：上传大文件时建议先压缩图片，避免超时
3. **线程安全**：`uploadFile` 内部使用 OkHttp3 异步请求，不会阻塞主线程
4. **回调**：`onResponse` 和 `onErrorResponse` 在主线程回调，可直接更新 UI
5. **参数名**：`fileKey` 参数（如 `"image"`）由服务器 API 决定，需查阅 API 文档
