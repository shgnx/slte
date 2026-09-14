package com.slte.app.kernel

/** 测速结果缓存：kernel 层经接口取数，避免直接依赖 data 层实现 */
interface SpeedResultStore {
    fun saveSpeedResults(results: Map<String, Int>)

    fun getSpeedResults(): Map<String, Int>?
}
