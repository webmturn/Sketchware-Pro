# Sketchware-Pro 项目数据格式文档

## 快速入门清单

> 以下是从零创建一个可编译项目的**最小步骤清单**。

1. **选择项目 ID**（如 `700`），确保设备上不存在同名目录
2. **创建 6 个文件**，内容如下：

| # | 文件 | 最小内容 |
|---|------|----------|
| 1 | `project` | 一行 JSON：`{"sc_id":"700","my_app_name":"MyApp","my_sc_pkg_name":"com.my.app","my_ws_name":"MyApp","sc_ver_code":"1","sc_ver_name":"1.0","sketchware_ver":150,"my_sc_reg_dt":"20260226120000","color_accent":-1,"color_primary":-1,"color_primary_dark":-1,"color_control_highlight":-1,"color_control_normal":-1,"custom_icon":false}` |
| 2 | `file` | `@activity` + 一行 Activity JSON + `@customview` |
| 3 | `library` | `@firebaseDB` `@compat` `@admob` `@googleMap` 各一行 `useYn:"N"` JSON |
| 4 | `resource` | `@images` `@sounds` `@fonts` 三个空节 |
| 5 | `view` | `@main.xml` + 至少一个 ViewBean JSON 行 |
| 6 | `logic` | `@MainActivity.java_var` `_list` `_func` `_components` `_events` + `@MainActivity.java_onCreate_initializeLogic` |

3. **推送到设备**（同时推 `data/{id}/` 和 `bak/{id}/`，project 推到 `mysc/list/{id}/`）
4. **强制停止 Sketchware**: `adb shell am force-stop pro.sketchware`
5. 打开 Sketchware → 找到项目 → 编译运行

> **关键检查项**:
> - `backgroundResource` 为 `"NONE"` 或 `null`（不是 `""`）
> - `backgroundResColor` / `resTextColor` / `resHintColor` 为 `null`（不是 `""`）
> - `initializeLogic` 不在 `_events` 节中声明
> - `initializeLogic` 段名为 `@{javaName}_onCreate_initializeLogic`
> - MoreBlock 函数声明放在 `@{javaName}_func` 节，格式为 `name:spec`（冒号分隔），块逻辑放在 `@{javaName}_{funcName}_moreBlock`
> - Block ID 在同一 Activity 内全局唯一
> - 所有 ViewBean 字段必须完整（不可省略）

## 最小 Hello World 项目模板

以下是一个可直接编译运行的最小项目（ID=700），点击按钮显示 Toast：

### project（明文，推到 `mysc/list/700/project`）
```json
{"sc_id":"700","my_app_name":"HelloApp","my_sc_pkg_name":"com.my.hello","my_ws_name":"HelloApp","sc_ver_code":"1","sc_ver_name":"1.0","sketchware_ver":150,"my_sc_reg_dt":"20260226120000","color_accent":-16537100,"color_primary":-16537100,"color_primary_dark":-16537100,"color_control_highlight":-2497793,"color_control_normal":-16537100,"custom_icon":false}
```

### file
```
@activity
{"fileName":"main","fileType":0,"keyboardSetting":0,"options":1,"orientation":0,"theme":-1}
@customview
```

### library
```
@firebaseDB
{"adUnits":[],"appId":"","configurations":{},"data":"","libType":0,"reserved1":"","reserved2":"","reserved3":"","testDevices":[],"useYn":"N"}
@compat
{"adUnits":[],"appId":"","configurations":{},"data":"","libType":1,"reserved1":"","reserved2":"","reserved3":"","testDevices":[],"useYn":"Y"}
@admob
{"adUnits":[],"appId":"","configurations":{},"data":"","libType":2,"reserved1":"","reserved2":"","reserved3":"","testDevices":[],"useYn":"N"}
@googleMap
{"adUnits":[],"appId":"","configurations":{},"data":"","libType":3,"reserved1":"","reserved2":"","reserved3":"","testDevices":[],"useYn":"N"}
```

### resource
```
@images
@sounds
@fonts
```

### view
```
@main.xml
{"adSize":"","adUnitId":"","alpha":1.0,"checked":0,"choiceMode":0,"clickable":1,"convert":"","customView":"","dividerHeight":1,"enabled":1,"firstDayOfWeek":1,"id":"button1","image":{"rotate":0,"scaleType":"CENTER"},"indeterminate":"false","index":0,"inject":"","layout":{"backgroundColor":-16537100,"backgroundResColor":null,"backgroundResource":"NONE","borderColor":0,"gravity":17,"height":-2,"layoutGravity":0,"marginBottom":0,"marginLeft":0,"marginRight":0,"marginTop":0,"orientation":-1,"paddingBottom":8,"paddingLeft":16,"paddingRight":16,"paddingTop":8,"weight":0,"weightSum":0,"width":-1},"max":100,"parentAttributes":{},"parentType":0,"preId":"","preIndex":0,"preParent":"","preParentType":0,"progress":0,"progressStyle":"?android:progressBarStyle","scaleX":1.0,"scaleY":1.0,"spinnerMode":1,"text":{"hint":"","hintColor":0,"imeOption":0,"inputType":1,"line":0,"singleLine":0,"text":"Click Me!","textColor":-1,"textFont":"default_font","textSize":16,"textType":1},"translationX":0.0,"translationY":0.0,"type":3,"parent":"root"}
```

### logic
```
@MainActivity.java_var
@MainActivity.java_list
@MainActivity.java_func
@MainActivity.java_components
@MainActivity.java_events
{"eventName":"onClick","eventType":1,"targetId":"button1","targetType":3}
@MainActivity.java_onCreate_initializeLogic
@MainActivity.java_button1_onClick
{"color":-13851166,"id":"1","nextBlock":-1,"opCode":"doToast","parameters":["Hello World!"],"spec":"Toast %s","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
```

> 将以上 6 个文件推送到 `data/700/` 和 `bak/700/`，project 推送到 `mysc/list/700/project`，即可在 Sketchware 中编译运行。

---

## 生成代码结构

Sketchware 为每个 Activity 生成一个 Java 文件，结构如下：

```java
package com.my.app;

// ===== 自动导入 =====
import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.content.*;
// ... (约20个标准导入)
// + 组件相关导入（如 Firebase, OkHttp 等）

public class MainActivity extends AppCompatActivity {

    // ===== 1. 自动生成的字段 =====
    // options=1 时自动生成:
    private Toolbar _toolbar;
    private AppBarLayout _app_bar;
    private CoordinatorLayout _coordinator;

    // ===== 2. view 文件中的视图 → 字段 =====
    private LinearLayout panel_list;
    private Button btn_save;
    private EditText edittext_title;
    // ... 每个 ViewBean 生成一个字段

    // ===== 3. _var 节中的变量 =====
    private double editIndex = 0;
    private String currentTitle = "";
    private ArrayList<HashMap<String, Object>> notes = new ArrayList<>();

    // ===== 4. _components 节中的组件 =====
    private SharedPreferences sp;

    // ===== 5. onCreate 方法 =====
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // 5a. 自动 findViewById（所有 view 文件中的视图）
        panel_list = findViewById(R.id.panel_list);
        btn_save = findViewById(R.id.btn_save);

        // 5b. Toolbar/Drawer/FAB 初始化（如果 options 启用）
        _toolbar = findViewById(R.id._toolbar);
        setSupportActionBar(_toolbar);

        // 5c. 组件初始化
        sp = getSharedPreferences("", Activity.MODE_PRIVATE);

        // 5d. 事件监听器绑定
        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View _view) {
                // → @MainActivity.java_btn_save_onClick 段的积木块
            }
        });

        // 5e. initializeLogic() 调用
        initializeLogic();
    }

    // ===== 6. initializeLogic 方法 =====
    private void initializeLogic() {
        // → @MainActivity.java_onCreate_initializeLogic 段的积木块
    }

    // ===== 7. 事件处理代码 =====
    // （内联在 onCreate 的监听器中）

    // ===== 8. MoreBlock 自定义方法 =====
    public void _refreshList() {
        // → @MainActivity.java_refreshList_moreBlock 段的积木块
    }

    // ===== 9. 适配器内部类（如有 Custom ListView）=====
    public class Custom_itemAdapter extends BaseAdapter { ... }
}
```

> 理解此结构有助于正确使用 `addSourceDirectly`：你写的代码会被插入到对应位置，可以直接引用上面列出的所有字段和变量。

## 常见多块模式

### Dialog 弹窗

```json
// 显示一个确认对话框（dialogOkButton/dialogCancelButton 是 C 形块，subStack1 内放点击回调逻辑）
{"id":"1","opCode":"dialogSetTitle","parameters":["dialog1","Confirm"],"spec":"%m.dialog set title %s",
 "nextBlock":2,"subStack1":-1,"subStack2":-1,"type":" "}
{"id":"2","opCode":"dialogSetMessage","parameters":["dialog1","Are you sure?"],"spec":"%m.dialog set message %s",
 "nextBlock":3,"subStack1":-1,"subStack2":-1,"type":" "}
{"id":"3","opCode":"dialogOkButton","parameters":["dialog1","Yes"],"spec":"%m.dialog OK Button %s Clicked",
 "nextBlock":4,"subStack1":10,"subStack2":-1,"type":"c"}
// subStack1 内的块（点击 Yes 后执行）
{"id":"10","opCode":"doToast","parameters":["Confirmed!"],"spec":"Toast %s",
 "nextBlock":-1,"subStack1":-1,"subStack2":-1,"type":" "}
{"id":"4","opCode":"dialogCancelButton","parameters":["dialog1","No"],"spec":"%m.dialog Cancel Button %s Clicked",
 "nextBlock":5,"subStack1":-1,"subStack2":-1,"type":"c"}
{"id":"5","opCode":"dialogShow","parameters":["dialog1"],"spec":"%m.dialog show",
 "nextBlock":-1,"subStack1":-1,"subStack2":-1,"type":" "}
```

需要在 `_components` 中声明: `{"componentId":"dialog1","param1":"","param2":"","param3":"","type":7}`

> **注意**: `dialogOkButton`/`dialogCancelButton`/`dialogNeutralButton` 都是 **C 形块**，点击回调逻辑直接放在 subStack1 中，**不需要单独声明事件**。如果 subStack1=-1 则表示空回调（仅关闭对话框）。

### Timer 定时器

```json
// 每 1000ms 执行一次（timerEvery 是 C 形块，subStack1 内放每次执行的逻辑）
{"id":"1","opCode":"timerEvery","parameters":["timer1","0","1000"],
 "spec":"%m.timer after %d ms for every %d ms","subStack1":2,"nextBlock":-1,"type":"c","color":-14575885}
// subStack1 内的块（每 1000ms 执行）
{"id":"2","opCode":"doToast","parameters":["Tick!"],"spec":"Toast %s",
 "nextBlock":-1,"subStack1":-1,"subStack2":-1,"type":" ","color":-7584130}

// 延时执行（只执行一次）
{"opCode":"timerAfter","parameters":["timer1","3000"],"spec":"%m.timer after %d ms",
 "subStack1":5,"type":"c"}

// 停止定时器
{"opCode":"timerCancel","parameters":["timer1"],"spec":"%m.timer cancel","type":" "}
```

需要声明: `{"componentId":"timer1","param1":"","param2":"","param3":"","type":5}`

> **注意**: `timerEvery` 和 `timerAfter` 是 **C 形块**（有 subStack1），定时逻辑直接放在 subStack 内，**不需要声明事件**。

### SharedPreferences 数据持久化

```json
// 初始化（在 initializeLogic 中）
{"opCode":"fileSetFileName","parameters":["sp","my_prefs"],"spec":"%m.file setFileName %s"}

// 写入
{"opCode":"fileSetData","parameters":["sp","key_name","value"],"spec":"%m.file setData key %s value %s"}

// 读取（返回字符串，type="s"）
{"opCode":"fileGetData","parameters":["sp","key_name"],"spec":"%m.file getData key %s"}
```

### Intent 打开网页

```json
{"opCode":"intentSetAction","parameters":["intent1","android.intent.action.VIEW"],"spec":"%m.intent setAction %m.intentAction"}
{"opCode":"intentSetData","parameters":["intent1","@2"],"spec":"%m.intent setData %s.intentData"}
{"opCode":"startActivity","parameters":["intent1"],"spec":"StartActivity %m.intent"}
// 块2: toStringWithFormat URI
{"id":"2","opCode":"addSourceDirectly","parameters":["Uri.parse(\"https://example.com\")"],"type":"s","nextBlock":-1}
```

### 切换视图可见性（面板切换）

```json
{"opCode":"setVisible","parameters":["panel_list","GONE"],"spec":"%m.view setVisible %m.visible"}
{"opCode":"setVisible","parameters":["panel_edit","VISIBLE"],"spec":"%m.view setVisible %m.visible"}
```

### 多 Activity Intent 导航

在 `file` 文件中声明多个 Activity：
```
@activity
{"fileName":"main","fileType":0,"keyboardSetting":0,"options":1,"orientation":0,"theme":-1}
{"fileName":"detail","fileType":0,"keyboardSetting":0,"options":1,"orientation":0,"theme":-1}
@customview
```

fileName 到 Java 类名映射：`main` → `MainActivity`，`detail` → `DetailActivity`。

在 `_components` 中声明 Intent：`{"componentId":"i","param1":"","param2":"","param3":"","type":1}`

跳转到另一个 Activity 并传递数据：
```json
{"opCode":"intentSetScreen","parameters":["i","DetailActivity"],"spec":"%m.intent setScreen %m.activity","type":" "}
{"opCode":"intentPutExtra","parameters":["i","key","value"],"spec":"%m.intent putExtra key %s value %s","type":" "}
{"opCode":"startActivity","parameters":["i"],"spec":"StartActivity %m.intent","type":" "}
```

在目标 Activity 中读取传递的数据：
```json
// intentGetString 返回字符串，用 addSourceDirectly 转换为其他类型
{"opCode":"addSourceDirectly","parameters":["String _val = getIntent().getStringExtra(\"key\");"],"type":" "}
```

关闭当前 Activity 返回上一个：
```json
{"opCode":"finishActivity","parameters":[],"spec":"Finish Activity","type":" "}
```

### 底部导航栏（BottomNav 模拟）

Sketchware 无内置 BottomNavigationView，可用 LinearLayout + 可点击 TextView 模拟：

**View 结构**（root 的两个子视图）：
```
root (vertical LinearLayout, 隐式)
├── linear_content  ← height=0, weight=1（填满剩余空间）
│   ├── linear_tab1_panel  ← VISIBLE（当前 tab）
│   ├── linear_tab2_panel  ← GONE
│   └── linear_tab3_panel  ← GONE
└── linear_nav      ← height=wrap(-2), horizontal, bg=深色
    ├── tv_tab1     ← weight=1, width=0, clickable=1, 白色文字(active)
    ├── tv_tab2     ← weight=1, width=0, clickable=1, 灰色文字
    └── tv_tab3     ← weight=1, width=0, clickable=1, 灰色文字
```

**关键 layout 属性**：
- `linear_content`: `{"height":0,"weight":1,"orientation":1,"width":-1}` — 填满 nav 上方空间
- `linear_nav`: `{"height":-2,"orientation":0,"backgroundColor":-13421773}` — 底部固定
- 每个 tab TextView: `{"width":0,"weight":1,"gravity":17,"paddingTop":12,"paddingBottom":12}` — 等宽分布

**Tab 点击事件**（切换面板 + 更新文字颜色）：
```json
// _events 声明
{"eventName":"onClick","eventType":1,"targetId":"tv_tab1","targetType":4}

// onClick 逻辑
{"opCode":"setVisible","parameters":["linear_tab1_panel","VISIBLE"],"spec":"%m.view setVisible %m.visible","type":" "}
{"opCode":"setVisible","parameters":["linear_tab2_panel","GONE"],"spec":"%m.view setVisible %m.visible","type":" "}
{"opCode":"addSourceDirectly","parameters":["tv_tab1.setTextColor(0xFFFFFFFF); tv_tab1.setTypeface(null, android.graphics.Typeface.BOLD); tv_tab2.setTextColor(0xFFA8A8A8); tv_tab2.setTypeface(null, android.graphics.Typeface.NORMAL);"],"type":" "}
```

### onResume 刷新数据

从另一个 Activity 返回后需要刷新数据时，使用 `onResume` 事件：

```json
// _events 声明（eventType=3 表示 Activity 事件）
{"eventName":"onResume","eventType":3,"targetId":"onResume","targetType":-1}

// 积木块段名: @{javaName}_onResume_onResume
// 在此段中调用数据加载 MoreBlock
{"opCode":"definedFunc","parameters":[],"spec":"loadData","type":" "}
```

> **注意**: `onResume` 在 `onCreate` 之后也会被调用，因此首次启动时 `initializeLogic` 和 `onResume` 中的代码都会执行。确保数据加载逻辑是幂等的（如先 `clear()` 再加载）。

### View weight 弹性布局

使用 `weight` 实现弹性布局（类似 CSS flex）：

| 场景 | 父容器 orientation | 子视图设置 |
|------|-------------------|-----------|
| 填满剩余高度 | vertical (1) | `height:0, weight:1` |
| 等宽分布 | horizontal (0) | `width:0, weight:1` |
| 固定 + 弹性 | vertical (1) | 固定部分 `height:-2`, 弹性部分 `height:0, weight:1` |

> 父容器的 `weightSum` 设为 `0` 即可（系统自动计算为所有子视图 weight 之和）。

### inject 字段添加自定义属性

ViewBean 的 `inject` 字段可向生成的 XML 注入属性：

```json
// 添加卡片阴影（API 21+）
{"id":"card1","inject":"android:elevation=\"4dp\"","type":0,...}

// EditText 多行输入配置
{"id":"et1","inject":"android:scrollbars=\"vertical\" android:inputType=\"textMultiLine\"","type":5,...}
```

---

## 概述

Sketchware-Pro 的项目数据存储在设备的 `/storage/emulated/0/.sketchware/` 目录下。

> **重要**: 打开项目时优先从 `bak/` 加载，如果 `bak/` 不存在则从 `data/` 加载。修改文件时需要**同时更新两个目录**。

### 完整目录结构

```
/storage/emulated/0/.sketchware/
├── mysc/                                    ← 项目构建与元数据
│   ├── list/                                ← 项目列表（每个项目一个子目录）
│   │   ├── {id}/
│   │   │   └── project                      ← 项目配置（明文 JSON，不加密）
│   │   └── ...
│   └── {id}/                                ← 编译输出（自动生成，不需手动创建）
│       ├── app/                             ← 生成的源码
│       ├── bin/                             ← 编译产物（APK 等）
│       ├── gen/                             ← 生成的 R.java 等
│       ├── build.gradle                     ← 生成的构建脚本
│       ├── gradle.properties
│       └── settings.gradle
│
├── data/                                    ← 项目数据（主存储）
│   └── {id}/
│       ├── file                             ← ⭐ Activity/CustomView 声明
│       ├── library                          ← ⭐ 库配置
│       ├── logic                            ← ⭐ 变量/组件/事件/积木块逻辑
│       ├── resource                         ← ⭐ 资源声明（图片/声音/字体名）
│       ├── view                             ← ⭐ UI 视图树（ViewBean JSON）
│       ├── permission                       ← 自定义权限（每行一个）
│       ├── proguard                         ← ProGuard 开关 JSON
│       ├── proguard-rules.pro               ← ProGuard 规则
│       ├── stringfog                        ← 字符串加密开关 JSON
│       ├── custom_blocks                    ← 自定义积木块 JSON 数组
│       ├── local_library                    ← 本地库引用 JSON 数组
│       ├── build_config                     ← 构建配置 JSON
│       ├── project_config                   ← 项目附加配置 JSON
│       ├── google_service                   ← Google Services 版本号
│       ├── gradle_tool                      ← Gradle 工具版本号
│       ├── compile_log                      ← 编译日志（自动生成）
│       ├── Injection/                       ← AndroidManifest 注入
│       ├── java/                            ← 额外 Java 源文件
│       └── files/                           ← 额外文件
│
├── bak/                                     ← 项目数据（备份，结构同 data/{id}/）
│   └── {id}/
│       └── (同 data/{id}/ 的文件结构)
│
├── resources/                               ← 项目资源文件（按类型分目录）
│   ├── images/
│   │   └── {id}/                            ← 项目图片（PNG/JPG，文件名=资源名）
│   │       ├── my_icon.png
│   │       └── background.jpg
│   ├── sounds/
│   │   └── {id}/                            ← 项目声音文件
│   ├── fonts/
│   │   └── {id}/                            ← 项目字体文件（TTF）
│   ├── icons/
│   │   └── {id}/                            ← 项目自定义图标
│   └── block/                               ← 积木块图标
│
├── collection/                              ← 收藏/模板
│   ├── block/                               ← 收藏的积木块组合
│   ├── more_block/                          ← 收藏的 MoreBlock
│   ├── widget/                              ← 收藏的 Widget
│   └── image/                               ← 收藏的图片
│
├── backups/                                 ← 项目备份（.swb 文件）
├── libs/                                    ← 额外库文件
│   ├── local_libs/                          ← 下载的本地库（主路径，卸载不丢失）
│   │   └── {artifactId}-v{version}/         ← 每个库一个目录
│   │       ├── classes.jar                  ← JAR 文件
│   │       ├── classes.dex                  ← DEX 文件（编译生成）
│   │       ├── config                       ← 包名配置
│   │       ├── res/                         ← 资源目录（AAR 库）
│   │       ├── AndroidManifest.xml           ← 清单文件（AAR 库）
│   │       ├── proguard.txt                 ← ProGuard 规则
│   │       └── assets/                      ← Assets 目录
│   └── repositories.json                    ← Maven 仓库配置
├── temp/                                    ← 临时文件
└── debug.txt                                ← 调试日志

# 另外，当主路径被 FUSE 阻止时（如 Samsung Android 16+），库会下载到：
# /storage/emulated/0/Android/data/<pkg>/files/local_libs/
```

### 创建新项目需要的最少文件

```
mysc/list/{id}/project                       ← 必须（明文 JSON）
data/{id}/file                               ← 必须（可加密）
data/{id}/library                            ← 必须
data/{id}/logic                              ← 必须
data/{id}/resource                           ← 必须
data/{id}/view                               ← 必须
```

> `bak/{id}/` 目录可以为空或不存在。首次打开项目时 Sketchware 会自动从 `data/` 复制到 `bak/`。
> 但建议同时推送到两个目录以确保项目立即可用。

## 加密

默认情况下，所有数据文件使用 AES/CBC/PKCS5Padding 加密（密钥和IV均为 `sketchwaresecure`）。

在设置中关闭“加密项目数据”开关后，文件以 UTF-8 明文存储。读取时自动兼容两种格式（先尝试解密，失败则当明文处理）。

### Gson 序列化行为

Sketchware 使用 `new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()` 进行序列化：

- **只序列化有 `@Expose` 注解的字段**（如 `id`, `type`, `layout` 等）
- **无 `@Expose` 的字段不会出现在 JSON 中**（如 `name`, `classInfo`, `isCustomWidget`）
- **null 字段默认不输出**（未调用 `serializeNulls()`）—— 如 `resTextColor`, `resHintColor` 为 null 时不会出现在 JSON 中
- **反序列化时 null 字段可以显式提供或省略**，效果相同

> 生成 JSON 时，可以省略 null 字段，也可以显式写 `null`。两者都能正确解析。

### 实操：如何创建加密文件

#### 方式一：关闭加密（推荐）

在 Sketchware-Pro 设置中关闭"加密项目数据"开关后，直接推送明文文件即可：

```powershell
# 直接 push 明文文件
adb push logic /storage/emulated/0/.sketchware/data/700/logic
```

#### 方式二：使用 Python 加密

```python
from Crypto.Cipher import AES
from Crypto.Util.Padding import pad

KEY = IV = b'sketchwaresecure'  # 16 bytes

def encrypt_sketchware(plaintext: str) -> bytes:
    cipher = AES.new(KEY, AES.MODE_CBC, IV)
    return cipher.encrypt(pad(plaintext.encode('utf-8'), AES.block_size))

def decrypt_sketchware(data: bytes) -> str:
    from Crypto.Util.Padding import unpad
    cipher = AES.new(KEY, AES.MODE_CBC, IV)
    return unpad(cipher.decrypt(data), AES.block_size).decode('utf-8')

# 用法
with open('logic', 'wb') as f:
    f.write(encrypt_sketchware(logic_content))
```

#### 方式三：使用 PowerShell 加密

```powershell
function Encrypt-Sketchware($plaintext) {
    $key = [System.Text.Encoding]::UTF8.GetBytes("sketchwaresecure")
    $aes = [System.Security.Cryptography.Aes]::Create()
    $aes.Key = $key; $aes.IV = $key; $aes.Mode = "CBC"; $aes.Padding = "PKCS7"
    $enc = $aes.CreateEncryptor()
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($plaintext)
    return $enc.TransformFinalBlock($bytes, 0, $bytes.Length)
}
```

> **注意**: `project` 文件（在 `mysc/list/{id}/` 下）**不加密**，始终为明文 JSON。只有 `data/` 和 `bak/` 目录下的 6 个文件需要加密。

### 完整推送脚本（PowerShell + adb）

以下脚本将本地目录中的项目文件推送到设备（假设加密已关闭）：

```powershell
$id = "700"  # 项目ID
$src = "C:\my-project"  # 本地文件目录（包含 project, file, library, logic, resource, view）
$base = "/storage/emulated/0/.sketchware"

# 1. 推送 project 文件（明文，到 mysc/list/）
adb shell "mkdir -p $base/mysc/list/$id"
adb push "$src\project" "$base/mysc/list/$id/project"

# 2. 推送 6 个核心数据文件到 data/
adb shell "mkdir -p $base/data/$id"
foreach ($f in @("file","library","logic","resource","view")) {
    adb push "$src\$f" "$base/data/$id/$f"
}

# 3. 复制到 bak/（确保立即可用）
adb shell "mkdir -p $base/bak/$id"
foreach ($f in @("file","library","logic","resource","view")) {
    adb push "$src\$f" "$base/bak/$id/$f"
}

# 4. 推送资源文件（如有）
# adb shell "mkdir -p $base/resources/images/$id"
# adb push "$src\images\my_icon.png" "$base/resources/images/$id/my_icon.png"

# 5. 强制停止 Sketchware 使其重新加载
adb shell "am force-stop pro.sketchware"

Write-Host "Project $id pushed successfully!"
```

---

## 通用格式

所有数据文件使用**分节格式**：

```
@sectionName1
{数据行1}
{数据行2}

@sectionName2
{数据行1}
```

- 以 `@` 开头的行是**节标题**
- 每个数据行是一个 JSON 对象或 `type:name` 格式的键值对
- 空行被忽略

---

## view 文件

存储 UI 组件布局信息。

### 节格式

| 节名 | 说明 | 数据格式 |
|------|------|----------|
| `@{xmlName}` | 视图列表（如 `@main.xml`） | 每行一个 ViewBean JSON |
| `@{xmlName}_fab` | FAB 按钮（如 `@main.xml_fab`） | 单个 ViewBean JSON |

> **注意**: 视图节名是 `main.xml`，**不是** `main.xml_views`。

### ViewBean 完整字段

```json
{
  "id": "button1",           // 组件ID（代码中引用的名称）
  "name": "button1",         // 显示名称（⚠️ 无 @Expose 注解，Gson 不序列化/反序列化此字段，可省略）
  "type": 3,                 // 组件类型（见下表）
  "parent": "root",          // 父容器ID（"root"=根布局）
  "parentType": 0,           // 父容器类型（0=LinearLayout）
  "index": 0,                // 在父容器中的顺序
  "preId": "",               // 之前的ID（用于撤销，通常为空）
  "preParent": "",           // 之前的父容器（通常为空）
  "preIndex": 0,             // 之前的索引
  "preParentType": 0,        // 之前的父类型
  "layout": { ... },         // 布局属性（LayoutBean）
  "text": { ... },           // 文本属性（TextBean）
  "image": { ... },          // 图片属性（ImageBean）
  "parentAttributes": {},    // 父容器属性（通常为空）
  "alpha": 1.0,              // 透明度（0.0~1.0）
  "translationX": 0.0,       // X平移
  "translationY": 0.0,       // Y平移
  "scaleX": 1.0,             // X缩放
  "scaleY": 1.0,             // Y缩放
  "enabled": 1,              // 是否启用（1=是, 0=否）
  "clickable": 1,            // 是否可点击（1=是, 0=否）
  "checked": 0,              // 选中状态（CheckBox/Switch, 0=未选中, 1=选中）
  "max": 100,                // 最大值（ProgressBar/SeekBar）
  "progress": 0,             // 当前进度（ProgressBar/SeekBar）
  "indeterminate": "false",  // 不确定模式（ProgressBar, "true"/"false"）
  "progressStyle": "?android:progressBarStyle",  // ProgressBar样式（或 "?android:progressBarStyleHorizontal"）
  "spinnerMode": 1,          // Spinner模式（0=dialog, 1=dropdown）
  "choiceMode": 0,           // ListView选择模式（0=none, 1=single, 2=multi）
  "dividerHeight": 1,        // ListView分割线高度
  "firstDayOfWeek": 1,       // CalendarView每周第一天
  "customView": "",          // 自定义视图名称（ListView/Spinner）
  "convert": "",             // 自定义视图转换器
  "inject": "",              // 注入XML属性（直接写入生成的XML标签中）
  "adSize": "",              // AdView广告尺寸
  "adUnitId": ""             // AdView广告单元ID
}
```

### 组件类型 (type)

| type | 组件 |
|------|------|
| 0 | LinearLayout |
| 1 | RelativeLayout |
| 2 | ScrollView (Horizontal) |
| 3 | Button |
| 4 | TextView |
| 5 | EditText |
| 6 | ImageView |
| 7 | WebView |
| 8 | ProgressBar |
| 9 | ListView |
| 10 | Spinner |
| 11 | CheckBox |
| 12 | ScrollView (Vertical) |
| 13 | Switch |
| 14 | SeekBar |
| 15 | CalendarView |
| 16 | FloatingActionButton |
| 17 | AdView |
| 18 | MapView |
| 19 | RadioButton |
| 20 | RatingBar |
| 21 | VideoView |
| 22 | SearchView |
| 23 | AutoCompleteTextView |
| 24 | MultiAutoCompleteTextView |
| 25 | GridView |
| 26 | AnalogClock |
| 27 | DatePicker |
| 28 | TimePicker |
| 29 | DigitalClock |
| 30 | TabLayout |
| 31 | ViewPager |
| 32 | BottomNavigationView |
| 33 | BadgeView |
| 34 | PatternLockView |
| 35 | WaveSideBar |
| 36 | CardView |
| 37 | CollapsingToolbarLayout |
| 38 | TextInputLayout |
| 39 | SwipeRefreshLayout |
| 40 | RadioGroup |
| 41 | MaterialButton |
| 42 | SignInButton |
| 43 | CircleImageView |
| 44 | LottieAnimationView |
| 45 | YoutubePlayerView |
| 46 | OTPView |
| 47 | CodeView |
| 48 | RecyclerView |

> **容器类型**（可作为其他视图的 `parent`）: 0(LinearLayout), 1(RelativeLayout), 2(HScrollView), 12(VScrollView), 30(TabLayout), 31(ViewPager), 36(CardView), 37(CollapsingToolbarLayout), 38(TextInputLayout), 39(SwipeRefreshLayout), 40(RadioGroup)

### layout 对象

```json
{
  "backgroundColor": -1,       // 背景色（ARGB int，-1=白色）
  "backgroundResColor": null,   // 背景颜色资源名（❗必须为 null 或有效资源名，空字符串会生成无效 @color/）
  "backgroundResource": "NONE", // 背景Drawable资源名（❗必须为 "NONE"、null 或有效资源名，空字符串会生成无效 @drawable/）
  "borderColor": 0,            // 边框色
  "gravity": 0,                // 内容对齐
  "height": -2,                // 高度（-1=match_parent, -2=wrap_content, >0=固定dp）
  "width": -1,                 // 宽度
  "layoutGravity": 0,          // 在父容器中的对齐
  "marginBottom": 8,
  "marginLeft": 0,
  "marginRight": 0,
  "marginTop": 0,
  "orientation": -1,           // 方向（0=horizontal, 1=vertical, -1=不适用）
  "paddingBottom": 8,
  "paddingLeft": 8,
  "paddingRight": 8,
  "paddingTop": 8,
  "weight": 0,
  "weightSum": 0
}
```

### text 对象

```json
{
  "hint": "",                  // 提示文本（EditText 为空时显示）
  "hintColor": 0,             // 提示文本颜色（ARGB int，0=默认灰色）
  "resHintColor": null,       // 提示文本颜色资源名（❗同 backgroundResColor 规则，必须为 null 或有效资源名）
  "imeOption": 0,             // 软键盘回车行为（见速查表）
  "inputType": 1,             // 输入类型（见速查表）
  "line": 0,                  // 最大行数（0=不限制）
  "singleLine": 0,            // 单行模式（0=多行, 1=单行）
  "text": "Button Text",      // 显示文本
  "textColor": -16777216,     // 文本颜色（ARGB int，-16777216=黑色）
  "resTextColor": null,       // 文本颜色资源名（❗同 backgroundResColor 规则，必须为 null 或有效资源名）
  "textFont": "default_font", // 字体（见速查表）
  "textSize": 14,             // 字号（sp）
  "textType": 0               // 字体样式（见速查表）
}
```

> ⚠️ `resTextColor` 和 `resHintColor` 遵循与 `backgroundResColor` 相同的规则：必须为 `null` 或有效资源名，**不可为空字符串 `""`**。

### image 对象

```json
{
  "resName": "default_image",   // 图片资源名（不含扩展名；常见值: "default_image"=无图片, "NONE"=无图片, ""=无图片, 或 resources/images 中的文件名）
  "rotate": 0,                 // 旋转角度
  "scaleType": "CENTER"        // 缩放类型（CENTER, FIT_XY, CENTER_CROP, CENTER_INSIDE, FIT_CENTER, FIT_START, FIT_END）
}
```

### 完整示例

```
@main.xml
{"adSize":"","adUnitId":"","alpha":1.0,"checked":0,"choiceMode":0,"clickable":1,"convert":"","customView":"","dividerHeight":1,"enabled":1,"firstDayOfWeek":1,"id":"button1","image":{"rotate":0,"scaleType":"CENTER"},"indeterminate":"false","index":0,"inject":"","layout":{"backgroundColor":-14575885,"backgroundResColor":null,"backgroundResource":"NONE","borderColor":0,"gravity":0,"height":-2,"layoutGravity":0,"marginBottom":8,"marginLeft":0,"marginRight":0,"marginTop":0,"orientation":-1,"paddingBottom":8,"paddingLeft":8,"paddingRight":8,"paddingTop":8,"weight":0,"weightSum":0,"width":-1},"max":100,"name":"button1","parent":"root","parentAttributes":{},"parentType":0,"preId":"","preIndex":0,"preParent":"","preParentType":0,"progress":0,"progressStyle":"?android:progressBarStyle","scaleX":1.0,"scaleY":1.0,"spinnerMode":1,"text":{"hint":"","hintColor":0,"imeOption":0,"inputType":1,"line":0,"singleLine":0,"text":"Click Me","textColor":-1,"textFont":"default_font","textSize":14,"textType":0},"translationX":0.0,"translationY":0.0,"type":3}
@main.xml_fab
{"adSize":"","adUnitId":"","alpha":1.0,"checked":0,"choiceMode":0,"clickable":1,"convert":"","customView":"","dividerHeight":1,"enabled":1,"firstDayOfWeek":1,"id":"_fab","image":{"rotate":0,"scaleType":"CENTER"},"indeterminate":"false","index":0,"inject":"","layout":{"backgroundColor":16777215,"backgroundResColor":null,"backgroundResource":"NONE","borderColor":-16740915,"gravity":0,"height":-2,"layoutGravity":85,"marginBottom":16,"marginLeft":16,"marginRight":16,"marginTop":16,"orientation":-1,"paddingBottom":0,"paddingLeft":0,"paddingRight":0,"paddingTop":0,"weight":0,"weightSum":0,"width":-2},"max":100,"name":"_fab","parent":"root","parentAttributes":{},"parentType":-1,"preId":"","preIndex":0,"preParent":"","preParentType":0,"progress":0,"progressStyle":"?android:progressBarStyle","scaleX":1.0,"scaleY":1.0,"spinnerMode":1,"text":{"hint":"","hintColor":16777215,"imeOption":0,"inputType":1,"line":0,"singleLine":0,"text":"","textColor":16777215,"textFont":"default_font","textSize":12,"textType":0},"translationX":0.0,"translationY":0.0,"type":16}
```

---

## logic 文件

存储变量、组件、事件和积木块逻辑。

### 节格式

| 节名模式 | 说明 | 数据格式 |
|----------|------|----------|
| `@{javaName}_var` | 变量 | `type:name`（每行一个） |
| `@{javaName}_list` | 列表变量 | `type:name`（每行一个） |
| `@{javaName}_components` | 组件 | 每行一个 ComponentBean JSON |
| `@{javaName}_func` | 自定义函数声明 | 每行 `functionName:spec` 格式 |
| `@{javaName}_events` | 事件声明 | 每行一个 EventBean JSON |
| `@{javaName}_{targetId}_{eventName}` | 积木块逻辑 | 每行一个 BlockBean JSON |

> **⚠️ 重要**: `initializeLogic` 事件的积木块段名必须是 `@{javaName}_onCreate_initializeLogic`（targetId 为 `onCreate`），
> 而非 `@{javaName}_initializeLogic`。代码生成器通过 `getBlocks(javaName, "onCreate_initializeLogic")` 查找该段。
>
> **⚠️ 不要在 `_events` 节中声明 `initializeLogic` 事件**。`EventListFragment` 会自动创建该事件（`new EventBean(3, -1, "onCreate", "initializeLogic")`）。
> 如果在 `_events` 节中重复声明，UI 中会出现两个 "onCreate" 事件。只需提供 `@{javaName}_onCreate_initializeLogic` 积木块段即可。

### logic 文件完整节顺序

多 Activity 项目中，所有 Activity 的同类型节**按 Activity 分组排列**，而非按节类型排列。完整顺序如下：

```
@MainActivity.java_var        ← 第1个Activity的变量
1:count
@SecondActivity.java_var      ← 第2个Activity的变量
2:title
@MainActivity.java_list       ← 第1个Activity的列表（所有Activity的_list紧跟在所有_var之后）
3:notes
@SecondActivity.java_list
@MainActivity.java_func       ← 第1个Activity的MoreBlock声明
refreshList:refreshList
@SecondActivity.java_func
@MainActivity.java_components
{"componentId":"sp","param1":"","param2":"","param3":"","type":2}
@SecondActivity.java_components
@MainActivity.java_events
{"eventName":"onClick","eventType":1,"targetId":"btn1","targetType":3}
@SecondActivity.java_events
@MainActivity.java_onCreate_initializeLogic   ← 积木块段
...blocks...
@MainActivity.java_btn1_onClick
...blocks...
@MainActivity.java_refreshList_moreBlock      ← MoreBlock 实现
...blocks...
@SecondActivity.java_onCreate_initializeLogic
...blocks...
```

> **关键**: 同类型的节（所有 `_var`、所有 `_list`、所有 `_func` 等）按 Activity 顺序连续排列，然后才是下一类型。积木块段在所有声明节之后。
>
> **节类型顺序**: `_var` → `_list` → `_func` → `_components` → `_events` → 积木块段。
> 每个节类型内，Activity 按 `file` 文件中的声明顺序排列（通常 `main` 在前）。
> 即使某个节内容为空，也必须包含节标题行（如空的 `@SecondActivity.java_func`）。

### 变量类型（`_var` 节）

| type | 类型 | 生成代码 |
|------|------|----------|
| 0 | boolean | `boolean name = false;` |
| 1 | int / Number | `double name = 0;` |
| 2 | String | `String name = "";` |
| 3 | Map | `HashMap<String, Object> name = new HashMap<>();` |

### 列表变量类型（`_list` 节）

| type | 类型 | 生成代码 |
|------|------|----------|
| 1 | Number List | `ArrayList<Double> name = new ArrayList<>();` |
| 2 | String List | `ArrayList<String> name = new ArrayList<>();` |
| 3 | Map List | `ArrayList<HashMap<String, Object>> name = new ArrayList<>();` |

> **注意**: 列表变量类型与普通变量类型编号含义不同。`_list` 节的 `1` 是 Number List，`3` 是 Map List。

### ComponentBean

```json
{
  "componentId": "DB",       // 组件实例ID（代码中引用）
  "param1": "test",          // 参数1（Firebase路径等）
  "param2": "",              // 参数2
  "param3": "",              // 参数3
  "type": 6                  // 组件类型（见下表）
}
```

### 组件类型 (ComponentBean.type)

| type | 组件 |
|------|------|
| 1 | Intent |
| 2 | SharedPreferences |
| 3 | Calendar |
| 4 | Vibrator |
| 5 | Timer (TimerTask) |
| 6 | **Firebase Database** |
| 7 | Dialog |
| 8 | MediaPlayer |
| 9 | SoundPool |
| 10 | ObjectAnimator |
| 11 | Gyroscope |
| 12 | **Firebase Auth** |
| 13 | InterstitialAd |
| 14 | **Firebase Storage** |
| 15 | Camera |
| 16 | FilePicker |
| 17 | RequestNetwork |
| 18 | TextToSpeech |
| 19 | SpeechToText |
| 20 | BluetoothConnect |
| 21 | LocationManager |
| 22 | RewardedVideoAd |
| 23 | ProgressDialog |
| 24 | DatePickerDialog |
| 25 | TimePickerDialog |
| 26 | Notification |
| 27 | FragmentAdapter |
| 28 | PhoneAuth |
| 30 | **Firebase Cloud Messaging** |
| 31 | Google Login |
| 36 | AsyncTask |

### EventBean

```json
{
  "eventName": "onClick",    // 事件名称
  "eventType": 1,            // 事件类型（1=View, 2=Component, 3=Activity, 4=DrawerView, 5=ETC/MoreBlock）
  "targetId": "button1",     // 目标组件ID
  "targetType": 3            // 目标组件类型（ViewBean.type 或 ComponentBean.type）
}
```

> **⚠️ Activity 事件的 targetId**: `initializeLogic` 等 Activity 事件的 `targetId` 必须设为 `"onCreate"`，`targetType` 为 `-1`。
> 示例: `{"eventName":"initializeLogic","eventType":3,"targetId":"onCreate","targetType":-1}`

### 常用事件名称

**View 事件 (eventType=1)**:

| 组件类型 | 事件名称 | 说明 |
|----------|----------|------|
| 所有可点击组件 (Clickable) | `onClick` | 点击 |
| 所有可点击组件 (Clickable) | `onLongClick` | 长按 |
| EditText | `onTextChanged` | 文本变化中 |
| EditText | `beforeTextChanged` | 文本变化前 |
| EditText | `afterTextChanged` | 文本变化后 |
| CompoundButton (CheckBox/Switch) | `onCheckedChange` | 选中状态变化 |
| SeekBar | `onProgressChanged` | 进度变化 |
| SeekBar | `onStartTrackingTouch` | 开始拖动 |
| SeekBar | `onStopTrackingTouch` | 停止拖动 |
| Spinner | `onItemSelected` | 选项选择 |
| Spinner | `onNothingSelected` | 无选择 |
| ListView | `onItemClicked` | 列表项点击 |
| ListView | `onItemLongClicked` | 列表项长按 |
| ListView | `onBindCustomView` | 绑定自定义视图 |
| WebView | `onPageStarted` | 页面开始加载 |
| WebView | `onPageFinished` | 页面加载完成 |
| CalendarView | `onDateChange` | 日期变化 |
| MapView | `onMapReady` | 地图就绪 |
| MapView | `onMarkerClicked` | 标记点击 |
| AdView | `onBannerAdLoaded` | 横幅广告加载完成 |
| AdView | `onBannerAdFailedToLoad` | 横幅广告加载失败 |
| AdView | `onBannerAdOpened` | 横幅广告打开 |
| AdView | `onBannerAdClicked` | 横幅广告点击 |
| AdView | `onBannerAdClosed` | 横幅广告关闭 |
| ListView | `onScrolled` | 列表滚动 |
| ListView | `onScrollChanged` | 滚动状态变化 |
| BottomNavigationView | `onNavigationItemSelected` | 导航项选择 |
| SearchView | `onQueryTextChanged` | 搜索文本变化 |
| SearchView | `onQueryTextSubmit` | 搜索文本提交 |
| TabLayout | `onTabSelected` | Tab选中 |
| TabLayout | `onTabReselected` | Tab重新选中 |
| TabLayout | `onTabUnselected` | Tab取消选中 |
| ViewPager | `onPageSelected` | 页面选中 |
| ViewPager | `onPageScrolled` | 页面滚动 |
| ViewPager | `onPageChanged` | 滚动状态变化 |
| RatingBar | `onRatingChanged` | 评分变化 |
| DatePicker | `onDateChanged` | 日期变化 |
| TimePicker | `onTimeChanged` | 时间变化 |
| WaveSideBar | `onLetterSelected` | 字母选中 |
| PatternLockView | `onPatternLockStarted` | 图案开始 |
| PatternLockView | `onPatternLockProgress` | 图案进行中 |
| PatternLockView | `onPatternLockComplete` | 图案完成 |
| PatternLockView | `onPatternLockCleared` | 图案清除 |
| RecyclerView | `onRecyclerScrollChanged` | 滚动状态变化 |
| RecyclerView | `onRecyclerScrolled` | 滚动偏移 |

**Component 事件 (eventType=2)**:

| 组件类型 | 事件名称 | 说明 |
|----------|----------|------|
| Firebase Database | `onChildAdded` | 子节点添加 |
| Firebase Database | `onChildChanged` | 子节点变化 |
| Firebase Database | `onChildMoved` | 子节点移动 |
| Firebase Database | `onChildRemoved` | 子节点删除 |
| Firebase Database | `onCancelled` | 操作取消 |
| Firebase Auth | `onCreateUserComplete` | 注册完成 |
| Firebase Auth | `onSignInUserComplete` | 登录完成 |
| Firebase Auth | `onResetPasswordEmailSent` | 重置密码邮件已发送 |
| Firebase Storage | `onUploadProgress` | 上传进度 |
| Firebase Storage | `onDownloadProgress` | 下载进度 |
| Firebase Storage | `onUploadSuccess` | 上传成功 |
| Firebase Storage | `onDownloadSuccess` | 下载成功 |
| Firebase Storage | `onDeleteSuccess` | 删除成功 |
| Firebase Storage | `onFailure` | 操作失败 |
| ObjectAnimator | `onAnimationStart` | 动画开始 |
| ObjectAnimator | `onAnimationEnd` | 动画结束 |
| ObjectAnimator | `onAnimationCancel` | 动画取消 |
| ObjectAnimator | `onAnimationRepeat` | 动画重复 |
| Gyroscope | `onSensorChanged` | 传感器变化 |
| Gyroscope | `onAccuracyChanged` | 精度变化 |
| InterstitialAd | `onInterstitialAdLoaded` | 插页广告加载完成 |
| InterstitialAd | `onInterstitialAdFailedToLoad` | 插页广告加载失败 |
| InterstitialAd | `onAdDismissedFullScreenContent` | 全屏广告关闭 |
| InterstitialAd | `onAdFailedToShowFullScreenContent` | 全屏广告显示失败 |
| InterstitialAd | `onAdShowedFullScreenContent` | 全屏广告显示 |
| Camera | `onPictureTaken` | 拍照完成 |
| Camera | `onPictureTakenCancel` | 拍照取消 |
| FilePicker | `onFilesPicked` | 文件选择完成 |
| FilePicker | `onFilesPickedCancel` | 文件选择取消 |
| RequestNetwork | `onResponse` | 网络响应 |
| RequestNetwork | `onErrorResponse` | 网络错误响应 |
| SpeechToText | `onSpeechResult` | 语音识别结果 |
| SpeechToText | `onSpeechError` | 语音识别错误 |
| BluetoothConnect | `onConnected` | 蓝牙已连接 |
| BluetoothConnect | `onDataReceived` | 蓝牙数据接收 |
| BluetoothConnect | `onDataSent` | 蓝牙数据发送 |
| BluetoothConnect | `onConnectionError` | 蓝牙连接错误 |
| BluetoothConnect | `onConnectionStopped` | 蓝牙连接断开 |
| LocationManager | `onLocationChanged` | 位置变化 |
| RewardedVideoAd | `onRewardAdLoaded` | 激励广告加载完成 |
| RewardedVideoAd | `onRewardAdFailedToLoad` | 激励广告加载失败 |
| RewardedVideoAd | `onUserEarnedReward` | 用户获得奖励 |
| RewardedVideoAd | `onAdDismissedFullScreenContent` | 全屏广告关闭 |
| RewardedVideoAd | `onAdFailedToShowFullScreenContent` | 全屏广告显示失败 |
| RewardedVideoAd | `onAdShowedFullScreenContent` | 全屏广告显示 |
| DatePickerDialog | `onDateSet` | 日期选择完成 |
| TimePickerDialog | `onTimeSet` | 时间选择完成 |
| FragmentAdapter | `onTabAdded` | Tab添加 |
| FragmentAdapter | `onFragmentAdded` | Fragment添加 |
| PhoneAuth | `onVerificationCompleted` | 验证完成 |
| PhoneAuth | `onVerificationFailed` | 验证失败 |
| PhoneAuth | `onCodeSent` | 验证码已发送 |
| FCM | `onCompleteRegister` | FCM注册完成 |
| GoogleLogin | `onAccountPicker` | 账号选择 |
| GoogleLogin | `onAccountPickerCancelled` | 账号选择取消 |
| AsyncTask | `onPreExecute` | 异步任务执行前 |
| AsyncTask | `doInBackground` | 后台执行 |
| AsyncTask | `onProgressUpdate` | 进度更新 |
| AsyncTask | `onPostExecute` | 异步任务执行后 |
| MediaPlayer | `onCompletion` | 播放完成 |
| MediaPlayer | `onPrepared` | 准备完成 |
| MediaPlayer | `onError` | 播放错误 |
| Firebase Auth | `onUpdateProfileComplete` | 更新个人资料完成 |
| Firebase Auth | `onUpdateEmailComplete` | 更新邮箱完成 |
| Firebase Auth | `onUpdatePasswordComplete` | 更新密码完成 |
| Firebase Auth | `onEmailVerificationSent` | 邮箱验证已发送 |
| Firebase Auth | `onDeleteUserComplete` | 删除用户完成 |

> **注意**: 上表中的 Firebase Auth 子事件（`onUpdateProfileComplete` 等）属于 Firebase Auth 组件 (type=12)，
> 不是独立的 PhoneAuth/GoogleLogin 组件事件。同样，`signInWithPhoneAuthComplete` 和 `onGoogleSignIn` 也属于 Firebase Auth 组件。

**Activity 事件 (eventType=3)**:

| 事件名称 | 说明 |
|----------|------|
| `initializeLogic` | 初始化逻辑（onCreate中执行） |
| `onBackPressed` | 按下返回键 |
| `onPostCreate` | Activity创建完成后 |
| `onStart` | Activity启动 |
| `onStop` | Activity停止 |
| `onDestroy` | Activity销毁 |
| `onResume` | Activity恢复 |
| `onPause` | Activity暂停 |
| `onSaveInstanceState` | 保存实例状态 |
| `onRestoreInstanceState` | 恢复实例状态 |
| `onCreateOptionsMenu` | 创建选项菜单 |
| `onOptionsItemSelected` | 选项菜单点击 |
| `onCreateContextMenu` | 创建上下文菜单 |
| `onContextItemSelected` | 上下文菜单点击 |

### BlockBean

```json
{
  "id": "1",                 // 唯一ID（字符串数字）
  "nextBlock": 2,            // 下一个块的ID（-1=无）
  "opCode": "setText",       // 操作码
  "spec": "%m.textview setText %s",  // 显示规格
  "parameters": ["textview1", "\"Hello\""],  // 参数列表
  "subStack1": -1,           // 子栈1（用于if/loop等，-1=无）
  "subStack2": -1,           // 子栈2（用于else等，-1=无）
  "type": " ",               // 块类型（" "=语句, "b"=布尔, "s"=字符串, "d"=数字, "c"=C形）
  "typeName": "",            // 类型名
  "color": -7584130          // 颜色（ARGB int）
}
```

### parameters 参数详解

`parameters` 数组中的每个元素按顺序对应 `spec` 中的占位符（`%s`、`%d`、`%b`、`%m.xxx`）。

#### spec 与 parameters 的对应关系

以 `spec: "%m.textview setText %s"` 为例：
- 第1个占位符 `%m.textview` → `parameters[0]` = 视图 ID（如 `"textview1"`）
- 第2个占位符 `%s` → `parameters[1]` = 字符串值

spec 中的**非占位符文本**（如 `setText`）仅用于编辑器显示，不占用 parameters 位置。

#### 参数值格式

| 值类型 | 格式 | 示例 | 说明 |
|--------|------|------|------|
| 视图/组件 ID | 直接写 ID | `"textview1"`, `"DB"` | `%m.xxx` 占位符的值 |
| 字符串字面量 | 无需转义引号 | `"Hello World"` | 代码生成时自动加引号 |
| 数字字面量 | 直接写数字 | `"100"`, `"3.14"` | 字符串形式存储 |
| 布尔字面量 | `"true"` / `"false"` | `"true"` | |
| 变量引用 | 直接写变量名 | `"count"`, `"name"` | 引用 `_var` 节中声明的变量 |
| 嵌套块引用 | `"@块ID"` | `"@5"` | 引用另一个块的返回值（`@` + BlockBean.id） |

#### 嵌套块示例

当一个块的参数需要引用另一个块的返回值时，使用 `@块ID` 格式：

```json
// 块3: setText textview1 to (getText edittext1)
{"id":"3","opCode":"setText","spec":"%m.textview setText %s",
 "parameters":["textview1","@4"],"nextBlock":-1,"subStack1":-1,"subStack2":-1,
 "type":" ","typeName":"","color":-7584130}

// 块4: getText (被块3引用)
{"id":"4","opCode":"getText","spec":"%m.edittext getText",
 "parameters":["edittext1"],"nextBlock":-1,"subStack1":-1,"subStack2":-1,
 "type":"s","typeName":"","color":-7584130}
```

执行时：块3 的 `parameters[1]` 为 `"@4"`，代码生成器会先解析块4，得到 `edittext1.getText().toString()`，然后替换到块3 中，最终生成 `textview1.setText(edittext1.getText().toString());`。

#### subStack 与 C/E 形块

对于 `if`、`forever`、`repeat`、`firebaseGetChildren` 等含子栈的块：
- `subStack1`: 第一个子栈入口块的 ID（true分支/循环体/回调体）
- `subStack2`: 第二个子栈入口块的 ID（仅 `ifElse` 的 else 分支使用）
- 子栈内的块通过 `nextBlock` 链式连接

```json
// if (count > 10) { setText textview1 "big" }
{"id":"1","opCode":"if","spec":"if %b","parameters":["@2"],
 "subStack1":3,"subStack2":-1,"nextBlock":-1,"type":"c","color":-14575885}
{"id":"2","opCode":">","spec":"%d > %d","parameters":["count","10"],
 "type":"b","color":-10701022}
{"id":"3","opCode":"setText","spec":"%m.textview setText %s",
 "parameters":["textview1","big"],"nextBlock":-1,"type":" ","color":-7584130}
```

#### 块链接规则

- **顺序执行**：通过 `nextBlock` 链接（块1 → 块2 → 块3...）
- **事件入口**：`@{javaName}_{targetId}_{eventName}` 节中第一个块是事件回调的入口
- **返回值块**：`type` 为 `"b"`/`"s"`/`"d"` 的块不能独立存在，必须被其他块通过 `@ID` 引用
- **终结块**：`type` 为 `"f"` 的块（如 `break`、`finishActivity`）的 `nextBlock` 必须为 `-1`

#### addSourceDirectly 注意事项

- `addSourceDirectly` 的 `parameters[0]` 直接嵌入生成的 Java 代码，不做任何转义或加引号
- 在 `addSourceDirectly` 中使用的类必须在编译时可用：
  - `org.json.*`（JSONArray, JSONObject）— **始终可用**（Android 内置）
  - `java.util.*`（HashMap, ArrayList, Iterator）— **始终可用**
  - `com.google.gson.Gson` — **仅当项目启用 Firebase 或其他包含 Gson 的库时可用**，仅启用 AppCompat 时 **不可用**
- 建议使用 `org.json.JSONArray` / `org.json.JSONObject` 替代 Gson 进行 JSON 序列化/反序列化

### 完整实际示例

以下是项目 logic 文件中的真实示例，展示各种常见模式：

#### 示例1：Firebase Database 写入数据

场景：点击按钮后，创建 Map 并写入 Firebase。

```
@MainActivity.java_button_write_onClick
```

```json
// 块1→2→3→4→5→6 通过 nextBlock 链式连接
// 块1: 直接代码创建 HashMap
{"id":"1","nextBlock":2,"opCode":"addSourceDirectly",
 "parameters":["java.util.HashMap<String, Object> map = new java.util.HashMap<>();"],
 "spec":"add source directly %s.inputOnly",
 "subStack1":-1,"subStack2":-1,"type":" ","color":-11899692}

// 块2: 直接代码放入 edittext 的值（需要 findViewById）
{"id":"2","nextBlock":3,"opCode":"addSourceDirectly",
 "parameters":["map.put(\"message\", ((EditText)findViewById(R.id.edittext1)).getText().toString());"],
 "spec":"add source directly %s.inputOnly",
 "subStack1":-1,"subStack2":-1,"type":" ","color":-11899692}

// 块5: Firebase 写入（组件ID=DB, child路径="data", Map变量=map）
{"id":"5","nextBlock":6,"opCode":"firebaseAdd",
 "parameters":["DB","\"data\"","map"],
 "spec":"%m.firebase add key %s value %m.varMap",
 "subStack1":-1,"subStack2":-1,"type":" ","color":-7711273}

// 块6: 设置文本显示结果（最后一个块 nextBlock=-1）
{"id":"6","nextBlock":-1,"opCode":"setText",
 "parameters":["textview1","\"Write OK!\""],
 "spec":"%m.textview setText %s",
 "subStack1":-1,"subStack2":-1,"type":" ","color":-7584130}
```

生成的 Java 代码：
```java
java.util.HashMap<String, Object> map = new java.util.HashMap<>();
map.put("message", ((EditText)findViewById(R.id.edittext1)).getText().toString());
map.put("timestamp", String.valueOf(System.currentTimeMillis()));
map.put("device", android.os.Build.MODEL);
DB.child("data").updateChildren(map);
textview1.setText("Write OK!");
```

#### 示例2：Firebase Database 读取数据（C形块 + subStack）

场景：读取所有子节点到 ListMap，回调中显示结果。

```
@MainActivity.java_button_read_onClick
```

```json
// 块7: firebaseGetChildren 是 C形块（type="c"），subStack1 指向回调体入口块8
{"id":"7","nextBlock":-1,"opCode":"firebaseGetChildren",
 "parameters":["DB","fbData"],
 "spec":"%m.firebase get children to %m.listMap then",
 "subStack1":8,"subStack2":-1,"type":"c","color":-7711273}

// 块8: 在 onDataChange 回调中执行（被 subStack1 引用）
{"id":"8","nextBlock":-1,"opCode":"setText",
 "parameters":["textview1","String.valueOf(fbData)"],
 "spec":"%m.textview setText %s",
 "subStack1":-1,"subStack2":-1,"type":" ","color":-7584130}
```

生成的 Java 代码：
```java
DB.addListenerForSingleValueEvent(new ValueEventListener() {
    @Override
    public void onDataChange(DataSnapshot _dataSnapshot) {
        fbData = new ArrayList<>();
        try {
            GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
            for (DataSnapshot _data : _dataSnapshot.getChildren()) {
                HashMap<String, Object> _map = _data.getValue(_ind);
                fbData.add(_map);
            }
        } catch (Exception _e) {
            Log.e("Fx", _e.getMessage(), _e);
        }
        textview1.setText(String.valueOf(fbData));  // ← subStack1 中的块8
    }
    @Override
    public void onCancelled(DatabaseError _databaseError) {
    }
});
```

#### 示例3：Firebase Auth 注册

```json
// parameters: [组件ID, 邮箱, 密码]
{"id":"9","nextBlock":-1,"opCode":"firebaseauthCreateUser",
 "parameters":["Fa","\"test@test.com\"","\"Test123456\""],
 "spec":"%m.firebaseauth createUserWith Email %s and Password %s",
 "subStack1":-1,"subStack2":-1,"type":" ","color":-11242015}
```

生成：`Fa.createUserWithEmailAndPassword("test@test.com", "Test123456").addOnCompleteListener(...);`

#### 示例4：事件回调中使用局部变量

`onChildAdded` 事件自动声明 `_childKey` 和 `_childValue`：

```
@MainActivity.java_DB_onChildAdded
```

```json
// 直接引用事件回调的局部变量 _childValue
{"id":"11","nextBlock":-1,"opCode":"setText",
 "parameters":["textview1","String.valueOf(_childValue)"],
 "spec":"%m.textview setText %s",
 "subStack1":-1,"subStack2":-1,"type":" ","color":-7584130}
```

#### 示例5：变量和列表声明

```
@MainActivity.java_var
0:isLoggedIn            // boolean isLoggedIn = false;
1:count                 // double count = 0;
2:userName              // String userName = "";
3:userData              // HashMap<String, Object> userData = new HashMap<>();

@MainActivity.java_list
1:scores                // ArrayList<Double> scores = new ArrayList<>();
2:names                 // ArrayList<String> names = new ArrayList<>();
3:dataList              // ArrayList<HashMap<String, Object>> dataList = new ArrayList<>();
```

#### 示例6：组件声明

```
@MainActivity.java_components
{"componentId":"DB","param1":"test","param2":"","param3":"","type":6}
{"componentId":"Fa","param1":"","param2":"","param3":"","type":12}
{"componentId":"timer1","param1":"","param2":"","param3":"","type":5}
{"componentId":"sp","param1":"","param2":"","param3":"","type":2}
{"componentId":"notif1","param1":"","param2":"","param3":"","type":26}
```

生成的字段声明：
```java
private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
private DatabaseReference DB = _firebase.getReference("test");   // param1="test"
private FirebaseAuth Fa = FirebaseAuth.getInstance();
private TimerTask timer1;
private SharedPreferences sp;
private NotificationCompat.Builder notif1;       // 双字段：Builder + Manager
private NotificationManager _nm_notif1;
```

> **⚠️ 组件变量命名规则**: 组件变量名 = `componentId`（**无** `_` 前缀）。
> 例如 `componentId:"DB"` → 变量 `DB`，`componentId:"auth"` → 变量 `auth`。
> 附属 listener 变量使用 `_` + componentId + `_listener名` 格式（如 `_DB_child_listener`、`_auth_create_user_listener`）。
> 扩展事件 listener（来自 ManageEvent）使用 componentId + `_listener名`（无前缀，如 `auth_updateEmailListener`）。
>
> **Block 参数中的组件引用**：`%m.firebase`、`%m.FirebaseAuth` 等选择器存储 raw `componentId`（如 `"DB"`、`"auth"`），
> **不加** `_` 前缀。代码生成器直接将此值插入生成代码中。

#### 示例7：Notification 通知完整流程

场景：点击按钮后创建渠道、设置标题/内容、显示通知。

```
@MainActivity.java_components
{"componentId":"notif1","param1":"","param2":"","param3":"","type":26}
```

```
@MainActivity.java_button1_onClick
```

```json
{"color":-13850718,"id":"1","nextBlock":2,"opCode":"notifCreateChannel","parameters":["notif1","\"my_channel\"","\"My Channel\"","IMPORTANCE_DEFAULT"],"spec":"%m.notification createChannel id %s name %s importance %m.notifImportance","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-13850718,"id":"2","nextBlock":3,"opCode":"notifSetTitle","parameters":["notif1","\"Hello!\""],"spec":"%m.notification setTitle %s","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-13850718,"id":"3","nextBlock":4,"opCode":"notifSetContent","parameters":["notif1","\"This is a test notification\""],"spec":"%m.notification setContent %s","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-13850718,"id":"4","nextBlock":5,"opCode":"notifSetAutoCancel","parameters":["notif1","true"],"spec":"%m.notification setAutoCancel %b","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-13850718,"id":"5","nextBlock":-1,"opCode":"notifShow","parameters":["notif1","1"],"spec":"%m.notification show id %d","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
```

生成的 Java 代码：
```java
// 字段声明（自动生成）
private NotificationCompat.Builder notif1;
private NotificationManager _nm_notif1;

// 初始化（onCreate 中自动生成）
_nm_notif1 = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
notif1 = new NotificationCompat.Builder(this, "default_channel");
notif1.setSmallIcon(R.mipmap.ic_launcher);
if (Build.VERSION.SDK_INT >= 33) {
    if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, new String[]{"android.permission.POST_NOTIFICATIONS"}, 9901);
    }
}

// onClick 回调中（由积木生成）
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    NotificationChannel _channel_notif1 = new NotificationChannel("my_channel", "My Channel", NotificationManager.IMPORTANCE_DEFAULT);
    _nm_notif1.createNotificationChannel(_channel_notif1);
}
notif1 = new NotificationCompat.Builder(getApplicationContext(), "my_channel");
notif1.setSmallIcon(R.mipmap.ic_launcher);
notif1.setContentTitle("Hello!");
notif1.setContentText("This is a test notification");
notif1.setAutoCancel(true);
_nm_notif1.notify((int)(1), notif1.build());
```

> **❗ 注意**: `notifCreateChannel` 会重建 Builder 并重设小图标，因此必须在其他 `notifSet*` 块之前调用。
> 生成代码中使用 `getApplicationContext()` 而非 `this`，因为积木代码运行在匿名内部类（如 `OnClickListener`）中。

### 完整 opCode 参考

#### 变量操作

| opCode | 说明 | 参数 | 生成代码 |
|--------|------|------|----------|
| `setVarBoolean` | 设置布尔变量 | [变量名, 值] | `var = value;` |
| `setVarInt` | 设置数字变量 | [变量名, 值] | `var = value;` |
| `setVarString` | 设置字符串变量 | [变量名, 值] | `var = value;` |
| `increaseInt` | 自增 | [变量名] | `var++;` |
| `decreaseInt` | 自减 | [变量名] | `var--;` |
| `getVar` | 获取变量 | — | `varName` |
| `getArg` | 获取事件回调参数 | — | `_argName`（见下文） |

> **getArg 详解**: `getArg` 用于在事件回调中获取参数。它的 `spec` 字段包含参数名（如 `"position"`、`"data"`），
> `type` 字段表示返回类型（`"d"`=数字、`"s"`=字符串、`"l"`=列表）。例如：
> ```json
> // 获取 onBindCustomView 中的 position（数字类型）
> {"opCode":"getArg","parameters":[],"spec":"position","type":"d"}
> // 获取 onBindCustomView 中的 data（列表类型）
> {"opCode":"getArg","parameters":[],"spec":"data","type":"l","typeName":"List Map"}
> ```
| `getResStr` | 获取资源字符串 | — | `Helper.getResString(R.string.xxx)` |

#### Map 操作

| opCode | 说明 | 参数 |
|--------|------|------|
| `mapCreateNew` | 创建新Map | [Map变量] |
| `mapPut` | 放入键值 | [Map, 键, 值] |
| `mapGet` | 获取值 | [Map, 键] |
| `mapContainKey` | 包含键 | [Map, 键] |
| `mapRemoveKey` | 移除键 | [Map, 键] |
| `mapSize` | Map大小 | [Map] |
| `mapClear` | 清空Map | [Map] |
| `mapIsEmpty` | 是否为空 | [Map] |
| `mapGetAllKeys` | 获取所有键 | [Map, List] |
| `strToMap` | JSON→Map | [JSON字符串, Map] |
| `mapToStr` | Map→JSON | [Map] |

#### List 操作

| opCode | 说明 | 参数 |
|--------|------|------|
| `addListInt` | 添加数字 | [值, List] |
| `addListStr` | 添加字符串 | [值, List] |
| `addMapToList` | 添加Map到List | [Map, List] |
| `insertListInt` | 插入数字 | [值, 索引, List] |
| `insertListStr` | 插入字符串 | [值, 索引, List] |
| `insertMapToList` | 插入Map | [Map, 索引, List] |
| `getAtListInt` | 获取数字 | [索引, List] |
| `getAtListStr` | 获取字符串 | [索引, List] |
| `getAtListMap` | 获取Map值 | [索引, 键, List] |
| `setListMap` | 设置Map值 | [键, 值, 索引, List] |
| `indexListInt` | 查找数字索引 | [值, List] |
| `indexListStr` | 查找字符串索引 | [值, List] |
| `containListInt` | 包含数字 | [List, 值] |
| `containListStr` | 包含字符串 | [List, 值] |
| `containListMap` | 包含Map键 | [List, 索引, 键] |
| `deleteList` | 删除元素 | [索引, List] |
| `lengthList` | List大小 | [List] |
| `clearList` | 清空List | [List] |
| `addListMap` | 添加键值到List | [键, 值, List] |
| `insertListMap` | 插入键值到List | [键, 值, 索引, List] |
| `getMapInList` | 获取Map | [索引, List, Map] |
| `strToListMap` | JSON→ListMap | [JSON字符串, ListMap] |
| `listMapToStr` | ListMap→JSON | [ListMap] |

#### 控制流

| opCode | 说明 | 参数 | 备注 |
|--------|------|------|------|
| `if` | 条件判断 | [条件] | subStack1=true分支 |
| `ifElse` | 条件分支 | [条件] | subStack1=true, subStack2=false |
| `forever` | 无限循环 | — | subStack1=循环体 |
| `repeat` | 重复N次 | [次数] | subStack1=循环体 |
| `break` | 跳出循环 | — | |
| `tryCatch` | 异常捕获 | — | subStack1=try体, subStack2=catch体 (type="e") |
| `getExceptionMessage` | 获取异常信息 | — | 返回 `e.getMessage()`（在 catch 中使用, type="s"） |

#### 运算符

| opCode | 说明 | 参数 |
|--------|------|------|
| `+` `-` `*` `/` `%` | 算术运算 | [左, 右] |
| `>` `<` `=` | 比较运算 | [左, 右] |
| `&&` `\|\|` | 逻辑运算 | [左, 右] |
| `not` | 逻辑非 | [值] |
| `true` `false` | 布尔值 | — |
| `random` | 随机数 | [最小, 最大] |

#### 字符串操作

| opCode | 说明 | 参数 |
|--------|------|------|
| `stringLength` | 长度 | [字符串] |
| `stringJoin` | 连接 | [字符串1, 字符串2] |
| `stringIndex` | 查找位置 | [搜索, 字符串] |
| `stringLastIndex` | 最后位置 | [搜索, 字符串] |
| `stringSub` | 截取 | [字符串, 起始, 结束] |
| `stringEquals` | 相等 | [字符串1, 字符串2] |
| `stringContains` | 包含 | [字符串1, 字符串2] |
| `stringReplace` | 替换 | [字符串, 旧值, 新值] |
| `stringReplaceFirst` | 替换首个 | [字符串, 旧值, 新值] |
| `stringReplaceAll` | 替换全部(正则) | [字符串, 正则, 新值] |
| `trim` | 去空格 | [字符串] |
| `toUpperCase` | 转大写 | [字符串] |
| `toLowerCase` | 转小写 | [字符串] |

#### 类型转换

| opCode | 说明 | 参数 |
|--------|------|------|
| `toNumber` | 字符串→数字 | [字符串] |
| `toString` | 数字→字符串(整数) | [数字] |
| `toStringWithDecimal` | 数字→字符串(小数) | [数字] |
| `toStringFormat` | 格式化数字 | [数字, 格式] |
| `currentTime` | 当前时间戳 | — |

#### 数学函数

| opCode | 说明 | 参数 |
|--------|------|------|
| `mathPi` | π | — |
| `mathE` | e | — |
| `mathPow` | 幂 | [底数, 指数] |
| `mathMin` `mathMax` | 最小/最大值 | [a, b] |
| `mathSqrt` | 平方根 | [值] |
| `mathAbs` | 绝对值 | [值] |
| `mathRound` | 四舍五入 | [值] |
| `mathCeil` `mathFloor` | 上/下取整 | [值] |
| `mathSin` `mathCos` `mathTan` | 三角函数 | [值] |
| `mathAsin` `mathAcos` `mathAtan` | 反三角函数 | [值] |
| `mathExp` `mathLog` `mathLog10` | 指数/对数 | [值] |
| `mathToRadian` `mathToDegree` | 弧度/角度转换 | [值] |
| `mathGetDip` | DP转PX | [dp值] |
| `mathGetDisplayWidth` | 屏幕宽 | — |
| `mathGetDisplayHeight` | 屏幕高 | — |

#### View 通用操作

| opCode | 说明 | 参数 |
|--------|------|------|
| `setText` | 设置文本 | [视图, 文本] |
| `getText` | 获取文本 | [视图] |
| `setHint` | 设置提示 | [视图, 提示] |
| `setHintTextColor` | 设置提示颜色 | [视图, 颜色] |
| `setVisible` | 设置可见性 | [视图, VISIBLE/GONE/INVISIBLE] |
| `setEnable` | 设置启用 | [视图, true/false] |
| `getEnable` | 获取启用状态 | [视图] |
| `setClickable` | 设置可点击 | [视图, true/false] |
| `setBgColor` | 设置背景色 | [视图, 颜色] |
| `setBgResource` | 设置背景资源 | [视图, 资源名] |
| `setTextColor` | 设置文字颜色 | [视图, 颜色] |
| `setTypeface` | 设置字体 | [视图, 字体, 样式] |
| `setImage` | 设置图片 | [视图, 资源名] |
| `setImageFilePath` | 设置图片路径 | [视图, 路径] |
| `setImageUrl` | 设置图片URL | [视图, URL] |
| `setColorFilter` | 设置颜色滤镜 | [视图, 颜色] |
| `requestFocus` | 请求焦点 | [视图] |
| `setRotate` / `getRotate` | 设置/获取旋转 | [视图, 角度] |
| `setAlpha` / `getAlpha` | 设置/获取透明度 | [视图, 值] |
| `setTranslationX/Y` | 设置平移 | [视图, 值] |
| `getTranslationX/Y` | 获取平移 | [视图] |
| `setScaleX/Y` | 设置缩放 | [视图, 值] |
| `getScaleX/Y` | 获取缩放 | [视图] |
| `getLocationX/Y` | 获取位置 | [视图] |
| `setChecked` / `getChecked` | 设置/获取选中 | [视图, true/false] |
| `viewOnClick` | 动态设置点击 | [视图] (subStack1=逻辑) |

#### ListView / Spinner / RecyclerView

| opCode | 说明 | 参数 |
|--------|------|------|
| `listSetData` | 设置简单数据 | [ListView, StringList] |
| `listSetCustomViewData` | 设置自定义适配器 | [ListView, MapList] |
| `listRefresh` | 刷新列表 | [ListView] |
| `listSmoothScrollTo` | 滚动到位置 | [ListView, 位置] |
| `listSetItemChecked` | 设置选中项 | [ListView, 位置, true/false] |
| `listGetCheckedPosition` | 获取选中位置 | [ListView] |
| `listGetCheckedPositions` | 获取多选位置 | [ListView, List] |
| `listGetCheckedCount` | 获取选中数 | [ListView] |
| `spnSetData` | Spinner设置数据 | [Spinner, StringList] |
| `spnSetCustomViewData` | Spinner自定义 | [Spinner, MapList] |
| `spnRefresh` | Spinner刷新 | [Spinner] |
| `spnSetSelection` | Spinner选择 | [Spinner, 位置] |
| `spnGetSelection` | Spinner获取选择 | [Spinner] |
| `recyclerSetCustomViewData` | RecyclerView适配器 | [RecyclerView, MapList] |
| `gridSetCustomViewData` | GridView适配器 | [GridView, MapList] |
| `pagerSetCustomViewData` | ViewPager适配器 | [ViewPager, MapList] |

#### WebView

| opCode | 说明 | 参数 |
|--------|------|------|
| `webViewLoadUrl` | 加载URL | [WebView, URL] |
| `webViewGetUrl` | 获取URL | [WebView] |
| `webViewSetCacheMode` | 设置缓存模式 | [WebView, 模式] |
| `webViewCanGoBack/Forward` | 能否返回/前进 | [WebView] |
| `webViewGoBack/Forward` | 返回/前进 | [WebView] |
| `webViewClearCache` | 清除缓存 | [WebView] |
| `webViewClearHistory` | 清除历史 | [WebView] |
| `webViewStopLoading` | 停止加载 | [WebView] |
| `webViewZoomIn/Out` | 放大/缩小 | [WebView] |

#### CalendarView

| opCode | 说明 | 参数 |
|--------|------|------|
| `calendarViewGetDate` | 获取日期 | [CalendarView] |
| `calendarViewSetDate` | 设置日期 | [CalendarView, 时间戳] |
| `calendarViewSetMinDate` | 设置最小日期 | [CalendarView, 时间戳] |
| `calnedarViewSetMaxDate` | 设置最大日期（⚠️ opCode 拼写如此，源码中的历史 typo） | [CalendarView, 时间戳] |

#### SeekBar

| opCode | 说明 | 参数 |
|--------|------|------|
| `seekBarSetProgress` | 设置进度 | [SeekBar, 值] |
| `seekBarGetProgress` | 获取进度 | [SeekBar] |
| `seekBarSetMax` | 设置最大值 | [SeekBar, 值] |
| `seekBarGetMax` | 获取最大值 | [SeekBar] |
| `setThumbResource` | 设置滑块图 | [SeekBar, 资源名] |
| `setTrackResource` | 设置轨道图 | [SeekBar, 资源名] |

#### Toast / Clipboard / Title

| opCode | 说明 | 参数 |
|--------|------|------|
| `doToast` | 显示Toast | [消息] |
| `copyToClipboard` | 复制到剪贴板 | [文本] |
| `getClipboard` | 读取剪贴板文本 | — |
| `setTitle` | 设置标题 | [标题] |
| `addSourceDirectly` | 直接插入Java代码 | [代码字符串] |

> **`addSourceDirectly` 中引用视图**: view 文件中定义的所有视图都会在 `initialize()` 方法中自动生成字段声明和 `findViewById`。
> 因此在 `addSourceDirectly` 中**可以直接使用视图 ID**（如 `edittext1.setText("...")`）。
> 只有**未在 view 文件中定义**的视图（如通过 `inject` 属性动态添加的视图）才需要手动 `((EditText)findViewById(R.id.xxx))`。

#### Intent

| opCode | 说明 | 参数 |
|--------|------|------|
| `intentSetAction` | 设置Action | [Intent, Action] |
| `intentSetData` | 设置Data | [Intent, URI] |
| `intentSetScreen` | 设置目标Activity | [Intent, Activity类名] |
| `intentPutExtra` | 放入Extra | [Intent, 键, 值] |
| `intentSetFlags` | 设置Flags | [Intent, Flag] |
| `intentGetString` | 获取StringExtra | [键] |
| `startActivity` | 启动Activity | [Intent] |
| `finishActivity` | 关闭Activity | — |

#### SharedPreferences

| opCode | 说明 | 参数 |
|--------|------|------|
| `fileSetFileName` | 设置文件名 | [SP组件, 文件名] |
| `fileGetData` | 读取数据 | [SP组件, 键] |
| `fileSetData` | 写入数据 | [SP组件, 键, 值] |
| `fileRemoveData` | 删除数据 | [SP组件, 键] |

#### Calendar

| opCode | 说明 | 参数 |
|--------|------|------|
| `calendarGetNow` | 获取当前时间 | [Calendar] |
| `calendarAdd` | 增加时间 | [Calendar, 字段, 值] |
| `calendarSet` | 设置时间 | [Calendar, 字段, 值] |
| `calendarFormat` | 格式化时间 | [Calendar, 格式] |
| `calendarDiff` | 时间差 | [Calendar1, Calendar2] |
| `calendarGetTime` | 获取毫秒 | [Calendar] |
| `calendarSetTime` | 设置毫秒 | [Calendar, 毫秒] |

#### Drawer

| opCode | 说明 |
|--------|------|
| `isDrawerOpen` | 抽屉是否打开 |
| `openDrawer` | 打开抽屉 |
| `closeDrawer` | 关闭抽屉 |

#### Timer

| opCode | 说明 | 参数 |
|--------|------|------|
| `timerAfter` | 延时执行 | [Timer, 毫秒] (subStack1=逻辑) |
| `timerEvery` | 定时执行 | [Timer, 延时, 间隔] (subStack1=逻辑) |
| `timerCancel` | 取消定时器 | [Timer] |

#### Vibrator

| opCode | 说明 | 参数 |
|--------|------|------|
| `vibratorAction` | 振动 | [Vibrator, 毫秒] |

#### Dialog

| opCode | 说明 | 参数 |
|--------|------|------|
| `dialogSetTitle` | 设置标题 | [Dialog, 标题] |
| `dialogSetMessage` | 设置消息 | [Dialog, 消息] |
| `dialogShow` | 显示对话框 | [Dialog] |
| `dialogOkButton` | 确定按钮 | [Dialog, 文本] (subStack1=点击逻辑) |
| `dialogCancelButton` | 取消按钮 | [Dialog, 文本] (subStack1=点击逻辑) |
| `dialogNeutralButton` | 中性按钮 | [Dialog, 文本] (subStack1=点击逻辑) |

#### MediaPlayer

| opCode | 说明 | 参数 |
|--------|------|------|
| `mediaplayerCreate` | 创建 | [MediaPlayer, 资源名] |
| `mediaplayerStart` | 播放 | [MediaPlayer] |
| `mediaplayerPause` | 暂停 | [MediaPlayer] |
| `mediaplayerSeek` | 跳转 | [MediaPlayer, 毫秒] |
| `mediaplayerGetCurrent` | 当前位置 | [MediaPlayer] |
| `mediaplayerGetDuration` | 总时长 | [MediaPlayer] |
| `mediaplayerReset` | 重置 | [MediaPlayer] |
| `mediaplayerRelease` | 释放 | [MediaPlayer] |
| `mediaplayerIsPlaying` | 是否播放中 | [MediaPlayer] |
| `mediaplayerSetLooping` | 设置循环 | [MediaPlayer, true/false] |
| `mediaplayerIsLooping` | 是否循环 | [MediaPlayer] |

#### SoundPool

| opCode | 说明 | 参数 |
|--------|------|------|
| `soundpoolCreate` | 创建 | [SoundPool, 最大流数] |
| `soundpoolLoad` | 加载 | [SoundPool, 资源名] |
| `soundpoolStreamPlay` | 播放 | [SoundPool, 流ID, 循环次数] |
| `soundpoolStreamStop` | 停止 | [SoundPool, 流ID] |

#### ObjectAnimator

| opCode | 说明 | 参数 |
|--------|------|------|
| `objectanimatorSetTarget` | 设置目标 | [Animator, 视图] |
| `objectanimatorSetProperty` | 设置属性 | [Animator, 属性名] |
| `objectanimatorSetValue` | 设置值 | [Animator, 值] |
| `objectanimatorSetFromTo` | 设置起止 | [Animator, 起始, 结束] |
| `objectanimatorSetDuration` | 设置时长 | [Animator, 毫秒] |
| `objectanimatorSetRepeatMode` | 设置重复模式 | [Animator, RESTART/REVERSE] |
| `objectanimatorSetRepeatCount` | 设置重复次数 | [Animator, 次数] |
| `objectanimatorSetInterpolator` | 设置插值器 | [Animator, 类型] |
| `objectanimatorStart` | 开始 | [Animator] |
| `objectanimatorCancel` | 取消 | [Animator] |
| `objectanimatorIsRunning` | 是否运行中 | [Animator] |

#### Firebase Database

| opCode | 说明 | 参数 |
|--------|------|------|
| `firebaseAdd` | 写入数据 | [组件ID, 子路径, Map变量] |
| `firebasePush` | Push数据 | [组件ID, Map变量] |
| `firebaseGetPushKey` | 获取Push Key | [组件ID] |
| `firebaseDelete` | 删除数据 | [组件ID, 子路径] |
| `firebaseGetChildren` | 读取数据(SingleValueEvent) | [组件ID, ListMap变量] (subStack1=成功逻辑) |
| `firebaseStartListen` | 开始子节点监听 | [组件ID] |
| `firebaseStopListen` | 停止子节点监听 | [组件ID] |

#### Firebase Auth

| opCode | 说明 | 参数 |
|--------|------|------|
| `firebaseauthCreateUser` | 注册 | [组件ID, 邮箱, 密码] |
| `firebaseauthSignInUser` | 登录 | [组件ID, 邮箱, 密码] |
| `firebaseauthSignInAnonymously` | 匿名登录 | [组件ID] |
| `firebaseauthSignOutUser` | 登出 | — |
| `firebaseauthIsLoggedIn` | 是否已登录 | — |
| `firebaseauthGetCurrentUser` | 获取当前用户邮箱 | — |
| `firebaseauthGetUid` | 获取当前用户UID | — |
| `firebaseauthResetPassword` | 重置密码 | [组件ID, 邮箱] |

#### Firebase Storage

| opCode | 说明 | 参数 |
|--------|------|------|
| `firebasestorageUploadFile` | 上传文件 | [组件ID, 本地路径, 远程路径] |
| `firebasestorageDownloadFile` | 下载文件 | [组件ID, 远程URL, 本地路径] |
| `firebasestorageDelete` | 删除文件 | [组件ID, 远程URL] |

#### Gyroscope

| opCode | 说明 | 参数 |
|--------|------|------|
| `gyroscopeStartListen` | 开始监听 | [组件ID] |
| `gyroscopeStopListen` | 停止监听 | [组件ID] |

#### AdMob

| opCode | 说明 | 参数 |
|--------|------|------|
| `adViewLoadAd` | 加载横幅广告 | [AdView] |
| `interstitialadCreate` | 创建插页广告 | — |
| `interstitialadLoadAd` | 加载插页广告 | — |
| `interstitialadShow` | 显示插页广告 | — |

#### Camera / FilePicker

| opCode | 说明 | 参数 |
|--------|------|------|
| `camerastarttakepicture` | 拍照 | [Camera] |
| `filepickerstartpickfiles` | 选择文件 | [FilePicker] |

#### FileUtil（文件操作）

| opCode | 说明 | 参数 |
|--------|------|------|
| `fileutilread` | 读取文件 | [路径] |
| `fileutilwrite` | 写入文件 | [内容, 路径] |
| `fileutilcopy` | 复制文件 | [源, 目标] |
| `fileutilmove` | 移动文件 | [源, 目标] |
| `fileutildelete` | 删除文件 | [路径] |
| `fileutilisexist` | 文件存在 | [路径] |
| `fileutilmakedir` | 创建目录 | [路径] |
| `fileutillistdir` | 列出目录 | [路径, List] |
| `fileutilisdir` | 是否目录 | [路径] |
| `fileutilisfile` | 是否文件 | [路径] |
| `fileutillength` | 文件大小 | [路径] |
| `fileutilStartsWith` | 路径开头 | [路径, 前缀] |
| `fileutilEndsWith` | 路径结尾 | [路径, 后缀] |
| `fileutilGetLastSegmentPath` | 最后路径段 | [路径] |
| `getExternalStorageDir` | 外部存储目录 | — |
| `getPackageDataDir` | 应用数据目录 | — |
| `getPublicDir` | 公共目录 | [类型] |

#### 图片处理

| opCode | 说明 | 参数 |
|--------|------|------|
| `resizeBitmapFileRetainRatio` | 等比缩放 | [路径, 目标, 最大尺寸] |
| `resizeBitmapFileToSquare` | 裁剪为正方形 | [路径, 目标, 尺寸] |
| `resizeBitmapFileToCircle` | 裁剪为圆形 | [路径, 目标] |
| `resizeBitmapFileWithRoundedBorder` | 圆角 | [路径, 目标, 半径] |
| `cropBitmapFileFromCenter` | 中心裁剪 | [路径, 目标, 高, 宽] |
| `rotateBitmapFile` | 旋转 | [路径, 目标, 角度] |
| `scaleBitmapFile` | 缩放 | [路径, 目标, 宽, 高] |
| `skewBitmapFile` | 倾斜 | [路径, 目标, x, y] |
| `setBitmapFileColorFilter` | 颜色滤镜 | [路径, 目标, 颜色] |
| `setBitmapFileBrightness` | 亮度 | [路径, 目标, 值] |
| `setBitmapFileContrast` | 对比度 | [路径, 目标, 值] |
| `getJpegRotate` | 获取JPEG旋转 | [路径] |

#### TextToSpeech

| opCode | 说明 | 参数 |
|--------|------|------|
| `textToSpeechSetPitch` | 设置音调 | [TTS, 值] |
| `textToSpeechSetSpeechRate` | 设置语速 | [TTS, 值] |
| `textToSpeechSpeak` | 朗读 | [TTS, 文本] |
| `textToSpeechIsSpeaking` | 是否朗读中 | [TTS] |
| `textToSpeechStop` | 停止 | [TTS] |
| `textToSpeechShutdown` | 关闭 | [TTS] |

#### SpeechToText

| opCode | 说明 | 参数 |
|--------|------|------|
| `speechToTextStartListening` | 开始识别 | [STT] |
| `speechToTextStopListening` | 停止识别 | [STT] |
| `speechToTextShutdown` | 关闭 | [STT] |

#### RequestNetwork（HTTP请求）

| opCode | 说明 | 参数 |
|--------|------|------|
| `requestnetworkSetParams` | 设置参数 | [组件, Map, 类型] |
| `requestnetworkSetHeaders` | 设置Headers | [组件, Map] |
| `requestnetworkStartRequestNetwork` | 发起请求 | [组件, 方法, URL, Tag] |

#### BluetoothConnect

| opCode | 说明 | 参数 |
|--------|------|------|
| `bluetoothConnectReadyConnection` | 准备连接 | [BT, Tag] |
| `bluetoothConnectReadyConnectionToUuid` | 准备连接(UUID) | [BT, Tag, UUID] |
| `bluetoothConnectStartConnection` | 开始连接 | [BT, Tag, 地址] |
| `bluetoothConnectStartConnectionToUuid` | 开始连接(UUID) | [BT, Tag, 地址, UUID] |
| `bluetoothConnectStopConnection` | 断开连接 | [BT, Tag] |
| `bluetoothConnectSendData` | 发送数据 | [BT, Tag, 数据] |
| `bluetoothConnectIsBluetoothEnabled` | 蓝牙是否可用 | [BT] |
| `bluetoothConnectIsBluetoothActivated` | 蓝牙是否激活 | [BT] |
| `bluetoothConnectActivateBluetooth` | 激活蓝牙 | [BT] |
| `bluetoothConnectGetPairedDevices` | 获取配对设备 | [BT, List] |
| `bluetoothConnectGetRandomUuid` | 获取随机UUID | [BT] |

#### LocationManager

| opCode | 说明 | 参数 |
|--------|------|------|
| `locationManagerRequestLocationUpdates` | 请求定位 | [LM, Provider, 间隔, 距离] |
| `locationManagerRemoveUpdates` | 停止定位 | [LM] |

#### MapView

| opCode | 说明 | 参数 |
|--------|------|------|
| `mapViewSetMapType` | 设置地图类型 | [MapView, 类型] |
| `mapViewMoveCamera` | 移动相机 | [MapView, 纬度, 经度] |
| `mapViewZoomTo` | 缩放到 | [MapView, 级别] |
| `mapViewZoomIn/Out` | 放大/缩小 | [MapView] |
| `mapViewAddMarker` | 添加标记 | [MapView, ID, 纬度, 经度] |
| `mapViewSetMarkerInfo` | 设置标记信息 | [MapView, ID, 标题, 描述] |
| `mapViewSetMarkerPosition` | 设置标记位置 | [MapView, ID, 纬度, 经度] |
| `mapViewSetMarkerColor` | 设置标记颜色 | [MapView, ID, 颜色, 大小] |
| `mapViewSetMarkerIcon` | 设置标记图标 | [MapView, ID, 资源名] |
| `mapViewSetMarkerVisible` | 设置标记可见 | [MapView, ID, true/false] |

#### ProgressBar

| opCode | 说明 | 参数 |
|--------|------|------|
| `progressBarSetIndeterminate` | 设置不确定模式 | [ProgressBar, true/false] |

#### RewardedVideoAd

| opCode | 说明 | 参数 |
|--------|------|------|
| `rewardedVideoAdLoad` | 加载激励广告 | [组件ID, Activity] |
| `rewardedVideoAdShow` | 显示激励广告 | [组件ID, Activity] |

> 这些块定义在 `ExtraBlocks.java` 中，不在 `BlockSpecRegistry.java`。

#### ProgressDialog

| opCode | 说明 | 参数 |
|--------|------|------|
| `progressdialogCreate` | 创建 | [ProgressDialog, Activity] |
| `progressdialogSetTitle` | 设置标题 | [ProgressDialog, 标题] |
| `progressdialogSetMessage` | 设置消息 | [ProgressDialog, 消息] |
| `progressdialogSetMax` | 设置最大值 | [ProgressDialog, 值] |
| `progressdialogSetProgress` | 设置进度 | [ProgressDialog, 值] |
| `progressdialogSetCancelable` | 设置可取消 | [ProgressDialog, true/false] |
| `progressdialogSetCanceledOutside` | 设置外部点击取消 | [ProgressDialog, true/false] |
| `progressdialogSetStyle` | 设置样式 | [ProgressDialog, 样式] |
| `progressdialogShow` | 显示 | [ProgressDialog] |
| `progressdialogDismiss` | 关闭 | [ProgressDialog] |

#### DatePickerDialog

| opCode | 说明 | 参数 |
|--------|------|------|
| `datePickerDialogShow` | 显示日期选择器 | — |

#### TimePickerDialog

| opCode | 说明 | 参数 |
|--------|------|------|
| `timePickerDialogShow` | 显示时间选择器 | [TimePickerDialog] |

#### AsyncTask

| opCode | 说明 | 参数 |
|--------|------|------|
| `AsyncTaskExecute` | 执行异步任务 | [AsyncTask, 消息] |
| `AsyncTaskPublishProgress` | 发布进度 | [进度值] |

#### Notification (type=26)

| opCode | 说明 | 参数 | 生成代码 |
|--------|------|------|----------|
| `notifCreateChannel` | 创建通知渠道（Android 8+）并设置 Builder 渠道 | [组件, 渠道ID, 渠道名, 重要性] | `NotificationChannel` + `NotificationCompat.Builder` 重建 |
| `notifSetChannel` | 切换通知渠道 | [组件, 渠道ID] | 重建 `NotificationCompat.Builder` |
| `notifSetTitle` | 设置通知标题 | [组件, 标题] | `notif1.setContentTitle("...");` |
| `notifSetContent` | 设置通知内容 | [组件, 内容] | `notif1.setContentText("...");` |
| `notifSetSmallIcon` | 设置通知小图标 | [组件, 图标资源名] | `notif1.setSmallIcon(R.drawable.xxx);` |
| `notifSetAutoCancel` | 设置点击后自动关闭 | [组件, true/false] | `notif1.setAutoCancel(true);` |
| `notifSetPriority` | 设置通知优先级 | [组件, 优先级] | `notif1.setPriority(NotificationCompat.xxx);` |
| `notifSetClickIntent` | 设置点击通知打开的 Intent | [组件, Intent] | `notif1.setContentIntent(PendingIntent.getActivity(...));` |
| `notifShow` | 显示通知 | [组件, 通知ID] | `_nm_notif1.notify(id, notif1.build());` |
| `notifCancel` | 取消通知 | [组件, 通知ID] | `_nm_notif1.cancel(id);` |

> **⚠️ 重要**: Notification 组件会生成**双字段**：`NotificationCompat.Builder notif1` 和 `NotificationManager _nm_notif1`。
> 初始化时自动设置 `R.mipmap.ic_launcher` 为默认小图标，并在 Android 13+ 自动请求 `POST_NOTIFICATIONS` 运行时权限。
> `notifCreateChannel` 会自动重建 Builder 以匹配创建的渠道 ID，因此**必须在 `notifSetTitle` 等设置块之前调用**。

#### FragmentAdapter (type=27)

> FragmentAdapter 组件本身没有独立的 opCode 块。它通过 ViewPager 的块来使用：

| opCode | 说明 | 参数 |
|--------|------|------|
| `pagerSetFragmentAdapter` | 设置Fragment适配器 | [ViewPager, FragmentAdapter, Tab数量] |

#### PhoneAuth (type=28) / FCM (type=30) / GoogleLogin (type=31)

> 这三个组件是**纯事件驱动**的，没有 opCode 块。它们的功能通过事件回调实现（见事件表）。
> 代码生成器会自动创建所需的监听器和回调代码。

#### 其他

| opCode | 说明 | 参数 |
|--------|------|------|
| `definedFunc` | 自定义函数调用 | — |
| `moreBlock` | More Block定义 | — |

### 完整示例

```
@MainActivity.java_var
1:result
2:message
@MainActivity.java_list
@MainActivity.java_func
@MainActivity.java_components
{"componentId":"DB","param1":"test","param2":"","param3":"","type":6}
{"componentId":"Fa","param1":"","param2":"","param3":"","type":12}
@MainActivity.java_events
{"eventName":"onClick","eventType":1,"targetId":"button1","targetType":3}
{"eventName":"onChildAdded","eventType":2,"targetId":"DB","targetType":6}
@MainActivity.java_button1_onClick
{"color":-7584130,"id":"1","nextBlock":-1,"opCode":"setText","parameters":["textview1","\"Hello World\""],"spec":"%m.textview setText %s","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
@MainActivity.java_DB_onChildAdded
{"color":-7584130,"id":"2","nextBlock":-1,"opCode":"setText","parameters":["textview1","_value"],"spec":"%m.textview setText %s","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
```

---

## library 文件

存储库配置（Firebase、AdMob、AppCompat、Google Map）。

### 节格式

| 节名 | 说明 |
|------|------|
| `@firebaseDB` | Firebase 配置 |
| `@compat` | AppCompat 配置 |
| `@admob` | AdMob 配置 |
| `@googleMap` | Google Map 配置 |

### ProjectLibraryBean

```json
{
  "useYn": "Y",
  "libType": 0,
  "data": "",
  "reserved1": "",
  "reserved2": "",
  "reserved3": "",
  "appId": "",
  "adUnits": [],
  "testDevices": [],
  "configurations": {}
}
```

#### 通用字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `useYn` | String | `"Y"` 启用, `"N"` 禁用 |
| `libType` | int | 库类型：0=Firebase, 1=AppCompat, 2=AdMob, 3=GoogleMap, 4=LocalLib, 5=NativeLib, 6=ExcludeBuiltin, 7=Material3。**注意**：此字段为冗余字段，加载时由节名（section key）强制覆盖，不以 JSON 中的值为准。 |
| `adUnits` | Array | 广告单元列表（仅 AdMob 使用） |
| `testDevices` | Array | 测试设备列表（仅 AdMob 使用） |
| `configurations` | Object | 附加配置（HashMap），通常为空 `{}` |

#### 各库类型的字段含义

| 字段 | Firebase (libType=0) | AdMob (libType=2) | GoogleMap (libType=3) | AppCompat (libType=1) |
|------|---------------------|-------------------|----------------------|----------------------|
| `data` | Realtime DB URL（如 `xxx.firebasedatabase.app`） | — | Google Maps API Key | — |
| `reserved1` | App ID（`mobilesdk_app_id`） | Banner 广告单元ID† | — | — |
| `reserved2` | API Key（`current_key`） | Interstitial 广告单元ID† | — | — |
| `reserved3` | Storage Bucket URL | Reward 广告单元ID† | — | — |
| `appId` | — | AdMob 应用ID（写入 AndroidManifest） | — | — |

> † AdMob 的 `reserved1`-`reserved3` 存储格式为 `"名称 : 单元ID"`，代码生成器通过 `substring(lastIndexOf(" : ") + 3)` 提取实际 ID。
>
> Firebase 的 `data`/`reserved1`-`reserved3` 对应 `google-services.json` 中的 `firebase_url`、`mobilesdk_app_id`、`current_key`、`storage_bucket` 字段。

### 最小 library 文件（仅启用 AppCompat）

大多数项目只需要启用 AppCompat。以下是最小 library 文件：

```
@firebaseDB
{"adUnits":[],"appId":"","configurations":{},"data":"","libType":0,"reserved1":"","reserved2":"","reserved3":"","testDevices":[],"useYn":"N"}
@compat
{"adUnits":[],"appId":"","configurations":{},"data":"","libType":1,"reserved1":"","reserved2":"","reserved3":"","testDevices":[],"useYn":"Y"}
@admob
{"adUnits":[],"appId":"","configurations":{},"data":"","libType":2,"reserved1":"","reserved2":"","reserved3":"","testDevices":[],"useYn":"N"}
@googleMap
{"adUnits":[],"appId":"","configurations":{},"data":"","libType":3,"reserved1":"","reserved2":"","reserved3":"","testDevices":[],"useYn":"N"}
```

> **注意**: 即使不使用某个库，也必须包含其节（`useYn:"N"`）。四个节必须全部存在。

---

## file 文件

存储 Activity 和自定义 View 列表。

### 节格式

```
@activity
{"fileName":"main","fileType":0,"keyboardSetting":0,"options":1,"orientation":0,"theme":-1}
@customview
```

### ProjectFileBean 字段

| 字段 | 说明 |
|------|------|
| `fileName` | 文件名（不含扩展名，如 `main`） |
| `fileType` | 文件类型（见下表） |
| `keyboardSetting` | 键盘设置（0=未指定, 1=可见, 2=隐藏） |
| `options` | Activity 选项位标志（见下表） |
| `orientation` | 屏幕方向（0=竖屏, 1=横屏, 2=双向） |
| `theme` | 主题（-1=无/已迁移, 0=默认, 1=无ActionBar, 2=全屏） |

### fileType 值

| 值 | 类型 |
|----|------|
| 0 | Activity |
| 1 | Custom View |
| 2 | Drawer |
| 3 | Fragment |
| 4 | Sheet |
| 5 | DialogFragment |

### options 位标志

`options` 字段使用位掩码（可组合）：

| 值 | 标志 | 说明 |
|----|------|------|
| 1 | `OPTION_ACTIVITY_TOOLBAR` | 显示 Toolbar |
| 2 | `OPTION_ACTIVITY_FULLSCREEN` | 全屏模式 |
| 4 | `OPTION_ACTIVITY_DRAWER` | 含抽屉菜单 |
| 8 | `OPTION_ACTIVITY_FAB` | 含 FAB 按钮 |

> 示例：`options: 5` = Toolbar(1) + Drawer(4)

---

## project 文件

存储项目元数据，位于 `/storage/emulated/0/.sketchware/mysc/list/{projectId}/project`。

### 格式

单行 JSON 对象（不使用分节格式）：

```json
{
  "sc_id": "601",
  "my_app_name": "MyApp",
  "my_sc_pkg_name": "com.my.newproject",
  "my_ws_name": "NewProject",
  "sc_ver_code": "1",
  "sc_ver_name": "1.0",
  "sketchware_ver": 150,
  "my_sc_reg_dt": "20260226201925",
  "color_accent": -10455380,
  "color_primary": -10455380,
  "color_primary_dark": -10455380,
  "color_control_highlight": -2497793,
  "color_control_normal": -10455380,
  "custom_icon": false,
  "isIconAdaptive": false
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `sc_id` | String | 项目 ID（如 `"601"`），对应文件夹名 |
| `my_app_name` | String | 应用显示名称 |
| `my_sc_pkg_name` | String | 应用包名（如 `"com.my.newproject"`） |
| `my_ws_name` | String | 工作区名称（项目内部名称） |
| `sc_ver_code` | String | 版本号（注意是字符串不是整数） |
| `sc_ver_name` | String | 版本名（如 `"1.0"`） |
| `sketchware_ver` | int | 创建此项目的 Sketchware-Pro 版本号 |
| `my_sc_reg_dt` | String | 创建时间，格式 `yyyyMMddHHmmss` |
| `color_accent` | int | 主题强调色（ARGB 有符号 int） |
| `color_primary` | int | 主题主色 |
| `color_primary_dark` | int | 主题暗主色 |
| `color_control_highlight` | int | 控件高亮色 |
| `color_control_normal` | int | 控件普通色 |
| `custom_icon` | boolean | 是否使用自定义图标（图标文件在 `mysc/icon/{sc_id}/icon.png`） |
| `isIconAdaptive` | boolean | 是否使用自适应图标（Android 8.0+） |

> **注意**:
> - `project` 文件**不加密**，与 `data/` 下的其他文件不同。
> - 颜色值为 ARGB 有符号整数，如 `-10455380` = `#FF5FA9CC`。
> - `sc_ver_code` 虽然是版本号但存储为字符串类型。
> - 代码中还存在 `proj_type` 字段（`1` 表示正常项目），用于按包名查找项目时过滤。

---

## resource 文件

存储项目引用的资源列表（图片、声音、字体）。

### 节格式

```
@images
image1.png
image2.jpg
@sounds
click.mp3
@fonts
custom_font
```

每个节下列出资源文件名（不含路径），资源文件实际存储在：
- 图片: `/storage/emulated/0/.sketchware/resources/images/{projectId}/`
- 声音: `/storage/emulated/0/.sketchware/resources/sounds/{projectId}/`
- 字体: `/storage/emulated/0/.sketchware/resources/fonts/{projectId}/`

---

## Block spec 格式

积木块的 `spec` 字段定义了积木块在编辑器中的显示方式和参数位置。

### 参数占位符

| 占位符 | 说明 | 参数类型 |
|--------|------|----------|
| `%s` | 字符串输入 | String |
| `%d` | 数字输入 | double |
| `%b` | 布尔输入 | boolean |
| `%m` | 下拉选择器（无类型限定） | — |
| `%s.inputOnly` | 仅输入（不可拖入其他块） | String |
| `%s.url` | URL 输入（提供 URL 键盘提示） | String |
| `%s.intentData` | Intent Data 输入 | String |

**视图选择器**（均为 `%m.xxx` 格式，选择对应类型的视图）：

| 占位符 | 视图类型 |
|--------|----------|
| `%m.view` | 所有视图 |
| `%m.layout` | Layout (LinearLayout/RelativeLayout等) |
| `%m.textview` | TextView/EditText/Button |
| `%m.button` | Button |
| `%m.edittext` | EditText |
| `%m.imageview` | ImageView |
| `%m.recyclerview` | RecyclerView |
| `%m.listview` | ListView |
| `%m.gridview` | GridView |
| `%m.cardview` | CardView |
| `%m.viewpager` | ViewPager |
| `%m.webview` | WebView |
| `%m.videoview` | VideoView |
| `%m.progressbar` | ProgressBar |
| `%m.seekbar` | SeekBar |
| `%m.switch` | Switch |
| `%m.checkbox` | CheckBox |
| `%m.spinner` | Spinner |
| `%m.tablayout` | TabLayout |
| `%m.bottomnavigation` | BottomNavigationView |
| `%m.adview` | AdView |
| `%m.swiperefreshlayout` | SwipeRefreshLayout |
| `%m.textinputlayout` | TextInputLayout |
| `%m.ratingbar` | RatingBar |
| `%m.datepicker` | DatePicker |
| `%m.otpview` | OTPView |
| `%m.lottie` | LottieAnimationView |
| `%m.badgeview` | BadgeView |
| `%m.codeview` | CodeView |
| `%m.patternview` | PatternLockView |
| `%m.signinbutton` | SignInButton |
| `%m.calendarview` | CalendarView |
| `%m.mapview` | MapView |
| `%m.youtubeview` | YoutubePlayerView |

**组件选择器**：

| 占位符 | 组件类型 |
|--------|----------|
| `%m.intent` | Intent |
| `%m.file` | SharedPreferences |
| `%m.calendar` | Calendar |
| `%m.vibrator` | Vibrator |
| `%m.timer` | Timer |
| `%m.dialog` | Dialog |
| `%m.mediaplayer` | MediaPlayer |
| `%m.soundpool` | SoundPool |
| `%m.objectanimator` | ObjectAnimator |
| `%m.firebase` | Firebase DB |
| `%m.firebaseauth` | Firebase Auth |
| `%m.firebasestorage` | Firebase Storage |
| `%m.gyroscope` | Gyroscope |
| `%m.interstitialad` | InterstitialAd |
| `%m.camera` | Camera |
| `%m.filepicker` | FilePicker |
| `%m.requestnetwork` | RequestNetwork |
| `%m.texttospeech` | TextToSpeech |
| `%m.speechtotext` | SpeechToText |
| `%m.bluetoothconnect` | BluetoothConnect |
| `%m.locationmanager` | LocationManager |
| `%m.phoneauth` | PhoneAuth |
| `%m.cloudmessage` | Firebase Cloud Messaging |
| `%m.googlelogin` | Google Login |
| `%m.asynctask` | AsyncTask |
| `%m.fragmentAdapter` | FragmentAdapter |
| `%m.videoad` | RewardedVideoAd |
| `%m.progressdialog` | ProgressDialog |
| `%m.timepickerdialog` | TimePickerDialog |
| `%m.notification` | Notification |

**变量选择器**：

| 占位符 | 变量类型 |
|--------|----------|
| `%m.varBool` | Boolean 变量 |
| `%m.varInt` | Number (double) 变量 |
| `%m.varStr` | String 变量 |
| `%m.varMap` | Map 变量 |
| `%m.listInt` | Number List 变量 |
| `%m.listStr` | String List 变量 |
| `%m.listMap` | Map List 变量 |
| `%m.list` | 所有 List 变量 |
| `%m.color` | 颜色选择器 |
| `%m.resource` | 图片资源 |
| `%m.resource_bg` | 背景图片资源 |
| `%m.activity` | Activity 选择器 |
| `%m.customViews` | 自定义 View 选择器 |

**枚举/下拉选择器**（这些选择器在编辑器中显示为固定选项列表，`parameters` 中直接填选项值字符串）：

| 占位符 | 说明 | 有效值 |
|--------|------|--------|
| `%m.visible` | 可见性 | `VISIBLE`, `INVISIBLE`, `GONE` |
| `%m.font` | 字体 | `default_font`, `sans_serif`, `serif`, `monospace`, 或自定义字体名 |
| `%m.typeface` | 字体样式 | `0`(Normal), `1`(Bold), `2`(Italic), `3`(BoldItalic) |
| `%m.sound` | 声音资源 | resource 文件 `@sounds` 节中声明的文件名 |
| `%m.intentAction` | Intent Action | `android.intent.action.VIEW`, `SEND`, `CALL` 等 |
| `%m.intentFlags` | Intent Flags | `FLAG_ACTIVITY_NEW_TASK`, `CLEAR_TOP`, `CLEAR_TASK` 等 |
| `%m.calendarField` | Calendar 字段 | `YEAR`, `MONTH`, `DAY_OF_MONTH`, `HOUR_OF_DAY`, `MINUTE`, `SECOND` |
| `%m.cacheMode` | WebView 缓存模式 | `LOAD_DEFAULT`, `LOAD_CACHE_ELSE_NETWORK`, `LOAD_NO_CACHE`, `LOAD_CACHE_ONLY` |
| `%m.animatorproperty` | 动画属性 | `translationX`, `translationY`, `scaleX`, `scaleY`, `rotation`, `alpha` |
| `%m.aniRepeatMode` | 动画重复模式 | `RESTART`, `REVERSE` |
| `%m.aniInterpolator` | 动画插值器 | `LINEAR`, `ACCELERATE`, `DECELERATE`, `BOUNCE` 等 |
| `%m.requestType` | 请求参数类型 | `REQUEST_PARAM`, `REQUEST_BODY` |
| `%m.method` | HTTP 方法 | `GET`, `POST`, `PUT`, `DELETE` |
| `%m.providerType` | 定位类型 | `GPS_PROVIDER`, `NETWORK_PROVIDER` |
| `%m.mapType` | 地图类型 | `MAP_TYPE_NORMAL`, `MAP_TYPE_SATELLITE`, `MAP_TYPE_TERRAIN`, `MAP_TYPE_HYBRID` |
| `%m.markerColor` | 标记颜色 | 颜色名称（如 `RED`, `BLUE`, `GREEN` 等） |
| `%m.directoryType` | 公共目录类型 | `DIRECTORY_MUSIC`, `DIRECTORY_PICTURES`, `DIRECTORY_DOWNLOADS` 等 |
| `%m.styleprogress` | 进度条样式 | `STYLE_HORIZONTAL`, `STYLE_SPINNER` |
| `%m.notifImportance` | 通知渠道重要性 | `IMPORTANCE_DEFAULT`, `IMPORTANCE_HIGH`, `IMPORTANCE_LOW`, `IMPORTANCE_MIN`, `IMPORTANCE_NONE` |
| `%m.notifPriority` | 通知优先级 | `PRIORITY_DEFAULT`, `PRIORITY_HIGH`, `PRIORITY_LOW`, `PRIORITY_MIN`, `PRIORITY_MAX` |

> 这些枚举选择器的详细值列表参见下方「属性值速查表」各小节。

### 示例

```
setText %m.textview %s          → setText [视图选择] [字符串输入]
%m.firebase add key %s value %m.varMap
                                → [Firebase组件选择] add key [路径] value [Map选择]
if %b                          → if [布尔输入]  (subStack1=true分支)
```

---

## Block type 和 color

### Block type 字段

| type值 | 说明 | 形状 |
|--------|------|------|
| `" "` (空格) | 语句块 | 矩形（执行操作，无返回值） |
| `"b"` | 布尔返回块 | 六角形（返回 true/false） |
| `"s"` | 字符串返回块 | 圆角矩形（返回字符串） |
| `"d"` | 数字返回块 | 圆角矩形（返回数字） |
| `"c"` | C形块 | 含子栈的块（如 if/loop/firebaseGetChildren） |
| `"e"` | E形块 | 含两个子栈的块（如 ifElse） |
| `"f"` | 终结块 | 底部无连接（如 break/finish） |

### Block color 约定

颜色值为 ARGB int。常用分类颜色：

| 颜色值 | 十六进制 | 分类 |
|--------|----------|------|
| `-7584130` | `#FF8C6BBE` | View 操作（紫色） |
| `-7711273` | `#FF8A55D7` | Firebase Database（深紫） |
| `-11242015` | `#FF5445E1` | Firebase Auth（蓝紫） |
| `-11899692` | `#FF4A86D4` | 直接代码 / addSourceDirectly（蓝色） |
| `-14575885` | `#FF21A083` | 控制流（绿色） |
| `-13850718` | `#FF2CA5E2` | 组件操作（蓝色，含 Notification） |
| `-10701022` | `#FF5CB722` | 运算符（黄绿） |

---

## Listener→Event 映射

Listener 是代码生成时使用的内部名称，每个 Listener 对应一个或多个事件：

| Listener 名称 | 事件名称 |
|---------------|----------|
| `onClickListener` | `onClick` |
| `onLongClickListener` | `onLongClick` |
| `onTextChangedListener` | `onTextChanged`, `beforeTextChanged`, `afterTextChanged` |
| `onCheckChangedListener` | `onCheckedChange` |
| `onSeekBarChangeListener` | `onProgressChanged`, `onStartTrackingTouch`, `onStopTrackingTouch` |
| `onItemSelectedListener` | `onItemSelected`, `onNothingSelected` |
| `onItemClickListener` | `onItemClicked` |
| `onItemLongClickListener` | `onItemLongClicked` |
| `webViewClient` | `onPageStarted`, `onPageFinished` |
| `onDateChangeListener` | `onDateChange` |
| `animatorListener` | `onAnimationStart`, `onAnimationEnd`, `onAnimationCancel`, `onAnimationRepeat` |
| `childEventListener` | `onChildAdded`, `onChildChanged`, `onChildMoved`, `onChildRemoved`, `onCancelled` |
| `sensorEventListener` | `onSensorChanged`, `onAccuracyChanged` |
| `authCreateUserComplete` | `onCreateUserComplete` |
| `authSignInUserComplete` | `onSignInUserComplete` |
| `authResetEmailSent` | `onResetPasswordEmailSent` |
| `interstitialAdLoadCallback` | `onInterstitialAdLoaded`, `onInterstitialAdFailedToLoad` |
| `fullScreenContentCallback` | `onAdDismissedFullScreenContent`, `onAdFailedToShowFullScreenContent`, `onAdShowedFullScreenContent` |
| `bannerAdViewListener` | `onBannerAdLoaded`, `onBannerAdFailedToLoad`, `onBannerAdOpened`, `onBannerAdClicked`, `onBannerAdClosed` |
| `onUploadProgressListener` | `onUploadProgress` |
| `onDownloadProgressListener` | `onDownloadProgress` |
| `onUploadSuccessListener` | `onUploadSuccess` |
| `onDownloadSuccessListener` | `onDownloadSuccess` |
| `onDeleteSuccessListener` | `onDeleteSuccess` |
| `onFailureListener` | `onFailure` |
| `requestListener` | `onResponse`, `onErrorResponse` |
| `recognitionListener` | `onSpeechResult`, `onSpeechError` |
| `bluetoothConnectionListener` | `onConnected`, `onDataReceived`, `onDataSent`, `onConnectionError`, `onConnectionStopped` |
| `onMapReadyCallback` | `onMapReady` |
| `onMapMarkerClickListener` | `onMarkerClicked` |
| `locationListener` | `onLocationChanged` |
| `rewardedAdLoadCallback` | `onRewardAdLoaded`, `onRewardAdFailedToLoad` |
| `onUserEarnedRewardListener` | `onUserEarnedReward` |
| `OnDateSetListener` | `onDateSet` |
| `OnTimeSetListener` | `onTimeSet` |
| `FragmentStatePagerAdapter` | `onTabAdded`, `onFragmentAdded` |
| `OnVerificationStateChangedListener` | `onVerificationCompleted`, `onVerificationFailed`, `onCodeSent` |
| `OnCompleteListenerFCM` | `onCompleteRegister` |
| `authsignInWithPhoneAuth` | `signInWithPhoneAuthComplete` |
| `googleSignInListener` | `onGoogleSignIn` |
| `AsyncTaskClass` | `onPreExecute`, `doInBackground`, `onProgressUpdate`, `onPostExecute` |
| `OnCompletionListener` | `onCompletion` |
| `OnPreparedListener` | `onPrepared` |
| `OnErrorListener` | `onError` |
| `OnScrollListener` | `onScrolled`, `onScrollChanged` |
| `OnRecyclerScrollListener` | `onRecyclerScrollChanged`, `onRecyclerScrolled` |
| `OnNavigationItemSelected` | `onNavigationItemSelected` |
| `OnQueryTextListener` | `onQueryTextChanged`, `onQueryTextSubmit` |
| `OnTabSelectedListener` | `onTabSelected`, `onTabReselected`, `onTabUnselected` |
| `OnPageChangeListener` | `onPageSelected`, `onPageScrolled`, `onPageChanged` |
| `OnRatingBarChangeListener` | `onRatingChanged` |
| `OnDateChangeListener` | `onDateChanged` |
| `OnTimeChangeListener` | `onTimeChanged` |
| `OnLetterSelectedListener` | `onLetterSelected` |
| `PatternLockViewListener` | `onPatternLockStarted`, `onPatternLockProgress`, `onPatternLockComplete`, `onPatternLockCleared` |
| `OnGridItemClickListener` | `onItemClicked`（GridView） |
| `OnGridItemLongClickListener` | `onItemLongClicked`（GridView） |
| `OnFailureListener` | `onFailureLink` |
| `authUpdateProfileComplete` | `onUpdateProfileComplete` |
| `authUpdateEmailComplete` | `onUpdateEmailComplete` |
| `authUpdatePasswordComplete` | `onUpdatePasswordComplete` |
| `authDeleteUserComplete` | `onDeleteUserComplete` |
| `authEmailVerificationSent` | `onEmailVerificationSent` |

### 事件回调中可用的局部变量

每个事件在回调中自动声明一些局部变量供积木块使用：

| 事件 | 可用变量 |
|------|----------|
| `onTextChanged` | `_charSeq` (String) |
| `onCheckedChange` | `_isChecked` (boolean) |
| `onItemSelected` / `onItemClicked` / `onItemLongClicked` | `_position` (int) |
| `onProgressChanged` | `_progressValue` (int) |
| `onDateChange` | `_year`, `_month`, `_day` (int) |
| `onPageStarted` / `onPageFinished` | `_url` (String) |
| `onChildAdded` / `onChildChanged` / `onChildRemoved` | `_childKey` (String), `_childValue` (HashMap) |
| `onCancelled` | `_errorCode` (int), `_errorMessage` (String) |
| `onSensorChanged` | `_x`, `_y`, `_z` (double) |
| `onCreateUserComplete` / `onSignInUserComplete` | `_success` (boolean), `_errorMessage` (String) |
| `onResetPasswordEmailSent` | `_success` (boolean) |
| `onUploadProgress` / `onDownloadProgress` | `_progressValue` (double, 百分比 0-100) |
| `onUploadSuccess` | `_downloadUrl` (String) |
| `onDownloadSuccess` | `_totalByteCount` (long) |
| `onDeleteSuccess` | — |
| `onFailure` | `_message` (String) |
| `onPictureTaken` | `_filePath` (String) — 通过 `onActivityResult` 获取 |
| `onFilesPicked` | `_filePath` (String) — 通过 `onActivityResult` 获取 |
| `onResponse` | `_tag` (String), `_response` (String), `_responseHeaders` (HashMap) |
| `onErrorResponse` | `_tag` (String), `_message` (String) |
| `onSpeechResult` | `_results` (ArrayList), `_result` (String, 第一个结果) |
| `onSpeechError` | `_errorMessage` (String) |
| `onConnected` | `_tag` (String), `_deviceData` (HashMap) |
| `onDataReceived` | `_tag` (String), `_data` (String) |
| `onDataSent` | `_tag` (String), `_data` (String) |
| `onConnectionError` | `_tag` (String), `_connectionState` (String), `_errorMessage` (String) |
| `onConnectionStopped` | `_tag` (String) |
| `onMapReady` | — |
| `onMarkerClicked` | `_id` (String) |
| `onLocationChanged` | `_lat` (double), `_lng` (double), `_acc` (double) |
| `onRewardAdFailedToLoad` | `_errorCode` (int), `_errorMessage` (String) |
| `onUserEarnedReward` | `_rewardAmount` (int), `_rewardType` (String) |
| `onAdFailedToShowFullScreenContent` | `_errorCode` (int), `_errorMessage` (String) |
| `onRewardAdLoaded` / `onAdDismissedFullScreenContent` / `onAdShowedFullScreenContent` | — |
| `onDateSet` | `_year` (int), `_month` (int), `_day` (int) |
| `onTimeSet` | `_hour` (int), `_minute` (int) |
| `onVerificationCompleted` | `_credential` (PhoneAuthCredential) |
| `onVerificationFailed` | `_exception` (String) |
| `onCodeSent` | `_verificationId` (String), `_token` (ForceResendingToken) |
| `onCompleteRegister` | `_success` (boolean), `_token` (String), `_errorMessage` (String) |
| `signInWithPhoneAuthComplete` | `_success` (boolean), `_errorMessage` (String) |
| `onGoogleSignIn` | `_success` (boolean), `_errorMessage` (String) |
| `onAccountPicker` / `onAccountPickerCancelled` | — |
| `doInBackground` | `_param` (String) |
| `onProgressUpdate` | `_value` (int) |
| `onPostExecute` | `_result` (String) |
| `onPreExecute` | — |
| `onBindCustomView` | `_position` (int), 各自定义视图组件引用（如 `custom_item_tv_title`） |
| `onChildMoved` | — |
| `onUpdateProfileComplete` / `onUpdateEmailComplete` / `onUpdatePasswordComplete` / `onDeleteUserComplete` / `onEmailVerificationSent` | `_success` (boolean), `_errorMessage` (String) |
| `onScrolled` | `_firstVisibleItem` (int), `_visibleItemCount` (int), `_totalItemCount` (int) |
| `onScrollChanged` | `_scrollState` (int) |
| `onNavigationItemSelected` | `_itemId` (int) |
| `onOptionsItemSelected` | `_id` (int), `_title` (String) |
| `onQueryTextChanged` / `onQueryTextSubmit` | `_charSeq` (String) |
| `onTabSelected` / `onTabReselected` / `onTabUnselected` | `_position` (int) |
| `onPageSelected` | `_position` (int) |
| `onPageScrolled` | `_position` (int), `_positionOffset` (float), `_positionOffsetPixels` (int) |
| `onRatingChanged` | `_value` (float), `_fromUser` (boolean) |
| `onLetterSelected` | `_index` (String) |
| `onDateChanged` | `_year` (int), `_month` (int), `_day` (int) |
| `onTimeChanged` | `_hour` (int), `_minute` (int) |
| `onRecyclerScrollChanged` | `_scrollState` (int) |
| `onRecyclerScrolled` | `_offsetX` (int), `_offsetY` (int) |
| `onCompletion` / `onPrepared` | — |
| `onError` | `_what` (int), `_extra` (int) |
| `onFailureLink` | `_errorMessage` (String) |

---

## ComponentBean param 详解

每种组件类型的 `param1`、`param2`、`param3` 含义：

| type | 组件 | param1 | param2 | param3 |
|------|------|--------|--------|--------|
| 1 | Intent | — | — | — |
| 2 | SharedPreferences | — | — | — |
| 3 | Calendar | — | — | — |
| 4 | Vibrator | — | — | — |
| 5 | Timer | — | — | — |
| 6 | **Firebase DB** | 数据库路径（如 `test`、`users/profile`） | — | — |
| 7 | Dialog | — | — | — |
| 8 | MediaPlayer | — | — | — |
| 9 | SoundPool | — | — | — |
| 10 | ObjectAnimator | — | — | — |
| 11 | Gyroscope | — | — | — |
| 12 | **Firebase Auth** | — | — | — |
| 13 | InterstitialAd | 广告单元ID | — | — |
| 14 | **Firebase Storage** | Storage路径（如 `uploads/images`） | — | — |
| 15 | Camera | — | — | — |
| 16 | FilePicker | MIME类型（如 `image/*`） | — | — |
| 17 | RequestNetwork | — | — | — |
| 18 | TextToSpeech | — | — | — |
| 19 | SpeechToText | — | — | — |
| 20 | BluetoothConnect | — | — | — |
| 21 | LocationManager | — | — | — |
| 22 | RewardedVideoAd | 广告单元ID | — | — |
| 23 | ProgressDialog | — | — | — |
| 24 | DatePickerDialog | — | — | — |
| 25 | TimePickerDialog | — | — | — |
| 26 | Notification | — | — | — |
| 27 | FragmentAdapter | — | — | — |
| 28 | PhoneAuth | — | — | — |
| 30 | **Firebase Cloud Messaging** | — | — | — |
| 31 | Google Login | — | — | — |
| 36 | AsyncTask | — | — | — |

> **注意**: 对于 Firebase DB（type=6），`param1` 是 `DatabaseReference` 的路径。生成代码为 `_firebase.getReference("param1")`。
> 对于 Firebase Storage（type=14），`param1` 是 `StorageReference` 的路径。生成代码为 `_firebase_storage.getReference("param1")`。

---

## 常见陷阱与易错点

> 以下是通过实际编译验证发现的关键问题。不遵守这些规则会导致编译失败、运行时崩溃或数据丢失。

### 1. 文件完整性

创建项目**必须提供全部 6 个文件**，即使某些文件内容为空：

| 文件 | 必须 | 可为空 |
|------|------|--------|
| `project` | ✅ | ❌ |
| `file` | ✅ | ❌（至少声明一个 Activity） |
| `library` | ✅ | ❌（至少包含 4 个节，可全部 `useYn:"N"`） |
| `resource` | ✅ | ⚠️（需包含 `@images` `@sounds` `@fonts` 三个空节） |
| `view` | ✅ | ⚠️（至少有 `@main.xml` 节） |
| `logic` | ✅ | ⚠️（至少有 `_var` `_list` `_func` `_components` `_events` 五个空节） |

文件必须**同时推送到 `data/{id}/` 和 `bak/{id}/` 两个目录**，否则 Sketchware 可能读取旧数据。

### 2. view 文件陷阱

#### 2.1 backgroundResource 和 backgroundResColor

```
❌ "backgroundResource": ""        → 生成 android:background="@drawable/"（编译错误）
✅ "backgroundResource": "NONE"    → 跳过该属性
✅ "backgroundResource": null      → 跳过该属性

❌ "backgroundResColor": ""        → 生成 android:background="@color/"（编译错误）
✅ "backgroundResColor": null      → 使用 backgroundColor 的 ARGB int 值
```

#### 2.2 视图层级关系

- `parent` 必须指向**已存在的视图 ID** 或 `"root"`（根 LinearLayout）
- `parentType` 必须与父视图的 `type` 一致（如父视图是 LinearLayout 则为 `0`）
- `index` 决定在父容器中的**排列顺序**（从 0 开始），同一父容器下不可重复
- 父视图必须是**容器类型**（LinearLayout=0, RelativeLayout=1, ScrollView=2/12 等）

#### 2.3 name 字段

`name` 字段无 `@Expose` 注解，Gson 不会序列化/反序列化它。解析器会在运行时自动设置 `name`，因此 JSON 中**可以省略**。包含它也无害。

#### 2.4 所有字段必须提供

ViewBean 的 **所有字段都必须存在**（不能省略）。缺少任何字段可能导致 JSON 反序列化失败。
建议直接复制完整端到端示例中的 ViewBean 作为模板，只修改必要字段。

> **例外**: `text` 对象中的 `resTextColor` 和 `resHintColor` 可省略（默认为 `null`，即不使用颜色资源覆盖）。
> 但如果提供，**不可为空字符串 `""`**，否则会生成无效的 `@color/` 引用导致编译错误。

### 3. logic 文件陷阱

#### 3.1 initializeLogic 事件（最常见错误）

```
❌ 在 _events 节中声明 initializeLogic → UI 中出现两个 onCreate
✅ 不声明，仅提供 @{javaName}_onCreate_initializeLogic 积木块段

❌ 段名: @MainActivity.java_initializeLogic → 块不会被代码生成器读取
✅ 段名: @MainActivity.java_onCreate_initializeLogic

❌ EventBean: {"targetId":"","targetType":0} → initializeLogic() 方法为空
✅ EventBean: {"targetId":"onCreate","targetType":-1}（但不要写入 _events 节）
```

#### 3.2 _func 节格式必须用冒号分隔

```
❌ @MainActivity.java_func
   updateDisplay               → 无冒号，解析器跳过此行，方法不会生成

✅ @MainActivity.java_func
   updateDisplay:updateDisplay  → name:spec 格式，正确生成 _updateDisplay() 方法
```

> 解析器 `parseMoreBlockFunctions` 检查 `line.contains(":")`，没有冒号的行会被静默忽略。

#### 3.3 Block ID 必须全局唯一

Block 的 `id` 在**同一 Activity 的所有事件段中**必须唯一。例如：

```
❌ @MainActivity.java_btn_add_onClick 中有 id:"1"
   @MainActivity.java_btn_save_onClick 中也有 id:"1"   → 冲突

✅ btn_add_onClick 使用 id:"10","11","12"...
   btn_save_onClick 使用 id:"40","41","42"...
```

#### 3.4 字符串参数的引号规则

这是最容易混淆的地方：

**标准块**（如 `setText`、`doToast`）：参数中的字符串**不需要外层引号**，代码生成器会自动加引号。

```json
// setText textview1 "Hello"
{"opCode":"setText","parameters":["textview1","Hello"]}
// 生成: textview1.setText("Hello");
```

**addSourceDirectly**：参数是**原始 Java 代码**，需要自己处理引号（用 `\"` 转义）。

```json
// 原始代码: edittext1.setText("Hello");
{"opCode":"addSourceDirectly","parameters":["edittext1.setText(\"Hello\");"]}
```

**fileSetFileName**：SharedPreferences 名称**不需要引号**。

```json
// 设置 SP 名为 "my_prefs"
{"opCode":"fileSetFileName","parameters":["sp","my_prefs"]}
// 生成: sp = getApplicationContext().getSharedPreferences("my_prefs", Activity.MODE_PRIVATE);
```

#### 3.5 变量类型是 double 不是 int

`_var` 节中类型 `1` 声明的变量生成 `double`，用作数组索引时必须强制转换：

```java
// editIndex 是 double 类型
notes.get((int)editIndex)     // ✅ 需要 (int) 转换
notes.get(editIndex)          // ❌ 编译错误: incompatible types
```

#### 3.6 SharedPreferences 必须先调用 fileSetFileName

`fileSetFileName` 块必须在任何 `fileGetData`/`fileSetData` 之前调用，否则 SP 名称为空字符串。
通常放在 `initializeLogic` 的第一个块。

#### 3.7 Event targetType 必须匹配

`EventBean.targetType` 必须与目标视图/组件的类型一致：

```json
// Button 的 ViewBean.type = 3，所以 targetType = 3
{"eventName":"onClick","eventType":1,"targetId":"btn_save","targetType":3}

// ListView 的 ViewBean.type = 9
{"eventName":"onItemClicked","eventType":1,"targetId":"listview1","targetType":9}

// Firebase DB 的 ComponentBean.type = 6
{"eventName":"onChildAdded","eventType":2,"targetId":"DB","targetType":6}
```

#### 3.8 C 形块的 nextBlock 与 subStack

```json
// if 块的执行流:
// 1. 评估条件 (@61)
// 2. 条件为真 → 执行 subStack1 (块62→63→...)
// 3. 无论条件结果，继续执行 nextBlock (块67)
{"id":"60","opCode":"if","parameters":["@61"],
 "subStack1":62,"nextBlock":67,"type":"c"}

// ❌ 常见错误: 把 nextBlock 设为 subStack 内的块
// ❌ 常见错误: subStack 内最后一个块的 nextBlock 指向 if 块之后的块
//    （subStack 内最后一个块的 nextBlock 应为 -1）
```

#### 3.9 布尔/字符串/数字返回块

返回值块（type `"b"`/`"s"`/`"d"`）：
- `nextBlock` **必须为 -1**（它们不在执行链中）
- 被其他块通过 `"@ID"` 引用
- 不能独立存在

```json
// ✅ 正确: 布尔块被 if 块引用
{"id":"61","opCode":">","parameters":["editIndex","-1"],
 "nextBlock":-1,"type":"b"}   // nextBlock 必须为 -1
{"id":"60","opCode":"if","parameters":["@61"],  // 通过 @61 引用
 "subStack1":62,"nextBlock":67,"type":"c"}
```

### 4. `%s` 参数自动包裹引号

`BlockInterpreter.resolveParam()` 对 `type==2`（即 `%s` 类型）的参数会**自动包裹双引号并转义内部 `"`**：

```java
// BlockInterpreter.java
else if (type == 2) {
    return "\"" + escapeString(param) + "\"";
}
```

因此 `%s` 参数必须存储**原始文本**，不含引号：

```
✅ "parameters":["auth","test@test.com","Test123456"]
   → 生成: createUser("test@test.com", "Test123456")

❌ "parameters":["auth","\"test@test.com\"","\"Test123456\""]
   → 生成: createUser("\"test@test.com\"", "\"Test123456\"")  ← 邮箱含引号字符
```

同理，`setText` 的 `%s` 参数、`firebaseAdd` 的 key 参数等都不应包含引号：

```
✅ "parameters":["textview1","Hello World"]     → setText("Hello World")
❌ "parameters":["textview1","\"Hello World\""]  → setText("\"Hello World\"")

✅ "parameters":["db","data","map"]              → child("data")
❌ "parameters":["db","\"data\"","map"]           → child("\"data\"")  ← key 含引号
```

> **例外**: `%s.inputOnly`（如 `addSourceDirectly`）**不会**自动包裹引号，参数值作为原始代码直接插入。
> 此类参数中的 Java 字符串引号需要在 JSON 中用 `\"` 转义（代表实际的 `"` 字符）。

### 5. Firebase 事件名映射

Firebase 组件的事件名必须使用代码生成器期望的**完整名称**。以下是核心事件名映射：

#### Firebase Auth 核心事件（ComponentCodeGenerator）

| eventName（logic 文件） | section 名 | 生成的 listener 变量 |
|--------------------------|------------|---------------------|
| `onCreateUserComplete` | `@{javaName}_{targetId}_onCreateUserComplete` | `_{targetId}_create_user_listener` |
| `onSignInUserComplete` | `@{javaName}_{targetId}_onSignInUserComplete` | `_{targetId}_sign_in_listener` |

> **⚠️ 注意**: 事件名是 `onSignInUserComplete`（含 "User"），**不是** `onSignInComplete`。

#### Firebase Auth 扩展事件（ManageEvent）

| eventName | 回调参数 |
|-----------|---------|
| `onUpdatePasswordComplete` | `%b.success %s.errorMessage` |
| `onEmailVerificationSent` | `%b.success %s.errorMessage` |
| `onDeleteUserComplete` | `%b.success %s.errorMessage` |
| `onUpdateEmailComplete` | `%b.success %s.errorMessage` |
| `onUpdateProfileComplete` | `%b.success %s.errorMessage` |
| `signInWithPhoneAuthComplete` | `%b.success %s.errorMessage` |
| `onGoogleSignIn` | `%b.success %s.errorMessage` |

#### Firebase Database 核心事件

| eventName | 触发时机 | 回调参数变量 |
|-----------|---------|------------|
| `onChildAdded` | 新增子节点 + listener 首次绑定时每个现有子节点 | `_childKey`, `_childValue` |
| `onChildChanged` | 已存在子节点被更新（`updateChildren`/`setValue`） | `_childKey`, `_childValue` |
| `onChildRemoved` | 子节点被删除 | `_childKey`, `_childValue` |

> **⚠️ `updateChildren()` 行为**: 对已存在的子节点调用 `updateChildren()` 触发 `onChildChanged`，**不是** `onChildAdded`。
> 如果需要同时处理新增和更新，必须同时注册 `onChildAdded` 和 `onChildChanged` 事件。

#### Firebase Cloud Messaging 事件

| eventName | 回调参数 | 生成的 listener 变量 |
|-----------|---------|---------------------|
| `onCompleteRegister` | `%b.success %s.token %s.errorMessage` | `{targetId}_onCompleteListener` |

### 6. ViewBinding 陷阱

#### 6.1 addSourceDirectly 中的视图引用

启用 ViewBinding（`project_config` 中 `enable_viewbinding:"true"`）后，标准块（如 `setText`、`setVisible`）会**自动**将视图引用转换为 `binding.viewId`。但 `addSourceDirectly` 块是**原始代码**，不经过此转换。

```
❌ addSourceDirectly: textview1.setText("hello");        → textview1 cannot be resolved
✅ addSourceDirectly: binding.textview1.setText("hello"); → 正确

❌ addSourceDirectly: edittext1.getText().toString();     → edittext1 cannot be resolved
✅ addSourceDirectly: binding.edittext1.getText().toString(); → 正确
```

> **注意**: 此规则也适用于组件事件回调（如 `onChildAdded`、`onCreateUserComplete`）中的 `addSourceDirectly` 块。
> 标准块（如 `setText`）在任何位置都能自动使用 `binding`，无需手动修改。

#### 6.2 google-services.json

使用 Firebase 库时，必须在设备上放置 `google-services.json`：

```
/storage/emulated/0/.sketchware/firebase/{sc_id}/google-services.json
```

此文件可从 [Firebase Console](https://console.firebase.google.com/) 下载。文件中的 `package_name` 必须与项目的 `my_sc_pkg_name` 一致。

### 7. library 文件陷阱

#### 7.1 Gson 不可用

仅启用 AppCompat (`compat.useYn:"Y"`) 时，`com.google.gson.Gson` **不在 classpath 中**。

```
✅ 使用 org.json.JSONArray / org.json.JSONObject（Android 内置）
✅ 使用 java.util.*（始终可用）
❌ 使用 com.google.gson.Gson（需要 Firebase 或其他含 Gson 的库）
```

#### 7.2 library 必须有 4 个节

即使不使用任何库，也必须包含 `@firebaseDB`、`@compat`、`@admob`、`@googleMap` 四个节，每个节下有一个 `useYn:"N"` 的 JSON。

#### 7.3 libType 由节名决定，JSON 中的值被忽略

`libType` 在加载时由 `parseLibrarySection()` 根据节名强制覆盖（`@firebaseDB`→0, `@compat`→1, `@admob`→2, `@googleMap`→3），JSON 中存储的 `libType` 值不被信任。部分旧项目的数据文件中所有条目的 `libType` 均为 0，如果直接使用会导致库管理器将所有库显示为 Firebase。

### 8. project 文件陷阱

- `sc_id` 必须与目录名一致（目录 `630/` → `"sc_id":"630"`）
- `my_sc_reg_dt` 格式为 `yyyyMMddHHmmss`（如 `"20260226223000"`）
- 颜色值是 **ARGB int**（如 `-15106817` 对应 `#FF1976D2`）

### 9. 推送文件后必须强制停止 Sketchware

```powershell
adb shell am force-stop pro.sketchware
```

如果不强制停止，Sketchware 会使用内存中的旧数据，并在退出时将旧数据覆写回文件。

---

## 完整端到端示例：笔记应用

以下是一个**经过编译验证**的完整笔记应用项目，展示所有 6 个文件如何协同工作。
功能：使用 SharedPreferences + org.json 实现笔记的增删改查和持久化。

### project 文件

```
{"sc_id":"630","my_app_name":"My Notes","my_sc_pkg_name":"com.my.noteapp","my_ws_name":"NoteApp","sc_ver_code":"1","sc_ver_name":"1.0","sketchware_ver":150,"my_sc_reg_dt":"20260226223000","color_accent":-15106817,"color_primary":-15106817,"color_primary_dark":-16050234,"color_control_highlight":-2497793,"color_control_normal":-15106817,"custom_icon":false}
```

### file 文件

```
@activity
{"fileName":"main","fileType":0,"keyboardSetting":2,"options":1,"orientation":0,"theme":-1}
@customview
```

### library 文件

```
@firebaseDB
{"useYn":"N","libType":0,"data":"","reserved1":"","reserved2":"","reserved3":"","appId":"","adUnits":[],"testDevices":[],"configurations":{}}
@compat
{"useYn":"Y","libType":1,"data":"","reserved1":"","reserved2":"","reserved3":"","appId":"","adUnits":[],"testDevices":[],"configurations":{}}
@admob
{"useYn":"N","libType":2,"data":"","reserved1":"","reserved2":"","reserved3":"","appId":"","adUnits":[],"testDevices":[],"configurations":{}}
@googleMap
{"useYn":"N","libType":3,"data":"","reserved1":"","reserved2":"","reserved3":"","appId":"","adUnits":[],"testDevices":[],"configurations":{}}
```

### resource 文件

```
@images
@sounds
@fonts
```

### view 文件

UI 结构：`panel_list`（列表面板）和 `panel_edit`（编辑面板）通过 `setVisible` 切换。

> **关键**: `backgroundResource` 必须为 `"NONE"`，`backgroundResColor` 必须为 `null`。

```
@main.xml
{"adSize":"","adUnitId":"","alpha":1.0,"checked":0,"choiceMode":0,"clickable":1,"convert":"","customView":"","dividerHeight":1,"enabled":1,"firstDayOfWeek":1,"id":"panel_list","image":{"rotate":0,"scaleType":"CENTER"},"indeterminate":"false","index":0,"inject":"","layout":{"backgroundColor":-1,"backgroundResColor":null,"backgroundResource":"NONE","borderColor":0,"gravity":0,"height":-1,"layoutGravity":0,"marginBottom":0,"marginLeft":0,"marginRight":0,"marginTop":0,"orientation":1,"paddingBottom":0,"paddingLeft":0,"paddingRight":0,"paddingTop":0,"weight":0,"weightSum":0,"width":-1},"max":100,"name":"panel_list","parent":"root","parentAttributes":{},"parentType":0,"preId":"","preIndex":0,"preParent":"","preParentType":0,"progress":0,"progressStyle":"?android:progressBarStyle","scaleX":1.0,"scaleY":1.0,"spinnerMode":1,"text":{"hint":"","hintColor":0,"imeOption":0,"inputType":1,"line":0,"singleLine":0,"text":"","textColor":-16777216,"textFont":"default_font","textSize":14,"textType":0},"translationX":0.0,"translationY":0.0,"type":0}
{"adSize":"","adUnitId":"","alpha":1.0,"checked":0,"choiceMode":0,"clickable":1,"convert":"","customView":"","dividerHeight":1,"enabled":1,"firstDayOfWeek":1,"id":"linear_header","image":{"rotate":0,"scaleType":"CENTER"},"indeterminate":"false","index":0,"inject":"","layout":{"backgroundColor":-15106817,"backgroundResColor":null,"backgroundResource":"NONE","borderColor":0,"gravity":16,"height":-2,"layoutGravity":0,"marginBottom":0,"marginLeft":0,"marginRight":0,"marginTop":0,"orientation":0,"paddingBottom":12,"paddingLeft":16,"paddingRight":16,"paddingTop":12,"weight":0,"weightSum":0,"width":-1},"max":100,"name":"linear_header","parent":"panel_list","parentAttributes":{},"parentType":0,"preId":"","preIndex":0,"preParent":"","preParentType":0,"progress":0,"progressStyle":"?android:progressBarStyle","scaleX":1.0,"scaleY":1.0,"spinnerMode":1,"text":{"hint":"","hintColor":0,"imeOption":0,"inputType":1,"line":0,"singleLine":0,"text":"","textColor":-16777216,"textFont":"default_font","textSize":14,"textType":0},"translationX":0.0,"translationY":0.0,"type":0}
{"adSize":"","adUnitId":"","alpha":1.0,"checked":0,"choiceMode":0,"clickable":1,"convert":"","customView":"","dividerHeight":1,"enabled":1,"firstDayOfWeek":1,"id":"tv_title","image":{"rotate":0,"scaleType":"CENTER"},"indeterminate":"false","index":0,"inject":"","layout":{"backgroundColor":0,"backgroundResColor":null,"backgroundResource":"NONE","borderColor":0,"gravity":0,"height":-2,"layoutGravity":0,"marginBottom":0,"marginLeft":0,"marginRight":0,"marginTop":0,"orientation":-1,"paddingBottom":0,"paddingLeft":0,"paddingRight":0,"paddingTop":0,"weight":1,"weightSum":0,"width":0},"max":100,"name":"tv_title","parent":"linear_header","parentAttributes":{},"parentType":0,"preId":"","preIndex":0,"preParent":"","preParentType":0,"progress":0,"progressStyle":"?android:progressBarStyle","scaleX":1.0,"scaleY":1.0,"spinnerMode":1,"text":{"hint":"","hintColor":0,"imeOption":0,"inputType":1,"line":0,"singleLine":0,"text":"My Notes","textColor":-1,"textFont":"default_font","textSize":22,"textType":1},"translationX":0.0,"translationY":0.0,"type":4}
{"adSize":"","adUnitId":"","alpha":1.0,"checked":0,"choiceMode":0,"clickable":1,"convert":"","customView":"","dividerHeight":1,"enabled":1,"firstDayOfWeek":1,"id":"btn_add","image":{"rotate":0,"scaleType":"CENTER"},"indeterminate":"false","index":1,"inject":"","layout":{"backgroundColor":-13388315,"backgroundResColor":null,"backgroundResource":"NONE","borderColor":0,"gravity":0,"height":-2,"layoutGravity":0,"marginBottom":0,"marginLeft":8,"marginRight":0,"marginTop":0,"orientation":-1,"paddingBottom":8,"paddingLeft":20,"paddingRight":20,"paddingTop":8,"weight":0,"weightSum":0,"width":-2},"max":100,"name":"btn_add","parent":"linear_header","parentAttributes":{},"parentType":0,"preId":"","preIndex":0,"preParent":"","preParentType":0,"progress":0,"progressStyle":"?android:progressBarStyle","scaleX":1.0,"scaleY":1.0,"spinnerMode":1,"text":{"hint":"","hintColor":0,"imeOption":0,"inputType":1,"line":0,"singleLine":0,"text":"+ New","textColor":-1,"textFont":"default_font","textSize":14,"textType":1},"translationX":0.0,"translationY":0.0,"type":3}
{"adSize":"","adUnitId":"","alpha":1.0,"checked":0,"choiceMode":0,"clickable":1,"convert":"","customView":"","dividerHeight":1,"enabled":1,"firstDayOfWeek":1,"id":"listview1","image":{"rotate":0,"scaleType":"CENTER"},"indeterminate":"false","index":1,"inject":"","layout":{"backgroundColor":-1,"backgroundResColor":null,"backgroundResource":"NONE","borderColor":0,"gravity":0,"height":0,"layoutGravity":0,"marginBottom":0,"marginLeft":0,"marginRight":0,"marginTop":0,"orientation":-1,"paddingBottom":0,"paddingLeft":0,"paddingRight":0,"paddingTop":0,"weight":1,"weightSum":0,"width":-1},"max":100,"name":"listview1","parent":"panel_list","parentAttributes":{},"parentType":0,"preId":"","preIndex":0,"preParent":"","preParentType":0,"progress":0,"progressStyle":"?android:progressBarStyle","scaleX":1.0,"scaleY":1.0,"spinnerMode":1,"text":{"hint":"","hintColor":0,"imeOption":0,"inputType":1,"line":0,"singleLine":0,"text":"","textColor":-16777216,"textFont":"default_font","textSize":14,"textType":0},"translationX":0.0,"translationY":0.0,"type":9}
{"adSize":"","adUnitId":"","alpha":1.0,"checked":0,"choiceMode":0,"clickable":1,"convert":"","customView":"","dividerHeight":1,"enabled":1,"firstDayOfWeek":1,"id":"panel_edit","image":{"rotate":0,"scaleType":"CENTER"},"indeterminate":"false","index":1,"inject":"","layout":{"backgroundColor":-657931,"backgroundResColor":null,"backgroundResource":"NONE","borderColor":0,"gravity":0,"height":-1,"layoutGravity":0,"marginBottom":0,"marginLeft":0,"marginRight":0,"marginTop":0,"orientation":1,"paddingBottom":0,"paddingLeft":0,"paddingRight":0,"paddingTop":0,"weight":0,"weightSum":0,"width":-1},"max":100,"name":"panel_edit","parent":"root","parentAttributes":{},"parentType":0,"preId":"","preIndex":0,"preParent":"","preParentType":0,"progress":0,"progressStyle":"?android:progressBarStyle","scaleX":1.0,"scaleY":1.0,"spinnerMode":1,"text":{"hint":"","hintColor":0,"imeOption":0,"inputType":1,"line":0,"singleLine":0,"text":"","textColor":-16777216,"textFont":"default_font","textSize":14,"textType":0},"translationX":0.0,"translationY":0.0,"type":0}
{"adSize":"","adUnitId":"","alpha":1.0,"checked":0,"choiceMode":0,"clickable":1,"convert":"","customView":"","dividerHeight":1,"enabled":1,"firstDayOfWeek":1,"id":"linear_edit_header","image":{"rotate":0,"scaleType":"CENTER"},"indeterminate":"false","index":0,"inject":"","layout":{"backgroundColor":-15106817,"backgroundResColor":null,"backgroundResource":"NONE","borderColor":0,"gravity":16,"height":-2,"layoutGravity":0,"marginBottom":0,"marginLeft":0,"marginRight":0,"marginTop":0,"orientation":0,"paddingBottom":12,"paddingLeft":16,"paddingRight":16,"paddingTop":12,"weight":0,"weightSum":0,"width":-1},"max":100,"name":"linear_edit_header","parent":"panel_edit","parentAttributes":{},"parentType":0,"preId":"","preIndex":0,"preParent":"","preParentType":0,"progress":0,"progressStyle":"?android:progressBarStyle","scaleX":1.0,"scaleY":1.0,"spinnerMode":1,"text":{"hint":"","hintColor":0,"imeOption":0,"inputType":1,"line":0,"singleLine":0,"text":"","textColor":-16777216,"textFont":"default_font","textSize":14,"textType":0},"translationX":0.0,"translationY":0.0,"type":0}
{"adSize":"","adUnitId":"","alpha":1.0,"checked":0,"choiceMode":0,"clickable":1,"convert":"","customView":"","dividerHeight":1,"enabled":1,"firstDayOfWeek":1,"id":"tv_edit_title","image":{"rotate":0,"scaleType":"CENTER"},"indeterminate":"false","index":0,"inject":"","layout":{"backgroundColor":0,"backgroundResColor":null,"backgroundResource":"NONE","borderColor":0,"gravity":0,"height":-2,"layoutGravity":0,"marginBottom":0,"marginLeft":0,"marginRight":0,"marginTop":0,"orientation":-1,"paddingBottom":0,"paddingLeft":0,"paddingRight":0,"paddingTop":0,"weight":1,"weightSum":0,"width":0},"max":100,"name":"tv_edit_title","parent":"linear_edit_header","parentAttributes":{},"parentType":0,"preId":"","preIndex":0,"preParent":"","preParentType":0,"progress":0,"progressStyle":"?android:progressBarStyle","scaleX":1.0,"scaleY":1.0,"spinnerMode":1,"text":{"hint":"","hintColor":0,"imeOption":0,"inputType":1,"line":0,"singleLine":0,"text":"Edit Note","textColor":-1,"textFont":"default_font","textSize":22,"textType":1},"translationX":0.0,"translationY":0.0,"type":4}
{"adSize":"","adUnitId":"","alpha":1.0,"checked":0,"choiceMode":0,"clickable":1,"convert":"","customView":"","dividerHeight":1,"enabled":1,"firstDayOfWeek":1,"id":"btn_back","image":{"rotate":0,"scaleType":"CENTER"},"indeterminate":"false","index":1,"inject":"","layout":{"backgroundColor":0,"backgroundResColor":null,"backgroundResource":"NONE","borderColor":0,"gravity":0,"height":-2,"layoutGravity":0,"marginBottom":0,"marginLeft":8,"marginRight":0,"marginTop":0,"orientation":-1,"paddingBottom":8,"paddingLeft":20,"paddingRight":20,"paddingTop":8,"weight":0,"weightSum":0,"width":-2},"max":100,"name":"btn_back","parent":"linear_edit_header","parentAttributes":{},"parentType":0,"preId":"","preIndex":0,"preParent":"","preParentType":0,"progress":0,"progressStyle":"?android:progressBarStyle","scaleX":1.0,"scaleY":1.0,"spinnerMode":1,"text":{"hint":"","hintColor":0,"imeOption":0,"inputType":1,"line":0,"singleLine":0,"text":"Back","textColor":-1,"textFont":"default_font","textSize":14,"textType":1},"translationX":0.0,"translationY":0.0,"type":3}
{"adSize":"","adUnitId":"","alpha":1.0,"checked":0,"choiceMode":0,"clickable":1,"convert":"","customView":"","dividerHeight":1,"enabled":1,"firstDayOfWeek":1,"id":"edittext_title","image":{"rotate":0,"scaleType":"CENTER"},"indeterminate":"false","index":1,"inject":"","layout":{"backgroundColor":-1,"backgroundResColor":null,"backgroundResource":"NONE","borderColor":0,"gravity":0,"height":-2,"layoutGravity":0,"marginBottom":0,"marginLeft":16,"marginRight":16,"marginTop":16,"orientation":-1,"paddingBottom":14,"paddingLeft":14,"paddingRight":14,"paddingTop":14,"weight":0,"weightSum":0,"width":-1},"max":100,"name":"edittext_title","parent":"panel_edit","parentAttributes":{},"parentType":0,"preId":"","preIndex":0,"preParent":"","preParentType":0,"progress":0,"progressStyle":"?android:progressBarStyle","scaleX":1.0,"scaleY":1.0,"spinnerMode":1,"text":{"hint":"Note Title","hintColor":-6381922,"imeOption":0,"inputType":1,"line":0,"singleLine":1,"text":"","textColor":-16777216,"textFont":"default_font","textSize":18,"textType":0},"translationX":0.0,"translationY":0.0,"type":5}
{"adSize":"","adUnitId":"","alpha":1.0,"checked":0,"choiceMode":0,"clickable":1,"convert":"","customView":"","dividerHeight":1,"enabled":1,"firstDayOfWeek":1,"id":"edittext_content","image":{"rotate":0,"scaleType":"CENTER"},"indeterminate":"false","index":2,"inject":"","layout":{"backgroundColor":-1,"backgroundResColor":null,"backgroundResource":"NONE","borderColor":0,"gravity":48,"height":0,"layoutGravity":0,"marginBottom":0,"marginLeft":16,"marginRight":16,"marginTop":8,"orientation":-1,"paddingBottom":14,"paddingLeft":14,"paddingRight":14,"paddingTop":14,"weight":1,"weightSum":0,"width":-1},"max":100,"name":"edittext_content","parent":"panel_edit","parentAttributes":{},"parentType":0,"preId":"","preIndex":0,"preParent":"","preParentType":0,"progress":0,"progressStyle":"?android:progressBarStyle","scaleX":1.0,"scaleY":1.0,"spinnerMode":1,"text":{"hint":"Write your note here...","hintColor":-6381922,"imeOption":0,"inputType":1,"line":0,"singleLine":0,"text":"","textColor":-16777216,"textFont":"default_font","textSize":16,"textType":0},"translationX":0.0,"translationY":0.0,"type":5}
{"adSize":"","adUnitId":"","alpha":1.0,"checked":0,"choiceMode":0,"clickable":1,"convert":"","customView":"","dividerHeight":1,"enabled":1,"firstDayOfWeek":1,"id":"linear_buttons","image":{"rotate":0,"scaleType":"CENTER"},"indeterminate":"false","index":3,"inject":"","layout":{"backgroundColor":0,"backgroundResColor":null,"backgroundResource":"NONE","borderColor":0,"gravity":0,"height":-2,"layoutGravity":0,"marginBottom":16,"marginLeft":16,"marginRight":16,"marginTop":8,"orientation":0,"paddingBottom":0,"paddingLeft":0,"paddingRight":0,"paddingTop":0,"weight":0,"weightSum":0,"width":-1},"max":100,"name":"linear_buttons","parent":"panel_edit","parentAttributes":{},"parentType":0,"preId":"","preIndex":0,"preParent":"","preParentType":0,"progress":0,"progressStyle":"?android:progressBarStyle","scaleX":1.0,"scaleY":1.0,"spinnerMode":1,"text":{"hint":"","hintColor":0,"imeOption":0,"inputType":1,"line":0,"singleLine":0,"text":"","textColor":-16777216,"textFont":"default_font","textSize":14,"textType":0},"translationX":0.0,"translationY":0.0,"type":0}
{"adSize":"","adUnitId":"","alpha":1.0,"checked":0,"choiceMode":0,"clickable":1,"convert":"","customView":"","dividerHeight":1,"enabled":1,"firstDayOfWeek":1,"id":"btn_save","image":{"rotate":0,"scaleType":"CENTER"},"indeterminate":"false","index":0,"inject":"","layout":{"backgroundColor":-14575885,"backgroundResColor":null,"backgroundResource":"NONE","borderColor":0,"gravity":0,"height":-2,"layoutGravity":0,"marginBottom":0,"marginLeft":0,"marginRight":4,"marginTop":0,"orientation":-1,"paddingBottom":14,"paddingLeft":8,"paddingRight":8,"paddingTop":14,"weight":1,"weightSum":0,"width":0},"max":100,"name":"btn_save","parent":"linear_buttons","parentAttributes":{},"parentType":0,"preId":"","preIndex":0,"preParent":"","preParentType":0,"progress":0,"progressStyle":"?android:progressBarStyle","scaleX":1.0,"scaleY":1.0,"spinnerMode":1,"text":{"hint":"","hintColor":0,"imeOption":0,"inputType":1,"line":0,"singleLine":0,"text":"Save","textColor":-1,"textFont":"default_font","textSize":16,"textType":1},"translationX":0.0,"translationY":0.0,"type":3}
{"adSize":"","adUnitId":"","alpha":1.0,"checked":0,"choiceMode":0,"clickable":1,"convert":"","customView":"","dividerHeight":1,"enabled":1,"firstDayOfWeek":1,"id":"btn_delete","image":{"rotate":0,"scaleType":"CENTER"},"indeterminate":"false","index":1,"inject":"","layout":{"backgroundColor":-769226,"backgroundResColor":null,"backgroundResource":"NONE","borderColor":0,"gravity":0,"height":-2,"layoutGravity":0,"marginBottom":0,"marginLeft":4,"marginRight":0,"marginTop":0,"orientation":-1,"paddingBottom":14,"paddingLeft":8,"paddingRight":8,"paddingTop":14,"weight":1,"weightSum":0,"width":0},"max":100,"name":"btn_delete","parent":"linear_buttons","parentAttributes":{},"parentType":0,"preId":"","preIndex":0,"preParent":"","preParentType":0,"progress":0,"progressStyle":"?android:progressBarStyle","scaleX":1.0,"scaleY":1.0,"spinnerMode":1,"text":{"hint":"","hintColor":0,"imeOption":0,"inputType":1,"line":0,"singleLine":0,"text":"Delete","textColor":-1,"textFont":"default_font","textSize":16,"textType":1},"translationX":0.0,"translationY":0.0,"type":3}
```

### logic 文件

> **注意**: `initializeLogic` 不在 `_events` 中声明，但积木块段名必须是 `@{javaName}_onCreate_initializeLogic`。
> 使用 `org.json` 而非 Gson（AppCompat 不含 Gson）。

```
@MainActivity.java_var
1:editIndex
@MainActivity.java_list
2:displayList
3:notes
@MainActivity.java_func
@MainActivity.java_components
{"componentId":"sp","param1":"","param2":"","param3":"","type":2}
@MainActivity.java_events
{"eventName":"onClick","eventType":1,"targetId":"btn_add","targetType":3}
{"eventName":"onClick","eventType":1,"targetId":"btn_back","targetType":3}
{"eventName":"onClick","eventType":1,"targetId":"btn_save","targetType":3}
{"eventName":"onClick","eventType":1,"targetId":"btn_delete","targetType":3}
{"eventName":"onItemClicked","eventType":1,"targetId":"listview1","targetType":9}
@MainActivity.java_onCreate_initializeLogic
{"color":-7584130,"id":"1","nextBlock":2,"opCode":"fileSetFileName","parameters":["sp","notes_app"],"spec":"%m.file setFileName %s","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-11899692,"id":"2","nextBlock":3,"opCode":"addSourceDirectly","parameters":["String _json = sp.getString(\"notes_json\", \"\");"],"spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-11899692,"id":"3","nextBlock":4,"opCode":"addSourceDirectly","parameters":["if (!_json.isEmpty()) { try { org.json.JSONArray _ja = new org.json.JSONArray(_json); for (int _i = 0; _i < _ja.length(); _i++) { org.json.JSONObject _jo = _ja.getJSONObject(_i); java.util.HashMap<String, Object> _m = new java.util.HashMap<>(); java.util.Iterator<String> _keys = _jo.keys(); while (_keys.hasNext()) { String _k = _keys.next(); _m.put(_k, _jo.getString(_k)); } notes.add(_m); } } catch(Exception e) {} }"],"spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-11899692,"id":"4","nextBlock":5,"opCode":"addSourceDirectly","parameters":["displayList = new java.util.ArrayList<>(); for (java.util.HashMap<String, Object> _n : notes) { displayList.add(_n.containsKey(\"title\") ? _n.get(\"title\").toString() : \"Untitled\"); }"],"spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-7584130,"id":"5","nextBlock":6,"opCode":"listSetData","parameters":["listview1","displayList"],"spec":"%m.listview setListViewData %m.listStr","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-7584130,"id":"6","nextBlock":7,"opCode":"setVisible","parameters":["panel_edit","GONE"],"spec":"%m.view setVisible %m.visible","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-7584130,"id":"7","nextBlock":-1,"opCode":"addSourceDirectly","parameters":["editIndex = -1;"],"spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
@MainActivity.java_btn_add_onClick
{"color":-11899692,"id":"10","nextBlock":11,"opCode":"addSourceDirectly","parameters":["editIndex = -1;"],"spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-11899692,"id":"11","nextBlock":12,"opCode":"addSourceDirectly","parameters":["edittext_title.setText(\"\");"],"spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-11899692,"id":"12","nextBlock":13,"opCode":"addSourceDirectly","parameters":["edittext_content.setText(\"\");"],"spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-11899692,"id":"13","nextBlock":14,"opCode":"addSourceDirectly","parameters":["tv_edit_title.setText(\"New Note\");"],"spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-7584130,"id":"14","nextBlock":15,"opCode":"setVisible","parameters":["panel_list","GONE"],"spec":"%m.view setVisible %m.visible","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-7584130,"id":"15","nextBlock":-1,"opCode":"setVisible","parameters":["panel_edit","VISIBLE"],"spec":"%m.view setVisible %m.visible","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
@MainActivity.java_btn_back_onClick
{"color":-7584130,"id":"20","nextBlock":21,"opCode":"setVisible","parameters":["panel_edit","GONE"],"spec":"%m.view setVisible %m.visible","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-7584130,"id":"21","nextBlock":-1,"opCode":"setVisible","parameters":["panel_list","VISIBLE"],"spec":"%m.view setVisible %m.visible","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
@MainActivity.java_btn_save_onClick
{"color":-11899692,"id":"40","nextBlock":41,"opCode":"addSourceDirectly","parameters":["java.util.HashMap<String, Object> _note = new java.util.HashMap<>();"],"spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-11899692,"id":"41","nextBlock":42,"opCode":"addSourceDirectly","parameters":["_note.put(\"title\", edittext_title.getText().toString());"],"spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-11899692,"id":"42","nextBlock":43,"opCode":"addSourceDirectly","parameters":["_note.put(\"content\", edittext_content.getText().toString());"],"spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-11899692,"id":"43","nextBlock":44,"opCode":"addSourceDirectly","parameters":["_note.put(\"time\", String.valueOf(System.currentTimeMillis()));"],"spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-11899692,"id":"44","nextBlock":45,"opCode":"addSourceDirectly","parameters":["if ((int)editIndex >= 0 && (int)editIndex < notes.size()) { notes.set((int)editIndex, _note); } else { notes.add(0, _note); }"],"spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-11899692,"id":"45","nextBlock":46,"opCode":"addSourceDirectly","parameters":["org.json.JSONArray _ja = new org.json.JSONArray(); for (java.util.HashMap<String, Object> _m : notes) { _ja.put(new org.json.JSONObject(_m)); } sp.edit().putString(\"notes_json\", _ja.toString()).apply();"],"spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-11899692,"id":"46","nextBlock":47,"opCode":"addSourceDirectly","parameters":["displayList = new java.util.ArrayList<>(); for (java.util.HashMap<String, Object> _n : notes) { displayList.add(_n.containsKey(\"title\") ? _n.get(\"title\").toString() : \"Untitled\"); }"],"spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-7584130,"id":"47","nextBlock":48,"opCode":"listSetData","parameters":["listview1","displayList"],"spec":"%m.listview setListViewData %m.listStr","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-7584130,"id":"48","nextBlock":49,"opCode":"setVisible","parameters":["panel_edit","GONE"],"spec":"%m.view setVisible %m.visible","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-7584130,"id":"49","nextBlock":50,"opCode":"setVisible","parameters":["panel_list","VISIBLE"],"spec":"%m.view setVisible %m.visible","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-7584130,"id":"50","nextBlock":-1,"opCode":"doToast","parameters":["Note saved!"],"spec":"Toast %s","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
@MainActivity.java_btn_delete_onClick
{"color":-14575885,"id":"60","nextBlock":67,"opCode":"if","parameters":["@61"],"spec":"if %b","subStack1":62,"subStack2":-1,"type":"c","typeName":""}
{"color":-10701022,"id":"61","nextBlock":-1,"opCode":">","parameters":["editIndex","-1"],"spec":"%d > %d","subStack1":-1,"subStack2":-1,"type":"b","typeName":""}
{"color":-11899692,"id":"62","nextBlock":63,"opCode":"addSourceDirectly","parameters":["notes.remove((int)editIndex);"],"spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-11899692,"id":"63","nextBlock":64,"opCode":"addSourceDirectly","parameters":["org.json.JSONArray _ja = new org.json.JSONArray(); for (java.util.HashMap<String, Object> _m : notes) { _ja.put(new org.json.JSONObject(_m)); } sp.edit().putString(\"notes_json\", _ja.toString()).apply();"],"spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-11899692,"id":"64","nextBlock":65,"opCode":"addSourceDirectly","parameters":["displayList = new java.util.ArrayList<>(); for (java.util.HashMap<String, Object> _n : notes) { displayList.add(_n.containsKey(\"title\") ? _n.get(\"title\").toString() : \"Untitled\"); }"],"spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-7584130,"id":"65","nextBlock":66,"opCode":"listSetData","parameters":["listview1","displayList"],"spec":"%m.listview setListViewData %m.listStr","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-7584130,"id":"66","nextBlock":-1,"opCode":"doToast","parameters":["Note deleted!"],"spec":"Toast %s","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-7584130,"id":"67","nextBlock":68,"opCode":"setVisible","parameters":["panel_edit","GONE"],"spec":"%m.view setVisible %m.visible","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-7584130,"id":"68","nextBlock":-1,"opCode":"setVisible","parameters":["panel_list","VISIBLE"],"spec":"%m.view setVisible %m.visible","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
@MainActivity.java_listview1_onItemClicked
{"color":-11899692,"id":"30","nextBlock":31,"opCode":"addSourceDirectly","parameters":["editIndex = _position;"],"spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-11899692,"id":"31","nextBlock":32,"opCode":"addSourceDirectly","parameters":["edittext_title.setText(notes.get((int)editIndex).containsKey(\"title\") ? notes.get((int)editIndex).get(\"title\").toString() : \"\");"],"spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-11899692,"id":"32","nextBlock":33,"opCode":"addSourceDirectly","parameters":["edittext_content.setText(notes.get((int)editIndex).containsKey(\"content\") ? notes.get((int)editIndex).get(\"content\").toString() : \"\");"],"spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-11899692,"id":"33","nextBlock":34,"opCode":"addSourceDirectly","parameters":["tv_edit_title.setText(\"Edit Note\");"],"spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-7584130,"id":"34","nextBlock":35,"opCode":"setVisible","parameters":["panel_list","GONE"],"spec":"%m.view setVisible %m.visible","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
{"color":-7584130,"id":"35","nextBlock":-1,"opCode":"setVisible","parameters":["panel_edit","VISIBLE"],"spec":"%m.view setVisible %m.visible","subStack1":-1,"subStack2":-1,"type":" ","typeName":""}
```

---

## 高级主题

### 多屏幕导航（多 Activity）

#### 添加第二个 Activity

需要在 3 个文件中添加内容：

**file 文件** — 添加新 Activity 声明：
```
@activity
{"fileName":"main","fileType":0,"keyboardSetting":0,"options":1,"orientation":0,"theme":-1}
{"fileName":"second","fileType":0,"keyboardSetting":0,"options":1,"orientation":0,"theme":-1}
@customview
```

**view 文件** — 添加新 Activity 的视图节：
```
@main.xml
... (main 的视图)
@second.xml
... (second 的视图，parent="root" 指向 second 的根容器)
```

**logic 文件** — 添加新 Activity 的所有节：
```
@MainActivity.java_var
...
@SecondActivity.java_var
...
@SecondActivity.java_list
@SecondActivity.java_func
@SecondActivity.java_components
@SecondActivity.java_events
@SecondActivity.java_onCreate_initializeLogic
...
```

> **命名规则**: `fileName` 为 `second` → javaName 为 `SecondActivity.java`，xmlName 为 `second.xml`。
> 规则: 首字母大写 + `Activity.java`（如 `settings` → `SettingsActivity.java`）。

#### Intent 跳转

```json
// 在 logic 中: 从 main 跳转到 second
{"id":"1","nextBlock":2,"opCode":"intentSetScreen","parameters":["intent1","SecondActivity"],
 "spec":"%m.intent setScreen %m.activity","type":" ","color":-7584130}
{"id":"2","nextBlock":-1,"opCode":"startActivity","parameters":["intent1"],
 "spec":"StartActivity %m.intent","type":" ","color":-7584130}
```

需要在 `_components` 中声明 Intent 组件：
```
{"componentId":"intent1","param1":"","param2":"","param3":"","type":1}
```

#### 数据传递

```json
// 发送端: 放入 Extra
{"opCode":"intentPutExtra","parameters":["intent1","key_name","value"],"spec":"%m.intent putExtra key %s value %s"}

// 接收端: 在 initializeLogic 中获取
{"opCode":"addSourceDirectly","parameters":["String receivedValue = getIntent().getStringExtra(\"key_name\");"]}
```

### options 位标志与自动生成视图

#### 常见组合值

| options | 含义 |
|---------|------|
| 0 | 无 Toolbar/Drawer/FAB（全屏活动等） |
| 1 | Toolbar |
| 2 | 全屏 |
| 3 | Toolbar + 全屏 (1\|2) |
| 5 | Toolbar + Drawer (1\|4) |
| 8 | FAB |
| 9 | Toolbar + FAB (1\|8) |
| 12 | Drawer + FAB (4\|8) |
| 13 | Toolbar + Drawer + FAB (1\|4\|8) |

`file` 文件中 `options` 字段控制 Activity 的自动生成行为：

| 位值 | 标志 | 自动生成的视图 | 自动生成的字段 |
|------|------|----------------|----------------|
| 1 | Toolbar | `_coordinator`(CoordinatorLayout), `_app_bar`(AppBarLayout), `_toolbar`(Toolbar) | `_toolbar`, `_app_bar`, `_coordinator` |
| 2 | Fullscreen | 无额外视图 | 无 |
| 4 | Drawer | `_drawer`(DrawerLayout), `_nav_view`(LinearLayout) | `_drawer` |
| 8 | FAB | `_fab`(FloatingActionButton) | `_fab` |

> **⚠️ 重要**: 这些自动生成的视图（`_coordinator`, `_app_bar`, `_toolbar`, `_drawer`, `_nav_view`, `_fab`）**不要在 view 文件中定义**。
> 它们由 `LayoutGenerator` 和 `ActivityCodeGenerator` 自动创建。在 view 文件中重复定义会导致 ID 冲突。

组合示例：
- `options: 1` = Toolbar
- `options: 5` = Toolbar + Drawer (1+4)
- `options: 9` = Toolbar + FAB (1+8)
- `options: 13` = Toolbar + Drawer + FAB (1+4+8)

> Toolbar(1) 是 Drawer(4) 和 FAB(8) 的**前提**。如果需要 Drawer 或 FAB，必须同时设置 Toolbar 位。

### FAB 按钮

当 `options` 包含位 8 时，view 文件需要一个**独立的 FAB 节**：

```
@main.xml
... (普通视图)
@main.xml_fab
{"adSize":"","adUnitId":"","alpha":1.0,"checked":0,"choiceMode":0,"clickable":1,"convert":"","customView":"","dividerHeight":1,"enabled":1,"firstDayOfWeek":1,"id":"_fab","image":{"rotate":0,"scaleType":"CENTER"},"indeterminate":"false","index":0,"inject":"","layout":{"backgroundColor":16777215,"backgroundResColor":null,"backgroundResource":"NONE","borderColor":-16740915,"gravity":0,"height":-2,"layoutGravity":85,"marginBottom":16,"marginLeft":16,"marginRight":16,"marginTop":16,"orientation":-1,"paddingBottom":0,"paddingLeft":0,"paddingRight":0,"paddingTop":0,"weight":0,"weightSum":0,"width":-2},"max":100,"name":"_fab","parent":"root","parentAttributes":{},"parentType":-1,"preId":"","preIndex":0,"preParent":"","preParentType":0,"progress":0,"progressStyle":"?android:progressBarStyle","scaleX":1.0,"scaleY":1.0,"spinnerMode":1,"text":{"hint":"","hintColor":16777215,"imeOption":0,"inputType":1,"line":0,"singleLine":0,"text":"","textColor":16777215,"textFont":"default_font","textSize":12,"textType":0},"translationX":0.0,"translationY":0.0,"type":16}
```

关键属性：
- `id` 必须为 `"_fab"`
- `parentType` 必须为 `-1`（不属于任何父容器，由 LayoutGenerator 自动放置）
- `type` 为 `16`（FloatingActionButton）
- `layoutGravity: 85` = 右下角 (Gravity.BOTTOM | Gravity.END)

FAB 的点击事件在 logic 中声明：
```json
{"eventName":"onClick","eventType":1,"targetId":"_fab","targetType":16}
```

### Drawer 抽屉

当 `options` 包含位 4 时：

#### Drawer 视图节

view 文件需要一个**单独的 drawer 视图节**，节名格式为 `@_drawer_{fileName}.xml`：

```
@main.xml
... (主布局视图)
@_drawer_main.xml
{"id":"drawer_textview1","parent":"root","parentType":0,"index":0,"type":4,
 ... (完整 ViewBean，与 main.xml 中的格式相同)}
```

> drawer 视图中的组件 ID 在代码中会自动加 `_drawer_` 前缀。例如 view 文件中 `id:"textview1"` 在代码中变为 `_drawer_textview1`。

#### Drawer 事件

Drawer 中视图的事件使用 `eventType: 4`（DrawerView）：

```json
{"eventName":"onClick","eventType":4,"targetId":"drawer_btn1","targetType":3}
```

事件段名格式: `@MainActivity.java_drawer_btn1_onClick`

#### 抽屉操作 opCode

```json
{"opCode":"openDrawer","parameters":[],"spec":"openDrawer"}
{"opCode":"closeDrawer","parameters":[],"spec":"closeDrawer"}
{"opCode":"isDrawerOpen","parameters":[],"spec":"isDrawerOpen","type":"b"}
```

### MoreBlock 自定义函数

MoreBlock 允许定义可复用的函数。

#### _func 节格式（⚠️ 不是 _moreBlock）

MoreBlock 函数声明放在 `@{javaName}_func` 节中（**不是** `_moreBlock`）。格式为 `functionName:spec`（冒号分隔）：

```
@MainActivity.java_func
refreshList:refreshList
showMessage:showMessage %s.message %d.duration
calculateTotal:calculateTotal %d.price %d.quantity %b.withTax
```

冒号左边是函数名，右边是 spec（定义参数）。

> **⚠️ 注意**: 解析器只识别 5 种节后缀：`_var`、`_list`、`_func`、`_components`、`_events`。
> `@{javaName}_moreBlock` 不是特殊节——它会被当作空的积木块段处理，不是必须的。真正的 MoreBlock 块逻辑段名为 `@{javaName}_{funcName}_moreBlock`。

#### MoreBlock 参数类型

spec 中的参数格式为 `%type.paramName`：
- `%s.message` → `final String _message`
- `%d.count` → `final double _count`
- `%b.flag` → `final boolean _flag`
- `%m.menuType.paramName` → 根据 menuType 确定 Java 类型（见下表）

##### %m 参数类型映射表

`%m` 参数的格式为 `%m.{menuType}.{paramName}`，其中 menuType 决定生成的 Java 类型：

| menuType | Java 类型 | 示例 spec | 生成代码 |
|----------|-----------|-----------|----------|
| `view` | `View` | `%m.view.target` | `final View _target` |
| `textview` | `TextView` | `%m.textview.tv` | `final TextView _tv` |
| `edittext` | `EditText` | `%m.edittext.et` | `final EditText _et` |
| `imageview` | `ImageView` | `%m.imageview.iv` | `final ImageView _iv` |
| `listview` | `ListView` | `%m.listview.lv` | `final ListView _lv` |
| `spinner` | `Spinner` | `%m.spinner.sp` | `final Spinner _sp` |
| `webview` | `WebView` | `%m.webview.wv` | `final WebView _wv` |
| `seekbar` | `SeekBar` | `%m.seekbar.sb` | `final SeekBar _sb` |
| `checkbox` | `CheckBox` | `%m.checkbox.cb` | `final CheckBox _cb` |
| `intent` | `Intent` | `%m.intent.i` | `final Intent _i` |
| `dialog` | `Dialog` | `%m.dialog.d` | `final Dialog _d` |
| `listMap` | `ArrayList<HashMap<String,Object>>` | `%m.listMap.data` | `final ArrayList<...> _data` |
| `listStr` | `ArrayList<String>` | `%m.listStr.items` | `final ArrayList<String> _items` |
| `listInt` | `ArrayList<Double>` | `%m.listInt.nums` | `final ArrayList<Double> _nums` |
| `varMap` | `HashMap<String,Object>` | `%m.varMap.map` | `final HashMap<...> _map` |
| `color` | `int`(颜色) | `%m.color.clr` | `final int _clr` |
| `activity` | `Context` | `%m.activity.ctx` | `final Context _ctx` |

> 完整 menuType 列表参见 `ComponentTypeMapper.getInternalTypeName()`。

生成的 Java 方法：
```java
// refreshList → public void _refreshList() { ... }
// showMessage %s.message %d.duration → public void _showMessage(final String _message, final double _duration) { ... }
// calculateTotal %d.price %d.quantity %b.withTax → public void _calculateTotal(final double _price, final double _quantity, final boolean _withTax) { ... }
```

> 方法名自动加 `_` 前缀，参数名也加 `_` 前缀。

#### MoreBlock 积木块段

函数体的积木块放在 `@{javaName}_{functionName}_moreBlock` 段中：

```
@MainActivity.java_refreshList_moreBlock
{"id":"100","nextBlock":-1,"opCode":"listSetData","parameters":["listview1","displayList"],
 "spec":"%m.listview setListViewData %m.listStr","type":" ","color":-7584130}
```

#### 调用 MoreBlock

使用 `definedFunc` opCode：

```json
// 调用无参函数
{"opCode":"definedFunc","parameters":[],"spec":"refreshList","type":" "}

// 调用有参函数
{"opCode":"definedFunc","parameters":["Hello","5"],"spec":"showMessage %s.message %d.duration","type":" "}
```

> `spec` 必须与 `_func` 节中定义的完全一致。

### Custom ListView 适配器

#### 基本概念

默认 ListView 使用 `listSetData`（简单字符串列表）。要自定义列表项布局，需要：

1. 在 `file` 文件中声明一个 Custom View
2. 在 `view` 文件中定义 Custom View 的布局
3. 在 `logic` 中使用 `listSetCustomViewData` 和 `onBindCustomView` 事件

#### 步骤 1: 声明 Custom View

**file 文件**:
```
@activity
{"fileName":"main","fileType":0,"keyboardSetting":0,"options":1,"orientation":0,"theme":-1}
@customview
{"fileName":"custom_item","fileType":1,"keyboardSetting":0,"options":0,"orientation":0,"theme":-1}
```

#### 步骤 2: 定义 Custom View 布局

**view 文件** — 添加 Custom View 的视图节：
```
@custom_item.xml
{"id":"tv_item_title","parent":"root","parentType":0,"index":0,"type":4,
 ... (完整 ViewBean)}
{"id":"tv_item_subtitle","parent":"root","parentType":0,"index":1,"type":4,
 ... (完整 ViewBean)}
```

#### 步骤 3: 关联 ListView 与 Custom View

在 view 文件中，ListView 的 `customView` 字段设为 Custom View 名称：

```json
{"id":"listview1","type":9,"customView":"custom_item", ...}
```

#### 步骤 4: 绑定数据

**logic 文件**:

设置数据（使用 MapList）：
```json
{"opCode":"listSetCustomViewData","parameters":["listview1","dataList"],
 "spec":"%m.listview setListCustomViewData %m.listMap","type":" "}
```

声明 `onBindCustomView` 事件：
```json
{"eventName":"onBindCustomView","eventType":1,"targetId":"listview1","targetType":9}
```

在 `onBindCustomView` 段中，可以使用 `_position` 局部变量和 Custom View 中的视图 ID：

```
@MainActivity.java_listview1_onBindCustomView
{"id":"200","opCode":"addSourceDirectly",
 "parameters":["custom_item_tv_item_title.setText(dataList.get(_position).get(\"title\").toString());"],
 "type":" "}
```

> Custom View 中的视图在代码中的名称为 `{customViewName}_{viewId}`（如 `custom_item_tv_item_title`）。

---

## 属性值速查表

### Gravity 值

`layout.gravity`（内容对齐）和 `layout.layoutGravity`（在父容器中的对齐）使用以下值，可通过 `|`（按位或）组合：

| 值 | 常量 | 说明 |
|----|------|------|
| 0 | `NONE` | 无对齐（默认） |
| 1 | `CENTER_HORIZONTAL` | 水平居中 |
| 3 | `LEFT` | 左对齐 |
| 5 | `RIGHT` | 右对齐 |
| 16 | `CENTER_VERTICAL` | 垂直居中 |
| 17 | `CENTER` | 水平+垂直居中 (1\|16) |
| 48 | `TOP` | 顶部对齐 |
| 80 | `BOTTOM` | 底部对齐 |
| 85 | `BOTTOM\|END` | 右下角 (80\|5)，FAB 常用 |

### width / height 特殊值

| 值 | 说明 |
|----|------|
| `-1` | `MATCH_PARENT`（填满父容器） |
| `-2` | `WRAP_CONTENT`（适应内容） |
| `0` | 通常配合 `weight > 0` 使用（按比例分配空间） |
| `> 0` | 固定 dp 值 |

### orientation 值

| 值 | 说明 |
|----|------|
| `-1` | 无方向（非 LinearLayout 时使用） |
| `0` | 水平排列 (HORIZONTAL) |
| `1` | 垂直排列 (VERTICAL) |

### ARGB 颜色转换

颜色值是 **有符号 32 位整数（signed int）**。转换方法：

```
十六进制 → ARGB int:
#AARRGGBB → 0xAARRGGBB → 视为 signed int

示例:
#FFFFFFFF (白) → 0xFFFFFFFF → -1
#FF000000 (黑) → 0xFF000000 → -16777216
#FF1976D2 (蓝) → 0xFF1976D2 → -15106817
#00000000 (透明) → 0x00000000 → 0

常用颜色速查:
-1          = #FFFFFFFF = 白色
-16777216   = #FF000000 = 黑色
0           = #00000000 = 透明
-657931     = #FFF5F5F5 = 浅灰背景 (Grey 100)
-3355444    = #FFCCCCCC = 浅灰色
-5723992    = #FFA8A8A8 = 中灰色 (inactive tab)
-6381922    = #FF9E9E9E = 灰色 (hint色)
-7829368    = #FF888888 = 深灰色 (副标题)
-13421773   = #FF333333 = 深色背景 (底部导航)
-15106817   = #FF1976D2 = Material Blue 700
-16537100   = #FF0288D1 = Material Light Blue 700
-16050234   = #FF0D47A1 = Material Blue 900
-13388315   = #FF339965 = 绿色
-11751600   = #FF4CAF50 = Material Green 500
-769226     = #FFF44336 = Material Red
-14575885   = #FF21A083 = 控制流绿 (block色)
-2497793    = #FFD9D9FF = 控件高亮色 (control highlight)
-10455380   = #FF607D8B = Material Blue Grey
```

> **计算公式**: `color = (int)(0xAARRGGBB)`。当最高位（Alpha）为 1 时，Java 的 int 为负数。
> 例如: `0xFF1976D2` 的十进制是 `4279970514`，超过 int 最大值 `2147483647`，
> 溢出后变为 `4279970514 - 4294967296 = -15106817`。

### inputType 值

EditText 的 `text.inputType`：

| 值 | 常量 | 说明 |
|----|------|------|
| 1 | `TYPE_CLASS_TEXT` | 普通文本 |
| 3 | `TYPE_CLASS_PHONE` | 电话号码 |
| 129 | `TYPE_TEXT_VARIATION_PASSWORD` | 密码（掩码显示） |
| 4098 | `TYPE_NUMBER_FLAG_SIGNED` | 有符号数字 |
| 8194 | `TYPE_NUMBER_FLAG_DECIMAL` | 小数数字 |
| 12290 | `TYPE_NUMBER_SIGNED_DECIMAL` | 有符号小数 |

### textType 值

`text.textType`（字体样式）：

| 值 | 说明 |
|----|------|
| 0 | Normal（普通） |
| 1 | **Bold**（粗体） |
| 2 | *Italic*（斜体） |
| 3 | ***Bold Italic***（粗斜体） |

### imeOption 值

`text.imeOption`（软键盘回车键行为）：

| 值 | 说明 |
|----|------|
| 0 | Normal（默认） |
| 1 | None |
| 2 | Go |
| 3 | Search |
| 4 | Send |
| 5 | Next |
| 6 | Done |

### scaleType 值

`image.scaleType`（图片缩放模式）：

| 值 | 说明 |
|----|------|
| `CENTER` | 居中不缩放 |
| `FIT_XY` | 拉伸填满 |
| `FIT_START` | 等比缩放靠左上 |
| `FIT_CENTER` | 等比缩放居中（默认） |
| `FIT_END` | 等比缩放靠右下 |
| `CENTER_CROP` | 等比缩放裁剪填满 |
| `CENTER_INSIDE` | 等比缩放不超出 |

### Visibility 值

`setVisible` 的 `%m.visible` 参数（⚠️ 是 `%m.visible`，不是 `%m.visibility`）：

| 值 | 说明 |
|----|------|
| `VISIBLE` | 可见 |
| `INVISIBLE` | 不可见（仍占空间） |
| `GONE` | 不可见（不占空间） |

### textFont 值

`text.textFont` 字段：

| 值 | 说明 |
|----|------|
| `default_font` | 系统默认字体 |
| `sans_serif` | 无衬线体 |
| `serif` | 衬线体 |
| `monospace` | 等宽字体 |

> 也可使用 `resource` 文件 `@fonts` 节中声明的自定义字体文件名。

### ObjectAnimator 枚举值

**属性名**（`objectanimatorSetProperty` 参数）：

| 值 | 说明 |
|----|------|
| `translationX` | X 平移 |
| `translationY` | Y 平移 |
| `scaleX` | X 缩放 |
| `scaleY` | Y 缩放 |
| `rotation` | 旋转角度 |
| `alpha` | 透明度 |

**插值器类型**（`objectanimatorSetInterpolator` 参数）：

| 值 | 说明 |
|----|------|
| `LINEAR` | 匀速 |
| `ACCELERATE` | 加速 |
| `DECELERATE` | 减速 |
| `ACCELERATE_DECELERATE` | 先加后减 |
| `ANTICIPATE` | 先回退再前进 |
| `OVERSHOOT` | 超过目标再回弹 |
| `ANTICIPATE_OVERSHOOT` | 回退+超弹 |
| `BOUNCE` | 弹跳 |

**重复模式**（`objectanimatorSetRepeatMode` 参数）：

| 值 | 说明 |
|----|------|
| `RESTART` | 从头开始 |
| `REVERSE` | 反向播放 |

### RequestNetwork 枚举值

**HTTP 方法**（`requestnetworkStartRequestNetwork` 参数）：

| 值 | 说明 |
|----|------|
| `GET` | GET 请求 |
| `POST` | POST 请求 |
| `PUT` | PUT 请求 |
| `DELETE` | DELETE 请求 |

**参数类型**（`requestnetworkSetParams` 的 type 参数）：

| 值 | 说明 |
|----|------|
| `REQUEST_PARAM` | URL 查询参数 |
| `REQUEST_BODY` | 请求体（JSON） |

### Intent Flag 值

`intentSetFlags` 的参数：

| 值 | 说明 |
|----|------|
| `FLAG_ACTIVITY_NEW_TASK` | 在新任务中启动 |
| `FLAG_ACTIVITY_CLEAR_TOP` | 清除目标之上的 Activity |
| `FLAG_ACTIVITY_CLEAR_TASK` | 清除整个任务栈 |
| `FLAG_ACTIVITY_SINGLE_TOP` | 如已在栈顶则不重新创建 |
| `FLAG_ACTIVITY_NO_HISTORY` | 不保留在历史记录中 |

### MapView 地图类型

`mapViewSetMapType` 的参数：

| 值 | 说明 |
|----|------|
| `MAP_TYPE_NORMAL` | 普通地图 |
| `MAP_TYPE_SATELLITE` | 卫星地图 |
| `MAP_TYPE_TERRAIN` | 地形地图 |
| `MAP_TYPE_HYBRID` | 混合地图 |

### LocationManager Provider

`locationManagerRequestLocationUpdates` 的 Provider 参数：

| 值 | 说明 |
|----|------|
| `GPS_PROVIDER` | GPS 定位 |
| `NETWORK_PROVIDER` | 网络定位 |

### Calendar 字段常量

`calendarAdd` / `calendarSet` 的字段参数：

| 值 | 说明 |
|----|------|
| `YEAR` | 年 |
| `MONTH` | 月 |
| `DAY_OF_MONTH` | 日 |
| `HOUR_OF_DAY` | 时（24h） |
| `MINUTE` | 分 |
| `SECOND` | 秒 |

### 设备文件路径汇总

| 路径 | 说明 |
|------|------|
| `/storage/emulated/0/.sketchware/mysc/list/{id}/project` | 项目元数据 |
| `/storage/emulated/0/.sketchware/data/{id}/file` | Activity/CustomView 列表 |
| `/storage/emulated/0/.sketchware/data/{id}/library` | 库配置 |
| `/storage/emulated/0/.sketchware/data/{id}/resource` | 资源列表 |
| `/storage/emulated/0/.sketchware/data/{id}/view` | 视图布局 |
| `/storage/emulated/0/.sketchware/data/{id}/logic` | 逻辑代码 |
| `/storage/emulated/0/.sketchware/bak/{id}/` | 备份目录（结构同 data） |
| `/storage/emulated/0/.sketchware/resources/images/{id}/` | 图片资源 |
| `/storage/emulated/0/.sketchware/resources/sounds/{id}/` | 声音资源 |
| `/storage/emulated/0/.sketchware/resources/fonts/{id}/` | 字体资源 |
| `/storage/emulated/0/.sketchware/libs/local_libs/{name}/` | 本地库文件（主路径） |
| `Android/data/<pkg>/files/local_libs/{name}/` | 本地库文件（回退路径，Samsung Android 16+） |
| `/storage/emulated/0/.sketchware/mysc/{id}/` | 编译输出目录 |

### Activity 命名映射规则

file 文件中的 `fileName` 决定了所有相关名称：

| fileName | javaName | xmlName | activityName | 类名 |
|----------|----------|---------|--------------|------|
| `main` | `MainActivity.java` | `main.xml` | `MainActivity` | `MainActivity` |
| `second` | `SecondActivity.java` | `second.xml` | `SecondActivity` | `SecondActivity` |
| `settings` | `SettingsActivity.java` | `settings.xml` | `SettingsActivity` | `SettingsActivity` |
| `my_page` | `MyPageActivity.java` | `my_page.xml` | `MyPageActivity` | `MyPageActivity` |

> **规则**: javaName = `首字母大写(fileName) + "Activity.java"`。如果 fileName 含下划线，每段首字母大写（如 `my_page` → `MyPageActivity`）。

### 积木块段名命名规则

logic 文件中积木块段的命名遵循固定模式：

| 事件类型 | 段名格式 | 示例 |
|----------|----------|------|
| View 事件 | `@{javaName}_{targetId}_{eventName}` | `@MainActivity.java_btn_save_onClick` |
| Component 事件 | `@{javaName}_{targetId}_{eventName}` | `@MainActivity.java_DB_onChildAdded` |
| Activity 事件 | `@{javaName}_{targetId}_{eventName}` | `@MainActivity.java_onCreate_initializeLogic` |
| Drawer 事件 | `@{javaName}_{targetId}_{eventName}` | `@MainActivity.java_drawer_btn1_onClick` |
| MoreBlock | `@{javaName}_{funcName}_moreBlock` | `@MainActivity.java_refreshList_moreBlock` |
| onActivityResult | `@{javaName}_onActivityResult_onActivityResult` | `@MainActivity.java_onActivityResult_onActivityResult` |

### 项目 ID 分配

Sketchware 从 `601` 开始自动分配项目 ID。手动创建项目时：
- 查看设备上 `/storage/emulated/0/.sketchware/mysc/list/` 目录下已有的 ID
- 选择一个**未使用的数字**作为新项目 ID
- `sc_id`（project 文件）、目录名、所有文件路径中的 ID 必须一致

### weight 与 weightSum 用法

用于在 LinearLayout 中按**比例分配空间**：

```json
// 父容器: weightSum=3, orientation=0 (水平)
{"id":"container","type":0,"layout":{"orientation":0,"weightSum":3,"width":-1,"height":-2,...}}

// 子视图1: weight=1, width=0 → 占 1/3
{"id":"view1","parent":"container","layout":{"weight":1,"width":0,"height":-2,...}}

// 子视图2: weight=2, width=0 → 占 2/3
{"id":"view2","parent":"container","layout":{"weight":2,"width":0,"height":-2,...}}
```

> 使用 weight 时，对应方向的尺寸必须设为 `0`（水平布局设 `width:0`，垂直布局设 `height:0`）。

### convert 字段

`convert` 字段存储视图的**类全名**。对于标准组件，它就是组件的类名：

```json
// 标准组件
{"id":"linear1","type":0,"convert":"LinearLayout"}
{"id":"btn1","type":3,"convert":"Button"}
{"id":"tv1","type":4,"convert":"TextView"}
{"id":"et1","type":5,"convert":"EditText"}
{"id":"iv1","type":6,"convert":"ImageView"}

// 扩展组件使用完整类名
{"id":"civ1","type":43,"convert":"de.hdodenhof.circleimageview.CircleImageView"}

// Custom View 适配器
{"id":"listview1","type":9,"convert":"custom_item.CustomView","customView":"custom_item"}

// 空字符串也可以
{"id":"fab1","type":16,"convert":""}
```

> 对于新建的视图，`convert` 可以设为空字符串 `""` 或对应类名。使用 Custom View 时格式为 `{customViewName}.CustomView`。

### preId / preIndex / preParent / preParentType 字段

这些字段用于**编辑器的撤销/重做功能**，记录视图被移动前的位置。创建新项目时全部设为默认值：

```json
{
  "preId": "",
  "preIndex": 0,
  "preParent": "",
  "preParentType": 0
}
```

### parentAttributes 字段

存储 RelativeLayout 子视图的相对定位属性（如 `layout_below`, `layout_toRightOf`）。对于非 RelativeLayout 子视图，始终为空对象：

```json
{"parentAttributes": {}}

// RelativeLayout 子视图示例:
{"parentAttributes": {"layout_below": "textview1", "layout_centerHorizontal": "true"}}
```

### 如何添加资源文件

#### 图片资源

1. 在 `resource` 文件的 `@images` 节中声明：
```
@images
my_icon
background_img
```

2. 将实际图片文件推送到设备：
```powershell
adb push my_icon.png /storage/emulated/0/.sketchware/resources/images/{id}/my_icon.png
```

3. 在 ViewBean 中引用：
```json
{"image":{"resName":"my_icon","rotate":0,"scaleType":"FIT_CENTER"}}
```

#### 声音资源

1. 在 `@sounds` 节中声明文件名（不含扩展名）
2. 推送到 `resources/sounds/{id}/`
3. 通过 SoundPool/MediaPlayer 组件引用

#### 字体资源

1. 在 `@fonts` 节中声明文件名（不含扩展名）
2. 推送到 `resources/fonts/{id}/`
3. 在 `text.textFont` 中引用字体名

### Block spec 与 opCode 的关系

每个 BlockBean 的 `spec` 字段必须与 `opCode` 对应的标准 spec 一致。`spec` 决定了：
- 编辑器中块的显示文本
- 参数占位符的位置和类型

```json
// ✅ 正确: spec 与 opCode 匹配
{"opCode":"setText","spec":"%m.textview setText %s"}
{"opCode":"doToast","spec":"Toast %s"}
{"opCode":"if","spec":"if %b"}

// ❌ 错误: spec 与 opCode 不匹配
{"opCode":"setText","spec":"set text %s %s"}  // spec 格式错误
```

> 建议直接从下表复制 spec，不要自行编写。

#### 完整 block spec 速查表

> 以下表格由 `BlockSpecRegistry.java` + `strings.xml` 自动提取生成，覆盖所有内置 opCode。
> `type` 列：`" "` = 语句块, `b` = 布尔, `s` = 字符串, `d` = 数值, `c` = C 形, `e` = E 形(ifElse), `f` = 终止块(break)。
> `%m.xxx` 占位符中 `.variable`、`.resource` 等后缀是选择器类型，`parameters` 中直接填 ID 或值即可。
> 特殊块: `getVar` (spec=变量名, type=b/s/d), `definedFunc` (spec=_func中的spec), `getArg` (spec=参数名)。

**变量 & 代码**:

| opCode | spec | type |
|--------|------|------|
| `setVarBoolean` | `set %m.varBool to %b` | `" "` |
| `setVarInt` | `set %m.varInt to %d` | `" "` |
| `increaseInt` | `%m.varInt increase 1` | `" "` |
| `decreaseInt` | `%m.varInt decrease 1` | `" "` |
| `setVarString` | `set %m.varStr to %s` | `" "` |
| `addSourceDirectly` | `add source directly %s.inputOnly` | `" "` |

**Map 操作**:

| opCode | spec | type |
|--------|------|------|
| `mapCreateNew` | `%m.varMap create new map` | `" "` |
| `mapPut` | `%m.varMap put key %s value %s` | `" "` |
| `mapGet` | `%m.varMap get key %s` | `s` |
| `mapContainKey` | `%m.varMap contain key %s` | `b` |
| `mapRemoveKey` | `%m.varMap remove key %s` | `" "` |
| `mapSize` | `%m.varMap size` | `d` |
| `mapClear` | `%m.varMap clear` | `" "` |
| `mapIsEmpty` | `%m.varMap is empty` | `b` |
| `mapGetAllKeys` | `%m.varMap get all keys to %m.listStr` | `" "` |

**List 操作 (Int)**:

| opCode | spec | type |
|--------|------|------|
| `addListInt` | `add %d to %m.listInt` | `" "` |
| `insertListInt` | `insert %d at %d to %m.listInt` | `" "` |
| `getAtListInt` | `get at %d of %m.listInt` | `d` |
| `indexListInt` | `index %d in %m.listInt` | `d` |
| `containListInt` | `%m.listInt contains %d` | `b` |

**List 操作 (Str)**:

| opCode | spec | type |
|--------|------|------|
| `addListStr` | `add %s to %m.listStr` | `" "` |
| `insertListStr` | `insert %s at %d to %m.listStr` | `" "` |
| `getAtListStr` | `get at %d of %m.listStr` | `s` |
| `indexListStr` | `index %s in %m.listStr` | `d` |
| `containListStr` | `%m.listStr contains %s` | `b` |

**List 操作 (Map)**:

| opCode | spec | type |
|--------|------|------|
| `addListMap` | `add key %s value %s to %m.listMap` | `" "` |
| `insertListMap` | `insert key %s value %s at %d to %m.listMap` | `" "` |
| `getAtListMap` | `get value at %d key %s of %m.listMap` | `s` |
| `setListMap` | `set key %s value %s at %d to %m.listMap` | `" "` |
| `containListMap` | `%m.listMap contains at %d key %s` | `b` |
| `addMapToList` | `add %m.varMap to %m.listMap` | `" "` |
| `insertMapToList` | `insert %m.varMap at %d to %m.listMap` | `" "` |
| `getMapInList` | `get at %d of %m.listMap to %m.varMap` | `" "` |

**List 通用**:

| opCode | spec | type |
|--------|------|------|
| `deleteList` | `delete at %d of %m.list` | `" "` |
| `lengthList` | `length of %m.list` | `d` |
| `clearList` | `clear %m.list` | `" "` |

**控制流**:

| opCode | spec | type |
|--------|------|------|
| `if` | `if %b then` | `c` |
| `ifElse` | `if %b then` | `e` |
| `repeat` | `repeat %d` | `c` |
| `forever` | `forever` | `c` |
| `break` | `stop` | `f` |
| `true` | `true` | `b` |
| `false` | `false` | `b` |

**运算符**:

| opCode | spec | type |
|--------|------|------|
| `<` | `%d < %d` | `b` |
| `=` | `%d = %d` | `b` |
| `>` | `%d > %d` | `b` |
| `&&` | `%b and %b` | `b` |
| `\|\|` | `%b or %b` | `b` |
| `not` | `not %b` | `b` |
| `+` | `%d + %d` | `d` |
| `-` | `%d - %d` | `d` |
| `*` | `%d * %d` | `d` |
| `/` | `%d / %d` | `d` |
| `%` | `%d % %d` | `d` |
| `random` | `pick random %d to %d` | `d` |

**字符串**:

| opCode | spec | type |
|--------|------|------|
| `stringLength` | `length of %s` | `d` |
| `stringJoin` | `join %s and %s` | `s` |
| `stringIndex` | `index %s of %s` | `d` |
| `stringLastIndex` | `last index %s of %s` | `d` |
| `stringSub` | `%s substring %d to %d` | `s` |
| `stringEquals` | `%s equals %s` | `b` |
| `stringContains` | `%s contains %s` | `b` |
| `stringReplace` | `%s replace all %s with %s` | `s` |
| `stringReplaceFirst` | `%s replace first RegEx %s with %s` | `s` |
| `stringReplaceAll` | `%s replace all RegEx %s with %s` | `s` |
| `toNumber` | `toNumber %s` | `d` |
| `trim` | `trim %s` | `s` |
| `toUpperCase` | `toUpperCase %s` | `s` |
| `toLowerCase` | `toLowerCase %s` | `s` |
| `toString` | `toString %d without decimal` | `s` |
| `toStringWithDecimal` | `toString %d with decimal` | `s` |
| `toStringFormat` | `%d toDecimalFormat %s` | `s` |

**JSON 转换**:

| opCode | spec | type |
|--------|------|------|
| `mapToStr` | `%m.varMap to JSON String` | `s` |
| `strToMap` | `JSON %s to %m.varMap` | `" "` |
| `listMapToStr` | `%m.listMap to JSON String` | `s` |
| `strToListMap` | `JSON %s to %m.listMap` | `" "` |

**数学**:

| opCode | spec | type |
|--------|------|------|
| `mathGetDip` | `getDip %d` | `d` |
| `mathGetDisplayWidth` | `getDisplayWidthPixels` | `d` |
| `mathGetDisplayHeight` | `getDisplayHeightPixels` | `d` |
| `mathPi` | `PI(π)` | `d` |
| `mathE` | `E(e)` | `d` |
| `mathPow` | `%d to the %d power` | `d` |
| `mathMin` | `minimum of %d and %d` | `d` |
| `mathMax` | `maximum of %d and %d` | `d` |
| `mathSqrt` | `square root of %d` | `d` |
| `mathAbs` | `absolute value of %d` | `d` |
| `mathRound` | `round %d` | `d` |
| `mathCeil` | `ceil %d` | `d` |
| `mathFloor` | `floor %d` | `d` |
| `mathSin` | `sin %d` | `d` |
| `mathCos` | `cos %d` | `d` |
| `mathTan` | `tan %d` | `d` |
| `mathAsin` | `arcsin %d` | `d` |
| `mathAcos` | `arccos %d` | `d` |
| `mathAtan` | `arctan %d` | `d` |
| `mathExp` | `exp %d` | `d` |
| `mathLog` | `ln %d` | `d` |
| `mathLog10` | `log %d` | `d` |
| `mathToRadian` | `Degree %d to Radian` | `d` |
| `mathToDegree` | `Radian %d to Degree` | `d` |

**Drawer**:

| opCode | spec | type |
|--------|------|------|
| `isDrawerOpen` | `isDrawerOpen` | `b` |
| `openDrawer` | `openDrawer` | `" "` |
| `closeDrawer` | `closeDrawer` | `" "` |

**View 通用**:

| opCode | spec | type |
|--------|------|------|
| `setEnable` | `%m.view setEnable %b` | `" "` |
| `getEnable` | `%m.view getEnable` | `b` |
| `setVisible` | `%m.view setVisible %m.visible` | `" "` |
| `setClickable` | `%m.view setClickable %b` | `" "` |
| `setBgColor` | `%m.view setBackgroundColor %m.color` | `" "` |
| `setBgResource` | `%m.view setBackgroundResource %m.resource_bg` | `" "` |
| `requestFocus` | `%m.view request focus` | `" "` |
| `setRotate` | `%m.view setRotation %d` | `" "` |
| `getRotate` | `%m.view getRotation` | `d` |
| `setAlpha` | `%m.view setAlpha %d` | `" "` |
| `getAlpha` | `%m.view getAlpha` | `d` |
| `setTranslationX` | `%m.view setTranslationX %d` | `" "` |
| `getTranslationX` | `%m.view getTranslationX` | `d` |
| `setTranslationY` | `%m.view setTranslationY %d` | `" "` |
| `getTranslationY` | `%m.view getTranslationY` | `d` |
| `setScaleX` | `%m.view setScaleX %d` | `" "` |
| `getScaleX` | `%m.view getScaleX` | `d` |
| `setScaleY` | `%m.view setScaleY %d` | `" "` |
| `getScaleY` | `%m.view getScaleY` | `d` |
| `getLocationX` | `%m.view getLocationX` | `d` |
| `getLocationY` | `%m.view getLocationY` | `d` |

**TextView / EditText**:

| opCode | spec | type |
|--------|------|------|
| `setText` | `%m.textview setText %s` | `" "` |
| `getText` | `%m.textview getText` | `s` |
| `setTypeface` | `%m.textview setTypeface %m.font with style %m.typeface` | `" "` |
| `setTextColor` | `%m.textview setTextColor %m.color` | `" "` |
| `setHint` | `%m.edittext set hint text to %s` | `" "` |
| `setHintTextColor` | `%m.edittext set hint color to %m.color` | `" "` |

**ImageView**:

| opCode | spec | type |
|--------|------|------|
| `setImage` | `%m.imageview setImage %m.resource` | `" "` |
| `setColorFilter` | `%m.imageview setColorFilter %m.color` | `" "` |
| `setImageFilePath` | `%m.imageview set image from file path %s` | `" "` |
| `setImageUrl` | `%m.imageview set image from url %s` | `" "` |

**CheckBox / Switch**:

| opCode | spec | type |
|--------|------|------|
| `setChecked` | `%m.checkbox setChecked %b` | `" "` |
| `getChecked` | `%m.checkbox getChecked` | `b` |
| `setThumbResource` | `%m.switch setThumbResource %m.resource` | `" "` |
| `setTrackResource` | `%m.switch setTrackResource %m.resource` | `" "` |

**SeekBar**:

| opCode | spec | type |
|--------|------|------|
| `seekBarSetMax` | `%m.seekbar setMax %d` | `" "` |
| `seekBarGetMax` | `%m.seekbar getMax` | `d` |
| `seekBarSetProgress` | `%m.seekbar setProgress %d` | `" "` |
| `seekBarGetProgress` | `%m.seekbar getProgress` | `d` |

**ProgressBar**:

| opCode | spec | type |
|--------|------|------|
| `progressBarSetIndeterminate` | `%m.progressbar setIndeterminate %b` | `" "` |

**ListView**:

| opCode | spec | type |
|--------|------|------|
| `listSetData` | `%m.listview setListViewData %m.listStr` | `" "` |
| `listSetCustomViewData` | `%m.listview setListCustomViewData %m.listMap` | `" "` |
| `listRefresh` | `%m.listview refreshData` | `" "` |
| `listSetItemChecked` | `%m.listview setItemChecked pos %d value %b` | `" "` |
| `listGetCheckedPosition` | `%m.listview getCheckedPosition` | `d` |
| `listGetCheckedPositions` | `%m.listview getCheckedPositions to %m.listInt` | `" "` |
| `listGetCheckedCount` | `%m.listview getCheckedCount` | `d` |
| `listSmoothScrollTo` | `%m.listview smoothScrollToPosition %d` | `" "` |

**Spinner**:

| opCode | spec | type |
|--------|------|------|
| `spnSetData` | `%m.spinner setSpinnerData %m.listStr` | `" "` |
| `spnRefresh` | `%m.spinner refreshData` | `" "` |
| `spnSetSelection` | `%m.spinner setSelection %d` | `" "` |
| `spnGetSelection` | `%m.spinner getSelection` | `d` |

**WebView**:

| opCode | spec | type |
|--------|------|------|
| `webViewLoadUrl` | `%m.webview loadUrl %s.url` | `" "` |
| `webViewGetUrl` | `%m.webview getUrl` | `s` |
| `webViewSetCacheMode` | `%m.webview setCacheMode %m.cacheMode` | `" "` |
| `webViewCanGoBack` | `%m.webview canGoBack` | `b` |
| `webViewCanGoForward` | `%m.webview canGoForward` | `b` |
| `webViewGoBack` | `%m.webview goBack` | `" "` |
| `webViewGoForward` | `%m.webview goForward` | `" "` |
| `webViewClearCache` | `%m.webview clearCache` | `" "` |
| `webViewClearHistory` | `%m.webview clearHistory` | `" "` |
| `webViewStopLoading` | `%m.webview stopLoading` | `" "` |
| `webViewZoomIn` | `%m.webview zoomIn` | `" "` |
| `webViewZoomOut` | `%m.webview zoomOut` | `" "` |

**CalendarView**:

| opCode | spec | type |
|--------|------|------|
| `calendarViewGetDate` | `%m.calendarview getDate(ms)` | `d` |
| `calendarViewSetDate` | `%m.calendarview setDate %d ms` | `" "` |
| `calendarViewSetMinDate` | `%m.calendarview setMinDate %d ms` | `" "` |
| `calnedarViewSetMaxDate` | `%m.calendarview setMaxDate %d ms` | `" "` |

**AdView / InterstitialAd**:

| opCode | spec | type |
|--------|------|------|
| `adViewLoadAd` | `%m.adview load` | `" "` |
| `interstitialadCreate` | `%m.interstitialad create` | `" "` |
| `interstitialadLoadAd` | `%m.interstitialad load` | `" "` |
| `interstitialadShow` | `%m.interstitialad show` | `" "` |

**MapView**:

| opCode | spec | type |
|--------|------|------|
| `mapViewSetMapType` | `%m.mapview set Map type %m.mapType` | `" "` |
| `mapViewMoveCamera` | `%m.mapview move camera lat %d lng %d` | `" "` |
| `mapViewZoomTo` | `%m.mapview zoom to %d` | `" "` |
| `mapViewZoomIn` | `%m.mapview zoom in` | `" "` |
| `mapViewZoomOut` | `%m.mapview zoom out` | `" "` |
| `mapViewAddMarker` | `%m.mapview add marker id %s position lat %d lng %d` | `" "` |
| `mapViewSetMarkerInfo` | `%m.mapview marker id %s set title %s snippet %s` | `" "` |
| `mapViewSetMarkerPosition` | `%m.mapview marker id %s set position lat %d lng %d` | `" "` |
| `mapViewSetMarkerColor` | `%m.mapview marker id %s set color %m.markerColor alpha %d` | `" "` |
| `mapViewSetMarkerIcon` | `%m.mapview marker id %s set icon %m.resource` | `" "` |
| `mapViewSetMarkerVisible` | `%m.mapview marker id %s set visible %b` | `" "` |

**Intent**:

| opCode | spec | type |
|--------|------|------|
| `intentSetAction` | `%m.intent setAction %m.intentAction` | `" "` |
| `intentSetData` | `%m.intent setData %s.intentData` | `" "` |
| `intentSetScreen` | `%m.intent setScreen %m.activity` | `" "` |
| `intentPutExtra` | `%m.intent putExtra key %s value %s` | `" "` |
| `intentSetFlags` | `%m.intent setFlags %m.intentFlags` | `" "` |
| `startActivity` | `StartActivity %m.intent` | `" "` |
| `intentGetString` | `Activity getExtra key %s` | `s` |
| `finishActivity` | `Finish Activity` | `" "` |

**SharedPreferences (type=2)**:

| opCode | spec | type |
|--------|------|------|
| `fileSetFileName` | `%m.file setFileName %s` | `" "` |
| `fileGetData` | `%m.file getData key %s` | `s` |
| `fileSetData` | `%m.file setData key %s value %s` | `" "` |
| `fileRemoveData` | `%m.file removeData key %s` | `" "` |

**Calendar 组件 (type=3)**:

| opCode | spec | type |
|--------|------|------|
| `calendarGetNow` | `%m.calendar getNow` | `" "` |
| `calendarAdd` | `%m.calendar add %m.calendarField value %d` | `" "` |
| `calendarSet` | `%m.calendar set %m.calendarField value %d` | `" "` |
| `calendarFormat` | `%m.calendar Format %s` | `s` |
| `calendarDiff` | `Difference %m.calendar - %m.calendar` | `d` |
| `calendarGetTime` | `%m.calendar getTime(ms)` | `d` |
| `calendarSetTime` | `%m.calendar setTime %d ms` | `" "` |

**Vibrator (type=4)**:

| opCode | spec | type |
|--------|------|------|
| `vibratorAction` | `%m.vibrator vibrate for %d ms` | `" "` |

**Timer (type=5)**:

| opCode | spec | type |
|--------|------|------|
| `timerAfter` | `%m.timer after %d ms` | `c` |
| `timerEvery` | `%m.timer after %d ms for every %d ms` | `c` |
| `timerCancel` | `%m.timer cancel` | `" "` |

**Dialog (type=7)**:

| opCode | spec | type |
|--------|------|------|
| `dialogSetTitle` | `%m.dialog set title %s` | `" "` |
| `dialogSetMessage` | `%m.dialog set message %s` | `" "` |
| `dialogOkButton` | `%m.dialog OK Button %s Clicked` | `c` |
| `dialogCancelButton` | `%m.dialog Cancel Button %s Clicked` | `c` |
| `dialogNeutralButton` | `%m.dialog Neutral Button %s Clicked` | `c` |
| `dialogShow` | `%m.dialog show` | `" "` |
| `dialogDismiss` | `%m.dialog dismiss` | `" "` |

**MediaPlayer (type=8)**:

| opCode | spec | type |
|--------|------|------|
| `mediaplayerCreate` | `%m.mediaplayer create %m.sound` | `" "` |
| `mediaplayerStart` | `%m.mediaplayer start` | `" "` |
| `mediaplayerPause` | `%m.mediaplayer pause` | `" "` |
| `mediaplayerSeek` | `%m.mediaplayer seek to %d` | `" "` |
| `mediaplayerGetCurrent` | `%m.mediaplayer get current duration` | `d` |
| `mediaplayerGetDuration` | `%m.mediaplayer get song duration` | `d` |
| `mediaplayerIsPlaying` | `%m.mediaplayer is playing` | `b` |
| `mediaplayerSetLooping` | `%m.mediaplayer set looping %b` | `" "` |
| `mediaplayerIsLooping` | `%m.mediaplayer is looping` | `b` |
| `mediaplayerReset` | `%m.mediaplayer reset` | `" "` |
| `mediaplayerRelease` | `%m.mediaplayer release` | `" "` |

**SoundPool (type=9)**:

| opCode | spec | type |
|--------|------|------|
| `soundpoolCreate` | `%m.soundpool create max stream count %d` | `" "` |
| `soundpoolLoad` | `SoundID : %m.soundpool load %m.sound` | `d` |
| `soundpoolStreamPlay` | `StreamID : %m.soundpool play Sound ID %d 1 + %d times` | `d` |
| `soundpoolStreamStop` | `%m.soundpool stop stream ID %d` | `" "` |

**ObjectAnimator (type=10)**:

| opCode | spec | type |
|--------|------|------|
| `objectanimatorSetTarget` | `%m.objectanimator set target %m.view` | `" "` |
| `objectanimatorSetProperty` | `%m.objectanimator set property %m.animatorproperty` | `" "` |
| `objectanimatorSetValue` | `%m.objectanimator set value %d` | `" "` |
| `objectanimatorSetFromTo` | `%m.objectanimator set values from %d to %d` | `" "` |
| `objectanimatorSetDuration` | `%m.objectanimator set duration %d` | `" "` |
| `objectanimatorSetRepeatMode` | `%m.objectanimator set repeat mode %m.aniRepeatMode` | `" "` |
| `objectanimatorSetRepeatCount` | `%m.objectanimator set repeat count %d` | `" "` |
| `objectanimatorSetInterpolator` | `%m.objectanimator set interpolator %m.aniInterpolator` | `" "` |
| `objectanimatorStart` | `%m.objectanimator start` | `" "` |
| `objectanimatorCancel` | `%m.objectanimator cancel` | `" "` |
| `objectanimatorIsRunning` | `%m.objectanimator is running` | `b` |

**Firebase (type=6)**:

| opCode | spec | type |
|--------|------|------|
| `firebaseAdd` | `%m.firebase add key %s value %m.varMap` | `" "` |
| `firebasePush` | `%m.firebase push value %m.varMap` | `" "` |
| `firebaseGetPushKey` | `%m.firebase push getKey` | `s` |
| `firebaseDelete` | `%m.firebase delete key %s` | `" "` |
| `firebaseGetChildren` | `%m.firebase get children to %m.listMap then` | `c` |
| `firebaseStartListen` | `%m.firebase start Listening` | `" "` |
| `firebaseStopListen` | `%m.firebase stop Listening` | `" "` |

**FirebaseAuth (type=12)**:

| opCode | spec | type |
|--------|------|------|
| `firebaseauthCreateUser` | `%m.firebaseauth createUserWith Email %s and Password %s` | `" "` |
| `firebaseauthSignInUser` | `%m.firebaseauth signInWith Email %s and Password %s` | `" "` |
| `firebaseauthSignInAnonymously` | `%m.firebaseauth signInAnonymously` | `" "` |
| `firebaseauthIsLoggedIn` | `FirebaseAuth isLoggedIn` | `b` |
| `firebaseauthGetCurrentUser` | `FirebaseAuth getEmail` | `s` |
| `firebaseauthGetUid` | `FirebaseAuth getUid` | `s` |
| `firebaseauthResetPassword` | `%m.firebaseauth send password reset email to %s` | `" "` |
| `firebaseauthSignOutUser` | `FirebaseAuth signOut` | `" "` |

**FirebaseStorage (type=14)**:

| opCode | spec | type |
|--------|------|------|
| `firebasestorageUploadFile` | `%m.firebasestorage upload file path %s name as %s` | `" "` |
| `firebasestorageDownloadFile` | `%m.firebasestorage download file url %s to file path %s` | `" "` |
| `firebasestorageDelete` | `%m.firebasestorage delete file url %s` | `" "` |

**Gyroscope (type=11)**:

| opCode | spec | type |
|--------|------|------|
| `gyroscopeStartListen` | `%m.gyroscope Sensor start` | `" "` |
| `gyroscopeStopListen` | `%m.gyroscope Sensor stop` | `" "` |

**Camera / FilePicker**:

| opCode | spec | type |
|--------|------|------|
| `camerastarttakepicture` | `%m.camera take picture` | `" "` |
| `filepickerstartpickfiles` | `%m.filepicker pick files` | `" "` |

**RequestNetwork**:

| opCode | spec | type |
|--------|------|------|
| `requestnetworkSetParams` | `%m.requestnetwork set params %m.varMap to request type %m.requestType` | `" "` |
| `requestnetworkSetHeaders` | `%m.requestnetwork set headers %m.varMap` | `" "` |
| `requestnetworkStartRequestNetwork` | `%m.requestnetwork start network request to method %m.method to url %s with tag %s` | `" "` |

**FileUtil**:

| opCode | spec | type |
|--------|------|------|
| `fileutildelete` | `delete file path %s` | `" "` |
| `fileutilcopy` | `copy file path %s to path %s` | `" "` |
| `fileutilwrite` | `write String %s to file path %s` | `" "` |
| `fileutilread` | `read file path %s` | `s` |
| `fileutilmove` | `move file path %s to path %s` | `" "` |
| `fileutilisexist` | `is exist file path %s` | `b` |
| `fileutilmakedir` | `make directory path %s` | `" "` |
| `fileutillistdir` | `file list in path %s to %m.listStr` | `" "` |
| `fileutilisdir` | `path %s is directory` | `b` |
| `fileutilisfile` | `path %s is file` | `b` |
| `fileutillength` | `get length of path %s` | `d` |
| `fileutilStartsWith` | `%s starts with %s` | `b` |
| `fileutilEndsWith` | `%s ends with %s` | `b` |
| `fileutilGetLastSegmentPath` | `get last segment path of %s` | `s` |

**Toast / 通用**:

| opCode | spec | type |
|--------|------|------|
| `doToast` | `Toast %s` | `" "` |
| `copyToClipboard` | `copyToClipboard %s` | `" "` |
| `setTitle` | `Activity set title %s` | `" "` |
| `getExternalStorageDir` | `get external storage directory` | `s` |
| `getPackageDataDir` | `get package data directory` | `s` |
| `getPublicDir` | `get public directory type %m.directoryType` | `s` |
| `getJpegRotate` | `get jpeg rotate from file path %s` | `d` |

**Bitmap 操作**:

| opCode | spec | type |
|--------|------|------|
| `resizeBitmapFileRetainRatio` | `resize image retain ratio from path %s to path %s max size %d` | `" "` |
| `resizeBitmapFileToSquare` | `resize image to square from path %s to path %s max size %d` | `" "` |
| `resizeBitmapFileToCircle` | `resize image to circle from path %s to path %s` | `" "` |
| `resizeBitmapFileWithRoundedBorder` | `resize image rounded from path %s to path %s round pixels %d` | `" "` |
| `cropBitmapFileFromCenter` | `crop image center from path %s to path %s width %d height %d` | `" "` |
| `rotateBitmapFile` | `rotate image from path %s to path %s angle %d` | `" "` |
| `scaleBitmapFile` | `scale image from path %s to path %s x %d y %d` | `" "` |
| `skewBitmapFile` | `skew image from path %s to path %s x %d y %d` | `" "` |
| `setBitmapFileColorFilter` | `set image color filter from path %s to path %s color %m.color` | `" "` |
| `setBitmapFileBrightness` | `set image brightness from path %s to path %s value %d` | `" "` |
| `setBitmapFileContrast` | `set image contrast from path %s to path %s value %d` | `" "` |

**TextToSpeech**:

| opCode | spec | type |
|--------|------|------|
| `textToSpeechSetPitch` | `%m.texttospeech set pitch %d` | `" "` |
| `textToSpeechSetSpeechRate` | `%m.texttospeech set speech rate %d` | `" "` |
| `textToSpeechSpeak` | `%m.texttospeech set speak %s` | `" "` |
| `textToSpeechIsSpeaking` | `%m.texttospeech is speaking` | `b` |
| `textToSpeechStop` | `%m.texttospeech stop` | `" "` |
| `textToSpeechShutdown` | `%m.texttospeech shutdown` | `" "` |

**SpeechToText**:

| opCode | spec | type |
|--------|------|------|
| `speechToTextStartListening` | `%m.speechtotext start listening` | `" "` |
| `speechToTextStopListening` | `%m.speechtotext stop listening` | `" "` |
| `speechToTextShutdown` | `%m.speechtotext shutdown` | `" "` |

**BluetoothConnect**:

| opCode | spec | type |
|--------|------|------|
| `bluetoothConnectReadyConnection` | `%m.bluetoothconnect ready connection with tag %s` | `" "` |
| `bluetoothConnectReadyConnectionToUuid` | `%m.bluetoothconnect ready connection to uuid %s with tag %s` | `" "` |
| `bluetoothConnectStartConnection` | `%m.bluetoothconnect start connection to address %s with tag %s` | `" "` |
| `bluetoothConnectStartConnectionToUuid` | `%m.bluetoothconnect start connection to uuid %s and address %s with tag %s` | `" "` |
| `bluetoothConnectStopConnection` | `%m.bluetoothconnect stop connection with tag %s` | `" "` |
| `bluetoothConnectSendData` | `%m.bluetoothconnect send data %s with tag %s` | `" "` |
| `bluetoothConnectIsBluetoothEnabled` | `%m.bluetoothconnect is bluetooth enabled` | `b` |
| `bluetoothConnectIsBluetoothActivated` | `%m.bluetoothconnect is bluetooth activated` | `b` |
| `bluetoothConnectActivateBluetooth` | `%m.bluetoothconnect activate bluetooth` | `" "` |
| `bluetoothConnectGetPairedDevices` | `%m.bluetoothconnect get paired devices to %m.listMap` | `" "` |
| `bluetoothConnectGetRandomUuid` | `%m.bluetoothconnect get random uuid` | `s` |

**LocationManager**:

| opCode | spec | type |
|--------|------|------|
| `locationManagerRequestLocationUpdates` | `%m.locationmanager request location updates type %m.providerType min time %d min distance %d` | `" "` |
| `locationManagerRemoveUpdates` | `%m.locationmanager remove updates` | `" "` |

**RewardedVideoAd (type=22)**:

| opCode | spec | type |
|--------|------|------|
| `rewardedVideoAdLoad` | `%m.videoad load in %m.activity` | `" "` |
| `rewardedVideoAdShow` | `%m.videoad show in %m.activity` | `" "` |

**ProgressDialog (type=23)**:

| opCode | spec | type |
|--------|------|------|
| `progressdialogCreate` | `%m.progressdialog Create in %m.activity` | `" "` |
| `progressdialogSetTitle` | `%m.progressdialog setTitle %s` | `" "` |
| `progressdialogSetMessage` | `%m.progressdialog setMessage %s` | `" "` |
| `progressdialogSetMax` | `%m.progressdialog setMax %d` | `" "` |
| `progressdialogSetProgress` | `%m.progressdialog setProgress %d` | `" "` |
| `progressdialogSetCancelable` | `%m.progressdialog setCancelable %b` | `" "` |
| `progressdialogSetCanceledOutside` | `%m.progressdialog setCancelableWhenTouchOutside %b` | `" "` |
| `progressdialogSetStyle` | `%m.progressdialog setProgressStyle %m.styleprogress` | `" "` |
| `progressdialogShow` | `%m.progressdialog show` | `" "` |
| `progressdialogDismiss` | `%m.progressdialog dismiss` | `" "` |

**DatePickerDialog (type=24)**:

| opCode | spec | type |
|--------|------|------|
| `datePickerDialogShow` | `DatePickerDialog show` | `" "` |

**TimePickerDialog (type=25)**:

| opCode | spec | type |
|--------|------|------|
| `timePickerDialogShow` | `%m.timepickerdialog show` | `" "` |

**AsyncTask (type=36)**:

| opCode | spec | type |
|--------|------|------|
| `AsyncTaskExecute` | `%m.asynctask execute message %s` | `" "` |
| `AsyncTaskPublishProgress` | `publish progress %d` | `" "` |

### inject 字段用法

ViewBean 的 `inject` 字段可以向生成的 XML 标签中注入自定义属性：

```json
{"id":"edittext1","type":5,"inject":"android:lines=\"5\" android:scrollbars=\"vertical\"", ...}
```

生成的 XML：
```xml
<EditText
    android:id="@+id/edittext1"
    android:lines="5"
    android:scrollbars="vertical"
    ... />
```

> `inject` 的内容会被原样插入到 XML 标签的属性中。可以用于添加 Sketchware 不原生支持的任何 XML 属性。

---

## Sketchware-Pro 扩展文件

除了 6 个核心数据文件外，Sketchware-Pro 在 `data/{id}/` 目录下还支持以下扩展文件（均为可选）：

| 文件名 | 格式 | 说明 | 默认值/空值 |
|--------|------|------|-------------|
| `permission` | 每行一个权限名 | 自定义权限声明 | 空文件 |
| `custom_blocks` | JSON 数组 | 自定义积木块定义 | `[]` |
| `proguard` | JSON 对象 | ProGuard 配置开关 | `{"debug":"false","enabled":"false"}` |
| `proguard-rules.pro` | ProGuard 规则文本 | 自定义混淆规则 | 空或默认规则 |
| `stringfog` | JSON 对象 | 字符串加密开关 | `{"enabled":"false"}` |
| `project_config` | JSON 对象 | 项目附加配置 | 空或 `{}` |
| `build_config` | JSON 对象 | 构建配置 | 空或 `{}` |
| `local_library` | JSON 数组 | 本地库引用 | `[]` |
| `compile_log` | 文本 | 编译日志（自动生成） | — |
| `Injection/` | 目录 | AndroidManifest 注入配置 | 空目录 |
| `java/` | 目录 | 额外 Java 源文件 | 空目录 |

### permission 文件

每行一个 Android 权限名：

```
android.permission.INTERNET
android.permission.CAMERA
android.permission.WRITE_EXTERNAL_STORAGE
```

> 基本权限（如 INTERNET）在启用对应组件时自动添加，通常不需要手动声明。

### proguard 文件

JSON 对象（`HashMap<String, String>`），控制代码混淆/缩减。由 `ProguardHandler` 类管理。

```json
{"enabled": "false", "debug": "false", "r8": "false"}
```

| 键 | 说明 | 默认值 |
|----|------|--------|
| `enabled` | 是否启用代码缩减（ProGuard/R8） | `"false"` |
| `debug` | 是否生成调试映射文件 | `"false"` |
| `r8` | 使用 R8 替代 ProGuard | `"false"` |

> 混淆规则文件为同目录下的 `proguard-rules.pro`（纯文本），默认包含 `-repackageclasses -ignorewarnings -dontwarn -dontnote`。
> 另有 `proguard_fm` 文件（JSON 字符串数组），列出启用全量混淆模式的库名。

### stringfog 文件

JSON 对象（`HashMap<String, String>`），控制字符串加密。由 `StringfogHandler` 类管理。

```json
{"enabled": "false"}
```

| 键 | 说明 | 默认值 |
|----|------|--------|
| `enabled` | 是否启用字符串加密 | `"false"` |

### custom_blocks 文件

JSON 数组，定义自定义积木块。新项目默认为空数组 `[]`。

每个元素是一个 `ExtraBlockInfo` 对象，由 Gson 序列化：

```json
[
  {
    "name": "myCustomBlock",
    "spec": "%m.textview setCustomText %s",
    "spec2": "",
    "code": "%s.setText(%s);",
    "color": -7584130,
    "paletteColor": 0
  }
]
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | String | 块的 opCode 名称（唯一标识符） |
| `spec` | String | 显示格式字符串，使用 `%s`/`%d`/`%b`/`%m.xxx` 占位符 |
| `spec2` | String | 第二行格式字符串（用于 `e` 类型块），通常为空 |
| `code` | String | Java 代码模板，`%s`/`%1$s`/`%2$s` 对应参数替换 |
| `color` | int | 块的颜色（ARGB int），如 `-7584130` 对应蓝色 |
| `paletteColor` | int | 面板分类颜色，`0` 表示使用默认 |

> **注意**: `isMissing` 字段标记为 `transient`，不会序列化到 JSON 中。
>
> **块加载来源**: 自定义块可以来自两个位置：
> - 项目级: `/storage/emulated/0/.sketchware/data/{id}/custom_blocks` — 随项目保存
> - 全局级: `/storage/emulated/0/.sketchware/resources/block/My Block/block.json` — 所有项目共享
>
> 全局块还有对应的面板文件: `.../My Block/palette.json`，定义面板分组。
>
> 在全局块定义中使用 HashMap 格式，`color` 字段为 `"#RRGGBB"` 十六进制字符串（而非 int），`palette` 字段为面板分组索引字符串。
> 项目级 `custom_blocks` 文件使用 `ExtraBlockInfo` 格式，`color` 为 ARGB int。

### project_config 文件

JSON 对象（`HashMap<String, String>`），存储项目级附加配置。由 `ProjectSettings` 类管理。

```json
{
  "min_sdk": "21",
  "target_sdk": "33",
  "enable_bridgeless_themes": "true",
  "enable_viewbinding": "false",
  "app_class": "",
  "disable_old_methods": "false",
  "xml_command": "false"
}
```

| 键 | 说明 | 默认值 |
|----|------|--------|
| `min_sdk` | 最低 SDK 版本 | `21` |
| `target_sdk` | 目标 SDK 版本 | — |
| `enable_bridgeless_themes` | 使用完整 Material 主题（非 Bridge） | `false` |
| `enable_viewbinding` | 启用 ViewBinding | `false` |
| `app_class` | 自定义 Application 类名 | 空 |
| `disable_old_methods` | 禁用生成的废弃方法（如 `showMessage`） | `false` |
| `xml_command` | 使用新 XML 命令系统 | `false` |

### build_config 文件

JSON 对象（`HashMap<String, String>`），存储构建级配置。由 `BuildSettings` 类管理（继承 `ProjectSettings`）。

```json
{
  "dexer": "D8",
  "java_ver": "1.8",
  "no_http_legacy": "false",
  "no_warn": "false",
  "enable_logcat": "false"
}
```

| 键 | 说明 | 有效值 |
|----|------|--------|
| `dexer` | DEX 编译器 | `"D8"`, `"Dx"` |
| `java_ver` | Java 源/目标版本 | `"1.7"`, `"1.8"`, `"1.9"`, `"10"`, `"11"` |
| `no_http_legacy` | 不使用 HTTP legacy 库 | `"true"`, `"false"` |
| `no_warn` | 禁用编译警告 | `"true"`, `"false"` |
| `enable_logcat` | 启用 Logcat 日志 | `"true"`, `"false"` |
| `android_jar` | 自定义 android.jar 路径 | 文件路径 |
| `classpath` | 额外 classpath | 路径 |

### local_library 文件

JSON 数组，引用本地库。每个元素包含库的文件路径和依赖信息。

```json
[
  {
    "name": "retrofit-v2.9.0",
    "dependency": "com.squareup.retrofit2:retrofit:2.9.0",
    "packageName": "retrofit2",
    "jarPath": "/storage/emulated/0/.sketchware/libs/local_libs/retrofit-v2.9.0/classes.jar",
    "dexPath": "/storage/emulated/0/.sketchware/libs/local_libs/retrofit-v2.9.0/classes.dex",
    "resPath": "/storage/emulated/0/.sketchware/libs/local_libs/retrofit-v2.9.0/res",
    "manifestPath": "/storage/emulated/0/.sketchware/libs/local_libs/retrofit-v2.9.0/AndroidManifest.xml",
    "pgRulesPath": "/storage/emulated/0/.sketchware/libs/local_libs/retrofit-v2.9.0/proguard.txt",
    "assetsPath": "/storage/emulated/0/.sketchware/libs/local_libs/retrofit-v2.9.0/assets"
  }
]
```

| 字段 | 说明 |
|------|------|
| `name` | 库目录名（格式：`{artifactId}-v{version}`） |
| `dependency` | Gradle 依赖声明（生成 `implementation 'xxx'`），可选 |
| `packageName` | 库包名（从 `config` 文件读取） |
| `jarPath` | classes.jar 路径 |
| `dexPath` | classes.dex 路径 |
| `resPath` | 资源目录路径 |
| `manifestPath` | AndroidManifest.xml 路径 |
| `pgRulesPath` | ProGuard 规则文件路径 |
| `assetsPath` | assets 目录路径 |

> 所有路径字段仅在对应文件存在时才包含。

#### 库文件存储路径（两级策略）

下载库时采用 **主路径优先、回退路径兜底** 的策略：

| 路径 | 位置 | 特点 |
|------|------|------|
| **主路径** | `/storage/emulated/0/.sketchware/libs/local_libs/{name}/` | 卸载 app 后库文件保留 |
| **回退路径** | `Android/data/<pkg>/files/local_libs/{name}/` | 卸载 app 后库文件删除 |

**工作流程**：
1. 下载时先尝试主路径
2. 下载成功 → 使用主路径（大多数设备）
3. 下载失败（FUSE/EPERM） → 自动切换到 app-specific 路径并重试（Samsung Android 16+、Huawei Android 12+ 等）
4. 备份恢复同理：先尝试主路径，FUSE 阻止时回退（并清理空目录）
5. 读取/列出/编译时两个路径都检查，主路径优先

> **背景**: Android 16 的 Samsung 设备上，FUSE 虚拟文件系统会阻止在共享存储中创建 `classes.jar` 等可见文件，即使已授予 `MANAGE_EXTERNAL_STORAGE` 权限。app-specific 存储（`getExternalFilesDir()`）不经过 FUSE 审查，因此作为回退路径。

### Injection/ 目录

AndroidManifest 注入配置目录，包含以下文件：

```
Injection/
└── androidmanifest/
    ├── attributes.json           ← 属性注入（JSON 数组）
    ├── activity_launcher.txt     ← 启动 Activity（纯文本）
    ├── activities_components.json ← 各 Activity 的额外组件（JSON 数组）
    └── app_components.txt        ← 应用级组件（原始 XML 文本）
```

**attributes.json** — 每个元素是 `{name, value}` 对象：

```json
[
  {"name": "_application_attrs", "value": "android:theme=\"@style/AppTheme\""},
  {"name": "MainActivity", "value": "android:screenOrientation=\"portrait\""}
]
```

- `name` 为 `"_application_attrs"` 时注入到 `<application>` 标签
- `name` 为 Activity 名时注入到对应 `<activity>` 标签

**activity_launcher.txt** — 一行文本，指定启动 Activity 的 `fileName`（如 `main`）。默认为 `main`。

**activities_components.json** — 每个元素是 `{name, value}` 对象，用于在各 Activity 的 `<activity>` 标签**内部**插入额外组件（如 `<intent-filter>`、`<meta-data>`）：

```json
[
  {"name": "MainActivity", "value": "<intent-filter>\n<action android:name=\"android.intent.action.SEND\"/>\n<category android:name=\"android.intent.category.DEFAULT\"/>\n<data android:mimeType=\"text/plain\"/>\n</intent-filter>"}
]
```

- `name` 为 Activity 类名（含后缀 `Activity`，如 `MainActivity`）
- `value` 为原始 XML 文本，插入到对应 `<activity>` 标签内部（在 `</activity>` 之前）

> 与 `attributes.json` 的区别：`attributes.json` 注入的是**标签属性**（如 `android:screenOrientation="portrait"`），
> `activities_components.json` 注入的是**子元素**（如 `<intent-filter>`）。

**app_components.txt** — 原始 XML 文本，直接插入到 `<application>` 标签内部（如 `<service>`、`<receiver>` 等）。

### java/ 目录

存放额外的 Java 源文件。编译时这些文件会被复制到生成项目的源码目录中一起编译。

> 这些扩展文件**不是必需的**。如果只需要基本功能，只需创建 6 个核心文件即可。

---

## 相关源码

| 文件 | 说明 |
|------|------|
| `pro.sketchware.core.ProjectDataStore` | 数据序列化/反序列化 |
| `pro.sketchware.core.ProjectDataParser` | 节名解析和数据类型判定 |
| `pro.sketchware.core.EncryptedFileUtil` | 加密/解密工具 |
| `pro.sketchware.core.LibraryManager` | 库配置管理 |
| `pro.sketchware.core.BlockInterpreter` | 积木块 opCode → Java 代码转换 |
| `com.besome.sketch.beans.ViewBean` | 视图数据模型 |
| `com.besome.sketch.beans.BlockBean` | 积木块数据模型 |
| `com.besome.sketch.beans.EventBean` | 事件数据模型 |
| `com.besome.sketch.beans.ComponentBean` | 组件数据模型 |
