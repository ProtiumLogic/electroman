<table border="1" width="100%">
		<tr>
			<th><b>Supplier Name</b></th>
		  	<th><b>Supplier Code</b></th>
	        <th><b>Product Id</b></th>
	        <th><b>Product Name</b></th>      
	        <th><b>Model Number</b></th>
	        <th><b>Department</b></th>
	        <th><b>Category</b></th>
	        <th><b>Sub Category</b></th> 
	        <th><b>Brand Name</b></th>
	        <th><b>Rebate</b></th>
	        <th><b>RSP</b></th>
	        <th><b>CP</b></th>
	        <th><b>QTY SOLD</b></th>
	        <th><b>ATP</b></th>
	        <th><b>Showroom</b></th>
	        <th><b>Main Warehouse</b></th>
	        <th><b>Display Location</b></th>
	        <th><b>Back Store</b></th>
	       	<th><b>Ageing Time</b></th>
	       	<th><b>Ageing Days</b></th>
	        
	       	<#--<th><b>Total Stock Received</b></th>-->
       </tr>
	   				<#assign invdate = "_NA_"/>

		<#list productReportList as productReport>
			<#assign supplierProdDetails = delegator.findByAnd("SupplierProduct", {"productId" : productReport.productId,"partyId",productReport.partyId})>
			<#if supplierProdDetails?has_content >
				<#assign supplierProduct = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(supplierProdDetails) />
				<#assign rebateRate = supplierProduct.rebate?if_exists>
			<#else>
				<#assign rebateRate = ZERO?if_exists>	
			</#if>
			<#assign productInfo = delegator.findOne("Product", {"productId" : productReport.productId}, false)>
			
			
			 	<#assign productCPInfos = delegator.findByAnd("ProductPrice", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",productInfo.productId,"productPriceTypeId","AVERAGE_COST","productPricePurposeId","PURCHASE" ))>
			 	<#if productCPInfos?has_content>
			 		<#assign productCPInfos = Static["org.ofbiz.entity.util.EntityUtil"].filterByDate(productCPInfos?if_exists) />
			 		<#if productCPInfos?has_content>
				 	 	<#assign productCPInfo = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(productCPInfos) />
				 	 	<#assign CPRate = productCPInfo.getBigDecimal("price")/>
			 	 	</#if>
			 	<#else>
			 	 	 <#assign CPRate = ZERO?if_exists/>
			 	</#if>
			 	<#assign productRSPInfos = delegator.findByAnd("ProductPrice", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",productInfo.productId,"productPriceTypeId","DEFAULT_PRICE","productPricePurposeId","PURCHASE" ))>
			 	<#if productRSPInfos?has_content>
			 	 	<#assign productRSPInfos = Static["org.ofbiz.entity.util.EntityUtil"].filterByDate(productRSPInfos?if_exists) />
			 	 	<#if productRSPInfos?has_content>
				 	 	<#assign productRSPInfo = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(productRSPInfos) />
				 		<#assign RSPRate = productRSPInfo.getBigDecimal("price")/>
			 		</#if>
			 	<#else>
			 		 <#assign RSPRate = ZERO?if_exists/>
			 	</#if>
          <#assign productCategoryList = delegator.findByAnd("ProductCategoryMember",Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",productReport.productId))>
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
 	
 	
              <#assign retailStore = delegator.findByAnd("InventoryItem",Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",productReport.productId,"facilityId","MyRetailStore"))>
  	         <#assign TotalQ = 0/>
              <#list retailStore as ret>
			  <#assign TotalQ = TotalQ + ret.getBigDecimal("quantityOnHandTotal")?if_exists />
			 </#list>
			 
             <#assign warHouse = delegator.findByAnd("InventoryItem",Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",productReport.productId,"facilityId","10000"))>
  	         <#assign TotalQ1 = 0/>
            <#list warHouse as war>
				<#assign TotalQ1 = TotalQ1 + war.getBigDecimal("quantityOnHandTotal")?if_exists />
			</#list>
			 
			 <#assign displayLoc = delegator.findByAnd("InventoryItem",Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",productReport.productId,"facilityId","10001"))>
  	         <#assign TotalQ2 = 0/>
              <#list displayLoc as dis>
				<#assign TotalQ2 = TotalQ2 + dis.getBigDecimal("quantityOnHandTotal")?if_exists />
			 </#list>
			 
			  <#assign backStore = delegator.findByAnd("InventoryItem",Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",productReport.productId,"facilityId","10002"))>
  	         <#assign TotalQ3 = 0/>
              <#list backStore as bac>
			  <#assign TotalQ3 = TotalQ3 + bac.getBigDecimal("quantityOnHandTotal")?if_exists />
			 </#list>
	<#assign inventoryItemDetails = delegator.findByAnd("InventoryItem", {"productId" : productReport.productId,"partyId":productReport.partyId},["-datetimeReceived"])>
			 	
			<#assign inventoryItemInfo = delegator.findByAnd("InventoryItem", {"productId" : productReport.productId},["-datetimeReceived"])>
			<#if inventoryItemDetails?has_content>
				<#assign inventoryItemz = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(inventoryItemDetails) />
				<#assign invdate = Static["org.ofbiz.base.util.UtilDateTime"].toDateString(inventoryItemz.datetimeReceived,"dd/MM/yyyy")?if_exists />
			<#else>
				<#assign invdate = "_NA_"/>
			</#if>
			<#assign orderItemInfo = delegator.findByAnd("OrderItem", {"productId" : productReport.productId})>
			
			<#assign QOHTotal = 0/>
			<#assign QTYSoldTotal = 0/>
			<#assign QTYTotal = 0/>
			<#list inventoryItemInfo as inventoryItem>
				<#assign QOHTotal = QOHTotal+inventoryItem.getBigDecimal("quantityOnHandTotal")?if_exists />
			</#list>
			
			<#list orderItemInfo as orderItem>
				<#assign orderInfo = delegator.findOne("OrderHeader", {"orderId" : orderItem.orderId}, false)>
				<#if orderInfo.orderTypeId=="SALES_ORDER">
					<#assign QTYSoldTotal = QTYSoldTotal+orderItem.getBigDecimal("quantity")?if_exists />
				</#if>
			</#list>
			<#list inventoryItemInfo as inventoryItem>
				<#assign QTYTotal = QTYTotal+inventoryItem.getBigDecimal("accountingQuantityTotal")?if_exists />
			</#list>
		
		 <tr>
             <td>${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, productReport.partyId, false)?if_exists}</td>
             <td>${productReport.partyId?if_exists}</td>
             <td>${productReport.productId?if_exists}</td>
             <td>${productInfo.internalName?if_exists}</td>
           
            <td>${productInfo.modelNo?if_exists}</td>
              <td>${catalogName?if_exists}</td>
        <#if primaryCategoryName=="_NA_">
	        <td>${categoryInfo.categoryName?if_exists}</td>
	        <td>${primaryCategoryName?if_exists}</td>
        <#else>
	        <td>${primaryCategoryName?if_exists}</td>
	        <td>${categoryInfo.categoryName?if_exists}</td>
        </#if>
             
             <td>${productInfo.brandName?if_exists}</td>
             <td>${rebateRate?if_exists}</td>
             <td>${RSPRate?if_exists}</td>
             <td>${CPRate?if_exists}</td>
             <td>${QTYSoldTotal?if_exists}</td>           
             <td>${QOHTotal?if_exists}</td>
              <td>${TotalQ?if_exists}</td> 
             <td>${TotalQ1?if_exists}</td>
             <td>${TotalQ2?if_exists}</td>
             <td>${TotalQ3?if_exists}</td>
             <#if invdate!="_NA_">
             <td>${Static["org.ofbiz.base.util.UtilDateTime"].toDateString(inventoryItemz.datetimeReceived,"dd/MM/yyyy")}</td>
             <td>${Static["org.ofbiz.base.util.UtilDateTime"].getIntervalInDays(inventoryItemz.datetimeReceived, nowTimestamp)}</td>
			 <#else>
			 <td>NA</td>
			 <td>NA</td>
             </#if>
            <#--<td>${QTYTotal?if_exists}</td>-->
      </tr>
         </#list>

</table>