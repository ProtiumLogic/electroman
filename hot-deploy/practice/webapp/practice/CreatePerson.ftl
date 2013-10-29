<h2>${uiLabelMap.PartyCreateNewPerson}</h2>
<div id="createPersonError" style="display:none"></div>
<form method="post" id="createPersonForm" action="<@ofbizUrl>createPracticePersonByAjax</@ofbizUrl>">
  <div>
    <fieldset>
      <div>
        <label>${uiLabelMap.FormFieldTitle_salutation}</label>
        <input type="text" name="salutation" value=""/>
      </div>
      <div>
        <label>${uiLabelMap.PartyFirstName}*</label>
        <input type="text" name="firstName" class="required" value=""/>
      </div>
      <div>
        <label>${uiLabelMap.PartyMiddleName}</label>
        <input type="text" name="middleName" value=""/>
      </div>
      <div>
        <label>${uiLabelMap.PartyLastName}*</label>
        <input type="text" name="lastName" class="required" value=""/>
      </div>
      <div>
        <label>${uiLabelMap.PartySuffix}</label>
        <input type="text" name="suffix" value=""/>
      </div>
      <div>
        <a id="createPerson" href="javascript:void(0);" class="buttontext">${uiLabelMap.CommonCreate}</a>
      </div>
    </fieldset>
  </div>
</form>
  