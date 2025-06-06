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
package cc.ioctl.hook.sideswipe

import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.forEach
import androidx.core.view.forEachIndexed
import androidx.core.view.get
import androidx.core.view.size
import cc.ioctl.util.HookUtils
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.getStaticObjectOrNull
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.paramCount
import com.github.kyuubiran.ezxhelper.utils.setViewZeroSize
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.tlb.ConfigTable
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Log
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.QQVersion.QQ_8_4_1
import io.github.qauxv.util.QQVersion.QQ_8_6_0
import io.github.qauxv.util.QQVersion.QQ_8_6_5
import io.github.qauxv.util.QQVersion.QQ_8_8_11
import io.github.qauxv.util.QQVersion.QQ_8_9_23
import io.github.qauxv.util.hostInfo
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.base.MultiItemDelayableHook
import xyz.nextalone.util.clazz
import xyz.nextalone.util.get
import xyz.nextalone.util.hide
import xyz.nextalone.util.throwOrTrue
import java.util.SortedMap

//侧滑栏精简
@FunctionHookEntry
@UiItemAgentEntry
object SimplifyQQSettingMe : MultiItemDelayableHook("SimplifyQQSettingMe") {

    const val MidContentName = "SimplifyQQSettingMe::MidContentName"

    override val preferenceTitle: String = "侧滑栏精简"
    override val description: String = "可能需要重启 QQ 后生效"
    override val allItems = setOf<String>()
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.SLIDING_UI
    override val isAvailable = requireMinQQVersion(QQ_8_4_1)
    override val enableCustom = false

    //Form 8.4.1
    //Body = [0,1,0,0,0,1,4] || [0,1,0,0,0,1,4,0]
    override var items: MutableList<String> = mutableListOf(
        "夜间模式",            //夜间模式 [0,1,0,0,0,1,6,1]
        "登录达人",          //登录达人 [0,1,0,0,0,1,6,2]
        "当前温度",         //当前温度 [0,1,0,0,0,1,6,3]
        "开播啦鹅",         //开播啦鹅 [0,1,0,0,0,1,4,0,1] || [0,1,0,0,0,1,4,0,1,1,1] || d_qq_shopping 大概?
        "我的小世界",        //我的小世界 [0,1,0,0,0,1,4,0,2] || [0,1,0,0,0,1,4,0,1,2,1] || d_smallworld
        "开通会员",         //开通会员 [0,1,0,0,0,1,4,0,3] || [0,1,0,0,0,1,4,0,1,3,1] || d_vip_identity
        "我的钱包",         //我的钱包 [0,1,0,0,0,1,4,0,4] || [0,1,0,0,0,1,4,0,1,4,1] || d_qqwallet
        "个性装扮",         //个性装扮 [0,1,0,0,0,1,4,0,5] || [0,1,0,0,0,1,4,0,1,5,1] || d_decoration
        "情侣空间",         //情侣空间 [0,1,0,0,0,1,4,0,6] || [0,1,0,0,0,1,4,0,1,6,1] || d_lovespace
        "我的收藏",         //我的收藏 [0,1,0,0,0,1,4,0,7] || [0,1,0,0,0,1,4,0,1,7,1] || d_favorite
        "我的相册",         //我的相册 [0,1,0,0,0,1,4,0,8] || [0,1,0,0,0,1,4,0,1,8,1] || d_album
        "我的文件",         //我的文件 [0,1,0,0,0,1,4,0,9] || [0,1,0,0,0,1,4,0,1,9,1] || d_document
        "我的日程",         //我的日程 [0,1,0,0,0,1,4,0,10] || [0,1,0,0,0,1,4,0,1,10,1] || 未知，可能已被移除
        "我的视频",         //我的视频 [0,1,0,0,0,1,4,0,11] || [0,1,0,0,0,1,4,0,1,11,1] || d_video
        "小游戏",          //小游戏 [0,1,0,0,0,1,4,0,12] || [0,1,0,0,0,1,4,0,1,12,1] || d_minigame
        "腾讯文档",         //腾讯文档 [0,1,0,0,0,1,4,0,13] || [0,1,0,0,0,1,4,0,1,13,1] || d_tencent_document
        "每日打卡",         //每日打卡 [0,1,0,0,0,1,4,0,14] || [0,1,0,0,0,1,4,0,1,14,1]
        "王卡免流量特权",   //开通王卡 [0,1,0,0,0,1,4,0,15] || [0,1,0,0,0,1,4,0,1,15,1] || d_vip_card
        "厘米秀", // d_cmshow
        "超级QQ秀", // d_zplan
        "下拉形象展示",
        "等级"
    )

    var items2Hide: MutableMap<String, String> = mutableMapOf(
        Pair("开播啦鹅", "d_qq_shopping"),       //开播啦鹅 [0,1,0,0,0,1,4,0,1] || [0,1,0,0,0,1,4,0,1,1,1] || d_qq_shopping 大概?
        Pair("我的小世界", "d_smallworld"),        //我的小世界 [0,1,0,0,0,1,4,0,2] || [0,1,0,0,0,1,4,0,1,2,1] || d_smallworld
        Pair("开通会员", "d_vip_identity"),       //开通会员 [0,1,0,0,0,1,4,0,3] || [0,1,0,0,0,1,4,0,1,3,1] || d_vip_identity
        Pair("我的钱包", "d_qqwallet"),        //我的钱包 [0,1,0,0,0,1,4,0,4] || [0,1,0,0,0,1,4,0,1,4,1] || d_qqwallet
        Pair("个性装扮", "d_decoration"),        //个性装扮 [0,1,0,0,0,1,4,0,5] || [0,1,0,0,0,1,4,0,1,5,1] || d_decoration
        Pair("情侣空间", "d_lovespace"),    //情侣空间 [0,1,0,0,0,1,4,0,6] || [0,1,0,0,0,1,4,0,1,6,1] || d_lovespace
        Pair("我的收藏", "d_favorite"),     //我的收藏 [0,1,0,0,0,1,4,0,7] || [0,1,0,0,0,1,4,0,1,7,1] || d_favorite
        Pair("我的相册", "d_album"),     //我的相册 [0,1,0,0,0,1,4,0,8] || [0,1,0,0,0,1,4,0,1,8,1] || d_album
        Pair("我的文件", "d_document"),     //我的文件 [0,1,0,0,0,1,4,0,9] || [0,1,0,0,0,1,4,0,1,9,1] || d_document
        Pair("我的视频", "d_video"),        //我的视频 [0,1,0,0,0,1,4,0,11] || [0,1,0,0,0,1,4,0,1,11,1] || d_video
        Pair("小游戏", "d_minigame"),    //小游戏 [0,1,0,0,0,1,4,0,12] || [0,1,0,0,0,1,4,0,1,12,1] || d_minigame
        Pair("腾讯文档", "d_tencent_document"),     //腾讯文档 [0,1,0,0,0,1,4,0,13] || [0,1,0,0,0,1,4,0,1,13,1] || d_tencent_document
        Pair("王卡免流量特权", "d_vip_card"),   //开通王卡 [0,1,0,0,0,1,4,0,15] || [0,1,0,0,0,1,4,0,1,15,1] || d_vip_card
        Pair("厘米秀", "d_cmshow"),  // d_cmshow
        Pair("超级QQ秀", "d_zplan")// d_zplan
    )

    private val keyWords: SortedMap<String, String> = sortedMapOf(
        "级" to "等级",
        "间" to "夜间模式",
        "等级" to "登录达人",
        "达" to "登录达人",
        "天" to "登录达人",
        "播" to "开播啦鹅",
        "世界" to "我的小世界",
        "会员" to "开通会员",
        "vip" to "开通会员",
        "VIP" to "开通会员",
        "钱包" to "我的钱包",
        "装扮" to "个性装扮",
        "情侣" to "情侣空间",
        "相册" to "我的相册",
        "收藏" to "我的收藏",
        "文件" to "我的文件",
        "日程" to "我的日程",
        "视频" to "我的视频",
        "游戏" to "小游戏",
        "文档" to "腾讯文档",
        "打卡" to "每日打卡",
        "王卡" to "王卡免流量特权",
        "流量" to "王卡免流量特权",
        "送12个月" to "王卡免流量特权",
        "厘米" to "厘米秀",
    )

    @Throws(Exception::class)
    override fun initOnce() = throwOrTrue {

        val kQQSettingMeView = if (requireMinQQVersion(QQVersion.QQ_8_9_25))
            Initiator.loadClass("com.tencent.mobileqq.activity.QQSettingMeView")
        else Initiator.loadClass("com.tencent.mobileqq.activity.QQSettingMe")
        XposedBridge.hookAllConstructors(kQQSettingMeView, HookUtils.afterIfEnabled(this) { param ->
            //中间部分(QQ会员 我的钱包等)
            val midContentName = ConfigTable.getConfig<String>(MidContentName)
            val midContentListLayout = if (requireMinQQVersion(QQ_8_6_5)) {
                param.thisObject.get(midContentName, LinearLayout::class.java)
            } else {
                param.thisObject.get(midContentName, View::class.java) as LinearLayout
            }
            //底端部分 设置 夜间模式 达人 等
            val vg = param.args[param.args.size - 1]
            val id = "${hostInfo.packageName}.R\$id".clazz?.getStaticObjectOrNull("drawer_bottom_container")
            val underSettingsLayout = if (id is Int && vg is ViewGroup) {
                vg.findViewById(id)
            } else if (requireMinQQVersion(QQ_8_6_5)) {
                val parent = midContentListLayout?.parent?.parent as ViewGroup
                var ret: LinearLayout? = null
                parent.forEach {
                    if (it is LinearLayout && it[0] is LinearLayout) {
                        ret = it
                    }
                }
                ret
            } else {
                val underSettingsName = if (requireMinQQVersion(QQ_8_6_0)) "l" else "h"
                param.thisObject.get(underSettingsName, View::class.java) as? LinearLayout
            }
            val correctUSLayout: LinearLayout? =
                if (requireMinQQVersion(QQ_8_9_23)) (underSettingsLayout as LinearLayout)[0] as LinearLayout else underSettingsLayout
            correctUSLayout?.forEachIndexed { i, v ->
                val tv = (v as LinearLayout)[1] as TextView
                val text = tv.text
                if (stringHit(text.toString()) || i == 3 && activeItems.contains("当前温度")) {
                    v.setViewZeroSize()
                }
            }
            val midRemovedList: MutableList<Int> = mutableListOf()
            midContentListLayout?.forEach {
                val child = it as LinearLayout
                val tv = if (child.size == 1) {
                    (child[0] as LinearLayout)[1]
                } else {
                    child[1]
                } as TextView
                val text = tv.text.toString()
                if (stringHit(text)) {
                    midRemovedList.add(midContentListLayout.indexOfChild(child))
                }
            }
            midRemovedList.sorted().forEachIndexed { index, i ->
                if (requireMinQQVersion(QQ_8_8_11)) {
                    midContentListLayout?.removeViewAt(i - index)
                } else {
                    midContentListLayout?.getChildAt(i)?.hide()
                }
            }
        })
        XposedBridge.hookAllMethods(ViewTreeObserver::class.java, "dispatchOnGlobalLayout", object : XC_MethodReplacement() {
            override fun replaceHookedMethod(param: MethodHookParam) {
                try {
                    XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                } catch (e: Exception) {
                    if (e.stackTraceToString().contains("QQSettingMe")) {
                        Log.d("SimplifyQQSettingMe: have prevented crash")
                    } else {
                        throw e
                    }
                }
            }
        })

        // for NT QQ 8.9.68.11450
        val clazz = Initiator.load("com.tencent.mobileqq.activity.QQSettingMeViewV9")
        clazz?.findAllMethods { paramCount == 1 && parameterTypes[0].name.contains("com.tencent.mobileqq.activity.qqsettingme") }?.hookAfter {
            val cz = clazz.superclass.superclass
            val m = cz.findMethod { returnType == View::class.java && paramCount == 1 && parameterTypes[0] == String::class.java }
            for (activeItem in activeItems) {
                if (items2Hide.contains(activeItem)) {
                    val viewObj = m.invoke(null, items2Hide[activeItem])
                    if (viewObj != null) {
                        val view = viewObj as View
                        view.visibility = View.GONE
                    }
                }
            }
        }

        // 关闭下拉形象展示abtest开关
        if (activeItems.contains("下拉形象展示")) {
            Initiator.load("com.tencent.mobileqq.activity.qqsettingme.utils.a")?.getDeclaredMethod("f")!!
                .hookBefore { it.result = false }
        }
    }

    private fun stringHit(string: String): Boolean {
        val mActiveItems = activeItems
        for (pair in keyWords) {
            if (string.contains(pair.key) && mActiveItems.contains(pair.value)) {
                return true
            }
        }
        return false
    }
}
