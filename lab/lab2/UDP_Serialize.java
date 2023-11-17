package lab2;

import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.util.Arrays;

class SenderWorker extends Thread{
    private final String cname;
    private final String oname;
    private final DatagramSocket socket;
    private final InetAddress dest_address;
    private final int dest_port;
    Message message;

    SenderWorker(String cname, String oname, DatagramSocket socket, InetAddress dest_address, int dest_port){
        this.cname = cname;
        this.oname = oname;
        this.socket = socket;
        this.dest_address = dest_address;
        this.dest_port = dest_port;
    }

    @Override
    public void run(){
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))){
            String line;
            while((line = br.readLine()) != null){
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                message = new Message(cname, oname, line);
                oos.writeObject(message);
                oos.flush();
                byte[] bytes = bos.toByteArray();
                DatagramPacket p = new DatagramPacket(bytes, bytes.length, dest_address, dest_port);
                socket.send(p);
                Thread.sleep(1000);
            }
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}

class ReceiverWorker extends Thread{
    private final DatagramSocket socket;
    private DatagramPacket p;
    private byte[] buffer;

    private Message message;

    ReceiverWorker(DatagramSocket socket){
        this.socket = socket;
        this.buffer = new byte[1000];
    }

    @Override
    public void run(){
        try{
            while(true){
                p = new DatagramPacket(buffer, buffer.length);
                socket.receive(p);
                byte[] bytes = p.getData();
                ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                ObjectInputStream ois = new ObjectInputStream(bis);
                message = (Message) ois.readObject();
                System.out.println(message);
            }
        } catch (IOException | ClassNotFoundException e){
            throw new RuntimeException(e);
        }
    }
}

public class UDP_Serialize extends Thread{
    private final String my_name;
    private final String other_name;
    private final DatagramSocket socket;
    private final InetAddress dest_address;
    private final int dest_port;

    public UDP_Serialize(String my_name, String other_name, int my_port, String dest_address, int dest_port) throws IOException{
        this.my_name = my_name;
        this.other_name = other_name;
        this.socket = new DatagramSocket(my_port);
        this.dest_address = InetAddress.getByName(dest_address);
        this.dest_port = dest_port;
    }

    @Override
    public void run(){
        ReceiverWorker receiver = new ReceiverWorker(socket);
        SenderWorker sender = new SenderWorker(my_name, other_name, socket, dest_address, dest_port);
        receiver.start();
        sender.start();
    }

    public static void main(String[] args) throws Exception{
        UDP_Serialize c1 = new UDP_Serialize("Alice","Bob",9753,"localhost",3579);
        UDP_Serialize c2 = new UDP_Serialize("Bob","Alice",3579,"localhost",9753);
        c1.start();
        c2.start();
    }

}

