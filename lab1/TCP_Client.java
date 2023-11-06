import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

class Receiver extends Thread{
    private final TCP_Client tcp;
    private final Socket socket;
    private final BufferedReader br;

    public Receiver(TCP_Client tcp, Socket socket) throws IOException{
        this.tcp = tcp;
        this.socket = socket;
        this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public void run(){
        try{
            System.out.println(tcp.get_timestamp() + "\nReceiver worker started.");
            String line;
            while(true){
                line = br.readLine();
                System.out.println(tcp.get_timestamp() + "\nReceived: " + line);
                tcp.resetTimeout(socket);
            }
        } catch (SocketTimeoutException e){
            try{
                System.out.println(TCP_Client.TIMEOUT_SECONDS + " seconds of inactivity passed. Receiver for " +
                        tcp.get_name() + " on " + socket.getPort() + " has been terminated.");
                tcp.connections.entrySet().removeIf(x -> x.getValue().equals(socket));
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
    private final TCP_Client tcp;
    private final BufferedReader br;
    private final PrintWriter bw;

    public Sender(TCP_Client tcp, Socket socket) throws IOException{
        this.tcp = tcp;
        this.br = new BufferedReader(new InputStreamReader(System.in));
        this.bw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    @Override
    public void run(){
        try{
            System.out.println(tcp.get_timestamp() + "\nSender worker started.");
            String line;
            while(true){
                TCP_Client.lock.acquire(1);
                System.out.println("\tSender for " + tcp.get_name());
                if((line = br.readLine()) != null){
                    if(line.startsWith("connect:")){
                        String[] parts = line.split(":");
                        if (parts.length == 2 && parts[1].equals(tcp.get_name())){
                            System.out.println(tcp.get_timestamp() + "\nSent connection message.");
                        }
                        else {
                            System.out.println(tcp.get_timestamp() + "\nAttempted to send invalid connection string.");
                        }
                    }
                    bw.println(line);
                    bw.flush();
                }
                // release the Std.In block and let time for the other client to acquire the lock
                TCP_Client.lock.release(1);
                Thread.sleep(100);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}

public class TCP_Client extends Thread{
    public final static int TIMEOUT_SECONDS = 10;
    private final String my_name;
    public String get_name(){
        return my_name;
    }
    private final ServerSocket server_socket;
    public HashMap<String, Socket> connections;
    private final InetAddress dest_address;
    private final int dest_port;
    public static Semaphore lock = new Semaphore(1);

    public TCP_Client(String my_name, int my_port, String dest_address, int dest_port) throws IOException{
        this.server_socket = new ServerSocket(my_port);
        this.my_name = my_name;
        connections = new HashMap<>();
        this.dest_address = InetAddress.getByName(dest_address);
        this.dest_port = dest_port;
    }

    @Override
    public void run(){
        try{
            Sender sender = new Sender(this, new Socket(dest_address, dest_port));
            sender.start();
            String line;
            BufferedReader br;
            while(true){
                Socket socket = server_socket.accept();
                br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                line = br.readLine();
                if (line.startsWith("connect:")){
                    String[] parts = line.split(":");
                    if (parts.length > 2 || parts[1].equals(my_name)){
                        System.out.println(get_timestamp() + "\nReceived invalid connection string.");
                        socket.close();
                    } else {
                        Receiver receiver = new Receiver(this, socket);
                        receiver.start();
                        System.out.println(get_timestamp() + "\nConnection string accepted. A receiver has been assigned to " +
                                "the connection, and will shut down after " + TIMEOUT_SECONDS + " seconds of inactivity.");
                        this.connections.put(parts[1], socket);
                        break;
                    }
                } else{
                    socket.close();
                }
            }
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public void resetTimeout(Socket socket) throws SocketException{
        socket.setSoTimeout(TIMEOUT_SECONDS * 1000);
    }

    protected String get_timestamp(){
        return my_name + "@[" + new Timestamp(System.currentTimeMillis()) + "]";
    }

    public static void main(String[] args) throws Exception{
        TCP_Client c1 = new TCP_Client("Alice",9753,"localhost",3579);
        TCP_Client c2 = new TCP_Client("Bob",3579,"localhost",9753);
        c1.start();
        c2.start();
    }
}
