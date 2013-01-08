/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package projectocc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 *
 * @author Miguel
 */
public class Aplicacao {

    ProtocoloTCP tcp;
    String root;
    
    public Aplicacao(String r) {
        root = r;
    }
    
    public void connectar(String enderecoDest, int portaDest) throws UnknownHostException, SocketException{
        tcp = new ProtocoloTCP(enderecoDest, portaDest, 9870);
        tcp.MAQUINA = 2;
    }
    
    public void terminar(){
        tcp.fecharSockets();
    }
    
    
    
    public String enviarComandoPUT(String ficheiro) throws IOException {
        String res = tcp.enviarPorTCP(("put " + ficheiro).getBytes());
        res = tcp.enviarPorTCPsemOPEN(pathToBytes(root + ficheiro));
        return "\n" + res;
    }
   
    public String enviarComandoGET(String ficheiro) throws IOException {
        String res="", get = "get " + ficheiro;
        res = tcp.enviarPorTCP(get.getBytes());
        if(res.equals(tcp.sucesso)){
            res = tcp.receberPorTCPsemOPEN();
            if(res.equals(tcp.sucesso)) {
                bytesToPath(tcp.bufferFinal, root + ficheiro);
            }
        }
        return "\n" + res;
    }
    
    public String enviarComandoDIRS() throws IOException{
        String dir ="dir",res="";
        res = tcp.enviarPorTCP(dir.getBytes());
        if(res.equals(tcp.sucesso)){
            res = tcp.receberPorTCPsemOPEN();
            if(res.equals(tcp.sucesso)) res = new String(tcp.bufferFinal,0,tcp.bufferFinal.length);
        }
        
        return "\n" + res;
    }
    
    public String enviarComandoCDS(String pasta) throws IOException{
        String cd ="cd",res="";
        res = tcp.enviarPorTCP((cd+" "+pasta).getBytes());
        
        return "\n" + res;
    }
    
    public String enviarComandoCDSoo() throws IOException{
        String cdoo ="cd..",res="";
        res = tcp.enviarPorTCP(cdoo.getBytes());
        
        return "\n" + res;
    }
    
    
    
    public byte[] pathToBytes(String file) throws IOException {
        InputStream in = new FileInputStream(file);
        byte[] data = new byte[in.available()];
        in.read(data);
        return data;
    }

    public void bytesToPath(byte[] bytes, String filename) throws FileNotFoundException, IOException{
        FileOutputStream outPut = new FileOutputStream(filename);  
        outPut.write(bytes); 
    }
    
    
    
    
    
    
    
    
    
    
    public void CommandLine() {
        BufferedReader read = new BufferedReader(new InputStreamReader(System.in));    
        String arg[];

        String conn[];
        conn = null;
        
        do{
          try{
            do{
                System.out.print(">");
                arg = read.readLine().split(" ");
            }while(arg.length<1);
            
            if(arg[0].equals("exit") || arg[0].equals("c")) break;
            
            if(arg[0].equals("help") || arg[0].equals("?")) ajuda();
            
            if(arg[0].equals("dir")) System.out.println(comandoDIR(root));
            
            if(arg[0].equals("disconnect")) {
                conn = null;
                System.err.println("terminado");
            }
            
            if(arg[0].equals("connect") && arg.length>2) {
                //connectar(arg[1], new Integer(arg[2]));
                System.err.println("conectado");
                conn = arg.clone();
            }
            
            if(arg[0].equals("dirs")) {
                if(conn!=null) connectar(conn[1], new Integer(conn[2]));
                System.out.println(enviarComandoDIRS());
                terminar();
            }
            
            if(arg[0].equals("cds")) {
                if(conn!=null) connectar(conn[1], new Integer(conn[2]));
                System.out.println(enviarComandoCDS(arg[1]));
                terminar();
            }
            
            if(arg[0].equals("cds..")) {
                if(conn!=null) connectar(conn[1], new Integer(conn[2]));
                System.out.println(enviarComandoCDSoo());
                terminar();
            }
            
            if(arg[0].equals("get")) {
                if(conn!=null) connectar(conn[1], new Integer(conn[2]));
                System.out.println(enviarComandoGET(arg[1]));
                terminar();
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
            
            if(arg[0].equals("put")) {
                if(conn!=null) connectar(conn[1], new Integer(conn[2]));
                System.out.println(enviarComandoPUT(arg[1]));
                terminar();
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
          }
          catch(Exception ex){}
        
        
        }while(true);
        
    }
    
    
    
    public String comandoDIR(String path){
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        
        String res="";

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                res += "\nFile:          ";
                res += listOfFiles[i].length()/1024 + " KB" + "    " + listOfFiles[i].getName();
            } else if (listOfFiles[i].isDirectory()) {
                res += "\nDirectory:     ";
                res += "      " + "    " + listOfFiles[i].getName();
                }
        }
        return this.root + "\n" + res;
    }
    
    
    public void ajuda(){
        System.out.println("---HELP---");
        System.out.println("? \t\t\t- para ajuda");
        System.out.println("help \t\t\t- para ajuda");
        System.out.println("exit \t\t\t- para sair");
        System.out.println("connect 'hostname' 'port' \t\t\t- para conectar ao servidor");
        System.out.println("disconnect \t\t\t- para desconectar do servidor");
        System.out.println("put 'ficheiro' \t\t\t- para enviar ficheiro");
        System.out.println("get 'ficheiro' \t\t\t- para descarregar ficheiro");
        System.out.println("dir \t\t\t- para mostrar todos os ficheiros e pastas na directoria actual");
        System.out.println("dirs \t\t\t- para mostrar todos os ficheiros e pastas na directoria do servidor");
        System.out.println("cds 'pasta'\t\t\t- para ir para a pasta pretendida");
        System.out.println("cds..\t\t\t- para voltar à pasta anterior");
        System.out.println("----------");
    }

    
    
}
