const { createApp, ref, computed, onMounted } = Vue;

const api = (url, opt = {}) =>
  fetch(url, { headers: { 'Content-Type': 'application/json', [CSRF_HEADER]: CSRF_TOKEN }, ...opt })
    .then(r => r.json());

createApp({
  setup() {
    const nodes       = ref([]);
    const templateName = ref(document.title.replace('編輯模板：', ''));
    const saving      = ref(false);
    const error       = ref('');

    // 拖曳狀態
    const draggingId  = ref(null);
    const draggingParentId = ref(null);
    const dragOverId  = ref(null);

    // 臨時 ID 計數器（負數代表尚未持久化的新節點）
    let tempId = -1;

    async function load() {
      const d = await api(`/api/templates/${TEMPLATE_ID}`);
      if (d.success) {
        templateName.value = d.data.name;
        nodes.value = d.data.nodes.map(n => ({ ...n }));
      } else {
        error.value = '載入失敗';
      }
    }

    const l1Nodes = computed(() =>
      nodes.value.filter(n => !n.parentId).sort((a, b) => a.sortOrder - b.sortOrder)
    );

    function childrenOf(parentId) {
      return nodes.value.filter(n => n.parentId === parentId).sort((a, b) => a.sortOrder - b.sortOrder);
    }

    function addNode(parentId, depth) {
      const siblings = nodes.value.filter(n => n.parentId === parentId);
      const sortOrder = siblings.length;
      nodes.value.push({ id: tempId--, parentId: parentId ?? null, title: '', notes: '', sortOrder, _new: true });
    }

    function removeNode(node) {
      if (node.id > 0 && !confirm(`確定刪除「${node.title || '（空白節點）'}」及其所有子節點？`)) return;
      removeRecursive(node.id);
    }

    function removeRecursive(nodeId) {
      nodes.value.filter(n => n.parentId === nodeId).forEach(c => removeRecursive(c.id));
      const idx = nodes.value.findIndex(n => n.id === nodeId);
      if (idx !== -1) nodes.value.splice(idx, 1);
    }

    // --- 拖曳排序 ---
    function onDragStart(evt, node, parentId) {
      draggingId.value = node.id;
      draggingParentId.value = parentId;
      evt.dataTransfer.effectAllowed = 'move';
    }

    function onDragOver(evt, overNodeId) {
      // 只允許同層拖曳
      const over = nodes.value.find(n => n.id === overNodeId);
      const dragging = nodes.value.find(n => n.id === draggingId.value);
      if (over && dragging && over.parentId === dragging.parentId) {
        dragOverId.value = overNodeId;
      }
    }

    function onDrop(evt, targetNode, parentId) {
      evt.preventDefault();
      dragOverId.value = null;
      const srcId = draggingId.value;
      if (!srcId || srcId === targetNode.id) { draggingId.value = null; return; }

      const src = nodes.value.find(n => n.id === srcId);
      if (!src || src.parentId !== targetNode.parentId) { draggingId.value = null; return; }

      // 重新排序同層節點
      const siblings = nodes.value
        .filter(n => n.parentId === src.parentId)
        .sort((a, b) => a.sortOrder - b.sortOrder);

      const fromIdx = siblings.findIndex(n => n.id === srcId);
      const toIdx   = siblings.findIndex(n => n.id === targetNode.id);
      siblings.splice(toIdx, 0, siblings.splice(fromIdx, 1)[0]);
      siblings.forEach((n, i) => { n.sortOrder = i; });

      draggingId.value = null;
    }

    // --- 儲存：新節點 POST，已存在節點 PUT，刪除的節點呼叫 DELETE ---
    async function save() {
      saving.value = true;
      error.value  = '';
      try {
        // 找出已從 nodes 消失的舊節點（需要刪除）
        const currentIds = new Set(nodes.value.filter(n => n.id > 0).map(n => n.id));
        const d = await api(`/api/templates/${TEMPLATE_ID}`);
        const serverIds = new Set((d.data?.nodes || []).map(n => n.id));
        for (const sid of serverIds) {
          if (!currentIds.has(sid)) {
            await api(`/api/templates/${TEMPLATE_ID}/nodes/${sid}`, { method: 'DELETE' });
          }
        }

        // 新節點：POST（需要按 sortOrder 順序，確保 parentId 已存在）
        const idMap = {};
        const ordered = [...nodes.value].sort((a, b) => {
          const depthA = depth(a); const depthB = depth(b);
          return depthA !== depthB ? depthA - depthB : a.sortOrder - b.sortOrder;
        });

        for (const node of ordered) {
          const realParentId = node.parentId && node.parentId < 0 ? idMap[node.parentId] : node.parentId;
          if (node._new || node.id < 0) {
            const r = await api(`/api/templates/${TEMPLATE_ID}/nodes`, {
              method: 'POST',
              body: JSON.stringify({ title: node.title || '（未命名）', parentId: realParentId ?? null, sortOrder: node.sortOrder, notes: node.notes || null })
            });
            if (r.success) idMap[node.id] = r.data.id;
          } else {
            await api(`/api/templates/${TEMPLATE_ID}/nodes/${node.id}`, {
              method: 'PUT',
              body: JSON.stringify({ title: node.title || '（未命名）', notes: node.notes || null })
            });
          }
        }

        // 批次送出排序
        const reorderItems = nodes.value.filter(n => n.id > 0).map(n => ({ id: n.id, sortOrder: n.sortOrder }));
        if (reorderItems.length) {
          await api(`/api/templates/${TEMPLATE_ID}/nodes/reorder`, {
            method: 'PATCH',
            body: JSON.stringify(reorderItems)
          });
        }

        await load();
      } catch (e) {
        error.value = '儲存失敗，請重試。';
      } finally {
        saving.value = false;
      }
    }

    function depth(node) {
      if (!node.parentId) return 1;
      const parent = nodes.value.find(n => n.id === node.parentId);
      return parent ? depth(parent) + 1 : 2;
    }

    onMounted(load);

    return {
      nodes, templateName, saving, error,
      l1Nodes, childrenOf,
      addNode, removeNode, save,
      draggingId, dragOverId,
      onDragStart, onDragOver, onDrop
    };
  }
}).mount('#app');
