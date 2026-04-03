package com.cn.board.contacts

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.cn.board.contacts.repository.OrganizationRepository
import com.cn.board.contacts.viewmodel.OrganizationViewModel
import kotlinx.coroutines.launch

class ContactsActivity : AppCompatActivity() {

    private lateinit var viewModel: OrganizationViewModel
    private val repository by lazy { OrganizationRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_contacts)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) {
            v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViewModel()
        observeData()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            OrganizationViewModelFactory(repository)
        )[OrganizationViewModel::class.java]
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when {
                    state.isLoading -> {
                        Log.d("ContactsActivity", "Loading data...")
                    }
                    state.error != null -> {
                        Log.e("ContactsActivity", "Error: ${state.error}")
                    }
                    else -> {
                        Log.d("ContactsActivity", "Data loaded successfully")
                        Log.d("ContactsActivity", "Departments: ${state.departments.size}")
                        Log.d("ContactsActivity", "Employees: ${state.employees.size}")
                        
                        state.departments.take(3).forEach { dept ->
                            Log.d("ContactsActivity", "Department: ${dept.name} (${dept.id})")
                        }
                        
                        state.employees.take(3).forEach { emp ->
                            Log.d("ContactsActivity", "Employee: ${emp.name} - ${emp.position}")
                        }
                    }
                }
            }
        }
    }
}
