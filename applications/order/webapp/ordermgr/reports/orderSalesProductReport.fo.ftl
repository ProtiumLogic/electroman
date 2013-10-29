<#escape x as x?xml>
<fo:block space-before="5mm" space-after="5mm"></fo:block>
<fo:block align ="center" space-before="5mm" space-after="5mm">
             <fo:block space-after.optimum="40pt" font-size="10pt">
              <fo:table width ="100%">
                <fo:table-column column-width="15%"/>
                <fo:table-column column-width="8%"/>
                <fo:table-column column-width="8%"/>
                <fo:table-column column-width="10%"/>
	           <fo:table-column column-width="18%"/>
	           <fo:table-column column-width="14%"/>
	           <fo:table-column column-width="12%"/>
			   <fo:table-column column-width="8%"/>
	           <fo:table-column column-width="8%"/>
                <fo:table-header>
                 <fo:table-row font-weight="bold">
                    <fo:table-cell border-bottom="thin solid grey"><fo:block>${"INVOICE_DATE"}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${"Cat"}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${"Ctgry"}</fo:block></fo:table-cell>
						<fo:table-cell border-bottom="thin solid grey"><fo:block>${"Sub-Ctgry"}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${"Product Id"}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${"Brand Name"}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${"Int Name"}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey" text-align="center"><fo:block>${"QTY"}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${"AMT"}</fo:block></fo:table-cell>
                   </fo:table-row>
                </fo:table-header>
                <fo:table-body>
                 <#list invoiceItems as item>
                 <#assign productInfo = delegator.findOne("Product", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",item.productId ), false)>
					<#assign productCategoryList = delegator.findByAnd("ProductCategoryMember",Static["org.ofbiz.base.util.UtilMisc"].toMap("productId",item.productId))>
					<#if productCategoryList?has_content>
					<#assign productCategory = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(productCategoryList) />
					<#assign categoryInfo = delegator.findOne("ProductCategory", Static["org.ofbiz.base.util.UtilMisc"].toMap("productCategoryId",productCategory.productCategoryId ), false)>
					<#if categoryInfo.primaryParentCategoryId?has_content>
					<#assign primaryCategoryInfo = delegator.findOne("ProductCategory", Static["org.ofbiz.base.util.UtilMisc"].toMap("productCategoryId",categoryInfo.primaryParentCategoryId), false)> 	
					</#if>
					<#assign productCatalogList = delegator.findByAnd("ProdCatalogCategory",Static["org.ofbiz.base.util.UtilMisc"].toMap("productCategoryId",productCategory.productCategoryId))>
					</#if>
					<#if productCatalogList?has_content>
					<#assign productCatalog = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(productCatalogList) />
					<#assign catalogInfo = delegator.findOne("ProdCatalog", Static["org.ofbiz.base.util.UtilMisc"].toMap("prodCatalogId",productCatalog.prodCatalogId ), false)> 	
					</#if>
                 	<fo:table-row>
						<fo:table-cell padding="2pt" >
							<fo:block>${item.invoiceDate?if_exists}</fo:block>
						</fo:table-cell>
						<fo:table-cell padding="2pt" >
							<fo:block>${catalogInfo.catalogName?if_exists}</fo:block>
						</fo:table-cell>
						<fo:table-cell padding="2pt" >
							<fo:block>${categoryInfo.categoryName?if_exists}</fo:block>
						</fo:table-cell>
						<fo:table-cell padding="2pt" >
							<fo:block><#if primaryCategoryInfo?has_content>${primaryCategoryInfo.categoryName?if_exists}</#if></fo:block>
						</fo:table-cell>
						<fo:table-cell padding="2pt" >
							<fo:block>${item.productId?if_exists}</fo:block>
						</fo:table-cell>
						<fo:table-cell padding="2pt" >
							<fo:block>${productInfo.brandName?if_exists}</fo:block>
						</fo:table-cell>
						<fo:table-cell padding="2pt" >
							<fo:block>${productInfo.internalName?if_exists}</fo:block>
						</fo:table-cell>
						<fo:table-cell padding="2pt" text-align="center">
							<fo:block>${item.quantity?if_exists}</fo:block>
						</fo:table-cell>
						<fo:table-cell padding="2pt" >
							<fo:block>${item.invoiceTotal?if_exists}</fo:block>
							o:block>
						</fo:table-cell>
					</fo:table-row>
                </#list>
                </fo:table-body>
            	</fo:table>

</fo:block>
</fo:block>

</#escape>
