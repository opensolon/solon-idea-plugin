package org.noear.solon.idea.plugin.navigation;

import com.intellij.lang.properties.psi.Property;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.noear.solon.idea.plugin.metadata.source.PropertyName;

import java.util.Iterator;

class PropertiesToPsiReference extends SolonPropertyToPsiReference<PropertyKeyImpl> {
    PropertiesToPsiReference(@NotNull PropertyKeyImpl source) {
        super(source);
    }

    @Override
    protected Iterator<String> candidateKeys(PropertyKeyImpl property) {
        String key = PsiTreeUtil.getParentOfType(property, Property.class).getUnescapedKey();
        assert key != null;
        PropertyName pn = PropertyName.adapt(key);
        return new Iterator<>() {
            private PropertyName next = pn;

            @Override
            public boolean hasNext() {
                return !next.isEmpty();
            }

            @Override
            public String next() {
                PropertyName n = next;
                next = next.getParent();
                return n.toString();
            }
        };
    }
}
