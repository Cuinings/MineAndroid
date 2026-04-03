package com.cn.board.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cn.board.contacts.repository.OrganizationRepository
import com.cn.board.contacts.viewmodel.OrganizationViewModel

class OrganizationViewModelFactory(
    private val repository: OrganizationRepository
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OrganizationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OrganizationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
