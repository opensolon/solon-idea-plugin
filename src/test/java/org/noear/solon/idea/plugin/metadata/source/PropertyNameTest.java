package org.noear.solon.idea.plugin.metadata.source;


class PropertyNameTest {

    public static void main(String[] args) {
        String oStr = "a.b[*].*.c";
        PropertyName propertyName = PropertyName.of(oStr);

        assert propertyName.toString().equals(oStr);

        propertyName = propertyName.subName(1);
        assert propertyName.toString().equals("b[*].*.c");
        propertyName = propertyName.subName(1);
        assert propertyName.toString().equals("[*].*.c");
        propertyName = propertyName.subName(1);
        assert propertyName.toString().equals("*.c");
        propertyName = propertyName.subName(1);
        assert propertyName.toString().equals("c");
    }

}