package lab2;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

class Receiver extends Thread{
    private final String cname;
    private final String oname;
    private final Socket socket;
    private final BufferedReader br;
    private boolean is_connected = false;
    Message message;

    public Receiver(String cname, String oname, Socket socket) throws IOException{
        this.cname = cname;
        this.oname = oname;
        this.socket = socket;
        this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public void run(){
        try{
            String line;
            while(true){
                message = Message.fromJson(br.readLine());
                line = message.messageText;
                if (!is_connected){
                    if (line.startsWith("connect:")){
                        String[] parts = line.split(":");
                        if (parts.length > 2 || parts[1].equals(cname)){
                            System.out.println("Received invalid connection string.");
                        } else {
                            this.is_connected = true;
                            System.out.println("Connected to " + parts[1]);
                        }
                    }
                }
                else {
                    System.out.println(message);
                    TCP_Serialize.resetTimeout(socket);
                }
            }
        } catch (SocketTimeoutException e){
            try{
                System.out.println(TCP_Serialize.TIMEOUT_SECONDS + " seconds of inactivity passed. Receiver for " +
                        oname + " has been terminated.");
                br.close();
                socket.close();
            } catch (IOException ex){
                throw new RuntimeException(ex);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}

class Sender extends Thread{
    private final String cname;
    private final String oname;
    private final BufferedReader br;
    private final PrintWriter pw;
    Message message;

    public Sender(String cname, String oname, Socket socket) throws IOException{
        this.cname = cname;
        this.oname = oname;
        this.br = new BufferedReader(new InputStreamReader(System.in));
        this.pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
    }

    @Override
    public void run(){
        try{
            String line;
            while(true){
                TCP_Serialize.lock.acquire(1);
                System.out.println(cname+":");
                if((line = br.readLine()) != null){
                    // if line is in format: file:<file-path>
                    // NOTE: included example file.txt on path lab/lab2/file.txt
                    if (line.startsWith("file:")){
                        String file_path = line.substring(5);
                        try(BufferedReader bfr = new BufferedReader(new FileReader(file_path))){
                            String file_line;
                            String[] parts = file_path.split("[\\\\/]"); // backward or forward slash
                            message = new Message(cname, oname, parts[parts.length-1], new ArrayList<>());
                            while((file_line = bfr.readLine()) != null){
                                message.fileContents.add(file_line);
                            }
                        } catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                    // regular text message
                    else {
                        message = new Message(cname, oname, line);
                    }
                    String json = message.toJson();
                    pw.println(json);
                }
                // release the Std.In block and let time for the other client to acquire the lock
                TCP_Serialize.lock.release(1);
                Thread.sleep(100);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}

public class TCP_Serialize extends Thread{
    public final static int TIMEOUT_SECONDS = 60;
    private final String my_name;
    private final String other_name;
    private final ServerSocket server_socket;
    private final InetAddress dest_address;
    private final int dest_port;
    public static Semaphore lock = new Semaphore(1);

    public TCP_Serialize(String my_name, String other_name, int my_port, String dest_address, int dest_port) throws IOException{
        this.server_socket = new ServerSocket(my_port);
        this.my_name = my_name;
        this.other_name = other_name;
        this.dest_address = InetAddress.getByName(dest_address);
        this.dest_port = dest_port;
    }

    @Override
    public void run(){
        try{
//            Thread.sleep(10000); // time to open the other client if testing from 2 processes
            Sender sender = new Sender(my_name, other_name, new Socket(dest_address, dest_port));
            sender.start();
            while(true){
                Socket socket = server_socket.accept();
                Receiver receiver = new Receiver(my_name, other_name, socket);
                receiver.start();
            }
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static void resetTimeout(Socket socket) throws SocketException{
        socket.setSoTimeout(TIMEOUT_SECONDS * 1000);
    }

    public static void main(String[] args) throws Exception{
        TCP_Serialize c1 = new TCP_Serialize("Alice","Bob",9753,"localhost",3579);
        TCP_Serialize c2 = new TCP_Serialize("Bob","Alice",3579,"localhost",9753);
        c1.start();
        c2.start();
    }
}
