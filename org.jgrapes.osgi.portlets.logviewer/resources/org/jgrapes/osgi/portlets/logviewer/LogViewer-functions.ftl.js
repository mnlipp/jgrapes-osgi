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
        let levelsToNum = {
                "AUDIT": 5,
                "ERROR": 4,
                "WARN": 3,
                "INFO": 2,
                "DEBUG": 1,
                "TRACE": 0
        }
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
                    sortKey: "sequence",
                    sortOrder: "down"
                }),
                messageThreshold: "INFO",
                entries: [],
                lang: portlet.closest('[lang]').attr('lang') || 'en',
                portletId: portletId
            },
            computed: {
                filteredData: function() {
                    let entries = [];
                    let threshold = levelsToNum[this.messageThreshold];
                    for (let entry of this.entries) {
                        if (levelsToNum[entry.logLevel] >= threshold) {
                            entries.push(entry);
                        }
                    }
                    return this.controller.filter(entries);
                }
            },
            methods: {
                resync: function(event) {
                        let portletId = $(this.$el).parent().attr("data-portlet-id");
                        JGPortal.notifyPortletModel(portletId, "resync");
                },
                formatTimestamp: function(timestamp) {
                    let ts = moment(timestamp);
                    ts.locale(this.lang)
                    return ts.format("L HH:mm:ss.SSS");
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
                    vm.entries = params[0];
                }
                Vue.nextTick(function () {
                    portlet.find('[data-toggle="popover"]').popover({
                        trigger: 'focus',
                        template: "<div class='jgrapes-osgi-logviewer-stacktrace popover' role='tooltip'><div class='arrow'></div><h3 class='popover-header'></h3><div class='popover-body'></div></div>"
                    })
                })
            });
     
    JGPortal.registerPortletMethod(
            "org.jgrapes.osgi.portlets.logviewer.LogViewerPortlet",
            "addEntry", function(portletId, params) {
                // View only
                let portlet = JGPortal.findPortletView(portletId);
                let vm = null;
                if (portlet && (vm = portlet.data("vue-model"))) {
                    vm.entries = Object.assign([params[0]], vm.entries);
                }
            });

})();

