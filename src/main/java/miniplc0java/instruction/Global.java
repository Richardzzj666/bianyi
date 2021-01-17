package miniplc0java.instruction;

public class Global {
    public boolean is_const;
    public int count;
    public byte[] items;

    public Global() {
        this.is_const = false;
        this.count = 0;
        this.items = null;
    }

    public Global(boolean is_const, int count, byte[] items) {
        this.is_const = is_const;
        this.count = count;
        this.items = items;
    }

}
