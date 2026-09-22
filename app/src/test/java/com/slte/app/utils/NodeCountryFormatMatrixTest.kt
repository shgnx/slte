package com.slte.app.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class NodeCountryFormatMatrixTest {

    private val mismatches = mutableListOf<String>()

    private fun expect(
        family: String,
        expected: String,
        vararg names: String,
    ) {
        names.forEach { name ->
            val actual = extractCountryCode(name)
            if (actual != expected) mismatches += "$family → 「$name」 期望 $expected 实际 $actual"
        }
    }

    private fun verifyAll() {
        val report = mismatches.joinToString("\n")
        mismatches.clear()
        assertEquals("识别不符的用例：\n$report", "", report)
    }

    @Test
    fun `协议前缀与各种括号装饰`() {
        expect(
            "协议/括号前缀",
            "HK",
            "香港01",
            "[vless]香港01",
            "[ss]香港01",
            "[vmess]香港01",
            "[trojan]香港01",
            "[Hy]香港01",
            "[Hy2]香港01",
            "[tuic]香港01",
            "[anytls]香港01",
            "[socks]香港01",
            "[SS]香港01",
            "[VLESS]香港01",
            "【香港】01",
            "(香港)01",
            "（香港）01",
            "『香港』01",
            "「香港」01",
            "{香港}01",
            "[香港]01",
        )
        verifyAll()
    }

    @Test
    fun `分隔符数字与地区码位置`() {
        expect(
            "分隔符与数字",
            "HK",
            "香港 01",
            "香港-01",
            "香港_01",
            "香港·01",
            "香港丨01",
            "香港|01",
            "香港／01",
            "香港.01",
            "01-香港",
            "01 香港",
            "①香港01",
            "香港０１",
            "香港01号",
        )
        expect(
            "地区码位置",
            "HK",
            "HK01",
            "HK-01",
            "hk_01",
            "HK 01",
            "HK.01",
            "[HK]01",
            "(HK)01",
            "【HK】01",
            "01-HK",
            "01 HK",
            "HKˣ³",
            "HK×3",
            "HK/IEPL/01",
        )
        verifyAll()
    }

    @Test
    fun `全角零宽字符与 emoji 装饰`() {
        expect(
            "全角/零宽/emoji",
            "HK",
            "\u200B香港01",
            "香港01\u200B",
            "香港\u00A001",
            "ＨＫ　０１",
            "🚀香港01",
            "⚡️香港01",
            "香港01🔥",
            "✨ 香港 01",
            "🇭🇰香港01",
            "[vless]🚀香港丨IEPLˣ³",
        )
        verifyAll()
    }

    @Test
    fun `同一地区的多语言多写法`() {
        expect("香港", "HK", "香港", "港", "HK", "hk", "Hong Kong", "HONG KONG", "hongkong", "HongKong")
        expect("台湾", "TW", "台湾", "台灣", "臺灣", "台北", "臺北", "TW", "tw", "Taipei", "TAIPEI")
        expect("澳门", "MO", "澳门", "澳門", "MO", "mo", "Macau", "Macao", "MACAU")
        expect("日本", "JP", "日本", "东京", "東京", "大阪", "札幌", "名古屋", "JP", "jp", "Japan", "Tokyo", "Osaka")
        expect("韩国", "KR", "韩国", "韓國", "首尔", "首爾", "釜山", "KR", "kr", "Korea", "Seoul", "Busan")
        expect("新加坡", "SG", "新加坡", "狮城", "獅城", "SG", "sg", "Singapore", "SINGAPORE")
        expect("马来西亚", "MY", "马来西亚", "馬來西亞", "吉隆坡", "MY", "my", "Malaysia", "Kuala Lumpur")
        expect("泰国", "TH", "泰国", "泰國", "曼谷", "TH", "th", "Thailand", "Bangkok")
        expect("越南", "VN", "越南", "河内", "胡志明", "VN", "vn", "Vietnam", "Hanoi")
        expect("菲律宾", "PH", "菲律宾", "菲律賓", "马尼拉", "PH", "ph", "Philippines", "Manila")
        expect("印尼", "ID", "印尼", "印度尼西亚", "印度尼西亞", "雅加达", "雅加達", "ID", "id", "Indonesia", "Jakarta")
        expect("印度", "IN", "印度", "孟买", "孟買", "IN", "in", "India", "Mumbai")
        expect("巴基斯坦", "PK", "巴基斯坦", "PK", "pk", "Pakistan")
        expect("缅甸", "MM", "缅甸", "緬甸", "MM", "mm", "Myanmar")
        expect("柬埔寨", "KH", "柬埔寨", "金边", "KH", "kh", "Cambodia")
        expect("蒙古", "MN", "蒙古", "乌兰巴托", "烏蘭巴托", "MN", "mn", "Mongolia")
        expect("阿联酋", "AE", "阿联酋", "阿聯酋", "迪拜", "阿布扎比", "AE", "ae", "Dubai", "UAE")
        expect("以色列", "IL", "以色列", "特拉维夫", "IL", "il", "Israel", "Tel Aviv")
        expect("土耳其", "TR", "土耳其", "伊斯坦布尔", "伊斯坦堡", "TR", "tr", "Turkey", "Istanbul")
        expect("德国", "DE", "德国", "德國", "法兰克福", "法蘭克福", "柏林", "慕尼黑", "DE", "de", "Germany", "Frankfurt", "Berlin", "Munich")
        expect("英国", "GB", "英国", "英國", "伦敦", "倫敦", "曼彻斯特", "爱丁堡", "GB", "gb", "London", "Britain")
        expect("法国", "FR", "法国", "法國", "巴黎", "FR", "fr", "France", "Paris")
        expect("荷兰", "NL", "荷兰", "荷蘭", "阿姆斯特丹", "NL", "nl", "Netherlands", "Amsterdam")
        expect("美国", "US", "美国", "美國", "洛杉矶", "洛杉磯", "圣何塞", "矽谷", "纽约", "紐約", "西雅图", "US", "us", "United States")
        expect("美国城市", "US", "芝加哥", "达拉斯", "迈阿密", "New York", "Los Angeles", "Chicago", "Dallas", "Miami")
        expect("加拿大", "CA", "加拿大", "多伦多", "多倫多", "蒙特利尔", "温哥华", "CA", "ca", "Canada", "Toronto", "Montreal", "Vancouver")
        expect("巴西", "BR", "巴西", "圣保罗", "聖保羅", "BR", "br", "Brazil", "Sao Paulo")
        expect("阿根廷", "AR", "阿根廷", "布宜诺斯艾利斯", "AR", "ar", "Argentina", "Buenos Aires")
        expect("俄罗斯", "RU", "俄罗斯", "俄羅斯", "莫斯科", "RU", "ru", "Russia", "Moscow")
        expect("乌克兰", "UA", "乌克兰", "烏克蘭", "基辅", "UA", "ua", "Ukraine", "Kyiv")
        expect("新西兰", "NZ", "新西兰", "新西蘭", "NZ", "nz", "New Zealand", "Auckland")
        expect("澳大利亚", "AU", "澳大利亚", "澳大利亞", "悉尼", "AU", "au", "Australia", "Sydney", "Melbourne")
        expect("埃及", "EG", "埃及", "开罗", "EG", "eg", "Egypt", "Cairo")
        expect("南非", "ZA", "南非", "约翰内斯堡", "开普敦", "ZA", "za", "South Africa", "Johannesburg")
        expect("墨西哥", "MX", "墨西哥", "墨西哥城", "MX", "mx", "Mexico", "Mexico City")
        expect("智利", "CL", "智利", "圣地亚哥", "CL", "cl", "Chile", "Santiago")
        expect("意大利", "IT", "意大利", "米兰", "米蘭", "罗马", "IT", "it", "Italy", "Milan")
        expect("西班牙", "ES", "西班牙", "马德里", "馬德里", "巴塞罗那", "ES", "es", "Spain", "Madrid", "Barcelona")
        expect("瑞士", "CH", "瑞士", "苏黎世", "蘇黎世", "日内瓦", "CH", "ch", "Switzerland", "Zurich")
        expect("奥地利", "AT", "奥地利", "奧地利", "维也纳", "維也納", "AT", "at", "Austria", "Vienna")
        expect("比利时", "BE", "比利时", "比利時", "布鲁塞尔", "BE", "be", "Belgium", "Brussels")
        expect("葡萄牙", "PT", "葡萄牙", "里斯本", "PT", "pt", "Portugal", "Lisbon")
        expect("罗马尼亚", "RO", "罗马尼亚", "羅馬尼亞", "RO", "ro", "Romania")
        expect("捷克", "CZ", "捷克", "布拉格", "CZ", "cz", "Czech", "Prague")
        expect("希腊", "GR", "希腊", "希臘", "雅典", "GR", "gr", "Greece", "Athens")
        expect("丹麦", "DK", "丹麦", "丹麥", "哥本哈根", "DK", "dk", "Denmark", "Copenhagen")
        expect("冰岛", "IS", "冰岛", "冰島", "雷克雅未克", "IS", "is", "Iceland")
        verifyAll()
    }

    @Test
    fun `旗帜 emoji 各种组合`() {
        expect("旗帜", "HK", "🇭🇰", "🇭🇰01", "🇭🇰香港", "🇭🇰 Hong Kong 01", "[vless]🇭🇰丨IEPLˣ³")
        expect("旗帜", "SG", "🇸🇬", "🇸🇬01", "🇸🇬公告节点", "🇸🇬node.example.com")
        expect("旗帜", "JP", "🇯🇵", "🇯🇵JP01", "🇯🇵 日本-Tokyo-01")
        expect("旗帜", "US", "🇺🇸", "🇺🇸US01", "🇺🇸美国丨Los Angelesˣ²")
        expect("旗帜", "MM", "🇲🇲", "🇲🇲01", "🇲🇲缅甸丨MMˣ³")
        expect("旗帜", "PK", "🇵🇰", "🇵🇰01", "🇵🇰巴基斯坦丨PKˣ³")
        expect("旗帜", "GB", "🇬🇧", "🇬🇧01")
        expect("旗帜", "DE", "🇩🇪", "🇩🇪01", "🇩🇪德国 Frankfurt 01")
        expect("旗帜", "UA", "🇺🇦", "🇺🇦01", "🇺🇦乌克兰丨UAˣ¹")
        verifyAll()
    }

    @Test
    fun `线路与宣传词不得被误判`() {
        expect(
            "线路词",
            "XX",
            "IEPL01",
            "IEPLˣ³",
            "IEPL x3",
            "IPLC",
            "IPLC 2",
            "BGP01",
            "BGPˣ²",
            "DRT01",
            "DRTˣ²",
            "MIX01",
            "MIXˣ²",
            "GIA",
            "Tier1",
            "Premium",
            "Premium01",
            "VIP",
            "Relay01",
            "Super",
            "Streaming",
            "HKT01",
        )
        expect(
            "中文宣传词",
            "XX",
            "隧道01",
            "中转01",
            "专线01",
            "家宽01",
            "官网",
            "官网地址",
            "公告",
            "通知",
            "请收藏",
            "测试",
            "剩余",
            "流量",
            "到期",
            "重置",
        )
        verifyAll()
    }

    @Test
    fun `公告与流量信息不得被误判`() {
        expect(
            "公告类",
            "XX",
            "剩余流量：100 GB",
            "剩余流量：1.5 TB",
            "剩余流量：500 MB",
            "已用流量：12.3 GB",
            "距离下次重置剩余：3 天",
            "套餐到期：2026-10-22",
            "过滤掉 3 条线路",
            "通知：请勿分享",
        )
        verifyAll()
    }

    @Test
    fun `真实风格混合命名`() {
        expect(
            "混合",
            "HK",
            "[vless]🇭🇰香港丨IEPLˣ³",
            "香港 03 · HKT",
            "香港 IEPL x3",
            "[ss]香港丨BGP²",
        )
        expect("混合", "JP", "🇯🇵 日本-Tokyo-01", "日本 JP｜BGP×3", "[Hy2]日本丨Osakaˣ²")
        expect("混合", "TW", "【台湾】TW-01 ⚡", "台灣 Taipei 01", "[vless]🇨🇳台湾丨BGPˣ²")
        expect("混合", "SG", "🇸🇬SG-01｜Premium", "[vless]新加坡丨IEPLˣ³")
        expect("混合", "US", "[Hy2]美国丨Los Angelesˣ²", "🇺🇸美国丨MIXˣ²")
        expect("混合", "KR", "韩国首尔 KR-01", "[vless]🇰🇷韩国丨KRˣ¹")
        expect("混合", "CN", "CN2 GIA", "CN-上海", "上海 IEPL")
        verifyAll()
    }
}
