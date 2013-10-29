<table border="1" width="100%">
		<tr>
	         <th colspan="5"><b>Cashier In Detail Report</b></th>
       </tr>
	   <tr>
	         <th width="25%"><b>Date</b></th>
			 <th width="30%"><b>Cashier Name</b></th>
	         <th width="15%"><b>Sales Type</b></th>
	         <th width="15%"><b>Invoice No</b></th>
	         <th width="15%"><b>Amount</b></th>
       </tr>
		<#list reportDataList1 as item>
        <tr>
             <td>${item.createdDate?if_exists}</td>
             <td>${item.cashierId?if_exists}</td>
			 <td>${item.paymentMethodTypeId?if_exists}</td>
             <td>${item.invoiceId?if_exists}</td>
             <td>${item.invoiceTotal?if_exists}</td>
       </tr>
        </#list>
</table>