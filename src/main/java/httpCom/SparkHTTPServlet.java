package httpCom;

import bbcom.utils.FileUtils;
import bbcom.utils.UnZip;
import bbcom.utils.Zip;
import com.google.gson.Gson;
import lib.Address;
import lib.Com;
import lib.Connection;
import lib.ConnectionType;
import network.EdgeDescriptor;
import network.NetworkGraph;
import network.NodeDescriptor;
import org.apache.velocity.app.VelocityEngine;
import spark.ModelAndView;
import spark.Service;
import spark.Spark;

import java.io.*;

import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.*;
import spark.template.velocity.VelocityTemplateEngine;


public class SparkHTTPServlet extends Com {



    // these are the folders to store the mo
    String tempfolderName = "temp";
    String resfolderName = "repo";
    final String comWorkingDir = System.getProperty("user.dir");

    final String tempfolder = comWorkingDir+"/temp";
    final String resfolder = comWorkingDir+"/repo";




    //public void setId(String id) {
   //    this.id = id;
    //}

    //String id = "node_2";
    //public int port;
    //todo let it bhe set from outside
    //public int socketPort = 8902;
    String inetAddress;

    public  Map<String,List<HttpConnection>> getConnections() {
        return connections;
    }

    // TODO: 2018. 12. 09.  incorportate  map and other things
    public String buildNetwork() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        String cName = this.launchModule("BBoCoordinator", new String[]{"-apath", "modules/coordinator/"});
        //--instantiate  -> sending request to nodes
        //one local worker
       // String wName = this.launchModule("BBOSlave", new String[]{});
        // this supposed to return all the neighbouring node
        for(HttpConnection c : this.connections.get(this.getName())){
            String rwName = this.launchRemoteModule(c, "BBOSlave", new String[]{});




            SparkHTTPServlet.HttpConnection rconn1 = (SparkHTTPServlet.HttpConnection) this.calculateRemoteProcessConnectionDescriptor(rwName, c);
            this.addBidirectionalChannel(rconn1, cName);


            /*String rwName2 = this.launchRemoteModule(c, "BBOSlave", new String[]{});

            SparkHTTPServlet.HttpConnection rconn2 = (SparkHTTPServlet.HttpConnection) this.calculateRemoteProcessConnectionDescriptor(rwName2, c);

            // the coordinator sets up connections to the workers
            this.addBidirectionalChannel(rconn2, cName);*/

        }
        return cName;
    }

    public void shotDownNetwork(String id){
        publish("STOP",id);

    }


    @Deprecated
    public void setConnections(Map<String,List<HttpConnection>> connections) {
        this.connections = connections;
    }

    public synchronized void sendFile(String fileName, int port) throws IOException {
        ServerSocket servsock = new ServerSocket(port);
        File myFile = new File(fileName);
        boolean sent = false;
        while (!sent) {
            System.out.println("FILE UPLOADING");
            Socket sock = servsock.accept();
            byte[] mybytearray = new byte[(int) myFile.length()];
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(myFile));
            bis.read(mybytearray, 0, mybytearray.length);
            OutputStream os = sock.getOutputStream();
            os.write(mybytearray, 0, mybytearray.length);
            os.flush();
            sock.close();
            sent = true;
        }

    }

    public  int sendEnvironment(String path,String url, int port, String sender) throws IOException {

        if(new File(path).isDirectory()) {
            Zip.compress(path, comWorkingDir+"/temp/Folder.zip");
            path = "temp/Folder.zip";
        }
        final String resourcepath = path  ;
       // for(HttpAddress httpAddress :connections.get(sender))
       // {
        new Thread(()-> {
            try {
                sendFile(resourcepath, this.defaultSecondaryPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        return (int) new File(path).length();
       // }


    }



    @Override
    public String getFile(String filename, String location, String fileDestinationNode) {
        try {
            //String s = System.getProperty("user.dir");
            //String defaultpath = new URI(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).resolve(resfolder).toString();
        String path = FileUtils.findRersource(location==null?resfolder:location,filename);
            System.out.println("PATH found = " +path);
        if (path == null && fileDestinationNode!=null) {
                path = pullFile(filename,fileDestinationNode);
        }
        System.out.println("Found FILE: "+path);
            return path;
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        }

    }

    /**
     *
     * @param filename
     * @param sender
     * @return
     * @throws Exception
     */
    public String pullFile(String filename,String sender) throws Exception {
        // TODO: 2018. 11. 08.
        System.out.println("PULLING FILE");

        for(HttpConnection connection : connections.get(sender)) {//make a http post with the msg
            //if(httpAddress.processId.equals())
            this.inetAddress = "http://"+getPublicIP()+":"+this.defaultPort+"/com";
            //getPublicIP();
            int size = Integer.parseInt(this.publish("PULL_FILE "+ new Gson().toJson(new HttpAddress(this.peerId,sender,getPublicIP(), this.defaultSecondaryPort),HttpAddress.class)+" "+filename, sender));
            Thread.sleep(1000);
            receiveFile(connection.httpAddress.getUrl(),this.defaultSecondaryPort,"temp/dl.zip", size);
            UnZip uz = new UnZip();
            //todo
            uz.unZipIt("temp/dl.zip","repo");
            

        }
        //here we have in path the original plavce but if it is sent over from different node it is only the very inner folder
        filename = filename.substring(filename.lastIndexOf("/"),filename.length());

        return "repo/"+filename;
    }

    private void receiveFile(String host, int port, String fileName, int size) throws IOException {
        System.out.println("DOWNLOADING FILE");
        Socket sock = new Socket(host, port);
        byte[] mybytearray = new byte[size];
        InputStream is = sock.getInputStream();
        FileOutputStream fos = new FileOutputStream(fileName);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        int bytesRead = is.read(mybytearray, 0, mybytearray.length);
        bos.write(mybytearray, 0, bytesRead);
        bos.close();
        sock.close();
    }


    Map<String,List<HttpConnection>> connections = new HashMap<>();


   // List<String> connections;
    List<String> ids;
    Map<String,List<String>> messages = new HashMap<>();

    @Override
    public void run(){
        System.out.println("Com Port: "+this.defaultPort);
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
        Service s = Service.ignite().port(this.defaultPort).threadPool(10);

        s.staticFileLocation("public/");

        s.post("/com", (request, response) -> {
            System.out.println("Incoming: "+request.raw());
            String to = request.queryParams("To");
            String receivedMsg = request.body();

            // pulling a file is handled by the peer
            if(receivedMsg.startsWith("PULL_FILE")) {
                System.out.println("FILE REQUEST");
                String as = receivedMsg.split(" ")[1];
                HttpAddress a = new Gson().fromJson(as, HttpAddress.class);
                as = receivedMsg.split(" ")[2];
                // TODO: 2018. 11. 08. path + id
                String nodeSourceLOC = getModuleReferenceByName(to).getSourceHome();
                nodeSourceLOC= nodeSourceLOC.substring(0,nodeSourceLOC.lastIndexOf("!"));
                nodeSourceLOC = nodeSourceLOC.substring(nodeSourceLOC.indexOf(":")+1,nodeSourceLOC.length());
                nodeSourceLOC= nodeSourceLOC.substring(0,nodeSourceLOC.lastIndexOf("/"))+"/";
                String loc = new URI(nodeSourceLOC).resolve("public").toString();
                // TODO: 2018. 11. 09. if file not found infinite sending!!!! 
                //as = getFile(as,loc,to);
                as = getFile(as,loc,null); // no further request if not found..
                System.out.println("requested found = "+as);
                //here we return
                return sendEnvironment(as,a.getHostAddress(),a.getPort(),"X");

            }
            // add the key(id) of addressee to the common message map
                if (!messages.containsKey(to))
                    this.messages.put(to, new LinkedList<>());
            // this should not happen
                if (!connections.containsKey(to))
                    this.connections.put(to, new LinkedList<>());
                // these to be handled by the peer, no need to add to the messages
                if (receivedMsg.startsWith("CONNECT"))
                {
                    String as = receivedMsg.split(" ")[1];
                    HttpConnection a = new Gson().fromJson(as, HttpConnection.class);
                    if(a.type== HttpConnectionType.OUPUT)
                        a.type = HttpConnectionType.INPUT;
                    if(a.type== HttpConnectionType.INPUT)
                        a.type = HttpConnectionType.OUPUT;
                    this.connections.get(to).add(a);
                    System.out.println("CONNECTION REQUEST - to : "+to+" from: "+as);

                }
                else if(receivedMsg.startsWith("DISCONNECT")){
                    HttpConnection a = new Gson().fromJson(receivedMsg.split(" ")[1], HttpConnection.class);
                    this.connections.forEach((key, value) -> value.remove(a));


                }
                else if (receivedMsg.startsWith("INSTANTIATE")){
                    String[] sa = receivedMsg.split(" ");
                    String moduleName = sa[1];
                    String args =receivedMsg.substring(receivedMsg.lastIndexOf(moduleName),receivedMsg.length());
                    String name = this.launchModule(moduleName,args.split(" "));
                    System.out.println("INSTANTIATING  "+moduleName+" @ "+to);
                    return name;

                }else // actual message to be handled by the running processes
                    this.messages.get(to).add(request.body());
            //}
            /*Map<String, Object> model1 = new HashMap<>();


            model1.put("algorithmname",config[0].getAlgorithmName());
            model1.put("filename",saveFileName[0]);
            model1.put("template","templates/algorithm.vtl");
            model1.put("algParamMap",algParamMap);
            model1.put("parametertypes",classList);*/
           // System.out.println("TO "+to+" - MSG received -> "+receivedMsg);
            return to;
        });
        s.get("/map", (request, response) -> {

            Map<String, Object> model1 = new HashMap<>();
            System.out.println("WORKING DIR: "+System.getProperty("user.dir"));
            String layout = "templates"+File.separator+"layout.vtl";
            model1.put("port_no",this.defaultPort);
            System.out.println(new File("templates").getAbsolutePath());
            model1.put("template","templates"+File.separator+"graph.vtl");
            return new VelocityTemplateEngine(velocityEngine).render( new ModelAndView(model1, layout));
        });
        s.get("/update_map", (request, response) -> {
            System.out.println("MAPPING res: "+new Gson().toJson(this.mapNetwork(),NetworkGraph.class));
            System.out.println("CONNECTIONS: "+new Gson().toJson(this.connections));
           return new Gson().toJson(this.connections);

        });
        s.awaitInitialization();
    }

    // might be unnecesary to get always, but what if changes?
    public String getPublicIP() throws Exception {
        //return "localhost";
        return IPChecker.getIp();
    }

    /*
    public boolean testPort(){
        try {
            String ip = this.getPublicIP();
            int port = getDefaultPort();
            (new Serve(ip, port)).close();
            System.out.println("CHECKED CONNECTION : "+ip+":"+port+" is reacheable.");
            return true;
        }
        catch(SocketException e) {
            e.printStackTrace();
            return false;
            // Could not connect.
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }
*/

    @Override
    public boolean addBidirectionalChannel(Connection descriptor,String processId){
        return connect( (HttpConnection)descriptor,  processId, HttpConnectionType.BIDIRECT) ;
    }

    @Override
    public HttpAddress getPeerAddress() {
        // TODO: 2018. 12. 04. getpeerid should be the name or name +  address or something
        try {
            new HttpAddress(this.getPeerId(),this.getName(),this.getPublicIP(), this.defaultPort);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    LinkedList<HttpConnection> pendingMapRequests;
    LinkedList<HttpConnection> pendingRcvdRequests;
    static int mapIdCounter = 0;

    @Override
    public NetworkGraph mapNetwork() {
        this.pendingMapRequests = new LinkedList<>();

        NetworkGraph ng = new NetworkGraph(new LinkedList<>());
        for(Map.Entry<String, List<HttpConnection>> e :this.connections.entrySet()){
            List<HttpConnection> connections = e.getValue();
            for(HttpConnection connection : connections)
            if(connection.type.equals(HttpConnectionType.NODE)){
                try {
                    long start = System.currentTimeMillis();
                    String response = send(connection,"MAP",this.getName());
                    NodeDescriptor nd = new Gson().fromJson(response,NodeDescriptor.class);
                    this.pendingMapRequests.add(connection);
                    long finish = System.currentTimeMillis();
                    long timeElapsed = finish - start;
                    EdgeDescriptor ed = new EdgeDescriptor(0L,(long)(timeElapsed/2),new NodeDescriptor[]{nd,this.getInfo()});
                    ng.addEdge(ed);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        while(!pendingMapRequests.isEmpty()){
            List<String> answers = receive(this.getName(),"MAP_RES");

            for(String s : answers){
                String[] sa = s.split(" ");
                HttpConnection c = new Gson().fromJson(sa[1],HttpConnection.class);
                pendingMapRequests.remove(c);
                NetworkGraph graph = new Gson().fromJson(s,NetworkGraph.class);
                ng.addSubGraph(graph);
            }
        }
        for(HttpConnection c : this.pendingMapRequests) {
            try {
                send(c,"MAP_RES "+this.getConnections().get(this.getName()) +" "+new Gson().toJson(ng, NetworkGraph.class), "");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.pendingMapRequests = new LinkedList<>();
        return ng;
    }

    @Override
    public boolean addOutPutChannel(Connection coordinatorDescriptor, String processId) {
        return  connect( (HttpConnection)coordinatorDescriptor,  processId, HttpConnectionType.OUPUT) ;

    }

    // we set up a channel as an input for the process
    @Override
    public boolean addInputchannel(Connection descriptor, String processId) {
        return connect((HttpConnection)descriptor,processId, HttpConnectionType.INPUT);

    }
    // we set up a channel as an output for the process
    @Override
    public boolean connectToNetwork(Connection coordinatorDescriptor){
        connect((HttpConnection) coordinatorDescriptor,this.peerId, HttpConnectionType.NODE);
        return true;
    }

    //processname should be here..
    // TODO: 2018. 12. 04. type is redundant
    public boolean connect(HttpConnection connectionDescriptor, String processId,HttpConnectionType type) {

        // httpAddress of node to be connected
        //HttpConnection c  = new Gson().fromJson(coordinatorDescriptor,HttpConnection.class);
        //trying to connect to ourself at building up the network
        if(connectionDescriptor.httpAddress.peerId.equals(this.peerId) && processId.equals(this.peerId))
            return true;
        if(!this.connections.containsKey(processId))
            this.connections.put(processId,new LinkedList<>());
       // List<HttpConnection> connections = this.connections.get(processId);
       // if(connections == null)
       //     connections =  new LinkedList<>();


        this.connections.get(processId).add(connectionDescriptor);

        HttpConnection connectionInfoOfThis = (SparkHTTPServlet.HttpConnection)this.getProcessConnectionDescriptor(processId);


        // add the slave process addreslist to the node connections
       //= this.connections.put(processId,connections);

  //      try (final DatagramSocket socket = new DatagramSocket()) {
           // socket.connect(InetAddress.getByName("8.8.8.8"), this.defaultPort);
 //           this.inetAddress = "http://"+socket.getLocalAddress().getHostAddress()+":"+port+"/com";
          //  this.inetAddress = "http://"+getPublicIP()+":"+this.defaultPort+"/com";
            //getPublicIP();
            //this.publish("CONNECT "+ new Gson().toJson(new HttpAddress(this.id,processId,getPublicIP(), port),HttpAddress.class), processId);
           // this.publish("CONNECT "+ new Gson().toJson(new HttpConnection(HttpConnectionType.BIDIRECT,new HttpAddress(this.peerId,processId,"localhost", this.defaultPort)),HttpConnection.class), processId);
            this.publish("CONNECT "+ new Gson().toJson(connectionInfoOfThis,HttpConnection.class), processId);

        //     } catch (UnknownHostException e) {
    //        e.printStackTrace();
    //        return false;
    //    } catch (SocketException e) {
    //        e.printStackTrace();
    //        return false;
    //    } catch (Exception e) {
    //        e.printStackTrace();
    //    }
        return true;

    }

    public void discover(String sender){
        String resultMapString =this.publish("MAP",sender);
    }

   // public

    public SparkHTTPServlet(/*Map<String,List<HttpConnection>> connections,*/ int port_no, String nodeName) throws IOException, ClassNotFoundException {
        super(nodeName);
        //Map<String,List<SparkHTTPServlet.HttpConnection>> addresses = ;
        //this.peerId = nodeName;
        this.connections = new HashMap<>();
        this.defaultPort = port_no;

        // dir for storing files temporarily for transmission
        FileUtils.createDirIfNotExists(tempfolder);

        // dir for storing codes, folders, etc..
        FileUtils.createDirIfNotExists(resfolder);

        /*try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            this.inetAddress = socket.getLocalAddress().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }*/

    }


    /*@Override
    public boolean addConnectionToRemote(String descriptor, String name, String descriptor2) {
        return false;
    }*/

    @Override
    public String launchRemoteModule(Connection c, String moduleName, String[] arguments) {
        String message = "INSTANTIATE "+moduleName+String.join(" ",arguments);
        String name = "";
        try {
            name = send(c,message,this.peerId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return name;
    }

    @Override
    public String killRemoteModule(Connection c) {
        String message = "STOP";
        String name = "";
        try {
            name = send(c,message,this.peerId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return name;
    }

    @Override
    public Connection getProcessConnectionDescriptor(String id) {
        try {
            HttpAddress a = new HttpAddress(this.getPeerId(),id,this.getPublicIP(),this.getDefaultPort());
            return new HttpConnection(HttpConnectionType.BIDIRECT,a);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    // TODO: 2018. 12. 06. this is local!!! not asking the remote node
    @Override
    public Connection calculateRemoteProcessConnectionDescriptor(String id,Connection c) {
        try {
            HttpAddress a =((HttpConnection)c).httpAddress;
            a = new HttpAddress(a.peerId,id,a.url,a.port);
            return new HttpConnection(HttpConnectionType.BIDIRECT,a);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }



    @Override
    public boolean addConnectionToRemote(Connection descriptor, String name, Connection descriptor2, ConnectionType connDEscriptor) {
        //HttpConnectionType t = HttpConnectionType.valueOf((HttpConnectionType)connDEscriptor);

        HttpConnection connToBuild = new HttpConnection((HttpConnectionType)connDEscriptor,((HttpConnection)descriptor2).httpAddress);
        //where to publish
        //HttpConnection c = new Gson().fromJson(descriptor,HttpConnection.class);
        // name is the name of the process that we want to connect with connToBuild
        String message =  "CONNECT "+name+" "+connToBuild;
    String response = "";
        try {
            response = send((HttpConnection)descriptor,message,this.peerId);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // thsi should be sent to one single process
    @Override
    public boolean startRemoteProcess(Connection descriptor, String name, String[] args) {
        //HttpConnection c = new Gson().fromJson(descriptor,HttpConnection.class);
        String message =  "INSTANAITATE "+name+" ";
        StringJoiner sj = new StringJoiner(" ");
        String res = null;

        for (String s : args)
            sj.add(s);
        message+=sj.toString();
        try {
            res = send((HttpConnection) descriptor,message,this.peerId );
        } catch (IOException e) {
            e.printStackTrace();
            return false;

        }
        return true;

    }


    public String publish(String msg, String sender) {
        int i = 0;
        List<String> responses = new ArrayList<>();
        for(HttpConnection connection: connections.get(sender)){//make a http post with the msg
            try {
                // TODO: 2018. 12. 03. parallel 
                //id = ids.get(i++);
                if(connection.type == HttpConnectionType.BIDIRECT || connection.type == HttpConnectionType.OUPUT || connection.type == HttpConnectionType.NODE) {
                    responses.add(send( connection, msg,sender));

                }
                else
                    return null;

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return responses.get(0);
        //return false;
    }

    public List<String> distribute(List<String> msgs, String sender) {
        int i = 0;
        List<String> responses = new ArrayList<>();
        for(String msg : msgs) {
            if(i == connections.get(sender).size())
                i = 0;
            HttpConnection connection = connections.get(sender).get(i++);

                try {

                    if (connection.type == HttpConnectionType.BIDIRECT || connection.type == HttpConnectionType.OUPUT || connection.type == HttpConnectionType.NODE) {
                        responses.add(send(connection, msg, sender));

                    } else
                        return null;

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        return responses;
        //return false;
    }

    
    @Override
    public String send(Connection connection, String msg, String sender) throws IOException {
        HttpConnection connection1 = (HttpConnection)connection;
        //System.out.println("TO "+httpAddress.getProcessId()+"@"+httpAddress.getHostAddress()+" - MSG sent ->"+msg);
        byte[] postDataBytes = msg.getBytes("UTF-8");
        String query = String.format("To=%s",
                URLEncoder.encode(connection1.httpAddress.getProcessId(), "UTF-8"));
        HttpURLConnection conn = (HttpURLConnection) (new URL(connection1.httpAddress.getHostAddress() + "?" + query).openConnection());
        //URL obj = new URL(httpAddress.url);
        //HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
        conn.setRequestMethod("POST");
        conn.setFixedLengthStreamingMode(postDataBytes.length);
        conn.setRequestProperty("Content-Type", "application/json");
        //conn.setRequestProperty("To", id);
        conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
        conn.setDoOutput(true);
        conn.getOutputStream().write(postDataBytes);
        Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (int c; (c = in.read()) >= 0; )
            sb.append((char) c);
        String response = sb.toString();
        System.out.println("Response from: " + response);
        return response;
    }


    @Override
    public synchronized List<String> receive(String id) {
        List<String> ret = new LinkedList<>();
        if(messages.containsKey(id))
            ret = messages.get(id);
        messages.put(id,new LinkedList<>());
        return ret;
    }

    public synchronized List<String> receive(String id,String filter) {
        List<String> ret = new LinkedList<>();

        if(messages.containsKey(id)) {
            for (String msg : messages.get(id))
                if(msg.contains(filter)) {
                    ret.add(msg);
                    messages.get(id).remove(msg);
                }
        }

        return messages.get(id);
    }

    public static class HttpAddress implements Address{
        private String peerId;
        private String processId;
        private String url;
        private int port;

        public String getPeerId() {
            return peerId;
        }

        public String getProcessId() {
            return processId;
        }

        public String getUrl() {
            return url;
        }

        public int getPort() {
            return port;
        }

        public HttpAddress(String peerId, String processId, String url, int port ) {
            this.peerId = peerId;
            this.processId = processId;
            this.url = url;
            this.port = port;
        }

        public String getHostAddress(){
            return "http://"+url+":"+port+"/com";
        }

        @Override
        public String toString() {
            return "HttpAddress{" +
                    "peerId='" + peerId + '\'' +
                    ", processId='" + processId + '\'' +
                    ", url='" + url + '\'' +
                    ", port=" + port +
                    '}';
        }


    }

   /* public static class connrctionRequest{
        String id, url,

    }*/
   public enum HttpConnectionType implements ConnectionType {
       INPUT,OUPUT,NODE,BIDIRECT;
   }

   public enum EdgeType {
       PARALLEL, PARTITIOM;
   }

   public static class HttpConnection implements Connection {
        public EdgeType eType;
        public HttpConnectionType type;
        public HttpAddress httpAddress;

       public HttpConnection(HttpConnectionType type,/* EdgeType eType,*/ HttpAddress httpAddress) {
           this.eType = eType;
           this.type = type;
           this.httpAddress = httpAddress;
       }

       @Override
       public boolean equals(Object o) {
           if (this == o) return true;
           if (o == null || getClass() != o.getClass()) return false;
           HttpConnection that = (HttpConnection) o;
           return Objects.equals(httpAddress, that.httpAddress);
       }

       @Override
       public int hashCode() {

           return Objects.hash(httpAddress);
       }

       @Override
       public String toString() {
           return "HttpConnection{" +
                   "eType=" + eType +
                   ", type=" + type +
                   ", httpAddress=" + httpAddress +
                   '}';
       }
   }
}
