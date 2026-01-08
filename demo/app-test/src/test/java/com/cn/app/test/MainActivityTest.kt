package com.cn.app.test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import com.google.common.truth.Truth.assertThat

/**
 * MainActivity 测试类
 *
 * 遵循统一测试规范：
 * 1. 使用 JUnit 4 注解
 * 2. 使用 Truth 进行断言
 * 3. 使用 Mockito 进行 mocking
 * 4. 编写有意义的测试方法名和显示名称
 * 5. 遵循 AAA 模式
 */
@RunWith(MockitoJUnitRunner::class)
class MainActivityTest {

    // 被测试的类
    private lateinit var mainActivity: MainActivity

    // Mock 对象
    @Mock
    private lateinit var mockBundle: Bundle

    @Before
    fun setUp() {
        // 初始化测试环境
        mainActivity = MainActivity()
    }

    @Test
    fun testActivityCreation() {
        // Arrange - 已经在 setUp 中完成

        // Act & Assert - 验证 Activity 创建时不抛出异常
        assertThat(mainActivity).isNotNull()
    }

    @Test
    fun testActivityIsInstanceOfAppCompatActivity() {
        // Arrange - 已经在 setUp 中完成

        // Act & Assert - 验证 Activity 类型
        assertThat(mainActivity).isInstanceOf(AppCompatActivity::class.java)
    }

    @Test
    fun testMockitoConfiguration() {
        // Arrange - 设置 Mock 对象的行为
        `when`(mockBundle.getString("test_key")).thenReturn("test_value")

        // Act - 使用 Mock 对象
        val result = mockBundle.getString("test_key")

        // Assert - 验证结果
        assertThat(result).isEqualTo("test_value")
    }
}