package org.noear.solon.idea.plugin.navigation;

import com.intellij.lang.properties.psi.Property;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.noear.solon.idea.plugin.metadata.source.PropertyName;

import java.util.Iterator;

class PropertiesToPsiReference extends SolonPropertyToPsiReference<PropertyKeyImpl> {

    private static final Logger LOG = Logger.getInstance(PropertiesToPsiReference.class);

    PropertiesToPsiReference(@NotNull PropertyKeyImpl source) {
        super(source);
    }

    @Override
    protected Iterator<String> candidateKeys(PropertyKeyImpl property) {
        PropertyName pn = this.getPropertyName(property);
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

    @Override
    protected PropertyName getPropertyName(PropertyKeyImpl source) {
        String key = PsiTreeUtil.getParentOfType(source, Property.class).getUnescapedKey();
        assert key != null;
        return PropertyName.adapt(key);
    }
}
