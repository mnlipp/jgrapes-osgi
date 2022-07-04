/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2021  Michael N. Lipp
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

import { reactive, createApp, computed }
    from "../../page-resource/vue/vue.esm-browser.js";
import JGConsole from "../../console-base-resource/jgconsole.js"
import JgwcPlugin, { JGWC } 
    from "../../page-resource/jgwc-vue-components/jgwc-components.js";
import { provideApi, getApi } 
    from "../../page-resource/aash-vue-components/lib/aash-vue-components.js";

const l10nBundles = new Map();
let entries = null;
// <#list supportedLanguages() as l>
entries = new Map();
l10nBundles.set('${l.locale.toLanguageTag()}', entries);
// <#list l.l10nBundle.keys as key>
entries.set('${key}', '${l.l10nBundle.getString(key)}')
// </#list>
// </#list>    

window.orgJGrapesOsgiConletServices = {};

window.orgJGrapesOsgiConletServices.initPreviewTable = function(content) {
    let previewTable = $(content).find(".jgrapes-osgi-services-preview-table");
    let app = createApp ({
        setup() {
            const controller = reactive(new JGConsole.TableController([
                ["id", 'serviceId'],
                ["type", 'serviceType']
                ], {
                sortKey: "id"
            }));
            const infosById = reactive(new Map());
            const filteredData = computed(() => {
                let infos = Array.from(infosById.values());
                return controller.filter(infos);
            });
            const localize = (key) => {
                return JGConsole.localize(
                    l10nBundles, JGWC.lang(), key);
            };
            
            provideApi (previewTable[0], {
                infosById: () => { return infosById }
            });
            
            return { controller, infosById, filteredData, localize }
        }
    });
    app.use(JgwcPlugin);
    app.mount(previewTable[0]);
}

window.orgJGrapesOsgiConletServices.initView = function(content) {
    const dom = $(content)[0];
    let app = createApp({
        setup() {
            const conletId = $(content).closest("[data-conlet-id]").data("conlet-id");
            const controller = reactive(new JGConsole.TableController([
                ["id", 'serviceId'],
                ["type", 'serviceType'],
                ["scopeDisplay", 'serviceScope'],
                ["bundleNameDisplay", 'serviceBundle'],
                ["implementationClass", 'serviceImplementedBy'],
                ], {
                sortKey: "id"
            }));
            const infosById = reactive(new Map());
            const detailsById = reactive(new Map());
            const filteredData = computed(() => {
                let infos = Array.from(infosById.values());
                for (let info of infos) {
                    info.scopeDisplay = localize(info.scope);
                    if (info.dsScope !== undefined) {
                        info.scopeDisplay += " (" 
                            + localize("serviceAbbrevDS") + ": " 
                            + localize(info.dsScope) + ")";
                    }
                    info.bundleNameDisplay = info.bundleName + " (" + info.bundleId + ")";
                }
                return controller.filter(infos);
            });
            
            let sortedProperties = (properties) => {
                let entries = Object.entries(properties);
                entries.sort();
                return entries;
            };
            
            const localize = (key) => {
                return JGConsole.localize(
                    l10nBundles, JGWC.lang(), key);
            };
            
            const idScope = JGWC.createIdScope();
            
            provideApi (content, {
                infosById: () => { return infosById; },
                detailsById: () => { return detailsById; }
            });
            
            return { conletId, controller, infosById, detailsById,
                filteredData, sortedProperties, localize,
                scopedId: (id) => { return idScope.scopedId(id); } }
        }
    });
    app.use(JgwcPlugin);
    app.mount(dom);
}
    
function updateInfos(api, infos, replace) {
    // Update
    let infosById = api.infosById();
    if (replace) {
        infosById.clear();
        for(let info of infos) {
            infosById.set(info.id, info);
        }
        return;
    }
    for(let info of infos) {
        if (info.uninstalled) {
            infosById.delete(info.id);
            continue;
        }
        infosById.set(info.id, info);
    }
}
  
JGConsole.registerConletFunction(
        "org.jgrapes.osgi.webconlet.services.ServiceListConlet",
        "serviceUpdates", function(conletId, serviceInfos, applyTo, replace) {
            // Preview
            if (applyTo === "preview" || applyTo === "*") {
                let table = $(JGConsole.findConletPreview(conletId).element())
                    .find(".jgrapes-osgi-services-preview-table");
                let api = null;
                if (table.length && (api = getApi(table[0]))) {
                    updateInfos(api, serviceInfos, replace);
                }
            }
            
            // View
            if (applyTo === "view" || applyTo === "*") {
                let view = $(JGConsole.findConletView(conletId).element())
                    .find(".jgrapes-osgi-services-view");
                let api = null;
                if (view.length && (api = getApi(view[0]))) {
                    if (replace) {
                        api.detailsById().clear();
                    }
                    updateInfos(api, serviceInfos, replace);
                }
            }
        });
