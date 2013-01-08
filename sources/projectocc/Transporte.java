/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package projectocc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 *
 * @author Miguel
 */
public class Transporte {

    //cabecalho = 16 bytes
        //0,1 - byte[2] offset dos dados em bytes
        //2,3 - byte[2] size dos dados em bytes
        //4,5 - byte[2] win : tamanho de janela em bytes
        
        //6,7 - byte[2] n_seq : numero de sequencia (incrementa sempre)
        //8,9 - byte[2] n_seqAck : numero de sequencia do ack
        //10  - byte    flag : SYN=1 | FIN=2 | PSH=3 | ACK=4  (2 bits)
    
        //11,12  - byte[2] MTU : maximum tranfer unit
    
    
    byte[] pacote;
    
    public Transporte(int length){
        this.pacote = new byte[length+16];
    }
    
    public Transporte(byte[] pacote){
        this.pacote = pacote;
    }
    
    public byte[] getPacote(){
        return this.pacote;
    }
    
    public void setPacote(byte[] pacote){
        this.pacote = pacote;
    }
    
    public byte[] getDados(){
        byte[] res = new byte[this.pacote.length-16];
        for(int k=16;k<this.pacote.length;k++)
            res[k-16] = this.pacote[k];
        return res;
    }
    
    public void setDados(byte[] dados){
        for(int k=0;k<dados.length;k++)
            this.pacote[k+16] = dados[k];
    }
    
    //start=0 : primeiro byte
    public void setDados(byte[] dados, int start, int len){
        for(int k=0;k<len;k++)
            this.pacote[k+16] = dados[k+start];
    }
    
    private byte[] getGeneric(int i){
        byte[] b = new byte[2];
        b[0] = this.pacote[i];
        b[1] = this.pacote[i+1];
        return b;
    }
    
    private void setGeneric(byte[] b, int i){
        this.pacote[i] = b[0];
        this.pacote[i+1] = b[1];
    }
    
    public byte[] getOffset(){
        return getGeneric(0);
    }
    
    public void setOffset(byte[] offset){
        setGeneric(offset,0);
    }
    
    public byte[] getSize(){
        return getGeneric(2);
    }
    
    public void setSize(byte[] size){
        setGeneric(size,2);
    }
    
    public byte[] getWin(){
        return getGeneric(4);
    }
    
    public void setWin(byte[] win){
        setGeneric(win,4);
    }
    
    public byte[] getSeq(){
        return getGeneric(6);
    }
    
    public void setSeq(byte[] seq){
        setGeneric(seq,6);
    }
    
    public byte[] getSeqAck(){
        return getGeneric(8);
    }
    
    public void setSeqAck(byte[] seqAck){
        setGeneric(seqAck,8);
    }
    
    public byte getFlag(){
        return this.pacote[10];
    }
    
    public String getFlagStr(){
        if (this.getFlag()==1) return "SYN";
        if (this.getFlag()==2) return "FIN";
        if (this.getFlag()==3) return "PSH";
        if (this.getFlag()==4) return "ACK";
        if (this.getFlag()==5) return "SYN_ACK";
        return "";
    }
    
    public void setFlag(byte flag){
        this.pacote[10] = flag;
    }
    
    public void setFlag(String s){
        if (s.equalsIgnoreCase("SYN")) this.pacote[10] = 1;
        else if (s.equalsIgnoreCase("FIN")) this.pacote[10] = 2;
        else if (s.equalsIgnoreCase("PSH")) this.pacote[10] = 3;
        else if (s.equalsIgnoreCase("ACK")) this.pacote[10] = 4;
        else if (s.equalsIgnoreCase("SYN_ACK")) this.pacote[10] = 5;
        else this.pacote[10] = 0;
    }
    
    
    public byte[] getMTU(){
        return getGeneric(11);
    }
    
    public void setMTU(byte[] mtu){
        setGeneric(mtu,11);
    }
    
    
    public static byte[] intToByte(int i, int b_long){
        byte[] bytes = new byte[b_long];
        for(int k=0;k<b_long;k++)
            bytes[k] = (byte) ( (i & (0xff << (b_long-k-1)*8 ) )     >> (b_long-k-1)*8  );
        return bytes;
    }
    
    
    public static int byteToInt(byte[] b, int b_long) {
        int l = 0;
        l |= b[0] & 0xFF;
        for(int k=1;k<b_long;k++){
            l <<= 8;
            l |= b[k] & 0xFF;
        }
        return l;
    }    
    
    
    public boolean eqFlag(String s){
        if (s.equalsIgnoreCase("SYN")) return this.getFlag()==1;
        if (s.equalsIgnoreCase("FIN")) return this.getFlag()==2;
        if (s.equalsIgnoreCase("PSH")) return this.getFlag()==3;
        if (s.equalsIgnoreCase("ACK")) return this.getFlag()==4;
        if (s.equalsIgnoreCase("SYN_ACK")) return this.getFlag()==5;
        return false;
    }
    
    public void createTransportePacket(String flag, int offset, int size, int win, int mtu,
                                                int seq, int seqAck, byte[] dados){
        this.setOffset(intToByte(offset, 2));
        this.setSize(intToByte(size, 2));
        this.setWin(intToByte(win, 2));
        this.setMTU(intToByte(mtu, 2));
        
        this.setSeq(intToByte(seq, 2));
        this.setSeqAck(intToByte(seqAck, 2));
        
        this.setFlag(flag);
        
        byte[] b = new byte[size];

        for(int i=0;i<size;i++) b[i] = dados[offset++];
        
        this.setDados(b);
    }
    

    
    
    
    public void createTransportePacket(String flag){
        this.setOffset(intToByte(0, 2));
        this.setSize(intToByte(10, 2));
        this.setWin(intToByte(4000, 2));
        this.setMTU(intToByte(1400, 2));
        
        this.setSeq(intToByte(1, 2));
        this.setSeqAck(intToByte(0, 2));
        
        this.setFlag(flag);

        String s = "pacote1234567";
        this.setDados(s.getBytes());
    }

    
    
    public void mostraPacote(){
        System.out.println("------ " + this.getFlagStr() + " ------");
        System.out.print("seq:" + byteToInt(this.getSeq(),2));
        System.out.println(", seqAck:" + byteToInt(this.getSeqAck(),2));
        
        System.out.print("offset:" + byteToInt(this.getOffset(),2));
        System.out.print(", size:" + byteToInt(this.getSize(),2));
        System.out.print(", win:" + byteToInt(this.getWin(),2));
        System.out.println(", mtu:" + byteToInt(this.getMTU(),2));
        System.out.println("-----------------");
        System.out.println("Dados:");
        System.out.println(
                new String(
                    this.getDados(), 0, byteToInt(this.getSize(),2)
                    )
                );
    }
    
    @Override
        public String toString(){
            String s = "------ " + this.getFlagStr() + " ------\n";
            s += "seq:" + byteToInt(this.getSeq(),2);
            s += ", seqAck:" + byteToInt(this.getSeqAck(),2) + "\n";
            s += "offset:" + byteToInt(this.getOffset(),2);
            s += ", size:" + byteToInt(this.getSize(),2);
            s += ", win:" + byteToInt(this.getWin(),2);
            s += ", mtu:" + byteToInt(this.getMTU(),2) + "\n";
            s += "-----------------\n";
            s += "Dados:\n";
            //s += new String(this.getDados(), 0, byteToInt(this.getSize(),2));

            return s;
    }
    
    @Override
        public Object clone(){
            return new Transporte(this.pacote);
        }
    
    
        
    
    public void enviarTransporte(DatagramSocket s, InetAddress addr, int port) throws IOException{
        Datagrama d = new Datagrama(this.pacote);
        Datagrama.enviarDatagrama(d.exportToBytes(), s, addr, port);
    }
    
    public Transporte receberTransporte(DatagramSocket s) throws IOException{
        Datagrama d = Datagrama.receberDatagrama(s);
        if(d!=null) return new Transporte(d.getDados());
        else return null;
    }
    
    public DatagramPacket receberDatagramPacket(DatagramSocket s) throws IOException{
        return Datagrama.receberDatagramPacket(s);
    }
    
    public Transporte receberDeDatagramPacket(DatagramPacket p) throws IOException{
        Datagrama d = Datagrama.receberDeDatagramPacket(p);
        if(d!=null) return new Transporte(d.getDados());
        else return null;
    }
    
    public void fecharSocket(DatagramSocket s){
        Datagrama.fecharSocket(s);
    }
    
    
    public static ArrayList<Transporte> fragmentaTransporte(byte[] bytes, int sizeEach, int mtu,int win){
        ArrayList<Transporte> res = new ArrayList<Transporte>();
        int begin=0;
        
        while(begin+sizeEach < bytes.length){
            Transporte t = new Transporte(sizeEach);
            t.createTransportePacket("PSH", begin, sizeEach, win, mtu, begin+1, 0, bytes);
            begin += sizeEach;
            res.add(t);
        }
        if(begin+sizeEach >= bytes.length){
            Transporte t = new Transporte(sizeEach);
            t.createTransportePacket("PSH", begin, bytes.length-begin, win, mtu, begin+1, 0, bytes);
            res.add(t);
        }
        
        return res;
    }
    
    public void colocarNoBuffer(byte[] buffer, int offset, int size){
        for(int i=0;i<size;i++)
            buffer[offset+i] = this.getDados()[i];
    }
    
    
    public static void main(String[] args){
        
        //String s = "Era uma vez o capuchinho vermelho. Ia o lobo mau a passear na floresta.";
        String s = "Era uma vez o capuchinho vermelho.";
        byte[] b = s.getBytes();
        
        
        ArrayList<Transporte> lista = Transporte.fragmentaTransporte(b, 10,1400,1000);
        
        for(int i=0;i<lista.size();i++) lista.get(i).mostraPacote();
        
        byte[] buffer = new byte[100];
        int sizeBuffer=0;
        for(int i=0;i<lista.size();i++) {
            Transporte t = lista.get(i);
            t.colocarNoBuffer(buffer, Transporte.byteToInt(t.getOffset(), 2), Transporte.byteToInt(t.getSize(), 2));
            
            sizeBuffer += Transporte.byteToInt(t.getSize(), 2);
        }
        
        System.out.println(new String(buffer, 0, sizeBuffer));
        
        
        /*
        Transporte t = new Transporte(10);
        
        t.createTransportePacket("PSH", 0, 10, 1000, 1400, 0, 0, b);
        t.mostraPacote();
        System.out.println(t.getDados().length);
        System.out.println(t.getPacote().length);
        
        
        t.createTransportePacket("PSH", 10, 10, 1000, 1400, 0, 0, b);
        t.mostraPacote();
        System.out.println(t.getDados().length);
        System.out.println(t.getPacote().length);
        */
        
        
        /*
        Transporte t = new Transporte(13);
        
        t.createTransportePacket("ACK");
        
        t.mostraPacote();
        System.out.println(t.getDados().length);
        System.out.println(t.getPacote().length);
        */
        
        /*
        Transporte t = new Transporte(100);
         t.setSeq(intToByte(1024,2));
        
        System.out.println(byteToInt(t.getSeq(),2));
        
        t.setFlag((byte) 2);
        System.out.println(t.eqFlag("fin"));
        
        String s = "ola";
        byte[] b = s.getBytes();

        System.out.println(new String(b));
        System.out.println(s.getBytes().length);
        t.setDados(s.getBytes());
        System.out.println(t.getDados().length);
        System.out.println(new String(t.getDados()));
        */
        //System.out.println(byteToInt(intToByte(1024,2),2));
        
        /*
        System.out.println(Integer.toBinaryString(1024));
        
        byte[] b = new byte [4];
        b = intToByte(1024,2);
        System.out.println(Integer.toBinaryString(   byteToInt(b,2)   ));
        */
    }
    
}
