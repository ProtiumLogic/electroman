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



<#if security.hasEntityPermission("ORDERMGR", "_VIEW", session)>
<fo:block space-before="5mm" space-after="5mm"> <fo:block/></fo:block>
<fo:block align ="center" space-before="5mm" space-after="5mm">      
<#if supplierProducts?has_content>
           <fo:block font-size="14pt" text-align ="center" space-before="5mm" >Credit Note</fo:block>
            <fo:block space-after="2mm" font-weight="bold" font-size="10pt">Supplier : ${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, supplierPartyId, false)?if_exists}</fo:block>
            <fo:block space-after="3mm" font-weight="bold" font-size="10pt">Date : ${Static["org.ofbiz.base.util.UtilDateTime"].toDateString(nowTimeStamp)}</fo:block>
            
            <fo:block  font-size="10pt">
            <fo:table >

	           <fo:table-column column-width="100px"/>
	       <!--<fo:table-column column-width="100px"/>-->
	           <fo:table-column column-width="150px"/>
	           <fo:table-column column-width="100px"/>
	           <fo:table-column column-width="60px"/>
	           <fo:table-column column-width="60px"/>
	           <fo:table-column column-width="60px"/>
	           <fo:table-column column-width="60px"/>
	           <fo:table-column column-width="60px"/>
	           <fo:table-column column-width="60px"/>
	           
                <fo:table-header>
                    <fo:table-row font-weight="bold">
                    <fo:table-cell border-bottom="thin solid grey"><fo:block>${"Product ID"}</fo:block></fo:table-cell>
                        <!--<fo:table-cell border-bottom="thin solid grey"><fo:block>${"Supplier ProductId"}</fo:block></fo:table-cell>-->
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${"Product Name"}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${"Curr CP"}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${"New CP"}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${"Product Stock"}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${"Prevailing Cost Value"}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${"Revised Cost Value"}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${"Diff Cost Value"}</fo:block></fo:table-cell>
                    </fo:table-row>
                </fo:table-header>
                <fo:table-body>
                <#assign PreCostTotal = ZERO/>
				<#assign RevCostTotal = ZERO/>
				<#assign DiffCostTotal = ZERO/>
				<#assign updatedLastPrice = ZERO/>
				<#assign StockTotal = 0/>
		        <#list supplierProducts as item>
						<#assign inventoryItemDetails = delegator.findByAnd("InventoryItem", {"productId" : item.productId,"partyId":supplierPartyId},["-datetimeReceived"])>
						<#assign productInfo = delegator.findOne("Product", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",item.productId ), false)>

						<#assign QOHTotal = 0/>
						<#assign PreCostVal = 0/>
						<#assign RevCostVal = 0/>
						<#assign DiffCostVal = 0/>
						<#assign QTYSoldTotal = 0/>
						<#assign QTYTotal = 0/>
						<#list inventoryItemDetails as inventoryItem>
							<#assign QOHTotal = QOHTotal+inventoryItem.getBigDecimal("quantityOnHandTotal")?if_exists />
						</#list>
						<#if item.updatedLastPrice? if_exists>
							<#assign updatedLastPrice = item.getBigDecimal("updatedLastPrice")/>
						
						</#if>
						<#assign PreCostVal = item.getBigDecimal("updatedLastPrice").multiply(QOHTotal)/>
						<#assign RevCostVal = item.getBigDecimal("lastPrice").multiply(QOHTotal)/>
						<#assign DiffCostVal = PreCostVal.subtract(RevCostVal)/>
						<#assign PreCostTotal =PreCostTotal.add (PreCostVal)/>
						<#assign RevCostTotal =RevCostTotal.add(RevCostVal)/>
						<#assign DiffCostTotal =DiffCostTotal.add(DiffCostVal)/>
						<#assign StockTotal =StockTotal+QOHTotal/>
						<fo:table-row>
                            <fo:table-cell padding="2pt" background-color="">
                                <fo:block>${item.productId?if_exists}</fo:block>
                            </fo:table-cell>
                            <#--<fo:table-cell padding="2pt" background-color="">
                                <fo:block>${item.supplierProductId?if_exists}</fo:block>
                            </fo:table-cell>-->
                            <fo:table-cell padding="2pt" background-color="">
                                <fo:block>${productInfo.internalName?if_exists}</fo:block>
                            </fo:table-cell>
                            
                            <fo:table-cell padding="2pt" background-color="">
                                <fo:block>${item.getBigDecimal("updatedLastPrice").setScale(decimals, rounding).toString()?if_exists}</fo:block>
                            </fo:table-cell>
							<fo:table-cell padding="2pt" background-color="">
                                <fo:block>${item.getBigDecimal("lastPrice").setScale(decimals, rounding).toString()?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="">
                                <fo:block>${QOHTotal?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="">
                                <fo:block>${PreCostVal.setScale(decimals, rounding).toString()?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="">
                                <fo:block>${RevCostVal.setScale(decimals, rounding).toString()?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="">
                                <fo:block>${DiffCostVal.setScale(decimals, rounding).toString()?if_exists}</fo:block>
                            </fo:table-cell>
                   </fo:table-row>
				</#list>
				<fo:table-row border-bottom="thin solid grey">
				<fo:table-cell>
                                <fo:block></fo:block>
                            </fo:table-cell>
				</fo:table-row>
				<fo:table-row>
				<fo:table-cell>
                                <fo:block></fo:block>
                            </fo:table-cell>
				</fo:table-row>
				 <fo:table-row font-weight="bold" font-size="11pt">
				 <fo:table-cell padding="2pt" background-color="" number-columns-spanned="4" text-align="center">
                                <fo:block >Total</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="">
                                <fo:block>${StockTotal?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="">
                                <fo:block>${PreCostTotal.setScale(decimals, rounding).toString()?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="">
                                <fo:block>${RevCostTotal.setScale(decimals, rounding).toString()?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="">
                                <fo:block>${DiffCostTotal.setScale(decimals, rounding).toString()?if_exists}</fo:block>
                            </fo:table-cell>

				  </fo:table-row>
				  <fo:table-row>
					 <fo:table-cell height="1.0cm">
	                      <fo:block space-after="1.0cm"></fo:block>
	                 </fo:table-cell>
	                 <fo:table-cell number-columns-spanned="5" text-align="right">
	                      <fo:block space-after="1.0cm"><fo:block/> </fo:block>
	                 </fo:table-cell>
				</fo:table-row>
				 <fo:table-row>
					 <fo:table-cell >
	                      <fo:block space-after="1.0cm"> Approved By</fo:block>
	                 </fo:table-cell>
	                 <fo:table-cell number-columns-spanned="5" text-align="right">
	                      <fo:block space-after="1.0cm"> Approved By</fo:block>
	                 </fo:table-cell>
				</fo:table-row>
				<fo:table-row height="1.0cm">
					 <fo:table-cell >
	                      <fo:block space-after="1.0cm"><fo:block/></fo:block>
	                 </fo:table-cell>
	                 <fo:table-cell number-columns-spanned="5" text-align="right">
	                      <fo:block space-after="1.0cm"><fo:block/> </fo:block>
	                 </fo:table-cell>
				</fo:table-row>
				<fo:table-row>
					 <fo:table-cell>
	                      <fo:block> Buyer</fo:block>
	                 </fo:table-cell>
	                 <fo:table-cell number-columns-spanned="5" text-align="right">
	                      <fo:block > Supplier</fo:block>
	                 </fo:table-cell>
				</fo:table-row>
				  
                </fo:table-body>
            </fo:table>
          </fo:block>
		<#else>
   
        <fo:block font-size="14pt">
            No Supplier Products Found!
        </fo:block>
  
</#if>
</fo:block>
<#else>
    <fo:block font-size="14pt">
        ${uiLabelMap.OrderViewPermissionError}
    </fo:block>
</#if>

</#escape>
