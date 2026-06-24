grammar MiniLang;

// ###Gramática libre de contexto###
// Estructura básica del programa
programa: PROGRAM ID PUNTO
	DIVISION DATA PUNTO
	declaracion_variables*

	DIVISION PROCEDURE PUNTO
	procesamiento_programa*
	EXIT_PROGRAM PUNTO;

// Sección de declaración de variables
declaracion_variables: PIC tipo ID ('=' valor)? PUNTO;
tipo: TIPO_NUMERICO | TIPO_STRING | TIPO_FLOTANTE | TIPO_BOOLEAN;
valor: NUM_INT | NUM_FLOAT | STRING | TRUE | FALSE;

// Sección de instrucciones del programa
procesamiento_programa: inst_asignacion | inst_salida | inst_if | inst_switch;

inst_asignacion: MOVE PAREN_OPEN exp PAREN_CLOSE TO ID PUNTO;

inst_salida: DISPLAY exp PUNTO;

inst_if:
	IF exp THEN 
		procesamiento_programa* (ELSE 
		procesamiento_programa*
	)? END_IF PUNTO;

exp: exp_logica;

exp_logica: exp_relacional ((AND | OR) exp_relacional)*;
exp_relacional:exp_aritmetica ((EQ | NEQ | LT | LEQ | GT | GEQ) exp_aritmetica)?;
exp_aritmetica: termino ((SUM | MINUS) termino)*;
termino: factor ((MULT | DIV) factor)*;
factor: NOT factor | MINUS factor | atomo;
atomo: NUM_INT | NUM_FLOAT | STRING | TRUE | FALSE | ID | PAREN_OPEN exp PAREN_CLOSE;


inst_switch:
    EVALUATE exp
        caso_switch+
        (DEFAULT PUNTO procesamiento_programa*)?
    END_EVALUATE PUNTO;

caso_switch: WHEN valor PUNTO procesamiento_programa* (BREAK PUNTO)?;

// ###Gramática regular###
PROGRAM: 'PROGRAM';
DIVISION: 'DIVISION';
DATA: 'DATA';
PROCEDURE: 'PROCEDURE';
EXIT_PROGRAM: 'STOP RUN';
PUNTO: '.';

// Declaración de variables
PIC: 'PIC';
TIPO_NUMERICO: 'I';
TIPO_STRING: 'X';
TIPO_FLOTANTE: 'V';
TIPO_BOOLEAN: 'BOOLEAN';

// Asignación de variables
MOVE: 'MOVE';
TO: 'TO';

// Expresiones
SUM: '+';
MINUS: '-';
MULT: '*';
DIV: '/';
MOD: '%';

NOT: 'NOT';
OR: 'OR';
AND: 'AND';

EQ: '=';
NEQ: '<>';
GT: '>';
LT: '<';
GEQ: '>=';
LEQ: '<=';

PAREN_OPEN: '(';
PAREN_CLOSE: ')';

// Salida por consola
DISPLAY: 'DISPLAY';

// estructura if
IF: 'IF';
THEN: 'THEN';
ELSE: 'ELSE';
END_IF: 'END-IF';

// estructura switch (Variante 5)
EVALUATE: 'EVALUATE';
WHEN: 'WHEN';
DEFAULT: 'WHEN OTHER';
END_EVALUATE: 'END-EVALUATE';
BREAK: 'STOP CASE';

// Identificadores
FALSE: 'FALSE';
TRUE: 'TRUE';
STRING: '"' (~["])* '"';
NUM_FLOAT: [0-9]+ '.' [0-9]+;
NUM_INT:   [0-9]+;
ID: [a-zA-Z][a-zA-Z0-9-]*;

// Comentarios (Corregido para soportar múltiples caracteres)
COMMENT_LINE: {getCharPositionInLine() == 0}? '*' ~[\r\n]* -> skip;
COMMENT_BLOCK: '*>' .*? '<*' -> skip;

// Espacios en blanco
WS: [ \t\r\n]+ -> skip;