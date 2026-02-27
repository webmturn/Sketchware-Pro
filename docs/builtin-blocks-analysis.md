# Sketchware-Pro 内置 Block 分析报告

> 生成日期：2026-02-27
> 数据来源：`BlockInterpreter.java`（代码生成）、`BlockSpecRegistry.java`（spec/参数定义）

---

## 一、现有内置 Block 完整清单

共 **174 个** 内置 opcode（不含运算符 `+` `-` `*` `/` `%` `>` `<` `=` `&&` `||`）。

### 1. 控制流（6 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `if` | 条件判断 | `if (cond) { ... }` |
| `ifElse` | 条件分支 | `if (cond) { ... } else { ... }` |
| `forever` | 无限循环 | `while(true) { ... }` |
| `repeat` | 计数循环 | `for(int _repeatN = 0; _repeatN < count; _repeatN++) { ... }` |
| `break` | 跳出循环 | `break;` |
| `not` | 逻辑非 | `!value` |

### 2. 变量操作（8 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `setVarBoolean` | 设置布尔变量 | `var = value;` |
| `setVarInt` | 设置数值变量 | `var = value;` |
| `setVarString` | 设置字符串变量 | `var = value;` |
| `increaseInt` | 数值自增 | `var++;` |
| `decreaseInt` | 数值自减 | `var--;` |
| `getVar` | 获取变量 | `varName` |
| `getArg` | 获取 MoreBlock 参数 | `_paramName` |
| `getResStr` | 获取字符串资源 | `Helper.getResString(R.string.xxx)` |

### 3. 逻辑/布尔值（2 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `true` | 真 | `true` |
| `false` | 假 | `false` |

### 4. 运算符（9 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `+` `-` `*` `/` `%` | 算术运算 | `a op b` |
| `>` `<` `=` | 比较运算 | `a op b`（`=` 生成 `==`） |
| `&&` `\|\|` | 逻辑运算 | `a op b` |
| `random` | 随机数 | `SketchwareUtil.getRandom(min, max)` |

### 5. 字符串操作（16 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `stringLength` | 长度 | `str.length()` |
| `stringJoin` | 拼接 | `str1.concat(str2)` |
| `stringIndex` | 查找位置 | `str.indexOf(sub)` |
| `stringLastIndex` | 最后位置 | `str.lastIndexOf(sub)` |
| `stringSub` | 截取 | `str.substring(from, to)` |
| `stringEquals` | 相等 | `str1.equals(str2)` |
| `stringContains` | 包含 | `str1.contains(str2)` |
| `stringReplace` | 替换（首个） | `str.replace(old, new)` |
| `stringReplaceFirst` | 正则替换首个 | `str.replaceFirst(regex, new)` |
| `stringReplaceAll` | 正则替换全部 | `str.replaceAll(regex, new)` |
| `toNumber` | 转数值 | `Double.parseDouble(str)` |
| `toString` | 数值转字符串（无小数） | `String.valueOf((long)(num))` |
| `toStringWithDecimal` | 数值转字符串（含小数） | `String.valueOf(num)` |
| `toStringFormat` | 格式化数值 | `new DecimalFormat(fmt).format(num)` |
| `trim` | 去空格 | `str.trim()` |
| `toUpperCase` | 转大写 | `str.toUpperCase()` |
| `toLowerCase` | 转小写 | `str.toLowerCase()` |
| `currentTime` | 当前时间戳 | `System.currentTimeMillis()` |

### 6. Map 操作（8 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `mapCreateNew` | 创建 Map | `map = new HashMap<>();` |
| `mapPut` | 添加键值对 | `map.put(key, value);` |
| `mapGet` | 获取值 | `map.get(key).toString()` |
| `mapContainKey` | 包含键 | `map.containsKey(key)` |
| `mapRemoveKey` | 移除键 | `map.remove(key);` |
| `mapSize` | 大小 | `map.size()` |
| `mapClear` | 清空 | `map.clear();` |
| `mapIsEmpty` | 判空 | `map.isEmpty()` |
| `mapGetAllKeys` | 获取所有键 | `SketchwareUtil.getAllKeysFromMap(map, list);` |

### 7. List（数值）操作（6 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `addListInt` | 添加 | `list.add(Double.valueOf(num));` |
| `insertListInt` | 插入 | `list.add(index, Double.valueOf(num));` |
| `getAtListInt` | 获取 | `list.get(index).doubleValue()` |
| `indexListInt` | 查找 | `list.indexOf(num)` |
| `containListInt` | 包含 | `list.contains(num)` |
| `lengthList` | 大小 | `list.size()` |

### 8. List（字符串）操作（5 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `addListStr` | 添加 | `list.add(str);` |
| `insertListStr` | 插入 | `list.add(index, str);` |
| `getAtListStr` | 获取 | `list.get(index)` |
| `indexListStr` | 查找 | `list.indexOf(str)` |
| `containListStr` | 包含 | `list.contains(str)` |

### 9. List（Map）操作（8 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `addListMap` | 添加 Map | 创建 HashMap 并 add |
| `addMapToList` | 直接添加 | `list.add(map);` |
| `insertListMap` | 插入 Map | 创建 HashMap 并 add at index |
| `insertMapToList` | 直接插入 | `list.add(index, map);` |
| `getAtListMap` | 获取值 | `list.get(index).get(key).toString()` |
| `setListMap` | 设置值 | `list.get(index).put(key, value);` |
| `containListMap` | 包含键 | `list.get(index).containsKey(key)` |
| `getMapInList` | 获取整个 Map | `map = list.get(index);` |

### 10. List 通用操作（2 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `clearList` | 清空 | `list.clear();` |
| `deleteList` | 删除元素 | `list.remove(index);` |

### 11. JSON 转换（4 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `strToMap` | JSON→Map | `map = new Gson().fromJson(str, ...)` |
| `mapToStr` | Map→JSON | `new Gson().toJson(map)` |
| `strToListMap` | JSON→List\<Map\> | `list = new Gson().fromJson(str, ...)` |
| `listMapToStr` | List\<Map\>→JSON | `new Gson().toJson(list)` |

### 12. 数学操作（20 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `mathPi` | 圆周率 | `Math.PI` |
| `mathE` | 自然对数底 | `Math.E` |
| `mathPow` | 幂 | `Math.pow(base, exp)` |
| `mathMin` | 最小值 | `Math.min(a, b)` |
| `mathMax` | 最大值 | `Math.max(a, b)` |
| `mathSqrt` | 平方根 | `Math.sqrt(x)` |
| `mathAbs` | 绝对值 | `Math.abs(x)` |
| `mathRound` | 四舍五入 | `Math.round(x)` |
| `mathCeil` | 向上取整 | `Math.ceil(x)` |
| `mathFloor` | 向下取整 | `Math.floor(x)` |
| `mathSin` | 正弦 | `Math.sin(x)` |
| `mathCos` | 余弦 | `Math.cos(x)` |
| `mathTan` | 正切 | `Math.tan(x)` |
| `mathAsin` | 反正弦 | `Math.asin(x)` |
| `mathAcos` | 反余弦 | `Math.acos(x)` |
| `mathAtan` | 反正切 | `Math.atan(x)` |
| `mathExp` | e^x | `Math.exp(x)` |
| `mathLog` | 自然对数 | `Math.log(x)` |
| `mathLog10` | 常用对数 | `Math.log10(x)` |
| `mathToRadian` | 角度→弧度 | `Math.toRadians(x)` |
| `mathToDegree` | 弧度→角度 | `Math.toDegrees(x)` |
| `mathGetDip` | dp→px | `SketchwareUtil.getDip(ctx, dp)` |
| `mathGetDisplayWidth` | 屏幕宽度 | `SketchwareUtil.getDisplayWidthPixels(ctx)` |
| `mathGetDisplayHeight` | 屏幕高度 | `SketchwareUtil.getDisplayHeightPixels(ctx)` |

### 13. View 操作（28 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `setText` | 设置文字 | `view.setText(text);` |
| `getText` | 获取文字 | `view.getText().toString()` |
| `setTypeface` | 设置字体 | `view.setTypeface(...)` |
| `setBgColor` | 背景色 | `view.setBackgroundColor(color);` |
| `setBgResource` | 背景资源 | `view.setBackgroundResource(R.drawable.xxx);` |
| `setTextColor` | 文字颜色 | `view.setTextColor(color);` |
| `setImage` | 图片资源 | `view.setImageResource(R.drawable.xxx);` |
| `setColorFilter` | 颜色滤镜 | `view.setColorFilter(color, PorterDuff.Mode.MULTIPLY);` |
| `setImageFilePath` | 文件路径图片 | `view.setImageBitmap(FileUtil.decodeSampleBitmapFromPath(...));` |
| `setImageUrl` | URL 图片 | `Glide.with(ctx).load(Uri.parse(url)).into(view);` |
| `setHint` | 提示文字 | `view.setHint(text);` |
| `setHintTextColor` | 提示颜色 | `view.setHintTextColor(color);` |
| `requestFocus` | 请求焦点 | `view.requestFocus();` |
| `setVisible` | 可见性 | `view.setVisibility(View.VISIBLE/GONE/INVISIBLE);` |
| `setClickable` | 可点击 | `view.setClickable(bool);` |
| `setEnable` | 启用 | `view.setEnabled(bool);` |
| `getEnable` | 是否启用 | `view.isEnabled()` |
| `setChecked` | 勾选 | `view.setChecked(bool);` |
| `getChecked` | 是否勾选 | `view.isChecked()` |
| `setRotate` | 旋转 | `view.setRotation(angle);` |
| `getRotate` | 获取旋转 | `view.getRotation()` |
| `setAlpha` | 透明度 | `view.setAlpha(alpha);` |
| `getAlpha` | 获取透明度 | `view.getAlpha()` |
| `setTranslationX/Y` | 平移 | `view.setTranslationX/Y(value);` |
| `getTranslationX/Y` | 获取平移 | `view.getTranslationX/Y()` |
| `setScaleX/Y` | 缩放 | `view.setScaleX/Y(value);` |
| `getScaleX/Y` | 获取缩放 | `view.getScaleX/Y()` |
| `getLocationX/Y` | 屏幕坐标 | `SketchwareUtil.getLocationX/Y(view)` |
| `viewOnClick` | 点击事件 | `view.setOnClickListener(...)` |

### 14. Drawer 抽屉（3 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `isDrawerOpen` | 是否打开 | `_drawer.isDrawerOpen(GravityCompat.START)` |
| `openDrawer` | 打开 | `_drawer.openDrawer(GravityCompat.START);` |
| `closeDrawer` | 关闭 | `_drawer.closeDrawer(GravityCompat.START);` |

### 15. Toast / 系统（3 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `doToast` | 显示 Toast | `SketchwareUtil.showMessage(ctx, msg);` |
| `copyToClipboard` | 复制到剪贴板 | `ClipboardManager.setPrimaryClip(...)` |
| `setTitle` | 设置标题 | `setTitle(text);` |

### 16. Intent / Activity（8 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `intentSetAction` | 设置 Action | `intent.setAction(Intent.ACTION_xxx);` |
| `intentSetData` | 设置 Data | `intent.setData(Uri.parse(uri));` |
| `intentSetScreen` | 设置目标 Activity | `intent.setClass(ctx, Activity.class);` |
| `intentPutExtra` | 放入额外数据 | `intent.putExtra(key, value);` |
| `intentSetFlags` | 设置标志 | `intent.setFlags(Intent.FLAG_ACTIVITY_xxx);` |
| `intentGetString` | 获取字符串数据 | `getIntent().getStringExtra(key)` |
| `startActivity` | 启动 Activity | `startActivity(intent);` |
| `finishActivity` | 结束 Activity | `finish();` |

### 17. SharedPreferences（4 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `fileSetFileName` | 设置文件名 | `sp = getSharedPreferences(name, MODE_PRIVATE);` |
| `fileGetData` | 读取数据 | `sp.getString(key, "")` |
| `fileSetData` | 写入数据 | `sp.edit().putString(key, value).commit();` |
| `fileRemoveData` | 删除数据 | `sp.edit().remove(key).commit();` |

### 18. Calendar 日历（7 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `calendarGetNow` | 获取当前时间 | `cal = Calendar.getInstance();` |
| `calendarAdd` | 添加时间 | `cal.add(Calendar.FIELD, amount);` |
| `calendarSet` | 设置时间 | `cal.set(Calendar.FIELD, value);` |
| `calendarFormat` | 格式化 | `new SimpleDateFormat(fmt).format(cal.getTime())` |
| `calendarDiff` | 时间差 | `cal1.getTimeInMillis() - cal2.getTimeInMillis()` |
| `calendarGetTime` | 获取毫秒 | `cal.getTimeInMillis()` |
| `calendarSetTime` | 设置毫秒 | `cal.setTimeInMillis(ms);` |

### 19. CalendarView 日历控件（4 个）

| opcode | 说明 | 生成代码 | 备注 |
|--------|------|----------|------|
| `calendarViewGetDate` | 获取日期 | `cv.getDate()` | |
| `calendarViewSetDate` | 设置日期 | `cv.setDate(ms, true, true);` | |
| `calendarViewSetMinDate` | 最小日期 | `cv.setMinDate(ms);` | |
| `calnedarViewSetMaxDate` | 最大日期 | `cv.setMaxDate(ms);` | ⚠️ **拼写错误** |

### 20. ListView / Spinner（12 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `listSetData` | 设置简单数据 | `lv.setAdapter(new ArrayAdapter<>(...));` |
| `listSetCustomViewData` | 自定义布局 | `lv.setAdapter(new XxxAdapter(data));` |
| `recyclerSetCustomViewData` | RecyclerView 同上 | 同上 |
| `spnSetCustomViewData` | Spinner 同上 | 同上 |
| `pagerSetCustomViewData` | ViewPager 同上 | 同上 |
| `gridSetCustomViewData` | GridView 同上 | 同上 |
| `listRefresh` | 刷新列表 | `((BaseAdapter)lv.getAdapter()).notifyDataSetChanged();` |
| `listSetItemChecked` | 勾选项目 | `lv.setItemChecked(pos, checked);` |
| `listGetCheckedPosition` | 获取勾选位置 | `lv.getCheckedItemPosition()` |
| `listGetCheckedPositions` | 获取所有勾选 | `SketchwareUtil.getCheckedItemPositionsToArray(lv)` |
| `listGetCheckedCount` | 勾选计数 | `lv.getCheckedItemCount()` |
| `listSmoothScrollTo` | 平滑滚动 | `lv.smoothScrollToPosition(pos);` |
| `spnSetData` | Spinner 设置数据 | `spn.setAdapter(new ArrayAdapter<>(...));` |
| `spnRefresh` | Spinner 刷新 | `((ArrayAdapter)spn.getAdapter()).notifyDataSetChanged();` |
| `spnSetSelection` | Spinner 选中 | `spn.setSelection(pos);` |
| `spnGetSelection` | Spinner 获取选中 | `spn.getSelectedItemPosition()` |

### 21. WebView（12 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `webViewLoadUrl` | 加载 URL | `wv.loadUrl(url);` |
| `webViewGetUrl` | 获取 URL | `wv.getUrl()` |
| `webViewSetCacheMode` | 缓存模式 | `wv.getSettings().setCacheMode(...);` |
| `webViewCanGoBack` | 可后退 | `wv.canGoBack()` |
| `webViewCanGoForward` | 可前进 | `wv.canGoForward()` |
| `webViewGoBack` | 后退 | `wv.goBack();` |
| `webViewGoForward` | 前进 | `wv.goForward();` |
| `webViewClearCache` | 清缓存 | `wv.clearCache(true);` |
| `webViewClearHistory` | 清历史 | `wv.clearHistory();` |
| `webViewStopLoading` | 停止加载 | `wv.stopLoading();` |
| `webViewZoomIn` | 放大 | `wv.zoomIn();` |
| `webViewZoomOut` | 缩小 | `wv.zoomOut();` |

### 22. AdMob 广告（3 个）

| opcode | 说明 | 生成代码 | 备注 |
|--------|------|----------|------|
| `adViewLoadAd` | 加载 Banner 广告 | `adView.loadAd(new AdRequest.Builder().build());` | |
| `interstitialadCreate` | 创建插页广告 | *(空)* | ⚠️ **已废弃** |
| `interstitialadLoadAd` | 加载插页广告 | *(空)* | ⚠️ **已废弃** |
| `interstitialadShow` | 显示插页广告 | *(空)* | ⚠️ **已废弃** |

### 23. Google Map（11 个）

| opcode | 说明 |
|--------|------|
| `mapViewSetMapType` | 设置地图类型 |
| `mapViewMoveCamera` | 移动相机 |
| `mapViewZoomTo` | 缩放到 |
| `mapViewZoomIn` / `mapViewZoomOut` | 放大/缩小 |
| `mapViewAddMarker` | 添加标记 |
| `mapViewSetMarkerInfo` | 设置标记信息 |
| `mapViewSetMarkerPosition` | 设置标记位置 |
| `mapViewSetMarkerColor` | 设置标记颜色 |
| `mapViewSetMarkerIcon` | 设置标记图标 |
| `mapViewSetMarkerVisible` | 标记可见性 |

### 24. Vibrator 震动（1 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `vibratorAction` | 震动 | `vibrator.vibrate(ms);` |

### 25. Timer 定时器（3 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `timerAfter` | 延迟执行 | `new TimerTask() → _timer.schedule(task, delay)` |
| `timerEvery` | 周期执行 | `new TimerTask() → _timer.scheduleAtFixedRate(task, delay, period)` |
| `timerCancel` | 取消 | `timer.cancel();` |

### 26. Firebase Database（7 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `firebaseAdd` | 更新子节点 | `ref.child(key).updateChildren(map);` |
| `firebasePush` | 推送 | `ref.push().updateChildren(map);` |
| `firebaseGetPushKey` | 获取推送键 | `ref.push().getKey()` |
| `firebaseDelete` | 删除子节点 | `ref.child(key).removeValue();` |
| `firebaseGetChildren` | 获取所有子项 | 注册 `addListenerForSingleValueEvent` |
| `firebaseStartListen` | 开始监听 | `ref.addChildEventListener(listener);` |
| `firebaseStopListen` | 停止监听 | `ref.removeEventListener(listener);` |

### 27. Firebase Auth（8 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `firebaseauthCreateUser` | 注册 | `auth.createUserWithEmailAndPassword(...)` |
| `firebaseauthSignInUser` | 登录 | `auth.signInWithEmailAndPassword(...)` |
| `firebaseauthSignInAnonymously` | 匿名登录 | `auth.signInAnonymously(...)` |
| `firebaseauthIsLoggedIn` | 已登录 | `FirebaseAuth.getInstance().getCurrentUser() != null` |
| `firebaseauthGetCurrentUser` | 当前用户邮箱 | `...getCurrentUser().getEmail()` |
| `firebaseauthGetUid` | 当前用户 UID | `...getCurrentUser().getUid()` |
| `firebaseauthResetPassword` | 重置密码 | `auth.sendPasswordResetEmail(email)` |
| `firebaseauthSignOutUser` | 登出 | `FirebaseAuth.getInstance().signOut();` |

### 28. Firebase Storage（3 个）

| opcode | 说明 |
|--------|------|
| `firebasestorageUploadFile` | 上传文件 |
| `firebasestorageDownloadFile` | 下载文件 |
| `firebasestorageDelete` | 删除文件 |

### 29. Gyroscope 陀螺仪（2 个）

| opcode | 说明 |
|--------|------|
| `gyroscopeStartListen` | 开始监听 |
| `gyroscopeStopListen` | 停止监听 |

### 30. Dialog 对话框（6 个）

| opcode | 说明 |
|--------|------|
| `dialogSetTitle` | 设置标题 |
| `dialogSetMessage` | 设置消息 |
| `dialogShow` | 显示 |
| `dialogOkButton` | 确定按钮 |
| `dialogCancelButton` | 取消按钮 |
| `dialogNeutralButton` | 中性按钮 |

### 31. MediaPlayer 媒体播放器（11 个）

| opcode | 说明 |
|--------|------|
| `mediaplayerCreate` | 创建 |
| `mediaplayerStart` | 播放 |
| `mediaplayerPause` | 暂停 |
| `mediaplayerSeek` | 跳转 |
| `mediaplayerGetCurrent` | 当前位置 |
| `mediaplayerGetDuration` | 总时长 |
| `mediaplayerReset` | 重置 |
| `mediaplayerRelease` | 释放 |
| `mediaplayerIsPlaying` | 是否播放中 |
| `mediaplayerSetLooping` | 设置循环 |
| `mediaplayerIsLooping` | 是否循环 |

### 32. SoundPool 音效池（4 个）

| opcode | 说明 |
|--------|------|
| `soundpoolCreate` | 创建 |
| `soundpoolLoad` | 加载 |
| `soundpoolStreamPlay` | 播放 |
| `soundpoolStreamStop` | 停止 |

### 33. SeekBar / Switch（6 个）

| opcode | 说明 |
|--------|------|
| `seekBarSetProgress` | 设置进度 |
| `seekBarGetProgress` | 获取进度 |
| `seekBarSetMax` | 设置最大值 |
| `seekBarGetMax` | 获取最大值 |
| `setThumbResource` | 设置滑块图标 |
| `setTrackResource` | 设置轨道图标 |

### 34. ObjectAnimator 动画（11 个）

| opcode | 说明 |
|--------|------|
| `objectanimatorSetTarget` | 设置目标 |
| `objectanimatorSetProperty` | 设置属性 |
| `objectanimatorSetValue` | 设置值 |
| `objectanimatorSetFromTo` | 设置起止值 |
| `objectanimatorSetDuration` | 设置时长 |
| `objectanimatorSetRepeatMode` | 重复模式 |
| `objectanimatorSetRepeatCount` | 重复次数 |
| `objectanimatorSetInterpolator` | 插值器 |
| `objectanimatorStart` | 开始 |
| `objectanimatorCancel` | 取消 |
| `objectanimatorIsRunning` | 是否运行中 |

### 35. 文件操作（19 个）

| opcode | 说明 | 生成代码 |
|--------|------|----------|
| `fileutilread` | 读文件 | `FileUtil.readFile(path)` |
| `fileutilwrite` | 写文件 | `FileUtil.writeFile(path, content);` |
| `fileutilcopy` | 复制 | `FileUtil.copyFile(src, dst);` |
| `fileutilmove` | 移动 | `FileUtil.moveFile(src, dst);` |
| `fileutildelete` | 删除 | `FileUtil.deleteFile(path);` |
| `fileutilisexist` | 是否存在 | `FileUtil.isExistFile(path)` |
| `fileutilmakedir` | 创建目录 | `FileUtil.makeDir(path);` |
| `fileutillistdir` | 列出目录 | `FileUtil.listDir(path, list);` |
| `fileutilisdir` | 是否目录 | `FileUtil.isDirectory(path)` |
| `fileutilisfile` | 是否文件 | `FileUtil.isFile(path)` |
| `fileutillength` | 文件大小 | `FileUtil.getFileLength(path)` |
| `fileutilStartsWith` | 前缀匹配 | `str.startsWith(prefix)` |
| `fileutilEndsWith` | 后缀匹配 | `str.endsWith(suffix)` |
| `fileutilGetLastSegmentPath` | 路径最后段 | `Uri.parse(path).getLastPathSegment()` |
| `getExternalStorageDir` | 外部存储 | `FileUtil.getExternalStorageDir()` |
| `getPackageDataDir` | 包数据目录 | `FileUtil.getPackageDataDir(ctx)` |
| `getPublicDir` | 公共目录 | `FileUtil.getPublicDir(Environment.XXX)` |

### 36. Bitmap 图片处理（9 个）

| opcode | 说明 |
|--------|------|
| `resizeBitmapFileRetainRatio` | 等比缩放 |
| `resizeBitmapFileToSquare` | 缩放为正方形 |
| `resizeBitmapFileToCircle` | 裁切为圆形 |
| `resizeBitmapFileWithRoundedBorder` | 圆角 |
| `cropBitmapFileFromCenter` | 居中裁切 |
| `rotateBitmapFile` | 旋转 |
| `scaleBitmapFile` | 缩放 |
| `skewBitmapFile` | 倾斜 |
| `setBitmapFileColorFilter` | 颜色滤镜 |
| `setBitmapFileBrightness` | 亮度 |
| `setBitmapFileContrast` | 对比度 |
| `getJpegRotate` | 获取 JPEG 旋转角度 |

### 37. File/Camera Picker（2 个）

| opcode | 说明 |
|--------|------|
| `filepickerstartpickfiles` | 选择文件 |
| `camerastarttakepicture` | 拍照 |

### 38. TextToSpeech 语音合成（6 个）

| opcode | 说明 |
|--------|------|
| `textToSpeechSetPitch` | 设置音调 |
| `textToSpeechSetSpeechRate` | 设置语速 |
| `textToSpeechSpeak` | 朗读 |
| `textToSpeechIsSpeaking` | 是否朗读中 |
| `textToSpeechStop` | 停止 |
| `textToSpeechShutdown` | 关闭 |

### 39. SpeechToText 语音识别（3 个）

| opcode | 说明 |
|--------|------|
| `speechToTextStartListening` | 开始监听 |
| `speechToTextStopListening` | 停止监听 |
| `speechToTextShutdown` | 关闭 |

### 40. Bluetooth 蓝牙（11 个）

| opcode | 说明 |
|--------|------|
| `bluetoothConnectReadyConnection` | 准备连接 |
| `bluetoothConnectReadyConnectionToUuid` | 准备连接（指定 UUID） |
| `bluetoothConnectStartConnection` | 开始连接 |
| `bluetoothConnectStartConnectionToUuid` | 开始连接（指定 UUID） |
| `bluetoothConnectStopConnection` | 停止连接 |
| `bluetoothConnectSendData` | 发送数据 |
| `bluetoothConnectIsBluetoothEnabled` | 蓝牙是否启用 |
| `bluetoothConnectIsBluetoothActivated` | 蓝牙是否激活 |
| `bluetoothConnectActivateBluetooth` | 激活蓝牙 |
| `bluetoothConnectGetPairedDevices` | 获取配对设备 |
| `bluetoothConnectGetRandomUuid` | 获取随机 UUID |

### 41. Location 位置（2 个）

| opcode | 说明 |
|--------|------|
| `locationManagerRequestLocationUpdates` | 请求位置更新 |
| `locationManagerRemoveUpdates` | 停止位置更新 |

### 42. 网络请求（3 个）

| opcode | 说明 |
|--------|------|
| `requestnetworkSetParams` | 设置参数 |
| `requestnetworkSetHeaders` | 设置请求头 |
| `requestnetworkStartRequestNetwork` | 发起请求 |

### 43. 其他（3 个）

| opcode | 说明 |
|--------|------|
| `addSourceDirectly` | 直接插入源代码 |
| `definedFunc` | 调用 MoreBlock |
| `progressBarSetIndeterminate` | 进度条不确定模式 |

---

## 二、已知 Bug / Typo

| 位置 | 问题 | 严重程度 |
|------|------|----------|
| `calnedarViewSetMaxDate` | 拼写错误，应为 `calendarViewSetMaxDate` | 🟡 低（仅影响内部命名） |
| `interstitialadCreate/LoadAd/Show` | 代码生成为空字符串，AdMob 插页广告完全不工作 | 🔴 高 |
| `AccelerateDeccelerate` | 拼写错误，应为 `AccelerateDecelerate`（`objectanimatorSetInterpolator` 内部） | 🟡 低 |

---

## 三、缺失分析：建议补充的 Block（经三轮审查）

> **重要发现**：项目存在**两套 block 系统**：
> 1. **核心块**（`BlockInterpreter.java` + `BlockSpecRegistry.java`）— ~174 个，硬编码在 switch 语句中
> 2. **额外块**（`BlocksHandler.java` 的 `builtInBlocks()` 方法）— ~200+ 个，通过 HashMap 动态定义
>
> 初始分析仅覆盖了核心块系统，遗漏了额外块系统。经三轮审查后，初步建议的 35 个缺失块中：
> - **4 个已存在于额外块系统**（tryCatch、stringSplit、hideKeyboard、isConnected）
> - **多个"可选"块也已存在**（setTextSize、getWidth/getHeight、sortList、setAtPos）
> - **仅 1 个确认缺失**

### 已存在于额外块系统的块（无需实现）

| 初始建议 | 已有块名 | 定义位置 | 生成代码 |
|----------|---------|----------|----------|
| `tryCatch` | `tryCatch` | BlocksHandler.java:2153 | `try { ... } catch (Exception e) { ... }` |
| `stringSplit` | `stringSplitToList` | BlocksHandler.java:753 | `new ArrayList<>(Arrays.asList(str.split(regex)))` |
| `hideKeyboard` | `hideKeyboard` | BlocksHandler.java:398 | `SketchwareUtil.hideKeyboard(ctx)` |
| `isNetworkAvailable` | `isConnected` | BlocksHandler.java:329 | `SketchwareUtil.isConnected(ctx)` |
| `setTextSize` | `setTextSize` | BlocksHandler.java:1166 | `view.setTextSize((int)size)` |
| `getWidth` | `getWidth` | BlocksHandler.java:781 | `view.getWidth()` |
| `getHeight` | `getHeight` | BlocksHandler.java:771 | `view.getHeight()` |
| `sortList` | `sortList` / `sortListnum` | BlocksHandler.java:2264/2274 | `Collections.sort(list)` |
| `setAtListStr` | `setAtPosListstr` | BlocksHandler.java:2314 | `list.set(index, value)` |
| `setAtListInt` | `setAtPosListnum` | BlocksHandler.java:2324 | `list.set(index, value)` |
| `sortListMap` | `sortListmap` | BlocksHandler.java:680 | `SketchwareUtil.sortListMap(...)` |
| `showKeyboard` | `showKeyboard` | BlocksHandler.java:406 | `SketchwareUtil.showKeyboard(ctx)` |

### 确认缺失并已实现（2 个）

| 类别 | 缺失 block | 生成代码 | 理由 | 状态 |
|------|-----------|----------|------|------|
| 系统 | `getClipboard` | `SketchwareUtil.getClipboardText(ctx)` | 有 `copyToClipboard`（写入）却无法读取，set/get 不对称 | ✅ 已实现 |
| 控制流 | `getExceptionMessage` | `e.getMessage()` | `tryCatch` 捕获异常后无法获取错误信息，块功能不完整 | ✅ 已实现 |

### 排除：已有现成方案（14 个）

| 初始建议 | 为什么不需要 |
|----------|-------------|
| `while` | `forever` + `if(!cond) { break; }` 完全等效 |
| `continue` | 用 `if` 反转条件包裹后续逻辑即可 |
| `forEach` | `repeat` + `lengthList` + `getAtListStr/Int` 可实现 |
| `stringIsEmpty` | `stringLength(str) = 0` 或 `stringEquals(str, "")` |
| `stringStartsWith` | **已有** `fileutilStartsWith`，生成代码完全相同 |
| `stringEndsWith` | **已有** `fileutilEndsWith`，生成代码完全相同 |
| `stringCharAt` | `stringSub(str, index, index+1)` |
| `toInt` | Sketchware 数值全为 double，需 int 时自动 `(int)` 转换 |
| `fileSetInt/getInt` | 用 `fileSetData` 存字符串 + `toNumber` 读回 |
| `fileSetBoolean/getBoolean` | 存 `"true"`/`"false"` 字符串 + `stringEquals` 判断 |
| `fileContainsKey` | `fileGetData` 检查返回值是否为空 |
| `getVisibility` | 用变量自行跟踪可见状态 |
| `listIsEmpty` | `lengthList(list) = 0` |

### 排除：需求不足或不适合块化（9 个）

| 初始建议 | 为什么不需要 |
|----------|-------------|
| `showNotification` | 太复杂（需 Channel + PendingIntent + 图标），需 5-6 个块 |
| `showSnackbar` | Toast 已覆盖大部分场景 |
| `logDebug` | Sketchware 用户看不到 logcat，用 Toast 调试 |
| `openUrlInBrowser` | **已可实现**：`intentSetAction` + `intentSetData` + `startActivity` |
| `intentGetInt` | Sketchware 项目间通信用 String 类型足够 |
| `setResult` | `startActivityForResult` 整个流程缺失 |
| `reverseList` | 单独需求低 |
| `mathAtan2` / `setPadding` / `setInputType` / `scrollViewScrollTo` | 过于专业 |

---

## 四、架构说明：如何添加新 Block

添加一个新的内置 block 需要修改以下文件：

1. **`BlockSpecRegistry.java`** — 注册 block 的 spec（显示文本）、参数列表、事件 spec
   - `getBlockParams(blockName)` — 参数类型列表（`%d` 数值, `%s` 字符串, `%b` 布尔, `%m.xxx` 菜单选择）
   - `getBlockSpec(blockName)` — 显示文本（如 `set %m.textview text %s`）

2. **`BlockInterpreter.java`** — `getBlockCode()` 方法中添加 `case "opcode":` 分支，定义代码生成逻辑

3. **Block 调色板** — 将 block 添加到 Logic Editor 的块面板中，使用户可以拖拽使用
   - 相关文件：`LogicEditorActivity.java` 或块菜单配置

4. **翻译** — 在 `values/strings.xml` 和各语言文件中添加 `block_xxx` 格式的翻译字符串

> **注意**: `BlockSpecRegistry.java` 使用 hash-based switch（基于 `String.hashCode()`），添加新 block 需要正确计算 hash 值并插入到正确位置，或改用更现代的 switch 语法。

---

## 五、总结

| 指标 | 数量 |
|------|------|
| 核心块（BlockInterpreter） | ~174 个 |
| 额外块（BlocksHandler） | ~200+ 个 |
| 已知 Bug/Typo | 3 个 |
| 初步建议中已存在于额外块 | 12 个 |
| **确认缺失并已实现** | **2 个**（`getClipboard`、`getExceptionMessage`） |
| 排除（已有方案或需求不足） | 23 个 |

### 结论

项目的 block 覆盖度**远比初始分析的结果更完善**。初始分析仅覆盖了核心块系统（`BlockInterpreter.java`），
遗漏了额外块系统（`BlocksHandler.java`）中已有的 200+ 个块。

经三轮审查（核心块分析 → 变通方案排查 + set/get 对称性检查 → 额外块系统交叉验证），
初步建议的 35 个缺失块中 **12 个已存在于额外块系统**，其余大部分有现成变通方案。

**已实现的 2 个缺失块**：
- **`getClipboard`** — 读取剪贴板（有 `copyToClipboard` 写入却无法读取，set/get 不对称）
- **`getExceptionMessage`** — 获取异常信息（`tryCatch` 捕获异常后无法获取 `e.getMessage()`，功能不完整）

其余需求可通过现有块（核心 + 额外）的组合或 `addSourceDirectly` 解决。
