# JAR 源码集成计划

## 目标
将 `run_x/` 中反编译并修复的 72 个 Java 源文件集成到主项目，替代 3 个 JAR 依赖。

## 分支
- 工作分支: `integrate-jar-sources`
- 主分支: `main` (不受影响，可随时回退)

---

## Phase 1: 移入 a.a.a 包文件

**源目录:**
- `run_x/a.a.a-important-classes.jar.src/a/a/a/` (3 文件)
- `run_x/a.a.a-notimportant-classes.jar.src/a/a/a/` (65 文件)

**目标目录:** `app/src/main/java/a/a/a/`

**跳过的文件 (主源码已有完整实现):**
- `mB.java` — UIHelper，主源码版本更完整
- `WB.java` — WidgetIdValidator，主源码版本更完整

**实际移入:** 66 个文件

**移入文件清单:**
```
# a.a.a-important (3 文件)
eC.java, lq.java, Rs.java

# a.a.a-notimportant (63 文件，跳过 mB, WB)
bC.java, by.java, cC.java, Cx.java, cy.java, DB.java, dC.java,
FB.java, fC.java, GB.java, gC.java, HB.java, hC.java, hI.java,
iB.java, iC.java, iI.java, Iw.java, jB.java, jv.java, Jw.java,
KB.java, kC.java, Kw.java, Lp.java, Lw.java, Mp.java, nA.java,
nB.java, Np.java, nv.java, oB.java, Op.java, PB.java, Pp.java,
QB.java, Qp.java, Qs.java, ro.java, Rp.java, rq.java, Sp.java,
Ss.java, Ts.java, tw.java, ty.java, uB.java, Us.java, Uu.java,
uw.java, vB.java, vq.java, Vs.java, vw.java, ww.java, xB.java,
xq.java, xw.java, YA.java, yB.java, yy.java, zB.java, zy.java
```

## Phase 2: 移入 com.besome.sketch 文件

**源目录:** `run_x/com.besome.sketch-classes.jar.src/com/besome/sketch/`

| 文件 | 目标目录 |
|------|---------|
| `BlockPane.java` | `app/src/main/java/com/besome/sketch/editor/logic/` |
| `CircleImageView.java` | `app/src/main/java/com/besome/sketch/lib/ui/` |
| `CustomHorizontalScrollView.java` | `app/src/main/java/com/besome/sketch/lib/ui/` |
| `CustomScrollView.java` | `app/src/main/java/com/besome/sketch/lib/ui/` |

**前置检查:** 确认目标目录中无同名文件

## Phase 3: Wrapper 兼容性 (无需操作)

8 个 JAR wrapper 类均不在 run_x 中，无冲突:
- `qA.java` (extends BaseFragment)
- `wq.java` (extends SketchwarePaths)
- `jC.java` (extends ProjectDataManager)
- `mq.java` (extends ComponentTypeMapper)
- `kq.java` (extends BlockColorMapper)
- `jq.java` (extends BuildConfig)
- `kv.java` (extends FirebasePreviewView)
- `Gx.java` (extends ClassInfo)

这些 wrapper 保留不动。run_x 源码通过 wrapper 名 (qA, wq 等) 引用，
实际调用链: run_x 类 → wrapper → 重命名后的父类 → 实际实现。

## Phase 4: 删除 JAR 文件

```
app/libs/a.a.a-important-classes.jar
app/libs/a.a.a-notimportant-classes.jar
app/libs/com.besome.sketch-classes.jar
```

**注意:** 不要删除其他 JAR (android-svg.jar 等)

## Phase 5: 编译验证

```bash
gradlew assembleDebug 2>&1 | tee build.log
```

### 可能的错误类型及处理:
1. **重复类定义** — JAR 未正确删除，或源码中有多份
2. **找不到符号** — import 路径问题，需检查包名
3. **泛型类型不匹配** — 源码泛型与调用方不一致
4. **方法签名不匹配** — 源码方法签名与 JAR 调用方期望不同

## Phase 6: 修复编译错误

逐个修复 Phase 5 发现的错误，预计:
- import 调整
- 泛型类型补全
- 可能的方法签名调整

## Phase 7: 提交

```bash
git add -A
git commit -m "feat: integrate JAR sources into main project

- Move 66 a.a.a files + 4 com.besome files from run_x to main source tree
- Delete 3 JAR dependencies (a.a.a-important, a.a.a-notimportant, com.besome.sketch)
- Keep 8 JAR wrapper classes for binary compatibility
- Skip mB.java, WB.java (main source has complete implementations)"
```

## 回退方案

```bash
# 如果集成失败，回退到主分支:
git checkout main

# 如果需要删除集成分支:
git branch -D integrate-jar-sources
```

## 风险评估

| 风险 | 等级 | 缓解措施 |
|------|------|---------|
| 重复类定义 | 低 | 删除 JAR 后不会重复 |
| Wrapper 不兼容 | 低 | Wrapper 类不在 run_x 中 |
| 方法签名不匹配 | 中 | 编译验证 + 逐个修复 |
| 运行时崩溃 | 中 | 需要实际运行测试 |
| com.besome 冲突 | 低 | 已检查无同名文件 |
