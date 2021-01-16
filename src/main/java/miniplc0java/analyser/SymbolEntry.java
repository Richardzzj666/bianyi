package miniplc0java.analyser;

public class SymbolEntry {
    boolean isConstant;
    boolean isInitialized;
    boolean isFunction;
    byte[] items;
    int index;
    String type;

    /**
     * @param isConstant
     * @param isDeclared
     * @param items
     */
    public SymbolEntry(boolean isConstant, boolean isDeclared, boolean isFunction, byte[] items, int index, String type) {
        this.isConstant = isConstant;
        this.isInitialized = isDeclared;
        this.isFunction = isFunction;
        this.items = items;
        this.index = index;
        this.type = type;
    }

    /**
     * @return the items
     */
    public byte[] getItems() {
        return items;
    }

    /**
     * @return the isConstant
     */
    public boolean isConstant() {
        return isConstant;
    }

    /**
     * @return the isFunction
     */
    public boolean isFunction() {
        return isFunction;
    }

    /**
     * @return the isInitialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * @param isConstant the isConstant to set
     */
    public void setConstant(boolean isConstant) {
        this.isConstant = isConstant;
    }

    /**
     * @param isInitialized the isInitialized to set
     */
    public void setInitialized(boolean isInitialized) {
        this.isInitialized = isInitialized;
    }

    /**
     * @param items the items to set
     */
    public void setItems(byte[] items) {
        this.items = items;
    }
}
