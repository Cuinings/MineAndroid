package com.cn.mine.wan.android.app.page.entrance

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.cn.library.common.activity.BasicVBActivity
import com.cn.library.common.flow.collectByScope
import com.cn.mine.wan.android.app.databinding.ActivityEntranceBinding
import com.cn.mine.wan.android.app.databinding.ActivityEntranceBinding.inflate
import com.cn.mine.wan.android.app.page.main.ArticleRecommendationActivity
import com.cn.mine.wan.android.entity.EntranceEntity
import com.cn.mine.wan.android.entity.EntranceType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map

/**
 * @Author: CuiNing
 * @Time: 2025/6/14 10:34
 * @Description:
 */
@AndroidEntryPoint
class EntranceActivity : BasicVBActivity<ActivityEntranceBinding>({ inflate(it) })  {

    private val viewModel by viewModels<EntranceActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.uiStateFlow.map { it.entranceUIState }.collectByScope(lifecycleScope) {
            when(it) {
                EntranceUIState.INIT -> viewModel.sendUIIntent(EntranceActivityUIEvent.LoadEntrance)
                is EntranceUIState.Entrance -> { binding.entrance.data = it.entrances }
            }
        }
        binding.entrance.onClickListener = object: EntranceView.OnClickListener {
            override fun onClick(entrance: EntranceEntity) {
                when(entrance.type) {
                    EntranceType.ARTICLE_RECOMMENDATION -> ArticleRecommendationActivity::class.java
                    EntranceType.HARMONY -> ArticleRecommendationActivity::class.java
                    EntranceType.MINE -> ArticleRecommendationActivity::class.java
                }.let {
                    startActivity(Intent(this@EntranceActivity, it))
                }
            }
        }
    }

}