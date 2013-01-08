/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package projectocc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
/**
 *
 * @author Miguel
 */
public class Datagrama {

    private int checksum;
    private int size;
    private byte[] dados;
    

    private static int MAX_SIZE = 1400;
    
    private static double te = 0.0; //taxa de erro
    private static double tp = 0.0; //taxa de perda
    
    
    public Datagrama(byte[] dados){
        this.checksum = 0x0;
        this.dados = dados.clone();
        this.size = dados.length;
    }
    
    public Datagrama(int size){
        this.checksum = 0x0;
        this.dados = new byte[size];
        this.size = size;
    }
    
    public int getChecksum(){
        return this.checksum;
    }
    
    public void setChecksum(int newchecksum){
        this.checksum = newchecksum;
    }
    
    public byte[] getDados(){
        return this.dados;
    }
    
    public void setDados(byte[] dados){
        this.dados = dados;
    }
    
    public int size(){
        return this.size;
    }
    
    public void showbin(int x){
        System.out.println(Integer.toBinaryString(x));
    }

    public void randomBinDataGen(){
        for(int i=0;i<this.dados.length;i++){
            Random generator = new Random();
            this.dados[i] = (byte) generator.nextInt(65535+1);
        }
    }
    
    public int generateChecksum(){
        int sum = 0;
        for(int i=0;i<this.dados.length;i++){
            sum += this.dados[i];
        }
        return ~sum;
    }
    
    public void randomDataChange(double te){
        Random generator = new Random();
        if(generator.nextDouble() <= te)
            this.randomBinDataGen();
    }
    
    public boolean checksumChecker(){
        return this.checksum == generateChecksum();
    }
    
    
    public byte[] exportToBytes(){
        int length = this.dados.length + 4;
        byte[] res = new byte[length];
        
        res[0] = this.intToByte4(this.checksum)[0];
        res[1] = this.intToByte4(this.checksum)[1];
        res[2] = this.intToByte4(this.checksum)[2];
        res[3] = this.intToByte4(this.checksum)[3];
        
        for(int i=0;i<this.dados.length;i++){
           res[i+4] = this.dados[i];
        }
        return res;    
    }
    
    public void importToDatagrama(byte[] data, int size){
        if(data.length<4) return;
        this.checksum = this.byte4ToInt(data);
        
        this.dados = new byte [data.length - 4];
        this.size = size-4; //tamanho do pacote - 4 bytes do checksum
               
        for(int i=4;i<data.length;i++){
           this.dados[i-4] = data[i];
        }
    }
    
    public byte[] intToByte4(int i){
        byte[] bytes = new byte[4];
        bytes[0] = (byte) ( (i & 0xff000000) >> 24 );
        bytes[1] = (byte) ( (i & 0xff0000)   >> 16 );
        bytes[2] = (byte) ( (i & 0xff00)     >> 8  );
        bytes[3] = (byte) (i & 0xff);
        return bytes;
    }
    
    
    public int byte4ToInt(byte[] b) {
        int l = 0;
        l |= b[0] & 0xFF;
        l <<= 8;
        l |= b[1] & 0xFF;
        l <<= 8;
        l |= b[2] & 0xFF;
        l <<= 8;
        l |= b[3] & 0xFF;
        return l;
    }    
    
    
    
    public static boolean taxaPerda(){
       Random generator = new Random();
       double rand = generator.nextDouble();
       return rand >= tp;
    }
    
    public static void enviarDatagrama(byte[] bytes, DatagramSocket s, InetAddress addr, int port) throws IOException {
       if(taxaPerda()){
           
           Datagrama sendDatagrama = new Datagrama(bytes.length); 

           sendDatagrama.importToDatagrama(bytes,bytes.length); //gerar camada de dados no datagrama
           sendDatagrama.setChecksum(sendDatagrama.generateChecksum()); //gerar checksum
           sendDatagrama.randomDataChange(te);

           DatagramPacket sendPacket = 
              new DatagramPacket(sendDatagrama.exportToBytes(), sendDatagrama.exportToBytes().length,          
                 addr, port); //Cria o datagrama com os dados a enviar, tamanho, endereço e porta do servidor

           s.send(sendPacket); //Envia datagrama para o servidor
           
       }
    }
    
    public static Datagrama receberDatagrama(DatagramSocket s) throws IOException{
       byte[] receiveData = new byte [MAX_SIZE];
       Datagrama receiveDatagrama = new Datagrama(MAX_SIZE); 
       DatagramPacket receivePacket = 
               new DatagramPacket(receiveData, receiveData.length); 
       
       s.receive(receivePacket); //Lê datagrama do servidor
       
       receiveDatagrama.importToDatagrama(receivePacket.getData(),receivePacket.getLength()); //importar bytes recebidos
       
       if (receiveDatagrama.checksumChecker()) return receiveDatagrama;
       else return null;
   }
    
    
   public static DatagramPacket receberDatagramPacket(DatagramSocket s) throws IOException{
       byte[] receiveData = new byte [MAX_SIZE];
       Datagrama receiveDatagrama = new Datagrama(MAX_SIZE); 
       DatagramPacket receivePacket = 
               new DatagramPacket(receiveData, receiveData.length); 
       
       s.receive(receivePacket); //Lê datagrama do servidor
       
       receiveDatagrama.importToDatagrama(receivePacket.getData(),receivePacket.getLength());
       
       if (receiveDatagrama.checksumChecker()) return receivePacket;
       else return null;
   } 
   
   public static Datagrama receberDeDatagramPacket(DatagramPacket receivePacket){
       Datagrama receiveDatagrama = new Datagrama(MAX_SIZE); 
       receiveDatagrama.importToDatagrama(receivePacket.getData(),receivePacket.getLength());
       
       if (receiveDatagrama.checksumChecker()) return receiveDatagrama;
       else return null;
   }
    
   public static void fecharSocket(DatagramSocket s){
       s.close();
   } 
    
    
    
    
    
    public static void main(String args[]) throws Exception {
        /*
        Datagrama dm = new Datagrama(1000);
        dm.randomBinDataGen(); //gera datagrama aleatorio
        dm.setChecksum(dm.generateChecksum()); //calcula o checksum

        dm.showbin(dm.getChecksum());
        System.out.println(Integer.toHexString(dm.getChecksum()));
        System.out.println(dm.checksumChecker());
        
        dm.randomDataChange(0.3);
        
        dm.showbin(dm.getChecksum());
        System.out.println(Integer.toHexString(dm.getChecksum()));
        System.out.println(dm.checksumChecker());
        
        System.out.println("---------------------");
        
        System.out.println(Integer.toHexString(dm.exportToBytes()[0]));
        System.out.println(Integer.toHexString(dm.exportToBytes()[1]));
        System.out.println(Integer.toHexString(dm.exportToBytes()[2]));
        System.out.println(Integer.toHexString(dm.exportToBytes()[3]));
        System.out.println(Integer.toHexString(dm.exportToBytes()[4]));
        
        System.out.println("---------------------1");
        
        System.out.println(Integer.toHexString(dm.checksum));
        System.out.println(Integer.toHexString(dm.dados[0]));
        
        System.out.println("---------------------2");
        
        System.out.println(Integer.toHexString(dm.byte4ToInt(dm.exportToBytes())));
        
        
        System.out.println("---------------------3");
        System.out.println(Integer.toHexString(dm.checksum));
        System.out.println(Integer.toHexString(dm.dados[0]));
        dm.importToDatagrama(dm.exportToBytes());
        System.out.println(Integer.toHexString(dm.checksum));
        System.out.println(Integer.toHexString(dm.dados[0]));
        
        System.out.println("---------------------4");
        
        String s = "aaaaaa";
        dm.importToDatagrama(s.getBytes());
        System.out.println(dm.checksum+","+ Integer.toHexString(dm.checksum));
        System.out.println(Integer.toHexString(dm.dados[0]));
         * */
    }

}
