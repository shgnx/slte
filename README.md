<div align="center">

# SLTE

[![许可证](https://img.shields.io/badge/许可证-GPL--3.0-blue?style=flat-square)](LICENSE)
[![平台](https://img.shields.io/badge/平台-Android-green?style=flat-square)](README.md)
[![内核](https://img.shields.io/badge/内核-mihomo-9cf?style=flat-square)](https://github.com/MetaCubeX/mihomo)

**基于 mihomo 内核的轻量级 Android 代理客户端，支持 XiaoV2b / Xboard 面板**

</div>

---

## 简介

基于 [mihomo](https://github.com/MetaCubeX/mihomo/tree/Alpha) 内核构建的 Android 代理客户端，支持 XiaoV2b / Xboard 面板。

## 开发环境

| 依赖 | 版本 |
|------|------|
| JDK | 17+（推荐 21） |
| Android SDK | compileSdk 36 |
| NDK | 28.2 |
| CMake | 3.22+ |
| Gradle | 8.13（wrapper 内置） |

> 内核已预编译为 `libclash.so` 随仓库提供，普通编译无需 Go 环境；仅修改 Go 补丁链或升级内核版本时才需要。

## 编译

```bash
# 调试包（含单元测试）
./gradlew :app:testDebugUnitTest :app:assembleDebug

# 发布包（必须提供签名环境变量）
SLTE_RELEASE_STORE_FILE=<keystore> \
SLTE_RELEASE_STORE_PASSWORD=<密码> \
SLTE_RELEASE_KEY_ALIAS=<别名> \
SLTE_RELEASE_KEY_PASSWORD=<密码> \
./gradlew :app:assembleRelease
```

## 配置

通过环境变量注入（默认值为占位符，请替换为自部署地址）：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `SLTE_API_BASE_URL` | `https://api.example.com` | 面板 API 地址 |
| `SLTE_API_TYPE` | `xiaov2b` | 后端类型（`xiaov2b` / `xboard`） |
| `SLTE_REMOTE_CONFIG_URLS` | 空（不启用） | 远程配置 URL，逗号分隔多源，如 `https://config.example.com/config.json` |

> **安全白名单**：为防配置投毒导致凭据外泄，API 地址与远程配置中的直连域名只允许在域名白名单内切换。白名单 = `SLTE_ALLOWED_DOMAINS` 追加项 + API 地址域名 + 远程配置源域名，构建期自动并入（详见 [CONFIG.md](CONFIG.md)），**无需修改代码**。仓库内置占位符 `example.com`，部署前请通过环境变量或 `app/gradle.properties` 注入你的域名。
>
> **内核直连兜底**：内核侧补丁链（`kernel-core/src/main/golang/native/config/process.go`）含独立的直连域名占位（与构建注入互不影响），自持域名需在此同步，并在修改后重新交叉编译 `libclash.so`（`GOOS=linux GOARCH=arm64 go build -tags "android cmfa with_gvisor" ./native/config/`，无需 NDK）。

## 相关项目

- [mihomo](https://github.com/MetaCubeX/mihomo) - 内核引擎（本项目核心依赖，vendored 于 `kernel-core/src/foss/golang/clash/`）
- [ClashMetaForAndroid](https://github.com/MetaCubeX/ClashMetaForAndroid) - 内核栈（`kernel-*` 模块）派生自该项目，第三方声明见 [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md)
- [V2Board (xiaov2b)](https://github.com/wyx2685/v2board/tree/master) - 兼容的机场面板后端（`xiaov2b` API，master 分支）
- [Xboard](https://github.com/cedar2025/Xboard/tree/master) - 兼容的机场面板后端（`xboard` API，master 分支）

## 联系与交流

- Telegram 频道：[https://t.me/Ciallo_RT](https://t.me/Ciallo_RT)
- Telegram 作者：[https://t.me/dc_slte](https://t.me/dc_slte)
- 个人邮箱：x@example.com

## 许可证

本项目以 [GPL-3.0](LICENSE) 协议开源，基于 [mihomo](https://github.com/MetaCubeX/mihomo/tree/Alpha) 内核构建，第三方组件声明见 [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md)。

---

<div align="center">

**© 2026 SLTE**

</div>
