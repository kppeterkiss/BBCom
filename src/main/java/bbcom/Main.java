package bbcom;

import com.google.gson.Gson;
import httpCom.SparkHTTPServlet;
import lib.Com;
import network.EdgeType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.net.DatagramSocket;
import java.util.Enumeration;

public class Main {



    public static void main(String[] args) throws IOException, InterruptedException {

        Thread t =new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("111111");

                try {
                    Thread.sleep(1000);
                    System.out.println("111111");

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });
       t.join();
        t.start();

        System.out.println("2222");
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
            /*String nodeName = "node_1";
            Com c = new SparkHTTPServlet(port,nodeName);
            //boolean reacheable = ((SparkHTTPServlet)c).testPort();
            c.start();
            c.join();
            System.out.println("=======================================");
            System.out.println("NODE ADDRESS => "+new Gson().toJson(c.getProcessConnectionDescriptor(c.getName(), SparkHTTPServlet.EdgeType.NODE), SparkHTTPServlet.HttpConnection.class));
            System.out.println("=======================================");*/

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String userinput ="";

            /*if(mode.equals("primary")) {
                try {
                    String nodeName = "node_1";
                    Com c = new SparkHTTPServlet(port,nodeName);
                    //boolean reacheable = ((SparkHTTPServlet)c).testPort();
                    c.start();
                    c.join();
                    System.out.println("=======================================");
                    System.out.println("NODE ADDRESS => "+new Gson().toJson(c.getProcessConnectionDescriptor(c.getName(), SparkHTTPServlet.EdgeType.NODE), SparkHTTPServlet.HttpConnection.class));
                    System.out.println("=======================================");
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

                    SparkHTTPServlet.HttpConnection conn = (SparkHTTPServlet.HttpConnection) c.getProcessConnectionDescriptor(wName, SparkHTTPServlet.EdgeType.BIDIRECT);

                    //conn.type = SparkHTTPServlet.EdgeType.BIDIRECT;
                    c.addBidirectionalChannel(conn, cName);

                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }// we connect to an existing node
            else */if(!connectionUrl.equals("")){
                String nodeName = "node_2";
                Com c = new SparkHTTPServlet(/*addresses,*/port,nodeName);
                //boolean reacheable = ((SparkHTTPServlet)c).testPort();
                c.start();
                c.join();
                System.out.println("=======================================");
                System.out.println("NODE ADDRESS => "+new Gson().toJson(c.getProcessConnectionDescriptor(c.getName(), EdgeType.NODE), SparkHTTPServlet.HttpConnection.class));
                System.out.println("=======================================");
                SparkHTTPServlet.HttpConnection nodeConnectionToConnect = new Gson().fromJson(connectionUrl, SparkHTTPServlet.HttpConnection.class);
                if(nodeConnectionToConnect.type.equals(EdgeType.NODE))
                    nodeConnectionToConnect.type = EdgeType.NODE;
                c.connectToNetwork(nodeConnectionToConnect.httpAddress/*, c.getName()*/);


            }
            else if (mode.equals("local")){
                String nodeName = "node_1";
                Com c = new SparkHTTPServlet(port,nodeName);
                //boolean reacheable = ((SparkHTTPServlet)c).testPort();
                c.start();
                c.join();
                System.out.println("=======================================");
                System.out.println("NODE ADDRESS => "+new Gson().toJson(c.getProcessConnectionDescriptor(c.getName(), EdgeType.NODE), SparkHTTPServlet.HttpConnection.class));
                System.out.println("=======================================");

                ((SparkHTTPServlet)c).buildNetwork2();

/*
                String cName = c.launchModule("BBoCoordinator", new String[]{"-apath", "modules/coordinator/"});
                //--instantiate  -> sending request to nodes
                //one local worker
                String wName = c.launchModule("BBOSlave", new String[]{});
                String wName1 = c.launchModule("BBOSlave", new String[]{});


                SparkHTTPServlet.HttpConnection conn = (SparkHTTPServlet.HttpConnection) c.getProcessConnectionDescriptor(wName, SparkHTTPServlet.EdgeType.BIDIRECT);
                SparkHTTPServlet.HttpConnection conn1 = (SparkHTTPServlet.HttpConnection) c.getProcessConnectionDescriptor(wName1, SparkHTTPServlet.EdgeType.BIDIRECT);
                //conn.type = SparkHTTPServlet.EdgeType.BIDIRECT;
                c.addBidirectionalChannel(conn, cName);
                c.addBidirectionalChannel(conn1, cName);*/

            }
            else if(mode.equals("starter")){
                String nodeName = "node_1";
                Com c = new SparkHTTPServlet(/*addresses,*/port,nodeName);
                //boolean reacheable = ((SparkHTTPServlet)c).testPort();
                c.start();
                c.join();
                System.out.println("=======================================");
                System.out.println("NODE ADDRESS => "+new Gson().toJson(c.getProcessConnectionDescriptor(c.getName(), EdgeType.NODE), SparkHTTPServlet.HttpConnection.class));
                System.out.println("=======================================");
                String c_name = "";
                while (!userinput.equals("STOP")) {
                    userinput = reader.readLine();
                    System.out.println("user input : "+userinput);
                    //at the coordinator node we build up the network
                    if(userinput.toUpperCase().equals("BUILD"))
                        c_name = ((SparkHTTPServlet)c).buildNetwork2();
                    if(userinput.toUpperCase().equals("MAP"))
                        c.mapNetwork();
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
