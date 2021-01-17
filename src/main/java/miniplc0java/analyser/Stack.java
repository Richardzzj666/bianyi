package miniplc0java.analyser;

import java.util.ArrayList;

public class Stack {
    private ArrayList<StackItem> stack;

    public Stack() {
        this.stack = new ArrayList<>();
    }

    public void push (StackItem item) {
        this.stack.add(item);
    }

    public void pop () {
        stack.remove(stack.size() - 1);
    }

    public StackItem getTop () {
        return stack.size() > 0 ? stack.get(stack.size() - 1) : null;
    }

    public StackItem getSecond () {
        return stack.size() > 1 ? stack.get(stack.size() - 2) : null;
    }
}
