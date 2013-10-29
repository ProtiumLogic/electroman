<table border="1" width="100%">
	<tr>
		<th colspan="5" size="18px">Sales summary report</th>
	</tr>
	<tr>
		<th><b>Date</b></th>
        <th><b>Total Sales</b></th>
        <th><b>Sales Returns</b></th>
        <th><b>Net Sales</b></th>
        <th><b>Cash</b></th>
        <th><b>Credit</b></th>
        <th><b>Gift Card</b></th>
        
    </tr>
        <#list reportDataList as item>
        <#assign netSales = item.netTotal.subtract(item.returnTotal)/>
        
    
    <tr>
    <td>${item.createdDate}</td>
    <td>${item.netTotal}</td>
    <td>${item.returnTotal?if_exists}</td>
    <td>${netSales}</td>
    <td>${item.cashTotal?if_exists}</td>
    <td>${item.creditTotal?if_exists}</td>
    <td>${item.giftTotal?if_exists}</td>
    
    </tr>
        </#list>
    
</table>