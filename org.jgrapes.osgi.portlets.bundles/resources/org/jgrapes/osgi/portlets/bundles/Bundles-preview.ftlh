<div data-portlet-title="${_("portletName")}" class="jgrapes-osgi-bundles-preview"> 
  <table>
    <thead>
      <tr>
        <th>${_("bundleId")}</th>
        <th>${_("bundleName")}</th>
      </tr>
    </thead>
    <tbody>
      <#list portlet.bundles() as bundle>
      <tr data-tooltip-id="${"BundlesPreviewTooltip-" + bundle.bundleId}">
        <td>${bundle.bundleId}</td>
        <td>${bundle.getHeaders(locale).get("Bundle-Name")!(bundle.symbolicName)}</td>
      </tr>
      </#list>
    </tbody>
  </table>
</div>

<#list portlet.bundles() as bundle>
<div id="${"BundlesPreviewTooltip-" + bundle.bundleId}" 
    class="jgrapes-tooltip-prototype">
  <table>
    <tr>
      <td>${_("bundleSymbolicName")}:</td>
      <td>${bundle.symbolicName}</td>
    </tr>
    <tr>
      <td>${_("bundleVersion")}:</td>
      <td>${bundle.version}</td>
    </tr>
    <tr>
      <td>${_("bundleCategory")}:</td>
      <td>${bundle.getHeaders(locale).get("Bundle-Category")!""}</td>
    </tr>
    <tr>
      <td>${_("bundleState")}:</td>
      <td>${_("bundleState_" + bundle.state)}</td>
    </tr>
  </table>
</div>
</#list>
  
<script>
    JGPortal.tooltip($( ".jgrapes-osgi-bundles-preview" ));
    $( ".jgrapes-osgi-bundles-preview" ).tooltip( "option", "classes.ui-tooltip", "ui-corner-all ui-widget-shadow jgrapes-osgi-bundles-preview-tooltip" );

	$( '[data-portlet-id="${portlet.portletId}"] .jgrapes-osgi-bundles-preview table' ).DataTable( {
		"searching": false,
		"paging": false,
		"info": false
	} )
</script>
