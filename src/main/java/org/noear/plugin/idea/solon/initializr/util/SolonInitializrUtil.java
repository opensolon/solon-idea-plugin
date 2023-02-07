package org.noear.plugin.idea.solon.initializr.util;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.Nullable;

/**
 * @author liupeiqiang
 * @date 2023/2/7 0:54
 */
public class SolonInitializrUtil {

    @Nullable
    public static JavaSdkVersion from(WizardContext context, ModuleBuilder builder) {
        Sdk wizardSdk = context.isCreatingNewProject() ?
                context.getProjectJdk() :
                ObjectUtils.chooseNotNull(builder.getModuleJdk(), context.getProjectJdk());
        return wizardSdk == null ? null : JavaSdk.getInstance().getVersion(wizardSdk);
    }

}
