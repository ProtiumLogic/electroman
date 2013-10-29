<#-- FTL file for excel -->
<table border="1">
	<tr rowspan="2">
		<th colspan="7" align="center" bold="bold"> Goods Recieved Report</th>
	</tr>
	<tr>
		<th>Order No.</th>
		<th>Supplier</th>
		<th>Product Id</th>
		<!--<th>Catalog</th>
		<th>Category</th>
		<th>Sub-Category</th>-->
		<th>Quantity Ordered</th>
		<th>Quantity Recieved</th>
		<th>Quantity Rejected</th>
		<th>Location</th>
	</tr>
	
<#if orderPurchaseProductSummaryList?has_content>
<#list orderPurchaseProductSummaryList as item>
	<#assign supplierName = Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, item.partyId, false)/>
	<#if item.productId?has_content>
	<#assign productInfo = delegator.findOne("Product", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",item.productId ), true)> 
	</#if>
	<#assign totalReceived = 0.0>
	<#assign shipmentReceipts = delegator.findByAnd("ShipmentReceipt", {"orderId" : item.orderId})/>
						<#if shipmentReceipts?exists && shipmentReceipts?has_content>
						    <#list shipmentReceipts as shipmentReceipt>
						      <#if shipmentReceipt.quantityAccepted?exists && shipmentReceipt.quantityAccepted?has_content>
						        <#assign  quantityAccepted = shipmentReceipt.quantityAccepted>
						        <#assign totalReceived = quantityAccepted + totalReceived>
						      </#if>
						      <#if shipmentReceipt.quantityRejected?exists && shipmentReceipt.quantityRejected?has_content>
						        <#assign  quantityRejected = shipmentReceipt.quantityRejected>
						        <#assign totalReceived = quantityRejected + totalReceived>
						      </#if>
						    </#list>
						    </#if>

	<tr>
		<td>${item.orderId?if_exists}</td>
		<td>${supplierName?if_exists}</td>
		<!--<td>${item.prodCatalogId?if_exists}</td>
		<td>${item.prodCategoryId?if_exists}</td>
		
		<td>${"SUB"}</td>-->
		<td>${item.productId?if_exists}</td>	
		<td>${item.quantity?if_exists}</td>
		<td>${quantityAccepted?if_exists}</td>
		<td>${quantityRejected?if_exists}</td>
		<td>${item.originFacilityId?if_exists}</td>
		
		
		
	</tr>
	
</#list>
<#else>
	<tr>
		<td colspan="8" align="center">No orders found</td>
	</tr>
</#if>

</table>