package org.noear.solon.idea.plugin.navigation;

import com.intellij.codeInspection.reference.PsiMemberReference;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiReferenceBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.noear.solon.idea.plugin.metadata.index.MetadataItem;
import org.noear.solon.idea.plugin.metadata.index.MetadataProperty;
import org.noear.solon.idea.plugin.metadata.service.ModuleMetadataService;
import org.noear.solon.idea.plugin.metadata.source.PropertyName;
import org.noear.solon.idea.plugin.misc.PsiTypeUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.regex.Pattern;

abstract class SolonPropertyToPsiReference<T extends PsiElement>
        extends PsiReferenceBase<T> implements PsiMemberReference {

    private static final Logger LOG = Logger.getInstance(SolonPropertyToPsiReference.class);

    SolonPropertyToPsiReference(@NotNull T source) {
        super(source, true);
    }

    @Override
    public @Nullable PsiElement resolve() {
        Module module = ModuleUtil.findModuleForPsiElement(getElement());
        if (module == null) {
            return null;
        }

        Iterator<String> candidates = candidateKeys(getElement());
        PropertyName propertyName = this.getPropertyName(getElement());
        ModuleMetadataService metadataService = ModuleMetadataService.getInstance(module);

        String propertyStr = propertyName.toString();
        // demo.bConfigs[0].cConfig[1].name

        Pattern listPattern = Pattern.compile(".*\\[\\d+\\].*");
        if (listPattern.matcher(propertyStr).matches()) {
            // list
            // 获取 [] 前
            String[] split = propertyStr.split("\\[\\d+\\]");
            String key = split[0];
            MetadataItem propertyOrGroup = metadataService.getIndex().getPropertyOrGroup(key);
            if (Objects.nonNull(propertyOrGroup)) {
                Project project = module.getProject();

                // java.util.List<xx>
                String typeStr = propertyOrGroup.getTypeStr();
                // 提取 <xx>
                int start = typeStr.indexOf("<") + 1;
                int end = typeStr.indexOf(">");
                String innerType = typeStr.substring(start, end);

                PsiClass psiClass = PsiTypeUtils.findClass(project, innerType);
                if (Objects.nonNull(psiClass)) {
                    String fieldPath = String.join("", Arrays.stream(split).skip(1).toList());
                    PsiField field = psiClass.findFieldByName(fieldPath, true);
                    if (Objects.nonNull(field)) {
                        return field;
                    } else {
                        LOG.info("can not find field: " + fieldPath + " in class: " + innerType);
                    }
                } else {
                    LOG.info("can not find class: " + innerType);
                }

            }
        } else {
            while (candidates.hasNext()) {
                String key = candidates.next();
                MetadataItem propertyOrGroup = metadataService.getIndex().getPropertyOrGroup(key);
                if (propertyOrGroup == null) {
                    continue;
                }
                if (propertyOrGroup instanceof MetadataProperty property) {
                    return property.getSourceField().map(f -> (PsiElement) f).or(property::getSourceType).orElse(null);
                } else {
                    return propertyOrGroup.getSourceType().orElse(null);
                }
            }
        }


        return null;
    }

    protected abstract Iterator<String> candidateKeys(T source);

    protected abstract PropertyName getPropertyName(T source);
}
