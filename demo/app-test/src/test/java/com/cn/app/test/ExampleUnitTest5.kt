package com.cn.app.test

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import com.google.common.truth.Truth.assertThat

/**
 * JUnit 4 示例测试类
 *
 * 演示了 JUnit 4 的基本用法和 Truth 断言
 */
@RunWith(MockitoJUnitRunner::class)
class ExampleUnitTest5 {

    // 被测试的类
    private lateinit var calculator: Calculator

    // Mock 对象
    @Mock
    private lateinit var mockService: Service

    @Before
    fun setUp() {
        // 初始化测试环境
        calculator = Calculator(mockService)
    }

    @Test
    fun testAddition() {
        // Arrange
        val a = 2
        val b = 3
        val expected = 5

        // Act
        val result = calculator.add(a, b)

        // Assert
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun testTruthAssertion() {
        // Arrange
        val list = listOf(1, 2, 3)

        // Act & Assert
        assertThat(list).contains(2)
        assertThat(list).hasSize(3)
        assertThat(list).isNotEmpty()
    }

    @Test
    fun testMockitoUsage() {
        // Arrange
        val mockResult = "mocked result"
        `when`(mockService.getValue()).thenReturn(mockResult)

        // Act
        val result = calculator.useService()

        // Assert
        assertThat(result).isEqualTo(mockResult)
    }

    // 辅助类
    class Calculator(private val service: Service) {
        fun add(a: Int, b: Int) = a + b
        fun useService() = service.getValue()
    }

    interface Service {
        fun getValue(): String
    }
}