package bbcom;

import bbcom.utils.Utils;
import com.google.gson.Gson;
import com.rabbitmq.client.*;
import httpCom.HTTPComServlet;
import httpCom.SparkHTTPServlet;
import lib.Com;
import lib.Node;
import lib.Slave;
import lib.Coordinator;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class Main {
    static String nodeId = "node1";
    static String moduleStoreLocation = "modules/";

    public static void main(String[] args) throws IOException {

       /* int c_num = 1;
        int s_num = 1;
        for(int i = 0; i< args.length;++i){
            if(args[i].equals("-c"))
                c_num = Integer.parseInt(args[++i]);
            if(args[i].equals("-s"))
                s_num = Integer.parseInt(args[++i]);
        }*/

        Boolean coordinator = true;
        String mainCoordinatorAddress = "localhost";
        if(args.length>0) {
            mainCoordinatorAddress = args[0];
            coordinator = false;
        }
        File b = new File(moduleStoreLocation);
        String absolute = b.getCanonicalPath();
        Map<Class<? extends Coordinator>,String> coordinatorClasses = null;
        try {
            int port = 8901;
            coordinatorClasses = Utils.findAllMatchingTypes(Coordinator.class, moduleStoreLocation +"coordinator/");
            Map<Class<? extends Node>,String> slaveClasses =  Utils.findAllMatchingTypes(Node.class, moduleStoreLocation+"slave/");
            Map<Class<? extends Com>,String> comClasses = new HashMap<>();// Utils.findAllMatchingTypes(Com.class, moduleStoreLocation);
            //comClasses.put(new HTTPComServlet().getClass(),null);
            String c_id = "cord";
            String nodeName = "node_1";
            String s_id = "slave";
            Map<String,List<SparkHTTPServlet.Address>> addresses = new HashMap<>();
            addresses.put(s_id,new LinkedList<SparkHTTPServlet.Address>());
            addresses.get(s_id).add(new SparkHTTPServlet.Address(nodeName,c_id,"http://"+mainCoordinatorAddress+":"+Integer.toString(port)+"/com"));

           // SparkHTTPServlet.Address coordinatoraddress = new SparkHTTPServlet.Address(nodeName,s_id,"http://localhost:"+Integer.toString(port)+"/com");
            //addresses.put(s_id,new LinkedList<SparkHTTPServlet.Address>());
            //addresses.get(s_id).add(new SparkHTTPServlet.Address("node1",c_id,"http://localhost:"+Integer.toString(port)+"/com"));

           // Com c = (Com)comClasses.keySet().toArray(new Class[0])[0].newInstance();
            Com c = new SparkHTTPServlet(addresses,port,nodeName);
            c.start();
            int i = 0;
       //     for(int i = 0;i<c_num;i++)
       //         startServer(coordinatorClasses.keySet().toArray(new Class[0])[0],c,c_id+Integer.toString(i));
            if(coordinator) {
                startServer(coordinatorClasses.keySet().toArray(new Class[0])[0], c, c_id);
            }
            else
                startSlave(slaveClasses.keySet().toArray(new Class[0])[0],c,s_id+"0",addresses.get(s_id).get(0));

       //     for(int i = 0;i<s_num;i++)
       //         startSlave(slaveClasses.keySet().toArray(new Class[0])[0],c,s_id+Integer.toString(i),addresses.get(s_id).get(0));
                startSlave(slaveClasses.keySet().toArray(new Class[0])[0],c,s_id+"0",addresses.get(s_id).get(0));


        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }


    }

    private static void startServer(Class<?> o,Com c,String id) throws IllegalAccessException, InstantiationException {
        Coordinator cord = (Coordinator) o.newInstance();

            cord.setProcessId(id);
            cord.setCom(c);
            cord.setArguments(new String[]{"-apath" ,"modules/coordinator/"});
            cord.start();

    }
    private static void startSlave(Class<?> o, Com c, String slaveProcessId, SparkHTTPServlet.Address coordinatorAddress) throws IllegalAccessException, InstantiationException {
        String coordinatorDescriptor = new Gson().toJson(coordinatorAddress, SparkHTTPServlet.Address.class);
        Node s = (Node)o.newInstance();
            s.setProcessId(slaveProcessId);
            s.setCoordinatorDescriptor(coordinatorDescriptor);
            s.setCom(c);
            s.connect();
            s.start();


    }

    /*@Override
    public boolean send(String s) {
        return false;
    }*/
/*
    private static void openCoordinatorQueue(String QUEUE_NAME, Coordinator c) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                String message = new String(body, "UTF-8");
                c.receive(message);
                System.out.println(" [x] Received '" + message + "'");
            }
        };
        channel.basicConsume(QUEUE_NAME, true, consumer);
    }
    private static void openSlaveQueue(String QUEUE_NAME, Slave s) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                String message = new String(body, "UTF-8");
                s.receive(message);
                System.out.println(" [x] Received '" + message + "'");
            }
        };
        channel.basicConsume(QUEUE_NAME, true, consumer);
    }*/
}
