package httpCom;

import bbcom.utils.FileUtils;
import bbcom.utils.UnZip;
import bbcom.utils.Zip;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.reflect.TypeToken;
import lib.Address;
import lib.Com;
import lib.Connection;
import lib.ConnectionType;
import network.*;
import org.apache.velocity.app.VelocityEngine;
import plan.CallGraph;
import plan.ExecutionEdge;
import plan.NodeDescriptor;
import plan.RelationMultiplicity;
import plan.algorithms.StarLayout;
import spark.*;

import java.io.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

import spark.template.velocity.VelocityTemplateEngine;


public class SparkHTTPServlet extends Com<SparkHTTPServlet.HttpConnection,SparkHTTPServlet.HttpAddress> {



    // these are the folders to store the mo
    String tempfolderName = "temp";
    String resfolderName = "repo";
    final String comWorkingDir = System.getProperty("user.dir");

    final String tempfolder = comWorkingDir+"/temp";
    final String resfolder = comWorkingDir+"/repo";

    NetworkGraph ng = new NetworkGraph(new LinkedList<>());





    //public void setId(String id) {
    //    this.id = id;
    //}

    //String id = "node_2";
    //public int port;
    //todo let it bhe set from outside
    //public int socketPort = 8902;
    String inetAddress;
    @Override
    public  Map<String,List<HttpConnection>> getConnections() {
        Map<String,List<HttpConnection>> res = new HashMap<>();
        this.connections.forEach((k,v)->{res.put(k,new LinkedList<>()); v.forEach(c->res.get(k).add(c));});
        return res;
    }


    public String buildNetwork2() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        //
        String coordId = "Coordinator";
        NodeDescriptor<HttpAddress> coordNode = new NodeDescriptor<>();
        coordNode.setArgs(new String[]{"-apath", "modules/coordinator/"});
        coordNode.setModuleClassName("BBoCoordinator");
        coordNode.setProcessId(coordId);

        NodeDescriptor<HttpAddress> slave = new NodeDescriptor<>();
        slave.setArgs(new String[]{});
        slave.setModuleClassName("BBOSlave");
        slave.setProcessId("slave");
        //coordNode.setProcessId("coordinator");
        ExecutionEdge e = new ExecutionEdge(100,20,new  NodeDescriptor[]{coordNode,slave}, RelationMultiplicity.ONE_TO_MANY);
        //ExecutionEdge e_ret = new ExecutionEdge(100,20,new  NodeDescriptor[]{slave,coordNode}, RelationMultiplicity.MANY_TO_ONE);
        List<ExecutionEdge> el = new LinkedList<>();
        el.add(e);
        //el.add(e_ret);
        CallGraph cp = new CallGraph(el,coordId,coordId);
        CallGraph deploymentPlan = planDeployment(cp,new StarLayout());
        this.deploy(deploymentPlan);

        return coordId;
    }


    // TODO: 2018. 12. 09.  incorportate  map and other things
    /*public String buildNetwork() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        String cName = this.launchModule("BBoCoordinator", new String[]{"-apath", "modules/coordinator/"}, "Coordinator");
        //--instantiate  -> sending request to nodes
        //one local worker
        // String wName = this.launchModule("BBOSlave", new String[]{});
        // this supposed to return all the neighbouring node
        System.out.println(getName());
        for(HttpConnection c : this.connections.get(this.getName())){
            String rwName = this.launchRemoteModule(c, "BBOSlave", new String[]{},"slave1");




            SparkHTTPServlet.HttpConnection rconn1 = (SparkHTTPServlet.HttpConnection) this.calculateRemoteProcessConnectionDescriptor(rwName, c);
            this.addBidirectionalChannel(rconn1, cName);


            String rwName2 = this.launchRemoteModule(c, "BBOSlave", new String[]{},"slave2");

            SparkHTTPServlet.HttpConnection rconn2 = (SparkHTTPServlet.HttpConnection) this.calculateRemoteProcessConnectionDescriptor(rwName2, c);

            // the coordinator sets up connections to the workers
            this.addBidirectionalChannel(rconn2, cName);

        }
        return cName;
    }*/

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

    public  String  sendEnvironment(String path,String url, int port, String sender) throws IOException {
// TODO: 2018. 12. 14. multiple secondary port for parallel transmission
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
        HttpConnection c =  (HttpConnection) this.getProcessConnectionDescriptor(this.getName(),EdgeType.NODE);
        c.httpAddress.port = this.defaultSecondaryPort;
        return new File(path).length() +" "+new Gson().toJson(c,HttpConnection.class);
        // }


    }

    final Map<String,Object> pullqueue = new HashMap<>();
    //static int enter =0;

    // here
    @Override
    public  String getFile(String filename, String location, String fileDestinationNode) {
        try {
           // int i = enter++;
           // System.out.println("enter"+i);
            //String s = System.getProperty("user.dir");
            //String defaultpath = new URI(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).resolve(resfolder).toString();
            String key = filename + (location==null?"":location);
            if(pullqueue.containsKey(key)) {
                if (pullqueue.get(key) instanceof String) {
                   // System.out.println("SHOULD BE A STRING");
                   // System.out.println("RETURN "+i);
                    return (String) pullqueue.get(key);
                }
                else ((Thread)pullqueue.get(key)).join();
            }else{
                //System.out.println("NO TRY YET");
                String[] path = new String[]{ FileUtils.findRersource(location==null?resfolder:location,filename)};
                System.out.println("PATH found = " +path[0]);

                if (path[0] == null && fileDestinationNode!=null) {
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                path[0] = pullFile(filename,fileDestinationNode);
                                pullqueue.put(key,path[0]);
                                System.out.println("thread replaced");

                            } catch (Exception e) {
                                System.out.println("HEREERERERERERERER");
                                e.printStackTrace();
                            }
                        }
                    });
                    pullqueue.put(key, t);
                    t.start();
                    System.out.println("strated leeft");

                    ((Thread)pullqueue.get(key)).join();
                    System.out.println("join leeft");

                }
                else
                    pullqueue.put(key,path[0]);
            }
            System.out.println("Found FILE: "+pullqueue.get(key));
           // System.out.println("RETURN "+i);
            return  (String) pullqueue.get(key);
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
            String[] ans = this.publish("PULL_FILE "+ new Gson().toJson(new HttpAddress(this.peerId,sender,getPublicIP(), this.defaultSecondaryPort),HttpAddress.class)+" "+filename, sender).split(" ");
            int size = Integer.parseInt(ans[0]);
            HttpConnection c = new Gson().fromJson(ans[1],HttpConnection.class);
            Thread.sleep(1000);
            receiveFile(connection.httpAddress.getUrl(),this.defaultSecondaryPort,"temp/dl.zip", size,c);
            UnZip uz = new UnZip();
            //todo
            uz.unZipIt("temp/dl.zip","repo");


        }
        //here we have in path the original place but if it is sent over from different node it is only the very inner folder
        filename = filename.contains("/")? filename.substring(filename.lastIndexOf("/"),filename.length()):filename;

        return "repo/"+filename;
    }

    private void receiveFile(String host, int port, String fileName, int size, HttpConnection c) throws IOException {
        host = c.httpAddress.url;
        port = c.httpAddress.port;
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
       /* GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithModifiers(Modifier.TRANSIENT);
        Gson gson = builder.create();
        NodeDescriptor nd =  this.getInfo();
        EdgeDescriptor ed = new EdgeDescriptor(0,0,new NodeDescriptor[]{nd,null});
        List<EdgeDescriptor> l = new LinkedList<>();
        l.add(ed);
        NetworkGraph ng  = new NetworkGraph(l);
        System.out.println("MAPPING res: "+new  Gson().toJson(ng,NetworkGraph.class));*/


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

        s.post("/com", (Request request, Response response) -> {
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
                String connectionToBuildString = receivedMsg.split(" ")[1];
                HttpConnection connetionToBuild = new Gson().fromJson(connectionToBuildString, HttpConnection.class);
                System.out.println("CONNECTION REQUEST - to : "+to+" from: "+connectionToBuildString);
                if(connetionToBuild.type== EdgeType.OUPUT)
                    connetionToBuild.type = EdgeType.INPUT;
                if(connetionToBuild.type== EdgeType.INPUT)
                    connetionToBuild.type = EdgeType.OUPUT;
                this.connections.get(to).add(connetionToBuild);
                System.out.println("Connection added:" + connetionToBuild.toString());

            }
            else if(receivedMsg.startsWith("DISCONNECT")){

                HttpConnection a = new Gson().fromJson(receivedMsg.split(" ")[1], HttpConnection.class);
                this.connections.forEach((key, value) -> value.remove(a));


            }
            else if(receivedMsg.startsWith("MAP")){
                if(receivedMsg.startsWith("MAP_RES")){
                    System.out.println("RESULT OF SUBMAP: "+receivedMsg);
                    this.messages.get(to).add(receivedMsg);
                    return "x";
                }
                String as = receivedMsg.substring(receivedMsg.indexOf(" ")+1,receivedMsg.length());
                System.out.println("Mapping request received - "+receivedMsg);
                System.out.println("Mapping request from:  "+as);
                HttpConnection a = new Gson().fromJson(as, HttpConnection.class);
                if(this.pendingRcvdRequests == null)
                    this.pendingRcvdRequests = new LinkedList<>();
                if(!pendingRcvdRequests.contains(a)){
                    this.pendingRcvdRequests.add(a);
                }//asynchron propagation of mapping request
                //when it is done, sends the result to the originator
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ng = mapNetwork();
                    }
                }).start();
                //immediate response with info of the node
                return new Gson().toJson(this.getInfo(), PeerDescriptor.class);
            }
            else if (receivedMsg.startsWith("INSTANTIATE")){
                String[] sa = receivedMsg.split(" ");
                String moduleName = sa[1];
                Type type = new TypeToken<String[]>() {}.getType();
                String[] moduleArgs = new Gson().fromJson(sa[2],type);
                String processId = null;
                if(sa.length>3)
                     processId = sa[3];
               // String args =receivedMsg.substring(receivedMsg.lastIndexOf(moduleName),receivedMsg.length());
                String name = this.launchModule(moduleName,moduleArgs,processId);
                System.out.println("INSTANTIATING  "+moduleName+" @ "+to);
                return name;

            }else if (receivedMsg.startsWith("ARRAY")){
                //String[] sa = receivedMsg.split(" ");
                String msgs = receivedMsg.substring(receivedMsg.indexOf(" ")+1,receivedMsg.length());
                //System.out.println("received array: "+msgs);
                this.messages.get(to).addAll(Arrays.asList(msgs.split(";")));

            }
            else // actual message to be handled by the running processes
                this.messages.get(to).add(receivedMsg);
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


            mapNetwork();
            System.out.println("MAPPING res: "+new Gson().toJson(ng,NetworkGraph.class));
            System.out.println("CONNECTIONS: "+new Gson().toJson(this.connections));
            return new Gson().toJson(ng,NetworkGraph.class);
            //return new Gson().toJson(this.connections);

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
    public boolean addBidirectionalChannel(HttpAddress descriptor,String processId){
        return connect( descriptor,  processId, EdgeType.BIDIRECT) ;
    }

    @Override
    public HttpAddress getPeerAddress() {
        // TODO: 2018. 12. 04. getpeerid should be the name or name +  address or something
        try {
            return new HttpAddress(this.getPeerId(),this.getName(),this.getPublicIP(), this.defaultPort);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //requests initiated from the node
    LinkedList<HttpConnection> pendingMapRequests;

    //todo might be useful who waits for the answers
    Map<HttpConnection,HttpConnection> mappingRequests;
    // requests need to be answered
    LinkedList<HttpConnection> pendingRcvdRequests;
    static int mapIdCounter = 0;
    final class CoonectionInstanceCreator implements InstanceCreator<Connection>{

        @Override
        public Connection createInstance(Type type) {
            return new HttpConnection();
        }
    }

    final class AddressInstanceCreator implements InstanceCreator<Address>{

        @Override
        public Address createInstance(Type type) {
            return new HttpAddress();
        }
    }
    @Override
    public NetworkGraph  mapNetwork() {
        //ini t a new mapping
       // ng = new NetworkGraph(new LinkedList<>());
       // NetworkGraph newNg = new NetworkGraph();
        this.pendingMapRequests = new LinkedList<>();


        //NetworkGraph ng = new NetworkGraph(new LinkedList<>());
        for(Map.Entry<String, List<HttpConnection>> e :this.connections.entrySet()){
            List<HttpConnection> connections = e.getValue();
            //ask all connections to make a map, except for initiators
            for(HttpConnection connection : connections)
                if(connection.type.equals(EdgeType.NODE) && !(this.pendingRcvdRequests!= null && this.pendingRcvdRequests.contains(connection))){
                    try {
                        long start = System.currentTimeMillis();
                        //Gson gson = new GsonBuilder().excludeFieldsWithModifiers().setPrettyPrinting().create();
                        String addr = new Gson().toJson(this.getProcessConnectionDescriptor(this.getName(),EdgeType.NODE),HttpConnection.class);
                        String response = send(connection.httpAddress,"MAP "+addr, this.getName());
                        GsonBuilder gsb = new GsonBuilder();
                        //gsb.registerTypeAdapter(Connection.class,new CoonectionInstanceCreator());
                        //gsb.registerTypeAdapter(Address.class,new AddressInstanceCreator());
                        PeerDescriptor<HttpAddress,HttpConnection> nd = gsb.create().fromJson(response,new TypeToken<PeerDescriptor<HttpAddress,HttpConnection>>(){}.getType());
                        this.pendingMapRequests.add(connection);
                        long finish = System.currentTimeMillis();
                        long timeElapsed = finish - start;
                        NetworkEdge ed = new NetworkEdge(0L,(long)(timeElapsed/2),new PeerDescriptor[]{this.getInfo(),nd});

                        System.out.println("ADDING EDGE");
                        this.ng.addEdge(ed);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
        }
        while(!pendingMapRequests.isEmpty()){
            List<String> answers = receive(this.getName(),"MAP_RES");
            if(answers==null) {
                try {
                    System.out.println("Waiting for submap results...");
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            for(String s : answers){
                String[] sa = s.split(" ");
                System.out.println("Adding submap from: "+ sa[1]);
                HttpConnection c = new Gson().fromJson(sa[1],HttpConnection.class);
                pendingMapRequests.remove(c);
                NetworkGraph graph = new Gson().fromJson(sa[2],NetworkGraph.class);
                this.ng.addSubGraph(graph);
            }
        }
        if(this.pendingRcvdRequests != null) {
            for (HttpConnection c : this.pendingRcvdRequests) {
                try {
                    String submapJson =  new Gson().toJson(ng, NetworkGraph.class);
                    System.out.println("Sending submap to initiator");
                    System.out.println("Submap: " + submapJson);
                    System.out.println("Sending to: "+ c.httpAddress.toString());
                    //why this
                   // send(c.httpAddress, "MAP_RES " + this.connections.get(this.getName()) + " " +submapJson, "");
                    send(c.httpAddress, "MAP_RES " + new Gson().toJson(this.getProcessConnectionDescriptor(this.getName(),EdgeType.NODE),HttpConnection.class) + " " +submapJson, "");
                    pendingRcvdRequests.remove(c);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        this.pendingMapRequests = new LinkedList<>();
        ng.setRoot(this.getInfo());
        System.out.println("Mapping finished.");
        return ng;
    }

    @Override
    public boolean addOutPutChannel(HttpAddress addressToConnect, String initiatingProcessId) {
        return  connect( addressToConnect,  initiatingProcessId, EdgeType.OUPUT) ;

    }

    // we set up a channel as an input for the process
    @Override
    public boolean addInputchannel(HttpAddress addressOfNodeToConnect, String processIdOfConnInitiator) {
        return connect(addressOfNodeToConnect, processIdOfConnInitiator, EdgeType.INPUT);

    }
    // we set up a channel as an output for the process
    @Override
    public boolean connectToNetwork(HttpAddress peerAddressToConnect){
        connect(peerAddressToConnect,this.peerId, EdgeType.NODE);
        return true;
    }

    @Override
    public boolean sendConnectionRequest(NodeDescriptor<HttpAddress> nd1, NodeDescriptor<HttpAddress> nd2) {
        return false;
    }

    //processname should be here..
    // TODO: 2018. 12. 04. type is redundant

    /**
     *
     * @param addressToConnect the address of the process to connect
     * @param initiatingProcessId process that required to build the connection
     * @param type type of the connection
     * @return
     */
    public boolean connect(HttpAddress addressToConnect, String initiatingProcessId,EdgeType type)  {

        // httpAddress of node to be connected
        //HttpConnection c  = new Gson().fromJson(coordinatorDescriptor,HttpConnection.class);
        //trying to connect to ourself at building up the network
        if(addressToConnect.peerId.equals(this.peerId) && initiatingProcessId.equals(this.peerId))
            return true;
        if(!this.connections.containsKey(initiatingProcessId))
            this.connections.put(initiatingProcessId,new LinkedList<>());
        // List<HttpConnection> connections = this.connections.get(processId);
        // if(connections == null)
        //     connections =  new LinkedList<>();

        HttpConnection connection = new HttpConnection();
        connection.httpAddress = addressToConnect;
        connection.type = type;
        this.connections.get(initiatingProcessId).add(connection);


        HttpConnection connectionInfoOfThis = this.getProcessConnectionDescriptor(initiatingProcessId,type);


        // add the slave process addreslist to the node connections
        //= this.connections.put(processId,connections);

        //      try (final DatagramSocket socket = new DatagramSocket()) {
        // socket.connect(InetAddress.getByName("8.8.8.8"), this.defaultPort);
        //           this.inetAddress = "http://"+socket.getLocalAddress().getHostAddress()+":"+port+"/com";
        //  this.inetAddress = "http://"+getPublicIP()+":"+this.defaultPort+"/com";
        //getPublicIP();
        //this.publish("CONNECT "+ new Gson().toJson(new HttpAddress(this.id,processId,getPublicIP(), port),HttpAddress.class), processId);
        // this.publish("CONNECT "+ new Gson().toJson(new HttpConnection(EdgeType.BIDIRECT,new HttpAddress(this.peerId,processId,"localhost", this.defaultPort)),HttpConnection.class), processId);
        try {
            this.send(addressToConnect,"CONNECT "+ new Gson().toJson(connectionInfoOfThis,HttpConnection.class), initiatingProcessId);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

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
    public String launchRemoteModule(HttpAddress remoteProcessAddress, String moduleName, String[] arguments,String processId) {
        Type type = new TypeToken<String[]>() {}.getType();
        String message = "INSTANTIATE "+moduleName+" "+new Gson().toJson(arguments,type)+" "+processId;
        String name = "";
        try {
            name = send(remoteProcessAddress,message,this.peerId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return name;
    }

    @Override
    public String killRemoteModule(HttpAddress remoteProcessAddress) {
        String message = "STOP";
        String name = "";
        try {
            name = send(remoteProcessAddress,message,this.peerId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return name;
    }

    @Override
    public HttpConnection getProcessConnectionDescriptor(String id,EdgeType type) {
        EdgeType t1 = type;
        try {
            HttpAddress a = new HttpAddress(this.getPeerId(),id,this.getPublicIP(),this.getDefaultPort());
            return new HttpConnection(t1,a);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }



    // TODO: 2018. 12. 06. this is local!!! not asking the remote node
    @Override
    public HttpConnection calculateRemoteProcessConnectionDescriptor(String id,Connection c) {
        try {
            HttpAddress a =((HttpConnection)c).httpAddress;
            a = new HttpAddress(a.peerId,id,a.url,a.port);
            return new HttpConnection(EdgeType.BIDIRECT,a);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }



    @Override
    public boolean addConnectionToRemote(HttpAddress descriptor, String name, Connection descriptor2, EdgeType edgeType) {
        //EdgeType t = EdgeType.valueOf((EdgeType)connDEscriptor);

        HttpConnection connToBuild = new HttpConnection((EdgeType)edgeType,((HttpConnection)descriptor2).httpAddress);
        //where to publish
        //HttpConnection c = new Gson().fromJson(descriptor,HttpConnection.class);
        // name is the name of the process that we want to connect with connToBuild
        String message =  "CONNECT "+name+" "+connToBuild;
        String response = "";
        try {
            response = send(descriptor,message,this.peerId);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // thsi should be sent to one single process
   /* @Override
    public boolean launchRemoteModule(HttpConnection descriptor, String name, String[] args, String processId) {
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

    }*/


    public String publish(String msg, String sender) {
        int i = 0;
        List<String> responses = new ArrayList<>();
        for(HttpConnection connection: connections.get(sender)){//make a http post with the msg
            try {
                if(connection.type == EdgeType.BIDIRECT || connection.type == EdgeType.OUPUT || connection.type == EdgeType.NODE) {
                    responses.add(send( connection.httpAddress, msg,sender));
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
        List<HttpConnection> interestedConns = connections.get(sender)
                .stream()
                .filter(connection->connection.type == EdgeType.BIDIRECT || connection.type == EdgeType.OUPUT || connection.type == EdgeType.NODE)
                .collect(Collectors.toList());
        int pernode = (int)((float)msgs.size() / interestedConns.size()+0.5f);
        for(HttpConnection c : interestedConns){
            StringJoiner sb= new StringJoiner(";");
            for(int j = 0;j<pernode && i<msgs.size();++j){
                sb.add(msgs.get(i++));
            }
            //String a = sb.toString()
            //
            //
            String msg = "ARRAY "+sb.toString();
            System.out.println("----------------------");
            System.out.println(msg);
            System.out.println("----------------------");
            try {
                responses.add(send(c.httpAddress, msg, sender));
            } catch (IOException e) {
                // TODO: 2018. 12. 14. maybe try to send elswhere
                e.printStackTrace();
            }
        }
        return responses;
    }


    @Override
    public String send(HttpAddress address, String msg, String sender) throws IOException {

        System.out.println("TO "+address.getProcessId()+"@"+address.getHostAddress()+" - MSG sent ->"+msg);
        byte[] postDataBytes = msg.getBytes("UTF-8");
        String query = String.format("To=%s",
                URLEncoder.encode(address.getProcessId(), "UTF-8"));
        HttpURLConnection conn = (HttpURLConnection) (new URL(address.getHostAddress() + "?" + query).openConnection());
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
        System.out.println("Response received from: "+address.toString()+" ==> message: " + response);
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
                else
                    System.out.println("Discarded message");
        }

        return ret;
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
        public HttpAddress(){}
        public HttpAddress(HttpAddress other) {
            this(other.peerId,other.processId,other.url,other.port);
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
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HttpAddress that = (HttpAddress) o;
            return port == that.port &&
                    Objects.equals(peerId, that.peerId) &&
                    Objects.equals(processId, that.processId) &&
                    Objects.equals(url, that.url);
        }

        @Override
        public int hashCode() {

            return Objects.hash(peerId, processId, url, port);
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


        @Override
        public Address resolveProcessAddress(String processId) {
            return new HttpAddress(peerId,processId,url,port);
        }
    }

    /* public static class connrctionRequest{
         String id, url,

     }*/
    /*public enum EdgeType implements ConnectionType {
        INPUT,OUPUT,NODE,BIDIRECT;
    }

    // TODO: 2018. 12. 28. not necessary probably 
    public enum EdgeType {
        PARALLEL, PARTITIOM;
    }*/

    public static class HttpConnection extends Connection<HttpAddress> {
        //public EdgeType eType;
        public EdgeType type;
        public HttpAddress httpAddress;

        public HttpConnection(){}

        public HttpConnection(EdgeType type,/* EdgeType eType,*/ HttpAddress httpAddress) {
           // this.eType = eType;
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
                    ", type=" + type +
                    ", httpAddress=" + httpAddress +
                    '}';
        }
    }
}
