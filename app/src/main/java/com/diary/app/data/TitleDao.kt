package com.diary.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TitleDao {
    // 查询所有称号定义
    @Query("SELECT * FROM title_definitions ORDER BY category, tier DESC")
    fun getAllDefinitions(): Flow<List<TitleDefinition>>

    // 按分类查询称号定义
    @Query("SELECT * FROM title_definitions WHERE category = :category ORDER BY tier DESC")
    fun getDefinitionsByCategory(category: String): Flow<List<TitleDefinition>>

    // 查询用户已获得的称号（带定义信息）
    @Query("""
        SELECT td.* FROM title_definitions td
        INNER JOIN user_titles ut ON td.key = ut.titleKey
        ORDER BY ut.unlockedAt DESC
    """)
    fun getUnlockedTitles(): Flow<List<TitleDefinition>>

    // 查询某个称号是否已获得
    @Query("SELECT * FROM user_titles WHERE titleKey = :key LIMIT 1")
    suspend fun getUserTitle(key: String): UserTitle?

    // 插入用户称号（解锁时调用）
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUserTitle(userTitle: UserTitle)

    // 批量插入称号定义（初始化时调用）
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefinitions(definitions: List<TitleDefinition>)

    // 查询已获得称号数量
    @Query("SELECT COUNT(*) FROM user_titles")
    fun getUnlockedCount(): Flow<Int>

    // 查询称号定义总数
    @Query("SELECT COUNT(*) FROM title_definitions")
    fun getTotalCount(): Flow<Int>

    // 获取当前展示的称号配置
    @Query("SELECT * FROM title_profile WHERE id = 1")
    fun getTitleProfile(): Flow<TitleProfile?>

    // 设置当前展示的称号配置
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setTitleProfile(profile: TitleProfile)

    // 获取最近解锁的称号（用于通知）
    @Query("""
        SELECT td.*, ut.unlockedAt as unlockTime FROM title_definitions td
        INNER JOIN user_titles ut ON td.key = ut.titleKey
        ORDER BY ut.unlockedAt DESC LIMIT :limit
    """)
    suspend fun getRecentUnlocked(limit: Int = 5): List<TitleWithUnlockTime>

    // 获取某个称号定义
    @Query("SELECT * FROM title_definitions WHERE key = :key LIMIT 1")
    suspend fun getDefinition(key: String): TitleDefinition?

    // 获取所有用户称号（用于查询解锁时间）
    @Query("SELECT * FROM user_titles")
    fun getAllUserTitles(): Flow<List<UserTitle>>

    // 获取所有用户称号（一次性查询，用于组合检测）
    @Query("SELECT * FROM user_titles")
    suspend fun getAllUserTitlesOnce(): List<UserTitle>
}

/**
 * 称号定义 + 解锁时间的组合数据类
 */
data class TitleWithUnlockTime(
    val key: String,
    val name: String,
    val description: String,
    val category: String,
    val iconName: String,
    val tier: Int,
    val isHidden: Boolean,
    val flavorText: String,
    val unlockTime: Long
)
