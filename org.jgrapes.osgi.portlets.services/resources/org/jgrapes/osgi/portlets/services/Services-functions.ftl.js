/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2018  Michael N. Lipp
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
        "serviceUsingBundles": "${_("serviceUsingBundles")}",
    }
};

(function() {

    var l10n = orgJGrapesOsgiPortletsServices.l10n;
    
    orgJGrapesOsgiPortletsServices.initPreviewTable = function(portletId) {
        let portlet = JGPortal.findPortletPreview(portletId);
        portlet.data("vue-model", new Vue({
            el: $(portlet.find(".jgrapes-osgi-services-preview-table"))[0],
            data: {
                controller: new JGPortal.TableController([
                    ["id", "${_("serviceId")}"],
                    ["type", "${_("serviceType")}"]
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
        }));
    }

    orgJGrapesOsgiPortletsServices.initViewTable = function(portletId) {
        let portlet = JGPortal.findPortletView(portletId);
        portlet.data("vue-model", new Vue({
            el: $(portlet.find(".jgrapes-osgi-services-view"))[0],
            data: {
                controller: new JGPortal.TableController([
                    ["id", "${_("serviceId")}"],
                    ["type", "${_("serviceType")}"],
                    ["scopeDisplay", "${_("serviceScope")}"],
                    ["bundleNameDisplay", "${_("serviceBundle")}"],
                    ["implementationClass", "${_("serviceImplementedBy")}"],
                    ], {
                    sortKey: "id"
                }),
                infosById: {},
                detailsFor: new Set(),
            },
            computed: {
                filteredData: function() {
                    let infos = Object.values(this.infosById);
                    return this.controller.filter(infos);
                },
            },
            methods: {
                toggleDetails: function(serviceId) {
                    if (this.detailsFor.has(serviceId)) {
                        this.detailsFor.delete(serviceId);
                        this.$forceUpdate();
                        return;
                    }
                    this.detailsFor.add(serviceId);
                    this.$forceUpdate();
                },
                sortedProperties: function(properties) {
                    let entries = Object.entries(properties);
                    entries.sort();
                    return entries;
                },
            },
        }));
    }
    
    function updateInfos(model, infos, replace) {
        // Augment info for display
        for (let info of infos) {
            info.scopeDisplay = l10n[info.scope];
            if (info.dsScope !== undefined) {
                info.scopeDisplay += " (" + l10n.serviceAbbrevDS + ": " + l10n[info.dsScope] + ")";
            }
            info.bundleNameDisplay = info.bundleName + " (" + info.bundleId + ")";
        }

        // Update
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
            "org.jgrapes.osgi.portlets.services.ServiceListPortlet",
            "serviceUpdates", function(portletId, params) {
                let serviceInfos = params[0];
                // Preview
                if (params[1] === "preview" || params[1] === "*") {
                    let portlet = JGPortal.findPortletPreview(portletId);
                    let vm = null;
                    if (portlet && (vm = portlet.data("vue-model"))) {
                        updateInfos(vm, params[0], params[2]);
                    }
                }
                
                // View
                if (params[1] === "view" || params[1] === "*") {
                    let portlet = JGPortal.findPortletView(portletId);
                    let vm = null;
                    if (portlet && (vm = portlet.data("vue-model"))) {
                        updateInfos(vm, params[0], params[2]);
                    }
                }
            });

})();

