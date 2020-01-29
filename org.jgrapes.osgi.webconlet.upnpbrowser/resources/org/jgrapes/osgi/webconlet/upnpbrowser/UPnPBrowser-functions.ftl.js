/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2018,2020 Michael N. Lipp
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

window.orgJGrapesOsgiConletUPnPBrowser = {};

window.orgJGrapesOsgiConletUPnPBrowser.initPreview = function(preview) {
    preview = $(preview);
    new Vue({
        el: preview.find("ul")[0],
        data: {
            devices: [],
            lang: $(preview.closest("[lang]").attr("lang")),
        },
    });
}

Vue.component('upnpbrowser-device-tree', {
    template: '#upnpbrowser-device-tree-template',
    props: {
        devices: {
            type: Array,
            default: []
        },
    },
    data: function () {
        return {
            expanded: {},
        }
    },
    methods: {
        isExpandable: function(device) {
            return 'childDevices' in device && 
                device.childDevices.length && !this.expanded[device.udn];
        },
        isExpanded: function(device) {
            return this.expanded[device.udn];
        },
        toggle: function(device, event) {
            let expandedDevices = this.expanded;
            if (expandedDevices[device.udn]) {
                delete expandedDevices[device.udn];
            } else {
                expandedDevices[device.udn] = true;
            }
            this.expanded = Object.assign({}, expandedDevices);
            event.stopPropagation();
        },
        deviceClasses: function(device) {
            return {
                expanded: 'childDevices' in device && 
                    device.childDevices.length && this.expanded[device.udn],
                expandable: 'childDevices' in device && 
                    device.childDevices.length && !this.expanded[device.udn],
                unexpandable: !('childDevices' in device) || 
                    device.childDevices.length == 0,
            }
        }
    }
});

window.orgJGrapesOsgiConletUPnPBrowser.initView = function(view) {
    view = $(view);
    new Vue({
        el: view.find("div")[0],
        data: {
            devices: [],
            lang: $(view.closest("[lang]").attr("lang")),
        },
    });
}

window.orgJGrapesOsgiConletUPnPBrowser.onUnload = function(content) {
    if ("__vue__" in content) {
        content.__vue__.$destroy();
        return;
    }
    for (let child of content.children) {
        window.orgJGrapesOsgiConletUPnPBrowser.onUnload(child);
    }
}

function findVueVm (conlet) {
    if ("__vue__" in conlet) {
        return conlet.__vue__;
    }
    for (let child of conlet.children) {
        let vm = findVueVm(child);
        if (vm != null) {
            return vm;
        }
    }
    return null;
}

JGConsole.registerConletMethod(
        "org.jgrapes.osgi.webconlet.upnpbrowser.UPnPBrowserConlet",
        "deviceUpdates", function(conletId, params) {
            // Preview
            if (params[1] === "preview" || params[1] === "*") {
                let preview = $(JGConsole.findConletPreview(conletId));
                let vm = null;
                if (preview && (vm = findVueVm($(preview)
                        .find(".jgrapes-osgi-upnpbrowser-preview")[0]))) {
                    updateInfos(vm, params[0], params[2]);
                }
            }
                
            // View
            if (params[1] === "view" || params[1] === "*") {
                let view = JGConsole.findConletView(conletId);
                let vm = null;
                if (view && (vm = findVueVm($(view)
                        .find(".jgrapes-osgi-upnpbrowser-view div")[0]))) {
                    updateInfos(vm, params[0], params[2]);
                }
            }
        });
    
function updateInfos(model, infos, replace) {
    // Update
    model.devices = infos;
    model.devices.sort(function(a, b) {
        return String(a.friendlyName)
            .localeCompare(b.friendlyName, model.lang);
    });
}
