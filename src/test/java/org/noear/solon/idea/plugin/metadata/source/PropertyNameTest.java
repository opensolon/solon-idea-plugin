package org.noear.solon.idea.plugin.metadata.source;


import java.util.Objects;

class PropertyNameTest {

    public static void main(String[] args) {
        String oStr = "a.b[*].*.c";
        PropertyName propertyName = PropertyName.of(oStr);

        assert propertyName.toString().equals(oStr);

        propertyName = propertyName.subName(1);
        PropertyNameTest.shouldBeEqual(propertyName.toString(), "b[*].*.c");
        propertyName = propertyName.subName(1);
        PropertyNameTest.shouldBeEqual(propertyName.toString(), "[*].*.c");
        propertyName = propertyName.subName(1);
        PropertyNameTest.shouldBeEqual(propertyName.toString(), "*.c");
        propertyName = propertyName.subName(1);
        PropertyNameTest.shouldBeEqual(propertyName.toString(), "c");
    }

    private static void shouldBeEqual(Object s1, Object s2) {
        if (!Objects.equals(s1, s2)) {
            throw new RuntimeException("Not equal: " + s1 + " != " + s2);
        }
    }
}