/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2018  Michael N. Lipp
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
        let portlet = JGPortal.findPortletView(portletId);
    }
    
    function updateInfos(model, infos, replace) {
        // Update
        model.devices = infos;
        model.devices.sort(function(a, b) {
            return String(a.friendlyName)
                .localeCompare(b.friendlyName, model.lang);
        });
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
    
})();

