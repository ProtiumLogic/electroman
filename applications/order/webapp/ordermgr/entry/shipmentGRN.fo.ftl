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
    <#if hasPermission>
    				<#assign i = 1/>
					<#list shipmentReceipts as shipmentItem>
					<#assign orderId = shipmentItem.get("orderId")>
					</#list>
                    <fo:block><fo:leader/></fo:block>
                    <fo:block font-size="14pt">GRN for Order: #${orderId}</fo:block>
                    <fo:block><fo:leader/></fo:block>
                    <fo:block space-after.optimum="10pt" font-size="10pt">
                   
                    </fo:block>
                    
                    
                    <fo:table>
                        <fo:table-column column-width="60pt"/>
                        <fo:table-column column-width="140pt"/>
                        <fo:table-column column-width="120pt"/>
                        <fo:table-column column-width="120pt"/>
                        <fo:table-column column-width="140pt"/>
                        <fo:table-column column-width="90pt"/>
                        
                        <fo:table-header>
                            <fo:table-row font-weight="bold">
                            	<fo:table-cell padding="2pt" background-color="#D4D0C8">
                                    <fo:block>S.No</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="#D4D0C8">
                                    <fo:block>${uiLabelMap.ProductProductId}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="#D4D0C8">
                                    <fo:block>Accepted Quantity</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="#D4D0C8">
                                    <fo:block>Rejected Quantity</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="#D4D0C8">
                                    <fo:block>Recieved By</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="#D4D0C8">
                                    <fo:block>Recieved Date</fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                        </fo:table-header>
                        <fo:table-body>
                            <#list shipmentReceipts as shipmentItem>
                              	<#assign userName ="NA"/>
                                <#assign shipmentItem = shipmentItem.get("shipmentItem")>
								<#assign orderId = shipmentItem.get("orderId")>
                                <#assign acceptedQTY = shipmentItem.get("quantityAccepted")>
                                <#assign rejectedQTY = shipmentItem.get("quantityRejected")>
                                <#assign receviedDateTime = shipmentItem.get("datetimeReceived")>
\                                <#assign product = shipmentItem.getRelatedOne("Product")>
                                <#assign itemIssuances = shipmentItem.getRelated("ItemIssuance")>
                                <#assign userLoginAndPartyDetails = delegator.findByAnd("UserLoginAndPartyDetails", Static["org.ofbiz.base.util.UtilMisc"].toMap("userLoginId", shipmentItem.receivedByUserLoginId))?if_exists>
	                     		<#if userLoginAndPartyDetails?has_content>
	                     			<#assign partyInfo = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(userLoginAndPartyDetails?if_exists) />
	                                <#if partyInfo?has_content>
	                                	<#assign userName = partyInfo.get("firstName")+" "+partyInfo.get("lastName")/>
	                                </#if>
                                </#if>
                                <fo:table-row>
                                		<fo:table-cell padding="2pt">
                                        <fo:block>${i}</fo:block>
                                    </fo:table-cell>
                                       <fo:table-cell padding="2pt">
                                        <fo:block>${product.internalName} [${shipmentItem.productId}]</fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="2pt">
                                        <fo:block text-align="center">${acceptedQTY}</fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="2pt">
                                        <fo:block text-align="center">${rejectedQTY}</fo:block>
                                    </fo:table-cell>
                                     <fo:table-cell padding="2pt">
                                        <fo:block text-align="center">${userName}</fo:block>
                                    </fo:table-cell>
                                     <fo:table-cell padding="2pt">
                                        <fo:block text-align="center">${Static["org.ofbiz.base.util.UtilDateTime"].toDateString(receviedDateTime)}</fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                                <#assign i = i+1/>
                            </#list>
                        </fo:table-body>
                    </fo:table>
    <#else>
        <fo:block font-size="14pt">
            ${uiLabelMap.ProductFacilityViewPermissionError}
        </fo:block>
    </#if>
</#escape>
