package com.cn.board.contacts.data

import com.google.gson.annotations.SerializedName

data class OrganizationData(
    @SerializedName("departments")
    val departments: List<Department>,
    @SerializedName("employees")
    val employees: List<Employee>
)

data class Department(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("parentId")
    val parentId: String?,
    @SerializedName("level")
    val level: Int,
    @SerializedName("description")
    val description: String
)

data class Employee(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("position")
    val position: String,
    @SerializedName("departmentId")
    val departmentId: String,
    @SerializedName("phone")
    val phone: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("avatar")
    val avatar: String,
    @SerializedName("level")
    val level: Int,
    @SerializedName("status")
    val status: String
)
