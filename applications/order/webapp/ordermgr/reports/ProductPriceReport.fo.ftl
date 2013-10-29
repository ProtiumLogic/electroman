<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<#escape x as x?xml>

<#-- do not display columns associated with values specified in the request, ie constraint values -->
<#assign showProductStore = !parameters.productStoreId?has_content>
<#assign showOriginFacility = !parameters.originFacilityId?has_content>
<#assign showTerminal = !parameters.terminalId?has_content>
<#assign showStatus = !parameters.statusId?has_content>


<#if security.hasEntityPermission("ORDERMGR", "_VIEW", session)>
<fo:block space-before="5mm" space-after="5mm"> <fo:block/></fo:block>
<fo:block align ="center" space-before="5mm" space-after="5mm">      
<#if productSummaryList?has_content>
           <fo:block font-size="14pt">${uiLabelMap.OrderReportPurchasesByProduct}</fo:block>
             <fo:block space-after.optimum="40pt" font-size="10pt">
            <fo:table >
                <#if showProductStore><fo:table-column column-width="70pt"/></#if>
                <#if showOriginFacility><fo:table-column column-width="70pt"/></#if>
                <#if showTerminal><fo:table-column column-width="50pt"/></#if>
                <#if showStatus><fo:table-column column-width="50pt"/></#if>
	           <fo:table-column column-width="100px"/>
	           <fo:table-column column-width="100px"/>
	           <fo:table-column column-width="100px"/>
	           <fo:table-column column-width="100px"/>
	           <fo:table-column column-width="100px"/>
	           <fo:table-column column-width="100px"/>
	           <fo:table-column column-width="150px"/>
	           <fo:table-column column-width="150px"/>
                <fo:table-header>
                    <fo:table-row font-weight="bold">
                    <fo:table-cell border-bottom="thin solid grey"><fo:block>${"Product ID"}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${"Description"}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${"Catalog"}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${"Category"}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${"Sub Category"}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${"Brand Name"}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${"RSP"}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${"CP"}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${"Profit (%)"}</fo:block></fo:table-cell>
                        
                    </fo:table-row>
                </fo:table-header>
                <fo:table-body>
                    <#assign rowColor = "white">
                    <#list productSummaryList as listProduct>
						<#assign CategoryInfo = delegator.findOne("ProductCategory", Static["org.ofbiz.base.util.UtilMisc"].toMap("productCategoryId",listProduct.productCategoryId ), false)> 
						<#assign CatalogInfo = delegator.findOne("ProdCatalog", Static["org.ofbiz.base.util.UtilMisc"].toMap("prodCatalogId",listProduct.prodCatalogId ), false)> 
                     	<#assign ProductRSPPriceInfos = delegator.findByAnd("ProductPrice", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",listProduct.productId,"productPriceTypeId","DEFAULT_PRICE" ))>
                     	<#if ProductRSPPriceInfos?has_content>
                          <#assign ProductRSPPriceInfo = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(ProductRSPPriceInfos) />
                          <#assign RSPRate = ProductRSPPriceInfo.getBigDecimal("price")>
                        </#if> 
                        <#assign ProductCPriceInfos = delegator.findByAnd("ProductPrice", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",listProduct.productId,"productPriceTypeId","AVERAGE_COST"))>
                        <#if ProductCPriceInfos?has_content>
                        <#assign ProductCPriceInfo = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(ProductCPriceInfos) />
                        <#assign CPRate = ProductCPriceInfo.getBigDecimal("price")>
                        <#else>
                        <#assign ProductCPriceInfo =ProductRSPPriceInfo/>
                        <#assign CPRate = ProductCPriceInfo.getBigDecimal("price")>
                        </#if>
                      	<#assign profitRate = RSPRate.subtract(CPRate)>
                      	 <#if CategoryInfo.primaryParentCategoryId?has_content>
							<#assign parentCategory = delegator.findOne("ProductCategory", {"productCategoryId" : CategoryInfo.primaryParentCategoryId}, false)>
							</#if>
							<#if parentCategory?has_content>
							<#assign parentCategoryName =parentCategory.categoryName>
							<#else>
							<#assign parentCategoryName ="_NA_">
							</#if>
                      	
                      	
                      	
                        
                        <fo:table-row>
                            <fo:table-cell padding="2pt" background-color="">
                                <fo:block>${listProduct.productId?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="">
                                <fo:block>${listProduct.internalName?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="">
                                <fo:block>${CatalogInfo.catalogName?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="">
                                <fo:block>${CategoryInfo.categoryName?if_exists}</fo:block>
                            </fo:table-cell>
                          
                            <fo:table-cell padding="2pt" background-color="">
                                <fo:block>${parentCategoryName?if_exists}</fo:block>
                            </fo:table-cell>
                            
                               <fo:table-cell padding="2pt" background-color="">
                                <fo:block>${listProduct.brandName?if_exists}</fo:block>
                            </fo:table-cell>
                            
                            
                            
                            
                            
                            
                            <fo:table-cell padding="2pt" background-color="">
                           <fo:block>${RSPRate?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="">
                           <fo:block><#if ProductCPriceInfo?has_content>${CPRate?if_exists}</#if></fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="">
                                <fo:block>${profitRate}</fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                        <#-- toggle the row color -->
                        <#if rowColor == "white">
                            <#assign rowColor = "#D4D0C8">
                        <#else>
                            <#assign rowColor = "white">
                        </#if>
                    </#list>
                </fo:table-body>
            </fo:table>
            </fo:block>

<#else>
   
        <fo:block font-size="14pt">
            ${uiLabelMap.OrderNoPurchaseProduct}
        </fo:block>
  
</#if>
</fo:block>
<#else>
    <fo:block font-size="14pt">
        ${uiLabelMap.OrderViewPermissionError}
    </fo:block>
</#if>

</#escape>
