/*
 * Copyright 2024 limbang and contributors.
 *
 * 此源代码的使用受 GNU AGPLv3 许可证的约束，该许可证可在"LICENSE"文件中找到。
 * Use of this source code is governed by the GNU AGPLv3 license that can be found in the "LICENSE" file.
 */

package top.limbang.whitelist


import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.mamoe.mirai.console.command.CommandSender.Companion.toCommandSender
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.contact.getMember
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import top.limbang.mcsm.mirai.command.MCSMCompositeCommand.apiMap
import top.limbang.mcsm.mirai.config.GroupInstance
import top.limbang.mcsm.mirai.config.MCSMData
import top.limbang.mcsm.model.GetFilesRequest
import top.limbang.mcsm.model.UpdateFilesRequest
import top.limbang.whitelist.entity.MojangRole
import top.limbang.whitelist.entity.Role
import top.limbang.whitelist.entity.UserInfo
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID

object WhitelistListener : SimpleListenerHost() {

    /**
     * ### 处理群消息检查 QQ 绑定
     *
     */
    @EventHandler
    suspend fun GroupMessageEvent.queryRoleBindingQQ() {
        if (toCommandSender().hasPermission(Whitelist.parentPermission).not()) return

        val content = message.contentToString()
        val match = """^查询角色\s(.*)""".toRegex().find(content) ?: return

        val (username) = match.destructured
        val userInfo = WhitelistData.bindingList.find { it.username == username } ?: run {
            group.sendMessage("角色[$username]未绑定。")
            return
        }

        group.sendMessage(
            PlainText("角色 $username [${if (userInfo.isOfficial) "正版" else "外置"}] 的 QQ 绑定为：${userInfo.qq},已为您@了。")
                .plus(At(userInfo.qq))
        )
    }

    /**
     * ### 处理群消息绑定角色
     *
     */
    @EventHandler
    suspend fun GroupMessageEvent.bindRole() {
        val content = message.contentToString()
        val match = """^绑定角色\s(.*)\s(正版|外置)""".toRegex().find(content) ?: return

        val (username, type) = match.destructured

        // 判断账号是否已绑定角色
        WhitelistData.bindingList.find { it.qq == sender.id }?.let {
            group.sendMessage(At(sender.id) + "账号已经绑定[${it.username}]，请勿重复绑定。")
            return
        }
        // 判断是否绑定已经绑定的角色
        WhitelistData.bindingList.find { it.username == username }?.let {
            val memberName = group.getMember(it.qq)?.nameCardOrNick
            group.sendMessage(At(sender.id) + "角色已被[$memberName - ${it.qq}]绑定，请勿重复绑定。")
            return
        }

        WhitelistData.bindingList.add(UserInfo(sender.id, username, type == "正版"))
        group.sendMessage(At(sender.id) + "绑定[$username]成功！")
    }

    /**
     * ### 处理群消息解绑角色
     *
     */
    @EventHandler
    suspend fun GroupMessageEvent.unbindRole() {
        if (toCommandSender().hasPermission(Whitelist.parentPermission).not()) return

        val content = message.contentToString()
        val match = """^解绑角色\s(.*)""".toRegex().find(content) ?: return

        val (username) = match.destructured
        WhitelistData.bindingList.removeIf { it.username == username }.let {
            if (it) {
                group.sendMessage(At(sender.id) + "解绑[$username]成功！")
            } else {
                group.sendMessage(At(sender.id) + "角色[$username]未绑定。")
            }
        }
    }

    /**
     * ## 处理群消息申请加入白名单消息
     */
    @EventHandler
    suspend fun GroupMessageEvent.applyJoinWhitelist() {
        val content = message.contentToString()
        val match = """^申请加入白名单\s?(.*)""".toRegex().find(content) ?: return

        // 判断是否绑定角色
        val userInfo = WhitelistData.bindingList.find { it.qq == sender.id } ?: run {
            group.sendMessage(At(sender.id) + "您还没有绑定,请绑定后再发送指令。\n\n发送指令: 绑定角色 角色名称 正版/外置")
            return
        }

        val (serverName) = match.destructured
        // 判断 MCSM 插件里面有没有对应的服务器
        val instance = MCSMData.groupInstances[group.id]?.find { it.name == serverName } ?: run {
            group.sendMessage(At(sender.id) + "没有找到对应的服务器。")
            return
        }

        // 处理加入白名单申请
        // 生成 UUID
        val uuid = if (!userInfo.isOfficial) {
            UUID.nameUUIDFromBytes(("OfflinePlayer:${userInfo.username}").toByteArray(StandardCharsets.UTF_8))
                .toString()
        } else {
            getMinecraftUUID(userInfo.username) ?: run {
                group.sendMessage(At(sender.id) + "查询角色 UUID 失败，请输入正确的角色名。")
                return
            }
        }

        // 读取服务器白名单
        val roles = readServerWhitelist(instance)

        // 判断是否已经在白名单中
        if (roles.any { it.name == userInfo.username && it.uuid == uuid }) {
            group.sendMessage(At(sender.id) + "您已经在白名单中。")
            return
        }

        // 写入服务器白名单并刷新白名单
        if (roles.add(Role(uuid, userInfo.username)) &&
            writeServerWhitelist(instance, roles) && reloadWhitelist(instance)
        ) {
            group.sendMessage(At(sender.id) + "申请加入白名单成功！")
        } else {
            group.sendMessage(At(sender.id) + "加入白名单失败，请联系管理员。")
        }
    }

    /**
     * ### 处理群消息删除白名单消息
     */
    @EventHandler
    suspend fun GroupMessageEvent.deleteWhitelist() {
        if (toCommandSender().hasPermission(Whitelist.parentPermission).not()) return

        val content = message.contentToString()
        val match = """^删除白名单\s(.*)\s(.*)""".toRegex().find(content) ?: return

        val (serverName, username) = match.destructured
        // 判断 MCSM 插件里面有没有对应的服务器
        val instance = MCSMData.groupInstances[group.id]?.find { it.name == serverName } ?: run {
            group.sendMessage(At(sender.id) + "没有找到对应的服务器。")
            return
        }
        // 读取服务器白名单
        val roles = readServerWhitelist(instance)
        // 过滤出指定角色
        val filteredRoles = roles.filter { it.name == username }
        // 判断是否已经在白名单中
        if (filteredRoles.isEmpty()) {
            group.sendMessage(At(sender.id) + "角色[$username]未在白名单中。")
            return
        }
        // 删除指定角色
        roles.removeAll(filteredRoles)
        // 写入服务器白名单并刷新白名单
        if (writeServerWhitelist(instance, roles) && reloadWhitelist(instance)) {
            group.sendMessage(At(sender.id) + "删除[$username]成功！")
        } else {
            group.sendMessage(At(sender.id) + "删除[$username]失败，请联系管理员。")
        }
    }

    /**
     * ## 查询服务器白名单
     */
    @EventHandler
    suspend fun GroupMessageEvent.queryServerWhitelist() {
        if (toCommandSender().hasPermission(Whitelist.parentPermission).not()) return

        val content = message.contentToString()
        val match = """^查询白名单\s?(.*)""".toRegex().find(content) ?: return
        val (serverName) = match.destructured
        val instance = MCSMData.groupInstances[group.id]?.find { it.name == serverName } ?: return

        val roles = readServerWhitelist(instance)
        group.sendMessage("[$serverName]白名单如下：\n\n" + roles.joinToString("\n") {
            "${it.name} - ${it.uuid.substring(0, 8)}"
        })
    }


    /**
     * ## 读取服务器白名单
     *
     * @param instance 服务器实例
     * @return
     */
    private suspend fun readServerWhitelist(instance: GroupInstance): MutableList<Role> {
        val whitelist = apiMap[instance.apiKey]!!.getFile(
            instance.uuid,
            instance.daemonUUID,
            instance.apiKey,
            GetFilesRequest("whitelist.json")
        ).data ?: return mutableListOf()

        return Json.decodeFromString(whitelist)
    }

    /**
     * ## 写入服务器白名单
     *
     * @param instance 服务器实例
     * @param roles 白名单角色列表
     * @return
     */
    private suspend fun writeServerWhitelist(instance: GroupInstance, roles: MutableList<Role>): Boolean {
        val result = apiMap[instance.apiKey]!!.updateFile(
            instance.uuid,
            instance.daemonUUID,
            instance.apiKey,
            UpdateFilesRequest("whitelist.json", Json.encodeToString(roles))
        ).data ?: return false
        return result
    }

    /**
     * ## 刷新服务器白名单
     *
     * @param instance 服务器实例
     * @return
     */
    private suspend fun reloadWhitelist(instance: GroupInstance): Boolean {
        return apiMap[instance.apiKey]!!.sendCommandInstance(
            instance.uuid,
            instance.daemonUUID,
            instance.apiKey,
            "whitelist reload"
        ).status == 200
    }


    /**
     * ## 获取 Minecraft UUID
     *
     * @param username 用户名
     * @return UUID 字符串
     */
    fun getMinecraftUUID(username: String): String? {
        val url = "https://api.mojang.com/users/profiles/minecraft/$username"
        return try {
            (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connect()
            }.let { connection ->
                if (connection.responseCode == 200) {
                    // 读取完整响应内容
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        val response = reader.readText() // 使用 readText() 读取完整内容
                        Json.decodeFromString<MojangRole>(response).id
                    }.let { formatUUID(it) }
                } else {
                    null // 如果响应码不是200，返回null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * ## 格式化 UUID 为带连字符的形式
     *
     * @param uuid 原始 UUID
     * @return 带连字符的 UUID
     *
     */
    fun formatUUID(uuid: String): String {
        return buildString {
            append(uuid.substring(0, 8)).append('-')
            append(uuid.substring(8, 12)).append('-')
            append(uuid.substring(12, 16)).append('-')
            append(uuid.substring(16, 20)).append('-')
            append(uuid.substring(20))
        }
    }
}