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
                  <fo:table>
                    <fo:table-column column-width="2.3in"/>
                    <fo:table-column column-width="1.2in"/>
                    <fo:table-column column-width="0.1in"/>
                    <fo:table-column column-width="1.8in"/>
                    <fo:table-body>
                    <fo:table-row>
                      <fo:table-cell></fo:table-cell>
                      <fo:table-cell></fo:table-cell>
                      <fo:table-cell></fo:table-cell>
                    </fo:table-row>

                    <fo:table-row >
                      <fo:table-cell></fo:table-cell>
                      <fo:table-cell ><fo:block font-size="10px" font-family="Times New Roman">${"Ordered Date"}</fo:block></fo:table-cell>
                      <fo:table-cell><fo:block font-size="10px" font-family="Times New Roman">${":"}</fo:block></fo:table-cell>
                      <#assign dateFormat = Static["java.text.DateFormat"].LONG>
                    <#assign orderDate = Static["java.text.DateFormat"].getDateInstance(dateFormat,locale).format(orderHeader.get("orderDate"))>
                      <fo:table-cell><fo:block font-size="10px" font-family="Times New Roman">${orderDate}</fo:block></fo:table-cell>
                    </fo:table-row>

                    <fo:table-row >
                      <fo:table-cell></fo:table-cell>
                      <fo:table-cell><fo:block font-size="10px" font-family="Times New Roman">${"Order ID"}</fo:block></fo:table-cell>
                      <fo:table-cell><fo:block font-size="10px" font-family="Times New Roman">${":"}</fo:block></fo:table-cell>
                      <fo:table-cell><fo:block font-size="10px" font-family="Times New Roman">${orderId}</fo:block></fo:table-cell>
                    </fo:table-row>

                    
                    <#if orderItem.cancelBackOrderDate?exists>
                      <fo:table-row>
                        <fo:table-cell><fo:block>${uiLabelMap.FormFieldTitle_cancelBackOrderDate}</fo:block></fo:table-cell>
                        <#assign dateFormat = Static["java.text.DateFormat"].LONG>
                        <#assign cancelBackOrderDate = Static["java.text.DateFormat"].getDateInstance(dateFormat,locale).format(orderItem.get("cancelBackOrderDate"))>
                        <fo:table-cell><#if cancelBackOrderDate?has_content><fo:block>${cancelBackOrderDate}</fo:block></#if></fo:table-cell>
                      </fo:table-row>
                    </#if>
                    </fo:table-body>
                  </fo:table>
</#escape>
