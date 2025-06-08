package com.cn.mine.wan.android.ui.main

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.cn.library.commom.viewmodel.BasicViewModel
import com.cn.library.commom.viewmodel.UIEvent
import com.cn.library.commom.viewmodel.UIState
import com.cn.library.common.activity.BasicActivity
import com.cn.library.common.application.ApplicationContextExt
import com.cn.library.common.flow.collectByScope
import com.cn.library.utils.network.isConnected
import com.cn.mine.wan.android.data.entity.ArticleEntity
import com.cn.mine.wan.android.data.entity.CommonEntity
import com.cn.mine.wan.android.data.entity.CommonPageEntity
import com.cn.mine.wan.android.data.entity.result
import com.cn.mine.wan.android.events.EventResult
import com.cn.mine.wan.android.events.article.ArticleEvent
import com.cn.mine.wan.android.events.banner.BannerEvent
import com.cn.mine.wan.android.net.WanAndroidAPI
import com.cn.mine.wan.android.ui.main.ArticleUIState.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(): BasicViewModel<MainActivityUIState, MainActivityUIEvent>() {

    @Inject
    lateinit var wanAndroidAPI: WanAndroidAPI

    private val bannerEvent: BannerEvent by lazy { BannerEvent() }
    private val articleEvent: ArticleEvent by lazy { ArticleEvent() }

    override fun initUIState(): MainActivityUIState {
        Log.d(MainActivityViewModel::class.simpleName, "initUIState: ")
        return MainActivityUIState(ArticleUIState.INIT)
    }

    override fun handleEvent(event: MainActivityUIEvent) {
        Log.d(TAG, "handleEvent: $event")
        when (event) {
            MainActivityUIEvent.GetBanner -> {
                bannerEvent.execute(viewModelScope, null) {
                    Log.d(TAG, "GetBanner execute result: $it")
                }
            }
            is MainActivityUIEvent.GetArticle -> {
                articleEvent.execute(viewModelScope, ArticleEvent.ArticleParam(event.page)) {
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
                /*ApplicationContextExt.context.isConnected().collectByScope(viewModelScope) {
                    Log.d(BasicActivity.Companion.TAG, "GetArticle NetWork isConnected : $it")
                    if (it)
                        wanAndroidAPI.article(event.page).result({
                            sendUiState { copy(articleUIState = Article(it)) }
                        }) {
                            sendUiState { copy(articleUIState = ArticleFinish(it)) }
                        }
                }*/
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