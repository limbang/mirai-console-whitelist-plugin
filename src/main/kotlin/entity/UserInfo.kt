/*
 * Copyright 2024 limbang and contributors.
 *
 * 此源代码的使用受 GNU AGPLv3 许可证的约束，该许可证可在"LICENSE"文件中找到。
 * Use of this source code is governed by the GNU AGPLv3 license that can be found in the "LICENSE" file.
 */

package top.limbang.whitelist.entity

import kotlinx.serialization.Serializable

/**
 * ## User info
 *
 * @property qq QQ 号码
 * @property username Minecraft 用户名
 * @property isOfficial 是否是正版
 * @constructor Create empty User info
 */
@Serializable
data class UserInfo(
    val qq: Long,
    val username: String,
    val isOfficial: Boolean
)

