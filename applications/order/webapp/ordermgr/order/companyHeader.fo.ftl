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

<fo:block  text-align="center" margin-left="340px" >
    <fo:external-graphic src="<@ofbizContentUrl>${'./applications/order/images/ofbiz_logo.jpg'}</@ofbizContentUrl>" width="250px" content-height="scale-to-fit"/>
 
</fo:block>
<fo:block  text-align="center" width="1000pt">
  
</fo:block>
<#--
<fo:block text-align="left">
    <#if logoImageUrl?has_content><fo:external-graphic src="<@ofbizContentUrl>${logoImageUrl}</@ofbizContentUrl>" overflow="hidden" height="400px" content-height="scale-to-fit"/></#if>
</fo:block>
-->


<fo:block font-size="11px" text-align="center" margin-right="-338px" font-family="Times New Roman" border-top-style="solid" border-bottom-style="solid" float="right" border-bottom-width="0.8pt" padding-top="5px" padding-bottom="5px" border-top-width="0.8pt" >
    <fo:block>${companyName}</fo:block>
    <#if postalAddress?exists>
        <#if postalAddress?has_content>
            <fo:block>${postalAddress.address1?if_exists}
            <#if postalAddress.address2?has_content> ,${postalAddress.address2?if_exists}</#if>
            , ${postalAddress.city?if_exists}, ${stateProvinceAbbr?if_exists} ${postalAddress.postalCode?if_exists}, ${countryName?if_exists}</fo:block>
        </#if>
    <#else>
        <fo:block>${uiLabelMap.CommonNoPostalAddress}</fo:block>
        <fo:block>${uiLabelMap.CommonFor}: ${companyName}</fo:block>
    </#if>

    <#if sendingPartyTaxId?exists || phone?exists || email?exists || website?exists || eftAccount?exists>
    <fo:list-block provisional-distance-between-starts=".5in">

        <#--<#if sendingPartyTaxId?exists>
        <fo:list-item>
            <fo:list-item-label>
                <fo:block>${uiLabelMap.PartyTaxId}:</fo:block>
            </fo:list-item-label>
            <fo:list-item-body start-indent="body-start()">
                <fo:block>${sendingPartyTaxId}</fo:block>
            </fo:list-item-body>
        </fo:list-item>
        </#if>-->

        <#if phone?exists>
        <fo:list-item>
            <fo:list-item-label>
                <fo:block>${uiLabelMap.CommonTelephoneAbbr}:</fo:block>
            </fo:list-item-label>
            <fo:list-item-body start-indent="body-start()">
                <fo:block><#if phone.countryCode?exists>${phone.countryCode}-</#if><#if phone.areaCode?exists>${phone.areaCode}-</#if>${phone.contactNumber?if_exists}</fo:block>
            </fo:list-item-body>
        </fo:list-item>
        </#if>
        <#if email?exists>
        <fo:list-item>
            <fo:list-item-label>
                <fo:block></fo:block>
            </fo:list-item-label>
            <fo:list-item-body start-indent="body-start()">
                <fo:block></fo:block>
            </fo:list-item-body>
        </fo:list-item>
        </#if>
        <#if website?exists>
        <fo:list-item>
            <fo:list-item-label>
                <fo:block>${uiLabelMap.CommonWebsite}:</fo:block>
            </fo:list-item-label>
            <fo:list-item-body start-indent="body-start()">
                <fo:block>${website.infoString?if_exists}</fo:block>
            </fo:list-item-body>
        </fo:list-item>
        </#if>
        <#if eftAccount?exists>
        <fo:list-item>
            <fo:list-item-label>
                <fo:block>${uiLabelMap.CommonFinBankName}:</fo:block>
            </fo:list-item-label>
            <fo:list-item-body start-indent="body-start()">
                <fo:block>${eftAccount.bankName?if_exists}</fo:block>
            </fo:list-item-body>
        </fo:list-item>
        <fo:list-item>
            <fo:list-item-label>
                <fo:block>${uiLabelMap.CommonRouting}:</fo:block>
            </fo:list-item-label>
            <fo:list-item-body start-indent="body-start()">
                <fo:block>${eftAccount.routingNumber?if_exists}</fo:block>
            </fo:list-item-body>
        </fo:list-item>
        <fo:list-item>
            <fo:list-item-label>
                <fo:block>${uiLabelMap.CommonBankAccntNrAbbr}:</fo:block>
            </fo:list-item-label>
            <fo:list-item-body start-indent="body-start()">
                <fo:block>${eftAccount.accountNumber?if_exists}</fo:block>
            </fo:list-item-body>
        </fo:list-item>
        </#if>
    </fo:list-block>
    </#if>
</fo:block> 
 
</#escape>
