package com.cn.board.contacts.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cn.board.contacts.data.Department
import com.cn.board.contacts.data.Employee
import com.cn.board.contacts.repository.OrganizationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OrganizationUiState(
    val isLoading: Boolean = true,
    val departments: List<Department> = emptyList(),
    val employees: List<Employee> = emptyList(),
    val selectedDepartment: Department? = null,
    val filteredEmployees: List<Employee> = emptyList(),
    val searchKeyword: String = "",
    val error: String? = null
)

class OrganizationViewModel(
    private val repository: OrganizationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrganizationUiState())
    val uiState: StateFlow<OrganizationUiState> = _uiState.asStateFlow()

    init {
        loadOrganizationData()
    }

    fun loadOrganizationData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val departments = repository.getAllDepartments()
                val employees = repository.getAllEmployees()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    departments = departments,
                    employees = employees,
                    filteredEmployees = employees
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun selectDepartment(department: Department?) {
        viewModelScope.launch {
            val filteredEmployees = if (department != null) {
                repository.getEmployeesByDepartment(department.id)
            } else {
                repository.getAllEmployees()
            }
            
            _uiState.value = _uiState.value.copy(
                selectedDepartment = department,
                filteredEmployees = filteredEmployees
            )
        }
    }

    fun searchEmployees(keyword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(searchKeyword = keyword)
            
            val filteredEmployees = if (keyword.isBlank()) {
                if (_uiState.value.selectedDepartment != null) {
                    repository.getEmployeesByDepartment(_uiState.value.selectedDepartment!!.id)
                } else {
                    repository.getAllEmployees()
                }
            } else {
                repository.searchEmployees(keyword)
            }
            
            _uiState.value = _uiState.value.copy(filteredEmployees = filteredEmployees)
        }
    }

    fun getEmployeeById(employeeId: String): Employee? {
        return repository.getEmployeeById(employeeId)
    }

    fun getDepartmentById(departmentId: String): Department? {
        return repository.getDepartmentById(departmentId)
    }

    fun getSubDepartments(parentId: String?): List<Department> {
        return repository.getSubDepartments(parentId)
    }

    fun refresh() {
        loadOrganizationData()
    }
}
