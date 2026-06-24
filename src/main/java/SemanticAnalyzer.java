import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;

public class SemanticAnalyzer extends MiniLangBaseVisitor<SymbolTable.Type> {

    private final SymbolTable symbolTable = new SymbolTable();

    // 1. Declaración de variables
    @Override
    public SymbolTable.Type visitDeclaracion_variables(MiniLangParser.Declaracion_variablesContext ctx) {
        String name = ctx.ID().getText();
        SymbolTable.Type type = resolveType(ctx.tipo());
        symbolTable.declare(name, type, null);
        return null;
    }

    // 2. Asignación con MOVE
    @Override
    public SymbolTable.Type visitInst_asignacion(MiniLangParser.Inst_asignacionContext ctx) {
        String target = ctx.ID().getText();
        if (!symbolTable.isDeclared(target))
            throw new RuntimeException("Error semántico: variable '" + target + "' no declarada.");

        SymbolTable.Type tipoVariable   = symbolTable.getType(target);
        SymbolTable.Type tipoExpresion  = visit(ctx.exp());

        // Permitimos INT y FLOAT como compatibles entre sí
        boolean ambosNumericos = esNumerico(tipoVariable) && esNumerico(tipoExpresion);
        if (!ambosNumericos && tipoVariable != tipoExpresion)
            throw new RuntimeException("Incompatibilidad de tipos: no se puede asignar "
                    + tipoExpresion + " a '" + target + "' de tipo " + tipoVariable + ".");
        return null;
    }

    // 3. Átomos — usa NUM_INT y NUM_FLOAT
    @Override
    public SymbolTable.Type visitAtomo(MiniLangParser.AtomoContext ctx) {
        if (ctx.NUM_INT() != null)   return SymbolTable.Type.INT;
        if (ctx.NUM_FLOAT() != null) return SymbolTable.Type.FLOAT;
        if (ctx.STRING() != null)    return SymbolTable.Type.STRING;
        if (ctx.TRUE() != null || ctx.FALSE() != null) return SymbolTable.Type.BOOLEAN;
        if (ctx.ID() != null) {
            String name = ctx.ID().getText();
            if (!symbolTable.isDeclared(name))
                throw new RuntimeException("Error semántico: variable '" + name + "' no declarada.");
            return symbolTable.getType(name);
        }
        return visit(ctx.exp());
    }

    // 4. Expresión aritmética — devuelve INT o FLOAT
    @Override
    public SymbolTable.Type visitExp_aritmetica(MiniLangParser.Exp_aritmeticaContext ctx) {
        SymbolTable.Type result = visit(ctx.termino(0));
        for (int i = 1; i < ctx.termino().size(); i++) {
            SymbolTable.Type right = visit(ctx.termino(i));
            if (!esNumerico(result) || !esNumerico(right))
                throw new RuntimeException("Error semántico: operación aritmética sobre tipo no numérico.");
            // Si alguno es FLOAT, el resultado es FLOAT
            if (result == SymbolTable.Type.FLOAT || right == SymbolTable.Type.FLOAT)
                result = SymbolTable.Type.FLOAT;
        }
        return result;
    }

    // 5. Término (multiplicación / división)
    @Override
    public SymbolTable.Type visitTermino(MiniLangParser.TerminoContext ctx) {
        SymbolTable.Type result = visit(ctx.factor(0));
        for (int i = 1; i < ctx.factor().size(); i++) {
            SymbolTable.Type right = visit(ctx.factor(i));
            if (!esNumerico(result) || !esNumerico(right))
                throw new RuntimeException("Error semántico: operación aritmética sobre tipo no numérico.");
            if (result == SymbolTable.Type.FLOAT || right == SymbolTable.Type.FLOAT)
                result = SymbolTable.Type.FLOAT;
        }
        return result;
    }

    // 6. Factor (NOT / negativo unario)
    @Override
    public SymbolTable.Type visitFactor(MiniLangParser.FactorContext ctx) {
        if (ctx.NOT() != null) {
            SymbolTable.Type t = visit(ctx.factor());
            if (t != SymbolTable.Type.BOOLEAN)
                throw new RuntimeException("Error semántico: NOT requiere un booleano.");
            return SymbolTable.Type.BOOLEAN;
        }
        if (ctx.MINUS() != null) {
            SymbolTable.Type t = visit(ctx.factor());
            if (!esNumerico(t))
                throw new RuntimeException("Error semántico: negación unaria requiere número.");
            return t;
        }
        return visit(ctx.atomo());
    }

    // 7. Expresión relacional — devuelve BOOLEAN
    @Override
    public SymbolTable.Type visitExp_relacional(MiniLangParser.Exp_relacionalContext ctx) {
        SymbolTable.Type left = visit(ctx.exp_aritmetica(0));
        if (ctx.exp_aritmetica().size() == 1) return left; // sin operador, pasa el tipo
        SymbolTable.Type right = visit(ctx.exp_aritmetica(1));
        if (!esNumerico(left) || !esNumerico(right))
            throw new RuntimeException("Error semántico: comparación entre tipos no numéricos.");
        return SymbolTable.Type.BOOLEAN;
    }

    // 8. Expresión lógica — devuelve BOOLEAN
    @Override
    public SymbolTable.Type visitExp_logica(MiniLangParser.Exp_logicaContext ctx) {
        SymbolTable.Type result = visit(ctx.exp_relacional(0));
        for (int i = 1; i < ctx.exp_relacional().size(); i++) {
            SymbolTable.Type right = visit(ctx.exp_relacional(i));
            if (result != SymbolTable.Type.BOOLEAN || right != SymbolTable.Type.BOOLEAN)
                throw new RuntimeException("Error semántico: AND/OR requieren operandos booleanos.");
            result = SymbolTable.Type.BOOLEAN;
        }
        return result;
    }

    // 9. IF — la condición debe ser booleana
    @Override
    public SymbolTable.Type visitInst_if(MiniLangParser.Inst_ifContext ctx) {
        SymbolTable.Type cond = visit(ctx.exp());
        if (cond != SymbolTable.Type.BOOLEAN)
            throw new RuntimeException("Error semántico: la condición del IF debe ser booleana.");
        ctx.procesamiento_programa().forEach(this::visit);
        return null;
    }

    // 10. SWITCH — valida que cada WHEN sea compatible con la expresión evaluada
    @Override
    public SymbolTable.Type visitInst_switch(MiniLangParser.Inst_switchContext ctx) {
        SymbolTable.Type tipoEval = visit(ctx.exp());
        for (var caso : ctx.caso_switch()) {
            SymbolTable.Type tipoCaso = visit(caso.valor());
            if (tipoEval != tipoCaso && !(esNumerico(tipoEval) && esNumerico(tipoCaso)))
                throw new RuntimeException("Error semántico: tipo del WHEN incompatible con el EVALUATE.");
            caso.procesamiento_programa().forEach(this::visit);
        }
        if (ctx.DEFAULT() != null)
            ctx.procesamiento_programa().forEach(this::visit);
        return null;
    }

    // 11. Valor literal (usado en declaraciones y WHEN)
    @Override
    public SymbolTable.Type visitValor(MiniLangParser.ValorContext ctx) {
        if (ctx.NUM_INT() != null)   return SymbolTable.Type.INT;
        if (ctx.NUM_FLOAT() != null) return SymbolTable.Type.FLOAT;
        if (ctx.STRING() != null)    return SymbolTable.Type.STRING;
        if (ctx.TRUE() != null || ctx.FALSE() != null) return SymbolTable.Type.BOOLEAN;
        return null;
    }

    // ---------- Helpers ----------
    private boolean esNumerico(SymbolTable.Type t) {
        return t == SymbolTable.Type.INT || t == SymbolTable.Type.FLOAT;
    }

    private SymbolTable.Type resolveType(MiniLangParser.TipoContext ctx) {
        return switch (ctx.getText()) {
            case "I"       -> SymbolTable.Type.INT;
            case "V"       -> SymbolTable.Type.FLOAT;
            case "X"       -> SymbolTable.Type.STRING;
            case "BOOLEAN" -> SymbolTable.Type.BOOLEAN;
            default -> throw new RuntimeException("Tipo desconocido: " + ctx.getText());
        };
    }
}