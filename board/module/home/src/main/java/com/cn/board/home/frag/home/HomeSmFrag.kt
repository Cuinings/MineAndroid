package com.cn.board.home.frag.home

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.cn.board.home.HomeModel.homeAppList
import com.cn.board.home.R
import com.cn.board.home.databinding.FragmentHomeBinding
import com.cn.board.home.databinding.ItemMainSoftBinding
import com.cn.board.home.entity.EmAppType
import com.cn.board.home.entity.SoftEntity
import com.cn.board.home.entity.diff.SoftDiffCallBack
import com.cn.core.ui.fragment.BasicVmDBFragment
import com.cn.core.ui.view.recyclerview.adapter.BaseBinderAdapter
import com.cn.core.ui.view.recyclerview.adapter.animation.BaseAnimation
import com.cn.core.ui.view.recyclerview.adapter.binder.QuickDataBindingItemBinder

/**
 * @author: cn
 * @time: 24/7/2026 下午 3:51
 * @history
 * @description:
 */
class HomeSmFrag: BasicVmDBFragment<HomeSmFragViewModel, FragmentHomeBinding>(
    { it[HomeSmFragViewModel::class.java] },
    { FragmentHomeBinding.inflate(it)}
) {
    private val binderMainAdapter = BaseBinderAdapter()

    override fun onBindViewModel() {
    }

    override fun onBindData() {
        with(binding) {
            with(recyclerView) {
                layoutManager =
                    GridLayoutManager(requireContext(), 2, GridLayoutManager.HORIZONTAL, false)
                addItemDecoration(MainSoftCornerItemDecoration())
                with(binderMainAdapter) {
                    animationEnable = true
                    isAnimationFirstOnly = true
                    adapterAnimation = MainSoftAnimation()
                    addItemBinder(BinderHomePageApp(), SoftDiffCallBack())
                    adapter = this
                }
            }
            moduleHomeActivityHomeFragLeft.post { moduleHomeActivityHomeFragLeft.invalidateOutline() }
        }
    }

    override fun onObserver() {
        homeAppList.observe(this@HomeSmFrag) {
            binderMainAdapter.setDiffNewData(ArrayList<SoftEntity>(it).toMutableList())
            if (binderMainAdapter.animationEnable) binding.root.postDelayed({
                binderMainAdapter.animationEnable = false
            }, 1000)
        }
    }

    class BinderHomePageApp : QuickDataBindingItemBinder<SoftEntity, ItemMainSoftBinding>() {

        @SuppressLint("NewApi")
        override fun convert(
            holder: BinderDataBindingHolder<ItemMainSoftBinding>,
            data: SoftEntity
        ) {
            holder.run {
                dataBinding.info = data
                when (data.appInfo?.appType) {
                    EmAppType.Add -> {
                        holder.dataBinding.ivAdd.visibility = View.VISIBLE
                        holder.dataBinding.ivThirdIcon.visibility = View.GONE
                        holder.dataBinding.ivtpIcon.visibility = View.GONE
                    }

                    else -> {
                        holder.dataBinding.ivAdd.visibility = View.GONE
                        holder.dataBinding.ivThirdIcon.visibility = View.VISIBLE
                        holder.dataBinding.ivtpIcon.visibility = View.VISIBLE
                    }
                }
                itemView.run {
                    tag = data
                }
            }
            convert(holder, data, arrayListOf(1))
        }

        override fun convert(
            holder: BinderDataBindingHolder<ItemMainSoftBinding>,
            data: SoftEntity,
            payloads: List<Any>
        ) {
            if (payloads.isEmpty()) {
                super.convert(holder, data, payloads)
                return
            }
            // DiffUtil dispatched partial update: delegate to full convert
            convert(holder, data)
        }

        override fun onCreateDataBinding(
            layoutInflater: LayoutInflater,
            parent: ViewGroup,
            viewType: Int
        ): ItemMainSoftBinding {
            return ItemMainSoftBinding.inflate(layoutInflater, parent, false)
        }

        override fun onClick(
            holder: BinderDataBindingHolder<ItemMainSoftBinding>,
            view: View,
            data: SoftEntity,
            position: Int
        ) {
        }
    }

    class MainSoftAnimation : BaseAnimation {

        companion object {
            const val DURATION = 500L
        }

        override fun animators(view: View): Array<Animator> {
            return arrayOf(
                ObjectAnimator.ofFloat(view, "scaleY", 1.5f, 1f).setDuration(DURATION)
                    .apply { DecelerateInterpolator() },
                ObjectAnimator.ofFloat(view, "scaleX", 1.5f, 1f).setDuration(DURATION)
                    .apply { DecelerateInterpolator() },
                ObjectAnimator.ofFloat(view, "alpha", 0.5f, 1f).setDuration(DURATION)
                    .apply { DecelerateInterpolator() },
                ObjectAnimator.ofFloat(view, "alpha", 0.5f, 1f).setDuration(DURATION)
                    .apply { DecelerateInterpolator() },
                ObjectAnimator.ofFloat(view, "translationX", 250f, 0f).setDuration(DURATION)
                    .apply { DecelerateInterpolator() },
                ObjectAnimator.ofFloat(view, "translationY", 0f, 0f).setDuration(DURATION)
                    .apply { DecelerateInterpolator() }
            )
        }
    }

    inner class MainSoftCornerItemDecoration : ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            super.getItemOffsets(outRect, view, parent, state)
            val index = parent.getChildAdapterPosition(view)
            if (index == -1) return
            val dimension = resources.getDimensionPixelSize(R.dimen.dp4)
            outRect.top = if (index == 1 || index == 3 || index == 5) dimension / 2 else 0
            outRect.left = if (index == 0 || index == 1) 0 else dimension
            outRect.bottom = 0
            outRect.right = 0
        }
    }
}