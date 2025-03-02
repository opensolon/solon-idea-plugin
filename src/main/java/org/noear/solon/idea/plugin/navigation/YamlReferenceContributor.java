package org.noear.solon.idea.plugin.navigation;

import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.noear.solon.idea.plugin.suggestion.filetype.SolonPropertiesFileType;
import org.noear.solon.idea.plugin.suggestion.filetype.SolonYamlFileType;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.patterns.PsiJavaPatterns.virtualFile;

//TODO refactor by com.intellij.psi.search.searches.DefinitionsScopedSearch.EP and
// com.intellij.psi.search.searches.ReferencesSearch.EP_NAME
public class YamlReferenceContributor extends PsiReferenceContributor {

  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    System.err.println("registerReferenceProviders");
    PsiElementPattern.Capture<YAMLKeyValue> pattern =
        psiElement(YAMLKeyValue.class)
            .withLanguage(YAMLLanguage.INSTANCE)
            .inVirtualFile(virtualFile().ofType(SolonYamlFileType.INSTANCE));
    registrar.registerReferenceProvider(pattern, new PsiReferenceProvider() {
      @Override
      @NotNull
      public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        if (element instanceof YAMLKeyValue yamlKeyValue) {
          return new YamlKeyToPsiReference[]{new YamlKeyToPsiReference(yamlKeyValue)};
        } else {
          return PsiReference.EMPTY_ARRAY;
        }
      }
    });
  }

}
