package org.noear.solon.idea.plugin.navigation.forward;

import com.intellij.lang.properties.PropertiesLanguage;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.noear.solon.idea.plugin.metadata.service.SolonConfigFilesService;


/**
 * Provides references from Solon configuration file (application.properties) to code.
 */
public class PropertiesToCodeReferenceContributor extends PsiReferenceContributor {

//TODO refactor by com.intellij.psi.search.searches.DefinitionsScopedSearch.EP and
// com.intellij.psi.search.searches.ReferencesSearch.EP_NAME


    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        // 同 YamlToCodeReferenceContributor：不能只认 app.properties，
        // 还要覆盖 loadAdd*("classpath:xxx.properties") 加载的插件配置。
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(PropertyKeyImpl.class)
                        .withLanguage(PropertiesLanguage.INSTANCE),
                new AbstractReferenceProvider() {
                    @Override
                    protected PsiElement getRefSource(@NotNull PsiElement element, @NotNull ProcessingContext context) {
                        if (!(element instanceof PropertyKeyImpl key)) {
                            return null;
                        }
                        VirtualFile file = element.getContainingFile().getVirtualFile();
                        return SolonConfigFilesService.getInstance(element.getProject())
                                .isSolonConfigFile(file) ? key : null;
                    }
                });
    }
}
