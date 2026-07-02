    const { createApp, ref, computed, onMounted, defineComponent, provide, inject, watch } = Vue;
    const STATUS_LABEL = { NOT_STARTED:'未開始', IN_PROGRESS:'進行中', DONE:'已完成' };
    const STATUS_CYCLE = { NOT_STARTED:'IN_PROGRESS', IN_PROGRESS:'DONE', DONE:'NOT_STARTED' };
    function syncParentStatus(nodes) {
      for (const n of nodes) {
        if (!n.children?.length) continue;
        syncParentStatus(n.children);
        const s = n.children.map(c => c.status);
        n.status = s.every(x => x === 'DONE') ? 'DONE'
                 : s.some(x => x === 'IN_PROGRESS' || x === 'DONE') ? 'IN_PROGRESS'
                 : 'NOT_STARTED';
      }
    }

    function numbering(nodes, prefix = '') {
      const map = {};
      nodes.forEach((n, i) => {
        const num = prefix ? `${prefix}.${i + 1}` : `${i + 1}`;
        map[n.id] = num;
        if (n.children?.length) Object.assign(map, numbering(n.children, num));
      });
      return map;
    }

    const h = () => ({ 'Content-Type':'application/json', [CSRF_HEADER]:CSRF_TOKEN });
    const api = (url, opt={}) => fetch(url, { headers: h(), ...opt }).then(r => r.json());

    const WbsNodeComp = defineComponent({
      name: 'wbs-node',
      props: ['node', 'cursors', 'locked', 'depth', 'deleteMode'],
      emits: ['add-child','delete','update','cursor-move','drop-item','node-drop'],
      template: `
        <div class="wbs-node-wrap">
          <div class="wbs-node"
               :class="['status-'+node.status.toLowerCase(),
                 {'drag-over': isDragOver && !locked,
                  'is-delete-mode': deleteMode && !locked,
                  'drop-before': dropPosition==='before',
                  'drop-child':  dropPosition==='child',
                  'drop-after':  dropPosition==='after'}]"
               draggable="true"
               @dragstart="!locked && onDragStart($event)"
               @mouseenter="onMouseEnter(node.id)" @mouseleave="onMouseLeave()"
               @dragover="onDragOver($event)"
               @dragleave="onDragLeave()"
               @drop.stop="onNodeDrop($event)">
            <span class="wbs-toggle" @click="node._open=!node._open">
              {{ node.children?.length ? (node._open?'▼':'▶') : '　' }}
            </span>
            <span class="wbs-status-badge" @click="!locked && cycleStatus()">
              {{ STATUS_LABEL[node.status] }}
            </span>
            <span class="wbs-num">{{ nodeNumbers[node.id] }}</span>
            <span class="wbs-title" v-if="!node._editing"
                  @dblclick="!locked && startEdit()">{{ node.title }}</span>
            <input class="wbs-title-input" v-else v-model="editTitle"
                   @blur="commitEdit" @keyup.enter="commitEdit" @keyup.escape="node._editing=false"
                   ref="titleInput" />
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
            <input v-if="(depth||1) >= 3" class="wbs-field-input" type="text" v-model="editNotes"
                   placeholder="備註" :readonly="locked"
                   @blur="!locked && commitField('notes', editNotes)" />
            <div class="wbs-node-actions" v-if="!locked && !deleteMode">
              <button v-if="(depth||1) < 3" @click="$emit('add-child', {node, depth: depth||1})">+ 子</button>
            </div>
            <div v-if="deleteMode && !locked" class="wbs-delete-overlay"
                 @click.stop="$emit('delete', node)"></div>
            <div class="cursor-indicators">
              <span v-for="c in nodeCursors(node.id)" :key="c.userId"
                    class="cursor-dot" :style="{background:c.color}"
                    :title="c.displayName + (c.editingNodeId===node.id?' 正在編輯':' 正在查看')">
              </span>
            </div>
          </div>
          <div class="wbs-children" v-if="node._open && node.children?.length">
            <wbs-node v-for="c in node.children" :key="c.id" :node="c" :cursors="cursors" :locked="locked" :depth="(depth||1)+1" :delete-mode="deleteMode"
                      @add-child="$emit('add-child',$event)"
                      @delete="$emit('delete',$event)"
                      @update="$emit('update',$event)"
                      @cursor-move="$emit('cursor-move',$event)"
                      @drop-item="$emit('drop-item',$event)"
                      @node-drop="$emit('node-drop',$event)"></wbs-node>
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
        const dropPosition   = ref(null); // 'before' | 'child' | 'after' | null
        const projectMembers = inject('projectMembers', ref([]));
        const nodeNumbers = inject('nodeNumbers', ref({}));

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
          if (props.node.children?.length) return;
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
        function onDragStart(event) {
          event.dataTransfer.setData('application/json',
            JSON.stringify({ nodeId: props.node.id, depth: props.depth || 1 }));
          event.dataTransfer.effectAllowed = 'move';
        }

        function onDragOver(event) {
          if (props.locked) return;
          // 快速子項拖曳（text/plain）：走舊邏輯
          if (!event.dataTransfer.types.includes('application/json')) {
            isDragOver.value = (props.depth || 1) < 3;
            if (isDragOver.value) event.preventDefault();
            return;
          }
          event.preventDefault();
          const rect = event.currentTarget.getBoundingClientRect();
          const pct  = (event.clientY - rect.top) / rect.height;
          const hasChildren = props.node.children?.length > 0;
          if (pct < 0.3) dropPosition.value = 'before';
          else if (pct > 0.7) dropPosition.value = 'after';
          else dropPosition.value = (!hasChildren && (props.depth || 1) < 3) ? 'child'
                                   : (pct < 0.5 ? 'before' : 'after');
        }

        function onDragLeave() {
          isDragOver.value = false;
          dropPosition.value = null;
        }

        // 同時處理 WBS 節點拖曳（application/json）與快速子項拖曳（text/plain）
        function onNodeDrop(event) {
          isDragOver.value = false;
          if (props.locked) return;

          const jsonRaw = event.dataTransfer.getData('application/json');
          if (jsonRaw) {
            const { nodeId } = JSON.parse(jsonRaw);
            const pos = dropPosition.value;
            dropPosition.value = null;
            if (nodeId !== props.node.id && pos) {
              emit('node-drop', { draggedId: nodeId, targetId: props.node.id, position: pos });
            }
            return;
          }

          // 快速子項拖曳
          if ((props.depth || 1) >= 3) return;
          const title = event.dataTransfer.getData('text/plain');
          if (!title) return;
          emit('drop-item', { parentId: props.node.id, title });
        }

        function fmtDate(d) {
          if (!d) return '';
          return d.replace(/-/g, '/');
        }

        return { editTitle, titleInput, editOwner, editStartDate, editEndDate, editNotes,
                 isDragOver, dropPosition, projectMembers, nodeNumbers,
                 startEdit, commitEdit, cycleStatus, commitField,
                 onMouseEnter, onMouseLeave, nodeCursors,
                 onDragStart, onDragOver, onDragLeave, onNodeDrop, STATUS_LABEL, fmtDate };
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
        const stompClient    = ref(null);
        const deleteMode     = ref(false);
        const pendingChanges = ref({});
        const hasPending     = computed(() => Object.keys(pendingChanges.value).length > 0);
        const searchQuery    = ref('');
        const statusFilter   = ref('ALL');

        const projectMembers = ref([]);

        // 提供鎖定狀態與成員清單給所有子節點
        provide('locked', locked);
        provide('projectMembers', projectMembers);

        const roots = computed(() => {
          let nodes = flatNodes.value;
          const q  = searchQuery.value.trim().toLowerCase();
          const sf = statusFilter.value;
          if (q || sf !== 'ALL') {
            const map = {};
            nodes.forEach(n => map[n.id] = n);
            const matchIds = new Set(
              nodes.filter(n =>
                (!q || n.title.toLowerCase().includes(q)) &&
                (sf === 'ALL' || n.status === sf)
              ).map(n => n.id)
            );
            const vis = new Set(matchIds);
            matchIds.forEach(id => {
              let c = map[id];
              while (c && c.parentId) { vis.add(c.parentId); c = map[c.parentId]; }
            });
            nodes = nodes.filter(n => vis.has(n.id));
          }
          const tree = buildTree(nodes);
          syncParentStatus(tree);
          return tree;
        });
        const hasNodes = computed(() => flatNodes.value.length > 0);

        const nodeNumbers = computed(() => numbering(roots.value));
        provide('nodeNumbers', nodeNumbers);

        const stats = computed(() => {
          const parentIdSet = new Set(flatNodes.value.map(n => n.parentId).filter(Boolean));
          const leaves = flatNodes.value.filter(n => !parentIdSet.has(n.id));
          const total = leaves.length;
          const done = leaves.filter(n => n.status === 'DONE').length;
          const inProgress = leaves.filter(n => n.status === 'IN_PROGRESS').length;
          const notStarted = total - done - inProgress;
          const pct = total === 0 ? 0 : Math.round((done / total) * 100);
          return { total, done, inProgress, notStarted, pct };
        });

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

        function toggleAll(nodes, open) {
          nodes.forEach(n => { n._open = open; if (n.children?.length) toggleAll(n.children, open); });
        }
        function expandAll()  { toggleAll(roots.value, true);  }
        function collapseAll(){ toggleAll(roots.value, false); }

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

        function isDescendant(ancestorId, targetId) {
          let node = flatNodes.value.find(n => n.id === targetId);
          while (node?.parentId != null) {
            if (node.parentId === ancestorId) return true;
            node = flatNodes.value.find(n => n.id === node.parentId);
          }
          return false;
        }

        function handleNodeDrop({ draggedId, targetId, position }) {
          if (locked.value || draggedId === targetId) return;
          if (isDescendant(draggedId, targetId)) return;

          const dragNode   = flatNodes.value.find(n => n.id === draggedId);
          const targetNode = flatNodes.value.find(n => n.id === targetId);
          if (!dragNode || !targetNode) return;

          const dragHasChildren = flatNodes.value.some(n => n.parentId === draggedId);
          const newParentId = position === 'child' ? targetId : (targetNode.parentId ?? null);

          // L1 有子節點時禁止成為其他節點的子節點（避免 L3）
          if (newParentId !== null && dragHasChildren) return;

          // 重組節點列表
          let nodes = flatNodes.value.filter(n => n.id !== draggedId);
          const updatedDrag = { ...dragNode, parentId: newParentId };

          if (position === 'child') {
            nodes = [...nodes, updatedDrag];
          } else {
            const idx = nodes.findIndex(n => n.id === targetId);
            const at  = position === 'before' ? idx : idx + 1;
            nodes = [...nodes.slice(0, at), updatedDrag, ...nodes.slice(at)];
          }

          // 計算受影響的父節點群組，重新分配 sortOrder
          const affectedParents = new Set([dragNode.parentId ?? null, newParentId]);
          const updates = [];
          affectedParents.forEach(pid => {
            nodes.filter(n => (n.parentId ?? null) === pid)
              .forEach((n, i) => {
                const orig = flatNodes.value.find(o => o.id === n.id);
                if (i !== orig?.sortOrder || (n.parentId ?? null) !== (orig?.parentId ?? null)) {
                  updates.push({ nodeId: n.id, parentId: n.parentId ?? null, sortOrder: i });
                }
              });
          });

          // 本地即時更新
          flatNodes.value = nodes.map(n => {
            const u = updates.find(u => u.nodeId === n.id);
            return u ? { ...n, parentId: u.parentId, sortOrder: u.sortOrder } : n;
          });

          if (stompClient.value?.connected && updates.length) {
            stompClient.value.publish({
              destination: `/app/project/${PROJECT_ID}/node/reorder`,
              body: JSON.stringify(updates)
            });
          }
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
              } else if (m.type === 'NODE_REORDER') {
                // 批次更新排序位置，避免重新拉取整棵樹（Task 5）
                flatNodes.value = flatNodes.value.map(n => {
                  const u = m.payload.find(u => u.nodeId === n.id);
                  return u ? { ...n, parentId: u.parentId, sortOrder: u.sortOrder } : n;
                });
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
          const updated = { ...pendingChanges.value };
          delete updated[node.id];
          pendingChanges.value = updated;
          stompClient.value.publish({
            destination: `/app/project/${PROJECT_ID}/node/delete`,
            headers: { nodeId: String(node.id) }
          });
        }

        function updateNode({ node, changes }) {
          const idx = flatNodes.value.findIndex(n => n.id === node.id);
          if (idx !== -1) flatNodes.value[idx] = { ...flatNodes.value[idx], ...changes };
          pendingChanges.value = {
            ...pendingChanges.value,
            [node.id]: { ...(pendingChanges.value[node.id] || {}), ...changes }
          };
        }

        function saveAll() {
          if (!hasPending.value || !stompClient.value?.connected) return;
          for (const [nodeId, changes] of Object.entries(pendingChanges.value)) {
            stompClient.value.publish({
              destination: `/app/project/${PROJECT_ID}/node/update`,
              headers: { nodeId },
              body: JSON.stringify(changes)
            });
          }
          pendingChanges.value = {};
        }

        function exportCsv() {
          // 改由後端產生，含層級編號與 BOM
          window.location.href = `/api/projects/${PROJECT_ID}/nodes/export.csv`;
        }

        function exportXlsx() {
          // 改由後端 Apache POI 產生，含層級編號與狀態底色
          window.location.href = `/api/projects/${PROJECT_ID}/nodes/export.xlsx`;
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

        watch(locked, (isLocked) => {
          if (isLocked && hasPending.value) saveAll();
        });

        const isReadOnly = typeof READ_ONLY !== 'undefined' ? READ_ONLY : false;
        return {
          roots, projectName, quickItems, panelCollapsed, locked, treeHighlight,
          showTemplateModal, availableTemplates, isReadOnly,
          addRoot, addChild, deleteNode, updateNode,
          exportCsv, exportXlsx, hasNodes, stats,
          applyTemplate, saveAsTemplate,
          onlineUsers, cursors, sendCursor,
          onDragStart, onDropToRoot, handleDropItem, handleNodeDrop,
          deleteMode, hasPending, saveAll,
          searchQuery, statusFilter, expandAll, collapseAll
        };
      }
    }).mount('#app');
