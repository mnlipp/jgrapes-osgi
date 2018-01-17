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

var orgJGrapesOsgiPortletsServices = {
    l10n: {
        "serviceAbbrevDS": "${_("serviceAbbrevDS")}",
        "serviceBundle": "${_("serviceBundle")}",
        "serviceImplementedBy": "${_("serviceImplementedBy")}",
        "serviceRanking": "${_("serviceRanking")}",
        "serviceScope": "${_("serviceScope")}",
        "serviceScopeBundle": "${_("serviceScopeBundle")}",
        "serviceScopePrototype": "${_("serviceScopePrototype")}",
        "serviceScopeSingleton": "${_("serviceScopeSingleton")}",
    }
};

(function() {

    var l10n = orgJGrapesOsgiPortletsServices.l10n;
    
    function createTooltip(service) {
        let tooltip = '<div>';
        tooltip += '<table><tbody>';
        tooltip += '<tr><td>' + l10n.serviceBundle + ':</td>';
        tooltip += '<td>' + service.bundleName + ' (' + service.bundleId + ')</td></tr>';
        tooltip += '<tr><td>' + l10n.serviceScope + ':</td>';
        tooltip += '<td>' + l10n[service.scope];
        if (service.dsScope !== undefined) {
            tooltip += " (" + l10n.serviceAbbrevDS + ": " + l10n[service.dsScope] + ")";
        }
        tooltip += '</td></tr>';
        tooltip += '<tr><td>' + l10n.serviceImplementedBy + ':</td>';
        tooltip += '<td>' + service.implementationClass + '</td></tr>';
        tooltip += '<tr><td>' + l10n.serviceRanking + ':</td>';
        tooltip += '<td>' + service.ranking + '</td></tr>';
        tooltip += '</tbody></table></div>';
        return tooltip;
    }

    function registerTooltip(row, serviceInfo) {
        row.data("serviceInfo", serviceInfo);
        row.tooltip({
            items: "[data-service-id]",
            content: function() {
                return createTooltip($( this ).data("serviceInfo"));
            },
            classes: {
                "ui-tooltip": "ui-corner-all ui-widget-shadow jgrapes-osgi-services-preview-tooltip"
            },
            close: function() {
                // https://bugs.jqueryui.com/ticket/10689
                $(".ui-helper-hidden-accessible > *:not(:last)").remove();
            },
        });
    }
    
    orgJGrapesOsgiPortletsServices.initPreviewTable = function(tableSelector) {
        JGPortal.lockMessageQueue();
        let table = tableSelector.DataTable( {
            "initComplete": function( settings, json ) {
                tableSelector.DataTable().processing(true);
                JGPortal.unlockMessageQueue();
            },
            "columns": [ 
                { "data": "id" },
                { "data": "type",
                  "render": function(data, type, row, meta) {
                      return data.replace(/\./g, '.&#x200b;');
                  }
                },
            ],
            "searching": false,
            "paging": false,
            "info": false,
            "dom": "t",
            "rowCallback": function( row, data, dataIndex ) {
                $(row).attr("data-service-id", data.id);
                registerTooltip($(row), data);
            },
            "processing": true 
        } );
        return table;
    }
    
    orgJGrapesOsgiPortletsServices.initViewTable = function(tableSelector) {
        JGPortal.lockMessageQueue();
        let table = tableSelector.DataTable( {
            "initComplete": function( settings, json ) {
                tableSelector.DataTable().processing(true);
                JGPortal.unlockMessageQueue();
            },
            "columns": [ 
                { "data": "id" },
                { "data": "type" },
                { "data": "scope",
                    "render": function ( data, type, row, meta ) {
                        let scope = l10n[data];
                        if (row.dsScope !== undefined) {
                            scope += " (" + l10n.serviceAbbrevDS + ": " + l10n[row.dsScope] + ")";
                        }
                        return scope;
                    }
                },
                { "data": "bundleName",
                  "render": function(data, type, row, meta) {
                      return data + ' (' + row.bundleId + ')';
                  }
                },
                { "data": "implementationClass",
                  "render": function(data, type, row, meta) {
                      return data.replace(/\./g, '.&#x200b;');
                  }
                },
                ],
            "lengthMenu": [ [10, 25, 50, -1], [10, 25, 50, $.fn.dataTable.defaults.oLanguage.sLengthAll] ],
            "pageLength": -1,
            "processing": true 
        } );
        return table;
    }
    
    function updateTable(table, serviceInfos, replace) {
        if (replace) {
            table.clear();
        }
        serviceInfos.forEach(function(serviceInfo) {
            if (!replace) {
                let row = table.row(function ( idx, data, node ) {
                    return data.id === serviceInfo.id;
                });
                if (row.length > 0) {
                    if (serviceInfo.updateType !== undefined 
                            && serviceInfo.updateType === "unregistering") {
                        table.row(row).remove();
                    } else {
                        table.row(row).data(serviceInfo);
                    }
                    return;
                }
            }
            if (!serviceInfo.uninstalled) {
                table.row.add(serviceInfo);
            }
        });
        table.processing(false);
        table.columns.adjust().draw();
    }
    
    JGPortal.registerPortletMethod(
            "org.jgrapes.osgi.portlets.services.ServiceListPortlet",
            "serviceUpdates", function(portletId, params) {
                let serviceInfos = params[0];
                // Preview
                if (params[1] === "preview" || params[1] === "*") {
                    let portlet = JGPortal.findPortletPreview(portletId);
                    if (portlet) {
                        let table = portlet.find(".jgrapes-osgi-services-preview table.services-table").DataTable();
                        updateTable(table, serviceInfos, params[2]);
                    }
                }
                
                // View
                if (params[1] === "view" || params[1] === "*") {
                    let portlet = JGPortal.findPortletView(portletId);
                    if (portlet) {
                        let table = portlet.find("table").DataTable();
                        updateTable(table, serviceInfos, params[2]);
                    }
                }
            });

})();

