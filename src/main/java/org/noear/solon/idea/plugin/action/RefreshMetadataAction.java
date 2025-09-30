package org.noear.solon.idea.plugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiFile;
import org.noear.solon.idea.plugin.metadata.service.ModuleMetadataService;
import org.noear.solon.idea.plugin.metadata.service.ModuleMetadataServiceImpl;

public class RefreshMetadataAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(RefreshMetadataAction.class);

    @Override
    public void actionPerformed(AnActionEvent e) {
        LOG.info("refresh solon metadata by action");
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (psiFile == null) return;

        Module module = ModuleUtil.findModuleForFile(psiFile.getVirtualFile(), e.getProject());
        if (module == null) return;

        ModuleMetadataService service = module.getService(ModuleMetadataService.class);
        if (service instanceof ModuleMetadataServiceImpl impl) {
            impl.refreshMetadata();
        }
    }
}