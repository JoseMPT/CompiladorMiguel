package compiler;

import compilerTools.TextColor;
import java.awt.Color;

%%
%class LexerColor
%public
%type TextColor
%char
%{
    private TextColor textColor(long start, int size, Color color){
    return new TextColor((int) start, size, color);
    }
%}
/*Variables basicas de comentarios y espacios*/
TerminadorDeLinea = \r|\n|r\n
EntradaDeCaracter = [^\r\n]
EspacioEnBlanco = {TerminadorDeLinea} | [ \t\f] | " "
ComentarioTradicional = "/*" [^*] ~"*/" | "/*" "*"+ "/"
FinDeLineaComentario = "//" {EntradaDeCaracter}* {TerminadorDeLinea}?
ContenidoComentario = ( [^*] | \*+ [^/*] )*
ComentarioDeDocumentacion = "/**" {ContenidoComentario} "*"+ "/"

/* Comentario */ 
Comentario = {ComentarioTradicional} | {FinDeLineaComentario} | {ComentarioDeDocumentacion}

/* Identificador */ 
Letra = [A-Za-zÑñ_ÁÉÍÓÚáéíóú]
Digito = [0-9]
Identificador = {Letra}({Letra}|{Digito})*

/* Numero */
Numero = 0 | [1-9][0-9]*
%%

/* Comentarios o espacios en blanco */
{Comentario} { return textColor(yychar, yylength(), new Color(146, 146, 146)); }
{EspacioEnBlanco} { /*Ignorar*/ }

/* identificador */
\${Identificador} { /*Ignorar*/ }

/* tipo de dato */
numero |
color {return textColor(yychar, yylength(), Color.red); }

/* Colores */
%[{Letra}|{Digito}]{6} { return textColor(yychar, yylength(), new Color (0, 255, 127)); }

/* Numeros */
{Numero} {return textColor(yychar, yylength(), new Color (251, 140, 0)); }

/* Operadores de agrupacion */
"(" | ")" { return textColor (yychar, yylength(), Color.red); }

/* Signos de puntuacion */
"," | ";" { return textColor(yychar, yylength(), new Color (0 , 0, 0)); }

/* Operador de asignacion */
--> { return textColor(yychar, yylength(), new Color (255, 215, 0)); }

/* Palabras Reservadas */
While |
Do |
For |
Else |
If { return textColor(yychar, yylength(), new Color (128, 0, 12)); }

/* Pintar */
pintar { return textColor(yychar, yylength(), new Color (255, 64, 129)); }

/* Detener Pintar */ 
detenerPintar { return textColor(yychar, yylength(), new Color (255, 64, 129)); }

/* Repetir */
repetir |
repetirMientras { return textColor(yychar, yylength(), new Color (121, 107, 255)); } 

/* Detener Repetir */
interrumpir { return textColor(yychar, yylength(), new Color (255, 64, 129)); } 

/* Estructura SI */
si |
sino { return textColor(yychar, yylength(), new Color (48, 63, 129)); }

/* Operador Logicos */
"&" |
"|" { return textColor(yychar, yylength(), new Color (112, 128, 144)); }  

/* FINAL */
final { return textColor(yychar, yylength(), new Color (198, 40, 40)); } 

/* Numero Erroneo */
0{Numero} { /* Ignorar */ }

/* Identificador */
{Identificador} { /* Ignorar */ }

. { /* Ignorar */ }

