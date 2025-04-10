package com.cn.mine.wan.android.app.page.main.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.chad.library.adapter.base.BaseBinderAdapter
import com.chad.library.adapter.base.binder.QuickDataBindingItemBinder
import com.chad.library.adapter.base.module.LoadMoreModule
import com.cn.mine.wan.android.R
import com.cn.mine.wan.android.data.entity.ArticleEntity
import com.cn.mine.wan.android.data.entity.CommonPageData
import com.cn.mine.wan.android.databinding.ItemArticleBinding

class ArticleView: SwipeRefreshLayout {

    companion object {
        val TAG = ArticleView::class.simpleName
    }

    var onRefreshCallBack: OnRefreshCallBack? = null
    var onLoadMoreCallBack: OnLoadMoreCallBack? = null

    private var mAdapter = ArticleAdapter()

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): super(context, attrs) { initArticleView() }

    private fun initArticleView() {
        addView(RecyclerView(context).apply {
            layoutParams = RecyclerView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter.apply {
                addItemBinder(ArticleItem())
                loadMoreModule.run {
                    isEnableLoadMore = true
                    isAutoLoadMore = true
                    setOnLoadMoreListener { onLoadMoreCallBack?.onLoadMore(pageNum) }
                }
            }
        })
        setOnRefreshListener { onRefreshCallBack?.onRefresh() }
    }

    private var pageCount = 0
        set(value) { value.takeIf { it != field }?.let {
            field = it
            Log.d(TAG, "pageCount:$it")
        } }

    private var pageNum = 0
        set(value) { value.takeIf { it != field }?.let {
            field = it
            Log.d(TAG, "pageNum:$it")
        } }

    fun addArticle(articles: CommonPageData<ArticleEntity>?) {
        articles?.run {
            this@ArticleView.pageCount = this.pageCount
            pageNum = curPage
            curPage.takeIf { it == 1 }?.let {
                mAdapter.setList(datas)
            }?: mAdapter.addData(datas)
            mAdapter.loadMoreModule.loadMoreComplete()
            (curPage != pageCount).takeIf { !it }?.let { mAdapter.loadMoreModule.loadMoreEnd() }
        }
    }

    inner class ArticleAdapter: BaseBinderAdapter(arrayListOf()), LoadMoreModule

    inner class ArticleItem: QuickDataBindingItemBinder<ArticleEntity, ItemArticleBinding>() {
        override fun onCreateDataBinding(layoutInflater: LayoutInflater, parent: ViewGroup, viewType: Int): ItemArticleBinding = ItemArticleBinding.inflate(layoutInflater, parent, false)
        override fun convert(holder: BinderDataBindingHolder<ItemArticleBinding>, data: ArticleEntity) {
            holder.dataBinding.article = data
        }
    }

    interface OnRefreshCallBack {
        fun onRefresh()
    }

    interface OnLoadMoreCallBack {
        fun onLoadMore(pageNum: Int)
    }
}

@BindingAdapter("articleAuthor")
fun bindArticleAuthor(view: TextView, author: String) {
    view.text = view.context.resources.getString(R.string.article_author, author)
}

@BindingAdapter("articleChapter")
fun bindArticleChapter(view: TextView, chapter: String) {
    view.text = view.context.resources.getString(R.string.article_chapter, chapter)
}

@BindingAdapter("articleTime")
fun bindArticleTime(view: TextView, time: String) {
    view.text = view.context.resources.getString(R.string.article_time, time)
}