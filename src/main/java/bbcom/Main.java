package bbcom;

import com.google.gson.Gson;
import httpCom.SparkHTTPServlet;
import lib.Com;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.net.DatagramSocket;
import java.util.Enumeration;

public class Main {



    public static void main(String[] args) throws IOException {

        String mode = "";
        if(args.length>=1)
            mode = args[0];
        //SparkHTTPServlet.HttpConnection rc= new Gson().fromJson(remoteNodeAddr, SparkHTTPServlet.HttpConnection.class);

        /*String line = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while(!line.trim().equals("exit")){
            line = reader.readLine();
            String[] sa = line.split(" ");
            String com = line.substring(0,line.indexOf(" "));


        }*/






        try {
            int port = 8901;
            String nodeName = "node_1";
            Com c = new SparkHTTPServlet(/*addresses,*/port,nodeName);
            //boolean reacheable = ((SparkHTTPServlet)c).testPort();
            c.start();
            c.join();
            System.out.println("=======================================");
            System.out.println("NODE ADDRESS => "+new Gson().toJson(c.getProcessConnectionDescriptor(c.getName()), SparkHTTPServlet.HttpConnection.class));
            System.out.println("=======================================");


            if(mode.equals("primary")) {
                try {
                    System.out.println("REMOTE ADDRESS");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    SparkHTTPServlet.HttpConnection rc = new Gson().fromJson(reader.readLine(), SparkHTTPServlet.HttpConnection.class);
                    String cName = c.launchModule("BBoCoordinator", new String[]{"-apath", "modules/coordinator/"});
                    //--instantiate  -> sending request to nodes
                    //one local worker
                    String wName = c.launchModule("BBOSlave", new String[]{});
                    //two rwmote worker
                    String rwName = c.launchRemoteModule(rc, "BBOSlave", new String[]{});
                    String rwName2 = c.launchRemoteModule(rc, "BBOSlave", new String[]{});

                    //String wName2 = c.launchModule("BBOSlave",null);
                    SparkHTTPServlet.HttpConnection conn = (SparkHTTPServlet.HttpConnection) c.getProcessConnectionDescriptor(wName);
                    conn.type = SparkHTTPServlet.HttpConnectionType.BIDIRECT;


                    SparkHTTPServlet.HttpConnection rconn1 = (SparkHTTPServlet.HttpConnection) c.calculateRemoteProcessConnectionDescriptor(rwName, rc);
                    SparkHTTPServlet.HttpConnection rconn2 = (SparkHTTPServlet.HttpConnection) c.calculateRemoteProcessConnectionDescriptor(rwName2, rc);

                    // the coordinator sets up connections to the workers
                    c.addBidirectionalChannel(rconn1, cName);
                    c.addBidirectionalChannel(rconn2, cName);
                    c.addBidirectionalChannel(conn, cName);

                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
