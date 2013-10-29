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

<fo:table table-layout="fixed" width="300%">
<fo:table-column column-width="13mm"/>
<fo:table-column column-width="68mm"/>
<fo:table-column column-width="40mm"/>
<fo:table-column column-width="30mm"/>
<fo:table-column column-width="80mm"/>
<fo:table-column column-width="70mm"/>
<fo:table-column column-width="90mm"/>
<fo:table-body>

 <fo:table-row height="48px">
           <fo:table-cell number-columns-spanned="7">
              <fo:block/>
           </fo:table-cell>
 </fo:table-row>

<fo:table-row>
 <fo:table-cell><fo:block/></fo:table-cell>
  <fo:table-cell display-align="left" padding-left="-35px"><fo:block font-size="10px" font-family="Times New Roman"><#if invoice?has_content>${invoice.invoiceId}</#if>
</fo:block></fo:table-cell>
 <fo:table-cell display-align="right" padding-left="40px"><fo:block font-size="10px" font-family="Times New Roman">${invoiceDate?if_exists}</fo:block>
</fo:table-cell>

 <fo:table-cell colspan="3"><fo:block/></fo:table-cell>

  <fo:table-cell display-align="right" padding-left="60px"><fo:block font-size="10px" font-family="Times New Roman"><#if invoice?has_content>${invoice.invoiceId}</#if>
</fo:block></fo:table-cell>
 <fo:table-cell display-align="right" padding-left="40px"><fo:block font-size="10px" font-family="Times New Roman" text-align="right" >${invoiceDate?if_exists}</fo:block>
</fo:table-cell>
</fo:table-row>

<fo:table-row height="8px">
           <fo:table-cell number-columns-spanned="7">
              <fo:block/>
           </fo:table-cell>
 </fo:table-row>

<fo:table-row>
 <fo:table-cell><fo:block/></fo:table-cell>
  <fo:table-cell display-align="left" padding-left="-25px"><fo:block font-size="10px" font-family="Times New Roman"><#if billingParty?has_content>${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, billingParty.partyId, false)?if_exists}(${billingParty.partyId})</#if>
</fo:block></fo:table-cell>
 <fo:table-cell colspan="3"><fo:block/></fo:table-cell>
 
 <fo:table-cell><fo:block/></fo:table-cell>
  <fo:table-cell display-align="right" padding-left="140px"><fo:block font-size="10px" font-family="Times New Roman"><#if billingParty?has_content>${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, billingParty.partyId, false)?if_exists}(${billingParty.partyId})</#if>
</fo:block></fo:table-cell>
 </fo:table-row>
 
 <fo:table-row height="3px">
           <fo:table-cell number-columns-spanned="7">
              <fo:block/>
           </fo:table-cell>
 </fo:table-row>


 <fo:table-row>
 <fo:table-cell><fo:block/></fo:table-cell>
 <fo:table-cell padding-left="-33px"><fo:block font-size="10px"  font-family="Times New Roman">${phonenumber? if_exists}</fo:block></fo:table-cell>
 <fo:table-cell colspan="3"><fo:block/></fo:table-cell>
 
 <fo:table-cell><fo:block/></fo:table-cell>
 <fo:table-cell padding-left="40px"><fo:block font-size="10px" font-family="Times New Roman">${phonenumber? if_exists}</fo:block></fo:table-cell>
 </fo:table-row>

 <fo:table-row height="10px">
           <fo:table-cell number-columns-spanned="7">
              <fo:block/>
           </fo:table-cell>
 </fo:table-row>

<#--
<#if billingParty.partyId!="_NA_">
<fo:table-row>
  <fo:table-cell colspan="3"><fo:block font-size="10px" font-family="Times New Roman"><#if billingParty?has_content>${billingParty.partyId}</#if></fo:block></fo:table-cell>

 <fo:table-cell colspan="4"><fo:block/></fo:table-cell>
</fo:table-row>
</#if>


<#if billingPartyTaxId?has_content>
  <fo:table-row>
    <fo:table-cell><fo:block font-size="10px" font-family="Times New Roman">${uiLabelMap.PartyTaxId}</fo:block></fo:table-cell>
  <fo:table-cell><fo:block  text-align="left" font-size="10px" font-family="Times New Roman">:</fo:block></fo:table-cell>
    <fo:table-cell><fo:block font-size="10px" font-family="Times New Roman"> ${billingPartyTaxId}</fo:block></fo:table-cell>
  </fo:table-row>
</#if>

-->

<#if invoice?has_content && invoice.description?has_content>
  <fo:table-row>
    <fo:table-cell colspan="3"><fo:block font-size="10px" font-family="Times New Roman">${uiLabelMap.AccountingDescr} ${" : "} ${invoice.description}</fo:block></fo:table-cell>
  </fo:table-row>
</#if>

<!--fo:table-row>
  <fo:table-cell><fo:block font-size="10px" font-family="Times New Roman">${uiLabelMap.CommonStatus}</fo:block></fo:table-cell>
  <fo:table-cell><fo:block font-weight="bold" font-size="10px" font-family="Times New Roman">${invoiceStatus.get("description",locale)}</fo:block></fo:table-cell>
</fo:table-row-->

</fo:table-body>
</fo:table>

 
</#escape>

