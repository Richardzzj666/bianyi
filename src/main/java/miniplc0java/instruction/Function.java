package miniplc0java.instruction;

import miniplc0java.analyser.StackItem;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class Function {
    public int name;
    public int ret_slot;
    public int param_slot;
    public int loc_slot;
    public int count;
    public ArrayList<Item> items;
    public StackItem type;

    public Function(int name, int ret_slot, int param_slot, int loc_slot, StackItem type) {
        this.name = name;
        this.ret_slot = ret_slot;
        this.param_slot = param_slot;
        this.loc_slot = loc_slot;
        this.count = 0;
        this.items = new ArrayList<>();
        this.type = type;
    }

    public void addItem(byte operation, byte[] num) {
        this.items.add(new Item(operation, num));
        this.count++;
    }
}

class Item {
    byte operation;
    byte[] num;

    public Item(byte operation, byte[] num) {
        this.operation = operation;
        this.num = num;
    }
}
