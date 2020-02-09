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

Vue.component('jgwc-tree', {
    props: {
        level: {
            type: Number,
            default: 1
            },
        items: {
            type: Array,
            required: true,
        },
        childItems: Function,
        onToggle: {
            type: Function,
            default: function(item, newStateOpen, event) {
                return newStateOpen;
            }
        },
        focusHolder: {
            type: Array,
            default: function() { return [null]; }
        }
    },
    data: function () {
        return {
            expanded: [],
        }
    },
    watch: {
        items: function(newItems, oldItems) {
            if (this.level === 1 && this.focusHolder[0] === null
                && newItems.length > 0) {
                this.focusHolder[0] = newItems[0];       
            }
        }
    },
    methods: {
        isExpandable: function(item) {
            let children = this.childItems(item);
            return children != null && children.length > 0;
        },
        isExpanded: function(item) {
            return this.expanded.includes(item);
        },
        ariaExpanded: function(item) {
            if (!this.isExpandable(item)) {
                return null;
            }
            return new Boolean(this.isExpanded(item)).toString();
        },
        toggle: function(item, event) {
            this.$set(this.focusHolder, 0, item);
            let index = this.expanded.indexOf(item);
            let isOpen = index >= 0;
            let isExpandable = this.isExpandable(item);
            let toBeOpened = this.onToggle(item, isExpandable && !isOpen, event);
            if (!isExpandable || toBeOpened == isOpen) {
                return;
            }
            if (toBeOpened) {
                this.expanded.push(item);
                let children = this.childItems(item);
            } else {
                this.expanded.splice(index, 1);
            }
        }
    },
    template: `
      <ul :role="level == 1 ? 'tree' : 'group'">
        <li v-for="(item, index) in items" ref="treeitem" 
          :role="isExpandable(item) ? 'treeitem' : 'none'"
          :tabindex="item === focusHolder[0] ? 0 : -1"
          :aria-level="level" :aria-setsize="items.length" 
          :aria-posinset="index + 1" :aria-expanded="ariaExpanded(item)">
          <span @click="toggle(item, $event)"
            ><slot name="label" v-bind:item="item"></slot></span>
          <jgwc-tree v-if="isExpanded(item)" 
            :level="level + 1" :items="childItems(item)"
            :child-items="childItems" :on-toggle="onToggle"
            :focus-holder="focusHolder">
            <template v-slot:label="props">
              <slot name="label" v-bind:item="props.item"></slot>
            </template>
          </jgwc-tree>
        </li>
      </ul>`,
});


window.orgJGrapesOsgiConletUPnPBrowser.initView = function(view) {
    view = $(view);
    new Vue({
        el: view.find("div")[0],
        data: {
            devices: [],
            lang: $(view.closest("[lang]").attr("lang")),
        },
        methods: {
            childItems: function(device) {
                if ('childDevices' in device) {
                    return device.childDevices;
                }
                return [];
            },
        }
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
