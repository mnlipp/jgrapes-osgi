/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2018 Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

var orgJGrapesOsgiPortletsBundles = {
    l10n: {
        "bundleCategory": '${_("bundleCategory")}',
        "bundleRefresh": '${_("bundleRefresh")}',
        "bundleStart": '${_("bundleStart")}',
        "bundleState": '${_("bundleState")}',
        "bundleStop": '${_("bundleStop")}',
        "bundleSymbolicName": '${_("bundleSymbolicName")}',
        "bundleUpdate": '${_("bundleUpdate")}',
        "bundleUninstall": '${_("bundleUninstall")}',
        "bundleVersion": '${_("bundleVersion")}',
        "detailsBeingLoaded": '${_("detailsBeingLoaded")}',
    }
};

(function() {

    var l10n = orgJGrapesOsgiPortletsBundles.l10n;
    
    orgJGrapesOsgiPortletsBundles.initPreviewTable = function(content) {
        let previewTable = $(content).find(".jgrapes-osgi-bundles-preview-table");
        new Vue({
            el: previewTable[0],
            data: {
                controller: new JGPortal.TableController([
                    ["id", '${_("bundleId")}'],
                    ["name", '${_("bundleName")}']
                    ], {
                    sortKey: "id"
                }),
                infosById: {},
            },
            computed: {
                filteredData: function() {
                    let infos = Object.values(this.infosById);
                    return this.controller.filter(infos);
                }
            },
        });
    }

    orgJGrapesOsgiPortletsBundles.initView = function(content) {
        let dtFormatter = new Intl.DateTimeFormat(
                $(content).closest('[lang]').attr('lang') || 'en',
                { year: 'numeric', month: 'numeric', day: 'numeric',
                  hour: 'numeric', minute: 'numeric', second: 'numeric',
                  hour12: false, timeZoneName: 'short' });
        new Vue({
            el: $(content)[0],
            data: {
                portletId: $(content).closest("[data-portlet-id]").data("portlet-id"),
                controller: new JGPortal.TableController([
                    ["id", '${_("bundleId")}'],
                    ["name", '${_("bundleName")}'],
                    ["version", '${_("bundleVersion")}'],
                    ["category", '${_("bundleCategory")}'],
                    ["state", '${_("bundleState")}'],
                    ], {
                    sortKey: "id"
                }),
                infosById: {},
                detailsById: {},
            },
            computed: {
                filteredData: function() {
                    let infos = Object.values(this.infosById);
                    return this.controller.filter(infos);
                }
            },
            methods: {
                bundleAction: function(bundleId, action) {
                    JGPortal.notifyPortletModel(this.portletId, action, parseInt(bundleId));
                },
                toggleDetails: function(bundleId) {
                    if (bundleId in this.detailsById) {
                        delete this.detailsById[bundleId];
                        this.$forceUpdate();
                        return;
                    }
                    JGPortal.notifyPortletModel(this.portletId, "sendDetails", parseInt(bundleId));
                },
                addManifestValueBreaks: function(text) {
                    text = String(text);
                    let parts = text.split('"');
                    text = "";
                    for (let i = 0; i < parts.length; i++) {
                        if (i > 0) {
                            text += '"';
                        }
                        if (i % 2 == 0) {
                            text += parts[i].replace(/,/g, ",<br/>");
                        } else {
                            text += parts[i];
                        }
                    }
                    return text.replace(/\./g, "&#x200b;.")
                },
                formatDateTime: function(value) {
                    return dtFormatter.format(new Date(value));
                }
            }
        });
    }

    function updateInfos(model, infos, replace) {
        if (replace) {
            let infosById = {};
            for(let info of infos) {
                infosById[info.id] = info;
            }
            model.infosById = infosById;
            return;
        }
        let infosById = model.infosById; 
        for(let info of infos) {
            if (info.uninstalled) {
                delete infosById[info.id];
                continue;
            }
            infosById[info.id] = info;
        }
        model.infosById = Object.assign({}, infosById);
    }
    
    JGPortal.registerPortletMethod(
            "org.jgrapes.osgi.portlets.bundles.BundleListPortlet",
            "bundleUpdates", function(portletId, params) {
                let bundleInfos = params[0];
                // Preview
                if (params[1] === "preview" || params[1] === "*") {
                    let table = $(JGPortal.findPortletPreview(portletId))
                        .find(".jgrapes-osgi-bundles-preview-table");
                    let vm = null;
                    if (table.length && (vm = table[0].__vue__)) {
                        updateInfos(vm, params[0], params[2]);
                    }
                }
                
                // View
                if (params[1] === "view" || params[1] === "*") {
                    let view = $(JGPortal.findPortletView(portletId))
                        .find(".jgrapes-osgi-bundles-view");
                    let vm = null;
                    if (view.length && (vm = view[0].__vue__)) {
                        updateInfos(vm, params[0], params[2]);
                    }
                }
            });

    JGPortal.registerPortletMethod(
            "org.jgrapes.osgi.portlets.bundles.BundleListPortlet",
            "bundleDetails", function(portletId, params) {
                let view = $(JGPortal.findPortletView(portletId))
                    .find(".jgrapes-osgi-bundles-view");
                let vm = null;
                if (!view.length || !(vm = view[0].__vue__)) {
                    return;
                }
                let bundleId = params[0];
                let bundleDetails = params[1];
                vm.detailsById[bundleId] = bundleDetails;
                vm.$forceUpdate();

//                let dialog = $("div[data-bundle-details-for=" + bundleId + "]");
//                let dtFormatter = new Intl.DateTimeFormat(
//                        dialog.closest('[lang]').attr('lang') || 'en',
//                        { year: 'numeric', month: 'numeric', day: 'numeric',
//                          hour: 'numeric', minute: 'numeric', second: 'numeric',
//                          hour12: false, timeZoneName: 'short' });
//                dialog.html(toTable(bundleInfos, dtFormatter));
           });

})();

