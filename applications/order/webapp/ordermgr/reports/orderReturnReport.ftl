<table border="1" width="100%">
<#if security.hasEntityPermission("ORDERMGR", "_ADMIN", session)>

    <tr>
    	<td><b>Date</b></td>
        <td><b>Invoice No</b></td>
        <td><b>Barcode</b></td>
        <td><b>Product Desc</b></td>
        <td><b>Qty</b></td>  
        <td><b>Amount</b></td>
        
    </tr>
 	 <#list returnItems as item>
 	 	<#assign productInfo = delegator.findOne("Product", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",item.productId ), false)>
 	 		<#assign orderItemBillingList = delegator.findByAnd("OrderItemBilling", Static["org.ofbiz.base.util.UtilMisc"].toMap("orderId",item.orderId))>
	<#if orderItemBillingList?has_content>
		<#assign orderItemBillingInfo = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(orderItemBillingList) />
	</#if>	
 	 	
 	
	<tr>
        <td>${item.returndate?if_exists}</td>
        <td>${orderItemBillingInfo.invoiceId?if_exists}</td>
                <td>${item.productId?if_exists}</td>
                <td>${productInfo.internalName?if_exists}</td>
                <td>${item.quantity?if_exists}</td>
                <td>${item.returnprice?if_exists}</td>
        
        <#--<td>${"barcode"?if_exists}</td>
        <td>${"product"?if_exists}</td>
        <td>${"qty"?if_exists}</td>
        <td>${"amt"?if_exists}</td>-->
        
    </tr>
    </#list>
    <#else>
    <tr>
    	<td><b>PERNISSION ERROR</b></td>
    </tr>
    <tr>
    	<td><b>THE USER DON'T HAVE PERMISSION TO GENERATE THIS REPORT.</b></td>
    </tr>
</#if>
</table>
