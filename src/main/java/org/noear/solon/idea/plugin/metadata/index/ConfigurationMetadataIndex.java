package org.noear.solon.idea.plugin.metadata.index;

import com.esotericsoftware.kryo.kryo5.minlog.Log;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.noear.solon.idea.plugin.metadata.source.ConfigurationMetadata;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * An index created from a {@link ConfigurationMetadata}
 */
@SuppressWarnings("LombokGetterMayBeUsed")
public class ConfigurationMetadataIndex extends MetadataIndexBase {
    private static final Logger LOG = Logger.getInstance(ConfigurationMetadataIndex.class);

    protected final List<Pattern> patterns = new LinkedList<>();
    protected final Map<String, MetadataProperty> patternNameAndProperties = new HashMap<>();
    private final MetadataSource source;


    public ConfigurationMetadataIndex(
            @NotNull ConfigurationMetadata metadata, @NotNull PsiElement sourceElement, @NotNull Project project) {
        super(project);
        add(sourceElement.toString(), metadata);
        this.source = new PsiElementMetadataSource(sourceElement);
        this.source.markSynchronized();
    }


    public ConfigurationMetadataIndex(@NotNull FileMetadataSource source, @NotNull Project project) throws IOException {
        super(project);
        add(source.getPresentation(), source.getContent());
        this.source = source;
    }


    @Override
    public @NotNull List<MetadataSource> getSource() {
        return List.of(source);
    }

    @Override
    public MetadataItem getPropertyOrGroup(String name) {
        MetadataItem propertyOrGroup = super.getPropertyOrGroup(name);
        if (propertyOrGroup != null) {
            return propertyOrGroup;
        }
        Pattern pattern = this.patterns
                .stream()
                .filter(item -> item.matcher(name).matches())
                .findFirst()
                .orElse(null);
        if (pattern != null) {
            Log.debug("Found pattern for property: " + name);
            return patternNameAndProperties.get(pattern.pattern());
        }
        return null;
    }

    @Override
    protected void add(ConfigurationMetadata.Property p) {
        String propertyName = p.getName();
        if (propertyName.contains("*")) {
            String patternPropertyName = propertyName
                    .replace(".", "\\.")
                    .replace("*", ".*");
            this.patterns.add(Pattern.compile(patternPropertyName));
            MetadataPropertyImpl prop = new MetadataPropertyImpl(this, p);
            this.patternNameAndProperties.put(patternPropertyName, prop);
        } else {
            super.add(p);
        }
    }
}
