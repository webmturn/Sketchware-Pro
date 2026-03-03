# Sketchware-Pro UI 设计器深度分析报告

> 生成日期：2026-03-03
> 数据来源：`ViewEditorFragment.java`、`ViewEditor.java`、`ViewPane.java`、`LayoutGenerator.java`、`ViewBean.java`、`ViewHistoryManager.java`

---

## 一、整体架构

```
ViewEditorFragment (Fragment 协调器)
  ├── ViewEditor (RelativeLayout + OnTouchListener)  ← 拖拽控制器
  │     ├── PaletteWidget / PaletteFavorite          ← 左侧 widget 面板
  │     ├── ViewPane (RelativeLayout)                ← 实际 widget 层级持有者
  │     │     ├── rootLayout (ScrollContainer)       ← 可注入根布局
  │     │     └── ItemView[N]                        ← 每个 widget 的可视表示
  │     └── ViewDummy                                ← 拖拽幽灵图
  ├── ViewProperty                                   ← 右侧属性面板
  └── ViewHistoryManager                             ← 撤销/重做（MAX_HISTORY_STEPS 步）
```

**数据流**：`ViewBean`（JSON 存储）→ `ViewPane.createItemView()` → `ItemView`（编辑器预览）→ `LayoutGenerator.writeWidget()` → XML 文件

---

## 二、ViewBean 数据模型

### 2.1 类型常量体系

`ViewBean.type` 是整型常量，跨三个包定义：

| 包 | 常量范围 | 数量 | 示例 |
|---|---|---|---|
| `com.besome.sketch.beans.ViewBean` | 0–18 | 19 | LINEAR=0, BUTTON=3, TEXTVIEW=4, FAB=16 |
| `mod.agus.jcoderz.beans.ViewBeans` | 19–40+ | ~20 | CARDVIEW=19, TABLAYOUT=25, VIEWPAGER=26 |
| 总计 | 0–40+ | ~40 | |

核心常量（`ViewBean.java`）：

```java
VIEW_TYPE_LAYOUT_LINEAR = 0      // LinearLayout
VIEW_TYPE_LAYOUT_RELATIVE = 1    // RelativeLayout
VIEW_TYPE_LAYOUT_HSCROLLVIEW = 2 // HorizontalScrollView
VIEW_TYPE_WIDGET_BUTTON = 3      // Button
VIEW_TYPE_WIDGET_TEXTVIEW = 4    // TextView
VIEW_TYPE_WIDGET_EDITTEXT = 5    // EditText
VIEW_TYPE_WIDGET_IMAGEVIEW = 6   // ImageView
VIEW_TYPE_WIDGET_WEBVIEW = 7     // WebView
VIEW_TYPE_WIDGET_PROGRESSBAR = 8 // ProgressBar
VIEW_TYPE_WIDGET_LISTVIEW = 9    // ListView
VIEW_TYPE_WIDGET_SPINNER = 10    // Spinner
VIEW_TYPE_WIDGET_CHECKBOX = 11   // CheckBox
VIEW_TYPE_LAYOUT_VSCROLLVIEW = 12// ScrollView (vertical)
VIEW_TYPE_WIDGET_SWITCH = 13     // Switch
VIEW_TYPE_WIDGET_SEEKBAR = 14    // SeekBar
VIEW_TYPE_WIDGET_CALENDARVIEW = 15// CalendarView
VIEW_TYPE_WIDGET_FAB = 16        // FloatingActionButton
VIEW_TYPE_WIDGET_ADVIEW = 17     // AdView
VIEW_TYPE_WIDGET_MAPVIEW = 18    // MapView
VIEW_TYPE_COUNT = 19             // 核心类型总数
```

### 2.2 关键字段

| 字段 | 类型 | 用途 |
|---|---|---|
| `type` | int | widget 类型常量（决定编辑器预览和 XML 生成） |
| `id` | String | 视图 ID（如 "linear1"、"textview1"） |
| `convert` | String | XML 类名覆盖（空 = 用 ClassInfo 默认类名） |
| `inject` | String | 直接注入的额外 XML 属性字符串 |
| `parentAttributes` | HashMap | 父子关系属性（RelativeLayout 对齐等） |
| `layout` | LayoutBean | 布局参数（宽/高/margin/padding/gravity/weight） |
| `text` | TextBean | 文本相关属性 |
| `image` | ImageBean | 图片相关属性 |
| `classInfo` | ClassInfo | 类型元信息（类名、父类型匹配） |
| `alpha` | float | 透明度 |
| `checked` | int | CheckBox/Switch 选中状态 |
| `clickable` | int | 可点击标志 |

### 2.3 ClassInfo 的作用

`ClassInfo` 通过 `viewBean.getClassInfo()` 获取，提供：
- `getClassName()` — 返回完整 XML 标签名（如 `"LinearLayout"`、`"androidx.appcompat.widget.AppCompatButton"`）
- `matchesType(String)` — 类型继承匹配（如 `matchesType("TextView")` 对 EditText 也返回 true）

---

## 三、ViewEditor 拖拽系统

### 3.1 触发机制

```java
// ViewEditor.java:285
handler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout() / 2);
```

- **长按触发时间**：系统长按超时的 50%（约 150ms）
- 比系统默认更短，提升响应速度但增加误触风险

### 3.2 拖拽流程

```
长按 PaletteWidget/已有 widget
  → 创建 ViewDummy (半透明幽灵图)
  → 跟随手指移动
  → 实时高亮可插入位置（ViewPane 中的 ItemView 边界）
  → 松手：
      → 在 ViewPane 插入区域内 → 插入 widget
      → 在删除区域内 → 删除 widget
      → 其他区域 → 取消
```

### 3.3 ViewDummy

`ViewDummy` 是拖拽过程中显示的半透明占位视图：
- 显示被拖拽 widget 的简化外观
- 跟随手指位置实时移动
- 帮助用户预览放置位置

---

## 四、ViewPane 组件树管理

### 4.1 createItemView() — 类型分发

核心方法：将 `ViewBean.type` 映射到具体的 `ItemView` 实现类。

```java
public View createItemView(ViewBean viewBean) {
    View item = switch (viewBean.type) {
        case VIEW_TYPE_LAYOUT_LINEAR, VIEW_TYPE_LAYOUT_COLLAPSINGTOOLBARLAYOUT,
             VIEW_TYPE_LAYOUT_TEXTINPUTLAYOUT, ... -> new ItemLinearLayout(context);
        case VIEW_TYPE_LAYOUT_RELATIVE -> new ItemRelativeLayout(context);
        case VIEW_TYPE_WIDGET_BUTTON -> new ItemButton(context);
        case VIEW_TYPE_WIDGET_TEXTVIEW -> new ItemTextView(context);
        // ... 共 35+ 个 case
        default -> getUnknownItemView(viewBean);
    };
    item.setId(++viewIdCounter);
    item.setTag(viewBean.id);
    ((ItemView) item).setBean(viewBean);
    updateItemView(item, viewBean);
    return item;
}
```

**类型 → ItemView 映射表**（部分）：

| ViewBean.type | ItemView 类 | 真实 Android 控件 |
|---|---|---|
| LINEAR (0) | ItemLinearLayout | LinearLayout |
| RELATIVE (1) | ItemRelativeLayout | RelativeLayout |
| HSCROLLVIEW (2) | ItemHorizontalScrollView | HorizontalScrollView |
| BUTTON (3) | ItemButton | Button |
| TEXTVIEW (4) | ItemTextView | TextView |
| EDITTEXT (5) | ItemEditText | EditText |
| IMAGEVIEW (6) | ItemImageView | ImageView |
| WEBVIEW (7) | ItemWebView | WebView |
| PROGRESSBAR (8) | ItemProgressBar | ProgressBar |
| LISTVIEW (9) | ItemListView | ListView |
| SPINNER (10) | ItemSpinner | Spinner |
| CHECKBOX (11) | ItemCheckBox | CheckBox |
| VSCROLLVIEW (12) | ItemVerticalScrollView | ScrollView |
| SWITCH (13) | ItemSwitch | Switch |
| SEEKBAR (14) | ItemSeekBar | SeekBar |
| CALENDARVIEW (15) | ItemCalendarView | CalendarView |
| FAB (16) | ItemFloatingActionButton | FloatingActionButton |
| CARDVIEW (19) | ItemCardView | CardView |
| RECYCLERVIEW (21) | ItemRecyclerView | RecyclerView |
| TABLAYOUT (25) | ItemTabLayout | TabLayout |
| VIEWPAGER (26) | ItemViewPager | ViewPager |
| BOTTOMNAVIGATION (27) | ItemBottomNavigationView | BottomNavigationView |

### 4.2 updateItemView() — 属性同步

当用户修改属性时，`updateItemView()` 将 `ViewBean` 属性应用到编辑器预览：
- 设置宽高、margin、padding
- 应用背景颜色/图片
- 设置文本内容、字体大小、颜色
- 设置图片源
- 应用可见性

### 4.3 viewIdCounter

```java
private int viewIdCounter = 99;
```

从 99 开始递增，用于 Android `View.setId()` 预览。每次打开设计器重置。

---

## 五、LayoutGenerator XML 生成系统

### 5.1 XML 标签名来源

```java
// LayoutGenerator.java:293-294
XmlBuilder widgetTag = convert.isEmpty()
    ? new XmlBuilder(viewBean.getClassInfo().getClassName())  // ClassInfo 提供类名
    : new XmlBuilder(convert.replaceAll(" ", ""));            // convert 字段覆盖
```

**优先级**：`viewBean.convert` > `ClassInfo.getClassName()`

### 5.2 属性生成三层机制

| 层级 | 机制 | 说明 |
|---|---|---|
| 1. 默认属性 | `writeWidget()` 内部逻辑 | 根据 `matchesType()` 写入标准属性 |
| 2. 属性注入 | `InjectAttributeHandler(viewBean)` | 读取 `viewBean.inject` 追加自定义属性 |
| 3. 属性排除 | `readAttributesToReplace(viewBean)` | 排除被自定义属性覆盖的默认属性 |

### 5.3 writeWidget() 属性分发链

```
writeWidget()
  ├── android:id
  ├── android:layout_width / layout_height
  ├── writeLayoutMargin()
  ├── writeViewPadding() / writeCardViewPadding()
  ├── writeBackgroundResource()
  ├── writeViewGravity()        ← ViewGroup 类型
  ├── android:orientation       ← LinearLayout 类型
  ├── android:weightSum         ← LinearLayout 类型
  ├── writeTextAttributes()     ← TextView 子类
  ├── writeImgSrcAttr()         ← ImageView 子类
  ├── writeImageScaleType()     ← ImageView 子类
  ├── writeSpinnerAttributes()  ← Spinner 类型
  ├── writeListViewAttributes() ← ListView 类型
  └── writeAdViewAttributes()   ← AdView 类型
```

### 5.4 writeBackgroundResource() — 背景色逻辑

背景色处理根据 widget 类型有不同分支：
- **CollapsingToolbarLayout / AppBarLayout** → `app:contentScrim`
- **BottomAppBar** → `android:backgroundTint="@android:color/transparent"`
- **CardView** → `app:cardBackgroundColor`
- **其他** → `android:background`

支持颜色资源引用（`@color/xxx`）、`?attr/` 引用、和直接十六进制色值。

---

## 六、已存在的扩展机制

| 机制 | 字段/类 | 用途 | 使用场景 |
|---|---|---|---|
| XML 类名覆盖 | `viewBean.convert` | 将 widget 渲染为任意 XML 标签 | 自定义控件、include 标签 |
| 自定义属性注入 | `viewBean.inject` | 追加任意 XML 属性字符串 | 非标准属性（如 `app:layout_scrollFlags`） |
| 父属性覆盖 | `viewBean.parentAttributes` | RelativeLayout 对齐等父子属性 | `layout_alignParentTop` 等 |
| 根布局注入 | `InjectRootLayoutManager` | 自定义根布局 | CoordinatorLayout、DrawerLayout 等 |
| Widget 集合 | `WidgetsCreatorManager` | 预设 widget 组合 | 用户收藏的控件组合（非新类型） |

### 6.1 convert 字段示例

```json
{
  "type": 0,
  "id": "my_toolbar",
  "convert": "androidx.appcompat.widget.Toolbar"
}
```

生成 XML：`<androidx.appcompat.widget.Toolbar android:id="@+id/my_toolbar" ...>`

### 6.2 inject 字段示例

```json
{
  "inject": "app:layout_scrollFlags=\"scroll|enterAlways\"\napp:elevation=\"4dp\""
}
```

直接附加到 XML 标签的属性列表中。

### 6.3 InjectRootLayoutManager

替换默认的 `LinearLayout` 根布局为其他容器：
- `CoordinatorLayout` + `AppBarLayout` 组合
- `DrawerLayout`
- `SwipeRefreshLayout`

---

## 七、ViewHistoryManager 撤销/重做系统

### 7.1 基本机制

```java
public static int MAX_HISTORY_STEPS = 50;  // 可配置上限

public final void addHistoryEntry(String key, HistoryViewBean historyViewBean) {
    ArrayList<HistoryViewBean> entries = historyMap.get(key);
    entries.add(historyViewBean);
    if (entries.size() > MAX_HISTORY_STEPS) {
        entries.remove(0);  // 超出上限移除最旧条目
    } else {
        incrementPosition(key);
    }
}
```

### 7.2 记录的操作类型

| 方法 | 记录内容 |
|---|---|
| `recordAdd()` | 添加单个 widget |
| `recordAddMultiple()` | 添加多个 widget（粘贴、从集合添加） |
| `recordRemove()` | 删除 widget |
| `recordMove()` | 移动 widget 位置 |
| `recordModify()` | 修改 widget 属性 |

### 7.3 与 BlockHistoryManager 的对比

| 特性 | ViewHistoryManager | BlockHistoryManager |
|---|---|---|
| 上限 | `MAX_HISTORY_STEPS = 50` | `MAX_HISTORY_STEPS = 50` |
| 键 | 布局文件名 | 事件名 |
| 数据 | `HistoryViewBean` | `HistoryBlockBean` |
| 模式 | 完全相同 | 完全相同 |

---

## 八、能力矩阵

| 功能 | 状态 | 说明 |
|---|---|---|
| 拖拽新增 widget | ✅ | 长按 ~150ms 触发 |
| 移动/删除 widget | ✅ | 拖到删除区域或使用菜单 |
| 属性编辑 | ✅ | `PropertyActivity` 独立页面 |
| 撤销/重做 | ✅ | 选项菜单触发，MAX_HISTORY_STEPS 步 |
| Widget 收藏集合 | ✅ | 组合已有 widgets，非新类型 |
| 自定义 XML 类名 | ✅ | `viewBean.convert` 字段 |
| 自定义 XML 属性注入 | ✅ | `viewBean.inject` 字段 |
| 自定义根布局 | ✅ | `InjectRootLayoutManager` |
| **新 widget 类型扩展** | ❌ | 需改 5+ 处代码（硬编码 switch） |
| **XML 导入** | ❌ | 无反向 XML→ViewBean 解析 |
| **ConstraintLayout 完整支持** | ❌ | 缺少约束编辑 UI |
| **撤销步数运行时可调** | ⚠️ | 常量已提取，但尚无 UI 设置入口 |
| 未知类型安全降级 | ✅ | 已修复（不再修改原始 ViewBean.type） |

---

## 九、架构限制详解

### 9.1 ⭐⭐⭐ 添加新 widget 需改 5+ 处代码

**现状**：添加一个新 widget 类型需要修改：

1. `ViewBeans.java` — 添加新整型常量
2. `ViewPane.createItemView()` — 添加 switch case
3. 新建 `Item*.java` — 编辑器预览视图类
4. `ClassInfo` — 注册 type → 类名映射
5. `ViewProperty` / `PropertyActivity` — 属性编辑支持
6. （可选）`LayoutGenerator.writeWidget()` — 特殊属性处理

**改进方案**：引入 `WidgetTypeDescriptor` 接口 + 注册表：

```java
public interface WidgetTypeDescriptor {
    int getTypeId();
    String getXmlClassName();
    View createEditorView(Context context);
    void writeXmlAttributes(XmlBuilder tag, ViewBean bean);
    void applyPreviewAttributes(View itemView, ViewBean bean);
}

public class WidgetRegistry {
    private static final Map<Integer, WidgetTypeDescriptor> registry = new HashMap<>();
    public static void register(WidgetTypeDescriptor desc) {
        registry.put(desc.getTypeId(), desc);
    }
    public static WidgetTypeDescriptor get(int type) {
        return registry.get(type);
    }
}
```

**评估**：高工作量重构，需触及全系统。建议在大版本重构时统一处理。

### 9.2 ⭐⭐⭐ 属性知识双重维护

**现状**：同一 widget 的属性处理逻辑在两处维护：
- `ViewPane.updateItemView()` — 编辑器预览应用属性
- `LayoutGenerator.writeWidget()` — XML 生成写出属性

**风险**：添加新属性时必须同步两处，容易遗漏。

**改进方向**：声明式属性描述（注解或配置文件），由统一引擎同时驱动预览和 XML 生成。

### 9.3 ⭐⭐ 无 XML 导入功能

**现状**：`LayoutGenerator` 有 `toXmlString()`（ViewBean → XML），但无反向 `fromXmlString()`（XML → ViewBean）。

`LayoutGenerator.java` 中有 `XmlPullParser` 相关 import，但无实际 XML 导入逻辑。

**改进方案**：
1. 实现 `XmlLayoutParser`，使用 `XmlPullParser` 解析 XML
2. 将 XML 标签名反向映射到 `ViewBean.type`（通过 ClassInfo 反查）
3. 提取属性并填入 `ViewBean` 各字段
4. 处理嵌套结构为父子关系

### 9.4 ⭐⭐ 无 ConstraintLayout 支持

`ViewPane.createItemView()` 无 ConstraintLayout case。用户只能通过 `InjectRootLayoutManager` 设置为根布局，但子视图的 `app:layout_constraint*` 属性无法在 UI 中设置。

**评估**：ConstraintLayout 支持需要全新的约束编辑 UI（类似 Android Studio 的约束拖拽），工作量巨大。

### 9.5 ⭐ 拖拽长按触发时间偏短

系统长按超时的 50%（约 150ms），与 Logic Editor 使用相同策略。优点是响应快，缺点是偶发误触。

---

## 十、已修复的问题

### 10.1 getUnknownItemView() 数据损坏 Bug

**修复前**（数据损坏）：
```java
private View getUnknownItemView(ViewBean bean) {
    bean.type = ViewBean.VIEW_TYPE_LAYOUT_LINEAR;  // 静默覆盖原始类型！
    return new ItemLinearLayout(context);
}
```

**修复后**（安全降级）：
```java
private View getUnknownItemView(ViewBean bean) {
    LogUtil.w("ViewPane", "Unknown widget type " + bean.type
        + " for view '" + bean.id + "'; showing as LinearLayout placeholder");
    return new ItemLinearLayout(context);
}
```

**影响**：用户在新版本中创建的布局，在旧版本中打开后不再丢失 widget 类型信息。

### 10.2 ViewHistoryManager / BlockHistoryManager 步数硬编码

**修复前**：`if (entries.size() > 50)` 硬编码

**修复后**：`public static int MAX_HISTORY_STEPS = 50;` + `if (entries.size() > MAX_HISTORY_STEPS)` 命名常量，两个管理器统一模式。

---

## 十一、总结与建议优先级

| 优先级 | 改进 | 工作量 | 状态 |
|---|---|---|---|
| 🔴 关键 | `getUnknownItemView()` 数据损坏修复 | 2 行 | ✅ 已修复 |
| 🟡 中等 | 历史步数命名常量化 | 5 行/文件 | ✅ 已修复 |
| 🟡 中等 | XML 导入功能 | ~500 行新代码 | 待实施 |
| 🟠 长期 | WidgetTypeDescriptor 注册表 | 大型重构 | 建议大版本时处理 |
| 🟠 长期 | 声明式属性系统 | 大型重构 | 建议与注册表一起处理 |
| 🔵 远期 | ConstraintLayout 完整支持 | 极大 | 需约束编辑 UI |
