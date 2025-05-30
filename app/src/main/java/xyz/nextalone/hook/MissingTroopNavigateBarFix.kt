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
import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.MyIsShowTroopNavigateBar
import xyz.nextalone.util.get
import xyz.nextalone.util.isPublic
import xyz.nextalone.util.isStatic
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object FixMissingTroopNavigateBar : CommonSwitchFunctionHook(arrayOf(MyIsShowTroopNavigateBar)) {

    override val name = "修复群聊不显示导航按钮的问题"

    override val description = "修复群聊不显示导航按钮的问题"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY

    private const val TAG = "FixMissingTroopNavigateBar"

    override fun initOnce(): Boolean {
        val m = DexKit.requireMethodFromCache(MyIsShowTroopNavigateBar)
        hookBeforeIfEnabled(m) { param ->
            val thisObj = param.thisObject ?: return@hookBeforeIfEnabled
            val clazz = thisObj.javaClass
            try {
                val fields = clazz.declaredFields

                // 找到 f24407b (inProgress) 和 unread_count 两个字段
                val inProgressField = clazz.declaredFields.firstOrNull { it.type == Boolean::class.java }
                val unreadCountField = clazz.declaredFields.firstOrNull { it.type == Int::class.java }
                if (inProgressField == null || unreadCountField == null) {
                    Log.w(TAG, "Required fields not found")
                    return@hookBeforeIfEnabled
                }

                inProgressField.isAccessible = true
                val inProgress = inProgressField.getBoolean(thisObj)
                if (inProgress) {
                    Log.d(TAG, "Skip patch: in-progress state is true")
                    param.setResult(false)
                    return@hookBeforeIfEnabled
                }

                unreadCountField.isAccessible = true
                val originalCount = unreadCountField.getInt(thisObj)
                if (originalCount >= 1) {
                    // unreadCountField.setInt(thisObj, 199)
                    param.setResult(true)
                    Log.d(TAG, "Patched unread_count from $originalCount to 199")
                }

            } catch (e: Throwable) {
                Log.e(TAG, "Error in FixMissingTroopNavigateBar hook", e)
            }
        }

        return true
    }
}
