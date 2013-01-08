/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package projectocc;

/**
 *
 * @author Miguel
 */
public class MainServidor {
    
    public static void main(String args[]) { 
        try {
            Thread daem = new Thread();
            daem = new Thread(new DaemonServidor(new Integer(args[0]),args[1]));
            daem.start();
            daem.join();

        } catch (InterruptedException ex) {}
    }


}
