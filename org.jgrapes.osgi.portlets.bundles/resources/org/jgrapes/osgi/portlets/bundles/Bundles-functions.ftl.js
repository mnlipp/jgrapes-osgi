/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2017  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

var orgJGrapesOsgiPortletsBundles = {
    l10n: {
        "bundleCategory": "${_("bundleCategory")}",
        "bundleRefresh": "${_("bundleRefresh")}",
        "bundleStart": "${_("bundleStart")}",
        "bundleState": "${_("bundleState")}",
        "bundleStop": "${_("bundleStop")}",
        "bundleSymbolicName": "${_("bundleSymbolicName")}",
        "bundleUpdate": "${_("bundleUpdate")}",
        "bundleUninstall": "${_("bundleUninstall")}",
        "bundleVersion": "${_("bundleVersion")}",
        "detailsBeingLoaded": "${_("detailsBeingLoaded")}",
    }
};

(function() {

    var l10n = orgJGrapesOsgiPortletsBundles.l10n;
    
    function createTooltip(bundle) {
        let tooltip = '<div>';
        tooltip += '<table><tbody><tr>';
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
            },
            close: function() {
                // https://bugs.jqueryui.com/ticket/10689
                $(".ui-helper-hidden-accessible > *:not(:last)").remove();
            },
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
    
    // Delay initialization, l10n is not yet initialized when loading this file
    let _buttonsPrototype = null;
    
    function buttonsPrototype() {
        if (!_buttonsPrototype) {
            _buttonsPrototype = $(
            '<div>'
            + '<button class="ui-button ui-widget ui-corner-all ui-button-icon-only"'
            + 'title="' + l10n.bundleStart + '" data-bundle-action="start">'
            + '<span class="ui-icon ui-icon-play"></span>' + l10n.bundleStart + '</button>'
            + '<button class="ui-button ui-widget ui-corner-all ui-button-icon-only"'
            + 'title="' + l10n.bundleStop + '" data-bundle-action="stop">'
            + '<span class="ui-icon ui-icon-stop"></span>' + l10n.bundleStop + '</button>'
//            + ' <button class="ui-button ui-widget ui-corner-all ui-button-icon-only"'
//            + 'title="' + l10n.bundleRefresh + '" data-bundle-action="refresh">'
//            + '<span class="ui-icon ui-icon-refresh"></span>' + l10n.bundleRefresh + '</button>'
            + ' <button class="ui-button ui-widget ui-corner-all ui-button-icon-only"'
            + 'title="' + l10n.bundleUpdate + '" data-bundle-action="update">'
            + '<span class="ui-icon ui-icon-transfer-e-w"></span>' + l10n.bundleUpdate + '</button>'
            + ' <button class="ui-button ui-widget ui-corner-all ui-button-icon-only"'
            + 'title="' + l10n.bundleUninstall + '" data-bundle-action="uninstall">'
            + '<span class="ui-icon ui-icon-eject"></span>' + l10n.bundleUninstall + '</button>'
            + '</div>');
        }
        return _buttonsPrototype;
    }
    
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
                          return row.name + " <em>(" + row.symbolicName + ")</em>" +
                              ' <span class="ui-icon ui-icon-popup"></span>';
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
                  "render": function ( data, type, row, meta ) {
                      if (type === "display") {
                          let buttons = buttonsPrototype().clone();
                          buttons.find("button").button();
                          buttons.find("button").attr("data-bundle-id", row.id);
                          // Update button states
                          let startButton = buttons.find('button[data-bundle-action="start"]');
                          let stopButton = buttons.find('button[data-bundle-action="stop"]');
                          if (row.startable) {
                              startButton.show();
                              stopButton.hide();
                          } else {
                              startButton.hide();
                              stopButton.show()
                          }
                          if (!row.stoppable) {
                              buttons.find('button[data-bundle-action="stop"]').button("disable");
                          }
                          if (!row.uninstallable) {
                              buttons.find('button [data-bundle-action="uninstall"]').button("disable");
                          }
                          return buttons.html();
                      }
                      return data;
                  }
                } ],
            "createdRow": function( row, data, dataIndex ) {
                let jqRow = $(row);
                jqRow.attr("data-bundle-id", data.id);
            },
            "rowCallback": function( row, data, index ) {
                let jqRow = $(row);
                // Handler must be reinstalled after each update (value may have changed)
                jqRow.find('.ui-icon-popup').off("click");
                jqRow.find('.ui-icon-popup').on("click", function() {
                    let portletId = $(this).closest("[data-portlet-id]").attr("data-portlet-id");
                    let bundleId = $(this).closest("[data-bundle-id]").attr("data-bundle-id");
                    let bundleDetailsDialog = $('<div />');
                    bundleDetailsDialog.attr("data-bundle-details-for", bundleId);
                    bundleDetailsDialog.html(l10n.detailsBeingLoaded);
                    JGPortal.sendToPortlet(portletId, "sendDetails", [ parseInt(bundleId) ]);
                    bundleDetailsDialog.dialog({
                        title: data.symbolicName,
                        classes: {
                            "ui-dialog": "ui-corner-all jgrapes-osgi-bundles-details"
                        },
                        width: "90%",
                        position: {
                            my: "center top",
                            at: "center top+50",
                            of: window
                        },
                        close: function( event, ui ) {
                            bundleDetailsDialog.dialog("destroy");
                            bundleDetailsDialog.remove();
                        }
                    });
                });
            },
            "lengthMenu": [ [10, 25, 50, -1], [10, 25, 50, $.fn.dataTable.defaults.oLanguage.sLengthAll] ],
            "pageLength": -1,
            "responsive": true,
            "processing": true 
        } );
        return table;
    }
    
    $("body").on("click", ".jgrapes-osgi-bundles-view table button",
        function(event) {
            let portletId = $(this).closest("[data-portlet-id]").attr("data-portlet-id");
            let bundleId = $(this).attr("data-bundle-id");
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

    function toTable(rows, dtFormatter) {
        let html = '<div><table style="width: 100%;"><tbody>';
        for (let keyValue of rows) {
            let key = keyValue[0];
            let value = keyValue[1];
            if (keyValue.length > 2) {
                if (keyValue[2] === "dateTime") {
                    value = dtFormatter.format(new Date(value));
                } else if (keyValue[2] === "table") {
                    value = toTable(value, dtFormatter);
                }
            }
            html += '<tr><td>' + key + ':</td>'
                + '<td>' + value + '</td></tr>';
        }
        html = html + '</tbody></table></div>';
        return html;
    }
    
    JGPortal.registerPortletMethod(
            "org.jgrapes.osgi.portlets.bundles.BundleListPortlet",
            "bundleDetails", function(portletId, params) {
                let bundleId = params[0];
                let bundleInfos = params[1];
                let dialog = $("div[data-bundle-details-for=" + bundleId + "]");
                let dtFormatter = new Intl.DateTimeFormat(
                        dialog.closest('[lang]').attr('lang') || 'en',
                        { year: 'numeric', month: 'numeric', day: 'numeric',
                          hour: 'numeric', minute: 'numeric', second: 'numeric',
                          hour12: false, timeZoneName: 'short' });
                dialog.html(toTable(bundleInfos, dtFormatter));
           });

})();

