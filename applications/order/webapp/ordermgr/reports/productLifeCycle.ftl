<table>
<tr><th>Barcode</th>
<th>Internal Name</th>
<th>Model No</th>
<th>Brand Name</th>
<th>Supplier</th>
<th>LPO No</th>
<th>Opening Balance</th>
<#list reportDataList as item>
<#assign productInfo = delegator.findOne("Product", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",item.productId ), false)>


<tr><td>${item.productId?if_exists},</td>
<td>${productInfo.internalName?if_exists}</td>
<td>${productInfo.modelNo?if_exists}</td>
<td>${productInfo.brandName?if_exists}</td>
<#if item.partyId != "_NA_">
<td>${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, item.partyId, false)?if_exists}</td>
<#else>
<td>_NA_</td>

</#if>
<td>${item.orderId?if_exists}</td>
<td>${item.itemQtyTotal?if_exists}</td>
</tr>
</#list>
</table>