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

var orgJGrapesOsgiPortletsLogViewer = {
    l10n: {
    }
};

(function() {

    var l10n = orgJGrapesOsgiPortletsLogViewer.l10n;
    
    orgJGrapesOsgiPortletsLogViewer.initView = function(portletId) {
        let portlet = JGPortal.findPortletView(portletId);
        portlet.data("vue-model", new Vue({
            el: $(portlet.find(".jgrapes-osgi-logviewer-view"))[0],
            data: {
                controller: new JGPortal.TableController([
                    ["time", "${_("timestamp")}"],
                    ["logLevel", "${_("level")}"],
                    ["message", "${_("message")}"],
                    ["bundle", "${_("bundle")}"],
                    ["service", "${_("service")}"],
                    ["exception", "${_("exception")}"],
                    ], {
                    sortKey: "time"
                }),
                entriesBySeq: {},
            },
            computed: {
                filteredData: function() {
                    let infos = Object.values(this.entriesBySeq);
                    return this.controller.filter(infos);
                }
            },
        }));
    }
    
    JGPortal.registerPortletMethod(
            "org.jgrapes.osgi.portlets.logviewer.LogViewerPortlet",
            "entries", function(portletId, params) {
                // View only
                let portlet = JGPortal.findPortletView(portletId);
                let vm = null;
                if (portlet && (vm = portlet.data("vue-model"))) {
                    updateInfos(vm, params[0], params[2]);
                }
            });
     
    function updateInfos(model, infos, replace) {
        // Update
//        alert ("Update")
        model.entriesBySeq = infos;
//        model.devices.sort(function(a, b) {
//            return String(a.friendlyName)
//                .localeCompare(b.friendlyName, model.lang);
//        });
    }

})();

