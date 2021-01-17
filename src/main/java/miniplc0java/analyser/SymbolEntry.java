package miniplc0java.analyser;

public class SymbolEntry {
    boolean isConstant;
    boolean isInitialized;
    boolean isFunction;

    //所在符号表中的索引
    int index;
    StackItem type;

    /**
     * @param isConstant
     * @param isDeclared
     */
    public SymbolEntry(boolean isConstant, boolean isDeclared, boolean isFunction, int index, String type) {
        this.isConstant = isConstant;
        this.isInitialized = isDeclared;
        this.isFunction = isFunction;
        this.index = index;
        if ("int".equals(type)) {
            this.type = StackItem.INT;
        } else if ("double".equals(type)) {
            this.type = StackItem.DOUBLE;
        } else {
            this.type = null;
        }
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
}
