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

import Vue from "../../page-resource/vue/vue.esm.browser.js"
import { jgwcIdScopeMixin } from "../../page-resource/jgwc-vue-components/jgwc-components.js";

window.orgJGrapesOsgiConletBundles = {};

window.orgJGrapesOsgiConletBundles.initPreviewTable = function(content) {
    let previewTable = $(content).find(".jgrapes-osgi-bundles-preview-table");
    new Vue({
        el: previewTable[0],
        data: {
            controller: new JGConsole.TableController([
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

window.orgJGrapesOsgiConletBundles.initView = function(content) {
    let dtFormatter = new Intl.DateTimeFormat(
            $(content).closest('[lang]').attr('lang') || 'en',
            { year: 'numeric', month: 'numeric', day: 'numeric',
              hour: 'numeric', minute: 'numeric', second: 'numeric',
              hour12: false, timeZoneName: 'short' });
    let cont = $(content);
    new Vue({
        mixins: [jgwcIdScopeMixin],
        el: $(content)[0],
        data: {
            conletId: $(content).closest("[data-conlet-id]").data("conlet-id"),
            controller: new JGConsole.TableController([
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
                JGConsole.notifyConletModel(this.conletId, action, parseInt(bundleId));
            },
            updateDetails: function(bundleId, show) {
                if (show) {
                    JGConsole.notifyConletModel(this.conletId, "sendDetails", parseInt(bundleId));
                } else {
                    Vue.delete(this.detailsById, bundleId);
                }
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
    cont = null;
}

window.orgJGrapesOsgiConletBundles.onUnload = function(content) {
    if ("__vue__" in content) {
        content.__vue__.$destroy();
        return;
    }
    for (let child of content.children) {
        window.orgJGrapesOsgiConletBundles.onUnload(child);
    }
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
    
JGConsole.registerConletMethod(
    "org.jgrapes.osgi.webconlet.bundles.BundleListConlet",
    "bundleUpdates", function(conletId, params) {
        let bundleInfos = params[0];
        // Preview
        if (params[1] === "preview" || params[1] === "*") {
            let table = $(JGConsole.findConletPreview(conletId))
                .find(".jgrapes-osgi-bundles-preview-table");
            let vm = null;
            if (table.length && (vm = table[0].__vue__)) {
                updateInfos(vm, params[0], params[2]);
            }
        }
            
        // View
        if (params[1] === "view" || params[1] === "*") {
            let view = $(JGConsole.findConletView(conletId))
                .find(".jgrapes-osgi-bundles-view");
                let vm = null;
                if (view.length && (vm = view[0].__vue__)) {
                    updateInfos(vm, params[0], params[2]);
                }
            }
        });

JGConsole.registerConletMethod(
    "org.jgrapes.osgi.webconlet.bundles.BundleListConlet",
    "bundleDetails", function(conletId, params) {
        let view = $(JGConsole.findConletView(conletId))
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

