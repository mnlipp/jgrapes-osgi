/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2018,2019  Michael N. Lipp
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
    
    orgJGrapesOsgiPortletsLogViewer.initView = function(content) {
        let levelsToNum = {
                "AUDIT": 5,
                "ERROR": 4,
                "WARN": 3,
                "INFO": 2,
                "DEBUG": 1,
                "TRACE": 0
        }
        new Vue({
            el: content,
            data: {
                portletId: $(content).closest("[data-portlet-id]").data("portlet-id"),
                controller: new JGPortal.TableController([
                    ["time", '${_("timestamp")}'],
                    ["logLevel", '${_("level")}'],
                    ["message", '${_("message")}'],
                    ["bundle", '${_("bundle")}'],
                    ["service", '${_("service")}'],
                    ["exception", '${_("exception")}'],
                    ], {
                    sortKey: "sequence",
                    sortOrder: "down"
                }),
                messageThreshold: "INFO",
                entries: [],
                lang: $(content).closest('[lang]').attr('lang') || 'en',
				autoUpdate: true
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
                resync: function() {
                        JGPortal.notifyPortletModel(this.portletId, "resync");
                },
                formatTimestamp: function(timestamp) {
                    let ts = moment(timestamp);
                    ts.locale(this.lang)
                    return ts.format("L HH:mm:ss.SSS");
                } 
            },
            watch: {
	            autoUpdate: function(newValue, oldValue) {
		            if (newValue) {
			            this.resync();
		            }
	            }
            }
        });
    }
    
    JGPortal.registerPortletMethod(
            "org.jgrapes.osgi.portlets.logviewer.LogViewerPortlet",
            "entries", function(portletId, params) {
                // View only
                let view = $(JGPortal.findPortletView(portletId))
                    .find(".jgrapes-osgi-bundles-view");
                let vm = null;
                if (view.length && (vm = view[0].__vue__)) {
                    vm.entries = params[0];
                }
            });
     
    JGPortal.registerPortletMethod(
            "org.jgrapes.osgi.portlets.logviewer.LogViewerPortlet",
            "addEntry", function(portletId, params) {
                // View only
                let view = $(JGPortal.findPortletView(portletId))
                    .find(".jgrapes-osgi-logviewer-view");
                let vm = null;
                if (view.length && (vm = view[0].__vue__)) {
					if (!vm.autoUpdate) {
						return;
					}
					if (vm.entries) {
						vm.entries.unshift(params[0]);
					} else {
						vm.entries = [params[0]];
					}
                }
            });

})();

