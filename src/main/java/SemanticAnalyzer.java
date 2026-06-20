import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;

public class SemanticAnalyzer extends MiniLangBaseVisitor<Void> {

    private final SymbolTable symbolTable = new SymbolTable();

    @Override
    public Void visitDeclaracion_variables(MiniLangParser.Declaracion_variablesContext ctx) {
        String name = ctx.ID().getText();
        SymbolTable.Type type = resolveType(ctx.tipo());
        symbolTable.declare(name, type, null);
        return null;
    }

    @Override
    public Void visitInst_asignacion(MiniLangParser.Inst_asignacionContext ctx) {
        String target = ctx.ID().getText();
        if (!symbolTable.isDeclared(target))
            throw new RuntimeException("Error semántico: variable '" + target + "' no declarada.");
        // Aquí pueden agregar validación de tipos
        visit(ctx.exp());
        return null;
    }

    @Override
    public Void visitInst_switch(MiniLangParser.Inst_switchContext ctx) {
        visit(ctx.exp()); // validar la expresión evaluada
        for (var caso : ctx.caso_switch()) {
            visitCaso_switch(caso);
        }
        return null;
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