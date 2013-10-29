



<form method="post" action="<@ofbizUrl>creditNote.pdf</@ofbizUrl>" name="creditNoteForm" style="margin: 0;">

	<input type="hidden" name="partyId" value="${supplierPartyId}"/>
	<table class="basic-table hover-bar">
		<tr class="header-row">
		<th>Product ID</th>
		<th>Product Name</th>
		<th>Select <input type="checkbox" name="viewall" value="Y" onClick="selectAll(this)" 
			<#if 
				state.hasAllStatus()>checked="checked"
			</#if>/>
		</th>
		<th><input type="submit" value="Create Credit Note"/></th>
		</tr>
		<#assign rowCount = 0>
		<#assign alt_row = false>
		<#if allsupplierProducts?has_content>
		<#list allsupplierProducts as listItem>
		<#assign productInfo = delegator.findOne("Product", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",listItem.productId ), false)>
		<tr id="creditNote_tableRow_${rowCount} valign="middle">
		<td>${listItem.productId}</td>
		<td>${productInfo.internalName}</td>
		<td><input id ="creditNote_item_o_${rowCount}" type="checkbox" name="productIds" value="${listItem.productId}" /></td>
		</tr>
		<#assign rowCount = rowCount + 1>
		<#assign alt_row = !alt_row>
		</#list>
		</#if>
		<tr>
        <td></td>
          	<td><input type="submit" class="smallSubmit" value="Create Credit Note"/></td>
         </tr>
	</table>
</form>


<script language="JavaScript">
	function selectAll(source) {
		checkboxes = document.getElementsByName('productIds');
		for(var i in checkboxes)
			checkboxes[i].checked = source.checked;
	}
</script>