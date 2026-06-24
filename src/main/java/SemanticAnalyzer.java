import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;

public class SemanticAnalyzer extends MiniLangBaseVisitor<SymbolTable.Type> {

    private final SymbolTable symbolTable = new SymbolTable();

    // 1. Al declarar, guardamos el tipo en la tabla
    @Override
    public SymbolTable.Type visitDeclaracion_variables(MiniLangParser.Declaracion_variablesContext ctx) {
        String name = ctx.ID().getText();
        SymbolTable.Type type = resolveType(ctx.tipo());
        symbolTable.declare(name, type, null);
        return null;
    }

    // 2. ¡EL FILTRO CLAVE! Al asignar con MOVE, validamos si los tipos coinciden
    @Override
    public SymbolTable.Type visitInst_asignacion(MiniLangParser.Inst_asignacionContext ctx) {
        String target = ctx.ID().getText();
        if (!symbolTable.isDeclared(target)) {
            throw new RuntimeException("Error semántico: variable '" + target + "' no declarada.");
        }
        
        SymbolTable.Type tipoVariable = symbolTable.getType(target);
        SymbolTable.Type tipoExpresion = visit(ctx.exp()); // Obtenemos el tipo de lo que viene después del MOVE
        
        if (tipoVariable != tipoExpresion) {
            throw new RuntimeException("Incompatibilidad de tipos: No se puede asignar un valor de tipo " 
                    + tipoExpresion + " a la variable '" + target + "' de tipo " + tipoVariable + ".");
        }
        return null;
    }

    // 3. Le enseñamos al analizador a reconocer los tipos de los datos sueltos
    @Override
    public SymbolTable.Type visitAtomo(MiniLangParser.AtomoContext ctx) {
        if (ctx.NUM() != null) {
            return ctx.NUM().getText().contains(".") ? SymbolTable.Type.FLOAT : SymbolTable.Type.INT;
        } 
        if (ctx.STRING() != null) {
            return SymbolTable.Type.STRING;
        }
        if (ctx.TRUE() != null || ctx.FALSE() != null) {
            return SymbolTable.Type.BOOLEAN;
        }
        if (ctx.ID() != null) {
            String name = ctx.ID().getText();
            if (!symbolTable.isDeclared(name)) {
                throw new RuntimeException("Error semántico: variable '" + name + "' no declarada.");
            }
            return symbolTable.getType(name);
        }
        return visit(ctx.exp());
    }

    private SymbolTable.Type resolveType(MiniLangParser.TipoContext ctx) {
        String t = ctx.getText();
        return switch (t) {
            case "9"       -> SymbolTable.Type.INT;
            case "V"       -> SymbolTable.Type.FLOAT;
            case "X"       -> SymbolTable.Type.STRING;
            case "BOOLEAN" -> SymbolTable.Type.BOOLEAN;
            default -> throw new RuntimeException("Tipo desconocido: " + t);
        };
    }
}