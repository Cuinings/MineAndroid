# Android工程配置到Jenkins详细指南

## 一、准备工作

### 1. 确保Jenkins已安装并运行
- 访问 `http://localhost:8080` 确认Jenkins正常运行

### 2. 安装必要的Jenkins插件

Jenkins 2.540版本支持以下插件，通过「Manage Jenkins」→「Manage Plugins」→「Available」安装：

| 插件名称 | 版本要求 | 用途 |
|---------|---------|------|
| Git Plugin | 4.15.1+ | Git源码管理 |
| Android Lint Plugin | 2.6+ | Android Lint报告集成 |
| Gradle Plugin | 2.8+ | Gradle构建支持 |
| JUnit Plugin | 1.60+ | 单元测试报告 |
| Email Extension Plugin | 2.92+ | 构建通知 |
| Pipeline | 2.89+ | 流水线支持（推荐） |

> 注意：Jenkins 2.540版本的插件管理界面与新版本略有不同，插件列表可能显示方式有所差异。

### 3. 确保工程已提交到Git仓库
```bash
# 初始化Git仓库（如果尚未初始化）
git init
# 添加所有文件
git add .
# 提交到本地仓库
git commit -m "Initial commit"
# 推送到远程仓库（如果需要）
git remote add origin [远程仓库URL]
git push -u origin master
```

## 二、创建Jenkins作业

### 1. 选择作业类型

- **推荐**：Pipeline作业（使用Jenkinsfile，便于版本控制和维护）
- **备选**：Freestyle项目（适合简单构建）

### 2. 创建Pipeline作业

#### 2.1 新建作业
- 登录Jenkins，点击「New Item」
- 输入作业名称（如「MineAndroid-Build」）
- 选择「Pipeline」，点击「OK」

#### 2.2 配置作业

##### 2.2.1 基本配置
- 描述：添加作业描述（可选）
- 丢弃旧的构建：设置构建保留策略（如保留30天，最多10个构建）

##### 2.2.2 构建触发器
- 选择「Poll SCM」：定期检查Git仓库变化
- 日程表：`H/15 * * * *`（每15分钟检查一次）
- 或选择「GitHub hook trigger for GITScm polling」（配合GitHub Webhook）

##### 2.2.3 流水线配置
- 定义：选择「Pipeline script from SCM」
- SCM：Git
- Repository URL：输入Git仓库URL
- Credentials：添加Git凭证（如果需要）
- Branch Specifier：`*/master`（或其他分支）
- Script Path：`Jenkinsfile`（Jenkinsfile文件路径）

## 三、编写Jenkinsfile

在工程根目录创建`Jenkinsfile`文件，内容如下（兼容Jenkins 2.540版本）：

```groovy
pipeline {
    agent any
    
    environment {
        ANDROID_HOME = tool 'Android SDK'
        JAVA_HOME = tool 'JDK 17'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Clean') {
            steps {
                sh './gradlew clean'
            }
        }
        
        stage('Build Debug') {
            steps {
                withEnv(["PATH+ANDROID=$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools"]) {
                    sh './gradlew assembleDebug'
                }
            }
        }
        
        stage('Run Tests') {
            steps {
                withEnv(["PATH+ANDROID=$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools"]) {
                    sh './gradlew testDebugUnitTest'
                }
            }
        }
        
        stage('Lint Check') {
            steps {
                withEnv(["PATH+ANDROID=$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools"]) {
                    sh './gradlew lintDebug'
                }
            }
        }
        
        stage('Archive Artifacts') {
            steps {
                archiveArtifacts artifacts: 'Sample/Test/build/outputs/apk/debug/*.apk', fingerprint: true
                junit '**/build/test-results/testDebugUnitTest/*.xml'
                androidLint pattern: '**/build/reports/lint-results-*.xml'
            }
        }
    }
    
    post {
        success {
            emailext (
                subject: 'Jenkins Build Success: ${JOB_NAME} #${BUILD_NUMBER}',
                body: 'Build Details: ${BUILD_URL}',
                to: 'your-email@example.com'
            )
        }
        failure {
            emailext (
                subject: 'Jenkins Build Failed: ${JOB_NAME} #${BUILD_NUMBER}',
                body: 'Build Details: ${BUILD_URL}',
                to: 'your-email@example.com'
            )
        }
    }
}
```

> 注意：Jenkins 2.540版本的Pipeline插件对环境变量处理与新版本略有不同，推荐使用`withEnv`方法来设置临时环境变量。

## 四、配置Jenkins工具

### 1. 配置JDK
在Jenkins 2.540版本中配置JDK：
- 导航到：「Manage Jenkins」→「Global Tool Configuration」→「JDK」
- 点击「Add JDK」
- 填写名称：「JDK 17」
- 取消勾选「Install automatically」（Jenkins 2.540版本的自动安装功能可能不稳定）
- 直接输入JDK主目录：`C:\Program Files\Java\jdk-17.0.17.10`（根据实际安装路径调整）
- 点击「Save」

### 2. 配置Android SDK
在Jenkins 2.540版本中配置Android SDK：
- 导航到：「Manage Jenkins」→「Global Tool Configuration」→「Android SDK」
- 点击「Add Android SDK」
- 输入名称：「Android SDK」
- 选择「Install automatically」或输入本地SDK路径
- 点击「Save」

> 注意：Jenkins 2.540版本的「Global Tool Configuration」界面布局与新版本有所不同，需要滚动查找相关配置项。

## 五、测试构建

### 1. 手动触发构建
- 进入Jenkins作业页面
- 点击「Build Now」手动触发构建
- 查看「Console Output」查看构建日志

### 2. 构建成功的标志
- 构建状态显示为「Success」
- 可以在「Artifacts」中下载生成的APK文件
- 测试报告和Lint报告可以在作业页面查看

## 六、常见问题及解决方法

### 1. 构建失败：Android SDK未找到
- 检查ANDROID_HOME环境变量是否正确设置
- 确保Android SDK已安装必要的SDK版本（compileSdk = 35）

### 2. 构建失败：权限问题
- 确保Jenkins用户有执行`gradlew`的权限：`chmod +x gradlew`

### 3. 构建速度慢
- 启用Gradle缓存
- 使用并行构建：`./gradlew assembleDebug --parallel`
- 配置Jenkins执行器数量

## 七、高级配置

### 1. 配置构建参数

在Jenkins 2.540版本中添加构建参数：

- 勾选「This project is parameterized」
- 点击「Add Parameter」，选择「Choice Parameter」
  - 名称：BUILD_TYPE
  - 选择项：debug,release
  - 描述：构建类型
- 点击「Add Parameter」，选择「String Parameter」
  - 名称：BRANCH
  - 默认值：master
  - 描述：Git分支

> 注意：Jenkins 2.540版本的「Branch Parameter」插件可能与新版本不兼容，推荐使用String Parameter代替。

### 2. 配置Gradle缓存加速构建

Jenkins 2.540版本支持Gradle缓存，但需要手动配置：

```groovy
pipeline {
    // ...
    stages {
        stage('Build') {
            steps {
                withEnv(["PATH+ANDROID=$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools",
                        "GRADLE_USER_HOME=$HOME/.gradle"]) {
                    sh './gradlew assembleDebug --gradle-user-home=$GRADLE_USER_HOME'
                }
            }
        }
        // ...
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    // ...
}

> 注意：Jenkins 2.540版本不支持`gradleWrapperLifecycle()`选项，需要手动配置Gradle缓存路径。
```

## 八、附录

### 1. 常用Gradle命令

| 命令 | 用途 |
|------|------|
| `./gradlew clean` | 清理构建产物 |
| `./gradlew assembleDebug` | 构建Debug版本APK |
| `./gradlew assembleRelease` | 构建Release版本APK |
| `./gradlew testDebugUnitTest` | 运行Debug单元测试 |
| `./gradlew lintDebug` | 执行Debug版本Lint检查 |

### 2. Jenkinsfile示例（带参数构建 - 兼容Jenkins 2.540）

```groovy
pipeline {
    agent any
    
    parameters {
        choice(name: 'BUILD_TYPE', choices: ['debug', 'release'], description: '构建类型')
        string(name: 'BRANCH', defaultValue: 'master', description: 'Git分支')
    }
    
    environment {
        ANDROID_HOME = tool 'Android SDK'
        JAVA_HOME = tool 'JDK 17'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: params.BRANCH]],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [],
                    submoduleCfg: [],
                    userRemoteConfigs: [[url: 'git@github.com:yourusername/MineAndroid.git']]
                ])
            }
        }
        
        stage('Build') {
            steps {
                withEnv(["PATH+ANDROID=$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools"]) {
                    sh "./gradlew assemble${params.BUILD_TYPE.capitalize()}"
                }
            }
        }
        
        stage('Test') {
            steps {
                withEnv(["PATH+ANDROID=$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools"]) {
                    sh "./gradlew test${params.BUILD_TYPE.capitalize()}UnitTest"
                }
            }
        }
        
        stage('Archive') {
            steps {
                archiveArtifacts artifacts: "**/build/outputs/apk/${params.BUILD_TYPE}/*.apk", fingerprint: true
            }
        }
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
}

> 注意：Jenkins 2.540版本不支持`gitParameter`，使用`string`参数代替；同时需要使用`withEnv`管理环境变量。
```

---

**生成时间**：2026-01-09
**适用Jenkins版本**：2.540
**适用Android Gradle Plugin版本**：8.9.2
**适用Gradle版本**：8.13