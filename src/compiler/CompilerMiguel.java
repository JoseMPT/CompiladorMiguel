package compiler;

import jflex.exceptions.SilentExit;

/**
 *
 * @author Jose Miguel Paniagua Tinajero | 077DB
 */
public class CompilerMiguel {
    public static void main(String[] args) {
        String lexerFile = System.getProperty("user.dir") + "/src/compiler/Lexer.flex";
        String lexerFileColor = System.getProperty("user.dir") + "/src/compiler/LexerColor.flex";
        try {
            jflex.Main.generate(new String[] {lexerFile, lexerFileColor});
        }catch (SilentExit exception){
            System.out.println("Error al generar el archivo Flex");
        }
    }
}
