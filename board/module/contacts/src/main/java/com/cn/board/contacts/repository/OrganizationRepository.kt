package com.cn.board.contacts.repository

import android.content.Context
import com.cn.board.contacts.data.Department
import com.cn.board.contacts.data.Employee
import com.cn.board.contacts.data.OrganizationData
import com.google.gson.Gson
import java.io.InputStreamReader

class OrganizationRepository(private val context: Context) {

    private val gson = Gson()

    fun loadOrganizationData(): OrganizationData? {
        return try {
            val inputStream = context.assets.open("organization.json")
            val reader = InputStreamReader(inputStream)
            gson.fromJson(reader, OrganizationData::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getAllDepartments(): List<Department> {
        return loadOrganizationData()?.departments ?: emptyList()
    }

    fun getAllEmployees(): List<Employee> {
        return loadOrganizationData()?.employees ?: emptyList()
    }

    fun getDepartmentById(departmentId: String): Department? {
        return getAllDepartments().find { it.id == departmentId }
    }

    fun getEmployeeById(employeeId: String): Employee? {
        return getAllEmployees().find { it.id == employeeId }
    }

    fun getEmployeesByDepartment(departmentId: String): List<Employee> {
        return getAllEmployees().filter { it.departmentId == departmentId }
    }

    fun getSubDepartments(parentId: String?): List<Department> {
        return getAllDepartments().filter { it.parentId == parentId }
    }

    fun searchEmployees(keyword: String): List<Employee> {
        return getAllEmployees().filter { 
            it.name.contains(keyword, ignoreCase = true) ||
            it.position.contains(keyword, ignoreCase = true) ||
            it.phone.contains(keyword, ignoreCase = true) ||
            it.email.contains(keyword, ignoreCase = true)
        }
    }

    fun searchDepartments(keyword: String): List<Department> {
        return getAllDepartments().filter {
            it.name.contains(keyword, ignoreCase = true) ||
            it.description.contains(keyword, ignoreCase = true)
        }
    }
}
