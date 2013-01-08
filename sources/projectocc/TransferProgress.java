/*
 * Copyright (c) 2002-2006, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */
package projectocc;

import jline.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class TransferProgress {
    
    Terminal terminal;
    ConsoleReader reader;
   
    
    public TransferProgress() {

       try {
	terminal = Terminal.setupTerminal();
	reader = new ConsoleReader();
	terminal.beforeReadLine(reader, "", (char)0);
       } catch (Exception e) {
	e.printStackTrace();
	terminal = null;
       }
       
    }


   public void escreverProgresso(int bytes){
       if (terminal == null)
		return;
       int w = reader.getTermwidth();
       String result = bytes + "bytes";
       try {
		reader.getCursorBuffer().clearBuffer();
		reader.getCursorBuffer().write(result);
		reader.setCursorPosition(w);
		reader.redrawLine();
	}
 	catch (IOException e) {
		e.printStackTrace();
	}
   }
   
   public void progressReset(){
        try {
            terminal.resetTerminal();
            reader.flushConsole();
            terminal.enableEcho();
        } catch (IOException ex) {}
   }
   

}
