/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2018  Michael N. Lipp
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

var orgJGrapesOsgiPortletsUPnPBrowser = {
    l10n: {
    }
};

(function() {

    var l10n = orgJGrapesOsgiPortletsUPnPBrowser.l10n;
    
    orgJGrapesOsgiPortletsUPnPBrowser.initPreview = function(portletId) {
        let portlet = JGPortal.findPortletPreview(portletId);
        portlet.data("vue-model", new Vue({
            el: $(portlet.find(".jgrapes-osgi-upnpbrowser-preview"))[0],
            data: {
                devices: [],
                lang: $(portlet.closest("[lang]").attr("lang")),
            },
        }));
    }

    orgJGrapesOsgiPortletsUPnPBrowser.initView = function(portletId) {
        let expandedDevices = {};
        Vue.component('upnpbrowser-device-tree', {
            template: '#upnpbrowser-device-tree-template',
            props: {
              devices: Object,
            },
            data: function () {
              return {
                  expanded: expandedDevices,
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
        let portlet = JGPortal.findPortletView(portletId);
        let content = $(portlet.find(".jgrapes-osgi-upnpbrowser-view"))[0];
        portlet.data("vue-model", new Vue({
            el: content,
            data: {
                devices: [],
                lang: $(portlet.closest("[lang]").attr("lang")),
            },
        }));
    }
    
    JGPortal.registerPortletMethod(
            "org.jgrapes.osgi.portlets.upnpbrowser.UPnPBrowserPortlet",
            "deviceUpdates", function(portletId, params) {
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
    
    function updateInfos(model, infos, replace) {
        // Update
        model.devices = infos;
        model.devices.sort(function(a, b) {
            return String(a.friendlyName)
                .localeCompare(b.friendlyName, model.lang);
        });
    }

})();

