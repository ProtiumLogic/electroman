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

<#assign showProductStore = !parameters.productStoreId?has_content>
<#assign showOriginFacility = !parameters.originFacilityId?has_content>
<#assign showTerminal = !parameters.terminalId?has_content>
<#assign showStatus = !parameters.statusId?has_content>
<#assign showOrderId = !parameters.orderId?has_content>

<#if orderPurchaseProductSummaryList?has_content>
        <fo:table margin-top="10px" border-spacing="3pt" border-style="solid" border-width="0.8pt" border-separation="3pt">
			<fo:table-column column-width="0.4in"/>
            <fo:table-column column-width="1in"/>
            <fo:table-column column-width="1in"/>
            <fo:table-column column-width="0.8in"/>
            <fo:table-column column-width="2.2in"/>
            <fo:table-column column-width="0.8in"/>
            <fo:table-column column-width="0.8in"/>
            <fo:table-column column-width="0.8in"/>
            <fo:table-column column-width="0.9in"/>
            <fo:table-column column-width="1.0in"/>
            <fo:table-header>
                <fo:table-row>
                    <fo:table-cell text-align="center" border-style="solid" height="30px" valign="middle" border-width="0.8pt">
                        <fo:block font-weight="bold" font-family="Times New Roman" padding-top="10px" font-size="10px" >${"Sl No."}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell text-align="center" border-style="solid" border-width="0.8pt">
                        <fo:block font-weight="bold" font-family="Times New Roman" padding-top="10px" font-size="10px">${"Order ID"}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell text-align="center" border-style="solid" border-width="0.8pt">
                        <fo:block font-weight="bold" font-family="Times New Roman" padding-top="10px" font-size="10px">${"Supplier"}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell text-align="center" border-style="solid" border-width="0.8pt">
                        <fo:block font-weight="bold" font-family="Times New Roman" padding-top="10px" font-size="10px">${"Product ID"}</fo:block>
                    </fo:table-cell>
		   			<fo:table-cell text-align="center" border-style="solid" border-width="0.8pt">
                        <fo:block font-weight="bold" font-family="Times New Roman" padding-top="10px" font-size="10px">${"Description"}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell text-align="center" border-style="solid" border-width="0.8pt">
                        <fo:block font-weight="bold" font-family="Times New Roman" padding-top="10px" font-size="10px">${"Quantity Ordered"}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell text-align="center" border-style="solid" border-width="0.8pt">
                        <fo:block font-weight="bold" font-family="Times New Roman" padding-top="10px" font-size="10px">${"Quantity Recieved"}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell text-align="center" border-style="solid" border-width="0.8pt">
                        <fo:block font-weight="bold" font-family="Times New Roman" padding-top="10px" font-size="10px">${"Quantity Canceled"}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell text-align="center" border-style="solid" border-width="0.8pt">
                        <fo:block font-weight="bold" font-family="Times New Roman" padding-top="10px" font-size="10px">${"Unit Value"}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell text-align="center" border-style="solid" border-width="0.8pt">
                        <fo:block font-weight="bold" font-family="Times New Roman" padding-top="10px" font-size="10px">${"Total Amount"}</fo:block>
                    </fo:table-cell>
                </fo:table-row>
            </fo:table-header>
            <fo:table-body>
<#assign itemNo=1>
				
                <#list orderPurchaseProductSummaryList as orderPurchaseProductSummary>
					
					
						<#assign orderQuantity= orderPurchaseProductSummary.quantity>
						<#assign productInfo = delegator.findOne("Product", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",orderPurchaseProductSummary.productId ), true)> 
						<#--<#assign cancelQuantity= orderPurchaseProductSummary.cancelQuantity?has_content>-->
						<#--<#assign receivedQuantity= orderQuantity - cancelQuantity?has_content>-->
						
				 <#assign totalReceived = 0.0>
					<#assign shipmentReceipts = delegator.findByAnd("ShipmentReceipt", {"orderId" : orderPurchaseProductSummary.orderId})/>
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
                             
                    <fo:table-row>
                        <fo:table-cell padding-top="5px" text-align="center" height="20px"  border-style="solid" border-width="0.8pt">
								<fo:block font-family="Times New Roman"  font-size="10px" >${itemNo}
									<#assign itemNo=itemNo+1>
			    				</fo:block>

                        </fo:table-cell>
                        <fo:table-cell padding-top="5px" text-align="left" border-style="solid" border-width="0.8pt">
                            <fo:block font-family="Times New Roman"  font-size="10px" text-indent="3px">${orderPurchaseProductSummary.orderId?if_exists}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding-top="5px" text-align="left" border-style="solid" border-width="0.8pt">
                            <fo:block font-family="Times New Roman"  font-size="10px" text-indent="3px">
                            ${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, orderPurchaseProductSummary.partyId, false)?if_exists}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding-top="5px" text-align="left" border-style="solid" border-width="0.8pt">
                            <fo:block font-family="Times New Roman"  font-size="10px" text-indent="3px">${orderPurchaseProductSummary.productId?if_exists}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding-top="5px" text-align="left" border-style="solid" border-width="0.8pt">
                            <fo:block font-family="Times New Roman"  font-size="10px" text-indent="3px">${productInfo.description?if_exists}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding-top="5px" text-align="center" border-style="solid" border-width="0.8pt">
                            <fo:block font-family="Times New Roman"  font-size="10px" text-indent="3px">${orderQuantity?if_exists}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding-top="5px" text-align="center" border-style="solid" padding-right="5px" border-width="0.8pt">
                            <fo:block font-family="Times New Roman"  font-size="10px" text-indent="3px">${quantityAccepted?if_exists}${orderCurrencyUnitPriceMap?if_exists}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding-top="5px" text-align="center" border-style="solid" border-width="0.8pt" padding-right="5px">
                            <fo:block font-family="Times New Roman"  font-size="10px" text-indent="3px">${quantityRejected?if_exists}
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding-top="5px" text-align="center" border-style="solid" padding-right="5px" border-width="0.8pt">
                            <fo:block font-family="Times New Roman"  font-size="10px" text-indent="3px">${orderPurchaseProductSummary.unitPrice?if_exists}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding-top="5px" text-align="center" border-style="solid" border-width="0.8pt" padding-right="5px">
                            <fo:block font-family="Times New Roman"  font-size="10px" text-indent="3px">${orderPurchaseProductSummary.grandTotal?if_exists}
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>

                </#list>
            </fo:table-body>
        </fo:table>
    <#else>
    	<fo:block font-family="Times New Roman"  font-size="20px" text-align="center" color="blue" padding-top="20px">Sorry, no order found.
                            </fo:block>
    </#if>
</#escape>
