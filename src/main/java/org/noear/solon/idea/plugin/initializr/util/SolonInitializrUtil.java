package org.noear.solon.idea.plugin.initializr.util;

import com.google.common.escape.Escaper;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.Nullable;

import static com.google.common.net.UrlEscapers.urlFormParameterEscaper;

/**
 * @author liupeiqiang
 * @date 2023/2/7 0:54
 */
public final class SolonInitializrUtil {

    private static final Escaper FORM_PARAMETER_ESCAPER = urlFormParameterEscaper();

    @Nullable
    public static JavaSdkVersion from(WizardContext context, ModuleBuilder builder) {
        Sdk wizardSdk = context.isCreatingNewProject() ?
                context.getProjectJdk() :
                ObjectUtils.chooseNotNull(builder.getModuleJdk(), context.getProjectJdk());
        return wizardSdk == null ? null : JavaSdk.getInstance().getVersion(wizardSdk);
    }

    public static String nameAndValueAsUrlParam(String name, String value) {
        return name + "=" + FORM_PARAMETER_ESCAPER.escape(value);
    }

}
