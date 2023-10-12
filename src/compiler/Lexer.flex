package compiler;
import compilerTools.Token;

%%
%class Lexer
%public
%type Token
%line
%column
%{
    private Token token(String lexeme, String lexicalComp, int line, int column){
    return new Token(lexeme, lexicalComp, line+1, column+1);
    }
%}
/*Variables basicas de comentarios y espacios*/
TerminadorDeLinea = \r|\n|\r\n
EntradaDeCaracter = [^\r\n]
EspaciosEnBlanco = {TerminadorDeLinea} | [\t\f]
ComentarioTradicional = "/*" [^*] ~"*/" | "/*" "*"+ "/"
FinDeLineaComentario = "//" {EntradaDeCaracter}* {TerminadorDeLinea}?
ContenidoComentario = ([^*] | \*+ [^/*])*
ComentarioDeDocumentacion = "/**" {ContenidoComentario} "*"+ "/"

/* Cometario */
Comentario = {ComentarioTradicional} | {FinDeLineaComentario} | {ComentarioDeDocumentacion}

/* Identificador */
Letra = [A-Za-zÑñ_ÁÉÍÓÚáéíóúÜü]
Digito = [0-9]
Identificador = {Letra} ({Letra}|{Digito})*

/* Numero */
Numero = 0 | [1-9] [0-9]*
%%

/* Comentarios o espacios en blanco */
{Comentario}|{EspaciosEnBlanco} { /*Ignorar*/ }

/* identificador */
\${Identificador} { return token (yytext(), "IDENTIFICADOR", yyline, yycolumn);}

/* Tipos de datos */
numero |
color {return token(yytext(), "TIPO_DATO", yyline, yycolumn);}

/* Numeros */
{Numero} {return token(yytext(), "NUMERO", yyline, yycolumn);}

/* Colores */
#[{Letra}|{Digito}]{6} { return token(yytext(), "COLOR", yyline, yycolumn);}

/* Operadores de agrupacion */
"(" {return token(yytext(), "PARENTESIS_A", yyline, yycolumn); }
")" {return token(yytext(), "PARENTESIS_C", yyline, yycolumn); }
"{" {return token(yytext(), "LLAVE_A", yyline, yycolumn); }
"}" {return token(yytext(), "LLAVE_C", yyline, yycolumn); }

/* Signos de puntuacion */
"," { return token(yytext(), "COMA", yyline, yycolumn); }
";" { return token(yytext(), "PUNTOYCOMA", yyline, yycolumn); } 

/* Operador de asignacion */
--> |
= { return token(yytext(), "OPDEASIGNACION", yyline, yycolumn); }

/* Movimiento */
adelante |
atras |
izquierda |
derecha |
norte |
sur |
este |
oeste { return token(yytext(), "MOVIMIENTO", yyline, yycolumn); }

/* Palabras Reservadas */
While |
Do |
For |
Else |
If |
Int { return token(yytext(), "PALABRA_RESERVADA", yyline, yycolumn); }

/* Pintar */
pintar { return token(yytext(), "PINTAR", yyline, yycolumn); }

/* Detener Pintar */
detenerPintar { return token(yytext(), "DETENER_PINTAR", yyline, yycolumn); }

/* Tomar */
tomar |
poner { return token(yytext(), "TOMAR", yyline, yycolumn); }

/* Lanzar Moneda */
lanzarMoneda { return token(yytext(), "LANZARMONEDA", yyline, yycolumn); }

/* Repetir */
repetir  |
repetirMientras { return token(yytext(), "REPETIR", yyline, yycolumn); }

/* Detener Repetir */
interrumpir { return token(yytext(), "DETENER_REPETIR", yyline, yycolumn); }

/* Estructura SI */
si  |
sino { return token(yytext(), "ESTRUCTURA_SI", yyline, yycolumn); }

/* Operadores logicos */
"&" |
"|" { return token(yytext(), "OPERADOR_LOGICO", yyline, yycolumn); }

/* Final */
final { return token(yytext(), "FINAL", yyline, yycolumn); }

/* Numero Erroneo */
0{Numero} { return token(yytext(), "ERROR_1", yyline, yycolumn); }

/* Identificador Erroneo */
{Identificador} { return token(yytext(), "ERROR_2", yyline, yycolumn); }

. { return token(yytext(), "ERROR", yyline, yycolumn); } 