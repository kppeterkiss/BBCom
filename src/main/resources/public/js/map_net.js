
//{"slave":[{"nodeId":"node_1","processId":"cord","url":"http://localhost:8901/com"}],
// "cord":[{"nodeId":"node_1","processId":"slave","url":"http://0.0.0.0:8901/com"}]}

//{"edgeList":[
// {"bandwidth":0,"delay":152,
// "nodes":[
// {"moduleDescriptionByCategory":{"lib.Coordinator":[{"classname":"BBoCoordinator","categoryName":"coordinator","names":[]}],"lib.Node":[{"classname":"BBOSlave","categoryName":"slave","names":[]}]},
// "connections":{"node_2":[{}]}},{"moduleDescriptionByCategory":{"lib.Node":[{"classname":"BBOSlave","categoryName":"slave","names":[]}],"lib.Coordinator":[{"classname":"BBoCoordinator","categoryName":"coordinator","names":[]}]},"connections":{"node_1":[{"type":"NODE","httpAddress":{"peerId":"node_2","processId":"node_2","url":"192.168.0.108","port":8901}}]}}]}]}

function start(port) {
    console.log("START RUNNING");
    var url = "http://localhost:"+port+"/update_map";

    var node_list = [];
    var edge_list = [];



    $.get(url, function (data1) {
        alert(data1)

        var graph_map = JSON.parse(data1);
        var orig_edge_list = graph_map['edgeList'];
        for(var e of orig_edge_list){
            let node1 = e['nodes'][0]['address']['peerId'];
            let node2 = e['nodes'][1]['address']['peerId'];
            if(!node_list.includes(node1)) {
                node_list.push(node1);
            }
            if(!node_list.includes(node2)) {
                node_list.push(node2);
            }
            edge_list.push({from: node1, to: node2})

            $.each(node1['connections'] ,function(node_name,node_connectionList) {
                node_name += node1 + "@" + node_name;
                if (!node_list.includes(node_name)) {
                    node_list.push(node_name);
                }
                //connect the peer to the node processes
                edge_list.push({from: node1, to: node_name})
                $.each(node_connectionList, function (connection) {
                    let proc_name = connection['peerId'] + "@" + connection['processId'];
                    if (!node_list.includes(proc_name)) {
                        node_list.push(proc_name);
                        edge_list.push({from: node_name, to: proc_name})
                    }});


            });



                $.each(node2['connections'] ,function(node_name,node_connectionList) {
                    node_name += node1 + "@" + node_name;
                    if (!node_list.includes(node_name)) {
                        node_list.push(node_name);
                    }
                    //connect the peer to the node processes
                    edge_list.push({from: nod2, to: node_name})
                    $.each(node_connectionList, function (connection) {
                        let proc_name = connection['peerId'] + "@" + connection['processId'];
                        if (!node_list.includes(proc_name)) {
                            node_list.push(proc_name);
                            edge_list.push({from: node_name, to: proc_name})
                        }});


                    });



        }
        /*Object.keys(graph_map).forEach(
            function (key) {
                node_list.push({id:count++, label:key  });
            }
        );
        Object.keys(graph_map).forEach(
            function (key) {
                var start_node_id = 1;
                for (var j = 0; j < node_list.length; j++) {
                    if (node_list[j].label == key) {
                        start_node_id = node_list[j].id;
                        break;
                    }
                }
                for (var i in graph_map[key]) {
                    var new_node_name = graph_map[key][i].processId;
                    var end_node_id = 1;
                    for (var j = 0; j < node_list.length; j++) {
                        if (node_list[j].label == new_node_name) {
                            end_node_id = node_list[j].id;
                            break;
                        }
                    }
                    edge_list.push({from: start_node_id, to: end_node_id})
                }

            }
        );*/
        nodes = new vis.DataSet(node_list);
        edges = new vis.DataSet(edge_list);
        var container = document.getElementById('chart_container');

        // provide the data in the vis format
        var data = {
            nodes: nodes,
            edges: edges
        };
        var options = {};

        // initialize your network!
        var network = new vis.Network(container, data, options);
    });


    var nodes, edges;
    setTimeout(function(){
        start(port);
    }, 10000);
}