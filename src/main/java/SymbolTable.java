import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    public enum Type { INT, FLOAT, STRING, BOOLEAN }

    private static class Symbol {
        Type type;
        Object value;
        Symbol(Type type, Object value) {
            this.type = type;
            this.value = value;
        }
    }

    private final Map<String, Symbol> table = new HashMap<>();

    public void declare(String name, Type type, Object initialValue) {
        if (table.containsKey(name))
            throw new RuntimeException("Error semántico: variable '" + name + "' ya declarada.");
        table.put(name, new Symbol(type, initialValue));
    }

    public void assign(String name, Object value) {
        if (!table.containsKey(name))
            throw new RuntimeException("Error semántico: variable '" + name + "' no declarada.");
        table.get(name).value = value;
    }

    public Object getValue(String name) {
        if (!table.containsKey(name))
            throw new RuntimeException("Error semántico: variable '" + name + "' no declarada.");
        return table.get(name).value;
    }

    public Type getType(String name) {
        if (!table.containsKey(name))
            throw new RuntimeException("Error semántico: variable '" + name + "' no declarada.");
        return table.get(name).type;
    }

    public boolean isDeclared(String name) {
        return table.containsKey(name);
    }
}