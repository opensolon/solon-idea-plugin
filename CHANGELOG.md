<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Solon Changelog

## [0.2.15]

**发布日期**: 2026-11-09
**平台**: IU-253.28294.334（2025.3）
**构建**: IntelliJ Platform Gradle Plugin 2.20.0 / Gradle 9.0.0 / JDK 25 / Lombok 9.8.3

本版本为一次集中式大版本升级，新增两条核心能力线：**配置文件 ↔ 源码双向导航** 与 **Controller 接口 REST 调试工具窗口**。以下变更均经过本地 `compileJava` / `buildPlugin` 验证。

---

### ✨ 新功能

#### 1. 配置文件 ↔ Java 源码双向导航

彻底打通 Solon 项目中 `app.yml / app.properties / 插件自带 yml` ↔ `@Inject / @BindProps / toBean / getBean / bindTo / *Properties` 配置类的双向跳转。

**覆盖的绑定路径（参考 solon.noear.org/article/31）**：

| # | 路径 | 识别方式 |
|---|------|----------|
| A1 | `@Inject("${key}")` 类/字段/方法参数 | `AnnotatedMembersSearch` + `AnnotatedElementsSearch`（参数单独处理） |
| A2 | `@BindProps(prefix = "...")` 类/方法 | `AnnotatedMembersSearch` |
| B1 | `cfg().toBean("prefix", XxxClass.class)` | `JavaMethodNameIndex` + `ReferencesSearch`，筛选 2 参数且第 2 参数为 `Class` |
| B2 | `cfg().getBean("prefix", XxxClass.class)` | 同 B1（`getBean` 别名） |
| B3 | `cfg().getProp("prefix").bindTo(XxxClass.class)` 链式调用 | 取 `getProp("prefix")` 的参数，不再把调用表达式本身当字符串解析 |
| C1 | `cfg().loadAdd*("classpath:xxx.yml")` 插件默认配置 | 扫描 `loadAdd` / `loadAddIfAbsent` 调用，取字面量或常量引用，在调用方模块内按路径后缀解析资源文件 |
| C2 | 约定式匹配（无前缀声明时） | 前缀末段 ↔ 配置类名去 `Properties/Property/Props/Config/Configuration/Options/Settings` 后缀，依赖字段重名校验防误配 |

**防误配约束（约定式匹配必须全部满足）**：
- 前缀 ≥ 2 段（单段多为三方库自带配置，跳过）
- 配置类非接口/枚举/注解/抽象
- 类名必须带配置类后缀
- 前缀下至少一个子键能对上配置类字段名（忽略大小写 / `-` / `_`）
- 显式绑定（toBean/@Inject/…）已登记的前缀不被约定覆盖
- 依赖库同名候选按「项目源码 > 同模块 > 字段命中数」排序

**反向索引（Find Usages 配置键）**：
- 覆盖 `app*.yml/yaml/properties` + 所有 `.yml/.yaml/.properties` 文件 + `loadAdd*` 加载的插件配置（运行时 `isSolonConfigFile` 过滤）
- 缓存按 `PsiModificationTracker` / Java PSI 修改计数失效，避免编辑 yml 时反复重算 `ReferencesSearch`

**消费端场景（组件无自带 yml、绑定 jar 内嵌）**：
- 约定式匹配的类查找从「同模块源码」扩展到 `GlobalSearchScope.allScope`（含依赖库）
- 解决 `ctsz-plugin-cache` 这类组件：绑定在消费端 app.yml，但配置类和 toBean 调用在依赖 jar 内

**限制**：
- 只识别 `classpath:` 字面量 / `static final String` 常量；URL / Properties 重载、拼接字符串不索引
- jar 内资源不可编辑，不纳入运行时配置文件集合
- jar 无 sources 时跳转打开类文件视图（可手动 Attach Sources）

#### 2. Controller 接口 REST 调试工具窗口

新增 Tool Window 提供轻量 REST 调试能力：

- **API 行标记（`ApiCallLineMarkerProvider`）**：在带 `@Mapping / @Get / @Post …` 注解的 Controller 方法旁展示「REST 调用」图标，点击弹出测试面板
- **Tool Window**：HTTP 方法选择、URL 路径拼接、Query/Path/Header/Body 参数编辑、快速发送
- **响应面板**：状态码 / Header / Body（格式化 + 源码两种查看方式）
- **服务配置自动解析**：根据 `solon.app.name` / `server.port` 推导 base URL
- **路径与参数解析**：从 `@Mapping` 注解 / `@Param` / `@Inject` 等提取路径段与参数

### 🐛 修复

#### 配置绑定扫描失效根因（本轮修复 7 个，之前用户反馈"改动没生效"）

| # | 根因 | 修复 |
|---|------|------|
| 1 | `JavaPsiFacade.findClass(INJECT_FQN, projectScope)` 返回 null（注解在 jar 内） | 注解类 / 框架方法查找统一改用 `GlobalSearchScope.allScope` |
| 2 | `PropertyUtil.getWritableProperties` 依赖 setter，Lombok `@Data` 产出 0 条 | `collectConfigFields` 先 `getWritableProperties`，再 `getAllFields()` 兜底（跳过 static / enum constant） |
| 3 | `MetadataIndexBase.add` 的 `getProperties()` 无 null 保护 → NPE | 所有新对象 `setProperties(new ArrayList<>())` / `setGroups(new ArrayList<>())` |
| 4 | `AggregatedMetadataIndex.toUnmodifiableMap` 重复 key 抛 `IllegalStateException` | 改为 `firstWins()` 合并函数，避免单条脏元数据使整条聚合链失效 |
| 5 | `scanGetPropBindToCalls` 把 `getProp("prefix")` 调用表达式本身当字符串解析，永远 null | 取 `receiverCall.getArgumentList().getExpressions()[0]` |
| 6 | 反向索引缓存只依赖文件列表，元数据刷新不失效 | `ReferenceService.clearIndexCache()` + holder `invalidate()` |
| 7 | `MetadataStartupActivity` 只刷新 `modules[0]` | 改为遍历所有 module 调 `refreshMetadata()` |

#### 其他修复

- **`@Inject` 位置参数形式**：`@Inject("${x}")` 与 `@Inject(value="${x}")` 两种 AST 统一兼容
- **`ConfigurationPropertyName.equals` / `elementDiffers`**：原实现按 `toString` 快路径 + 大小写敏感比较，导致 `whiteList`（驼峰 metadata）与 `white-list`（中划线 yaml）永远不相等。改为统一走 `defaultElementEquals`（忽略大小写 + 忽略分隔符），与 `hashCode` 的归一化语义一致
- **`SolonConfigClassScanner`**：`SmartInvokeLater` 异步 CompletableFuture 同步执行，避免 refreshMetadata 等待时序问题
- **`ScanContext.register`**：单值/容器类型走 `getPropertyMetadata`，对象类型继续 `registerByType`

### 🔧 工程 / 构建

- **构建工具升级**：IntelliJ Platform Gradle Plugin 2.20.0 / Gradle 9.0.0 / IntelliJ 2025.3 IU
- **Lombok 支持**：`io.freefair.lombok` 插件，扫描时 `getAllFields()` 覆盖 Lombok 生成的 setter
- **新增依赖**：`commons-collections4`（`MultiValuedMap`）
- **plugin.xml**：注册 4 个新的 `PsiReferenceContributor`、1 个 Tool Window factory、1 个 `ProjectActivity`
- **`CachedValuesManager` 显式 Key**：两参 `getCachedValue` 每次新建 lambda 无法复用缓存，统一用 `Key.create("solon.xxx")`

### 📁 新增 / 修改文件清单

**新增（5 个源文件）**：

| 文件 | 职责 |
|------|------|
| `metadata/service/SolonConfigFilesService.java` | 配置文件识别：`loadAdd*` 调用扫描 + 路径解析 + `isSolonConfigFile` 判定 |
| `metadata/service/SolonConfigClassScanner.java` | 配置绑定扫描器（590 行），覆盖 A1-C2 全部 7 条路径 |
| `navigation/InjectStringLiteralReferenceContributor.java` | `@Inject("${key}")` 字面量引用 |
| `navigation/InjectStringLiteralToPsiReference.java` | 上述引用的 target 实现 |
| `api/*`（6 个包 12 个文件） | Controller REST 调试：行标记、Tool Window、参数解析、HTTP 请求 |

**修改（10 个核心源文件）**：`ConfigurationPropertyName.java`（equals 宽松化）、`AggregatedMetadataIndex.java`（firstWins 合并）、`ProjectClassMetadataService.java`（所有字段登记 + 深度递归 5 层）、`ModuleMetadataServiceImpl.java`（缓存失效）、`MetadataStartupActivity.java`（多模块遍历）、`ReferenceService.java`（反向索引扩大 + 缓存失效）、`YamlToCodeReferenceContributor.java`（全 YAML 注册 + 运行时过滤）、`PropertiesToCodeReferenceContributor.java`（同上）、`SolonIcons.java`（新增 API call 图标）、`plugin.xml`（扩展点注册）。

### 🧪 验证

| 项目 | 结果 |
|------|------|
| `compileJava` | ✅ 通过（`build.gradle.kts` / `settings.gradle.kts` / 所有 27 个源文件） |
| `buildPlugin` | ✅ 产物 `build/distributions/Solon-0.2.15.zip`（4.2 MB，2026-11-09 16:55） |
| `ConfigurationPropertyName.equals` 离线验证 | ✅ `ctsz.request.whiteList` vs `ctsz.request.white-list` → equals=true；`solon.app.name` vs `solon.app.title` → equals=false |
| 约定匹配字段重名校验 | ✅ `OAuth2Properties.enabled` 命中 oauth2.yml 的 `ctsz.oauth2.enabled`；`sa-token.oauth2.*` 因无字段命中被拒绝 |
| `runIdeForUiTests` GUI 级端到端 | ⚠️ 沙箱限制（`/proc/*/coredump_filter` 被访问控制拦截），未能完成；需在本机 IDE 中验证 |

**IDE 验证步骤**（本机）：
1. 安装 `Solon-0.2.15.zip` 并重启
2. 打开包含依赖插件组件的工程（如 `qingzhou-boot`），等待索引完成
3. 右键 `Refresh Solon Metadata`
4. 在 `app.yml` 的 `ctsz.cache.enabled` 上 Ctrl+Click → 应跳到 `CacheProperty.enabled`
5. 在 `CacheProperty` 上 Alt+F7 → 应列出 `app.yml` 对应键
6. Controller 方法旁点击「REST 调用」图标 → 弹出调试面板
7. `Help → Show Log` 中应出现 `Convention binding: ...` 与 `Solon config scan finished: ...`

### 🗑️ 回退方案

- 还原上述 15 个源文件 + 重装上一版 zip；插件无运行时数据写入
- 元数据与反向索引均为项目级 `CachedValue`，重启 IDE 自动清空

---

## [0.1.10]

- 修复问题

## [0.1.9]

- 优化 Solon 文件类型配置，移除扩展名限制以避免与通用文件类型冲突

## [0.1.8]

- 优化 '类'、'字段'的高亮处理（避免 jdk 注解高亮）

## [0.1.7]

- 添加'类'、'字段'的高亮
- 添加 Solon 应用运行支持功能
- 优化 Solon 插件运行配置和 UI 组件
- 重构 Solon 主类扫描器，提取注解检测逻辑到工具类
- 重构 Solon 运行配置，复用 IntelliJ 标准 Application 配置

## [0.1.6]

- 配置提示支持复杂类型跳转
- 优化property输入体验
- 优化yml输入体验
- 优化yml代码提示

## [0.1.5]

- pluginUntilBuild 升为 252

## [0.1.4]

### Updated

- fixed: Read access is allowed from inside read-action only



## [0.1.3]

### Updated

- Configuration property names use entity field names 


## [0.1.2]

### Updated

- fix issues:
    - com.intellij.serviceContainer.AlreadyDisposedException: Already disposed: Module: 'xxx' (disposed)

## [0.1.1]

### Updated

- fix issues:
    - Too many non-blocking read actions submitted at once. Please use coalesceBy, BoundedTaskExecutor or another way of
      limiting the number of concurrently running threads.: 11 with similar stack traces are currently active
    - No dependencies provided which causes CachedValue to be never recalculated again. If this is intentional, please
      use ModificationTracker.NEVER_CHANGED

## [0.1.0]

### Updated

- Only support IDEA 243.* version
- Support configuration data hints and property navigation for yml and properties files

## [0.0.12]

### Updated

- IDEA 243.* version support
- Replace deprecated interfaces

## [0.0.11]

### Updated

- IDEA 243.* version support

## [0.0.10]

### Updated

- IDEA 242.* version support

## [0.0.9]

### Updated

- IDEA 241.* version support

## [0.0.8]

### Updated

- Fix bug in initializer

## [0.0.7]

### Updated

- Modify the initializer interface
- Initializer UI optimization
- Compatible with version 2023.3
- Abandon using abandoned interfaces

## [0.0.7-M2]

### Updated

- Initializer UI optimization
- Compatible with version 2023.3
- Abandon using abandoned interfaces

## [0.0.7-M1]

### Updated

- Modify the initializer interface

## [0.0.6]

### Added

- Code prompt garbled
- Fix configuration issues under native project
- Fix the issue of occasionally not displaying configuration prompts

## [0.0.6-M4]

### Added

- Code prompt garbled
- Fix configuration issues under native project
- Fix the issue of occasionally not displaying configuration prompts

## [0.0.6-M3]

### Added

- Fix configuration issues under native project
- Fix the issue of occasionally not displaying configuration prompts

## [0.0.6-M2]

### Added

- Code prompt garbled

## [0.0.6-M1]

### Added

- Code prompt garbled

## [0.0.5]

### Added

- Fix exceptions
- Fix various types of conversion exceptions
- Yaml code prompts for adding comments
- Optimize yaml code tips
- ui formate

## [0.0.5-M4]

### Added

- Fix exceptions

## [0.0.5-M3]

### Added

- Fix exceptions

## [0.0.5-M2]

### Added

- Fix various types of conversion exceptions
- ui formate

## [0.0.5-M1]

### Added

- Fix various types of conversion exceptions
- Yaml code prompts for adding comments
- Optimize yaml code tips

## [0.0.4]

### Added

- adjust ui
- After creating the project, the value of java. version in pom.xml is incorrect
- handle Assertion failed
- Abnormal display of interface in the Mac system

## [0.0.4-M5]

### Added

- [Feature] adjust ui

## [0.0.4-M4]

### Added

- [fix] After creating the project, the value of java. version in pom.xml is incorrect

## [0.0.4-M3]

### Added

- Attempting to handle Assertion failed

## [0.0.4-M2]

### Added

- Attempting to handle Assertion failed

## [0.0.4-M1]

### Added

- Abnormal display of interface in the Mac system

## [0.0.3]

### Added

- IDEA Compatible with 2023.2.1 version.
- Yaml code supplement.
- Initializer ux logic optimization.
- The class with @Controller and its corresponding method will cancel unused.
- Unassigned warning for private field cancellation with @Inject.

## [0.0.3-M1]

### Added

- [Feature] support yml key hint
- [Feature] Optimization of yaml support

## [0.0.2-M1]

### Added

- [Feature] support yml key hint
- [Feature] Optimization of yaml support

## [0.0.1-M8]

### Added

- [Feature] Suppresses unused warnings for beans annotated with solons
- [Feature] Suppresses unassigned warnings for private fields with solon annotations

## [0.0.1-M7]

### Added

- Initializer ux logic optimization
- Removing Pulling Metadata Popup
- [fix] app.properties code hint bug

## [0.0.1-M6]

### Added

- [Feature] fix packing loss

## [0.0.1-M5]

### Added

- [Feature] Group and Artifact are associated with Package name
- [Feature] Use IDEA Default Project directory 'as the default location

## [0.0.1-M4]

### Added

- IDEA Compatible with 2023.2.1 version

## [0.0.1-M3]

### Added

- Provides Solon Initializr New Project wizard.
- Autocompletion for Properties configuration files.

## [0.0.1-M2]

### Added

- Provides Solon Initializr New Project wizard.
- Autocompletion for Properties configuration files.

## [0.0.1-M1]

### Added

- Provides Solon Initializr New Project wizard.
- Autocompletion for Properties configuration files.