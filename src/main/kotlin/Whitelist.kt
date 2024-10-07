/*
 * Copyright 2024 limbang and contributors.
 *
 * 此源代码的使用受 GNU AGPLv3 许可证的约束，该许可证可在"LICENSE"文件中找到。
 * Use of this source code is governed by the GNU AGPLv3 license that can be found in the "LICENSE" file.
 */

package top.limbang.whitelist

import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin

object Whitelist : KotlinPlugin (
    JvmPluginDescription(
        id = "top.limbang.whitelist",
        name = "Whitelist",
        version = "0.0.1",
    ){
        author("Limbang")
        info("MC服务器白名单管理插件")
    }
){
    override fun onEnable() {

    }
}