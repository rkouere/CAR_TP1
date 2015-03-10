/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ftp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that will wait for a connection from an ftp client.
 * When connected, it will start a new object of the class FtPRequest.
 * @author echallier
 */
public class Serveur {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        int port = 4000; 
        Hashtable<String, String> users = new Hashtable<String, String>();
        ServerSocket socket = null;
        Socket conn;
        FtpRequest rq = null;
        
        users.put("nico", "password");

        /* we create a new socket listening on port 4000 */
        try {
            socket = new ServerSocket(port);
        } catch (IOException ex) {
            Logger.getLogger(Serveur.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        while(true) {
            /* we wait for an incoming connection */
            rq = new FtpRequest();
            try {
                conn = socket.accept();
                rq.init(conn, users);
            } catch (IOException ex) {
                Logger.getLogger(Serveur.class.getName()).log(Level.SEVERE, null, ex);
            }
            /* we launch a thread */
            rq.start();
        }
    }
 
    
}
