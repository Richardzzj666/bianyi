package miniplc0java.instruction;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class Function {
    int name;
    int ret_slot;
    int param_slot;
    int loc_slot;
    int count;
    ArrayList<Item> items;

    public Function(int name, int ret_slot, int param_slot, int loc_slot) {
        this.name = name;
        this.ret_slot = ret_slot;
        this.param_slot = param_slot;
        this.loc_slot = loc_slot;
        this.count = 0;
        this.items = new ArrayList<>();
    }

    public void addItem(String operation, String num) {
        this.items.add(new Item(operation, num));
        this.count++;
    }
}

class Item {
    String operation;
    String num;

    public Item(String operation, String num) {
        this.operation = operation;
        this.num = num;
    }
}
