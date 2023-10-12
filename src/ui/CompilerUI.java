package ui;

import compiler.Lexer;
import compiler.LexerColor;
import compilerTools.CodeBlock;
import compilerTools.Directory;
import compilerTools.ErrorLSSL;
import compilerTools.Functions;
import compilerTools.Grammar;
import compilerTools.Production;
import compilerTools.TextColor;
import compilerTools.Token;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JOptionPane;
import javax.swing.Timer;

/**
 *
 * @author Jose Miguel Paniagua Tinajero | 077DB
 */
public class CompilerUI extends javax.swing.JFrame {
    private String title; //Variable para el Titulo
    private Directory directorio; //Directorio
    private ArrayList<Token> tokens; //ArrayList para guardar los Tokens
    private ArrayList<ErrorLSSL> errors; //ArrayList donde se guardaran los errores
    private ArrayList<TextColor> textsColor; //Se guardaran los colores de las palabras reservadas
    private Timer timerKeyReleased; //El uso de esta variable se utilizara para que se coloreen  las palabras
    private ArrayList<Production> identProd; //extra los identificadores del analizador sintatico
    private HashMap<String, String> identificadores;
    private boolean codeHasBeenCompiled = false;

    /**
     * Creates new form CompilerUI
     */
    public CompilerUI() {
        initComponents();
        init();
    }
    
    private void init(){
        title = "Compilador"; //Se iniciara la variable con el nombre del compilador
        setLocationRelativeTo(null); //Se ejecutara el metodo SetLocation para centrar la ventana
        setTitle(title); //Se ejecuta para ponerle titulo a la ventana
        directorio = new Directory(this, textPaneCode, title, ".comp");
        
        addWindowListener(new WindowAdapter(){ //Cuando presiona "X" de la esquina superior derecha para cerrar la ventana
            @Override
            public void windowClosing (WindowEvent e){ //Se sobreescribe el metodo
                directorio.Exit();// Si estamos editando codigo nos preguntara si deseamos guardar o descargar el archivo
                System.exit(0);// indica que no hubo ningun error
            }
        });
        Functions.setLineNumberOnJTextComponent(textPaneCode);/* Usaremos un metodo de la clase function de la libreria
        compiler tools donde se pasa el parametro el JTXT PANE y esto permitira que se muestre los numeros de linea*/
        timerKeyReleased = new Timer((int) (1000*0.3), (ActionEvent e) -> { //inicializar el timer para colorear las palanras del codigo
                timerKeyReleased.stop();
                colorAnalysis(); //metodo color analysis sin ninguna instruccion
                });

        Functions.insertAsteriskInName(this, textPaneCode, () -> {
            timerKeyReleased.restart();
        });

        tokens = new ArrayList<>(); // se inicializar cada uno de los arraylist vacios
        errors = new ArrayList<>();
        textsColor = new ArrayList<>();
        identProd = new ArrayList<>();
        identificadores = new HashMap<>();
        Functions.setAutocompleterJTextComponent(new String[]{"Color", "Miguel", "Número"}, textPaneCode, () -> {
        timerKeyReleased.restart();
        });
    }
    
    private void compile(){
        clearFields();
        lexicalAnalysis();
        fillTableTokens();
        syntaticAnalysis();
        semanticAnalysis();
        printConsole();
        codeHasBeenCompiled = true;
    }
    
    private void clearFields() {
        Functions.clearDataInTable(tablaTokens);
        textAreaResult.setText("");
        tokens.clear();
        errors.clear();
        identProd.clear();
        identificadores.clear();
        codeHasBeenCompiled = false;
    }
    
    private void lexicalAnalysis() {
        Lexer lexer;
        try{
            File codigo = new File ("code.encrypter");
            FileOutputStream output = new FileOutputStream(codigo);
            byte[] bytesText = textPaneCode.getText().getBytes();
            output.write(bytesText);
            BufferedReader entrada = new BufferedReader (new InputStreamReader (new FileInputStream (codigo),"UTF8"));
            lexer = new Lexer(entrada);
            while(true){
                Token token =lexer.yylex();
                if (token == null){
                    break;
                }
                tokens.add(token);
            }
        }catch (FileNotFoundException ex){
            System.out.println("Archivo no encotrado..." + ex.getMessage());
        }catch (IOException ex){
            System.out.println("Error al escribir el archivo..." + ex.getMessage());
        }
    }
    
    private void fillTableTokens() {
        tokens.forEach(token -> {
            Object[] data = new Object[]{token.getLexicalComp(), token.getLexeme(),
                "[" + token.getLine() + ", " + token.getColumn() + "]"};
            Functions.addRowDataInTable(tablaTokens, data);
        });
    }
    
    private void syntaticAnalysis() {
        Grammar gramatica = new Grammar(tokens, errors);
        
        /* Eliminación de errores */
        gramatica.delete(new String[]{"ERROR", "ERROR_1", "ERROR_2"}, 1);
        gramatica.group("VALOR", "NUMERO | COLOR");
        
        gramatica.group("VARIABLE", "TIPO_DATO IDENTIFICADOR OPDEASIGNASION VALOR", true, identProd);
        gramatica.group("VARIABLE", "IDENTIFICADOR OPDEASIGNACION VALOR", true, 
                2, "Error Sintatico {} Falta el tipo de dato en la variable [#,%]");
        gramatica.finalLineColumn();
        
        gramatica.group("VARIABLE", "TIPO_DATO OPDEASIGNACION VALOR", true,
                3, "Error Sintatico {} Falta el identificador en la variable [#,%]");
        gramatica.finalLineColumn();

        gramatica.group("VARIABLE", "TIPO_DATO IDENTIFICADOR VALOR", true,
                4, "Error Sintatico {} Falta el operador de asignación en la variable [#,%]");
        gramatica.finalLineColumn();

        gramatica.group("VARIABLE", "TIPO_DATO IDENTIFICADOR OPDEASIGNACION", true,
                5, "Error Sintatico {} Falta el valor en la variable [#,%]");
        gramatica.initialLineColumn();
        gramatica.finalLineColumn();

        /* Eliminación de tipos de datos y operadores de asignación */
        gramatica.delete("TIPO_DATO", 7,
                    "Error Sintatico {} : El tipo de dato no está en una declaracion [#,%]");
        gramatica.delete("OPDEASIGNACION", 8,
                    "Error Sintatico {} : El operador de asignación no está en una declaracion [#,%]");

        /* Agrupar identificadores y definición de parámetros */
        gramatica.group("PARAMETROS", "VALOR (COMA VALOR)+");
        gramatica.group("FUNCION", "PALABRA_RESERVADA | PINTAR | DETENER_PINTAR | REPETIR | DETENER_REPETIR | ESTRUCTURA_SI", true);
        gramatica.group("FUNCION_COMP", "FUNCION_PARENTESIS_A (VALOR | PARAMETROS)? PARENTESIS_C", true);
        gramatica.group("FUNCION_COMP", "FUNCION (VALOR | PARAMETROS)? PARENTESIS_C", true, 
                9,"Error Sintatico {} : Falta el paréntesis que abre la funcion[#,%]");

        gramatica.finalLineColumn();
        gramatica.group("FUNCION_COMP", "FUNCION PARENTESIS_A (VALOR | PARAMETROS)?", true,
                10, "Error Sintatico {} : Falta el paréntesis de cierre de la funcion [#,%]");
        gramatica.finalLineColumn();

        /* Eliminación de funciones incompletas */ 
        gramatica.delete("FUNCION", 11,
                "Error Sintatico {} : La función no está declarada correctamente [#,%]");
        gramatica.loopForFunExecUntilChangeNotDetected(()-> {
            gramatica.group("EXP_LOGICA", "(FUNCION_COMP | EXP_LOGICA) (OPERADOR_LOGICO(FUNCION_COMP | EXP_LOGICA))+");
            gramatica.group("EXP_LOGICA", "PARENTESIS_A (EXP_LOGICA | FUNCION_COMP) PARENTESIS_C");
        });

        /* Eliminación de un operador lógico */
        gramatica.delete("OPERADOR_LOGICO", 12,
                "Error Sintatico {} : El operador lógico no  está contenido en una expresión");

        /*Agrupación de expresiones lógicas */
        gramatica.group("VALOR",  "EXP_LOGICA");
        gramatica.group("PARAMETROS", "VALOR (COMA VALOR)+");

        /* Agrupación de estructuras de control */
        gramatica.group("EST_CONTROL", "REPETIR | ESTRUCTURA_SI");
        gramatica.group("EST_CONTROL_COMP", "EST_CONTROL PARENTESIS_A PARENTESIS_C");
        gramatica.group("EST_CONTROL_COMP", "EST_CONTROL (VALOR | PARAMETROS)");
        gramatica.group("EST_CONTROL_COMP", "EST_CONTROL PARENTESIS_A | (VALOR | PARAMETROS) PARENTESIS_C");

        /* Eliminación de estructuras de control incompletas */
        gramatica.delete("EST_CONTROL", 13,
                "Error Sintatico {} : La estructura de control no está declarada correctamente [#,%]");

        /* Eliminación de paréntesis */
        gramatica.delete(new String [] {"PARENTESIS_A", "PARENTESIS_C"},
                14, "Error Sintatico {} : El paréntesis [] no está declarado correctamente [#,%]");
        gramatica.finalLineColumn();

        /* Verificación de punto y coma */
        gramatica.group("VARIABLE_PC", "VARIABLE PUNTOYCOMA");
        gramatica.group("VARIABLE_PC", "VARIABLE", true,
                15, "Error Sintatico {} : Falta el punto y coma [] al final de la variable [#,%]");

        /* Funciones */
        gramatica.group("FUNCION_COMP_PC", "FUNCION_COMP PUNTOYCOMA");
        gramatica.group("FUNCION_COMP_PC", "FUNCION_COMP",
                16, "Error Sintatico {} : Falta el punto y coma [] de la declaracion de la función");

        /* Eliminar punto y coma */
        gramatica.delete("PUNTOYCOMA",
                17, "Errir Sintatico {} : El punto y coma no esta al final de una sentencia");

        /* Sentencias */
        gramatica.group("SENTENCIAS", "(VARIABLE_PC | FUNCION_COMP_PC)+");
        gramatica.loopForFunExecUntilChangeNotDetected(()-> {
            gramatica.group("EST_CONTROL_COMP_LASLC",
                    "EST_CONTROL_COMP LLAVE_A (SENTENCIAS)? LLAVE_C", true);
            gramatica.group("SENTENCIAS", ("(SENTENCIAS | EST_CONTROL_COMP_LASLC)+"));
        });

        /*Estructura de una función incompleta*/
        gramatica.loopForFunExecUntilChangeNotDetected(()-> {
            gramatica.initialLineColumn();
            gramatica.group("EST_CONTROL_COMP_LASLC", "EST_CONTROL_COMP (SENTENCIAS)? LLAVE_C", true,
                    18, "Error Sintatico {} : Falta la llave que abre en la estructura de control [#,%]");
            gramatica.finalLineColumn();
            gramatica.group("EST_CONTROL_COMP", "EST_CONTROL_COMP LLAVE_A (SENTENCIAS)?", true,
                    19,"Error Sintatico {} : Falta la llave que cierra en la estructura de control [#,%]");
            gramatica.group("SENTENCIAS", "(SENTENCIAS | EST_CONTROL_COMP_LASLC)");
        });

        gramatica.delete(new String[] {"LLAVE_A", "LLAVE_C"}, 20,
                "Error Sintatico {} la llave [] no esta contenida en una agrupacion [#,%]");
        gramatica.show();
    }
    
    private void semanticAnalysis() {
        HashMap<String, String> identDataType = new HashMap<>();
        identDataType.put("color", "COLOR");
        identDataType.put("numero", "NUMERO");
        for (Production id: identProd){
            if (!identDataType.get(id.lexemeRank(0)).equals(id.lexicalCompRank(-1))) {
                errors.add(new ErrorLSSL(1, "Error Semantico {} : Valor No Compatible con el tipo de dato[#, %]", id, true));
            }else if(id.lexicalCompRank(-1).equals("COLOR") && !id.lexemeRank(-1).matches("[0-9a-fA-F]+")){
                errors.add(new ErrorLSSL(1, "Error Semantico {} : El Color no es un numero Hexadecimal [#, %]", id, false));
            }else{
                identificadores.put(id.lexemeRank(1), id.lexemeRank(-1));
            }
        }
    }
    
    private void printConsole() {
        int sizeErrors = errors.size();
        if(sizeErrors > 0){
            Functions.sortErrorsByLineAndColumn(errors);
            String strErrors = "\n";
            for (ErrorLSSL error: errors) {
                String strERROR = String.valueOf(error);
                strErrors += strERROR + "\n";
            }
            textAreaResult.setText("Compilación terminada... \n" + strErrors + "\n La compilación terminó con errores...");
        } else {
            textAreaResult.setText("¡Compilación exitosa!");
        }
        textAreaResult.setCaretPosition(0);
    }
    
    private void colorAnalysis(){
        textsColor.clear();
        LexerColor lexerColor;
        try{
            File codigo = new File ("color.encrypter");
            FileOutputStream output = new FileOutputStream(codigo);
            byte[] bytesText = textPaneCode.getText().getBytes();
            output.write(bytesText);
            BufferedReader entrada = new BufferedReader (new InputStreamReader(new FileInputStream(codigo), "UTF8"));
            lexerColor = new LexerColor(entrada);

            while (true){
                TextColor textColor = lexerColor.yylex();
                if(textColor == null){
                    break;
                }
                textsColor.add(textColor);
            }
        }catch (FileNotFoundException ex){
            System.out.println("Archivo no encontrado..." + ex.getMessage());
        }catch (IOException ex){
            System.out.println("Error al escribir en el archivo..." + ex.getMessage());
        }
        Functions.colorTextPane(textsColor, textPaneCode, new Color(40, 40, 40));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        btnOpen = new javax.swing.JButton();
        btnSave = new javax.swing.JButton();
        btnSaveAs = new javax.swing.JButton();
        btnNew = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        btnCompiler = new javax.swing.JButton();
        btnExecute = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        textPaneCode = new javax.swing.JTextPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        textAreaResult = new javax.swing.JTextArea();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tablaTokens = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(255, 255, 255));

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        btnOpen.setText("Abrir");
        btnOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenActionPerformed(evt);
            }
        });

        btnSave.setText("Guardar");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        btnSaveAs.setText("Guardar como");
        btnSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveAsActionPerformed(evt);
            }
        });

        btnNew.setText("Nuevo");
        btnNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(18, Short.MAX_VALUE)
                .addComponent(btnNew)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnOpen)
                .addGap(13, 13, 13)
                .addComponent(btnSave)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnSaveAs)
                .addGap(24, 24, 24))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnOpen)
                    .addComponent(btnSave)
                    .addComponent(btnSaveAs)
                    .addComponent(btnNew))
                .addContainerGap(26, Short.MAX_VALUE))
        );

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        btnCompiler.setText("Compilar");
        btnCompiler.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCompilerActionPerformed(evt);
            }
        });

        btnExecute.setText("Ejecutar");
        btnExecute.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExecuteActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addComponent(btnCompiler)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnExecute)
                .addContainerGap(28, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCompiler)
                    .addComponent(btnExecute))
                .addContainerGap(27, Short.MAX_VALUE))
        );

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Compilador: Jose Miguel Paniagua Tinajero");

        jScrollPane1.setViewportView(textPaneCode);

        textAreaResult.setColumns(20);
        textAreaResult.setRows(5);
        jScrollPane2.setViewportView(textAreaResult);

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Resultado");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Tabla de símbolos");

        tablaTokens.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Componente léxico", "Lexema", "Línea - Columna"
            }
        ));
        jScrollPane3.setViewportView(tablaTokens);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jScrollPane1)
                                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 685, Short.MAX_VALUE))
                                .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(348, 348, 348)
                        .addComponent(jLabel2)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(42, 42, 42)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 551, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(26, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel3)
                        .addGap(248, 248, 248))))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(454, 454, 454))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(jLabel1)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 380, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel2)
                                .addGap(27, 27, 27)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 542, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(34, 34, 34)
                        .addComponent(jLabel3)))
                .addContainerGap(47, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewActionPerformed
        // TODO add your handling code here:
        directorio.New();
        clearFields();
    }//GEN-LAST:event_btnNewActionPerformed

    private void btnOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenActionPerformed
        // TODO add your handling code here:
        if (directorio.Open()) {
            colorAnalysis();
            clearFields();
        }
    }//GEN-LAST:event_btnOpenActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        // TODO add your handling code here:
        if (directorio.Save()) {
            clearFields();
        }
    }//GEN-LAST:event_btnSaveActionPerformed

    private void btnSaveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveAsActionPerformed
        // TODO add your handling code here:
        if (directorio.SaveAs()) {
            clearFields();
        }
    }//GEN-LAST:event_btnSaveAsActionPerformed

    private void btnCompilerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCompilerActionPerformed
        // TODO add your handling code here:
        if (getTitle().contains("&")||getTitle().equals(ABORT)) {
            if (directorio.Save()) {
                compile();
            }
        }else{
            compile();
        }
    }//GEN-LAST:event_btnCompilerActionPerformed

    private void btnExecuteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExecuteActionPerformed
        // TODO add your handling code here:
        btnCompiler.doClick();
        if (codeHasBeenCompiled) {
            if (!errors.isEmpty()){
            JOptionPane.showMessageDialog(null, "No se puede ejecutar el codigo ya que se encontro uno o mas errores",
                    "Error en la compilación", JOptionPane.ERROR_MESSAGE);
        } else {
                CodeBlock codeBlock = Functions.splitCodeInCodeBlocks(tokens, "{", "}", ";");
                System.out.println(codeBlock);
                ArrayList<String> blockOfCode = codeBlock.getBlocksOfCodeInOrderOfExec();
                System.out.println(blockOfCode);
            }
        }
    }//GEN-LAST:event_btnExecuteActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CompilerUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new CompilerUI().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCompiler;
    private javax.swing.JButton btnExecute;
    private javax.swing.JButton btnNew;
    private javax.swing.JButton btnOpen;
    private javax.swing.JButton btnSave;
    private javax.swing.JButton btnSaveAs;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable tablaTokens;
    private javax.swing.JTextArea textAreaResult;
    private javax.swing.JTextPane textPaneCode;
    // End of variables declaration//GEN-END:variables
}
