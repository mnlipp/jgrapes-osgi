<div data-portlet-title="${_("portletName")}" class="jgrapes-osgi-bundles-view">
  <table style="width: 100%;">
    <thead>
      <tr>
        <th>${_("bundleId")}</th>
        <th>${_("bundleName")}</th>
        <th>${_("bundleVersion")}</th>
        <th>${_("bundleCategory")}</th>
        <th>${_("bundleState")}</th>
        <th>${_("bundleActions")}</th>
      </tr>
    </thead>
    <tbody>
    </tbody>
  </table>
</div>

<script>
	$.extend(orgJGrapesOsgiPortletsBundles.l10n, {
		"bundleStop": "${_("bundleStop")}",
		"bundleStart": "${_("bundleStart")}",
		"bundleRefresh": "${_("bundleRefresh")}",
		"bundleUpdate": "${_("bundleUpdate")}",
		"bundleUninstall": "${_("bundleUninstall")}",
	});

	orgJGrapesOsgiPortletsBundles.initTable($( '[data-portlet-id="${portlet.portletId}"] .jgrapes-osgi-bundles-view table' ));
</script>
