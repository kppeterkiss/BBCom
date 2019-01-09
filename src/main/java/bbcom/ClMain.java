package bbcom;

import httpCom.SparkHTTPServlet;
import lib.Com;
import network.EdgeType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

public class ClMain {

    public static void main(String[] args) throws IOException {

        String line = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while(!line.trim().equals("exit")){
            line = reader.readLine();
            String[] sa = line.split(" ");
            String com = line.substring(0,line.indexOf(" "));


        }


        try {
            int port = 8901;
            String nodeName = "node_1";
            Com c = new SparkHTTPServlet(/*addresses,*/port,nodeName);
            //boolean reacheable = ((SparkHTTPServlet)c).testPort();
            c.start();
            c.join();
            try {
                String cName = c.launchModule("BBoCoordinator",new String[]{"-apath" ,"modules/coordinator/"},null);
                String wName = c.launchModule("BBOSlave",null,null);
                //String rWName = c.launchRemoteModule(,"BBOSlave",null);

                //String wName2 = c.launchModule("BBOSlave",null);
                SparkHTTPServlet.HttpConnection conn = ( SparkHTTPServlet.HttpConnection)c.getProcessConnectionDescriptor(wName, EdgeType.BIDIRECT); // type not sure
                conn.type = EdgeType.BIDIRECT;
                c.addBidirectionalChannel(conn.httpAddress,cName);

            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
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
