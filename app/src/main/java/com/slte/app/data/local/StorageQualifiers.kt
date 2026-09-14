package com.slte.app.data.local

import javax.inject.Qualifier

/**
 * 加密偏好存储的限定符。
 *
 * 会话/缓存与登录凭证使用不同的加密文件与 Keystore 密钥，二者类型均为
 * [android.content.SharedPreferences]，靠限定符区分。Store 只依赖 SharedPreferences
 * 抽象，加密实现（含三级降级）留在 DI 边界，单元测试可直接注入内存实现。
 */

/** 会话与订阅/节点/测速缓存（slte_session） */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionPrefs

/** 记住登录账号的凭证（slte_credential_store） */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CredentialPrefs
