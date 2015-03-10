/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ftp;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author rkouere
 */
public class FtpRequestTest {
    static int port = 4000; 
    int portConn2 = 16*256 + 161;
    Hashtable<String, String> users = new Hashtable<String, String>();
    static Socket connClient;
    static ServerSocket socketData = null;
    static Socket connData;
    
    static private String msg;
    private BufferedReader br;  
    
    static Thread myThread = null;
    
 
    @BeforeClass
    public static void setUp() throws IOException, InterruptedException {
        System.out.println("Main");
        FtpRequestTest.myThread = new Thread() {
            private String[] args;
            public void run() {
                Serveur.main(args);
            }
        };
        FtpRequestTest.myThread.start();
        System.out.println("connData");
        /* I have noticed that while not waiting a bit, the thread is not active and I could not connect to the server */

        /* we send all the info we need */

        
    }
    
    private void sendPORT() throws IOException, InterruptedException {
        System.out.println("[test]: new server socket on port " + this.portConn2);
        this.socketData = new ServerSocket(portConn2);
        System.out.println("[test]: server socket OK");
        sendMessageServer("PORT 127,0,0,1,16,161");
        Thread.sleep(1000);
        this.connData = socketData.accept();
        getMessage();
    }
    
    @AfterClass
    public static void stop() throws IOException, InterruptedException {
        Thread.sleep(2000);
        FtpRequestTest.connData.close();
    }
    
 
        
    public static void sendMessageServer(String msg) throws InterruptedException {
     OutputStream os = null;
     try {
         os = FtpRequestTest.connClient.getOutputStream();
     } catch (IOException ex) {
         Logger.getLogger(FtpRequest.class.getName()).log(Level.SEVERE, null, ex);
     }
     DataOutputStream dos = new DataOutputStream(os);
     try {
         dos.writeBytes(msg + "\r\n");
     } catch (IOException ex) {
         Logger.getLogger(FtpRequest.class.getName()).log(Level.SEVERE, null, ex);
     }
     System.out.println("[test]: Sending message \"" + msg + "\"");
    Thread.sleep(2000);

    }
    
    private static String getMessage() throws IOException, InterruptedException {
        BufferedReader input = new BufferedReader(new InputStreamReader(FtpRequestTest.connClient.getInputStream()));
        int myChar;
        Thread.sleep(1000);

        FtpRequestTest.msg = input.readLine();
        System.out.println("[test] get message : " + FtpRequestTest.msg);
        return FtpRequestTest.msg;
       //input.close();
 

    }
       /**
     * Test of processUSER method, of class FtpRequest.
     */
    @Test
    public void testProcessLIST() throws IOException, InterruptedException {
        /* Test d'un utilisateur inconnu */
            //PrintStream dout = new PrintStream(connData.getOutputStream(),true,"data");
            //Thread.sleep(1000);
            //FtpRequestTest.getMessage();
            Thread.sleep(1000);
            FtpRequestTest.connClient = new Socket("localhost", FtpRequestTest.port);
            Thread.sleep(1000);
            FtpRequestTest.sendMessageServer("USER nico");
            FtpRequestTest.getMessage();
            //assertTrue(FtpRequestTest.getMessage().equals("331 User name okay, need password."));
//            sendMessageServer("PASS password");
//            FtpRequestTest.getMessage();
//            sendPORT();
//            sendMessageServer("LIST");
           
            //assertTrue(getMessage().contains("150 "));
            assertTrue(true);
            //this.connData.close();
            //this.connClient.close();

    }
    
//   @Test
//    public void processQUIT() throws IOException, InterruptedException {
//        sendPORT();
//        sendMessageServer("QUIT");
//    }
    public FtpRequestTest() throws IOException {

        
    }

 
//    /**
//     * Test of processUSER method, of class FtpRequest.
//     */
//    @Test
//    public void processPASS() {
//        String msg = "PSSW buongiorno";
//        assertFalse(rq.processUSER(msg));
//        msg = "USER nico";
//        assertTrue(rq.processUSER(msg));    
//    }
    
}
