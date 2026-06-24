import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        String file = args.length > 0 ? args[0] : "01-basico.txt";
        CharStream input = CharStreams.fromFileName(file);

        MiniLangLexer  lexer  = new MiniLangLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MiniLangParser parser = new MiniLangParser(tokens);

        /*tokens.fill();
        for (Token token : tokens.getTokens()) {
            System.out.println("Token: " + token.getType() 
                + " | Texto: '" + token.getText() 
                + "' | Línea: " + token.getLine());
        }*/
        
        // Errores de sintaxis
        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?,?> r, Object sym,
                                    int line, int col,
                                    String msg, RecognitionException e) {
                System.out.println("\n[ERROR SINTÁCTICO] En la línea " + line + ", columna " + col + ": " + msg);
                System.out.println("No se puede continuar con la ejecución.");
                System.exit(1); // Cierra el programa de forma limpia sin romper la consola
            }
        });

        ParseTree tree = parser.programa();

        /// 1) Análisis semántico
        try {
            new SemanticAnalyzer().visit(tree);
        } catch (RuntimeException e) {
            System.out.println("\n[ERROR SEMÁNTICO] " + e.getMessage());
            System.out.println("Ejecución cancelada por fallas semánticas.");
            System.exit(1);
        }

     // 2) Ejecución
        try {
            System.out.println("--- Iniciando Ejecución del Intérprete ---");
            new Interpreter().visit(tree);
            System.out.println("--- Fin de la Ejecución ---");
        } catch (RuntimeException e) {
            System.out.println("\n[ERROR DE EJECUCIÓN] " + e.getMessage());
            System.exit(1);
        }
    }
}