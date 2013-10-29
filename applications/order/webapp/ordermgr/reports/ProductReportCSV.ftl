<table>
	<tr rowspan="2">
		<th colspan="8" align="center" bold="bold">Product Details Report</th>
	</tr>
	<tr>
		<th>Product ID</th>
		<th>Catalog</th>
		<th>Category</th>
		<th>Brand</th>
		<th>RSP</th>
		<th>CP</th>
		<th>PROFIT(%)</th>
	</tr>
<#if productSummaryList?has_content>
<#list productSummaryList as item>
						<#assign CategoryInfo = delegator.findOne("ProductCategory", Static["org.ofbiz.base.util.UtilMisc"].toMap("productCategoryId",item.productCategoryId ), false)> 
						<#assign CatalogInfo = delegator.findOne("ProdCatalog", Static["org.ofbiz.base.util.UtilMisc"].toMap("prodCatalogId",item.prodCatalogId ), false)> 
                     	<#assign productRSPPriceInfos = delegator.findByAnd("ProductPrice", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",item.productId,"productPriceTypeId","DEFAULT_PRICE" ))>
                     	<#if productRSPPriceInfos?has_content>
                          <#assign productRSPPriceInfo = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(productRSPPriceInfos) />
                          <#assign RSPRate = productRSPPriceInfo.getBigDecimal("price")>
                        </#if> 
                        <#assign productCPriceInfos = delegator.findByAnd("ProductPrice", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",item.productId,"productPriceTypeId","AVERAGE_COST"))>
                        <#if productCPriceInfos?has_content>
                        <#assign productCPriceInfo = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(productCPriceInfos) />
                        <#assign CPRate = productCPriceInfo.getBigDecimal("price")>
                        <#else>
                        <#assign productCPriceInfo =productRSPPriceInfo/>
                        <#assign CPRate = productCPriceInfo.getBigDecimal("price")>
                        </#if>
                        <#assign profitRate = RSPRate.subtract(CPRate)>
	<tr>
		<td>${item.productId?if_exists}</td>
		<td>${supplierName?if_exists}</td>
		<td>${CatalogInfo.catalogName?if_exists}</td>
		<td>${CategoryInfo.categoryName?if_exists}</td>	
		<td>${item.brandName?if_exists}</td>
		<td>${RSPRate?if_exists}</td>
		<td>${CPRate?if_exists}</td>
		<td>${profitRate?if_exists}</td>
	</tr>
</#list>
<#else>
	<tr>
		<td colspan="8" align="center">No products found under this criteria</td>
	</tr>
</#if>
</table>