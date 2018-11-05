
//{"slave":[{"nodeId":"node_1","processId":"cord","url":"http://localhost:8901/com"}],
// "cord":[{"nodeId":"node_1","processId":"slave","url":"http://0.0.0.0:8901/com"}]}

function start(port) {
    console.log("START RUNNING");
    var url = "http://localhost:"+port+"/update_map";

    node_list = [];
    edge_list = [];
    var count = 1;

    $.get(url, function (data) {
        var graph_map = JSON.parse(data);
        Object.keys(graph_map).forEach(
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
        );
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