<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Solon Changelog

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