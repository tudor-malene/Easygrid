<table >
    <thead>
    <tr>
        <g:each in="${gridConfig.columns}" var="col">
            <g:if test="${col?.classic?.sortable}">
                <g:sortableColumn property="${col.property}" title="${message(code: col.label, default: col.label)}"/>
            </g:if>
            <g:else>
                <th>${message(code: col.label, default: col.label)}</th>
            </g:else>
        </g:each>
    </tr>
    </thead>
    <tbody>

    <g:each in="${rows.rows}" var="row" status="i">
        <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
            <g:each in="${gridConfig.columns}" var="col">
                <td>
                    <g:if test="${col?.classic?.link}">
                        <g:link action="show" id="${row[col]}">${row[col]}</g:link>
                    </g:if>
                    <g:else>
                        ${row[col]}
                    </g:else>
                </td>
            </g:each>
        </tr>
    </g:each>

    </tbody>
</table>

<div class="pagination">
    %{--todo --}%
    <g:paginate total="${rows.records}"/>
</div>