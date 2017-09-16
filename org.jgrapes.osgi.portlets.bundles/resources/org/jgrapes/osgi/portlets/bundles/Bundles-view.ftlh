<div data-portlet-title="${_("portletName")}" class="jgrapes-osgi-bundles-view">
  <table style="width: 100%;">
    <thead>
      <tr>
        <th>${_("bundleId")}</th>
        <th>${_("bundleName")}</th>
        <td>${_("bundleVersion")}</td>
        <td>${_("bundleCategory")}</td>
        <td>${_("bundleState")}</td>
      </tr>
    </thead>
    <tbody>
      <#list portlet.bundles() as bundle>
      <tr>
        <td>${bundle.bundleId}</td>
        <td>${bundle.getHeaders(locale).get("Bundle-Name")!(bundle.symbolicName)}
          <em>(${bundle.symbolicName})</em></td>
        <td>${bundle.version}</td>
        <td>${bundle.getHeaders(locale).get("Bundle-Category")!""}</td>
        <td>${_("bundleState_" + bundle.state)}</td>
      </tr>
      </#list>
    </tbody>
  </table>
</div>

<script>
	$( '[data-portlet-id="${portlet.portletId}"] .jgrapes-osgi-bundles-view table' ).DataTable( {
		"lengthMenu": [ [10, 25, 50, -1], [10, 25, 50, $.fn.dataTable.defaults.oLanguage.sLengthAll] ],
		"pageLength": -1
	} )
</script>
