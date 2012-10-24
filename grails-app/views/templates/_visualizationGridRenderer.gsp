<script type="text/javascript">
    google.load('visualization', '1', {'packages':['table']});
    google.setOnLoadCallback(init${gridConfig.id});
    var dataSourceUrl = '${g.createLink(action: "${gridConfig.id}Rows")}';
    var query, options, container;

    function init${gridConfig.id}() {
        query = new google.visualization.Query(dataSourceUrl);
        container = document.getElementById("${gridConfig.id}_div");

    <g:if test="${gridConfig.visualization.loadAllData}">
        // Send the query with a callback function.
        query.send(handleQueryResponse);
        //todo - add options

    </g:if>
    <g:else>
        options = {};
        query.abort();
        var tableQueryWrapper = new TableQueryWrapper(query, container, options);
        tableQueryWrapper.sendAndDraw();

    </g:else>


    }

    function handleQueryResponse(response) {
        if (response.isError()) {
            alert('Error in query: ' + response.getMessage() + ' ' + response.getDetailedMessage());
            return;
        }

        var data = response.getDataTable();
        var visualization = new google.visualization.Table(container);
        visualization.draw(data, null);
    }

    /*
     function setOption(prop, value) {
     options[prop] = value;
     sendAndDraw();
     }
     */

</script>


<div id="${gridConfig.id}_div"></div>

%{--
<form action="">
    Number of rows to show:
    <select onChange="setOption('pageSize', parseInt(this.value, 10))">
        <option selected=selected value="5">5</option>
        <option value="10">10</option>
        <option value="20">20</option>
        <option value="-1">-1</option>
    </select>
</form>--}%
