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
    <#-- list of orders -->
 
<br/><br/><br/><br/>
    <#-- list of terms -->
    <#if terms?has_content>
    <fo:table table-layout="fixed" width="100%" space-before="0.1in">
        <fo:table-column column-width="6.5in"/>

        <fo:table-header height="14px">
          <fo:table-row>
            <fo:table-cell>
              <fo:block font-weight="bold">${uiLabelMap.AccountingAgreementItemTerms}</fo:block>
            </fo:table-cell>
          </fo:table-row>
        </fo:table-header>

        <fo:table-body>
          <#list terms as term>
          <#assign termType = term.getRelatedOne("TermType")/>
          <fo:table-row>
            <fo:table-cell>
              <fo:block font-size ="10px" font-weight="bold" font-family="Times New Roman">${termType.description?if_exists} ${term.description?if_exists} ${term.termDays?if_exists} ${term.textValue?if_exists}</fo:block>
            </fo:table-cell>
          </fo:table-row>
          </#list>
        </fo:table-body>
    </fo:table>
    </#if>
	<fo:table width="95%" border-spacing="0pt"  border="1">
		 <fo:table-body>
			 <fo:table-row height="90px">
				<fo:table-cell height="90px">
				  <fo:block></fo:block>
				</fo:table-cell>
			  </fo:table-row>
		   </fo:table-body>
	</fo:table>
	<#--<fo:block-container  height="14.2cm">-->
    <fo:table margin-left="-21.5px" length="14cm" width="95%" border-spacing="0pt"  border="1">
			<fo:table-column column-width="15%"/><#-- Model -->
            <fo:table-column column-width="17%"  /><#-- Desc -->
            <fo:table-column column-width="6%"/><#-- Unit Price -->
            <fo:table-column column-width="5%"/><#-- Qty -->
			<fo:table-column column-width="5%"/><#-- Loc -->
            <fo:table-column column-width="10%"/><#-- Total -->

            <fo:table-column  column-width="7%"/><#-- Space -->

			
			<fo:table-column column-width="20%"/><#-- Model -->
            <fo:table-column  column-width="17%"/><#-- Desc -->
            <fo:table-column column-width="10%"/><#-- Unit Price -->
            <fo:table-column column-width="3%"/><#-- Qty -->
			<fo:table-column column-width="3%"/><#-- Loc -->
            <fo:table-column  column-width="10%"/><#-- Total -->


    

        
    <fo:table-body  font-size="10pt" >
        <#assign currentShipmentId = "">
        <#assign newShipmentId = "">
        <#-- if the item has a description, then use its description.  Otherwise, use the description of the invoiceItemType -->
        <#assign itemTotal = ZERO/>
        
        <#list returnItems as returnItem>
        	 <#assign facilityName = "NA"/>
            <#assign itemType = returnItem.getRelatedOne("InvoiceItemType")>
            <#assign isItemAdjustment = Static["org.ofbiz.entity.util.EntityTypeUtil"].hasParentType(delegator, "InvoiceItemType", "invoiceItemTypeId", itemType.getString("invoiceItemTypeId"), "parentTypeId", "INVOICE_ADJ")/>
            <#assign taxRate = returnItem.getRelatedOne("TaxAuthorityRateProduct")?if_exists>
            <#assign itemBillings = returnItem.getRelated("OrderItemBilling")?if_exists>
            <#if itemBillings?has_content>
                <#assign itemBilling = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(itemBillings)>
                <#assign orderItemRes = delegator.findByPrimaryKey("OrderReservationSummary", Static["org.ofbiz.base.util.UtilMisc"].toMap("orderId",itemBilling.orderId,"orderItemSeqId",returnItem.invoiceItemSeqId))>
                <#if orderItemRes?has_content>
                <#assign facilityInfo = delegator.findByPrimaryKey("Facility", Static["org.ofbiz.base.util.UtilMisc"].toMap("facilityId",orderItemRes.facilityId))>
                <#assign facilityName = facilityInfo.description?if_exists>
                </#if>
                <#if itemBilling?has_content>
                    <#assign itemIssuance = itemBilling.getRelatedOne("ItemIssuance")?if_exists>
                    <#if itemIssuance?has_content>
                        <#assign newShipmentId = itemIssuance.shipmentId>
                        <#assign issuedDateTime = itemIssuance.issuedDateTime/>
                    </#if>
                </#if>
            </#if>
            <#if returnItem.description?has_content>
                <#assign description=returnItem.description>
            <#elseif taxRate?has_content & taxRate.get("description",locale)?has_content>
                <#assign description=taxRate.get("description",locale)>
            <#elseif itemType.get("description",locale)?has_content>
                <#assign description=itemType.get("description",locale)>
            </#if>

            <#if newShipmentId?exists & newShipmentId != currentShipmentId>
                <#-- the shipment id is printed at the beginning for each
                     group of invoice items created for the same shipment
                -->
                <fo:table-row height="14px">
                    <fo:table-cell number-columns-spanned="5">
                            <fo:block></fo:block>
                       </fo:table-cell>
                </fo:table-row>
                <fo:table-row height="14px">
                   <fo:table-cell number-columns-spanned="5">
                        <fo:block font-size="10px" font-family="Times New Roman" font-weight="bold"> ${uiLabelMap.ProductShipmentId}: ${newShipmentId}<#if issuedDateTime?exists> ${uiLabelMap.CommonDate}: ${Static["org.ofbiz.base.util.UtilDateTime"].toDateString(issuedDateTime)}</#if></fo:block>
                   </fo:table-cell>
                </fo:table-row>
                <#assign currentShipmentId = newShipmentId>
            </#if>
<#assign total = ZERO/>
<#assign productInfo = delegator.findOne("Product", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",returnItem.productId ), true)>
<#assign bundleProductList = delegator.findByAnd("ProductAssoc", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",returnItem.productId,"productAssocTypeId","MANUF_COMPONENT" ))>


                <fo:table-row height="14px" >
                    <fo:table-cell  padding-top="3px">
                        <fo:block text-align="left">${productInfo.modelNo?if_exists}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell padding-top="3px" >
                    <#if productInfo.productTypeId=="MARKETING_PKG_AUTO">
						<fo:block text-align="left" font-weight="bold" font-size="10px" font-family="Times New Roman">
							<#if productInfo.productName?exists> ${productInfo.productName}
							<#elseif productInfo.internalName?exists> ${productInfo.internalName}
							</#if>
						</fo:block>
						<fo:block text-align="left"  font-size="8px" font-family="Times New Roman">
							<#list bundleProductList as bundledItem>
								<#assign item = delegator.findOne("Product", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",bundledItem.productIdTo ), true)>
								<#if item.productName?exists> ${item.productName}<fo:block/>
								<#elseif item.internalName?exists> ${item.internalName}<fo:block/>
								</#if>
							</#list>
						</fo:block>
					<#else>
						<fo:block text-align="left"  font-size="10px" font-family="Times New Roman">
							<#if productInfo.productName?exists> ${productInfo.productName}
							<#elseif productInfo.internalName?exists> ${productInfo.internalName}
							</#if>
						</fo:block>
					</#if>
                    </fo:table-cell>
                      <fo:table-cell padding-left="15px" padding-top="3px">
                        <fo:block font-size="10px" font-family="Times New Roman">${returnItem.getBigDecimal("returnPrice").setScale(decimals, rounding).toString()} </fo:block>
                    </fo:table-cell>
					<fo:table-cell padding-top="3px" padding-left="35px" text-align="center">
						<fo:block  font-size="10px" font-family="Times New Roman"> <#if returnItem.returnQuantity?exists>${returnItem.returnQuantity?string.number}</#if> </fo:block>
                    </fo:table-cell> 
					<#-- New Loc -->
					<fo:table-cell padding-top="3px" padding-left="35px" text-align="center">
						<fo:block  font-size="10px" font-family="Times New Roman">${facilityName?if_exists}</fo:block>
                    </fo:table-cell> 
					
					<#assign total = total.add(returnItem.getBigDecimal("returnQuantity").multiply(returnItem.getBigDecimal("returnPrice")))/>
                    <fo:table-cell text-align="right" padding-left="45px" padding-top="3px">
                        <fo:block font-size="10px" font-family="Times New Roman">${total.setScale(decimals, rounding).toString()} </fo:block>
                    </fo:table-cell>

	<fo:table-cell><fo:block/></fo:table-cell>


			<fo:table-cell  padding-top="3px" padding-left="40px">
                         <fo:block text-align="left">${productInfo.modelNo?if_exists}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell padding-top="3px" padding-left="14px">
                    <#if productInfo.productTypeId=="MARKETING_PKG_AUTO">
						<fo:block text-align="left" font-weight="bold" font-size="10px" font-family="Times New Roman">
							<#if productInfo.productName?exists> ${productInfo.productName}
							<#elseif productInfo.internalName?exists> ${productInfo.internalName}
							</#if>
						</fo:block>
						<fo:block text-align="left"  font-size="8px" font-family="Times New Roman">
							<#list bundleProductList as bundledItem>
								<#assign item = delegator.findOne("Product", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",bundledItem.productIdTo ), true)>
								<#if item.productName?exists> ${item.productName}<fo:block/>
								<#elseif item.internalName?exists> ${item.internalName}<fo:block/>
								</#if>
							</#list>
						</fo:block>
					<#else>
						<fo:block text-align="left"  font-size="10px" font-family="Times New Roman">
							<#if productInfo.productName?exists> ${productInfo.productName}
							<#elseif productInfo.internalName?exists> ${productInfo.internalName}
							</#if>
						</fo:block>
					</#if>
                    </fo:table-cell>
                    <fo:table-cell text-align="left" padding-top="3px" padding-left="40px">
                        <fo:block font-size="10px" font-family="Times New Roman"> ${returnItem.getBigDecimal("returnPrice").setScale(decimals, rounding).toString()} </fo:block>
                    </fo:table-cell>
					<fo:table-cell padding-top="3px" padding-left="25px" text-align="center">
                        <fo:block  font-size="10px" font-family="Times New Roman"> <#if returnItem.returnQuantity?exists>${returnItem.returnQuantity?string.number}</#if>
					</fo:block> 
                    </fo:table-cell> 
					
					<#-- New Loc -->
					<fo:table-cell padding-top="3px" padding-left="40px" text-align="center">
						<fo:block  font-size="10px" font-family="Times New Roman"> ${facilityName?if_exists} </fo:block>
                    </fo:table-cell> 
					
                    <#assign itemTotal = itemTotal.add(total)/>
                    <fo:table-cell padding-top="3px"  padding-left="65px">
                        <fo:block font-size="10px" font-family="Times New Roman">
							${total.setScale(decimals, rounding).toString()}
						</fo:block>
                    </fo:table-cell>


                </fo:table-row>
            
        </#list>

        <#-- blank line -->
        <fo:table-row height="7px" number-columns-spanned="11">
            <fo:table-cell number-columns-spanned="5"><fo:block><#-- blank line --></fo:block></fo:table-cell>
        </fo:table-row>

        <#-- the grand total -->
       


	

    </fo:table-body>
 </fo:table>
 <#--</fo:block-container>-->


<#if vatTaxIds?has_content>
 <fo:table>
    <fo:table-column column-width="105mm"/>
    <fo:table-column column-width="40mm"/>
    <fo:table-column column-width="25mm"/>

    <fo:table-header>
      <fo:table-row>
        <fo:table-cell>
          <fo:block/>
        </fo:table-cell>
        <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
          <fo:block font-weight="bold">${uiLabelMap.AccountingVat}</fo:block>
        </fo:table-cell>
        <fo:table-cell text-align="right" border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
          <fo:block font-weight="bold">${uiLabelMap.AccountingAmount}</fo:block>
        </fo:table-cell>
      </fo:table-row>
    </fo:table-header>

    <fo:table-body font-size="10pt">

    <#list vatTaxIds as vatTaxId>
    <#assign taxRate = delegator.findOne("TaxAuthorityRateProduct", Static["org.ofbiz.base.util.UtilMisc"].toMap("taxAuthorityRateSeqId", vatTaxId), true)/>
    <fo:table-row>
        <fo:table-cell>
          <fo:block/>
        </fo:table-cell>
        <fo:table-cell number-columns-spanned="1">
            <fo:block>${taxRate.description}</fo:block>
        </fo:table-cell>
        <fo:table-cell number-columns-spanned="1" text-align="right">
            <fo:block font-weight="bold"><@ofbizCurrency amount=vatTaxesByType[vatTaxId] isoCode=invoice.currencyUomId?if_exists/></fo:block>
        </fo:table-cell>
    </fo:table-row>
    </#list>
    </fo:table-body>
 </fo:table>
</#if>

 <#-- a block with the invoice message-->
 <#if invoice.invoiceMessage?has_content><fo:block>${invoice.invoiceMessage}</fo:block></#if>
 
 <fo:block margin-left="-21.5px"   border-width="thin" border-color="black" padding-top="7px" width="100%">
 
        <fo:footnote >
          <fo:inline/>
          <fo:footnote-body   border-width="thin" border-color="black" >
            <fo:block>
            
 <fo:table  width="100%">

	<fo:table-column column-width="35%"/>
	<fo:table-column column-width="15%"/>
	<fo:table-column column-width="45.7%"/>
	<fo:table-column column-width="8%"/>
 
	  <fo:table-body>
	  
 
		<#assign numbertoword = Static["org.ofbiz.accounting.invoice.NumberToWords"].convert(itemTotal.setScale(decimals, rounding).toString(),locale)/>
        <fo:table-row>
           <fo:table-cell padding-left="70px" border-width="thin" border-color="black">
             <fo:block font-size="8px" text-align="left" font-family="Times New Roman">${numbertoword?if_exists}</fo:block>
           </fo:table-cell>
           <fo:table-cell padding-left="120px" border-width="thin" border-color="black">
              <fo:block font-size="8px" text-align="left" font-family="Times New Roman">${itemTotal.setScale(decimals, rounding).toString()}
</fo:block>
           </fo:table-cell>

           <fo:table-cell padding-left="190px"  border-width="thin" border-color="black">
            <fo:block font-size="8px" text-align="left" font-family="Times New Roman">${numbertoword?if_exists}
			 </fo:block>
           </fo:table-cell>
		   <fo:table-cell  border-width="thin" border-color="black"  padding-left="160px">
              <fo:block font-size="8px"  font-family="Times New Roman">${itemTotal.setScale(decimals, rounding).toString()}</fo:block>
           </fo:table-cell>
        </fo:table-row>
        <fo:table-row height="21px">
           <fo:table-cell  number-columns-spanned="4">
              <fo:block/>
           </fo:table-cell>
        </fo:table-row>
        <fo:table-row>
           <fo:table-cell border-width="thin" border-color="black" number-columns-spanned="2" padding-right="120.4px">
			<fo:block font-size="8px" text-align="right" font-family="Times New Roman">
			    <#if cashierId?has_content>
			${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, cashierId, false)?if_exists}
			</#if>
			 </fo:block>	      
	       </fo:table-cell>
           <fo:table-cell padding-left="55px"  border-width="thin" border-color="black" number-columns-spanned="2" padding-right="50.4px">
			<fo:block font-size="8px" text-align="right" font-family="Times New Roman">
			<#if cashierId?has_content>
			${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, cashierId, false)?if_exists}
			</#if>
			 </fo:block>
           </fo:table-cell>
        </fo:table-row>
  </fo:table-body>
 </fo:table>
            </fo:block>
			
          </fo:footnote-body>
        </fo:footnote>
	 
 
      </fo:block>
	  
	  
</#escape>
