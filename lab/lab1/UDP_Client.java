package lab1;

import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.util.Arrays;

class SenderWorker extends Thread{
    private final String cname;
    private final DatagramSocket socket;
    private final InetAddress dest_address;
    private final int dest_port;

    SenderWorker(String cname, DatagramSocket socket, InetAddress dest_address, int dest_port){
        this.cname = cname;
        this.socket = socket;
        this.dest_address = dest_address;
        this.dest_port = dest_port;
    }

    @Override
    public void run(){
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try{
            System.out.println(get_timestamp() + "\nlab1.Sender worker started.");
            String line;
            while((line = br.readLine()) != null){
                System.out.println(get_timestamp() + "\nSending: " + line);
                byte[] bytes = line.getBytes();
                DatagramPacket p = new DatagramPacket(bytes, bytes.length, dest_address, dest_port);
                socket.send(p);
                Thread.sleep(1000);
            }
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private String get_timestamp(){
        return cname + "@[" + new Timestamp(System.currentTimeMillis()) + "]";
    }
}

class ReceiverWorker extends Thread{
    private final String cname;
    private final DatagramSocket socket;
    private DatagramPacket p;
    private byte[] buffer;

    ReceiverWorker(String cname, DatagramSocket socket){
        this.cname = cname;
        this.socket = socket;
        this.buffer = new byte[256];
    }

    @Override
    public void run(){
        try{
            System.out.println(get_timestamp() + "\nlab1.Receiver worker started.");
            while(true){
                p = new DatagramPacket(buffer, buffer.length);
                socket.receive(p);
                String received = new String(Arrays.copyOfRange(p.getData(), 0, p.getLength()));
                System.out.println(get_timestamp() + "\nReceived: " + received);
            }
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }
    private String get_timestamp(){
        return cname + "@[" + new Timestamp(System.currentTimeMillis()) + "]";
    }
}

public class UDP_Client extends Thread{
    private final String my_name;
    private final DatagramSocket socket;
    private final InetAddress dest_address;
    private final int dest_port;

    public UDP_Client(String my_name, int my_port, String dest_address, int dest_port) throws IOException{
        this.my_name = my_name;
        this.socket = new DatagramSocket(my_port);
        this.dest_address = InetAddress.getByName(dest_address);
        this.dest_port = dest_port;
    }

    @Override
    public void run(){
        ReceiverWorker receiver = new ReceiverWorker(my_name, socket);
        InetAddress send_address = (socket.getInetAddress() != null) ? socket.getInetAddress() : dest_address;
        int send_port = (socket.getPort() != -1) ? socket.getPort() : dest_port;
        SenderWorker sender = new SenderWorker(my_name, socket, send_address, send_port);
        receiver.start();
        sender.start();
    }

    public static void main(String[] args) throws Exception{
        UDP_Client c1 = new UDP_Client("Alice",9753,"localhost",3579);
        UDP_Client c2 = new UDP_Client("Bob",3579,"localhost",9753);
        c1.start();
        c2.start();
    }

}
