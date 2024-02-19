/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package xyz.nextalone.hook

import android.util.Log
import cc.ioctl.util.HookUtils
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.MyRecentAdapterFilter
import xyz.nextalone.util.get
import xyz.nextalone.util.isPublic
import xyz.nextalone.util.isStatic
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object FixMissingTroopChat : CommonSwitchFunctionHook(arrayOf(MyRecentAdapterFilter)) {

    override val name = "修复消失的群聊聊天记录"

    override val description = "修复部分群聊在消息列表不显示的问题"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY

    private const val TAG = "FixMissingTroopChat"

    override fun initOnce(): Boolean = throwOrTrue {
        val method = DexKit.requireClassFromCache(MyRecentAdapterFilter).declaredMethods.single { m ->
            val argt = m.parameterTypes
            argt.size == 2
                && argt[0].name.endsWith("QQAppInterface")
                && argt[1].name.endsWith("RecentBaseData")
                && m.returnType == java.lang.Boolean.TYPE
                && m.isPublic
                && m.isStatic
        }
        Log.w(TAG, "method: $method")

        HookUtils.hookAfterIfEnabled(this, method) { param ->
            val mUser = param.args[1].get("mUser")
            val uin = mUser.get("uin")
            Log.w(TAG, "uin: $uin, result: ${param.result}")
            param.result = false
        }
    }
}
