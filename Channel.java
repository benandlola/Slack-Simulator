import java.net.*;
import java.io.*;
import java.util.*;

public class Channel extends Thread{

    private ServerSocket serverSocket = null;
    LinkedList<ClientHandler> clients = new LinkedList<ClientHandler>();

    public Channel(Integer port){
        try{
            serverSocket = new ServerSocket(port);
        }
        catch(Exception e) {
            System.err.println("Cannot establish server socket");
            System.exit(1);
        }
    }

    public void serve(){
        while(true){
            try{
                //accept incoming connection
                Socket clientSock = serverSocket.accept();
                System.out.println("New connection: " + clientSock.getRemoteSocketAddress());
                //start the thread
                ClientHandler client = new ClientHandler(clientSock);
                clients.add(client);
                client.start();
                //continue looping
            }
            catch(Exception e){}
        }
    }

    private class ClientHandler extends Thread{

        Socket socket;
        PrintWriter pw = null;
        BufferedReader in = null;

        public void run(){
            try{
                pw = new PrintWriter(socket.getOutputStream());
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                if(in.readLine().equals("NAME")){
                    this.setName(in.readLine());
                }

                //print out the client list
                for(ClientHandler client: clients){
                    client.pw.println("START_CLIENT_LIST");
                    for(ClientHandler user: clients){
                        client.pw.println(user.getName());
                    }
                    client.pw.println("END_CLIENT_LIST");
                    client.pw.flush();
                }
                //read and echo back forever!
                while(true){
                    String msg = in.readLine();
                    if(msg == null) break; //read null, remote closed
                    for(ClientHandler client: clients){
                        client.pw.println("[" + this.getName() + "] " + msg);
                        client.pw.flush();
                    }
                }

                //close the connections
                pw.close();
                in.close();
                socket.close();
                
            }
            catch(Exception e){
                System.err.println("Cannot Disconnect");
                System.exit(1);
            }

            //note the loss of the connection
            System.out.println("Connection lost: " + socket.getRemoteSocketAddress());
        
            for (ClientHandler client: clients){
                client.pw.println("START_CLIENT_LIST");
                for (ClientHandler user: clients){
                    if (user.getName().equals(this.getName())){
                        clients.remove(user);
                    }
                    else{
                        client.pw.println(user.getName());
                    }
                }
                client.pw.println("END_CLIENT_LIST");
                client.pw.flush();
            }
            
            //close the connections
            try {
                pw.close();
                in.close();
                socket.close();
            } 
            catch (Exception e) {
                System.err.println("Cannot Disconnect");
            }         
        }
    } 

    public static void main(String args[]){
        Channel server = new Channel(Integer.parseInt(args[0]));
        server.serve();
    }

}