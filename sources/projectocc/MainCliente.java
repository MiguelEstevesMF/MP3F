/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package projectocc;

import java.io.IOException;

/**
 *
 * @author Miguel
 */


class ThreadCliente implements Runnable {
    String root;
    public ThreadCliente(String root){
        this.root = root;
    }
    
    public void run() { 

        //connect localhost 9876
        Aplicacao cliente = new Aplicacao(root);
        cliente.CommandLine();
    }

}
 

public class MainCliente {
    
   public static void main(String args[]) { 
       try {
            Thread t = new Thread();
            t = new Thread(new ThreadCliente(args[0]));
            t.start();
            t.join();
            
        } catch (InterruptedException ex) {}
   }
    
}
