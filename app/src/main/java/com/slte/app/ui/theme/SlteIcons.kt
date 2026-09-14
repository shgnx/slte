package com.slte.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.HeadsetMic
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 全项目唯一图标库：页面只允许引用 `SlteIcons.*`，禁止直接写 `Icons.*`（CI 有守卫）。
 * 换图标集只需改本文件，页面零改动。
 *
 * 选型规范：
 * - 实心（Rounded）：顶栏圆形按钮、首页/邀请页入口按钮、信息行徽章——有底衬场景更醒目；
 * - 镂空（Outlined）：列表行、表单、弹窗、订单状态、公告——信息密集处更轻；
 * - RTL 场景统一 `Icons.AutoMirrored.*`。
 * - 颜色三档：蓝 = 可操作（输入框/弹窗内图标恒为蓝色）、灰 = 导航/次要、红 = 危险（退出登录）。
 */
object SlteIcons {

    /** 顶栏：客服 */
    val Support: ImageVector = Icons.Rounded.HeadsetMic

    /** 顶栏：公告 */
    val Notifications: ImageVector = Icons.Rounded.Notifications

    /** 顶栏：个人中心 */
    val Profile: ImageVector = Icons.Rounded.Person

    /** 节点页顶栏：测速 */
    val SpeedTest: ImageVector = Icons.Rounded.Speed

    /** 节点页顶栏：更新订阅 */
    val SyncSubscription: ImageVector = Icons.Rounded.Sync

    /** 首页信息行：有效期 */
    val Expiry: ImageVector = Icons.Rounded.Schedule

    /** 首页信息行：服务器 */
    val Server: ImageVector = Icons.Rounded.Storage

    /** 首页信息行：代理模式 */
    val ProxyMode: ImageVector = Icons.Rounded.Layers

    /** 首页信息行：当前 IP */
    val CurrentIp: ImageVector = Icons.Rounded.CheckCircle

    /** 首页入口按钮：更新订阅 */
    val UpdateSubscription: ImageVector = Icons.Rounded.Refresh

    /** 首页入口按钮：邀请返利 */
    val Invite: ImageVector = Icons.Rounded.GroupAdd

    /** 邀请页入口按钮：佣金划转 */
    val Transfer: ImageVector = Icons.Rounded.SwapHoriz

    /** 邀请页入口按钮：申请提现 */
    val Wallet: ImageVector = Icons.Rounded.AccountBalanceWallet

    /** 返回（AutoMirrored，RTL 自动镜像） */
    val Back: ImageVector = Icons.AutoMirrored.Outlined.ArrowBack

    /** 行尾箭头 / 结构引导 */
    val ChevronRight: ImageVector = Icons.Outlined.ChevronRight

    /** 展开 / 收起 */
    val ExpandMore: ImageVector = Icons.Outlined.ExpandMore
    val ExpandLess: ImageVector = Icons.Outlined.ExpandLess

    /** 选中勾（下拉列表内） */
    val Check: ImageVector = Icons.Outlined.Check

    /** 新增 / 复制（邀请码） */
    val Add: ImageVector = Icons.Outlined.Add
    val Copy: ImageVector = Icons.Outlined.ContentCopy

    /** 退出登录（危险；AutoMirrored） */
    val Logout: ImageVector = Icons.AutoMirrored.Outlined.Logout

    /** 个人中心行：邮箱 / 余额 */
    val Email: ImageVector = Icons.Outlined.Email
    val Balance: ImageVector = Icons.Outlined.AccountBalanceWallet

    /** 个人中心行：我的订单 / 邀请返利 / 联系我们 / 其他设置 / 关于软件 */
    val Orders: ImageVector = Icons.Outlined.Receipt
    val InviteRow: ImageVector = Icons.Outlined.GroupAdd
    val CustomerService: ImageVector = Icons.Outlined.SupportAgent
    val Settings: ImageVector = Icons.Outlined.Settings
    val About: ImageVector = Icons.Outlined.Info

    /** 设置页行：TUN 堆栈 / 语言 / 修改密码 / 深色 / 浅色 / 邮件提醒 */
    val TunStack: ImageVector = Icons.Outlined.Lan
    val Language: ImageVector = Icons.Outlined.Public
    val ChangePassword: ImageVector = Icons.Outlined.Key
    val DarkMode: ImageVector = Icons.Outlined.DarkMode
    val LightMode: ImageVector = Icons.Outlined.LightMode
    val Remind: ImageVector = Icons.Outlined.SwitchAccount

    /** 关于页行：导出日志 */
    val ExportLog: ImageVector = Icons.Outlined.Description

    /** 表单 / 弹窗：提现方式、提现账号、金额、优惠券 */
    val WithdrawMethod: ImageVector = Icons.Outlined.AccountBalance
    val AtSign: ImageVector = Icons.Outlined.AlternateEmail
    val Amount: ImageVector = Icons.Outlined.AttachMoney
    val Coupon: ImageVector = Icons.Outlined.ConfirmationNumber

    /** 订单状态徽章 */
    val OrderPending: ImageVector = Icons.Outlined.Schedule
    val OrderCompleted: ImageVector = Icons.Outlined.CheckCircle
    val OrderCancelled: ImageVector = Icons.Outlined.Cancel
    val OrderAbnormal: ImageVector = Icons.Outlined.ErrorOutline

    /** 认证页 / 表单输入 */
    val Account: ImageVector = Icons.Outlined.Email
    val Password: ImageVector = Icons.Outlined.Lock
    val VerificationCode: ImageVector = Icons.Outlined.Sms
    val InviteCode: ImageVector = Icons.Outlined.CardGiftcard
    val VisibilityOn: ImageVector = Icons.Outlined.Visibility
    val VisibilityOff: ImageVector = Icons.Outlined.VisibilityOff
}
