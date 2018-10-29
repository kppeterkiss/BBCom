package network;

import lib.Coordinator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkMap {

    static String server_addr = "localhost";
    //stores the overlay network of a given task
    Map<String,List<String>> overlayNetworks = new HashMap<>();
    //stores the physical adresses of the nodes per id
    Map<String, String> adresses = new HashMap<>();
}
