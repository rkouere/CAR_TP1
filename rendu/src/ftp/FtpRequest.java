/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/*
INFO : bonne spec ftp : https://tools.ietf.org/html/rfc2428

*/
package ftp;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that deals with all the connection request from an ftp client.
 * @author echallier
 */
public class FtpRequest extends Thread {
    // needed for the client's connection
    private Socket connServer;
    private InputStream is;
    private String msg;
    private BufferedReader br;

    /* passive */
    ServerSocket socketPassive = null;
    Boolean passiveCon = false;

    // list of users
    private Hashtable<String, String> users;
    private String userName;
    private Boolean loggedIn;
    private Boolean anonymousLogin;
   
    private String[] split = new String[2]; /*used for stocking the answers */
    private String directory;
    
    /* need for connecting with the client */
    private int clientPort;
    private String clientIP;    
    private Socket connData;
    
    /* info server */
    String ipServeur = new String();
    int [] portServeur = new int[2];
    
 
    public FtpRequest() {
        
    }
 /**
  * Initialises the variables to use in the thread.
  * @param conn The connection with the client
  * @param users The list of users and passwords allowed on the server
  */
    public void init(Socket conn, Hashtable<String, String> users) {
        this.connServer = conn;
        this.users = users;
        this.directory = System.getProperty("user.dir");
        /* we use this as the default value, just to be on the safe side ! */
        this.anonymousLogin = true;
    }

    /**
     * Manages the request from the client.
     * We therefore follow the protocol. i.e : we wait for a username, we check the password and we manage the requests.
     */
    public void run() {
        /* send "220 Service ready for new user" */
        connectionFtp();
        getMessage();
        processUSER(msg);
        getMessage();
        processPASS(msg);
        try {
            loggedInManager();
        } catch (IOException ex) {
            Logger.getLogger(FtpRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
 
    
    /**
     * This is the function that manages the command request from the client.
     * @throws IOException 
     */
    private void loggedInManager() throws IOException {
        while(this.loggedIn == true) {
            getMessage();
            this.split = msg.split(" ");
            switch(split[0]) {
                case "SYST":
                    sendMessageClient("215 UNIX");
                    break;
                case "QUIT":
                    processQUIT();
                    this.loggedIn = false;
                    break;
                case "RETR":
                    processRETR(this.split[1]);
                    break;
                 case "STOR": // connection passive
                    processSTOR(this.split[1]);
                    break;                   
//                case "EPRT": // connection active -> server doit stoquer information connection active. Repond OK. Il faut ouvrir connection
//                    processPASS(this.split[1]);
//                    this.loggedIn = false;
//                    break;
                case "PORT": // connection active
                    processPORT(this.split[1]);
                    break;
               case "EPSV": // connection passive
                    //initPassiveMode();
                    break;                
               case "PASV": // connection passive
                    processPASV();
                    break;                
              case "LIST": // connection passive
                    processLIST();
                    break;                            
              case "PWD": // connection passive
                    processPWD();
                    break;
                case "CWD": // connection passive
                    processCWD(this.split[1]);
                    break;
                case "CDUP": // connection passive
                    processCDUP();  
                    break;
               case "TYPE": // connection passive
                    processTYPEI();  
                    break;            
               default: 
                  sendMessageClient("502 Command not implemented");
                  break;
            }
        }
    }
    
 
    /**
     * Change the remote machine working directory to the parent of the current remote machine working directory.
     * If we are already at the root of the machine, we tell it to the user.
     * INFO : we decided to let the user go in the directory he wants on the machine. We could have manage a user's directory by setting a global variable 
     * "root" and checking that the user was always going in this directory.
     * @return false if we are at the root of the dir tree.
     */
    private Boolean processCDUP() {
        /* even though a client should not send an empty string, we check it nevertheless. Better beeing safe that sorry. */
         /* We then check that the directory exists */
        String [] dirTmp = new String[this.directory.length()];
        dirTmp = directory.split("\\/");
        /* if we are at the root of the directory */
        System.out.println(dirTmp.length);
        if(dirTmp.length < 2) {
            sendMessageClient("521 Current directory is root. Can't go further up the directory structure.");
            return false;
        }
        else {
            this.directory = "";
            for(int i = 0; i < dirTmp.length - 1; i++)
                this.directory += dirTmp[i] + "/";
            sendMessageClient("521 The current directory is now " + this.directory + ".");
            return true;
        }
    }
    /**
     * Change the current directory
     * If the directory does not exists, we send the appropriate message
     * @param msg The pathname of the directory the client wishes to access.
     * @return false if the directory does not exists or if the message sent by the client is empty.
     */
    private Boolean processCWD(String msg) {
        /* even though a client should not send an empty string, we check it nevertheless. Better beeing safe that sorry. */
        if(msg.isEmpty()) {
            sendMessageClient("521 We need the name of a directory.");
            return false;
        }
        /* We then check that the directory exists */
        File directory = new File(msg);
        if(!directory.exists()) {
            sendMessageClient("521 The directory " + msg + " does not exists.");
            return false;
        }
        else {
            this.directory = msg;
            sendMessageClient("200 directory changed to " + this.directory + ".");
            return true;
        }
    }
    /**
     * Sends a list of files/directory in the current directory.
     * @throws IOException  If an outputstream can't be opened
     */
    public void processLIST() throws IOException {
        // create a file that is really a directory
        File aDirectory = new File(this.directory);
        //get all the files from a directory
        File[] fList = aDirectory.listFiles();
        OutputStream dout= this.connData.getOutputStream();

        sendMessageClient("150 File status okay; about to open data connection.");
        /* We tell the client where we are. It's always important to know where we are ! */
        this.msg += ". Current dir = " + this.directory + "\r\n";
        for (File file : fList){
            this.msg += file.getName() + "\r\n";
        }
        byte[] msgToSend=msg.getBytes();
        dout.write(msgToSend,0,msgToSend.length);

        /* we tidy the connections */
        dout.close();
        if(this.passiveCon == false)
            connData.close();

        /* afficher un message de succès */
        sendMessageClient("226 Closing data connection file transfer successful");
    }
    /**
     * Store a file on the serveur.
     * If the user is in anonymous mode, he or she is forbidden to use this method.
     * @param msg The pathname of the file
     * @return false if a file with the requested name already exists/if an anonymous user is trying to store a file
     * @throws IOException If the FileOutputStream cannot be created
     */
       public Boolean processSTOR(String msg) throws IOException {
        if(this.anonymousLogin == true) {
            sendMessageClient("532 Need account for storing files.");
            return false;
        }
        else {
            /* we check that the file does not exists */
            File file = new File(this.directory + "/" + msg);
            if(file.exists()) {
                sendMessageClient("450 Requested file action not taken. Name already in use in this folder.");
                return false;
            }
            /* sinon on la cree */ 
            else {
                BufferedInputStream  fin = new BufferedInputStream(this.connData.getInputStream());
                //PrintStream dout = new PrintStream(connData.getOutputStream(),true,"data");
                OutputStream dout = new FileOutputStream(file);
                tranferData(fin, dout);

                if(this.passiveCon == false)
                    connData.close();

                /* afficher un message de succès */
                //sendMessageClient("226 Transfer complete !");
                sendMessageClient("250 Requested file action okay, completed.");
                return true;
            }
        }
    }
    /**
     * Send file to client.
     * @param msg The path of the file to send
     * @return true if the file was successfully sent
     * @return false if the file was not found on the server
     * @throws FileNotFoundException If the file that has to be created cannot be created.
     * @throws IOException If the OutputStream cannot be created
     */   
    public Boolean processRETR(String msg) throws FileNotFoundException, IOException {
        /* ouvre un stream */
        File f = new File(this.directory + "/" + msg);
        if(!f.exists()) {
            sendMessageClient("550 Requested action not taken. File unavailable (e.g., file not found, no access).");
            return false;
        }
        else {
            BufferedInputStream  fin = new BufferedInputStream(new FileInputStream(f));
            //PrintStream dout = new PrintStream(connData.getOutputStream(),true,"data");
            OutputStream dout= this.connData.getOutputStream();
            
            tranferData(fin, dout);
 
            if(this.passiveCon == false)
                connData.close();

            /* afficher un message de succès */
            //sendMessageClient("226 Transfer complete !");
            sendMessageClient("250 Requested file action okay, completed.");
            return true;
        }

    }
    
    /**
     * (Active mode) Instantiate a connection with a client
     * @throws IOException If the connection with the client could not be made
     */
    public void connectWithClient() throws IOException {
        connData =  new Socket(this.clientIP,this.clientPort);
        System.out.println("[server]: connected with " + this.clientIP + " sur port : " + this.clientPort);
        
    }
    
    /**
     * (Active mode) Process the information sent by the client to initiate a connection with the client.
     * @param msg The message sent from the client, but without the ftp command
     * @throws IOException if their was a connection failure
     */
    public void processPORT(String msg) throws IOException {
        /* we make sure that we are using the active mode */
        this.passiveCon = false;
        int[] clientPort = new int[2];
        /* the message sent back from the client contains first the ip adresse and then the port number used
            The last two numbers are formated in hex. To get the actual port number, we need to transform those two numbers in hex, add them and convert them to decimal.
        Ex : 4,15. 	
            first number (4) translate to hex (0x04)
            second number (15) translate to hex (0x0F)
            Now we need to take the entire set of hex bytes (0x040F) and translate the bytes from hex to decimal (1055). 
        Source : http://www.securitypronews.com/understanding-the-ftp-port-command-2003-09
        */
        String tmp[]=msg.split(",");
        /* we store the client's IP adresse */
        this.clientIP = tmp[0] + '.' + tmp[1] + '.' + tmp[2] + '.' + tmp[3];
           
        /* we get each number and transofmr them to hex */
        clientPort[0] = Integer.parseInt(tmp[tmp.length - 2]);
        clientPort[1] = Integer.parseInt(tmp[tmp.length - 1]);
        /* we add both number and transform them in decimal */
        this.clientPort = Integer.parseInt(Integer.toHexString(clientPort[0]) + Integer.toHexString(clientPort[1]), 16);
        connectWithClient();
        sendMessageClient("200 PORT command successful. ");
    }
    
 
    /**
     * 
     * (Passive mode) Waits for a connection from the client.
     * @throws IOException If the connection cannot be opened on the requested port
     */
    public void processPASV() throws IOException {
        int port[] = new int[2];
        port[0] = 175;
        port[1] = 16;
        System.out.println("port = " + ((port[0]*256) + port[1]));
        if(this.passiveCon == false) 
            this.socketPassive =  new ServerSocket(port[0]*256 + port[1]);
        
        sendMessageClient("227 Entering Passive Mode (127,0,0,1," + port[0] +"," + port[1] + ").");
        this.connData = socketPassive.accept();
        this.passiveCon = true;

    }
    
 
    /**
     * Close the connection with the client
     */
    private void processQUIT() {
        sendMessageClient("221 Service closing control connection.");
    }

   /**
     * Display the current working directory
     */
    private void processPWD() {
        sendMessageClient("257 " + this.directory + ".");
    }    
    /**
     * Check the password of the user trying to connect
     * @param msg the message sent from the client
     * @return true if the password is correct or if the password contains a @ for anonymous logins
     */
    private Boolean processPASS(String msg) {
        this.split = msg.split(" ");
        if(anonymousLogin) {
            /* I know, this is a bit ugly and is really not secured */
            if(this.split[1].contains("@")) {
                this.loggedIn = true;
                sendMessageClient("230 User logged in, proceed.");
                return true;
            }
            else {
                System.out.println("We need a mail adress to log you in.");
                sendMessageClient("530 Not logged in (we need an e mail adress to log you in).");
                return false;
            }
        }
        if(!split[1].equals(this.users.get(userName))) {
            System.out.println("utilisateur " + split[1] + " inconnu.");
            sendMessageClient("530 Not logged in.");
            /* we close the connection at the moment... later on, we could ask the user to try again */
            try {
                connServer.close();
            } catch (IOException ex) {
                Logger.getLogger(FtpRequest.class.getName()).log(Level.SEVERE, null, ex);
            }
            return false;
        }
        else {
            this.loggedIn = true;
            sendMessageClient("230 User logged in, proceed.");
            return true;
        }
    }

    
    /** 
     * Checks whether a user exists in the allowed user list.
     * @param msg the message received from the client
     * @return true if the user exists/anonymous login
     * @return false if the user does not exists
     */
    public Boolean processUSER(String msg) {
        this.split = msg.split(" ");
        //if(this.split[0] != "")
        this.userName = split[1];
        if(this.userName.contains("anonymous")) {
            anonymousLogin = true;
            sendMessageClient("331 User name okay, need password.");
            return true;
        }
        else if(!users.containsKey(userName)) {
            System.out.println("utilisateur " + split[1] + " inconnu.");
            sendMessageClient("226 Not logged in.");
            return false;
        }
        else {
            this.anonymousLogin = false;
            sendMessageClient("331 User name okay, need password.");
            return true;
        }
    }
    
    /*
    ------------------TOOLS------------------
    */
    


    /**
     * Tell the client that a connection has been initialised 
     */
    private void connectionFtp() {
        System.out.println("Connected");
        try {
            is = connServer.getInputStream();
        } catch (IOException ex) {
            Logger.getLogger(FtpRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
        sendMessageClient("220 Service ready for new user. Bonjour. L'utilisateur a utiliser est \"nico\" et son mot de passe est \"password\".");

    }
    /**
     * Sends data in byte format
     * @param fin a buffer Input Stream
     * @param dout a buffer Output Stream
     * @throws IOException If the program could not read from the the input stream or write in the output stream.
     */
    public void tranferData(BufferedInputStream  fin, OutputStream dout) throws IOException {
            sendMessageClient("150 File status okay; about to open data connection.");
            /* transmettre le fichier */
            byte[] buf = new byte[1024];
            int l = 0;
            while((l=fin.read(buf,0,1024))!=-1)
            {
                    dout.write(buf,0,l);
            }

            /* fermer les flux et le socket */
            fin.close();
            dout.close();
    }
   /**
    * Sends a message to the client with a hard return at the end.
    * @param msg The message to send
    */
    public void sendMessageClient(String msg) {
     OutputStream os = null;
     try {
         os = this.connServer.getOutputStream();
     } catch (IOException ex) {
         Logger.getLogger(FtpRequest.class.getName()).log(Level.SEVERE, null, ex);
     }
     DataOutputStream dos = new DataOutputStream(os);
     try {
         dos.writeBytes(msg + "\r\n");
     } catch (IOException ex) {
         Logger.getLogger(FtpRequest.class.getName()).log(Level.SEVERE, null, ex);
     }
     System.out.println("[server]: Sending answer \"" + msg + "\"");
    }
    /**
     * Get the message from the client and stores the message in this.msg
     */
    private void getMessage() {
        this.br = new BufferedReader(new InputStreamReader(this.is));
        try {
            this.msg = br.readLine();
        } catch (IOException ex) {
            Logger.getLogger(FtpRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("[client]: get message : \"" + msg + " \"");
    }

    private void processTYPEI() {
        sendMessageClient("200 Command OK");
    }
 
}

