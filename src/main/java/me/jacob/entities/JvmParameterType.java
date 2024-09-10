package me.jacob.entities;

public class JvmParameterType {
    private final String typeName;
    private final boolean isArray;
    private final int dimensionality;

    public JvmParameterType(String typeName) {
        this.typeName = typeName;
        this.dimensionality = 0;
        this.isArray = false;
    }

    public JvmParameterType(String typeName, int dimensionality) {
        this.typeName = typeName;
        this.dimensionality = dimensionality;
        this.isArray = true;
    }

    public String getTypeName() {
        return typeName;
    }

    @Override
    public String toString() {
        return typeName;
    }

    public boolean isArray() {
        return isArray;
    }

    public int getDimensionality() {
        return dimensionality;
    }
}
