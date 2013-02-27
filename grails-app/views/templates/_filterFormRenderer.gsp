<div id="${attrs.id}_FilterDiv">
    <form name="${attrs.id}_FilterForm" onsubmit="return filterForm${attrs.id}(this)">
        <fieldset class="form">
            <g:hiddenField name="_filterForm" value="true"/>
            <g:each in="${gridConfig.filterForm}">
                <div class="fieldcontain  ">
                    <label for="${it.name}">
                        <g:message code="${it.label}" default="${it.label}"/>
                    </label>

                    <g:if test="${it.type == 'between'}">
                        <div style='display:inline;'>
                            <g:field name="${it.name}.from" type="number"/>
                            <g:field name="${it.name}.to" type="number"/>
                        </div>
                    </g:if>
                    <g:else>
                        <g:field name="${it.name}" type="${it.type}"/>
                    </g:else>
                </div>
            </g:each>
            <g:submitButton name="Filter"/>
        </fieldset>
    </form>
</div>
