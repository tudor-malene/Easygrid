<div id="${attrs.id}_FilterDiv">
    <form name="${attrs.id}_FilterForm" onsubmit="return filterForm${attrs.id}(this)" action="#">
        <fieldset class="form">
            <g:hiddenField name="_filterForm" value="true"/>
            <g:each in="${gridConfig.filterForm}">
                <div class="fieldcontain  ">
                    <label for="${it.name}">
                        <g:message code="${it.label}" default="${it.label}"/>
                    </label>
                    %{--todo - treaba asta trebuie modificata --}%
                    <g:field name="${it.name}" type="${it.type}"/>
                </div>
            </g:each>
            <g:submitButton name="Filter"/>
        </fieldset>
    </form>
</div>
