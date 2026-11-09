package org.noear.solon.idea.plugin.navigation.forward;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.noear.solon.idea.plugin.metadata.service.SolonConfigFilesService;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class YamlToCodeReferenceContributor extends PsiReferenceContributor {

//TODO refactor by com.intellij.psi.search.searches.DefinitionsScopedSearch.EP and
// com.intellij.psi.search.searches.ReferencesSearch.EP_NAME


    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        // 不能只在 SolonYamlFileType（app*.yml）上注册：Solon 插件组件通过
        // cfg().loadAdd*("classpath:oauth2.yml") 加载自带的 yml，这些文件同样是 Solon 配置，
        // 只是不叫 app.yml。这里对所有 YAML 注册，运行时用服务精确过滤。
        registrar.registerReferenceProvider(
                psiElement(YAMLKeyValue.class).withLanguage(YAMLLanguage.INSTANCE),
                new AbstractReferenceProvider() {
                    @Override
                    protected PsiElement getRefSource(@NotNull PsiElement element, @NotNull ProcessingContext context) {
                        if (!(element instanceof YAMLKeyValue yamlKeyValue)) {
                            return null;
                        }
                        VirtualFile file = element.getContainingFile().getVirtualFile();
                        return SolonConfigFilesService.getInstance(element.getProject())
                                .isSolonConfigFile(file) ? yamlKeyValue : null;
                    }
                });
    }
}
