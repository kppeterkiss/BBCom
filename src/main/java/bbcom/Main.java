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
        String connectionUrl = "";
        if(args.length==1)
            mode = args[0];
        if(args.length==2 && args[0].equals("-c")){
            connectionUrl = args[1];
        }
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
            String nodeName = "node_2";
            Com c = new SparkHTTPServlet(/*addresses,*/port,nodeName);
            //boolean reacheable = ((SparkHTTPServlet)c).testPort();
            c.start();
            c.join();
            System.out.println("=======================================");
            System.out.println("NODE ADDRESS => "+new Gson().toJson(c.getProcessConnectionDescriptor(c.getName(), SparkHTTPServlet.HttpConnectionType.NODE), SparkHTTPServlet.HttpConnection.class));
            System.out.println("=======================================");

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String userinput ="";

            if(mode.equals("primary")) {
                try {

                    String cName = c.launchModule("BBoCoordinator", new String[]{"-apath", "modules/coordinator/"});
                    //--instantiate  -> sending request to nodes
                    //one local worker
                    String wName = c.launchModule("BBOSlave", new String[]{});


                    //two remote worker

                    System.out.println("REMOTE ADDRESS");
                    userinput = reader.readLine();
                    SparkHTTPServlet.HttpConnection rc = new Gson().fromJson(userinput, SparkHTTPServlet.HttpConnection.class);
                   String rwName = c.launchRemoteModule(rc, "BBOSlave", new String[]{});
                    String rwName2 = c.launchRemoteModule(rc, "BBOSlave", new String[]{});

                    //String wName2 = c.launchModule("BBOSlave",null);



                    SparkHTTPServlet.HttpConnection rconn1 = (SparkHTTPServlet.HttpConnection) c.calculateRemoteProcessConnectionDescriptor(rwName, rc);
                    SparkHTTPServlet.HttpConnection rconn2 = (SparkHTTPServlet.HttpConnection) c.calculateRemoteProcessConnectionDescriptor(rwName2, rc);

                    // the coordinator sets up connections to the workers
                    c.addBidirectionalChannel(rconn1, cName);
                    c.addBidirectionalChannel(rconn2, cName);

                    SparkHTTPServlet.HttpConnection conn = (SparkHTTPServlet.HttpConnection) c.getProcessConnectionDescriptor(wName, SparkHTTPServlet.HttpConnectionType.BIDIRECT);

                    //conn.type = SparkHTTPServlet.HttpConnectionType.BIDIRECT;
                    c.addBidirectionalChannel(conn, cName);

                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }// we connect to an existing node
            else if(!connectionUrl.equals("")){
                SparkHTTPServlet.HttpConnection nodeConnectionToConnect = new Gson().fromJson(connectionUrl, SparkHTTPServlet.HttpConnection.class);
                if(nodeConnectionToConnect.type.equals(SparkHTTPServlet.HttpConnectionType.NODE))
                    nodeConnectionToConnect.type = SparkHTTPServlet.HttpConnectionType.NODE;
                c.connectToNetwork(nodeConnectionToConnect/*, c.getName()*/);


            }
            else if (mode.equals("local")){
                String cName = c.launchModule("BBoCoordinator", new String[]{"-apath", "modules/coordinator/"});
                //--instantiate  -> sending request to nodes
                //one local worker
                String wName = c.launchModule("BBOSlave", new String[]{});


                SparkHTTPServlet.HttpConnection conn = (SparkHTTPServlet.HttpConnection) c.getProcessConnectionDescriptor(wName, SparkHTTPServlet.HttpConnectionType.BIDIRECT);
                //conn.type = SparkHTTPServlet.HttpConnectionType.BIDIRECT;
                c.addBidirectionalChannel(conn, cName);
            }
            else if(mode.equals("starter")){
                String c_name = "";
                while (!userinput.equals("STOP")) {
                    userinput = reader.readLine();
                    System.out.println("user input : "+userinput);
                    //at the coordinator node we build up the network
                    if(userinput.toUpperCase().equals("BUILD"))
                        c_name = ((SparkHTTPServlet)c).buildNetwork();
                }
                ((SparkHTTPServlet)c).shotDownNetwork(c_name);
            }


        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
