package com.alphi.qhmk.module;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import java.lang.reflect.Method;
import kotlin.collections.ArraysKt;

@UiItemAgentEntry
@FunctionHookEntry
public class HookQWallet extends CommonSwitchFunctionHook {

    private HookQWallet() {
    }

    public static final HookQWallet INSTANCE = new HookQWallet();

    @Override
    protected boolean initOnce() throws Exception {
        // NT
        Class<?> kQWalletHomeFragment = Initiator.load("com/tencent/mobileqq/qwallet/home/QWalletHomeFragment");
        if (kQWalletHomeFragment != null) {
            Class<?> kQWalletHomePreviewController = Initiator.loadClass("com/tencent/mobileqq/qwallet/home/QWalletHomePreviewController");
            // public final QWalletHomePreviewController.?(QWallBaseFragment|QWalletBaseFragment)Z
            Method method1 = ArraysKt.single(kQWalletHomePreviewController.getDeclaredMethods(),
                    it -> it.getReturnType() == boolean.class && it.getParameterTypes().length == 1 &&
                            it.getParameterTypes()[0].getSimpleName().endsWith("BaseFragment"));
            HookUtils.hookBeforeIfEnabled(this, method1, param -> param.setResult(true));
            Method onViewCreated = kQWalletHomeFragment.getDeclaredMethod("onViewCreated", View.class, Bundle.class);
            HookUtils.hookBeforeIfEnabled(this, onViewCreated, param -> {
                // 加载广告及视图初始化
                param.setResult(null);
            });
        }
        // 高版本 已测试 8.8.50
        Class<?> aClass = Initiator.load("Lcom/tencent/mobileqq/qwallet/config/impl/QWalletConfigServiceImpl;");
        if (aClass != null) {
            for (Method method : aClass.getDeclaredMethods()) {
                XposedBridge.hookMethod(method, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) {
                        if (method.getReturnType() == boolean.class) {
                            return false;
                        }
                        if (method.getReturnType() == int.class) {
                            return 0;
                        }
                        if (method.getReturnType() == long.class) {
                            return 0L;
                        }
                        return null;
                    }
                });
            }
            return true;
        }

        // 针对低版本 8.4.0
        aClass = Initiator.loadClass("Lcom/tencent/mobileqq/activity/qwallet/config/QWalletConfigManager;");
        XposedBridge.hookAllConstructors(aClass, HookUtils.beforeIfEnabled(this, param -> param.args[0] = null));
        return true;
    }

    @NonNull
    @Override
    public String getName() {
        return "去除QQ钱包广告";
    }

    @Nullable
    @Override
    public CharSequence getDescription() {
        return "省流量方案需重启";
    }

    @Override
    public boolean isApplicationRestartRequired() {
        return true;
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Simplify.UI_MISC;
    }

}
