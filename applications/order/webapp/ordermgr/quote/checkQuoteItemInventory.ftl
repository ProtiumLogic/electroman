<script language="JavaScript" type="text/javascript">
jQuery(document).ready(function(){
	jQuery("#checkstock").click(function(){ 
	 jQuery.ajax({
                url: 'getItemFacilityInfo',
                async: false,
                type: 'POST',
                data: jQuery('#checkItemFacility').serialize(),
                success: function(data) {
                    jQuery('#facilityInfo').html(data);
                }
            });
	});
});
</script>

<form id="checkItemFacility" name="checkItemFacility" method ="post" >
<table>
<tr><td>ProductId</td>
<td><@htmlTemplate.lookupField name="productId" id="productId" formName="checkItemFacility" fieldFormName="LookupProduct"/>
</td></tr>
<tr><td><input id="checkstock" type="button" name="submit" value="Submit"/></td></tr>
</table>
</form>
<div id="facilityInfo">
</div>




<#-- Commented for ajax functionality-->
<#--<#if facilityMapList?has_content>
<h2>Stock level information for ${productId?if_exists}</h2>
<table border="1" cellspacing="3" cellpadding="3">
<tr>
<th><span class="label">Facility Name</span><th><th><span class="label">Stock level</span></th>
</tr>
<#list facilityMapList as facilityItem>
<tr><td>${facilityItem.facilityId?if_exists}</td> <td><td>${facilityItem.availableToPromiseTotal?if_exists}<td></td></tr>
</#list>
<table>

</#if>-->