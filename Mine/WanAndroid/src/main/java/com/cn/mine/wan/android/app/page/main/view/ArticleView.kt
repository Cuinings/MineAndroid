package com.cn.mine.wan.android.app.page.main.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.chad.library.adapter.base.BaseBinderAdapter
import com.chad.library.adapter.base.binder.QuickDataBindingItemBinder
import com.cn.mine.wan.android.data.entity.ArticleEntity
import com.cn.mine.wan.android.data.entity.CommonPageData
import com.cn.mine.wan.android.databinding.ItemArticleBinding

class ArticleView: SwipeRefreshLayout {

    private var mAdapter = BaseBinderAdapter(arrayListOf())

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): super(context, attrs) { initArticleView() }

    private fun initArticleView() {
        addView(RecyclerView(context).apply {
            layoutParams = RecyclerView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter.apply {
                addItemBinder(ArticleItem())
            }
        })
    }

    fun addArticle(articles: CommonPageData<ArticleEntity>) {
        Log.d(ArticleView::class.simpleName, "addArticle: $articles")
        articles.takeIf { it.curPage == 1 }?.let { mAdapter.setList(it.datas) }?:mAdapter.addData(articles.datas)
        isRefreshing = false
    }

    inner class ArticleItem: QuickDataBindingItemBinder<ArticleEntity, ItemArticleBinding>() {
        override fun onCreateDataBinding(layoutInflater: LayoutInflater, parent: ViewGroup, viewType: Int): ItemArticleBinding = ItemArticleBinding.inflate(layoutInflater, parent, false)
        override fun convert(holder: BinderDataBindingHolder<ItemArticleBinding>, data: ArticleEntity) {
            holder.dataBinding.article = data
        }
    }
}