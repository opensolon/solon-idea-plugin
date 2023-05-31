package org.noear.solon.idea.plugin.initializr.processor;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;

public interface ModuleBuilderPostProcessor {
    ExtensionPointName<ModuleBuilderPostProcessor> EXTENSION_POINT_NAME =
            ExtensionPointName.create("solon.initializr.moduleBuilderPostProcessor");

    /**
     * @param module module
     * @return true if project is imported, false otherwise
     */
    boolean postProcess(Module module);
}