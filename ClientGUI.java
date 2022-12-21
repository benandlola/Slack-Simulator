import java.util.*;
import java.net.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ClientGUI extends JFrame{

    protected volatile boolean connected = false;
    protected Socket socket = null;
    protected Thread client = null;
    protected PrintWriter pw = null;
    protected BufferedReader in = null;
    protected TextArea messages;
    protected TextArea members;

    public ClientGUI() {
        super();
        this.setTitle("Slacky-- Slack Simulator (disconnected)");
        this.setSize(600, 350);
        this.setLocation(100, 100);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //The top panel
        JPanel top = new JPanel(new FlowLayout());
        JTextField name = new JTextField("", 10);
        JTextField address = new JTextField("", 16);
        JTextField port = new JTextField("", 5);
        JButton connect = new JButton("Connect");
        JButton disconnect = new JButton("Disconnect");

        top.add(new JLabel("Name"));
        top.add(name);
        top.add(new JLabel("IP Address"));
        top.add(address);
        top.add(new JLabel("Port"));
        top.add(port);
        top.add(connect);
        top.add(disconnect);
        disconnect.setVisible(false);
        this.add(top, BorderLayout.NORTH);

        //Members, Messages, and Compose
        JPanel mid = new JPanel(new FlowLayout());
        JPanel left = new JPanel(new FlowLayout());
        messages = new TextArea(9, 25);
        TextArea compose = new TextArea(3, 25);
        members = new TextArea(12, 10);
        left.add(new JLabel("Members Online"));
        left.add(members, BorderLayout.CENTER);
        members.setEditable(false);
        messages.setEditable(false);
        mid.add(new JLabel("Messages"));
        mid.add(messages, BorderLayout.NORTH);
        mid.add(new JLabel("Compose"));
        mid.add(compose, BorderLayout.SOUTH);
        this.add(left, BorderLayout.WEST);
        this.add(mid, BorderLayout.CENTER);

        //bottom panel
        JPanel bottom = new JPanel(new FlowLayout());
        JButton send = new JButton("Send");
        String[] topics = {"Dark", "Light", "Orange", "Blue", "Green"};
        JComboBox<String> themes = new JComboBox<String>(topics);
        bottom.add(themes, BorderLayout.WEST);
        bottom.add(send, BorderLayout.EAST);
        this.add(bottom, BorderLayout.SOUTH);

        //themes
        themes.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent a){
                String theme = (String)themes.getSelectedItem();
                Color background = Color.LIGHT_GRAY;

                if (theme.equals("Dark")){
                    background = Color.BLACK;
                }
                if (theme.equals("Light")){
                    background = Color.WHITE;
                }
                if (theme.equals("Orange")){
                    background = Color.ORANGE;
                }
                if (theme.equals("Blue")){
                    background = Color.BLUE;
                }
                if (theme.equals("Green")){
                    background = Color.GREEN;
                }
                top.setBackground(background);
                mid.setBackground(background);
                bottom.setBackground(background);
                left.setBackground(background);
            }
        });


        //client code
        connect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a){
                //CONNECT TO SERVER  
                try{
                    socket = new Socket(address.getText(), Integer.parseInt(port.getText()));
                }
                catch(Exception e) {
                    JFrame error = new JFrame("ERROR");
                    JOptionPane.showMessageDialog(error, "Cannot Establish Server Socket", "ERROR", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }

                try{
                    pw = new PrintWriter(socket.getOutputStream());
                    //Authentication
                    if (!(address.getText().equals("127.0.0.1"))){
                        pw.println("SECRET");
                        pw.println("3c3c4ac618656ae32b7f3431e75f7b26b1a14a87");
                        pw.println("NAME");
                        pw.println(name.getText());
                        pw.flush();
                    }
                    //for local
                    else{
                        pw.println("NAME");
                        pw.println(name.getText());
                        pw.flush();
                    }

                    connect.setVisible(false);
                    disconnect.setVisible(true);

                    setTitle("Slacky -- Slack Simulator (connected)");
                    connected = true;

                    //make text areas non-editable
                    name.setEditable(false);
                    address.setEditable(false);
                    port.setEditable(false);
                    messages.setEditable(false);
                    compose.setEditable(true);  

                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                    //messages to send on enter and click
                    compose.addKeyListener(new KeyAdapter() {
                        public void keyPressed(KeyEvent e){
                            if(e.getKeyCode() == KeyEvent.VK_ENTER){
                                pw.println(compose.getText());
                                pw.flush();
                            }
                        }
                        public void keyReleased(KeyEvent d){
                            if(d.getKeyCode() == KeyEvent.VK_ENTER){
                                compose.setText("");
                            }
                        }
                    });

                    send.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent send){
                            pw.println(compose.getText());
                            pw.flush();
                            compose.setText("");
                        }
                    });
            
                    client = new Client();
                    client.start();

                    //DISCONNECT FROM SERVER
                    disconnect.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent a){
                            try{
                                //close socket
                                connected = false;
                                pw.close();
                                in.close();
                                socket.close();
    
                                pw = null;
                                in = null;
                                socket = null;
                                client = null;
    
                                //clear texts
                                name.setText("");
                                address.setText("");
                                port.setText("");
                                messages.setText("");
                                members.setText("");
                                connect.setVisible(true);
                                disconnect.setVisible(false);
                                setTitle("Slacky -- Slack Simulator (disconnected)");
                                name.setEditable(true);
                                address.setEditable(true);
                                port.setEditable(true);
                                messages.setEditable(false);
                                compose.setEditable(false);
                            }
                            catch(Exception e){
                                JFrame error = new JFrame("ERROR");
                                JOptionPane.showMessageDialog(error, "Disconnected from server", "ERROR", JOptionPane.ERROR_MESSAGE);
                                System.exit(1);
                            }
                        }
                    });   
                }
                catch (Exception e) {
                    JFrame error = new JFrame("ERROR");
                    JOptionPane.showMessageDialog(error, "IO Exception", "ERROR", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                } 
            }
        });
    }

    //private class for connection
    private class Client extends Thread{
        public Client(){
            super();
        }
        public void run(){
            try{
                String msgs = "";
                String msg = "";
                while(connected){
                    if(in.ready()){
                        msg = in.readLine();
                    }
                    else { continue; }
                    if(msg.equals("START_CLIENT_LIST")){
                        msg = in.readLine();
                        String names = "";
                        while(!msg.equals("END_CLIENT_LIST")){
                            names = names.concat(msg + '\n');
                            msg = in.readLine();
                        }
                        members.setText(names);
                    }
                    else{
                        msgs = msgs.concat(msg + '\n');
                        messages.setText(msgs);
                    }
                }
            }
            catch (Exception e) {
                JFrame error = new JFrame("ERROR");
                JOptionPane.showMessageDialog(error, "IO Exception", "ERROR", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }
    }

    public static void main(String args[]){
        ClientGUI gui = new ClientGUI();
        gui.setVisible(true);  
    }
}