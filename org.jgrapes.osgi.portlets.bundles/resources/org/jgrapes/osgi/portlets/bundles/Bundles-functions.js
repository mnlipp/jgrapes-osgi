'use strict';

var orgJGrapesOsgiPortletsBundles = {
    l10n: {
    }
};

(function() {

    var l10n = orgJGrapesOsgiPortletsBundles.l10n;
    
    orgJGrapesOsgiPortletsBundles.initViewTable = function(tableSelector) {
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
                      var buttons = ''
                      if (row.startable) {
                          buttons += '<button data-bundle-action="start" \
                              >' + l10n.bundleStart + '</button>';
                      } else {
                          buttons += '<button data-bundle-action="stop" \
                              >' + l10n.bundleStop + '</button>';
                      }
                      buttons += ' <button data-bundle-action="refresh" \
                          >' + l10n.bundleRefresh + '</button> \
                        <button data-bundle-action="update" \
                            >' + l10n.bundleUpdate + '</button> \
                        <button data-bundle-action="uninstall" \
                            >' + l10n.bundleUninstall + '</button>';
                      return buttons;
                  }
                } ],
            "createdRow": function( row, data, dataIndex ) {
                $(row).attr("data-bundle-id", data.id);
            },
            "rowCallback": function( row, data, index ) {
                $( row ).find('button[data-bundle-action="start"]').button( {
                    icon: "ui-icon-play",
                    showLabel: false
                } );
                $( row ).find('button[data-bundle-action="stop"]').button( {
                    icon: "ui-icon-stop",
                    showLabel: false
                } );
                $( row ).find('button[data-bundle-action="refresh"]').button( {
                    icon: "ui-icon-refresh",
                    showLabel: false
                } );
                $( row ).find('button[data-bundle-action="update"]').button( {
                    icon: "ui-icon-transfer-e-w",
                    showLabel: false
                } );
                $( row ).find('button[data-bundle-action="uninstall"]').button( {
                    icon: "ui-icon-eject",
                    showLabel: false
                } );
                if (!data.stoppable) {
                    $( row ).find('button[data-bundle-action="stop"]').button("disable");
                }
                if (!data.uninstallable) {
                    $( row ).find('button [data-bundle-action="uninstall"]').button("disable");
                }
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
            "bundleList", function(portletId, params) {
                let portlet = JGPortal.findPortletView(portletId);
                let table = portlet.find("table");
                table.find("tbody").children().remove();
                table = table.DataTable();
                params[0].forEach(function(value) {
                    table.row.add(value);
                });
                table.draw();
            });
    JGPortal.registerPortletMethod(
            "org.jgrapes.osgi.portlets.bundles.BundleListPortlet",
            "bundleUpdate", function(portletId, params) {
                let portlet = JGPortal.findPortletView(portletId);
                let table = portlet.find("table");
                let bundleInfo = params[0];
                let row = table.find('tr[data-bundle-id="' + bundleInfo.id + '"]');
                table = table.DataTable();
                if (row.length > 0) {
                    table.row(row).remove();
                }
                if (!bundleInfo.uninstalled) {
                    table.row.add(bundleInfo);
                }
                table.draw();
            });

})();

