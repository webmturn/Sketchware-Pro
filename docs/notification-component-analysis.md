# Notification 组件实现分析

> **✅ 状态：已全部实现。** 以下分析中标记的所有 ❌ 层级均已完成实现。
> 实现涉及的文件：`BlockCodeRegistry.java`、`BlockSpecRegistry.java`、`BlockColorMapper.java`、
> `ExtraPaletteBlock.java`、`StringResource.java`、`ComponentCodeGenerator.java`、`ComponentTypeMapper.java`、`EditorManifest.java`。

## 原始现状（实现前）

Notification 组件 (`COMPONENT_TYPE_NOTIFICATION = 26`) 曾是一个**空壳组件**：
- 已在 UI 中注册（可添加），但没有任何积木、代码生成、import
- 用户添加后无法使用，生成的代码仅为无用的 `private Notification myNotif;`

## 7 层架构分析

### 第 1 层：组件注册 ✅ 已有（实现前即存在）

| 文件 | 位置 | 状态 |
|------|------|------|
| `ComponentBean.java` | `COMPONENT_TYPE_NOTIFICATION = 26` | ✅ |
| `AddComponentBottomSheet.java` | `componentList.add(...)` | ✅ |
| `ComponentBean.getComponentTypeName()` | `case 26 → "Notification"` | ✅ |

### 第 2 层：类型映射 ✅ 已实现

**`ComponentTypeMapper.java`** — 3 处修改：

1. `getActualTypeName()`: 添加 `case "Notification" -> "NotificationCompat.Builder";`
2. `getImportsByTypeName()`: 添加 Notification 的 import 列表：
   - `android.app.NotificationChannel`
   - `android.app.NotificationManager`
   - `android.app.PendingIntent`
   - `android.content.Context`
   - `android.os.Build`
   - `androidx.core.app.NotificationCompat`
3. `getInternalTypeName()`: 添加 `case "notification", "Notification" -> "Notification";`

### 第 3 层：字段声明 ✅ 已实现

**`ComponentCodeGenerator.getFieldDeclaration()`** — 特殊处理双字段：

```java
case "Notification":
    fieldDeclaration += " NotificationCompat.Builder " + typeInstanceName + ";";
    fieldDeclaration += "\r\nprivate NotificationManager _nm_" + typeInstanceName + ";";
    break;
```

生成结果：
```java
private NotificationCompat.Builder myNotif;
private NotificationManager _nm_myNotif;
```

### 第 4 层：初始化代码 ✅ 已实现

**`ComponentCodeGenerator.getComponentInitializerCode()`**:

```java
case "Notification":
    return "_nm_" + componentName + " = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);\r\n"
         + componentName + " = new NotificationCompat.Builder(this, \"default_channel\");\r\n"
         + componentName + ".setSmallIcon(R.drawable.app_icon);";
```

### 第 5 层：积木 Spec 定义 ✅ 已实现

积木通过 `BlockSpecRegistry` 的 HashMap 注册表直接注册（`BlockSpecRegistry.java` 已重构为注册表模式）。

已实现的积木（方案 C — Builder 模式）：

| opCode | spec 格式 | 类型 | 参数 |
|--------|----------|------|------|
| `notifCreateChannel` | `%m.notification createChannel id %s name %s importance %m.notifImportance` | `" "` | 4 |
| `notifSetChannel` | `%m.notification setChannel %s` | `" "` | 2 |
| `notifSetTitle` | `%m.notification setTitle %s` | `" "` | 2 |
| `notifSetContent` | `%m.notification setContent %s` | `" "` | 2 |
| `notifSetSmallIcon` | `%m.notification setSmallIcon %s` | `" "` | 2 |
| `notifSetAutoCancel` | `%m.notification setAutoCancel %b` | `" "` | 2 |
| `notifSetPriority` | `%m.notification setPriority %m.notifPriority` | `" "` | 2 |
| `notifSetClickIntent` | `%m.notification setClickIntent %m.intent` | `" "` | 2 |
| `notifShow` | `%m.notification show id %d` | `" "` | 2 |
| `notifCancel` | `%m.notification cancel id %d` | `" "` | 2 |

下拉菜单常量（`BlockConstants.java`）：
```java
public static final String[] NOTIFICATION_IMPORTANCE = {
    "IMPORTANCE_DEFAULT", "IMPORTANCE_HIGH", "IMPORTANCE_LOW",
    "IMPORTANCE_MIN", "IMPORTANCE_NONE"
};
public static final String[] NOTIFICATION_PRIORITY = {
    "PRIORITY_DEFAULT", "PRIORITY_HIGH", "PRIORITY_LOW",
    "PRIORITY_MIN", "PRIORITY_MAX"
};
```

### 第 6 层：积木面板注册 ✅ 已实现

**`ExtraPaletteBlock.setBlock()`** — 在 `case 7:` 中添加：

```java
if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_NOTIFICATION)) {
    logicEditor.addPaletteCategory("Notification", getTitleBgColor());
    logicEditor.createPaletteBlock(" ", "notifCreateChannel");
    logicEditor.createPaletteBlock(" ", "notifSetChannel");
    logicEditor.createPaletteBlock(" ", "notifSetTitle");
    logicEditor.createPaletteBlock(" ", "notifSetContent");
    logicEditor.createPaletteBlock(" ", "notifSetSmallIcon");
    logicEditor.createPaletteBlock(" ", "notifSetAutoCancel");
    logicEditor.createPaletteBlock(" ", "notifSetPriority");
    logicEditor.createPaletteBlock(" ", "notifSetClickIntent");
    logicEditor.createPaletteBlock(" ", "notifShow");
    logicEditor.createPaletteBlock(" ", "notifCancel");
}
```

### 第 7 层：代码生成 ✅ 已实现

**`BlockCodeRegistry.java`** — 10 个 handler（注册表模式，已从 `BlockInterpreter` 迁移）：

```java
case "notifCreateChannel":
    opcode = String.format(
        "if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {\r\n" +
        "NotificationChannel _channel_%s = new NotificationChannel(%s, %s, NotificationManager.%s);\r\n" +
        "_nm_%s.createNotificationChannel(_channel_%s);\r\n}",
        params.get(0), params.get(1), params.get(2), params.get(3),
        params.get(0), params.get(0));
    break;

case "notifSetChannel":
    opcode = String.format("%s = new NotificationCompat.Builder(this, %s);",
        params.get(0), params.get(1));
    break;

case "notifSetTitle":
    opcode = String.format("%s.setContentTitle(%s);", params.get(0), params.get(1));
    break;

case "notifSetContent":
    opcode = String.format("%s.setContentText(%s);", params.get(0), params.get(1));
    break;

case "notifSetSmallIcon":
    opcode = String.format("%s.setSmallIcon(R.drawable.%s);", params.get(0), params.get(1));
    break;

case "notifSetAutoCancel":
    opcode = String.format("%s.setAutoCancel(%s);", params.get(0), params.get(1));
    break;

case "notifSetPriority":
    opcode = String.format("%s.setPriority(NotificationCompat.%s);", params.get(0), params.get(1));
    break;

case "notifSetClickIntent":
    opcode = String.format(
        "%s.setContentIntent(PendingIntent.getActivity(this, 0, %s, " +
        "PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));",
        params.get(0), params.get(1));
    break;

case "notifShow":
    opcode = String.format("_nm_%s.notify((int)(%s), %s.build());",
        params.get(0), params.get(1), params.get(0));
    break;

case "notifCancel":
    opcode = String.format("_nm_%s.cancel((int)(%s));", params.get(0), params.get(1));
    break;
```

## 其他已处理的文件

| 文件 | 修改内容 | 状态 |
|------|---------|------|
| `BlockColorMapper.java` | 添加 notification 积木的颜色映射 | ✅ 已实现 |
| `EditorManifest.java` | 当项目使用 Notification 组件时，自动添加 `POST_NOTIFICATIONS` 权限 | ✅ 已实现 |
| `ExtraMenuBean.java` | `%m.notification` 菜单已有（case "notification"），无需修改 | ✅ 无需改动 |
| `StringResource.java` | 添加 10 个 notif 积木的本地化字符串 | ✅ 已实现 |

## 不需要事件

Notification 组件**不需要**监听事件：
- 通知是"发后不管"操作，`NotificationManager.notify()` 同步返回
- 点击跳转由 `PendingIntent` 处理，不回调当前 Activity
- Android 的 `NotificationManager` 不提供发送成功/失败的回调
- `POST_NOTIFICATIONS` 权限由通用权限系统处理

## 实际改动量

- **修改文件数**: 8 个（`ComponentTypeMapper`、`ComponentCodeGenerator` × 2、`BlockSpecRegistry`、`BlockCodeRegistry`、`ExtraPaletteBlock`、`BlockColorMapper`、`StringResource`、`EditorManifest`）
- **新增代码量**: ~300 行
- **设备调试修复**: 8 处（参数越界 guard、图标路径、Channel 重建、`this` 引用、Android 13+ 运行时权限等）

## 生成代码示例

用户添加 Notification 组件 `myNotif` 后，生成的完整代码：

```java
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class MainActivity extends AppCompatActivity {

    private NotificationCompat.Builder myNotif;
    private NotificationManager _nm_myNotif;

    private void initialize() {
        _nm_myNotif = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        myNotif = new NotificationCompat.Builder(this, "default_channel");
        myNotif.setSmallIcon(R.drawable.app_icon);

        // 用户拖拽积木生成的代码：
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel _channel_myNotif = new NotificationChannel(
                "msg_channel", "消息通知", NotificationManager.IMPORTANCE_DEFAULT);
            _nm_myNotif.createNotificationChannel(_channel_myNotif);
        }
        myNotif = new NotificationCompat.Builder(this, "msg_channel");
        myNotif.setContentTitle("新消息");
        myNotif.setContentText("你有一条未读消息");
        myNotif.setSmallIcon(R.drawable.app_icon);
        myNotif.setAutoCancel(true);
        myNotif.setPriority(NotificationCompat.PRIORITY_HIGH);
        _nm_myNotif.notify(1001, myNotif.build());
    }
}
```
