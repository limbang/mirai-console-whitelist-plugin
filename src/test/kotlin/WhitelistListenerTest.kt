
import top.limbang.whitelist.WhitelistListener
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.test.Test

/*
 * Copyright 2024 limbang and contributors.
 *
 * 此源代码的使用受 GNU AGPLv3 许可证的约束，该许可证可在"LICENSE"文件中找到。
 * Use of this source code is governed by the GNU AGPLv3 license that can be found in the "LICENSE" file.
 */

internal class WhitelistListenerTest {

    @Test
    fun getMinecraftUUID(){
        val uuid = WhitelistListener.getMinecraftUUID("limbang")
        println("在线玩家UUID：$uuid")

        val uuid2 = UUID.nameUUIDFromBytes(("OfflinePlayer:limbang").toByteArray(StandardCharsets.UTF_8))
            .toString()

        println("离线玩家UUID：$uuid2")
    }
}