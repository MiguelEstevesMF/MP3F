/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package projectocc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;

/**
 *
 * @author Miguel
 */
public class DaemonServidor implements Runnable {
   DatagramSocket receiveSocket;
   ArrayList<String> root;
   

   public DaemonServidor(int port,String root){
       try { 
           receiveSocket = new DatagramSocket(port); 
           receiveSocket.setSoTimeout(1000);
       } catch (SocketException ex) {}
       
       this.root = new ArrayList();
       this.root.add(root);
   }

   public void run(){ 
       
       while(true){
            try {
                try { Thread.sleep(1000); } catch (InterruptedException ex) {}

                System.err.println("Deamon");
                Transporte tr = new Transporte(100);
                DatagramPacket p = tr.receberDatagramPacket(receiveSocket);

              if(p!=null){

                System.err.print("Deamon: ");
                System.err.println(p.getAddress() + "," + p.getPort());

                tr = tr.receberDeDatagramPacket(p);

                System.err.print("Deamon: ");
                System.err.println(tr.toString()); //teste

                if(tr.eqFlag("SYN")){
                    Thread t = new Thread(new AplicacaoServidor(p,this.root));
                    t.start();
                    //t.join();
                }
               
              }
            
            //} catch (InterruptedException ex) {
            } catch (IOException ex) {}
       }   
   }      
}
