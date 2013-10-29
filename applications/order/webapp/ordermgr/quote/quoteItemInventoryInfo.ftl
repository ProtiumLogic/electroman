
<#if facilityMapList?has_content>
<h2>Stock level information for ${productId?if_exists}</h2>
<table border="1" cellspacing="3" cellpadding="4">
<tr>
<th><span class="label">Facility Name</span><th><th><span class="label">Stock level</span></th>
</tr>
<#list facilityMapList as facilityItem>
<#assign facility = delegator.findByPrimaryKey("Facility", Static["org.ofbiz.base.util.UtilMisc"].toMap("facilityId", facilityItem.facilityId))>

<h3><tr><td>${facility.facilityName?if_exists}</td> <td><td>${facilityItem.availableToPromiseTotal?if_exists}<td></td></tr></h3>
</#list>
<table>
</#if>