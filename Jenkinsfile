// MineAndroid Jenkins Pipeline
// 适用 Jenkins 2.540+, JDK 17, Gradle 8.13, Android SDK 35
pipeline {
    agent any

    parameters {
        choice(
            name: 'BUILD_TYPE',
            choices: ['debug', 'release'],
            description: '构建类型'
        )
        choice(
            name: 'MODULE',
            choices: ['all', 'demo:app-test', 'board:proxy'],
            description: '要构建的模块'
        )
        string(
            name: 'BRANCH',
            defaultValue: 'master',
            description: 'Git 分支'
        )
    }

    environment {
        // JDK 17 — 需在 Jenkins Global Tool Configuration 中配置名为 "JDK17" 的 JDK
        JAVA_HOME = tool name: 'JDK17', type: 'jdk'
        // Android SDK — 需在 Jenkins Global Tool Configuration 中配置名为 "AndroidSDK" 的 SDK
        ANDROID_HOME = tool name: 'AndroidSDK', type: 'hudson.plugins.android__emulator.SdkInstallation'
        // 确保 Gradle 使用项目内的 wrapper
        GRADLE_USER_HOME = "${WORKSPACE}/.gradle-cache"
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "refs/heads/${params.BRANCH}"]],
                        doGenerateSubmoduleConfigurations: false,
                        extensions: [[$class: 'CleanCheckout']],
                        submoduleCfg: [],
                        userRemoteConfigs: [[
                            url: 'git@github.com:Cuinings/MineAndroid.git',
                            credentialsId: 'github-ssh'
                        ]]
                    ])
                }
            }
        }

        stage('Setup') {
            steps {
                script {
                    // 确保 gradlew 有执行权限
                    sh 'chmod +x gradlew'

                    // 检查 Android SDK
                    sh '''
                        echo "=== JDK 版本 ==="
                        java -version
                        echo "=== ANDROID_HOME ==="
                        echo ${ANDROID_HOME}
                        echo "=== 检查必要的 SDK 组件 ==="
                        if [ -d "${ANDROID_HOME}/platforms/android-35" ]; then
                            echo "Android SDK 35 已安装"
                        else
                            echo "警告: Android SDK 35 未找到，尝试通过 sdkmanager 安装..."
                            yes | ${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager "platforms;android-35" "build-tools;35.0.0" || true
                        fi
                    '''
                }
            }
        }

        stage('Clean') {
            steps {
                sh './gradlew clean'
            }
        }

        stage('Build') {
            steps {
                script {
                    def gradleTasks = getGradleTasks(params.BUILD_TYPE, params.MODULE)
                    def buildTypeUpper = params.BUILD_TYPE.capitalize()

                    sh """
                        export ANDROID_HOME=${ANDROID_HOME}
                        export PATH=\${ANDROID_HOME}/platform-tools:\${ANDROID_HOME}/cmdline-tools/latest/bin:\${PATH}
                        ./gradlew ${gradleTasks} --parallel --build-cache --no-daemon
                    """
                }
            }
        }

        stage('Unit Test') {
            steps {
                script {
                    def testTasks = getTestTasks(params.BUILD_TYPE, params.MODULE)
                    if (testTasks) {
                        sh """
                            export ANDROID_HOME=${ANDROID_HOME}
                            export PATH=\${ANDROID_HOME}/platform-tools:\${ANDROID_HOME}/cmdline-tools/latest/bin:\${PATH}
                            ./gradlew ${testTasks} --no-daemon
                        """
                    } else {
                        echo '当前模块无单元测试任务，跳过'
                    }
                }
            }
        }

        stage('Lint Check') {
            steps {
                script {
                    def lintTasks = getLintTasks(params.BUILD_TYPE, params.MODULE)
                    if (lintTasks) {
                        sh """
                            export ANDROID_HOME=${ANDROID_HOME}
                            export PATH=\${ANDROID_HOME}/platform-tools:\${ANDROID_HOME}/cmdline-tools/latest/bin:\${PATH}
                            ./gradlew ${lintTasks} --no-daemon
                        """
                    } else {
                        echo '当前模块无 Lint 任务，跳过'
                    }
                }
            }
        }

        stage('Archive Artifacts') {
            steps {
                script {
                    // 归档所有生成的 APK
                    archiveArtifacts artifacts: '**/build/outputs/apk/**/*.apk', fingerprint: true

                    // 归档测试报告
                    junit testDataPublishers: [[$class: 'StabilityTestDataPublisher']],
                        testResults: '**/build/test-results/test*UnitTest/*.xml',
                        allowEmptyResults: true

                    // 归档 Lint 报告
                    androidLint pattern: '**/build/reports/lint-results-*.xml',
                        allowEmptyResults: true
                }
            }
        }
    }

    post {
        always {
            // 清理 Gradle 缓存，释放空间
            cleanWs(
                cleanWhenAborted: true,
                cleanWhenFailure: true,
                cleanWhenNotBuilt: true,
                cleanWhenSuccess: false,
                deleteDirs: true,
                notFailBuild: true,
                patterns: [[pattern: '.gradle-cache/caches/', type: 'EXCLUDE']]
            )
        }
        success {
            script {
                def appModules = getApplicationModules(params.MODULE)
                echo "========================================="
                echo "  构建成功！"
                echo "  分支: ${params.BRANCH}"
                echo "  构建类型: ${params.BUILD_TYPE}"
                echo "  模块: ${params.MODULE}"
                echo "  构建编号: ${BUILD_NUMBER}"
                echo "========================================="
                echo "  APK 文件可在构建页面的 Artifacts 中下载"
                echo "========================================="
            }
        }
        failure {
            script {
                echo "========================================="
                echo "  构建失败！请检查控制台输出排查问题"
                echo "  构建编号: ${BUILD_NUMBER}"
                echo "  构建日志: ${BUILD_URL}console"
                echo "========================================="
            }
            // 可选: 发送邮件通知
            // emailext (
            //     subject: "[Jenkins] 构建失败: ${JOB_NAME} #${BUILD_NUMBER}",
            //     body: "构建详情: ${BUILD_URL}",
            //     to: 'your-email@example.com'
            // )
        }
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '20', daysToKeepStr: '30'))
        disableConcurrentBuilds()
        timeout(time: 60, unit: 'MINUTES')
        timestamps()
    }
}

// ==========================================
// 辅助方法
// ==========================================

/**
 * 获取当前激活的应用模块列表
 */
def getApplicationModules(String module) {
    if (module == 'all') {
        return [':demo:app-test', ':board:proxy']
    }
    return [":${module}"]
}

/**
 * 根据构建类型和模块生成 Gradle 构建任务
 */
def getGradleTasks(String buildType, String module) {
    def type = buildType.capitalize()

    if (module == 'all') {
        // 构建所有应用模块
        return ":demo:app-test:assemble${type} :board:proxy:assemble${type}"
    }
    return ":${module}:assemble${type}"
}

/**
 * 根据构建类型和模块生成 Gradle 测试任务
 */
def getTestTasks(String buildType, String module) {
    def type = buildType.capitalize()

    if (module == 'all') {
        // 运行所有模块的单元测试
        return ":demo:app-test:test${type}UnitTest :board:proxy:test${type}UnitTest"
    }
    return ":${module}:test${type}UnitTest"
}

/**
 * 根据构建类型和模块生成 Gradle Lint 任务
 */
def getLintTasks(String buildType, String module) {
    def type = buildType.capitalize()

    if (module == 'all') {
        return ":demo:app-test:lint${type} :board:proxy:lint${type}"
    }
    return ":${module}:lint${type}"
}
