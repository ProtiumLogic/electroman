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
    <#if orderHeader?has_content>
        <fo:table border-spacing="3pt" border-style="solid" border-separation="3pt">
	    <fo:table-column column-width="0.5in"/>
            <fo:table-column column-width="1.2in"/>
            <fo:table-column column-width="0.8in"/>
            <fo:table-column column-width="2.6in"/>
            <fo:table-column column-width="1.4in"/>
            <fo:table-column column-width="1.in"/>
            <fo:table-column column-width="1.3in"/>
            <fo:table-column column-width="1.3in"/>
            <fo:table-header>
                <fo:table-row>
                    <fo:table-cell text-align="center" border-style="solid" height="30px" valign="middle">
                        <fo:block font-weight="bold" font-family="Times New Roman" padding-top="10px" font-size="10px" >${"Sl No."}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell text-align="center" border-style="solid">
                        <fo:block font-weight="bold" font-family="Times New Roman" padding-top="10px" font-size="10px">${"Item Code"}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell text-align="center" border-style="solid">
                        <fo:block font-weight="bold" font-family="Times New Roman" padding-top="10px" font-size="10px">${"Model No."}</fo:block>
                    </fo:table-cell>
		    		<fo:table-cell text-align="center" border-style="solid">
                        <fo:block font-weight="bold" font-family="Times New Roman" padding-top="10px" font-size="10px">${"Description"}</fo:block>
                    </fo:table-cell>
                      <#-- brand name insertion-->
                                    <fo:table-cell text-align="center" border-style="solid">
                        <fo:block font-weight="bold" font-family="Times New Roman" padding-top="10px" font-size="10px">${"Brand Name"}</fo:block>
                    </fo:table-cell>      
                    
                    <fo:table-cell text-align="center" border-style="solid">
                        <fo:block font-weight="bold" font-family="Times New Roman" padding-top="10px" font-size="10px">${uiLabelMap.OrderQuantity}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell text-align="center" border-style="solid">
                        <fo:block font-weight="bold" font-family="Times New Roman" padding-top="10px" font-size="10px">${"Unit Price"}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell text-align="center" border-style="solid">
                        <fo:block font-weight="bold" font-family="Times New Roman" padding-top="10px" font-size="10px">${uiLabelMap.OrderSubTotal}</fo:block>
                    </fo:table-cell>
                </fo:table-row>
            </fo:table-header>
            <fo:table-body>
<#assign itemNo=1>
				
                <#list orderItemList as orderItem>
				
                    <#assign orderItemType = orderItem.getRelatedOne("OrderItemType")?if_exists>
                    <#assign productId = orderItem.productId?if_exists>
                    <#assign brandId= orderItem.brandId?if_exists>
					<#assign productInfo = delegator.findOne("Product", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",productId ), true)>
                    <#assign remainingQuantity = (orderItem.quantity?default(0) - orderItem.cancelQuantity?default(0))>
                    <#assign itemAdjustment = Static["org.ofbiz.order.order.OrderReadHelper"].getOrderItemAdjustmentsTotal(orderItem, orderAdjustments, true, false, false)>

			

                    <fo:table-row>
                        <fo:table-cell padding-top="5px" text-align="center" height="20px"  border-style="solid" >
									<fo:block font-family="Times New Roman"  font-size="10px">${itemNo}
												<#assign itemNo=itemNo+1>
			   						 </fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding-top="5px" text-align="left" border-style="solid" >
                            <fo:block font-family="Times New Roman"  font-size="10px" >${productId}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding-top="5px" text-align="left" border-style="solid" >
                            <fo:block font-family="Times New Roman"  font-size="10px" >${productInfo.modelNo?if_exists}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding-top="5px" text-align="left" border-style="solid">
                            <fo:block font-family="Times New Roman"  font-size="10px">${productInfo.description?if_exists}
                            
                            <#--
                                <#if orderItem.supplierProductId?has_content>
                                   ${orderItem.itemDescription?if_exists}
                                <#elseif productId?exists>
                                    ${orderItem.itemDescription?if_exists}
                                <#elseif orderItemType?exists>
                                    ${orderItem.itemDescription?if_exists}
                                <#else>
                                    ${orderItem.itemDescription?if_exists}
                                </#if>
                                -->
                                
                            </fo:block>
                        </fo:table-cell>
                         <fo:table-cell padding-top="5px"  text-align="center" border-style="solid">
                        <fo:block font-family="Times New Roman"  font-size="10px">${productInfo.brandName?if_exists}</fo:block>
                          </fo:table-cell> 
                          
                        <fo:table-cell padding-top="5px" text-align="center" border-style="solid">
                            <fo:block font-family="Times New Roman"  font-size="10px">${remainingQuantity}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding-top="5px" text-align="right" border-style="solid" padding-right="5px" >
                            <fo:block font-family="Times New Roman"  font-size="10px" ><@ofbizCurrency amount=orderItem.unitPrice isoCode=currencyUomId/></fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding-top="5px" text-align="right" border-style="solid"  padding-right="5px">
                            <fo:block font-family="Times New Roman"  font-size="10px">
                                <#if orderItem.statusId != "ITEM_CANCELLED">
                                    <@ofbizCurrency amount=Static["org.ofbiz.order.order.OrderReadHelper"].getOrderItemSubTotal(orderItem, orderAdjustments) isoCode=currencyUomId/>
									
                                <#else>
                                    <@ofbizCurrency amount=0.00 isoCode=currencyUomId/>
                                </#if>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>

<#--
                    <#if itemAdjustment != 0>
                        <fo:table-row>
                            <fo:table-cell number-columns-spanned="2" >
                                <fo:block text-indent="0.2in" font-family="Times New Roman"  font-size="10px">
                                    <fo:inline font-style="italic">${uiLabelMap.OrderAdjustments}</fo:inline>
                                    : <@ofbizCurrency amount=itemAdjustment isoCode=currencyUomId/>
                                </fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </#if>
-->
                </#list>
                <#list orderHeaderAdjustments as orderHeaderAdjustment>
                    <#assign adjustmentType = orderHeaderAdjustment.getRelatedOne("OrderAdjustmentType")>
                    <#assign adjustmentAmount = Static["org.ofbiz.order.order.OrderReadHelper"].calcOrderAdjustment(orderHeaderAdjustment, orderSubTotal)>
<#--
                    <#if adjustmentAmount != 0>
                        <fo:table-row>
                            <fo:table-cell></fo:table-cell>
                            <fo:table-cell number-columns-spanned="2">
                                <fo:block font-weight="bold">
                                    ${adjustmentType.get("description",locale)} :
                                    <#if orderHeaderAdjustment.get("description")?has_content>
                                        (${orderHeaderAdjustment.get("description")?if_exists})
                                    </#if>
                                </fo:block>
                            </fo:table-cell>
                            <fo:table-cell text-align="right">
                                <fo:block><@ofbizCurrency amount=adjustmentAmount isoCode=currencyUomId/></fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </#if>
-->
                </#list>
                <#-- summary of order amounts -->
                <fo:table-row>
                    <fo:table-cell number-columns-spanned="5" height="20px"></fo:table-cell>
                    <fo:table-cell padding-right="5px" padding-top="3px" number-columns-spanned="2" border-style="solid" cellspacing="10" >
                        <fo:block font-weight="bold" text-indent="5mm" font-family="Times New Roman"  font-size="10px">${uiLabelMap.OrderItemsSubTotal}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell padding-right="5px" padding-top="3px" text-align="right" border-style="solid">
                        <fo:block font-family="Times New Roman"  font-size="10px"><@ofbizCurrency amount=orderSubTotal isoCode=currencyUomId/></fo:block>
                    </fo:table-cell>
                </fo:table-row>
                <#if otherAdjAmount != 0>
                    <fo:table-row>
                        <fo:table-cell height="20px" number-columns-spanned="5"></fo:table-cell>
                        <fo:table-cell padding-top="3px" padding-right="5px" number-columns-spanned="2" border-style="solid">
                            <fo:block font-weight="bold" font-family="Times New Roman"  font-size="10px" text-indent="5mm">${uiLabelMap.OrderTotalOtherOrderAdjustments}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding-top="3px" padding-right="5px" text-align="right" border-style="solid">
                            <fo:block font-family="Times New Roman"  font-size="10px"><@ofbizCurrency amount=otherAdjAmount isoCode=currencyUomId/></fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </#if>
                <#if shippingAmount != 0>
                    <fo:table-row>
                        <fo:table-cell height="20px" number-columns-spanned="5"></fo:table-cell>
                        <fo:table-cell padding-top="3px" number-columns-spanned="2" border-style="solid">
                            <fo:block font-weight="bold" text-indent="5mm">${uiLabelMap.OrderTotalShippingAndHandling}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding-top="3px" padding-right="5px" text-align="right" border-style="solid">
                            <fo:block><@ofbizCurrency amount=shippingAmount isoCode=currencyUomId/></fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </#if>
                <#if taxAmount != 0>
                    <fo:table-row>
                        <fo:table-cell height="20px" number-columns-spanned="5"></fo:table-cell>
                        <fo:table-cell padding-top="3px" padding-right="5px" number-columns-spanned="2" border-style="solid">
                            <fo:block font-family="Times New Roman"  font-size="10px" font-weight="bold" text-indent="5mm">${uiLabelMap.OrderTotalSalesTax}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding-top="3px" padding-right="5px" text-align="right" border-style="solid">
                            <fo:block font-family="Times New Roman"  font-size="10px"><@ofbizCurrency amount=taxAmount isoCode=currencyUomId/></fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </#if>
                <#if grandTotal != 0>
                    <fo:table-row>
                        <fo:table-cell height="20px" number-columns-spanned="5" background-color="#EEEEEE" >
                        	<fo:block font-weight="bold" font-family="Times New Roman"  font-size="10px" text-indent="5mm">
								${orderTotalInWords}</fo:block>
						</fo:table-cell>
                        <fo:table-cell padding-top="3px" number-columns-spanned="2" padding-right="5px" background-color="#EEEEEE" border-style="solid">
                            <fo:block font-weight="bold" font-family="Times New Roman"  font-size="10px" text-indent="5mm">${"Total Amount"}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding-top="3px" padding-right="5px" text-align="right" background-color="#EEEEEE" border-style="solid">
                            <fo:block font-family="Times New Roman"  font-size="10px"><@ofbizCurrency amount=grandTotal isoCode=currencyUomId/>
							<#--
							<fo:external-graphic src="<@ofbizContentUrl>${'./applications/order/images/price_space_right1.jpg'}</@ofbizContentUrl>" />
							-->
							</fo:block>
                        </fo:table-cell>
						
                    </fo:table-row>
                </#if>
                <#-- notes -->
                <#if orderNotes?has_content>
                    <#if showNoteHeadingOnPDF>
                        <fo:table-row>
                            <fo:table-cell number-columns-spanned="3">
                                <fo:block font-weight="bold">${uiLabelMap.OrderNotes}</fo:block>
                                <fo:block>
                                    <fo:leader leader-length="19cm" leader-pattern="rule"/>
                                </fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </#if>
                    <#list orderNotes as note>
                        <#if (note.internalNote?has_content) && (note.internalNote != "Y")>
                            <fo:table-row>
                                <fo:table-cell number-columns-spanned="1">
                                    <fo:block>${note.noteInfo?if_exists}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell number-columns-spanned="2">
                                    <#if note.noteParty?has_content>
                                        <#assign notePartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", note.noteParty, "compareDate", note.noteDateTime, "lastNameFirst", "Y", "userLogin", userLogin))/>
                                        <fo:block>${uiLabelMap.CommonBy}: ${notePartyNameResult.fullName?default("${uiLabelMap.OrderPartyNameNotFound}")}</fo:block>
                                    </#if>
                                </fo:table-cell>
                                <fo:table-cell number-columns-spanned="1">
                                    <fo:block>${uiLabelMap.CommonAt}: ${note.noteDateTime?string?if_exists}</fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                        </#if>
                    </#list>
                </#if>
            </fo:table-body>
        </fo:table>
    </#if>
</#escape>
