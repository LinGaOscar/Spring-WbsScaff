    const { createApp, ref, computed, onMounted, defineComponent, provide, inject } = Vue;
    const STATUS_LABEL = { NOT_STARTED:'未開始', IN_PROGRESS:'進行中', DONE:'完成' };
    const STATUS_CYCLE = { NOT_STARTED:'IN_PROGRESS', IN_PROGRESS:'DONE', DONE:'NOT_STARTED' };
    const h = () => ({ 'Content-Type':'application/json', [CSRF_HEADER]:CSRF_TOKEN });
    const api = (url, opt={}) => fetch(url, { headers: h(), ...opt }).then(r => r.json());

    const WbsNodeComp = defineComponent({
      name: 'wbs-node',
      props: ['node', 'cursors', 'locked', 'depth'],
      emits: ['add-child','delete','update','cursor-move','drop-item'],
      template: `
        <div class="wbs-node-wrap">
          <div class="wbs-node" :class="['status-'+node.status.toLowerCase(), {'drag-over': isDragOver}]"
               @mouseenter="onMouseEnter(node.id)" @mouseleave="onMouseLeave()"
               @dragover.prevent="isDragOver=(!locked && (depth||1) < 3)"
               @dragleave="isDragOver=false"
               @drop.stop="onNodeDrop($event)">
            <span class="wbs-toggle" @click="node._open=!node._open">
              {{ node.children?.length ? (node._open?'▼':'▶') : '　' }}
            </span>
            <span class="wbs-title" v-if="!node._editing"
                  @dblclick="!locked && startEdit()">{{ node.title }}</span>
            <input class="wbs-title-input" v-else v-model="editTitle"
                   @blur="commitEdit" @keyup.enter="commitEdit" @keyup.escape="node._editing=false"
                   ref="titleInput" />
            <span class="wbs-status-badge" @click="!locked && cycleStatus()">
              {{ STATUS_LABEL[node.status] }}
            </span>
            <select class="wbs-field-input wbs-owner-select" v-model="editOwner"
                    :disabled="locked"
                    @change="!locked && commitField('owner', editOwner)">
              <option value="">負責人</option>
              <option v-for="m in projectMembers" :key="m.userId" :value="m.displayName">
                {{ m.displayName }}
              </option>
            </select>
            <span class="wbs-date-btn" :class="{'has-date':!!editStartDate,'is-locked':locked}">
              <span class="wbs-date-icon">📅</span>
              <span v-if="editStartDate" class="wbs-date-text">{{ fmtDate(editStartDate) }}</span>
              <input v-if="!locked" type="date" v-model="editStartDate"
                     class="wbs-date-overlay"
                     @change="commitField('startDate', editStartDate)" />
            </span>
            <span class="wbs-date-btn" :class="{'has-date':!!editEndDate,'is-locked':locked}">
              <span class="wbs-date-icon">📅</span>
              <span v-if="editEndDate" class="wbs-date-text">{{ fmtDate(editEndDate) }}</span>
              <input v-if="!locked" type="date" v-model="editEndDate"
                     class="wbs-date-overlay"
                     @change="commitField('endDate', editEndDate)" />
            </span>
            <input class="wbs-field-input" type="text" v-model="editNotes"
                   placeholder="備註" :readonly="locked"
                   @blur="!locked && commitField('notes', editNotes)" />
            <div class="wbs-node-actions" v-if="!locked">
              <button v-if="(depth||1) < 3" @click="$emit('add-child', {node, depth: depth||1})">+ 子</button>
              <button @click="$emit('delete', node)">刪</button>
            </div>
            <div class="cursor-indicators">
              <span v-for="c in nodeCursors(node.id)" :key="c.userId"
                    class="cursor-dot" :style="{background:c.color}"
                    :title="c.displayName + (c.editingNodeId===node.id?' 正在編輯':' 正在查看')">
              </span>
            </div>
          </div>
          <div class="wbs-children" v-if="node._open && node.children?.length">
            <wbs-node v-for="c in node.children" :key="c.id" :node="c" :cursors="cursors" :locked="locked" :depth="(depth||1)+1"
                      @add-child="$emit('add-child',$event)"
                      @delete="$emit('delete',$event)"
                      @update="$emit('update',$event)"
                      @cursor-move="$emit('cursor-move',$event)"
                      @drop-item="$emit('drop-item',$event)"></wbs-node>
          </div>
        </div>
      `,
      setup(props, { emit }) {
        const editTitle      = ref('');
        const titleInput     = ref(null);
        const editOwner      = ref(props.node.owner     || '');
        const editStartDate  = ref(props.node.startDate || '');
        const editEndDate    = ref(props.node.endDate   || '');
        const editNotes      = ref(props.node.notes     || '');
        const isDragOver     = ref(false);
        const projectMembers = inject('projectMembers', ref([]));

        function startEdit() {
          editTitle.value = props.node.title;
          props.node._editing = true;
          emit('cursor-move', { hoveringNodeId: props.node.id, editingNodeId: props.node.id });
          setTimeout(() => titleInput.value?.focus(), 50);
        }
        function commitEdit() {
          if (editTitle.value.trim() && editTitle.value !== props.node.title) {
            emit('update', { node: props.node, changes: { title: editTitle.value.trim() } });
          }
          props.node._editing = false;
          emit('cursor-move', { hoveringNodeId: props.node.id, editingNodeId: null });
        }
        function cycleStatus() {
          const next = STATUS_CYCLE[props.node.status];
          emit('update', { node: props.node, changes: { status: next } });
        }
        function commitField(field, value) {
          if (value !== (props.node[field] || '')) {
            emit('update', { node: props.node, changes: { [field]: value || null } });
          }
        }
        function onMouseEnter(nodeId) {
          emit('cursor-move', { hoveringNodeId: nodeId, editingNodeId: null });
        }
        function onMouseLeave() {
          emit('cursor-move', { hoveringNodeId: null, editingNodeId: null });
        }
        function nodeCursors(nodeId) {
          if (!props.cursors) return [];
          return Object.values(props.cursors).filter(
            c => c.hoveringNodeId === nodeId || c.editingNodeId === nodeId
          );
        }
        // 快速子項拖曳到此節點：L1/L2 可接收（最多三層）
        function onNodeDrop(event) {
          isDragOver.value = false;
          if (props.locked || (props.depth || 1) >= 3) return;
          const title = event.dataTransfer.getData('text/plain');
          if (!title) return;
          emit('drop-item', { parentId: props.node.id, title });
        }

        function fmtDate(d) {
          if (!d) return '';
          const [, m, day] = d.split('-');
          return `${m}/${day}`;
        }

        return { editTitle, titleInput, editOwner, editStartDate, editEndDate, editNotes,
                 isDragOver, projectMembers,
                 startEdit, commitEdit, cycleStatus, commitField,
                 onMouseEnter, onMouseLeave, nodeCursors, onNodeDrop, STATUS_LABEL, fmtDate };
      }
    });

    createApp({
      components: { 'wbs-node': WbsNodeComp },
      setup() {
        const flatNodes    = ref([]);
        const projectName  = ref('');
        const quickItems   = ref([]);
        const panelCollapsed   = ref(false);
        const locked       = ref(typeof READ_ONLY !== 'undefined' ? READ_ONLY : false);
        const treeHighlight    = ref(false);
        const showTemplateModal    = ref(false);
        const availableTemplates   = ref([]);
        const onlineUsers  = ref([]);
        const cursors      = ref({});
        const stompClient  = ref(null);

        const projectMembers = ref([]);

        // 提供鎖定狀態與成員清單給所有子節點
        provide('locked', locked);
        provide('projectMembers', projectMembers);

        const roots    = computed(() => buildTree(flatNodes.value));
        const hasNodes = computed(() => flatNodes.value.length > 0);

        function buildTree(nodes) {
          const map = {};
          nodes.forEach(n => map[n.id] = { ...n, children: [], _open: true, _editing: false });
          const result = [];
          nodes.forEach(n => {
            if (n.parentId) map[n.parentId]?.children.push(map[n.id]);
            else result.push(map[n.id]);
          });
          return result;
        }

        async function load() {
          const [nd, pd, md] = await Promise.all([
            api(`/api/projects/${PROJECT_ID}/nodes`),
            api(`/api/projects/${PROJECT_ID}`),
            api(`/api/projects/${PROJECT_ID}/members`)
          ]);
          if (nd.success) flatNodes.value = nd.data;
          if (pd.success) projectName.value = pd.data.name;
          if (md.success) projectMembers.value = md.data;
        }

        async function loadQuickItems() {
          const d = await api('/api/quick-items');
          if (d.success) quickItems.value = d.data;
        }

        async function loadTemplates() {
          const d = await api('/api/templates');
          if (d.success) availableTemplates.value = [...(d.data.system||[]), ...(d.data.custom||[])];
        }

        // 拖曳開始：把 item title 寫入 dataTransfer
        function onDragStart(event, title) {
          event.dataTransfer.setData('text/plain', title);
          event.dataTransfer.effectAllowed = 'copy';
        }

        // 拖曳至 WBS 背景：新增為根節點
        function onDropToRoot(event) {
          treeHighlight.value = false;
          if (locked.value) return;
          const title = event.dataTransfer.getData('text/plain');
          if (!title || !stompClient.value?.connected) return;
          stompClient.value.publish({
            destination: `/app/project/${PROJECT_ID}/node/create`,
            body: JSON.stringify({ title, sortOrder: flatNodes.value.length })
          });
        }

        // 拖曳至某節點：新增為該節點的子節點
        function handleDropItem({ parentId, title }) {
          if (locked.value || !stompClient.value?.connected) return;
          const siblings = flatNodes.value.filter(n => n.parentId === parentId);
          stompClient.value.publish({
            destination: `/app/project/${PROJECT_ID}/node/create`,
            body: JSON.stringify({ title, parentId, sortOrder: siblings.length })
          });
        }

        function connectWs() {
          const sock   = new SockJS('/ws');
          const client = new StompJs.Client({ webSocketFactory: () => sock });
          client.onConnect = () => {
            client.publish({ destination: `/app/project/${PROJECT_ID}/join`, body: '{}' });

            client.subscribe(`/topic/project/${PROJECT_ID}/nodes`, msg => {
              const m = JSON.parse(msg.body);
              if (m.type === 'NODE_UPDATE') {
                const idx = flatNodes.value.findIndex(n => n.id === m.nodeId);
                if (idx !== -1) flatNodes.value[idx] = { ...flatNodes.value[idx], ...m.payload };
              } else if (m.type === 'NODE_CREATE') {
                if (!flatNodes.value.find(n => n.id === m.payload.id)) {
                  flatNodes.value.push(m.payload);
                }
              } else if (m.type === 'NODE_DELETE') {
                flatNodes.value = flatNodes.value.filter(n => n.id !== m.nodeId);
              }
            });

            client.subscribe(`/topic/project/${PROJECT_ID}/cursors`, msg => {
              const c = JSON.parse(msg.body);
              cursors.value = { ...cursors.value, [c.userId]: c };
            });

            client.subscribe(`/app/project/${PROJECT_ID}/presence`, msg => {
              onlineUsers.value = JSON.parse(msg.body);
            });

            client.subscribe(`/topic/project/${PROJECT_ID}/presence`, msg => {
              const p = JSON.parse(msg.body);
              if (p.type === 'JOIN') {
                if (!onlineUsers.value.find(u => u.userId === p.userId)) {
                  onlineUsers.value = [...onlineUsers.value, p];
                }
              } else {
                onlineUsers.value = onlineUsers.value.filter(u => u.userId !== p.userId);
                const nc = { ...cursors.value };
                delete nc[p.userId];
                cursors.value = nc;
              }
            });
          };
          client.activate();
          stompClient.value = client;

          window.addEventListener('beforeunload', () => {
            if (client.connected) {
              client.publish({ destination: `/app/project/${PROJECT_ID}/leave`, body: '{}' });
              client.deactivate();
            }
          });
        }

        function sendCursor(cursorData) {
          if (!stompClient.value?.connected) return;
          stompClient.value.publish({
            destination: `/app/project/${PROJECT_ID}/cursor`,
            body: JSON.stringify(cursorData)
          });
        }

        function addRoot() {
          const base = projectName.value || '大項';
          const rootTitles = flatNodes.value.filter(n => !n.parentId).map(n => n.title);
          let title = base;
          if (rootTitles.includes(title)) {
            let i = 2;
            while (rootTitles.includes(`${base}${i}`)) i++;
            title = `${base}${i}`;
          }
          stompClient.value.publish({
            destination: `/app/project/${PROJECT_ID}/node/create`,
            body: JSON.stringify({ title, sortOrder: flatNodes.value.length })
          });
        }

        async function addChild({ node: parent, depth }) {
          if ((depth || 1) >= 3) return; // 最多三層
          const title = prompt('子節點名稱');
          if (!title) return;
          const siblings = flatNodes.value.filter(n => n.parentId === parent.id);
          stompClient.value.publish({
            destination: `/app/project/${PROJECT_ID}/node/create`,
            body: JSON.stringify({ title, parentId: parent.id, sortOrder: siblings.length })
          });
        }

        async function deleteNode(node) {
          if (!confirm(`確定刪除「${node.title}」及其所有子節點？`)) return;
          stompClient.value.publish({
            destination: `/app/project/${PROJECT_ID}/node/delete`,
            headers: { nodeId: String(node.id) }
          });
        }

        async function updateNode({ node, changes }) {
          stompClient.value.publish({
            destination: `/app/project/${PROJECT_ID}/node/update`,
            headers: { nodeId: String(node.id) },
            body: JSON.stringify(changes)
          });
        }

        function exportCsv() {
          const header = ['編號','標題','負責人','開始日','結束日','狀態'];
          const rows   = [header];
          flatNodes.value.forEach(n => rows.push([
            n.id, n.title, n.owner||'', n.startDate||'', n.endDate||'',
            STATUS_LABEL[n.status]
          ]));
          const csv  = rows.map(r => r.map(v => `"${v}"`).join(',')).join('\n');
          const blob = new Blob(['﻿'+csv], { type: 'text/csv;charset=utf-8' });
          const a    = document.createElement('a');
          a.href     = URL.createObjectURL(blob);
          a.download = `wbs-${PROJECT_ID}.csv`;
          a.click();
        }

        function exportXlsx() {
          const header = ['編號','標題','負責人','開始日','結束日','狀態'];
          const rows   = [header];
          flatNodes.value.forEach(n => rows.push([
            n.id, n.title, n.owner||'', n.startDate||'', n.endDate||'',
            STATUS_LABEL[n.status]
          ]));
          const ws = XLSX.utils.aoa_to_sheet(rows);
          const wb = XLSX.utils.book_new();
          XLSX.utils.book_append_sheet(wb, ws, 'WBS');
          XLSX.writeFile(wb, `wbs-${PROJECT_ID}.xlsx`);
        }

        async function applyTemplate(templateId) {
          try {
            const r = await fetch(`/api/projects/${PROJECT_ID}/nodes/init`, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json', [CSRF_HEADER]: CSRF_TOKEN },
              body: JSON.stringify({ templateId })
            });
            if (r.status === 403) {
              alert('套用失敗：權限不足或登入逾時，請重新整理頁面後再試。');
              return;
            }
            if (!r.ok) {
              alert('套用失敗，請稍後再試。');
              return;
            }
            showTemplateModal.value = false;
            load();
          } catch (e) {
            alert('網路錯誤，請重新整理頁面。');
          }
        }

        async function saveAsTemplate() {
          const name = prompt('模板名稱');
          if (!name) return;
          const d = await api(`/api/templates/from-project/${PROJECT_ID}?name=${encodeURIComponent(name)}`, {
            method: 'POST'
          });
          if (d.success) alert('已儲存為自訂模板：' + d.data.name);
          else alert('儲存失敗：' + (d.message || '未知錯誤'));
        }

        onMounted(() => {
          load();
          loadQuickItems();
          loadTemplates();
          connectWs();
        });

        const isReadOnly = typeof READ_ONLY !== 'undefined' ? READ_ONLY : false;
        return {
          roots, projectName, quickItems, panelCollapsed, locked, treeHighlight,
          showTemplateModal, availableTemplates, isReadOnly,
          addRoot, addChild, deleteNode, updateNode,
          exportCsv, exportXlsx, hasNodes,
          applyTemplate, saveAsTemplate,
          onlineUsers, cursors, sendCursor,
          onDragStart, onDropToRoot, handleDropItem
        };
      }
    }).mount('#app');
