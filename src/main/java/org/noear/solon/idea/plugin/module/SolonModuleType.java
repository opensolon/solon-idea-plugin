package org.noear.solon.idea.plugin.module;

import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import org.jetbrains.annotations.NotNull;
import org.noear.solon.idea.plugin.SolonIcons;
import org.noear.solon.idea.plugin.initializr.SolonInitializrBuilder;

import javax.swing.*;

/**
 * @author liupeiqiang
 * @date 2023/2/6 12:17
 */
public class SolonModuleType extends ModuleType<SolonInitializrBuilder> {

    private static final String ID = "SOLON_MODULE_TYPE";

    public SolonModuleType() {
        super(ID);
    }

    public static SolonModuleType getInstance() {
        return (SolonModuleType) ModuleTypeManager.getInstance().findByID(ID);
    }

    @NotNull
    @Override
    public SolonInitializrBuilder createModuleBuilder() {
        return new SolonInitializrBuilder();
    }

    @NotNull
    @Override
    public String getName() {
        return "Solon Module";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Solon Module";
    }

    @NotNull
    @Override
    public Icon getNodeIcon(@Deprecated boolean b) {
        return SolonIcons.SolonIcon_16x16;
    }


}
