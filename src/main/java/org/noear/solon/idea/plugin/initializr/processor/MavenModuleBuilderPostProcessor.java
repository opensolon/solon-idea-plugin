package org.noear.solon.idea.plugin.initializr.processor;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.noear.solon.idea.plugin.common.util.PsiCustomUtil;

import java.util.Collections;

public class MavenModuleBuilderPostProcessor implements ModuleBuilderPostProcessor {
    @Override
    public boolean postProcess(final Module module) {
        Project project = module.getProject();
        VirtualFile pomFile = PsiCustomUtil.findFileUnderRootInModule(module, "pom.xml");
        if (pomFile == null) { // not a maven project
            return true;
        } else {
            MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(project);
            mavenProjectsManager.addManagedFiles(Collections.singletonList(pomFile));
            return false;
        }
    }
}