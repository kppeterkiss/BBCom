
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
    let counter = 0;
    let node_name_id_map={};


    function extracted(node_o,node_o_name) {
        $.each(node_o['connections'], function (node_name, node_connectionList) {
            node_name += "@" + node_o_name;
            if (!(node_name in node_name_id_map)) {
                //node_list.push(node_name);
                node_list.push({'id':counter,'label':node_name});
                node_name_id_map[ node_name] = counter++;
            }
            //connect the peer to the node processes
            edge_list.push({from: node_name_id_map[node_o_name], to: node_name_id_map[node_name]})
            $.each(node_connectionList, function (idx, connection) {
                let proc_name =connection['httpAddress']['processId']+ "@" + connection['httpAddress']['peerId']  ;
                if (!(proc_name in node_name_id_map)) {
                    //node_list.push(proc_name);
                    node_list.push({'id':counter,'label':proc_name});
                    node_name_id_map[ proc_name] = counter++;
                    edge_list.push({from: node_name_id_map[node_name], to: node_name_id_map[proc_name]})
                }
            });


        });
    }

    $.get(url, function (data1) {        // alert(data1)

        var graph_map = JSON.parse(data1);
        var orig_edge_list = graph_map['edgeList'];
        for(var e of orig_edge_list){
            let node1 = e['nodes'][0]
            let node2 = e['nodes'][1]
            let node1_name = node1['address']['peerId'];
            let node2_name = node2['address']['peerId'];
            if(!(node1_name in node_name_id_map)) {
                node_list.push({'id':counter,'label':node1_name});
                node_name_id_map[ node1_name] = counter++;
            }
            if(!(node2_name in node_name_id_map)) {
                node_list.push({'id':counter,'label':node2_name});
                node_name_id_map[node2_name] = counter++;

            }
            edge_list.push({from: node_name_id_map[node1_name], to: node_name_id_map[node2_name]})

            extracted(node1, node1_name);
            extracted(node2,node2_name);



           /* $.each(node2['connections'] ,function(node_name,node_connectionList) {
                node_name += "@" + node2_name;
                if (!node_list.includes(node_name)) {
                    node_list.push(node_name);
                }
                //connect the peer to the node processes
                edge_list.push({from: node2_name, to: node_name})
                $.each(node_connectionList, function (idx,connection) {
                    let proc_name = connection['httpAddress']['peerId'] + "@" + connection['httpAddress']['processId'];
                    if (!node_list.includes(proc_name)) {
                        node_list.push(proc_name);
                        edge_list.push({from: node_name, to: proc_name})
                    }});


            });*/



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
*/
//        let new_node_list
        let nodes = new vis.DataSet(node_list);
        let edges = new vis.DataSet(edge_list);
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


   // var nodes, edges;
    setTimeout(function(){
        start(port);
    }, 10000);
}