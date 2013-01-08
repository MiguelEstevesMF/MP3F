/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package projectocc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.locks.ReentrantLock;


class ListaRecebidos{
    public ArrayList<DatagramPacket> lista;
    ReentrantLock mutex;
    public ListaRecebidos(){
        lista = new ArrayList();
        mutex = new ReentrantLock();
    }
    public void lock(){
        mutex.lock();
    }
    public void unlock(){
        mutex.unlock();
    }
}




/**
 *
 * @author Miguel
 */
public class ProtocoloTCP {

    //variaveis de tempo (ms)
    private int SOCKETTIMEOUT=1000/100;
    private int SMALLTIMEOUT_OPEN=5000/10;
    private int SMALLTIMEOUT_OPEN2=7000/10;
    private int SMALLTIMEOUT=10000/100;
    
   private DatagramSocket receiveSocket,sendSocket;
   private InetAddress sendIPAddress;
   private int sendPort,receivePort;
   ListaRecebidos Recebidos;
   
   private boolean END_TimeOut,END_AReceber,END_SmallTimeOut;
   private int NOTIFYED;
   
   BoundedBuffer boundedB;
   IntObj timeout;
   int win,mtu,seq,seqAck;
   
   int RTT_INIT=100;
   int RTT;
   String rtt_error = "RTT expirou";
   String sucesso = "sucesso";
   
   int initTransporteACKeSYN = 10;
   int initTransportePSH = 10*50;
   int FragmentSize = 10*50;
   
   int MINWIN=10*50;
   int MAXWIN=40*50;
   
   int BufferSize = 65000;
   byte[] bufferFinal;
   
   TransferProgress progress;
   
   public ProtocoloTCP(String sendaddress, int sendport, int receiveport) throws UnknownHostException, SocketException{
       this.sendIPAddress = InetAddress.getByName(sendaddress); //endereco de destino
       this.sendPort = sendport; //porta de envio
       this.receivePort = -1; //porta do socket que enviou o pacote para mim
       this.receiveSocket = new DatagramSocket(receiveport); //socket e porta para receber
       this.receiveSocket.setSoTimeout(SOCKETTIMEOUT);  //socket recebe durante 1 segundo dentro de while(true)
       
       this.sendSocket = new DatagramSocket(); //socket de envio
       
       this.Recebidos = new ListaRecebidos();
       
       boundedB= new BoundedBuffer();
       timeout = new IntObj();
       win = 1000;
       mtu = 1400;
       seq = 0;
       seqAck = 0;
       
       this.bufferFinal = null;
       
       progress = new TransferProgress();
   }
   
   
   public String openSEND() throws IOException{
        Transporte t = new Transporte(initTransporteACKeSYN);
        Transporte t1 = new Transporte(initTransporteACKeSYN);
        
        seq = 0; //numero de sequencia por defeito para o cliente
        mtu = 1400;
        win = MINWIN;
        
     //criar SYN (dados = porta que recebe)
        t.createTransportePacket("SYN", 0, 2, win, mtu, seq, 0, Transporte.intToByte(this.receiveSocket.getLocalPort(), 2));
        Console_println(t.toString()); //teste
        
        
        
     //envia SYN e espera por SYN_ACK   
        RTT = RTT_INIT;
        do{ RTT--;
            t.enviarTransporte(this.sendSocket, this.sendIPAddress, this.sendPort);
            Console_println(">SYN enviado"+ " para "+this.sendIPAddress.getHostAddress()+":"+this.sendPort);

            this.iniciar_smallTimeOut(boundedB,timeout,SMALLTIMEOUT_OPEN*4);
            
            Console_println(">a espera de SYN_ACK");
            
            t1 = recebeuACKde(t, this.sendIPAddress);
            if(timeout.value==1) Console_println("timeout");
            if(RTT<0) return rtt_error;
        }while(timeout.value==1 || t1==null);
        
        this.sendPort = Transporte.byteToInt(t1.getDados(),2); //muda a porta de envio
        
        Transporte syn = (Transporte) t.clone(); //cria copia do SYN
        
     //criar ACK (size=1) de SYN_ACK
        
        int x = Transporte.byteToInt(t1.getSeq(), 2) + 1;  //num_seq(SYN_ACK) + 1
        t.createTransportePacket("ACK", 0, 1, win, mtu, seq, x, new byte[1]);
        Console_println(t.toString()); //teste
        
        
     //envia ACK (size=1) de SYN_ACK
        RTT = RTT_INIT;
        do{ RTT--;
            t.enviarTransporte(this.sendSocket, this.sendIPAddress, this.sendPort);
            
            Console_println(">ACK enviado");
            
            this.iniciar_smallTimeOut(boundedB,timeout,SMALLTIMEOUT_OPEN2); //timeout superior ao do outro terminal
            
            Console_println(">a espera de SYN_ACK repetido");
            
            t1 = recebeuACKde(syn, this.sendIPAddress);
            if(timeout.value==1) Console_println("timeout");
            if(RTT<0) return rtt_error;
        }while(t1!=null && timeout.value==0);
        
        return sucesso;
   }
   
   public String enviarBytesSEND(byte[] bytes) throws IOException{
       
        Transporte t = new Transporte(initTransportePSH);
        Transporte t1 = new Transporte(initTransportePSH); 
        Transporte fin = new Transporte(initTransportePSH); 
        Transporte rttT = new Transporte(initTransportePSH); 
        

      //fragmenta bytes numa lista de PSH  
        seq = 1; //importante
        seqAck = 1; //importante
                
        ArrayList<Transporte> listaFragmentos = Transporte.fragmentaTransporte(bytes, FragmentSize, mtu, win);
        
        //int offsetActual=0;
        int i=0;
        //envia PSHs e espera por ACK
        
        RTT = RTT_INIT;
        do{ 
            int offsetActual=0;
            
            do{
                t = listaFragmentos.get(i);
                int size = Transporte.byteToInt(t.getSize(), 2);
                
                seq = offsetActual + 1;
                
                t.setSeqAck(Transporte.intToByte(seqAck, 2));
                t.setWin(Transporte.intToByte(win, 2));
                
                Console_println(t.toString());
                
                if(Transporte.byteToInt(t.getSeq(), 2) <=  Transporte.byteToInt(rttT.getSeq(), 2)) {RTT--;} 
                else {RTT = RTT_INIT; rttT = (Transporte) t.clone();}
                if(RTT<0) return rtt_error;

                t.enviarTransporte(this.sendSocket, this.sendIPAddress, this.sendPort);
                
                
                Console_PrintPercentage(seqAck);
                
                Console_println(">PSH enviado"+ " para "+this.sendIPAddress.getHostAddress()+":"+this.sendPort);
                
                offsetActual += size;
                i++;
                
                
            }while(offsetActual<win && i<listaFragmentos.size());
        
            Console_println("..............i="+i);
            Console_println("..............offAct="+offsetActual);
            Console_println("..............siz="+Transporte.byteToInt(t.getSize(), 2));
            
            if(i==listaFragmentos.size()) {
                //criar e enviar FIN
                int x = Transporte.byteToInt(listaFragmentos.get(i-1).getSeq(),2) + Transporte.byteToInt(listaFragmentos.get(i-1).getSize(),2);
                fin.createTransportePacket("FIN", 0, 1, win, mtu, x, seqAck, new byte[1]);
                Console_println(fin.toString());
                fin.enviarTransporte(sendSocket, sendIPAddress, sendPort);
            }
            
            
            
            this.iniciar_smallTimeOut(boundedB,timeout,SMALLTIMEOUT);
            if(timeout.value==1) Console_println("timeout");
            
            Console_println(">a espera de ACK");
            
            //percorre a lista de fragmentos do fim para o inicio ate encontrar 
            //o fragmento ao qual o Ack recebido corresponde
            
            do{
                t1 = recebeuACKdePSH(listaFragmentos.get(i-1), this.sendIPAddress);
                Console_println("reconhecendo");
            
                if(t1!=null) {
                    seqAck = Transporte.byteToInt(t1.getSeqAck(),2);
                    Console_println("removendo parte da lista");
                    while(i-->0) listaFragmentos.remove(0);
                    i=0;
                    if(win*2> MAXWIN) win=MAXWIN; else win *=2;
                }
                else {
                    //offsetActual -= Transporte.byteToInt(listaFragmentos.get(i-1).getSize(), 2);
                    i--;
                    if(win/2<= MINWIN) win=MINWIN; else win /=2;
                }
                
            }while(i>0 && t1==null);
            
            //se recebeu ACK mas nao o reconheceu
            if(t1 == null) {
                for(int k=listaFragmentos.size()-1; k>=0; k--){
                    t1 = recebeuACKdePSH(listaFragmentos.get(k), this.sendIPAddress);
                    Console_println("reconhecendo++");
                    if(t1!=null) {
                        seqAck = Transporte.byteToInt(t1.getSeqAck(),2);
                        Console_println("removendo++ parte da lista");
                        while(k-->=0) listaFragmentos.remove(0);
                        i=0;
                        if(win*2> MAXWIN) win=MAXWIN; else win *=2;
                    }
                }
            
            }
            
            
            
            if(i==listaFragmentos.size()) {
                
                
                Console_println("---------break");break;
            }
            
        
        }while(true);

        return sucesso;
   }
   
   public String enviarPorTCP(byte[] bytes) throws IOException{ //active open
       Console_println("receiveSocket:"+this.receiveSocket.getLocalPort());
       Console_println("sendSocket:"+this.sendSocket.getLocalPort());

        Thread Ta = new Thread();
        Ta = this.iniciar_smallAReceber(boundedB);
        Ta.start(); //iniciar o listener de pacotes
              
        Transporte t = new Transporte(initTransportePSH);
        Transporte t1 = new Transporte(initTransportePSH); 
        
        
        
        if(this.openSEND().equals(rtt_error)) {
            this.finalizar_smallAReceber(Ta); //finalizar o listener de pacotes
            return rtt_error;
        }
       
        Console_println("CLIENTE CONECTADO");
        
        
        if(this.enviarBytesSEND(bytes).equals(rtt_error)) {
            this.finalizar_smallAReceber(Ta); //finalizar o listener de pacotes
            return rtt_error;
        }
        
        this.finalizar_smallAReceber(Ta); //finalizar o listener de pacotes
        
        Console_println(timeout.value);
        
        return sucesso;
   }
   
   
   public String openRECEIVE(DatagramPacket syn) throws IOException{
        Transporte t = new Transporte(initTransporteACKeSYN);
        Transporte t1 = new Transporte(initTransporteACKeSYN);
        
        seq = 100;        

        
     //desencapsula SYN e reconhece o endereco/porta do outro terminal
        t1 = t1.receberDeDatagramPacket(syn);
        this.sendIPAddress = syn.getAddress();
        this.sendPort = Transporte.byteToInt(t1.getDados(),2); //recebe a receivePort do cliente
        this.receiveSocket = new DatagramSocket();
        this.receiveSocket.setSoTimeout(SOCKETTIMEOUT);
        mtu = Transporte.byteToInt(t1.getMTU(),2);
        win = Transporte.byteToInt(t1.getWin(),2);
                
     //criar SYN_ACK 
        int x = Transporte.byteToInt(t1.getSeq(), 2) + 1; //num_ack = num_seq(SYN) + 1
        t.createTransportePacket("SYN_ACK", 0, 2, win, mtu, seq, x, Transporte.intToByte(this.receiveSocket.getLocalPort(), 2));
        Console_println(t.toString()); //teste
        
     //envia SYN_ACK e espera por ACK
        RTT = RTT_INIT;
        do{ RTT--;
            t.enviarTransporte(this.sendSocket, this.sendIPAddress, this.sendPort);
            Console_println(">SYN_ACK enviado"+ " para "+this.sendIPAddress.getHostAddress()+":"+this.sendPort);

            this.iniciar_smallTimeOut(boundedB,timeout,SMALLTIMEOUT_OPEN);
            
            Console_println(">a espera de ACK");
            
            t1 = recebeuACKde(t, this.sendIPAddress);
            if(timeout.value==1) Console_println("timeout");
            if(RTT<0) return rtt_error;
        }while(timeout.value==1 || t1==null);
        
        return sucesso;
   }
   
   public String enviarBytesRECEIVE() throws IOException{
        byte[] buffer = new byte[BufferSize];
        int sizeBuffer=0;
        int size=1,seqAckPsh=1;
        
        
        Transporte t = new Transporte(initTransportePSH);
        Transporte t1 = new Transporte(initTransportePSH); 
        Transporte rttT = new Transporte(initTransportePSH); 

        boolean flagFIN = false;
        
        seq=1;
        
        int sizeWin = 0;
        
        RTT = RTT_INIT;
        do{ //RTT--;
            Console_println(">aguardando um PSH");

            this.iniciar_smallTimeOut(boundedB,timeout,SMALLTIMEOUT);
            
            Console_println(">a espera de PSH");
            
            Console_println("a receber seq="+seq);
            
            //recebe o PSH que tenha num_sequencia = seq - k
            for(int k=0;k<MAXWIN;k+=size){
                t1 = recebeuPSH(seq-k, this.sendIPAddress);
                if(t1!=null) {
                    if(k>0) {
                        //sizeBuffer -= Transporte.byteToInt(t1.getSize(), 2);
                        sizeWin = win;
                    }
                    break;
                }
            }
            
            //se recebeu PSH
            if(t1!=null && t1.eqFlag("PSH")) 
                win = Transporte.byteToInt(t1.getWin(), 2);
            
            if(t1!=null && t1.eqFlag("PSH") &&
                    seq <= Transporte.byteToInt(t1.getSeq(), 2) + Transporte.byteToInt(t1.getSize(), 2)) {
                
                size = Transporte.byteToInt(t1.getSize(), 2);
                seqAckPsh = Transporte.byteToInt(t1.getSeqAck(), 2);
                
                seq = Transporte.byteToInt(t1.getSeq(), 2) + size;
                win = Transporte.byteToInt(t1.getWin(), 2);

                //guardar no buffer
                t1.colocarNoBuffer(buffer, Transporte.byteToInt(t1.getOffset(), 2), Transporte.byteToInt(t1.getSize(), 2));
                sizeBuffer += size;
                sizeWin += Transporte.byteToInt(t1.getSize(), 2);
            }
            
            
            Console_println("seq="+seq);
            Console_println("sizeWin+1="+(sizeWin+1));
            //supondo seq(PSH)=1, win(PSH)=1024
            //se seq=1025 maior que 1024
            
            if( sizeWin+1 > win || (t1!=null && t1.eqFlag("FIN")) ) {
                Console_println("buffer: "+new String(buffer, 0, sizeBuffer));
                seqAck = seq;
                //criar e enviar ACK
                t.createTransportePacket("ACK", 0, 1, win, mtu, seq, seqAck, new byte[1]);
                Console_println(t.toString());
                


                if(Transporte.byteToInt(t.getSeq(), 2) <=  Transporte.byteToInt(rttT.getSeq(), 2)) {RTT--;} 
                else {RTT = RTT_INIT; rttT = (Transporte) t.clone();}
                if(RTT<0) return rtt_error;
                
                t.enviarTransporte(sendSocket, sendIPAddress, sendPort);
                
                Console_PrintPercentage(seqAck);
                
                Console_println(">ACK enviado"+ " para "+this.sendIPAddress.getHostAddress()+":"+this.sendPort);
                sizeWin=0;
            }
            else {
                if(RTT<0) return rtt_error;
                if(timeout.value==1) RTT--;
            }
            
            //se recebeu FIN
            if(t1!=null && t1.eqFlag("FIN")) {
                flagFIN = true;
                sizeBuffer = Transporte.byteToInt(t1.getSeq(),2)-1;
                Console_println("bufferFinal: "+new String(buffer, 0, sizeBuffer));
                this.bufferFinal = new byte [sizeBuffer];
                for(int k=0;k<sizeBuffer;k++)
                    this.bufferFinal[k] = buffer[k];
                
                
            }
            
            Console_println("\t\t\twin="+win); 
            if(timeout.value==1) {
                Console_println("timeout"); 
                //if(win /2>MINWIN) win /=2; else win = MINWIN;
                
                
                //if(seq-size>=seqAckPsh) seq -= size;
                /*seq = seqAckPsh;
                Console_println("VAI DESCER O SEQ: seq:" + seq +", seqAckPsh:"+seqAckPsh); 
                sizeWin =0;
                sizeBuffer = seqAckPsh-1;
                 */
            }
            
            
            
        }while(!flagFIN);
        
        return sucesso;
   }
   
   public String receberPorTCP(DatagramPacket syn) throws IOException{ //passive open
       Console_println("receiveSocket:"+this.receiveSocket.getLocalPort());
       Console_println("sendSocket:"+this.sendSocket.getLocalPort());
       
        Transporte t = new Transporte(initTransportePSH);
        Transporte t1 = new Transporte(initTransportePSH);
        

        Thread Ta = new Thread();
        Ta = this.iniciar_smallAReceber(boundedB);
        Ta.start(); //iniciar o listener de pacotes
        
        
        if(this.openRECEIVE(syn).equals(rtt_error)) {
            this.finalizar_smallAReceber(Ta); //finalizar o listener de pacotes
            return rtt_error;
        }
        
        Console_println("SERVIDOR CONECTADO");
        
        if(this.enviarBytesRECEIVE().equals(rtt_error)) {
            this.finalizar_smallAReceber(Ta); //finalizar o listener de pacotes
            return rtt_error;
        }
        
        this.finalizar_smallAReceber(Ta); //finalizar o listener de pacotes
        
        Console_println(timeout.value);
       
        return sucesso;
   }
   
   
   
   
   
   
   public String enviarPorTCPsemOPEN(byte[] bytes) throws IOException{ //active open
       Console_println("receiveSocket:"+this.receiveSocket.getLocalPort());
       Console_println("sendSocket:"+this.sendSocket.getLocalPort());

        Thread Ta = new Thread();
        Ta = this.iniciar_smallAReceber(boundedB);
        Ta.start(); //iniciar o listener de pacotes
              
        Transporte t = new Transporte(initTransportePSH);
        Transporte t1 = new Transporte(initTransportePSH); 
        
        
        Console_println("SERVIDOR CONECTADO");
        
        
        if(this.enviarBytesSEND(bytes).equals(rtt_error)) {
            this.finalizar_smallAReceber(Ta); //finalizar o listener de pacotes
            return rtt_error;
        }
        
        this.finalizar_smallAReceber(Ta); //finalizar o listener de pacotes
        
        Console_println(timeout.value);
        
        return sucesso;
   }
   
   
   public String receberPorTCPsemOPEN() throws IOException{ //passive open
       Console_println("receiveSocket:"+this.receiveSocket.getLocalPort());
       Console_println("sendSocket:"+this.sendSocket.getLocalPort());
       
        Transporte t = new Transporte(initTransportePSH);
        Transporte t1 = new Transporte(initTransportePSH);
        

        Thread Ta = new Thread();
        Ta = this.iniciar_smallAReceber(boundedB);
        Ta.start(); //iniciar o listener de pacotes
        
        if(this.enviarBytesRECEIVE().equals(rtt_error)) {
            this.finalizar_smallAReceber(Ta); //finalizar o listener de pacotes
            return rtt_error;
        }
        
        this.finalizar_smallAReceber(Ta); //finalizar o listener de pacotes
        
        Console_println(timeout.value);
       
        return sucesso;
   }
   
   
   
   
   public Transporte recebeuACKde(Transporte t, InetAddress adress){
       Transporte t1=new Transporte(initTransportePSH);
       
       this.Recebidos.lock();
       ArrayList<DatagramPacket> lista = (ArrayList<DatagramPacket>) this.Recebidos.lista.clone();
       this.Recebidos.unlock();
       
       for(DatagramPacket p : lista){
           if(p.getAddress().equals(adress)){
                try { t1 = t1.receberDeDatagramPacket(p); } catch (IOException ex) {}
                
                int x = 1;
                //se enviarmos pacote com seq=0, o ACK recebido vai ter seqAck=1
                if( Transporte.byteToInt(t1.getSeqAck(), 2) == Transporte.byteToInt(t.getSeq(), 2) + x ){
                    this.Recebidos.lock();
                    this.Recebidos.lista.remove(p);
                    this.Recebidos.unlock();
                    return t1;
                    
                }
                    
           }
       }
       
       return null;
   }
   
   
   public Transporte recebeuACKdePSH(Transporte t, InetAddress adress){
       Transporte t1=new Transporte(initTransportePSH);
       
       this.Recebidos.lock();
       ArrayList<DatagramPacket> lista = (ArrayList<DatagramPacket>) this.Recebidos.lista.clone();
       this.Recebidos.unlock();
       
       for(DatagramPacket p : lista){
           if(p.getAddress().equals(adress)){
                try { t1 = t1.receberDeDatagramPacket(p); } catch (IOException ex) {}
                
                int x = Transporte.byteToInt(t.getSize(),2);
                //se enviarmos pacote com seq=0, o ACK recebido vai ter seqAck=1
                if( Transporte.byteToInt(t1.getSeqAck(), 2) == Transporte.byteToInt(t.getSeq(), 2) + x ){
                    this.Recebidos.lock();
                    this.Recebidos.lista.remove(p);
                    this.Recebidos.unlock();
                    return t1;
                    
                }
                    
           }
       }
       
       return null;
   }
   
   
   public Transporte recebeuPSH(int n_seq, InetAddress adress){
       Transporte t1=new Transporte(initTransportePSH);
       
       this.Recebidos.lock();
       ArrayList<DatagramPacket> lista = (ArrayList<DatagramPacket>) this.Recebidos.lista.clone();
       this.Recebidos.unlock();
       
       for(DatagramPacket p : lista){
           if(p.getAddress().equals(adress)){
                try { t1 = t1.receberDeDatagramPacket(p); } catch (IOException ex) {}
                
                
                //se enviarmos pacote com seq=0, o ACK recebido vai ter seqAck=1
                if( Transporte.byteToInt(t1.getSeq(), 2) == n_seq ){
                    this.Recebidos.lock();
                    this.Recebidos.lista.remove(p);
                    this.Recebidos.unlock();
                    return t1;
                    
                }

           }
       }
       
       return null;
   }
   
   
   
   public Transporte recebeuSYN(){
       Transporte t=new Transporte(initTransportePSH);
       
       this.Recebidos.lock();
       ArrayList<DatagramPacket> lista = (ArrayList<DatagramPacket>) this.Recebidos.lista.clone();
       this.Recebidos.unlock();
       
       for(DatagramPacket p : lista){
            try { t = t.receberDeDatagramPacket(p); } catch (IOException ex) {}

            if( t.eqFlag("SYN") ){
                this.Recebidos.lock();
                this.Recebidos.lista.remove(p);
                this.Recebidos.unlock();
                return t;
            }            
            
       }
   
       return null;
   }
   
   
   
   
   
   ////////////////////////////////////////////////////////////////////////////
   
   private void iniciar_smallTimeOut(BoundedBuffer b,IntObj timeout,int millisec) {
       timeout.value = 0;
       NOTIFYED = 0;
       Console_println("is waiting"); 
       b.BBDorme(millisec);
       Console_println("stopped waiting");
       if(NOTIFYED==0) timeout.value = 1;
       if(NOTIFYED==1) timeout.value = 0;
       
   }


   private Thread iniciar_smallAReceber(BoundedBuffer b) throws IOException{
       this.Recebidos.lista = new ArrayList<DatagramPacket>();
       END_AReceber = false;
       return new Thread(new SmallAReceber(b));
   }
   
   private void finalizar_smallAReceber(Thread t){
        END_AReceber = true;
        try { t.join(); } catch (InterruptedException ex) {}
   }
   
   
   class SmallAReceber implements Runnable {
       
       BoundedBuffer B;
       
       public SmallAReceber(BoundedBuffer b){
           B=b;
       }
       public void run(){ 
           while(!END_AReceber){
                try {
                    //try { Thread.sleep(1000); } catch (InterruptedException ex) {}
                    
                    //Console_println("AR1");
                    Transporte tr = new Transporte(initTransportePSH);
                    DatagramPacket p = tr.receberDatagramPacket(receiveSocket);
                    
                  if(p!=null){
                    
                    Console_println(p.getAddress() + "," + p.getPort());
                    
                    tr = tr.receberDeDatagramPacket(p); //teste
                    Console_println(tr.toString()); //teste
                    
                    if(sendIPAddress.equals(p.getAddress())) {
                        if(receivePort == -1) receivePort = p.getPort();
                        
                        if(p.getPort() == receivePort){ 
                            Recebidos.lock();
                            Recebidos.lista.add(p);
                            Recebidos.unlock();
                            NOTIFYED = 1; B.BBAcorda(); Console_println("ips/portas iguais");
                        }
                        
                    }
                    else Console_println("ips diferentes");
                  }
                    
                } catch (IOException ex) {}
           }
           
       }
       
       public void join(){    
           NOTIFYED = 1; 
           B.BBAcorda();
       }
       
   }

   
   
   class BoundedBuffer{
       public BoundedBuffer(){}
       
       public synchronized void BBDorme(int millisec){
            try { wait(millisec); } catch (InterruptedException ex) {}
       }
       
       public synchronized void BBAcorda(){
           notifyAll();
       }
   }
   
   
   
   
   
   public void fecharSockets(){
       this.sendSocket.close();
       this.receiveSocket.close();
   }
   
   
   
   public int MAQUINA=0;
   public void Console_println(Object o){
       
        Calendar cal = new GregorianCalendar();
    
        int min = cal.get(Calendar.MINUTE);             // 0..59
        int sec = cal.get(Calendar.SECOND);             // 0..59
        int ms = cal.get(Calendar.MILLISECOND);         // 0..999
    
        if(MAQUINA==1) try {Thread.sleep(5);} catch (InterruptedException ex){}
       //  if(MAQUINA==1) System.err.println(min+":"+sec+":"+ms+": "+o);
      // if(MAQUINA==2) System.out.println(min+":"+sec+":"+ms+": "+o);
     //  if(MAQUINA==3) System.out.println(min+":"+sec+":"+ms+":cliente 2: "+o);
     
   }
      
   
   public void Console_PrintPercentage(int bytes){
       if(MAQUINA!=1) progress.escreverProgresso(bytes);
       if(MAQUINA!=1) progress.progressReset();
   }
   
   
   
   public static void main(String[] args) throws UnknownHostException, SocketException, IOException{
       //ProtocoloTCP p = new ProtocoloTCP("localhost",9870,9876);
       
       //String s = "pacote";
       //p.enviarPorTCP(s.getBytes());
       
       
   }
   
   
}


