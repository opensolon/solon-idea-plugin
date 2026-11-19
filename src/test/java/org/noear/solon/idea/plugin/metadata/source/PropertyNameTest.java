package org.noear.solon.idea.plugin.metadata.source;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertyNameTest {

    @Test
    void shouldSplitBySubName() {
        String oStr = "a.b[*].*.c";
        PropertyName propertyName = PropertyName.of(oStr);

        assertEquals(oStr, propertyName.toString());

        propertyName = propertyName.subName(1);
        assertEquals("b[*].*.c", propertyName.toString());
        propertyName = propertyName.subName(1);
        assertEquals("[*].*.c", propertyName.toString());
        propertyName = propertyName.subName(1);
        assertEquals("*.c", propertyName.toString());
        propertyName = propertyName.subName(1);
        assertEquals("c", propertyName.toString());
    }
}
