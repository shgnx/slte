# SLTE 配置说明

SLTE 的配置分三层，按优先级从高到低：

1. **环境变量**（CI secrets / 发布时 `export`）—— 最高优先级
2. **`app/gradle.properties`**（本地文件，已被 .gitignore 忽略，不入库）—— 日常开发测试改这里
3. **远程配置 JSON**（部署在 OSS/Worker 上，运行时下发）—— 运行时可调，无需重新编译

> 完整模板 `app/gradle.properties.example`（已入库），包含全部配置项与注释。
> 复制为 `app/gradle.properties` 填写即可编译，无需修改任何代码：
> `cd app && cp gradle.properties.example gradle.properties`

---

## 一、构建配置（app/gradle.properties）

| 变量 | 默认值 | 作用 |
|---|---|---|
| `SLTE_APP_NAME` | `SLTE` | 应用显示名（桌面名称；图标需自行替换 res/mipmap） |
| `SLTE_VERSION_CODE` | `1` | 版本号（整数） |
| `SLTE_VERSION_NAME` | `1.0.0` | 版本号（显示） |
| `SLTE_API_BASE_URL` | `https://api.example.com` | 后端 API 主地址（https） |
| `SLTE_API_TYPE` | `xiaov2b` | 后端类型：`xiaov2b` / `xboard` |
| `SLTE_SUBSCRIBE_PATH` | `/api/v1/client/subscribe` | 订阅接口路径（与后端契约一致时勿动） |
| `SLTE_REMOTE_CONFIG_URLS` | 空 | 远程配置源 URL（逗号分隔多个，https） |
| `SLTE_ALLOWED_DOMAINS` | 空 | 追加 API 域名白名单（API 与配置源域名自动并入，一般无需填写） |
| `SLTE_CRISP_WEBSITE_ID` | 空 | Crisp 客服 ID（远程配置会覆盖） |
| `SLTE_CRISP_ENABLED` | `false` | 是否启用 Crisp 客服（远程配置会覆盖） |
| `SLTE_NOTIFICATION_TITLE` | 空 | 通知标题（空 = 跟随应用名） |
| `SLTE_NOTIFICATION_TRAFFIC` | `true` | 通知是否显示实时流量/流速 |
| `SLTE_RELEASE_STORE_FILE` | 空 | keystore 路径（发布签名，只编 debug 可留空） |
| `SLTE_RELEASE_STORE_PASSWORD` | 空 | store 密码 |
| `SLTE_RELEASE_KEY_ALIAS` | `slte` | 密钥别名 |
| `SLTE_RELEASE_KEY_PASSWORD` | 空 | 密钥密码 |

> 安全白名单：凭据（JWT / 订阅 token）只发往白名单内域名。
> 白名单 = `SLTE_ALLOWED_DOMAINS` + API 地址域名 + 远程配置源域名（构建期自动并入），
> 运行时校验见 `RemoteConfig.ALLOWED_HOST_SUFFIXES`。

---

## 二、远程配置 JSON（运行时下发，无需重新编译）

部署在 `SLTE_REMOTE_CONFIG_URLS` 指向的地址（OSS / CF Workers / 静态托管）。
多源并发拉取、按 `config_version` 择优，单源失效不影响。

### 完整字段说明

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `api_base_url` | string | 否 | 主 API 地址（https，须在白名单内）；缺失回退构建注入值 |
| `api_base_urls` | string 或 array | 否 | 全部 API 候选（多地址容灾），支持 Base64 编码混用 |
| `api` | string 或 array | 否 | 与 `api_base_urls` 等价的别名（兼容第三方托管格式），支持 Base64 编码混用 |
| `direct_domains` | string 或 array | 否 | 直连域名（走直连不走节点），自动注入订阅 DIRECT 规则 + fake-ip 豁免 |
| `api_type` | string | 否 | 后端类型；只认与构建期内置值一致的取值 |
| `crisp_website_id` | string | 否 | Crisp 客服网站 ID（空 = 关闭客服） |
| `crisp_enabled` | boolean | 否 | 是否启用 Crisp 客服 |
| `config_version` | string | 否 | 配置版本号（多源择优依据），建议填写如 `"1"` |
| `update_version` | string | 否 | 新版本号；空 = 无更新 |
| `update_changelog_title` | string | 否 | 更新弹窗标题（空回退本地文案） |
| `update_changelog` | string | 否 | 更新日志内容（空回退本地文案） |
| `update_force` | boolean | 否 | 是否强制更新 |
| `update_apk_url` | string | 否 | 新版 APK 下载地址（https，须在白名单内） |

### 完整示例（占位符）

```json
{
  "api_base_url": "https://api.example.com",
  "api_base_urls": [
    "https://api.example.com",
    "aHR0cHM6Ly9hcGkyLmV4YW1wbGUuY29t"
  ],
  "direct_domains": [
    "example.com",
    "cdn.example.com"
  ],
  "api_type": "xiaov2b",
  "crisp_website_id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "crisp_enabled": true,
  "config_version": "1",
  "update_version": "",
  "update_changelog_title": "",
  "update_changelog": "",
  "update_force": false,
  "update_apk_url": ""
}
```

### 说明

- **Base64 混用**：`api_base_urls` / `api` 数组元素支持 Base64 编码的 https 地址（防托管平台审查），
  解码后仍须通过 https + 域名白名单校验；明文 URL 照常使用。
- **直连域名建议**：把自持域名全部放进 `direct_domains`（API 域名自动直连，无需重复），
  尤其是远程配置源域名——VPN 开启后访问配置源走直连，不依赖节点可用性。

---

## 三、发布签名

- 只编 debug：`SLTE_RELEASE_STORE_*` 全部留空。
- 编 release：在 `app/gradle.properties` 填 keystore 路径与密码即可。
  未配置时 `assembleRelease` 直接报错（防止误用 debug 签名发布，本地调试请用 assembleDebug）。

---

## 四、安全约定

- `app/gradle.properties`、`.signing-env`、`*.keystore` 均已被 .gitignore 忽略，不会入库。
- 仓库内只保留占位符（`https://api.example.com` / `example.com`），真实域名一律通过
  `app/gradle.properties` + 远程配置下发。
- 远程配置中的 `api_base_url` / `update_apk_url` / `direct_domains` 均经过域名白名单校验，
  非白名单域名会被拒绝。
