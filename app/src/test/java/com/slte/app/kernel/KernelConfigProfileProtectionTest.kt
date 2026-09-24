package com.slte.app.kernel

import android.content.Context
import com.github.kr328.clash.common.Global
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.service.remote.IProfileManager
import com.slte.app.BuildConfig
import com.slte.app.support.MainDispatcherRule
import com.slte.app.support.RobolectricTestApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.util.UUID
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = RobolectricTestApplication::class)
class KernelConfigProfileProtectionTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val context = mockk<Context>(relaxed = true)
    private val manager = mockk<KernelManager>(relaxed = true)
    private val profiles = mockk<IProfileManager>(relaxed = true)
    private val subscribeSource = mockk<SubscribeSource>(relaxed = true)
    private val remoteConfig = mockk<AppRemoteConfig>(relaxed = true)
    private val reporter = KernelFaultReporter(mainRule.dispatcher)

    private val email = "a@b.c"
    private val apiBaseUrl = "https://app.example.com"
    private val subscribeUrl = apiBaseUrl + BuildConfig.SUBSCRIBE_PATH

    private val oldYaml =
        """
        proxies:
          - name: "jp-01"
            type: ss
            server: 1.2.3.4
            port: 8388
        rules:
          - MATCH,节点选择
        """.trimIndent()

    private val duplicatedYaml =
        """
        proxies:
          - name: "jp-01"
            type: ss
            server: 1.1.1.1
            port: 443
          - name: "jp-01"
            type: ss
            server: 2.2.2.2
            port: 443
        rules:
          - MATCH,节点选择
        """.trimIndent()

    private val namelessYaml =
        """
        proxies:
          - type: ss
            server: 1.1.1.1
            port: 443
        rules:
          - MATCH,节点选择
        """.trimIndent()

    @Before
    fun initGlobal() {
        Global.init(RuntimeEnvironment.getApplication())
    }

    private fun config(): KernelConfig {
        every { context.filesDir } returns tmp.root
        every { manager.profile() } returns profiles
        every { remoteConfig.apiBaseUrl } returns apiBaseUrl
        every { remoteConfig.directDomains } returns listOf("example.com")
        every { subscribeSource.getEmail() } returns email
        return KernelConfig(reporter, manager, subscribeSource, remoteConfig, context)
    }

    private fun profile(
        uuid: UUID,
        imported: Boolean = true,
    ) = Profile(
        uuid = uuid,
        name = profileNameFor(email),
        type = Profile.Type.Url,
        source = subscribeUrl,
        active = true,
        interval = 0,
        upload = 0,
        download = 0,
        total = 0,
        expire = 0,
        updatedAt = 0,
        imported = imported,
        pending = false,
    )

    private fun body(text: String) = text.toResponseBody("application/yaml".toMediaType())

    private fun importedFile(uuid: UUID): File = tmp.root.resolve("imported/$uuid/config.yaml").apply { parentFile?.mkdirs() }

    @Test
    fun `重名订阅更新后写入的名字唯一`() = runTest(mainRule.dispatcher) {
        val uuid = UUID.randomUUID()
        val cfg = config()
        coEvery { profiles.queryAll() } returns listOf(profile(uuid))
        coEvery { subscribeSource.fetchSubscribeYaml() } returns body(duplicatedYaml)
        importedFile(uuid).writeText(oldYaml)

        assertEquals(ProfileUpdateResult.UPDATED, cfg.updateProfile())

        val written = importedFile(uuid).readText()
        assertTrue("写入的配置不得再含重名", SubscriptionSanitizer.isKernelLoadable(written))
        assertTrue("重名节点应带后缀区分", written.contains("\"jp-01 #2\""))
    }

    @Test
    fun `无名节点的订阅不覆盖旧配置`() = runTest(mainRule.dispatcher) {
        val uuid = UUID.randomUUID()
        val cfg = config()
        coEvery { profiles.queryAll() } returns listOf(profile(uuid))
        coEvery { subscribeSource.fetchSubscribeYaml() } returns body(namelessYaml)
        importedFile(uuid).writeText(oldYaml)

        assertEquals(ProfileUpdateResult.FAILED, cfg.updateProfile())
        assertEquals("致命问题不得覆盖已有配置", oldYaml, importedFile(uuid).readText())
    }

    @Test
    fun `现有配置含重名时连接前自动重写`() = runTest(mainRule.dispatcher) {
        val uuid = UUID.randomUUID()
        val cfg = config()
        coEvery { profiles.queryAll() } returns listOf(profile(uuid))
        coEvery { profiles.queryByUUID(uuid) } returns profile(uuid)
        coEvery { subscribeSource.fetchSubscribeYaml() } returns body(duplicatedYaml)
        importedFile(uuid).writeText(duplicatedYaml)

        assertEquals(uuid, cfg.ensureProfile())

        coVerify { profiles.commit(uuid) }
        val pending = tmp.root.resolve("pending/$uuid/config.yaml")
        assertTrue("应重新写入可加载的配置", pending.exists())
        assertTrue(SubscriptionSanitizer.isKernelLoadable(pending.readText()))
    }
}
