# 中文国际化补全

本次将确认遗漏的应用界面文案接入 Android 字符串资源，并补齐中文。范围包含菜单、签名选项、输入校验、变量分类、库下载和管理、编译进度、备份错误、事件说明、主题及布局预设名称，以及复核发现的属性选项、颜色分组、积木折叠数量和批量图片数量提示。继续排查覆盖了依赖库文件选择器、数组类型、默认字体和编辑器主题、APK 摘要错误及导出密钥库错误提示。

## 资源覆盖

- 补齐 575 项已有资源的中文，包括普通积木和事件说明片段、错误及提示。
- 新增 636 项默认及中文字符串，其中 308 项用于内置扩展积木的主说明和分支说明。首次复核追加 77 项字符串及 1 项积木折叠数量复数资源；继续排查追加 29 项字符串。
- 修正 14 项中文资源中的英文显示标签，其中最新 5 项为 Java/Kotlin 活动、DEX 编译器及字符串/颜色空列表提示。
- 补全后默认字符串资源有 4357 项，中文字符串资源有 3763 项。剩余 594 项差异为 563 个空字符串、10 个符号或数字，以及 21 个网址、类名、品牌或标识。

新增字符串放在 `app/src/main/res/values/strings_i18n.xml` 和 `values-zh/strings_i18n.xml`；复数资源放在对应目录的 `plurals_i18n.xml`。依赖组件文案放在对应目录的 `strings_dependency_i18n.xml`，包含文件选择器的 16 项遗漏文案、Material 导航栏的 2 项无障碍标签及 Firebase 的通知分组名称；文件选择器的取消按钮沿用已有中文资源。资源统计针对 XML 字符串，排查另覆盖固定选项表、动态计数和颜色分组的显示路径，并读取 Gradle 合并资源清单以检查依赖库文案。

## 数据兼容性

内置扩展积木通过 `BuiltinBlockLocalization` 在渲染和搜索时翻译。原始定义、保存的 spec、操作码、参数标记及顺序保持原值。只有操作码和原始说明都匹配内置定义时才使用译文，用户修改的说明直接显示原文。

`LocalizedDisplayNames` 为变量类型、主题和预设提供显示映射，保留项目导入、代码生成和预设匹配使用的名称。变量和参数积木仅根据操作码、积木类型与规范类型名翻译内置 Map/列表标签，Java 类名及收藏中的类型名保留原文。选择器标签按内置选择器 ID 翻译，自定义选择器名称及其默认提示保留原文，即使名称为 `Image`、`Color`、`Default` 或其他内置标签也不会误翻译。默认 `typeview` 选择器仅在标题仍为原始默认值时翻译，用户自定义标题按原值显示。

`PropertyDisplayNames` 在属性摘要与选项行中翻译星期、方向、字体、输入类型、输入法操作、选择模式、进度条样式、图片缩放方式、广告尺寸及布尔选项，也处理尺寸和对齐摘要。选项的 tag、控件返回值、固定选项表和代码生成使用的标识保持原值；未知属性值直接显示原值。

`ColorDisplayNames` 仅翻译内置色板名称和 Material 属性分组标题。用户定义的颜色资源名称、属性名称、十六进制颜色和 `TRANSPARENT` / `NONE` 颜色编码保持原值，即使用户的资源名称与内置颜色名称相同，也不翻译资源标识。

数组编辑器的创建、编辑和列表使用本地化类型标签；选择控件的 tag 保存 `STRING` / `INTEGER` / `OBJECT` 枚举，保存操作直接使用该枚举。XML 输出仍为 `string-array` / `integer-array` / `array`，数组名称和用户条目保留原值。编辑器默认主题和默认字体仅替换显示标签，主题下标和 `default_font` 标识保持原值。

语言或导入翻译变化时，积木和事件说明缓存重新加载。带前后空格的资源片段使用 Android 字符串引号语法保留空格。

布局预览示例、用户填写的文案、网址、品牌和 Java/XML 标识保留原值。未启用的 `CompileQuizManager` 题库不在当前界面范围内。

## 验证

- 默认与中文 XML 资源无重复键，新增资源均有中文对应值。
- 308 条内置说明的参数类型、数量及顺序保持一致。
- 调试 APK 和测试 APK 编译成功，100 项 JVM 单元测试通过。
- 已连接 Android 设备上的 20 项测试通过：包括中文资源读取、实际积木渲染与搜索、语言切换缓存，以及自定义积木新增、插入、编辑和参数转义。
- 新增的 5 项设备测试覆盖实际属性选项和摘要、原始存储值、颜色选择回调与资源名称、批量图片数量提示、积木双分支折叠，以及英文数量提示的单复数形式。
- 继续排查新增的 3 项设备测试覆盖实际文件选择器的文件/文件夹/混合模式、取消按钮和全部排序菜单，验证选中文件返回原始路径；同时验证数组类型标签、枚举与 XML 往返，以及 APK 摘要错误的中英文显示。语言切换测试使用独立的内存翻译表，避免设备已有积木翻译文件干扰测试；不改动该文件。
- 误翻译修复新增 2 项设备回归测试，验证与内置标签同名的自定义类名、参数和收藏积木标签、选择器名称保留原文，并验证内置 Map/列表及图片/颜色选择器仍显示中文。修复后重新构建两个 APK，100 项 JVM 单元测试和 `BuiltinBlockLocalizationTest` 的全部 8 项设备测试通过。

构建命令：

```powershell
.\gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
```

设备测试类为 `RemainingDisplayLocalizationTest`、`EditorDisplayLocalizationTest`、`BuiltinBlockLocalizationTest` 和 `BlockSpecValidationTest`。本地缺少 Gradle UTP 的离线依赖时，可以安装这两个 APK 后用 Android 自带的运行器执行：

```powershell
adb shell am instrument -w -r -e class pro.sketchware.util.locale.RemainingDisplayLocalizationTest,pro.sketchware.util.locale.EditorDisplayLocalizationTest,pro.sketchware.core.codegen.BuiltinBlockLocalizationTest,pro.sketchware.core.codegen.BlockSpecValidationTest pro.sketchware.test/androidx.test.runner.AndroidJUnitRunner
```
