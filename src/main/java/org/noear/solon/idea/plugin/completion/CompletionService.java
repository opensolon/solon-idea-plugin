package org.noear.solon.idea.plugin.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupElementDecorator;
import com.intellij.codeInsight.lookup.VariableLookupItem;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiVariable;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.noear.solon.idea.plugin.common.util.GenericUtil;
import org.noear.solon.idea.plugin.metadata.index.*;
import org.noear.solon.idea.plugin.metadata.index.hint.Hint;
import org.noear.solon.idea.plugin.metadata.index.hint.provider.HandleAsValueProvider;
import org.noear.solon.idea.plugin.metadata.index.hint.value.ValueHint;
import org.noear.solon.idea.plugin.metadata.service.ModuleMetadataService;
import org.noear.solon.idea.plugin.metadata.source.ConfigurationMetadata;
import org.noear.solon.idea.plugin.metadata.source.PropertyName;
import org.noear.solon.idea.plugin.misc.PsiTypeUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.intellij.openapi.module.ModuleUtilCore.findModuleForPsiElement;
import static org.noear.solon.idea.plugin.metadata.source.ConfigurationPropertyName.Form.UNIFORM;

@Service(Service.Level.PROJECT)
public final class CompletionService {
    private final Project project;


    public CompletionService(Project project) {
        this.project = project;
    }


    public static CompletionService getInstance(Project project) {
        return project.getService(CompletionService.class);
    }

    private static @NotNull PrefixMatcher getPrefixMatcher(@Nullable PrefixMatcher prefixMatcher, String queryString) {
        if (prefixMatcher == null || StringUtils.isBlank(prefixMatcher.getPrefix())) {
            return new CamelHumpMatcher(queryString, false);
        } else {
            return prefixMatcher;
        }
    }

    public boolean findSuggestionForKey(
            @NotNull CompletionParameters completionParameters, @NotNull CompletionResultSet resultSet,
            @Nullable String parentName, String queryString, InsertHandler<LookupElement> insertHandler
    ) {
        Module module = findModule(completionParameters);
        Collection<MetadataItem> candidates = findProperty(module, parentName, queryString);
        List<? extends LookupElement> suggestions = null;
        if (!candidates.isEmpty()) {
            suggestions = candidates.stream().map(metaItem -> {
                        if (metaItem instanceof MetadataProperty property) {
                            return createLookupElement(parentName, property);
                        } else if (metaItem instanceof MetadataGroup group) {
                            return createLookupElement(parentName, group);
                        } else {
                            throw new IllegalStateException("Unexpected value: " + metaItem);
                        }
                    })
                    .filter(Objects::nonNull)
                    .map(le -> LookupElementDecorator.withInsertHandler(le, insertHandler))
                    .toList();
        }
        // Or maybe user is asking suggestion for a Map key
        MetadataProperty property = ModuleMetadataService.getInstance(module).getIndex().getProperty(parentName);
        if (property != null && property.isMapType()) {
            suggestions = completionForMapKey(
                    property, completionParameters, resultSet.getPrefixMatcher(), queryString, insertHandler);
        }
        if (suggestions != null && !suggestions.isEmpty()) {
            resultSet.addAllElements(suggestions);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Retrieve candidates for a property's value completion.
     *
     * @param propertyName The context property name for querying value, must be existed.
     * @param queryString  The user input for completion.
     * @return true if some suggestions have been added to the resultSet.
     */
    public boolean findSuggestionForValue(
            @NotNull CompletionParameters completionParameters,
            @NotNull final CompletionResultSet completionResultSet,
            @NotNull String propertyName, String queryString,
            InsertHandler<LookupElement> insertHandler
    ) {
        //If user is asking suggestion for an array value
        PropertyName propName = PropertyName.adapt(propertyName);
        if (propName.isNumericIndex(propName.getNumberOfElements() - 1)) {
            propName = propName.chop(propName.getNumberOfElements() - 1);
            propertyName = propName.toString();
        }
        completionResultSet.restartCompletionWhenNothingMatches();
        CompletionResultSet resultSet = completionResultSet.caseInsensitive();
        Module module = findModule(completionParameters);
        PrefixMatcher prefixMatcher = resultSet.getPrefixMatcher();
        List<LookupElement> hints = completionForValue(completionParameters, propertyName, prefixMatcher, queryString,
                insertHandler);
        if (!hints.isEmpty()) {
            resultSet.addAllElements(hints);
            return true;
        }
        // Maybe we are looking for a Map's value suggestion
        String parentKey = propName.getParent().toString();
        if (StringUtils.isNotBlank(parentKey)) {
            MetadataIndex index = ModuleMetadataService.getInstance(module).getIndex();
            MetadataProperty parent = index.getProperty(parentKey);
            if (parent != null && parent.isMapType()) {
                hints = completionForValue(completionParameters, parentKey, prefixMatcher, queryString, insertHandler);
                if (!hints.isEmpty()) {
                    resultSet.addAllElements(hints);
                    return true;
                }
            }
            // If we have a property whose type is Map<String,?>, it can map to any depth of key,
            // so let's find to the ancestors till find it, and use its value's hint.
            parent = index.getNearestParentProperty(parentKey);
            if (parent != null && parent.getFullType().filter(t -> PsiTypeUtils.isValueMap(project, t)).isPresent()) {
                hints = completionForValue(completionParameters, parent.getNameStr(), prefixMatcher, queryString,
                        insertHandler);
                if (!hints.isEmpty()) {
                    resultSet.addAllElements(hints);
                    return true;
                }
            }
        }
        return false;
    }

    private Collection<MetadataItem> findProperty(
            @NotNull Module module, @Nullable String parentName, String queryString) {
        if (parentName == null) parentName = "";
        NameTreeNode searchRoot = ModuleMetadataService.getInstance(module).getIndex().findInNameTrie(parentName.trim());
        if (searchRoot == null || searchRoot.isIndexed()) {
            // we can't provide suggestion for an indexed key, user has to create the sub element then ask for suggestion.
            return Collections.emptySet();
        }
        Collection<NameTreeNode> candidates = Collections.singleton(searchRoot);
        if (StringUtils.isNotBlank(queryString)) {
            PropertyName query = PropertyName.adapt(queryString);
            for (int i = 0; !candidates.isEmpty() && i < query.getNumberOfElements(); i++) {
                String qp = query.getElement(i, UNIFORM);
                candidates = candidates.parallelStream().filter(tn -> !tn.isIndexed()).map(NameTreeNode::getChildren)
                        .map(trie -> trie.prefixMap(qp)).flatMap(m -> m.values().parallelStream()).collect(Collectors.toSet());
            }
        }
        // get all properties in candidates;
        Collection<NameTreeNode> nodes = candidates;
        Set<MetadataItem> result = new HashSet<>();
        while (!nodes.isEmpty()) {
            Set<NameTreeNode> nextNodes = new HashSet<>();
            for (NameTreeNode n : nodes) {
                if (n != searchRoot) {
                    result.addAll(n.getData());
                }
                if (!n.isIndexed()) {
                    // Suggestion should not contain indexes(Map or List), because it is hard to insert this suggestion to code.
                    nextNodes.add(n);
                }
            }
            nodes = nextNodes.parallelStream().flatMap(tn -> tn.getChildren().values().stream()).collect(Collectors.toSet());
        }
        return result;
    }

    @NotNull
    private Module findModule(CompletionParameters completionParameters) {
        return Objects.requireNonNull(findModuleForPsiElement(completionParameters.getPosition()));
    }

    private List<LookupElement> completionForMapKey(
            MetadataProperty property, @NotNull CompletionParameters completionParameters,
            @Nullable PrefixMatcher prefixMatcher, String queryString, InsertHandler<LookupElement> insertHandler
    ) {
        return property.getKeyHint()
                .map(h -> getHintValues(h, completionParameters, prefixMatcher, queryString, insertHandler))
                .orElseGet(Collections::emptyList);
    }

    private @NotNull List<LookupElement> completionForValue(
            @NotNull CompletionParameters completionParameters, @NotNull String propertyName,
            @Nullable PrefixMatcher prefixMatcher, String queryString, InsertHandler<LookupElement> insertHandler
    ) {
        Module module = findModule(completionParameters);
        MetadataProperty property = ModuleMetadataService.getInstance(module).getIndex().getProperty(propertyName);
        if (property == null) return List.of();
        Optional<MetadataHint> hint = property.getHint();
        if (hint.isPresent()) {
            return getHintValues(hint.get(), completionParameters, prefixMatcher, queryString, insertHandler);
        } else {
            // If no hint available, try to provide completion for some specific property type like there is handle-as hint
            PrefixMatcher matcher = getPrefixMatcher(prefixMatcher, queryString);
            return HandleAsValueProvider.getHandler(property.getMetadata().getType())
                    .handle(completionParameters, matcher)
                    .stream().map(this::createLookupElement)
                    .map(le -> (LookupElement) LookupElementDecorator.withInsertHandler(le, insertHandler))
                    .toList();
        }
    }

    /**
     * Use metadata hint - value providers to provide candidates.
     */
    private List<LookupElement> getHintValues(
            MetadataHint hint, CompletionParameters completionParameters,
            @Nullable PrefixMatcher prefixMatcher, String queryString, InsertHandler<LookupElement> insertHandler
    ) {
        PrefixMatcher matcher = getPrefixMatcher(prefixMatcher, queryString);
        return Stream.concat(
                        hint.getValues().stream().map(ValueHint::toHint),
                        hint.getProviders().parallelStream().flatMap(vp ->
                                vp.provideValues(completionParameters, matcher).stream())
                ).map(this::createLookupElement)
                .filter(matcher::prefixMatches)
                .map(le -> (LookupElement) LookupElementDecorator.withInsertHandler(le, insertHandler))
                .toList();
    }

    private LookupElement getLookupElement(Hint hint) {
        PsiElement psiElement = hint.psiElement();
        if (psiElement instanceof PsiVariable psiVariable) {
            return new VariableLookupItem(psiVariable);
        } else if (psiElement instanceof PsiClass psiClass) {
            JavaPsiClassReferenceElement li = new JavaPsiClassReferenceElement(psiClass);
            if (StringUtils.isNotBlank(hint.value())) {
                li.setLookupString(hint.value());
                li.setForcedPresentableName(li.getLookupString());
            }
            return li;
        } else if (psiElement instanceof PsiMethod psiMethod) {
            return new JavaMethodCallElement(psiMethod);
        } else {
            LookupElementBuilder le = ReadAction.compute(() ->
                    LookupElementBuilder.create(hint.value()).withIcon(hint.icon())
                            .withPsiElement(new SourceContainer(hint, project)));
            if (StringUtils.isNotBlank(hint.oneLineDescription())) {
                le = le.withTailText("(" + hint.oneLineDescription() + ")", true);
            }
            return le;
        }
    }

    private LookupElement createLookupElement(Hint hint) {
        LookupElement result = this.getLookupElement(hint);
        if (hint.priorityGroup() != null) {
            return PrioritizedLookupElement.withGrouping(result, hint.priorityGroup());
        } else {
            return result;
        }
    }

    private LookupElement createLookupElement(String propertyNameAncestors, MetadataProperty property) {
        ConfigurationMetadata.Property.Deprecation deprecation = property.getMetadata().getDeprecation();
        if (deprecation != null && deprecation.getLevel() == ConfigurationMetadata.Property.Deprecation.Level.ERROR) {
            // Fully unsupported property should not be included in suggestions
            return null;
        }
        LookupElementBuilder leb = LookupElementBuilder.create(removeParent(propertyNameAncestors, property.getNameStr()))
                .withIcon(property.getIcon().getSecond()).withPsiElement(new SourceContainer(property, project))
                .withStrikeoutness(deprecation != null);
        if (StringUtils.isNotBlank(property.getMetadata().getDescription())) {
            leb = leb.withTailText("(" + property.getMetadata().getDescription() + ")", true);
        }
        if (StringUtils.isNotBlank(property.getMetadata().getType())) {
            leb = leb.withTypeText(GenericUtil.shortenJavaType(property.getMetadata().getType()), true);
        }
        return leb;
    }

    private LookupElement createLookupElement(String propertyNameAncestors, MetadataGroup group) {
        return LookupElementBuilder.create(removeParent(propertyNameAncestors, group.getNameStr()))
                .withIcon(group.getIcon().getSecond()).withPsiElement(new SourceContainer(group, project));
    }

    private String removeParent(String parent, String name) {
        PropertyName parentKey = PropertyName.adapt(parent);
        PropertyName key = PropertyName.adapt(name);
        assert parentKey.isAncestorOf(key) : "Invalid parent and child:" + parentKey + "," + key;
        return key.subName(parentKey.getNumberOfElements()).toString();
    }
}
