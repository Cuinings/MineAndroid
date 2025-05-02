package com.cn.mine.wan.android.ui.main

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.cn.library.commom.viewmodel.BasicViewModel
import com.cn.library.commom.viewmodel.UIEvent
import com.cn.library.commom.viewmodel.UIState
import com.cn.mine.wan.android.data.entity.ArticleEntity
import com.cn.mine.wan.android.data.entity.CommonPageData
import com.cn.mine.wan.android.data.entity.result
import com.cn.mine.wan.android.net.WanAndroidAPI
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(): BasicViewModel<MainActivityUIState, MainActivityUIEvent>() {

    @Inject
    lateinit var wanAndroidAPI: WanAndroidAPI

    override fun initUIState(): MainActivityUIState {
        Log.d(MainActivityViewModel::class.simpleName, "initUIState: ")
        return MainActivityUIState(ArticleUIState.INIT)
    }

    override fun handleEvent(event: MainActivityUIEvent) {
        Log.d(TAG, "handleEvent: $event")
        when (event) {
            is MainActivityUIEvent.GetArticle -> viewModelScope.launch { wanAndroidAPI.article(event.page).result({
                sendUiState { copy(articleUIState = ArticleUIState.Article(it)) }
            }) {
                sendUiState { copy(articleUIState = ArticleUIState.ArticleFinish(it)) }
            } }
        }
    }
}

data class  MainActivityUIState(val articleUIState: ArticleUIState): UIState {}

sealed class ArticleUIState: UIState {
    data object INIT: ArticleUIState()
    data class Article(val articles: CommonPageData<ArticleEntity>?, ): ArticleUIState()
    data class ArticleFinish(val msg: String? = null): ArticleUIState()
}

sealed class MainActivityUIEvent: UIEvent {
    data class GetArticle(val page: Int): MainActivityUIEvent()
}