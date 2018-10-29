package httpCom;

import com.google.gson.Gson;
import com.sun.deploy.net.URLEncoder;
import lib.Com;
import org.apache.velocity.app.VelocityEngine;
import spark.ModelAndView;
import spark.Service;
import spark.Spark;

import java.io.*;

import java.net.*;
import java.util.*;
import spark.template.velocity.VelocityTemplateEngine;


public class SparkHTTPServlet extends Com {
    //public String getId() {
   //    return id;
    //}

    public void setId(String id) {
        this.id = id;
    }

    String id = "node_2";
    public int port;
    String inetAddress;

    public  Map<String,List<Address>> getAddresses() {
        return addresses;
    }

    public void setAddresses( Map<String,List<Address>> addresses) {
        this.addresses = addresses;
    }

    //public void setAddresses( Map<String,List<Address>> addresses) {
    //    this.addresses = addresses;
    //}


    Map<String,List<Address>> addresses = new HashMap<>();

   // List<String> addresses;
    List<String> ids;
    Map<String,List<String>> messages = new HashMap<>();

    @Override
    public void run(){
        System.out.println("Com Port: "+port);
        Spark.exception(Exception.class, (e, request, response) -> {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);
            System.err.println(sw.getBuffer().toString());
        });
        Properties properties = new Properties();
        properties.setProperty("resource.loader", "class");
        properties.setProperty(
                "class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        VelocityEngine velocityEngine = new VelocityEngine(properties);
        velocityEngine.init();
        System.out.println("COM STARTED");
        Service s = Service.ignite().port(this.port).threadPool(10);
        //return "/Users/peterkiss/IdeaProjects/BBCom/modules/coordinator/public/";
        // TODO: 2018. 10. 18. path hack
        //String jarPublicLocation = System.getProperty("user.dir")+File.separator+"./../../public";
        //String resourcePublicLocation =System.getProperty("user.dir") +File.separator+"./../../src" +File.separator+"main" +File.separator+"resources"+File.separator+"public";

        //String ret =  Files.exists(Paths.get(jarPublicLocation))?jarPublicLocation:resourcePublicLocation;
        //System.out.println("RET :"+ret);
        s.staticFileLocation("public/");

        s.post("/com", (request, response) -> {
            System.out.println("Incoming: "+request.raw());
            String to = request.queryParams("To");
            String receivedMsg = request.body();
            if(!messages.containsKey(to))
                this.messages.put(to, new LinkedList<>());
            if(!addresses.containsKey(to))
                this.addresses.put(to, new LinkedList<>());
            if(receivedMsg.startsWith("CONNECT")) {
                String as = receivedMsg.split(" ")[1];
                Address a = new Gson().fromJson(as, Address.class);
                this.addresses.get(to).add(a);
            }
            else
                this.messages.get(to).add(request.body());

            /*Map<String, Object> model1 = new HashMap<>();


            model1.put("algorithmname",config[0].getAlgorithmName());
            model1.put("filename",saveFileName[0]);
            model1.put("template","templates/algorithm.vtl");
            model1.put("algParamMap",algParamMap);
            model1.put("parametertypes",classList);*/

            return to;
        });
        s.get("/map", (request, response) -> {

            Map<String, Object> model1 = new HashMap<>();
            System.out.println("WORKING DIR: "+System.getProperty("user.dir"));
            String layout = "templates"+File.separator+"layout.vtl";
            model1.put("port_no",port);
            System.out.println(new File("templates").getAbsolutePath());
            model1.put("template","templates"+File.separator+"graph.vtl");
            return new VelocityTemplateEngine(velocityEngine).render( new ModelAndView(model1, layout));
        });
        s.get("/update_map", (request, response) -> {
           return new Gson().toJson(this.addresses);

        });
    }

    public String getPublicIP() throws Exception {
        return IPChecker.getIp();
    }


    //processname should be here..
    public boolean connect(String coordinatorDescriptor, String processId) {
        List<Address> addresses = new LinkedList<>();

        //address of the coordinator to connect to
        addresses.add(new Gson().fromJson(coordinatorDescriptor,Address.class));

        // add the slave process addreslist to the node addresses
        this.addresses.put(processId,addresses);

        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), this.port);
            this.inetAddress = "http://"+socket.getLocalAddress().getHostAddress()+":"+port+"/com";
            //this.inetAddress = "http://"+getPublicIP()+":"+port+"/com";
            //getPublicIP();
            this.send("CONNECT "+ new Gson().toJson(new Address(this.id,processId,this.inetAddress),Address.class), processId);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        } catch (SocketException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;

    }

    public SparkHTTPServlet( Map<String,List<Address>> addresses, int port_no,String nodeName) {
        this.id = nodeName;
        this.addresses = addresses;
        this.port = port_no;

        /*try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            this.inetAddress = socket.getLocalAddress().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }*/

    }

    @Override
    public boolean send(String msg,String sender) {
        int i = 0;
        for(Address address :addresses.get(sender)){//make a http post with the msg
            try {
                //id = ids.get(i++);
                byte[] postDataBytes = msg.getBytes("UTF-8");
                String query = String.format("To=%s",
                        URLEncoder.encode(address.processId, "UTF-8"));
                HttpURLConnection conn = (HttpURLConnection) (new URL(address.url + "?" + query).openConnection());
                //URL obj = new URL(address.url);
                //HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
                conn.setRequestMethod("POST");
                conn.setFixedLengthStreamingMode( postDataBytes.length);
                conn.setRequestProperty("Content-Type", "application/json");
                //conn.setRequestProperty("To", id);
                conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
                conn.setDoOutput(true);
                conn.getOutputStream().write(postDataBytes);
                Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                for (int c; (c = in.read()) >= 0;)
                    sb.append((char)c);
                String response = sb.toString();
                System.out.println(response);

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return false;
    }


    @Override
    public synchronized List<String> receive(String id) {
        List<String> ret = new LinkedList<>();
        if(messages.containsKey(id))
            ret = messages.get(id);
        messages.put(id,new LinkedList<>());
        return ret;
    }

    public static class Address{
        String nodeId;
        String processId;
        String url;

        public Address(String nodeId, String processId, String url) {
            this.nodeId = nodeId;
            this.processId = processId;
            this.url = url;
        }
    }

   /* public static class connrctionRequest{
        String id, url,

    }*/
}
