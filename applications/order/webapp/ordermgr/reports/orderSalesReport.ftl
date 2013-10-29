<table border="1" width="100%">
<#if security.hasEntityPermission("ORDERMGR", "_ADMIN", session)>
    <tr>
    	<td><b>ORG_NAME</b></td>
        <td><b>SALES_TYPE</b></td>
        <td><b>INVOICE_ID</b></td>
        <td><b>ORDER_STATUS</b></td>
        <td><b>ORDER_DATE</b></td>
        <td><b>CUSTOMER_NUMBER</b></td>
        <td><b>CUSTOMER_NAME</b></td>
        <td><b>CUSTOMER_PHONE</b></td>
        <td><b>SALES_PERSON_CODE</b></td>
        <td><b>SALES_PERSON_NAME</b></td>
        <td><b>CATALOG</b></td>
        <td><b>CATEGORY</b></td>
        <td><b>SUB-CATEGORY</b></td>
        <td><b>PRODUCT_ID</b></td>
        <td><b>BRAND_NAME</b></td>
        <td><b>INTERNAL_NAME</b></td>
        <td><b>QUANTITY</b></td>
        <td><b>RSP</b></td>
        <td><b>CP</b></td>
        <td><b>GROSS_SALE</b></td>
        <td><b>GROSS_PROFIT</b></td>
        
    </tr>
 <#list invoiceItems as item>
 	<#assign itemPrice = item.amount?if_exists/>
 	<#assign primaryCategoryName ="_NA_"/>
	<#assign orderItemBillingList = delegator.findByAnd("OrderItemBilling", Static["org.ofbiz.base.util.UtilMisc"].toMap("invoiceId",item.invoiceId))>
	<#if orderItemBillingList?has_content>
		<#assign orderItemBillingInfo = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(orderItemBillingList) />
		<#assign orderInfo = delegator.findOne("OrderHeader", Static["org.ofbiz.base.util.UtilMisc"].toMap("orderId",orderItemBillingInfo.orderId), false)>
		<#assign salesChannelInfo = delegator.findOne("Enumeration", Static["org.ofbiz.base.util.UtilMisc"].toMap("enumId",orderInfo.salesChannelEnumId), false)>
		<#assign placingCustomerInfoList = delegator.findByAnd("OrderRole", Static["org.ofbiz.base.util.UtilMisc"].toMap("orderId",orderItemBillingInfo.orderId,"roleTypeId","PLACING_CUSTOMER"))>
		<#if placingCustomerInfoList?has_content>
			<#assign placingCustomerInfo = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(placingCustomerInfoList) />
		</#if>
		<#assign salesRepInfoList = delegator.findByAnd("OrderRole", Static["org.ofbiz.base.util.UtilMisc"].toMap("orderId",orderItemBillingInfo.orderId,"roleTypeId","SALES_REP"))>
		<#if salesRepInfoList?has_content>
			<#assign salesRepInfo = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(salesRepInfoList) />
			<#assign salesRepId = salesRepInfo.partyId?if_exists />
		<#else>
			<#assign salesRepId = "_NA_" />
		</#if>
		<#if placingCustomerInfo?has_content>
		<#assign partyContactMechList = delegator.findByAnd("PartyContactMechPurpose", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId",placingCustomerInfo.partyId,"contactMechPurposeTypeId","PHONE_HOME"))>
		<#else>
		<#assign partyContactMechList = delegator.findByAnd("PartyContactMechPurpose", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId",item.partyId,"contactMechPurposeTypeId","PHONE_HOME"))>
		</#if>
		<#if partyContactMechList?has_content>
			<#assign contactInfo = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(partyContactMechList)/>
			<#assign customerPhoneInfo = delegator.findOne("TelecomNumber", Static["org.ofbiz.base.util.UtilMisc"].toMap("contactMechId",contactInfo.contactMechId), false)>
			<#assign contactNumber = customerPhoneInfo.contactNumber?if_exists/>
		<#else>
			<#assign contactNumber = "_NA_"/>
		</#if>
	</#if> 	
 	<#assign productInfo = delegator.findOne("Product", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",item.productId ), false)>
 	<#assign productCPInfos = delegator.findByAnd("ProductPrice", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",item.productId,"productPriceTypeId","AVERAGE_COST","productPricePurposeId","PURCHASE" ))>
 	<#if productCPInfos?has_content>
 		<#assign productCPInfos = Static["org.ofbiz.entity.util.EntityUtil"].filterByDate(productCPInfos?if_exists) />
 		<#if productCPInfos?has_content>
	 	 	<#assign productCPInfo = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(productCPInfos) />
	 	 	<#assign CPRate = productCPInfo.getBigDecimal("price")/>
 	 	</#if>
 	<#else>
 	 	 <#assign CPRate = ZERO?if_exists/>
 	</#if>
 	<#assign productRSPInfos = delegator.findByAnd("ProductPrice", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",item.productId,"productPriceTypeId","DEFAULT_PRICE","productPricePurposeId","PURCHASE" ))>
 	<#if productRSPInfos?has_content>
 	 	<#assign productRSPInfos = Static["org.ofbiz.entity.util.EntityUtil"].filterByDate(productRSPInfos?if_exists) />
 	 	<#if productRSPInfos?has_content>
 	 	<#assign productRSPInfo = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(productRSPInfos) />
 		<#assign RSPRate = productRSPInfo.getBigDecimal("price")/>
 		</#if>
 	<#else>
 		 <#assign RSPRate = ZERO?if_exists/>
 	</#if>
 	<#assign productCategoryList = delegator.findByAnd("ProductCategoryMember",Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",item.productId))>
 	<#if productCategoryList?has_content>
		<#assign productCategory = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(productCategoryList) />
	 	<#assign categoryInfo = delegator.findOne("ProductCategory", Static["org.ofbiz.base.util.UtilMisc"].toMap("productCategoryId",productCategory.productCategoryId ), false)>
		<#if categoryInfo.primaryParentCategoryId?has_content>
		 	<#assign primaryCategoryInfo = delegator.findOne("ProductCategory", Static["org.ofbiz.base.util.UtilMisc"].toMap("productCategoryId",categoryInfo.primaryParentCategoryId), false)>
		 	<#assign primaryCategoryName = primaryCategoryInfo.categoryName?if_exists/>
		</#if>
		<#if categoryInfo.primaryParentCategoryId?has_content>
			<#assign productCatalogList = delegator.findByAnd("ProdCatalogCategory",Static["org.ofbiz.base.util.UtilMisc"].toMap("productCategoryId",categoryInfo.primaryParentCategoryId))>
			<#if productCatalogList?has_content>
					<#assign productCatalog = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(productCatalogList) />
				 	<#assign catalogInfo = delegator.findOne("ProdCatalog", Static["org.ofbiz.base.util.UtilMisc"].toMap("prodCatalogId",productCatalog.prodCatalogId ), false)>
				 	<#assign catalogName = catalogInfo.catalogName?if_exists/>
	 		</#if>
	 	<#else>
	 		<#assign catalogList = delegator.findByAnd("ProdCatalogCategory",Static["org.ofbiz.base.util.UtilMisc"].toMap("productCategoryId",categoryInfo.productCategoryId))>
			<#if catalogList?has_content>
				<#assign productCatalog = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(catalogList) />
				<#assign catalogInfo = delegator.findOne("ProdCatalog", Static["org.ofbiz.base.util.UtilMisc"].toMap("prodCatalogId",productCatalog.prodCatalogId ), false)>
				<#assign catalogName = catalogInfo.catalogName?if_exists/>
			<#else>
				<#assign catalogName = "_NA_"/>
			</#if>		
 		</#if>
 	</#if>
 	
	<#if item.entryType == "SALES_ORDER">
	<tr>
        <td>${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, "Company", false)?if_exists}</td>
        <td>${salesChannelInfo.description?if_exists}</td>
        <td>${item.invoiceId?if_exists}</td>
        <td>${orderInfo.getRelatedOneCache("StatusItem").get("description",locale)}</td>
        <td>${item.invoiceDate?if_exists}</td>
        <td>${item.partyId?if_exists}</td>
        <td>${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, item.partyId, false)?if_exists}</td>
        <td>${contactNumber?if_exists}</td>
        <td>${salesRepId?if_exists}</td>
       	<td>${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, salesRepId, false)?if_exists}</td>
        <td>${catalogName?if_exists}</td>
        <#if primaryCategoryName=="_NA_">
	        <td>${categoryInfo.categoryName?if_exists}</td>
	        <td>${primaryCategoryName?if_exists}</td>
        <#else>
	        <td>${primaryCategoryName?if_exists}</td>
	        <td>${categoryInfo.categoryName?if_exists}</td>
        </#if>
        <td>${item.productId?if_exists}</td>
        <td>${productInfo.brandName?if_exists}</td>
        <td>${productInfo.internalName?if_exists}</td>
        <td>${item.quantity?if_exists}</td>
		<#assign RSPMultiRate=ZERO?if_exists/>
       	<#assign CPMultiRate=ZERO?if_exists/>
        <#assign profitRate=ZERO?if_exists/>
        <#assign RSPMultiRate = itemPrice.multiply(item.quantity)/>  
		<#if CPRate?has_content>
        <#assign CPMultiRate = CPRate.multiply(item.quantity)/>        <#assign profitRate = RSPMultiRate.subtract(CPMultiRate)>
		<#assign profitRate = RSPMultiRate.subtract(CPMultiRate)>
		</#if>
        <td>${RSPRate?if_exists}</td>
        <td>${CPRate?if_exists}</td>
        <td>${RSPMultiRate?if_exists}</td>
        <td>${profitRate?if_exists}</td>
    </tr>
	<#else>
		<#assign orderItemBillingList = delegator.findByAnd("OrderItemBilling", Static["org.ofbiz.base.util.UtilMisc"].toMap("orderId",item.orderId))>
		<#if orderItemBillingList?has_content>
			<#assign orderItemBillingInfo = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(orderItemBillingList) />
		</#if>

		<#assign partyContactMechList = delegator.findByAnd("PartyContactMechPurpose", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId",item.fromPartyId,"contactMechPurposeTypeId","PHONE_HOME"))>
			<#if partyContactMechList?has_content>
				<#assign contactInfo = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(partyContactMechList)/>
				<#assign customerPhoneInfo = delegator.findOne("TelecomNumber", Static["org.ofbiz.base.util.UtilMisc"].toMap("contactMechId",contactInfo.contactMechId), false)>
				<#assign contactNumber = customerPhoneInfo.contactNumber?if_exists/>
			<#else>
				<#assign contactNumber = "_NA_"/>
		</#if>
		<tr>
        <td>${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, "Company", false)?if_exists}</td>
        <td>Sales Return</td>
        <td>${orderItemBillingInfo.invoiceId}</td>
        <td>Return</td>
        <td>${item.returndate?if_exists}</td>
        <td>${item.fromPartyId?if_exists}</td>
        <td>${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, item.fromPartyId, false)?if_exists}</td>
        <td>${contactNumber?if_exists}</td>
        <td>_NA_</td>
       	<td>_NA_</td>
        <td>${catalogName?if_exists}</td>
        <#if primaryCategoryName=="_NA_">
	        <td>${categoryInfo.categoryName?if_exists}</td>
	        <td>${primaryCategoryName?if_exists}</td>
        <#else>
	        <td>${primaryCategoryName?if_exists}</td>
	        <td>${categoryInfo.categoryName?if_exists}</td>
        </#if>
        <td>${item.productId?if_exists}</td>
        <td>${productInfo.brandName?if_exists}</td>
        <td>${productInfo.internalName?if_exists}</td>
        <td>${item.returnquantity?if_exists}</td>
		<#assign RSPMultiRate=ZERO?if_exists/>
       	<#assign CPMultiRate=ZERO?if_exists/>
        <#assign profitRate=ZERO?if_exists/>
       <#-- <#assign RSPMultiRate = RSPRate.multiply(item.quantity)/>  
		<#if CPRate?has_content>
        <#assign CPMultiRate = CPRate.multiply(item.quantity)/> 
		<#assign profitRate = RSPMultiRate.subtract(CPMultiRate)>
		</#if>-->
        <td>${RSPRate?if_exists}</td>
        <td>${CPRate?if_exists}</td>
        <td>${item.returnprice}</td>
        <td>${profitRate}</td>
    </tr>
	</#if>     
    </#list>
<#else>
    <tr>
    	<td><b>PERNISSION ERROR</b></td></tr>
        <tr><td><b>THE USER DON'T HAVE PERMISSION TO GENERATE THIS REPORT.</b></td></tr>
</#if>
</table>




