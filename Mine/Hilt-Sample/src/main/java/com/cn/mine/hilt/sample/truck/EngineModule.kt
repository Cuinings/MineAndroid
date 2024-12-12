package com.cn.mine.hilt.sample.truck

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

/**
 * @Author: CuiNing
 * @Time: 2024/12/12 11:03
 * @Description:
 */
@Module
@InstallIn(ActivityComponent::class)
abstract class EngineModule {

    @UseGasEngine
    @Binds
    abstract fun bindGasEngine(gasEngine: GasEngine): Engine

    @UseElectricEngine
    @Binds
    abstract fun bindElectricEngine(electricEngine: ElectricEngine): Engine
}