/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package projectocc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author Miguel
 */

public class AplicacaoServidor implements Runnable {
    DatagramPacket syn;
    ArrayList<String> root;
       public AplicacaoServidor(DatagramPacket p,ArrayList<String> root){
           this.syn = p;
           this.root = root;
       }
       public void run(){ 
            try {
                //servidor
                System.err.println("\t\t\t\tServer started");
                
                Random generator = new Random();
                
                ProtocoloTCP p = new ProtocoloTCP(syn.getAddress().getHostAddress(), syn.getPort(), generator.nextInt(65535));
                System.err.println("\t\t\t\tAAAAAA");
                p.MAQUINA=1;

                String res = p.receberPorTCP(syn);
                
                if(res.equals("sucesso")){
                    System.out.println(res);
              //      System.out.println(new String(p.bufferFinal,0, p.bufferFinal.length));
                }else return;
                
                
                Aplicacao app = new Aplicacao(root.get(0));

                
                String arg = new String(p.bufferFinal,0, p.bufferFinal.length > 30? 30 :  p.bufferFinal.length);
                
//                System.out.println(arg);
                
                if(arg.startsWith("dir")) {
                    res = p.enviarPorTCPsemOPEN(app.comandoDIR(app.root).getBytes());
                }
                
                if(arg.startsWith("cd ")) {
                    root.set(0, root.get(0) + arg.split(" ")[1] + "/" );
                }
                
                if(arg.startsWith("cd..")) {
                    String[] newroots = root.get(0).split("/");
                    int x = newroots[newroots.length-1].length()+1;
                    
                    root.set(0, root.get(0).substring(0, root.get(0).length()-x) );
                }
                
                if(arg.startsWith("get")) {
                    String args[] = arg.split(" ");
                    res = p.enviarPorTCPsemOPEN( app.pathToBytes(root.get(0)+args[1]) );
                }
                
                if(arg.startsWith("put")) {
                    res = p.receberPorTCPsemOPEN();
                    if(res.equals("sucesso")){
                        System.out.println(res);
  //                      System.out.println(new String(p.bufferFinal,0, p.bufferFinal.length));
                    }else return;
                    
                    app.bytesToPath(p.bufferFinal, root.get(0) + arg.split(" ")[1]);
                }
                
                
                
                System.out.println(res);
                
                p.fecharSockets();
                
            } catch (IOException ex) { System.out.println(ex.getMessage()); }
       }      
}

