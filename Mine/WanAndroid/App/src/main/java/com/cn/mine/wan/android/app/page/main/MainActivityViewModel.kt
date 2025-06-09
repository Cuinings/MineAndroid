package com.cn.mine.wan.android.app.page.main

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.cn.library.commom.viewmodel.BasicViewModel
import com.cn.library.commom.viewmodel.UIEvent
import com.cn.library.commom.viewmodel.UIState
import com.cn.library.common.flow.collectByScope
import com.cn.mine.wan.android.app.page.main.ArticleUIState.Article
import com.cn.mine.wan.android.app.page.main.ArticleUIState.ArticleFinish
import com.cn.mine.wan.android.data.entity.ArticleEntity
import com.cn.mine.wan.android.data.entity.CommonEntity
import com.cn.mine.wan.android.data.entity.CommonPageEntity
import com.cn.mine.wan.android.events.EventResult
import com.cn.mine.wan.android.events.article.ArticleEvent
import com.cn.mine.wan.android.events.banner.BannerEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(): BasicViewModel<MainActivityUIState, MainActivityUIEvent>() {

    @Inject
    lateinit var bannerEvent: BannerEvent
    @Inject
    lateinit var articleEvent: ArticleEvent

    override fun initUIState(): MainActivityUIState {
        Log.d(MainActivityViewModel::class.simpleName, "initUIState: ")
        return MainActivityUIState(ArticleUIState.INIT)
    }

    override fun handleEvent(event: MainActivityUIEvent) {
        Log.d(TAG, "handleEvent: $event")
        when (event) {
            MainActivityUIEvent.GetBanner -> {
                bannerEvent.execute(null).collectByScope(viewModelScope) {
                    Log.d(TAG, "GetBanner execute result: $it")
                }
            }
            is MainActivityUIEvent.GetArticle -> {
                articleEvent.execute(ArticleEvent.ArticleParam(event.page)).collectByScope(viewModelScope) {
                    when(it) {
                        is EventResult.Loading -> {}
                        is EventResult.Success<CommonEntity<CommonPageEntity<ArticleEntity>>> -> {
                            Log.d(TAG, "GetArticle execute result: ${it.result?.data?.size}")
                            sendUiState { copy(articleUIState = Article(it.result?.data)) }
                        }
                        is EventResult.Failed -> {
                            sendUiState { copy(articleUIState = ArticleFinish()) }
                        }
                    }
                }
            }
        }
    }


}

data class  MainActivityUIState(val articleUIState: ArticleUIState): UIState {}

sealed class ArticleUIState: UIState {
    data object INIT: ArticleUIState()
    data class Article(val articles: CommonPageEntity<ArticleEntity>?): ArticleUIState()
    data class ArticleFinish(val msg: String? = null): ArticleUIState()
}

sealed class MainActivityUIEvent: UIEvent {
    object GetBanner: MainActivityUIEvent()
    data class GetArticle(val page: Int): MainActivityUIEvent()
}