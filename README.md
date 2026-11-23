# GoldCc · 林间复盘

国服《金铲铲之战》S18「自然之力」的离线安卓复盘工具。从相册导入一张赛后截图，按名次识别一位玩家的出场阵容，对照原图校正后保存。首页可以重新打开和编辑历史复盘。适用于 Android 7.0（API 24）及以上。

## 截什么图

对局结束后，打开**特质/职业**页。在同一张横屏原图里保留左侧排名、中间「出场阵容」的八行方形头像和右侧羁绊。列表应从第一名开始，各行高度相同。关闭弹窗，保留四边。三张实测示例见下方验证表。

- 点「从相册选择截图」。默认显示第一名。点名次按钮可切换到第 2–8 名，切换会重新识别。
- 上方显示完整截图，名次下方放大当前行。点图可以双指缩放、拖动。对照头像点「替换」「删除」或「补上弈子」。同名弈子可以重复。
- 点「保存复盘」，关闭后重开，在「我的复盘」中按日期、名次和枚数查找。

识别依赖标准结算布局。商店卡面、站位模型、海报、拼图、裁剪、展开或换行的列表超出此版范围。比例检查和低置信度过滤只能排除部分陌生图。候选仍需核对。人口数字、涂鸦、遮挡、特殊单位和新版 UI 均可能导致误检或漏检。未识别时允许从本版名册手动补卡。

## Windows 构建

安装 JDK 17、Android SDK Platform 35、Build Tools 35.0.0、Python 3.13 和 curl。将 JDK 路径设为 JAVA_HOME、SDK 根目录设为 ANDROID_HOME，确保 python 和 curl 在 PATH。在仓库根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\build-apk.ps1
```

输出为 `app/build/outputs/apk/debug/app-debug.apk`。已配好 JDK/SDK 的环境也可直接执行 `.\gradlew.bat :app:assembleDebug`。USB 调试安装命令为 `adb install -r app/build/outputs/apk/debug/app-debug.apk`。

Wrapper 固定 Gradle 8.9 并校验发行包 SHA-256，Android Gradle Plugin 为 8.7.3。首次构建需联网获取工具依赖和 74 张头像。构建任务用 Pillow 13.0.0 转换头像，并逐张检查 64×64 RGB 像素摘要。若本机 Pillow 版本不同，会在项目的 .gradle 下建立独立 Python 环境。后续增量构建复用本地缓存。APK 使用本机 Android debug 密钥签名。应用本身无需联网，清单不申请网络或相册读取权限。用户通过系统文件选择器只授权所选图片；截图的 JPEG 副本和复盘记录保存在应用私有目录，卸载时一并清除。

## 数据与素材

| 项目 | 快照与用途 |
| --- | --- |
| [金铲铲阵容库 S18](https://jcc.np5.top/tools/seasons/s18) · [固定版本索引](https://jcc.np5.top/season-assets/bb8e731abf914bc185ae6651e4194e39/data/index.json) | 国服资料库 18.18.6，生效日 2026-11-03。取 65 个中文名、费用和 74 个头像条目，拉克丝的多个形态合并显示为同一名称。 |
| [腾讯 TFT 静态数据](https://game.gtimg.cn/images/lol/act/img/tft/js/chess.js) | 交叉检查时版本 16.22，season 为 2026.S18。排除零费召唤物、合并拉克丝形态后，65 个名称与上项一致。 |
| [国服官方赛季活动与玩家结算图](https://www.taptap.cn/moment/838728403220694206) | 核对赛季、结算版式及部分实战头像。逐张测试记录见下表。 |

PNG 由固定的资料库 WebP 转换，下载路径与每张像素摘要在 `tools/portraits.lock`，名册位于 `app/src/main/assets/units.tsv`。此版本覆盖该快照的弈子，未逐一实测每种形态。游戏角色、美术和商标属于相应权利人。本项目用于个人复盘测试；未取得游戏美术的商业使用或再分发授权。公开仓库保存源码、名册和素材清单；第三方游戏头像由构建脚本获取，玩家原图仅链接到公开来源。对外分发含素材的 APK 前需另行确认授权。

## 真图验证

取自同一官方活动评论区的不同对局，去除重复投稿。先用 4 张图确定版式、模板取样区域与阈值，再用另外 3 张图逐卡核对前两行。桌面校验运行的是应用同一份 Java Matcher。输入按比例缩到 600 像素高，在各槽位中央比较 RGB 差异，避开顶部星级和底部装备。择优及次优差距过小的候选会被略过。每行最多扫描 10 格，重复弈子按位置独立计数。

| 独立验证原图 | 名次 | 应识别的弈子，从左到右 | 正确 / 漏检 / 多报 |
| --- | --- | --- | --- |
| [398157429 · 3088×1440](https://img2-tc.tapimg.com/moment/etag/FtwH2NaFmQXn26HWOP2QHT1M-Prl_20260829013055.jpg) | 1 | 凯南、纳尔、德莱文、伊泽瑞尔、阿利斯塔、乐芙兰、茂凯、塔里克、艾希 | 9 / 0 / 1（人口提示报成卡兹克） |
| 同图 | 2 | 拉露恩、拉露恩、拉克丝、拉克丝、拉露恩、拉露恩、拉克丝、拉克丝 | 8 / 0 / 0 |
| [398026829 · 2676×1236](https://img2-tc.tapimg.com/moment/etag/Fqa60GNEJQ93a20CyvjW4YJRmSdJ_20260827230723.jpg) | 1 | 瑟提、纳尔、阿狸、蔚、卡尔玛、约里克、婕拉、芸阿娜 | 8 / 0 / 0 |
| 同图 | 2 | 瑟庄妮、乐芙兰、凯尔、奥恩、洛、蕾欧娜、霞、伊莉丝 | 8 / 0 / 0 |
| [397965150 · 2832×1280](https://img2-tc.tapimg.com/moment/etag/Fvuck7jrwyxmo3X0RGh4R5nqNzZS_20260827135504.jpg) | 1 | 远古巨龙、芸阿娜、乐芙兰、卡尔玛、约里克、拉克丝、凯南、峡谷迅捷蟹 | 8 / 0 / 0 |
| 同图 | 2 | 黛安娜、瑟庄妮、凯尔、蕾欧娜、拉露恩、厄斐琉斯、卡兹克、凯南、拉克丝 | 9 / 0 / 0 |

合计 50 个真实卡位，正确 50，漏检 0，多报 1。调参用图的评论编号为 398637289、398229958、398222845、398207742。将 398637289 缩到 1280 和 1920 像素宽并以 JPEG 质量 82 重编码，前两行的 11 个结果均保留。另用一张纯色合成图检查空结果。以上数字限于这些样本与行次。

## 复验

构建后，用 JDK 编译桌面校验入口，最后一个参数指向下载的结算原图。输出按名次分行，括号内是像素误差值，越小越接近。

```powershell
New-Item -Force -ItemType Directory build/check | Out-Null
& "$env:JAVA_HOME/bin/javac" -encoding UTF-8 -d build/check app/src/main/java/cn/goldcc/review/Matcher.java tools/Check.java
& "$env:JAVA_HOME/bin/java" -cp build/check Check .\sample.jpg
```

运行 `.\gradlew.bat testDebugUnitTest` 可在 Robolectric 的 Android 9 环境中测试选图意图、空结果、增删替换、保存及销毁并重建 Activity 后回看。将环境变量 GOLDCC_TEST_IMAGE 指向上表 398026829 原图可额外运行图片导入测试。没有连接的安卓设备，系统相册、真机触控和安装后的冷启动仍待实机验证。
