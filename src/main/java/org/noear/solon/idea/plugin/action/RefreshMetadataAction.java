package org.noear.solon.idea.plugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
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
    public void update(AnActionEvent e) {
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (virtualFile == null) {
            Editor hostEditor = e.getData(CommonDataKeys.HOST_EDITOR);
            if (hostEditor != null) {
                virtualFile = hostEditor.getVirtualFile();
            }
        }

        if (virtualFile != null) {
            // 检查文件名和扩展名
            String fileName = virtualFile.getName();
            String extension = virtualFile.getExtension();
            boolean isSolonFile = false;

            // 检查是否为 solon properties 文件
            if ("properties".equals(extension) && (fileName.equals("app.properties") || fileName.startsWith("app-"))) {
                isSolonFile = true;
            }
            // 检查是否为 solon yaml 文件
            else if (("yaml".equals(extension) || "yml".equals(extension)) &&
                    (fileName.equals("app.yaml") || fileName.equals("app.yml") ||
                            fileName.startsWith("app-") && (fileName.endsWith(".yaml") || fileName.endsWith(".yml")))) {
                isSolonFile = true;
            }

            e.getPresentation().setVisible(isSolonFile);
        } else {
            e.getPresentation().setVisible(false);
        }
        
    }

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