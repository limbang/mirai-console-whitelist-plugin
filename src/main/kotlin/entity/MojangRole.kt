/*
 * Copyright 2024 limbang and contributors.
 *
 * 此源代码的使用受 GNU AGPLv3 许可证的约束，该许可证可在"LICENSE"文件中找到。
 * Use of this source code is governed by the GNU AGPLv3 license that can be found in the "LICENSE" file.
 */

package top.limbang.whitelist.entity

import kotlinx.serialization.Serializable

/**
 * ## Mojang 角色实体类
 *
 * @property uuid UUID
 * @property name 角色名称
 * @constructor Create empty Role
 */
@Serializable
data class MojangRole(val id: String, val name: String)
