
<table border="1" width="100%">
		<tr>
	         <th colspan="5"><b>Cashier Summary Report</b></th>
       </tr>
	   <tr>
	         <th width="25%"><b>Date</b></th>
			 <th width="30%"><b>Cashier Name</b></th>
	         <th width="15%"><b>Cash Sales</b></th>
	         <th width="15%"><b>Credit Sales</b></th>
	         <th width="15%"><b>Total</b></th>
       </tr>
		<#list reportDataList as item>
	   <tr>
             <td>${item.createdDate?if_exists}</td>
             <td>${item.cashierId?if_exists}</td>
			 <td>${item.cashTotal?if_exists}</td>
             <td>${item.creditTotal?if_exists}</td>
             <td>${item.netTotal?if_exists}</td>
       </tr>
        </#list>

</table>