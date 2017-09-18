'use strict';

var orgJGrapesOsgiPortletsBundles = {
    l10n: {
    }
};

(function() {

    var l10n = orgJGrapesOsgiPortletsBundles.l10n;
    
    orgJGrapesOsgiPortletsBundles.initTable = function(tableSelector) {
        JGPortal.lockMessageQueue();
        let table = tableSelector.DataTable( {
            "initComplete": function( settings, json ) {
                JGPortal.unlockMessageQueue();
            },
            "columns": [ 
                { "data": "id" },
                { "data": "name",
                  "render": function ( data, type, row, meta ) {
                      if (type === "display") {
                          return row.name + " <em>(" + row.symbolicName + ")</em>";
                      }
                      return data;
                  }
                },
                { "data": "version" },
                { "data": "category" },
                { "data": "state" },
                { "data": null,
                  "orderable": false,
                  "render": function ( data, type, row, meta ) {
                    return '<button data-bundle-action="stop" \
                        class="ui-button ui-widget ui-corner-all ui-button-icon-only" \
                        title="' + l10n.bundleStop + '"><span class="ui-icon ui-icon-stop"></span>' + l10n.bundleStop + '</button><button data-bundle-action="start" \
                        class="ui-button ui-widget ui-corner-all ui-button-icon-only" \
                        title="' + l10n.bundleStart + '"><span class="ui-icon ui-icon-play"></span>' + l10n.bundleStart + '</button> \
                        <button data-bundle-action="refresh" \
                            class="ui-button ui-widget ui-corner-all ui-button-icon-only" \
                            title="' + l10n.bundleRefresh + '"><span class="ui-icon ui-icon-refresh"></span>' + l10n.bundleRefresh + '</button> \
                        <button data-bundle-action="update" \
                            class="ui-button ui-widget ui-corner-all ui-button-icon-only" \
                            title="' + l10n.bundleUpdate + '"><span class="ui-icon ui-icon-transfer-e-w"></span>' + l10n.bundleUpdate + '</button> \
                        <button data-bundle-action="uninstall" \
                            class="ui-button ui-widget ui-corner-all ui-button-icon-only" \
                            title="' + l10n.bundleUninstall + '"><span class="ui-icon ui-icon-eject"></span>' + l10n.bundleUninstall + '</button>';
                    }
                } ],
            "createdRow": function( row, data, dataIndex ) {
                $(row).attr("data-bundle-id", data.id);
            },
            "lengthMenu": [ [10, 25, 50, -1], [10, 25, 50, $.fn.dataTable.defaults.oLanguage.sLengthAll] ],
            "pageLength": -1
        } );
        return table;
    }
    
    $("body").on("click", ".jgrapes-osgi-bundles-view table button",
        function(event) {
            let portletId = $(this).closest("[data-portlet-id]").attr("data-portlet-id");
            let bundleId = $(this).closest("[data-bundle-id]").attr("data-bundle-id");
            let action = $(this).attr("data-bundle-action")
            JGPortal.sendToPortlet(portletId, action, [ parseInt(bundleId) ]);
    });
    
    JGPortal.registerPortletMethod(
            "org.jgrapes.osgi.portlets.bundles.BundleListPortlet",
            "updateBundles", function(portletId, params) {
                let portlet = JGPortal.findPortletView(portletId);
                let table = portlet.find("table").DataTable();
                params[0].forEach(function(value) {
                    table.row.add(value);
                });
                table.draw();
            });

})();

