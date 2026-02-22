# JAR 反编译文件分析报告

## 总览
- **总文件数**: 71 个 Java 文件
- **来源**: 3 个 JAR (a.a.a-important 3个, a.a.a-notimportant 64个, com.besome.sketch 4个)
- **总代码行数**: ~22,000 行
- **已有源码**: mB, WB (已存在于 main source tree)

## 反编译质量评估

### ⚠️ 含原始字节码（未完全反编译）的文件
这些文件中部分方法只有 `// Byte code:` 注释，需要手动重写：

| 文件 | 行数 | 字节码方法数 | 角色 |
|------|------|-------------|------|
| **Rs** | 1494 | ~5 | Block 视图（核心） |
| **Ts** | 1057 | ~3 | Block 基类视图 |
| **Ss** | 317 | ~1 | 参数输入视图 |
| **Us** | 348 | ~1 | MoreBlock 视图 |
| **eC** | 1837 | 0 | 项目数据管理器 |
| **lq** | 6550 | 0 | 块参数注册表（巨大switch） |
| **xB** | 1527 | ~3 | 块规格/翻译管理 |
| **gC** | 1032 | ~3 | 逻辑数据解析器 |
| **KB** | 918 | ~5 | ZIP 解压工具 |
| **oB** | 766 | ~2 | 加密文件读写 |
| **kC** | 950 | ~2 | 资源管理器(图片/声音/字体) |
| **hC** | 436 | ~1 | 项目文件管理器 |
| **iC** | 308 | ~1 | 库管理器 |
| **HB** | 263 | ~2 | URI 路径解析 |
| **Cx** | 110 | ~1 | 最近使用记录(MRU) |
| **Mp** | 178 | ~1 | 块集合管理器(singleton) |
| **Np** | 344 | ~1 | 图片集合管理器 |
| **Op** | 444 | ~1 | 声音集合管理器 |
| **Pp** | 171 | ~1 | MoreBlock 集合管理器 |
| **Qp** | 347 | ~1 | 字体集合管理器 |
| **Rp** | 217 | ~1 | 控件集合管理器 |
| **BlockPane** | 1032 | ~3 | 块编辑面板 |

### ✅ 完整反编译（可直接使用）的文件

#### 接口类（简单，直接可用）
| 文件 | 行数 | 推荐名称 | 用途 |
|------|------|----------|------|
| Vs | 8 | BlockSizeListener | 接口: a(int, int) |
| Qs | 9 | EventCallback | 接口: a(EventBean) |
| Iw | 11 | ViewEditorCallback | 接口: a(), a(String, ViewBean) |
| Jw | 8 | StringCallback | 接口: a(String) |
| Kw | 8 | PropertyChangeListener | 接口: a(String, Object) |
| Lw | 9 | ViewBeanCallback | 接口: a(ViewBean) |
| YA | 8 | IntCallback | 接口: a(int) |
| by | 9 | FileCallback | 接口: a(int, ProjectFileBean) |
| cy | 12 | DownloadCallback | 接口: a(), a(String), a(boolean, String) |
| ty | 10 | ScrollController | 接口: a(), setChildScrollEnabled(boolean) |
| nv | 15 | LibrarySettingsView | 接口: Firebase 库设置 |
| Uu | 15 | LibraryDataView | 接口: Firebase 数据视图 |

#### 工具类（完整反编译）
| 文件 | 行数 | 推荐名称 | 用途 |
|------|------|----------|------|
| DB | 101 | SharedPrefsHelper | SharedPreferences 封装 |
| FB | 218 | StringUtils | 格式化、随机数、hex、剪贴板、块spec解析 |
| GB | 194 | DeviceUtils | 设备信息(CPU、屏幕、网络、版本) |
| vB | 16 | GsonMapHelper | HashMap<String,Object> 的 Gson 序列化 |
| uB | 8 | MapTypeToken | TypeToken<HashMap<String,Object>> |
| yB | 35 | MapValueHelper | Map 取值工具(安全类型转换) |
| iB | 182 | BitmapUtils | 图片缩放、旋转、EXIF 处理 |
| zB | 187 | NinePatchUtils | 9-patch 图片处理 |
| nB | 83 | DateTimeUtils | 日期格式化/解析 |
| nA | 26 | ReflectionToString | 反射 toString |
| Sp | 23 | ThrottleTimer | 30秒节流计时器 |
| vq | 23 | ActivityThemeConfig | 主题/方向/键盘配置 |
| xq | 22 | VersionChecker | 版本号范围验证(200-600) |
| ro | 23 | UserLevelConfig | 用户等级配置 |
| jB | 15 | EnableRunnable | View.setEnabled(true) Runnable |
| yy | 21 | BuildException | 构建异常(带详细列表) |
| zy | 10 | CompileException | 编译异常 |

#### 验证器（完整反编译）
| 文件 | 行数 | 推荐名称 | 用途 |
|------|------|----------|------|
| PB | 159 | FileNameValidator | 文件名验证(小写+下划线) |
| QB | 167 | XmlNameValidator | XML名称验证(保留字检查) |
| WB | 100 | WidgetIdValidator | 控件ID验证 |

#### 历史/撤销管理
| 文件 | 行数 | 推荐名称 | 用途 |
|------|------|----------|------|
| bC | 202 | BlockHistoryManager | 块操作历史(undo/redo) |
| cC | 185 | ViewHistoryManager | 视图操作历史 |

#### 数据管理
| 文件 | 行数 | 推荐名称 | 用途 |
|------|------|----------|------|
| Lp | 217 | BaseCollectionManager | 集合管理基类(抽象) |
| rq | 261 | DefaultViewFactory | 默认视图/布局模板工厂 |
| iI | 145 | KeyStoreManager | 签名密钥生成/管理 |
| hI | 15 | KeyStoreOutputStream | iI 的 OutputStream 适配 |

#### UI 辅助/事件类（xw 的内部类相关）
| 文件 | 行数 | 推荐名称 | 用途 |
|------|------|----------|------|
| xw | 363 | ViewManagerFragment | 视图管理Fragment(RecyclerView) |
| tw | 24 | ViewScrollListener | xw的滚动监听 |
| uw | 27 | ViewClickListener | xw的点击监听 |
| vw | 26 | ViewLongClickListener | xw的长按监听 |
| ww | 34 | ViewPresetClickListener | xw的预设点击 |
| jv | 15 | FirebaseToggleListener | kv的点击切换监听 |
| mB | 106 | UIHelper | 代码高亮/防抖/键盘/动画 |

#### com.besome.sketch 包
| 文件 | 行数 | 推荐名称 | 用途 |
|------|------|----------|------|
| BlockPane | 1032 | (已命名) | 块编辑画布 |
| CircleImageView | 55 | (已命名) | 圆形头像 ImageView |
| CustomHorizontalScrollView | 59 | (已命名) | 可禁用滚动的HScrollView |
| CustomScrollView | 42 | (已命名) | 可禁用滚动的ScrollView |

#### 空类（仅包名声明）
| 文件 | 说明 |
|------|------|
| dC | 空（可能是标记类或被移除） |
| fC | 空（同上） |

## 关键依赖关系图

```
BlockPane
  └─ Rs (Block视图) extends Ts (Block基类视图)
       ├─ Ss (参数输入视图) extends Ts
       ├─ Us (MoreBlock视图) extends Rs
       └─ kq (BlockColorMapper) ← Ss.a() 调用
  
eC (项目数据管理)
  ├─ hC (文件管理)
  ├─ iC (库管理)
  ├─ kC (资源管理)
  └─ oB (文件IO)

Lp (集合基类)
  ├─ Mp (块集合)
  ├─ Np (图片集合)
  ├─ Op (声音集合)
  ├─ Pp (MoreBlock集合)
  ├─ Qp (字体集合)
  └─ Rp (控件集合)

xB (块规格管理)
  └─ lq (块参数注册表)

gC (逻辑数据解析)

xw (视图管理Fragment)
  ├─ tw, uw, vw, ww (内部监听类)
  └─ 依赖 qA(BaseFragment), jC, wq
```

## 难度分级 & 建议优先级

### 🟢 Phase 1: 简单类（直接可用，仅需重命名）~20个文件
- 所有接口类 (12个)
- 空类 dC, fC
- 小工具: jB, uB, vB, yB, Sp, nA, vq, xq, ro, yy, zy
- com.besome.sketch UI 类 (3个)

### 🟡 Phase 2: 中等类（完整反编译，需重命名字段/方法）~15个文件
- DB, FB, GB, nB, iB, zB
- PB, QB, WB (验证器)
- bC, cC (历史管理)
- rq, Lp
- hI, jv

### 🔴 Phase 3: 复杂类（含字节码，需手动重写方法）~15个文件
- Cx, HB, KB, oB
- hC, iC, kC
- Mp, Np, Op, Pp, Qp, Rp
- iI, xw, tw/uw/vw/ww

### ⚫ Phase 4: 核心巨型类（最复杂，含大量字节码）~7个文件
- **Ts** (1057行) - 块基类视图，绘制逻辑
- **Rs** (1494行) - 块视图，布局算法
- **Ss** (317行) - 参数输入视图
- **Us** (348行) - MoreBlock 视图
- **eC** (1837行) - 项目数据管理
- **lq** (6550行) - 块参数注册表
- **xB** (1527行) - 块规格管理
- **gC** (1032行) - 逻辑数据解析
- **BlockPane** (1032行) - 块编辑面板

## 约束
- 这些类名**不能改变**，因为其他 JAR 文件通过二进制方式引用它们
- 只能重命名字段和方法名（public字段也可能被JAR引用，需谨慎）
- 字节码方法需要手动理解并用 Java 重写
