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
import java.io.File;
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
    static Socket connClient;
    static ServerSocket socketData = null;
    static Socket connData;
    static String currentDirectory = new String();
    static String rootDirectory = new String();

    static private String msg;
    
    
 
    @BeforeClass
    public static void setUp() throws IOException, InterruptedException {
        FtpRequestTest.connClient = new Socket("localhost", FtpRequestTest.port);
        FtpRequestTest.getMessage();
        FtpRequestTest.sendMessageServer("USER nico");
        FtpRequestTest.getMessage();
        FtpRequestTest.sendMessageServer("PASS password");
        FtpRequestTest.getMessage();
        FtpRequestTest.sendMessageServer("PWD");
        FtpRequestTest.rootDirectory = FtpRequestTest.getMessage().split(" ")[1];       
    }
    
   
    @AfterClass
    public static void stop() throws IOException, InterruptedException {
        Thread.sleep(2000);
        FtpRequestTest.connClient.close();
    }
 
 
    /**
     * Test la commande cdup
     * @throws InterruptedException
     * @throws IOException 
     */
    @Test
    public void CDUPTest() throws InterruptedException, IOException {
        FtpRequestTest.sendMessageServer("PWD");
        FtpRequestTest.currentDirectory = FtpRequestTest.getMessage().split(" ")[1];

        String[] tmp = FtpRequestTest.currentDirectory.split("\\/");
        String targetFolder = new String();
        String replyFtp = new String();
        /* the name of the directory we are supposed to be in after the command */
        for (int i = 0; i < tmp.length - 1; i++) {
            targetFolder += tmp[i] + "/";
        }
        FtpRequestTest.sendMessageServer("CDUP");
        tmp = FtpRequestTest.getMessage().split(" ");
        replyFtp = tmp[tmp.length - 1];
        assertTrue(replyFtp.equals(targetFolder + "."));

    }
    /**
     * Test changement de dossier
     * @throws InterruptedException
     * @throws IOException 
     */
    @Test
    public void CWDTest() throws InterruptedException, IOException {
        System.out.println("!!!! CWDTest");
        /* the list we are supposed to get back */
        String replyFtp = new String();

        /* on s'assure que l'on est pas dans le dossier par default */
        FtpRequestTest.sendMessageServer("CDUP");
        FtpRequestTest.getMessage();
        /* on va dans le dossier root */
        FtpRequestTest.sendMessageServer("CWD " + FtpRequestTest.rootDirectory);
        String[] tmp = FtpRequestTest.getMessage().split(" ");
        replyFtp = tmp[tmp.length - 1];
        assertTrue(replyFtp.equals(FtpRequestTest.rootDirectory + "."));

    }
 
    /**
     * test de la commande LIST
     * @throws IOException
     * @throws InterruptedException 
     */
    @Test
    public void LISTTest() throws IOException, InterruptedException {
        /* the list we are supposed to get back */
        String[] resultExpected = new String[100];
        String resultFromFtp = new String();
        resultExpected[0] = "dist";
        Boolean resultBool = true;
        FtpRequestTest.sendMessageServer("PWD");
        FtpRequestTest.currentDirectory = FtpRequestTest.getMessage().split(" ")[1];
        /* we get the list of the files in the directory */
        File folder = new File(FtpRequestTest.currentDirectory);
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            resultExpected[i] = listOfFiles[i].getName();
        }
    
        sendPORT();
        FtpRequestTest.getMessage();
        FtpRequestTest.sendMessageServer("LIST");
        resultFromFtp = FtpRequestTest.getMessagePort();
        FtpRequestTest.getMessage();
        /* we check that all the files/folders in the directory are what we get back from the ftp */
        for (int i = 0; i < listOfFiles.length; i++) {
            if(resultFromFtp.contains(resultExpected[i]) == false)
                resultBool = false;
        }
        assertTrue(resultBool);

    }
    

    
    private void sendPORT() throws IOException, InterruptedException {
        System.out.println("[test]: new server socket on port " + this.portConn2);
        this.socketData = new ServerSocket(portConn2);
        System.out.println("[test]: server socket OK");
        sendMessageServer("PORT 127,0,0,1,16,161");
        Thread.sleep(1000);
        this.connData = socketData.accept();
        //getMessage();
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
     System.out.println("[test] sending message \"" + msg + "\"");
    Thread.sleep(2000);

    }
    /**
     * Reads a message from a data transfer. prints the message and returns it
     * @return the message
     * @throws IOException
     * @throws InterruptedException 
     */
    private static String getMessagePort() throws IOException, InterruptedException {
        BufferedReader input = new BufferedReader(new InputStreamReader(FtpRequestTest.connData.getInputStream()));
        String tmp = new String();

        while((tmp = input.readLine()) != null) {
            FtpRequestTest.msg += tmp;
            FtpRequestTest.msg += "\n";
        };
        System.out.println("[test] get message : " + FtpRequestTest.msg);
        FtpRequestTest.connData.close();
        return FtpRequestTest.msg;
       //input.close();
    }
    private static String getMessage() throws IOException, InterruptedException {
        BufferedReader input = new BufferedReader(new InputStreamReader(FtpRequestTest.connClient.getInputStream()));
        int myChar;
        //Thread.sleep(500);

        FtpRequestTest.msg = input.readLine();
        System.out.println("[test] get message : " + FtpRequestTest.msg);
        return FtpRequestTest.msg;
       //input.close();
    }
    
}
