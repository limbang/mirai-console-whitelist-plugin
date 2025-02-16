/*
 * Copyright 2024 limbang and contributors.
 *
 * 此源代码的使用受 GNU AGPLv3 许可证的约束，该许可证可在"LICENSE"文件中找到。
 * Use of this source code is governed by the GNU AGPLv3 license that can be found in the "LICENSE" file.
 */

package top.limbang.whitelist

import kotlinx.coroutines.cancel
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.registerTo

object Whitelist : KotlinPlugin (
    JvmPluginDescription(
        id = "top.limbang.whitelist",
        name = "Whitelist",
        version = "0.0.2",
    ){
        author("Limbang")
        info("MC服务器白名单管理插件")
        dependsOn("top.limbang.mcsm")
    }
){
    override fun onEnable() {
        WhitelistData.reload()
        // 创建事件通道
        val eventChannel = GlobalEventChannel.parentScope(this)

        WhitelistListener.registerTo(eventChannel)
    }

    override fun onDisable() {
        WhitelistListener.cancel()
    }
}