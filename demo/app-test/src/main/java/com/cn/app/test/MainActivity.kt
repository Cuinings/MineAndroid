package com.cn.app.test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cn.app.test.binder.UserBinder
import com.cn.app.test.model.User
import com.cn.core.ui.view.recyclerview.adapter.BaseBinderAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: BaseBinderAdapter
    private lateinit var recyclerView: RecyclerView

    private val testData = mutableListOf(
        User(1, "张三", "avatar1", 1280, false),
        User(2, "李四", "avatar2", 890, true),
        User(3, "王五", "avatar3", 2340, false),
        User(4, "赵六", "avatar4", 5670, true),
        User(5, "钱七", "avatar5", 8900, false),
        User(6, "孙八", "avatar6", 3400, true),
        User(7, "周九", "avatar7", 1560, false),
        User(8, "吴十", "avatar8", 7890, true)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initRecyclerView()
        simulateLocalRefresh()
    }

    private fun initRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = BaseBinderAdapter(testData.toMutableList())
            .addItemBinder(User::class.java, UserBinder(), UserBinder.DiffCallback())

        recyclerView.adapter = adapter
    }

    private fun simulateLocalRefresh() {
        CoroutineScope(Dispatchers.Main).launch {
            delay(2000)
            modifyUserNameWithBundle()
            delay(1500)
            modifyUserLikesWithBundle()
            delay(1500)
            batchUpdateWithDiff()
        }
    }

    private fun modifyUserNameWithBundle() {
        val position = 0
        val user = adapter.data[position] as User
        val updatedUser = user.copy(name = "张三_修改后")
        adapter.data[position] = updatedUser
        
        val bundle = Bundle()
        bundle.putString(UserBinder.KEY_NAME, updatedUser.name)
        adapter.notifyItemChanged(position, bundle)
    }

    private fun modifyUserLikesWithBundle() {
        val position = 2
        val user = adapter.data[position] as User
        val updatedUser = user.copy(likes = user.likes + 100)
        adapter.data[position] = updatedUser
        
        val bundle = Bundle()
        bundle.putInt(UserBinder.KEY_LIKES, updatedUser.likes)
        adapter.notifyItemChanged(position, bundle)
    }

    private fun batchUpdateWithDiff() {
        val newData = testData.toMutableList().apply {
            this[3] = this[3].copy(name = "赵六_更新", likes = 5780)
            this[5] = this[5].copy(isFollowed = false)
            this[7] = this[7].copy(likes = 8000, isFollowed = false)
        }
        adapter.setDiffNewData(newData.toMutableList())
    }
}
