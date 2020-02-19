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

import Vue from "../../page-resource/vue/vue.esm.browser.js"
import { jgwcIdScopeMixin } from "../../page-resource/jgwc-vue-components/jgwc-components.js";

const l10nBundles = {
    // <#list supportedLanguages() as l>
    '${l.locale.toLanguageTag()}': {
        // <#list l.l10nBundle.keys as key>
        '${key}': '${l.l10nBundle.getString(key)}',
        // </#list>
    },
    // </#list>    
};

window.orgJGrapesOsgiConletServices = {};

window.orgJGrapesOsgiConletServices.initPreviewTable = function(content) {
    let previewTable = $(content).find(".jgrapes-osgi-services-preview-table");
    new Vue({
        el: previewTable[0],
        data: {
            controller: new JGConsole.TableController([
                ["id", 'serviceId'],
                ["type", 'serviceType']
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
        methods: {
            localize: function(key) {
                return JGConsole.localize(
                    l10nBundles, this.jgwc.observed.lang, key);
            }
        }
    });
}

window.orgJGrapesOsgiConletServices.initView = function(content) {
    new Vue({
        mixins: [jgwcIdScopeMixin],
        el: $(content)[0],
        data: {
            conletId: $(content).closest("[data-conlet-id]").data("conlet-id"),
            controller: new JGConsole.TableController([
                ["id", 'serviceId'],
                ["type", 'serviceType'],
                ["scopeDisplay", 'serviceScope'],
                ["bundleNameDisplay", 'serviceBundle'],
                ["implementationClass", 'serviceImplementedBy'],
                ], {
                sortKey: "id"
            }),
            infosById: {},
            detailsById: {},
        },
        computed: {
            filteredData: function() {
                let infos = Object.values(this.infosById);
                for (let info of infos) {
                    info.scopeDisplay = this.localize(info.scope);
                    if (info.dsScope !== undefined) {
                        info.scopeDisplay += " (" 
                            + this.localize("serviceAbbrevDS") + ": " 
                            + this.localize(info.dsScope) + ")";
                    }
                    info.bundleNameDisplay = info.bundleName + " (" + info.bundleId + ")";
                }
                return this.controller.filter(infos);
            },
        },
        methods: {
            sortedProperties: function(properties) {
                let entries = Object.entries(properties);
                entries.sort();
                return entries;
            },
            localize: function(key) {
                return JGConsole.localize(
                    l10nBundles, this.jgwc.observed.lang, key);
            }
        },
    });
}
    
function updateInfos(model, infos, replace) {
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
  
JGConsole.registerConletMethod(
        "org.jgrapes.osgi.webconlet.services.ServiceListConlet",
        "serviceUpdates", function(conletId, params) {
            let serviceInfos = params[0];
            // Preview
            if (params[1] === "preview" || params[1] === "*") {
                let table = $(JGConsole.findConletPreview(conletId))
                    .find(".jgrapes-osgi-services-preview-table");
                let vm = null;
                if (table.length && (vm = table[0].__vue__)) {
                    updateInfos(vm, params[0], params[2]);
                }
            }
            
            // View
            if (params[1] === "view" || params[1] === "*") {
                let view = $(JGConsole.findConletView(conletId))
                    .find(".jgrapes-osgi-services-view");
                let vm = null;
                if (view.length && (vm = view[0].__vue__)) {
                    updateInfos(vm, params[0], params[2]);
                }
            }
        });
