public class Interpreter extends MiniLangBaseVisitor<Object> {

    private final SymbolTable symbolTable = new SymbolTable();

    // ---------- Declaración ----------
    @Override
    public Object visitDeclaracion_variables(MiniLangParser.Declaracion_variablesContext ctx) {
        String name = ctx.ID().getText();
        SymbolTable.Type type = resolveType(ctx.tipo());
        Object initVal = ctx.valor() != null ? visit(ctx.valor()) : null;
        symbolTable.declare(name, type, initVal);
        return null;
    }
    
 // ---------- Valor (Asignación en la declaración) ----------
    @Override
    public Object visitValor(MiniLangParser.ValorContext ctx) {
        if (ctx.NUM_INT() != null)
            return Integer.parseInt(ctx.NUM_INT().getText());
        if (ctx.NUM_FLOAT() != null)
            return Double.parseDouble(ctx.NUM_FLOAT().getText());
        if (ctx.STRING() != null) {
            String texto = ctx.STRING().getText();
            return texto.substring(1, texto.length() - 1);
        }
        if (ctx.TRUE() != null)  return true;
        if (ctx.FALSE() != null) return false;
        return null;
    }

    // ---------- Asignación ----------
    @Override
    public Object visitInst_asignacion(MiniLangParser.Inst_asignacionContext ctx) {
        Object val = visit(ctx.exp());
        symbolTable.assign(ctx.ID().getText(), val);
        return null;
    }

    // ---------- Salida ----------
    @Override
    public Object visitInst_salida(MiniLangParser.Inst_salidaContext ctx) {
        Object val = visit(ctx.exp());
        System.out.println(val);
        return null;
    }

    // ---------- If ----------
 // ---------- If ----------
    @Override
    public Object visitInst_if(MiniLangParser.Inst_ifContext ctx) {
        Boolean cond = (Boolean) visit(ctx.exp());
        boolean inElse = false;

        // Recorremos todos los nodos "hijos" del IF uno por uno
        for (int i = 0; i < ctx.getChildCount(); i++) {
            var child = ctx.getChild(i);
            
            // Si leemos la palabra ELSE, prendemos la bandera para saber que cambiamos de rama
            if (child.getText().equals("ELSE")) {
                inElse = true;
                continue;
            }
            
            // Si el hijo es una instrucción real (y no una palabra como THEN, END-IF, o el punto)
            if (child instanceof MiniLangParser.Procesamiento_programaContext) {
                // Visitamos (ejecutamos) la instrucción solo si la condición coincide con la rama en la que estamos
                if (cond && !inElse) {
                    visit(child);
                } else if (!cond && inElse) {
                    visit(child);
                }
            }
        }
        return null;
    }

    // ---------- SWITCH ----------
    @Override
    public Object visitInst_switch(MiniLangParser.Inst_switchContext ctx) {
        Object evaluated = visit(ctx.exp());
        boolean matched = false;

        for (var caso : ctx.caso_switch()) {
            Object caseVal = visit(caso.valor());
            if (valoresIguales(evaluated, caseVal)) {
                matched = true;
                for (var inst : caso.procesamiento_programa()) {
                    visit(inst);
                }
                // Si tiene BREAK, salir del switch
                if (caso.BREAK() != null) break;
            }
        }

        // DEFAULT
        if (!matched && ctx.DEFAULT() != null) {
            for (var inst : ctx.procesamiento_programa()) {
                visit(inst);
            }
        }
        return null;
    }

    private boolean valoresIguales(Object a, Object b) {
        if (a == null || b == null) return false;
        return a.toString().equals(b.toString());
    }

    // ---------- Expresiones ----------
    @Override
    public Object visitExp_aritmetica(MiniLangParser.Exp_aritmeticaContext ctx) {
        Object result = visit(ctx.termino(0));
        for (int i = 1; i < ctx.termino().size(); i++) {
            Object right = visit(ctx.termino(i));
            String op = ctx.getChild(2 * i - 1).getText();
            result = switch (op) {
                case "+" -> toDouble(result) + toDouble(right);
                case "-" -> toDouble(result) - toDouble(right);
                default  -> throw new RuntimeException("Operador desconocido: " + op);
            };
        }
        return result;
    }

    @Override
    public Object visitTermino(MiniLangParser.TerminoContext ctx) {
        Object result = visit(ctx.factor(0));
        for (int i = 1; i < ctx.factor().size(); i++) {
            Object right = visit(ctx.factor(i));
            String op = ctx.getChild(2 * i - 1).getText();
            if (op.equals("/") && toDouble(right) == 0)
                throw new RuntimeException("Error semántico: división por cero.");
            result = switch (op) {
                case "*" -> toDouble(result) * toDouble(right);
                case "/" -> toDouble(result) / toDouble(right);
                default  -> throw new RuntimeException("Operador desconocido: " + op);
            };
        }
        return result;
    }

    @Override
    public Object visitAtomo(MiniLangParser.AtomoContext ctx) {
        if (ctx.NUM_INT() != null)
            return Integer.parseInt(ctx.NUM_INT().getText());
        if (ctx.NUM_FLOAT() != null)
            return Double.parseDouble(ctx.NUM_FLOAT().getText());
        if (ctx.STRING() != null) {
            String texto = ctx.STRING().getText();
            return texto.substring(1, texto.length() - 1);
        }
        if (ctx.TRUE() != null)  return true;
        if (ctx.FALSE() != null) return false;
        if (ctx.ID() != null)
            return symbolTable.getValue(ctx.ID().getText());
        return visit(ctx.exp());
    }
    
 // ---------- Expresiones Lógicas (AND / OR) ----------
    @Override
    public Object visitExp_logica(MiniLangParser.Exp_logicaContext ctx) {
        // Se evalúa la primera expresión
        Object result = visit(ctx.exp_relacional(0));

        // Si hay operadores encadenados (ej: exp AND exp OR exp)
        for (int i = 1; i < ctx.exp_relacional().size(); i++) {
            Object right = visit(ctx.exp_relacional(i));
            String op = ctx.getChild(2 * i - 1).getText();

            // Las operaciones relacionales siempre devuelven booleanos
            boolean l = (Boolean) result;
            boolean r = (Boolean) right;

            result = switch (op) {
                case "AND" -> l && r;
                case "OR"  -> l || r;
                default -> throw new RuntimeException("Operador lógico desconocido: " + op);
            };
        }
        return result;
    }

    // ---------- Expresiones Relacionales (>, <, =, <>) ----------
    @Override
    public Object visitExp_relacional(MiniLangParser.Exp_relacionalContext ctx) {
        // Evaluamos el lado izquierdo
        Object left = visit(ctx.exp_aritmetica(0));

        // Si no hay comparación (es solo un número o variable suelta), devolvemos el valor directo
        if (ctx.exp_aritmetica().size() == 1) {
            return left;
        }

        // Si hay un operador, evaluamos el lado derecho
        Object right = visit(ctx.exp_aritmetica(1));
        String op = ctx.getChild(1).getText(); // El operador siempre está en el medio

        double l = toDouble(left);
        double r = toDouble(right);

        return switch (op) {
            case "="  -> l == r;
            case "<>" -> l != r;
            case ">"  -> l > r;
            case "<"  -> l < r;
            case ">=" -> l >= r;
            case "<=" -> l <= r;
            default -> throw new RuntimeException("Operador relacional desconocido: " + op);
        };
    }
    
    // ... visitAtomo, visitExp_relacional, visitExp_logica, etc.

    private double toDouble(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof String s) return Double.parseDouble(s);
        throw new RuntimeException("No es número: " + o);
    }

    private SymbolTable.Type resolveType(MiniLangParser.TipoContext ctx) { String t = ctx.getText();
	    return switch (t) {
	    case "I"       -> SymbolTable.Type.INT;
	    case "V"       -> SymbolTable.Type.FLOAT;
	    case "X"       -> SymbolTable.Type.STRING;
	    case "BOOLEAN" -> SymbolTable.Type.BOOLEAN;
	    default -> throw new RuntimeException("Tipo desconocido: " + t);
    	}; 
    }
}