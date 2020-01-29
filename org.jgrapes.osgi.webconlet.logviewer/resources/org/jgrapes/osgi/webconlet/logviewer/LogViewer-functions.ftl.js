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

import Vue from "../../page-resource/vue/vue.esm.browser.js"

window.orgJGrapesOsgiConletLogViewer = {};

window.orgJGrapesOsgiConletLogViewer.initView = function(content) {
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
            conletId: $(content).closest("[data-conlet-id]").data("conlet-id"),
            controller: new JGConsole.TableController([
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
                    JGConsole.notifyConletModel(this.conletId, "resync");
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

window.orgJGrapesOsgiConletLogViewer.onUnload = function(content) {
    if ("__vue__" in content) {
        content.__vue__.$destroy();
        return;
    }
    for (let child of content.children) {
        window.orgJGrapesOsgiConletBundles.onUnload(child);
    }
}

JGConsole.registerConletMethod(
        "org.jgrapes.osgi.webconlet.logviewer.LogViewerConlet",
        "entries", function(conletId, params) {
            // View only
            let view = $(JGConsole.findConletView(conletId))
                .find(".jgrapes-osgi-logviewer-view");
            let vm = null;
            if (view.length && (vm = view[0].__vue__)) {
                vm.entries = params[0];
            }
        });
 
JGConsole.registerConletMethod(
        "org.jgrapes.osgi.webconlet.logviewer.LogViewerConlet",
        "addEntry", function(conletId, params) {
            // View only
            let view = $(JGConsole.findConletView(conletId))
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

