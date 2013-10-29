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
<#assign showToParty = !parameters.toPartyId?has_content>
<#assign showFromParty = !parameters.fromPartyId?has_content>

<#if security.hasEntityPermission("ORDERMGR", "_PURCHASE_VIEW", session)>

<#if productReportList?has_content>
          <fo:block font-size="14pt">Stock Report</fo:block>
          <fo:block space-after.optimum="10pt" font-size="10pt">
            <fo:table>
                <fo:table-column column-width="70pt"/>
                <fo:table-column column-width="100pt"/>
                   <fo:table-column column-width="90pt"/>
                    <fo:table-column column-width="70pt"/>
                     <fo:table-column column-width="40pt"/>
                      <fo:table-column column-width="40pt"/>
                      <fo:table-column column-width="100pt"/>
                   
                <fo:table-header>
                    <fo:table-row font-weight="bold">
                     <fo:table-cell border-bottom="thin solid grey"><fo:block>Product</fo:block></fo:table-cell>
                    <fo:table-cell border-bottom="thin solid grey"><fo:block>Catalog</fo:block></fo:table-cell>
                    <fo:table-cell border-bottom="thin solid grey"><fo:block>Category </fo:block></fo:table-cell>
                    <fo:table-cell border-bottom="thin solid grey"><fo:block>Subcategory </fo:block></fo:table-cell>
                    <fo:table-cell border-bottom="thin solid grey"><fo:block>Qh</fo:block></fo:table-cell>
                    <fo:table-cell border-bottom="thin solid grey"><fo:block>Atp </fo:block></fo:table-cell>
                    <fo:table-cell border-bottom="thin solid grey"><fo:block>Location</fo:block></fo:table-cell>
              
                    </fo:table-row>
                </fo:table-header>
                <fo:table-body>
                    <#assign rowColor = "white">
                    <#list productReportList as productReport>
                     
                     <#assign Catalog = delegator.findOne("ProdCatalog", {"prodCatalogId" : productReport.prodCatalogId}, false)>
                    <#assign Category = delegator.findOne("ProductCategory", {"productCategoryId" : productReport.productCategoryId}, false)>
					<#if Category.primaryParentCategoryId?has_content>
					<#assign parentCategory = delegator.findOne("ProductCategory", {"productCategoryId" : Category.primaryParentCategoryId}, false)>
					</#if>
					<#if parentCategory?has_content>
					<#assign parentCategoryName =parentCategory.categoryName>
					<#else>
					<#assign parentCategoryName ="_NA_">
					</#if>
                   <#assign Facility = delegator.findOne("Facility", {"facilityId" : productReport.facilityId}, false)>
					<#assign inventoryInfo =  dispatcher.runSync("getInventoryAvailableByFacility", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId", productReport.productId, "facilityId",productReport.facilityId ))/>
				   
                  <#-- <#assign Product = delegator.findOne("Product", {"productId" : productReport.productId}, true)>--> 
                        <fo:table-row>
                          <fo:table-cell padding="2pt" >
                                <fo:block>${productReport.productId?if_exists} </fo:block>
                            </fo:table-cell>
                        <fo:table-cell padding="2pt" >
                                <fo:block>${Catalog.catalogName?if_exists} </fo:block>
                            </fo:table-cell>
                             <fo:table-cell padding="2pt" >
                                <fo:block><#if parentCategoryName?has_content>${parentCategoryName?if_exists}</#if></fo:block>
                            </fo:table-cell>
                            
                                <fo:table-cell padding="2pt" >
                                <fo:block>  ${Category.categoryName?if_exists}     </fo:block>
                            </fo:table-cell>
                          
                                  <fo:table-cell padding="2pt" >
                                <fo:block>${inventoryInfo.availableToPromiseTotal?if_exists} </fo:block>
                            </fo:table-cell>
                                  <fo:table-cell padding="2pt" >
                                <fo:block>${inventoryInfo.quantityOnHandTotal?if_exists} </fo:block>
                            </fo:table-cell>
                                  <fo:table-cell padding="2pt" >
                                <fo:block>${Facility.facilityName?if_exists} </fo:block>
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
            ${"Product Not Found"}.
        </fo:block>
   </#if>

<#else>
    <fo:block font-size="14pt">
        ${uiLabelMap.OrderViewPermissionError}
    </fo:block>
</#if>
</#escape>
