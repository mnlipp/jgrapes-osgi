'use strict';

var orgJGrapesOsgiPortletsBundles = {
    l10n: {
    }
};

(function() {

    var l10n = orgJGrapesOsgiPortletsBundles.l10n;
    
    function createTooltip(bundle) {
        let tooltip = '<div>';
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
        row.data("bundleInfo", bundleInfo);
        row.tooltip({
            items: "[data-bundle-id]",
            content: function() {
                return createTooltip($( this ).data("bundleInfo"));
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
                tableSelector.DataTable().processing(true);
                JGPortal.unlockMessageQueue();
            },
            "columns": [ 
                { "data": "id" },
                { "data": "name"}
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
        return table;
    }
    
    let buttonsPrototype = $(
            '<button class="ui-button ui-widget ui-corner-all ui-button-icon-only"'
            + 'title="' + l10n.bundleStart + '" data-bundle-action="start">'
            + '<span class="ui-icon ui-icon-play"></span>' + l10n.bundleStart + '</button>'
            + '<button class="ui-button ui-widget ui-corner-all ui-button-icon-only"'
            + 'title="' + l10n.bundleStop + '" data-bundle-action="stop">'
            + '<span class="ui-icon ui-icon-stop"></span>' + l10n.bundleStop + '</button>'
            + ' <button class="ui-button ui-widget ui-corner-all ui-button-icon-only"'
            + 'title="' + l10n.bundleRefresh + '" data-bundle-action="refresh">'
            + '<span class="ui-icon ui-icon-refresh"></span>' + l10n.bundleRefresh + '</button>'
            + ' <button class="ui-button ui-widget ui-corner-all ui-button-icon-only"'
            + 'title="' + l10n.bundleUpdate + '" data-bundle-action="update">'
            + '<span class="ui-icon ui-icon-transfer-e-w"></span>' + l10n.bundleUpdate + '</button>'
            + ' <button class="ui-button ui-widget ui-corner-all ui-button-icon-only"'
            + 'title="' + l10n.bundleUninstall + '" data-bundle-action="uninstall">'
            + '<span class="ui-icon ui-icon-eject"></span>' + l10n.bundleUninstall + '</button>' );
    
    orgJGrapesOsgiPortletsBundles.initViewTable = function(tableSelector) {
        JGPortal.lockMessageQueue();
        let table = tableSelector.DataTable( {
            "initComplete": function( settings, json ) {
                tableSelector.DataTable().processing(true);
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
                  "defaultContent": "",
                  "orderable": false,
                } ],
            "createdRow": function( row, data, dataIndex ) {
                $(row).attr("data-bundle-id", data.id);
                $( $(row).find("td")[5] ).append(buttonsPrototype.clone());
                $( $(row).find("td")[5] ).find("button").button();
            },
            "rowCallback": function( row, data, index ) {
                if ($( $(row).find("td")[5] ).find("button").length === 0) {
                    $( $(row).find("td")[5] ).append(buttonsPrototype.clone());
                    $( $(row).find("td")[5] ).find("button").button();
                }
                let startButton = $( row ).find('button[data-bundle-action="start"]');
                let stopButton = $( row ).find('button[data-bundle-action="stop"]');
                if (data.startable) {
                    startButton.show();
                    stopButton.hide();
                } else {
                    startButton.hide();
                    stopButton.show()
                }
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
        table.columns.adjust().draw();
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

