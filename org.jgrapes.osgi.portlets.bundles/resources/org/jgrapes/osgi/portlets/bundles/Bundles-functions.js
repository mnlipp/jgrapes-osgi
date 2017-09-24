'use strict';

var orgJGrapesOsgiPortletsBundles = {
    l10n: {
    }
};

(function() {

    var l10n = orgJGrapesOsgiPortletsBundles.l10n;
    
    function createTooltip(bundle) {
        let tooltip = '<div class="jgrapes-tooltip-prototype">';
        tooltip += '<table><tbody>';
        tooltip += '<td>' + l10n.bundleSymbolicName + ':</td>';
        tooltip += '<td>' + bundle.symbolicName + '</td>';
        tooltip += '</tr><tr>';
        tooltip += '<td>' + l10n.bundleVersion + ':</td>';
        tooltip += '<td>' + bundle.version + '</td>';
        tooltip += '</tr><tr>';
        tooltip += '<td>' + l10n.bundleCategory + ':</td>';
        tooltip += '<td>' + bundle.category + '</td>';
        tooltip += '</tr><tr>';
        tooltip += '<td>' + l10n.bundleState + ':</td>';
        tooltip += '<td>' + bundle.state + '</td>';
        tooltip += '</tr></tbody></table></div>';
        return tooltip;
    }

    function registerTooltip(row, bundleInfo) {
        row.tooltip({
            items: "[data-bundle-id]",
            content: function() {
                let tooltip = $( this ).find(".jgrapes-tooltip-prototype");
                tooltip = tooltip.clone(true);
                tooltip.removeClass("jgrapes-tooltip-prototype")
                return tooltip;
            },
            classes: {
                "ui-tooltip": "ui-corner-all ui-widget-shadow jgrapes-osgi-bundles-preview-tooltip"
            }
        });
    }
    
    orgJGrapesOsgiPortletsBundles.initPreviewTable = function(tableSelector) {
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
                          return data + createTooltip(row);
                      }
                      return data;
                  }
                }
            ],
            "searching": false,
            "paging": false,
            "info": false,
            "dom": "t",
            "rowCallback": function( row, data, dataIndex ) {
                $(row).attr("data-bundle-id", data.id);
                registerTooltip($(row), data);
            },
            "processing": true 
        } );
        table.processing(true);
        return table;
    }
    
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
            "pageLength": -1,
            "processing": true 
        } );
        table.processing(true);
        return table;
    }
    
    $("body").on("click", ".jgrapes-osgi-bundles-view table button",
        function(event) {
            let portletId = $(this).closest("[data-portlet-id]").attr("data-portlet-id");
            let bundleId = $(this).closest("[data-bundle-id]").attr("data-bundle-id");
            let action = $(this).attr("data-bundle-action")
            JGPortal.sendToPortlet(portletId, action, [ parseInt(bundleId) ]);
    });

    function updateTable(table, bundleInfos, replace) {
        if (replace) {
            table.clear();
        }
        bundleInfos.forEach(function(bundleInfo) {
            if (!replace) {
                let row = table.row(function ( idx, data, node ) {
                    return data.id === bundleInfo.id;
                });
                if (row.length > 0) {
                    if (bundleInfo.uninstalled) {
                        table.row(row).remove();
                    } else {
                        table.row(row).data(bundleInfo);
                    }
                    return;
                }
            }
            if (!bundleInfo.uninstalled) {
                table.row.add(bundleInfo);
            }
        });
        table.processing(false);
        table.draw();
    }
    
    JGPortal.registerPortletMethod(
            "org.jgrapes.osgi.portlets.bundles.BundleListPortlet",
            "bundleUpdates", function(portletId, params) {
                let bundleInfos = params[0];
                // Preview
                if (params[1] === "preview" || params[1] === "*") {
                    let portlet = JGPortal.findPortletPreview(portletId);
                    if (portlet) {
                        let table = portlet.find(".jgrapes-osgi-bundles-preview table.bundles-table").DataTable();
                        updateTable(table, bundleInfos, params[2]);
                    }
                }
                
                // View
                if (params[1] === "view" || params[1] === "*") {
                    let portlet = JGPortal.findPortletView(portletId);
                    if (portlet) {
                        let table = portlet.find("table").DataTable();
                        updateTable(table, bundleInfos, params[2]);
                    }
                }
            });

})();

