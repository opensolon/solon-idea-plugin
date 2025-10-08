package org.noear.solon.idea.plugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.noear.solon.idea.plugin.common.util.ProjectUtil;
import org.noear.solon.idea.plugin.metadata.service.ModuleMetadataService;
import org.noear.solon.idea.plugin.metadata.service.ModuleMetadataServiceImpl;

import java.util.Set;

public class RefreshMetadataAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(RefreshMetadataAction.class);

    @Override
    public void actionPerformed(AnActionEvent e) {
        LOG.info("refresh solon metadata by action");
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (psiFile == null) return;

        Project project = e.getProject();
        Module module = ModuleUtil.findModuleForFile(psiFile.getVirtualFile(), project);
        if (module == null) return;

        ModuleMetadataService service = module.getService(ModuleMetadataService.class);
        if (service instanceof ModuleMetadataServiceImpl impl) {
            Set<VirtualFile> additionalProjectRootsToIndex = ProjectUtil.getAdditionalProjectRootsToIndex(project);
            impl.refreshMetadata(additionalProjectRootsToIndex, true);
        }
    }
}