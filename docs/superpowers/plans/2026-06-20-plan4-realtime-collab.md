# Spring-WbsScaff Plan 4: Real-time Collaboration

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 WBS 編輯器加入即時多人協作：節點變更即時同步、其他人游標/hover 位置可見、在線人員列表顯示。

**Architecture:** Spring WebSocket + STOMP 廣播。Client 透過 SockJS 連線，每個專案有三個 topic：`/topic/project/{id}/nodes`（節點變更）、`/topic/project/{id}/cursors`（游標位置）、`/topic/project/{id}/presence`（上線/離線）。WebSocket 握手時驗證 Spring Session。

**Tech Stack:** 承接 Plan 1-3，新增 Spring WebSocket + STOMP、SockJS（本地靜態）、stomp.js（本地靜態）。

## Global Constraints

- 承接 Plan 1、Plan 2、Plan 3 所有 Global Constraints
- WebSocket 握手驗證 Spring Session Cookie，未登入拒絕連線
- SockJS：`src/main/resources/static/js/sockjs.min.js`（離線可用）
- STOMP client：`src/main/resources/static/js/stomp.min.js`（離線可用）
- 衝突策略：Last Write Wins（最後寫入者獲勝），無版本鎖定
- 用戶顏色：依 `userId % 8` 從固定色盤取色，Session 內固定

---

### Task 1: 下載靜態資源 + WebSocket Config

**Files:**
- Create: `src/main/resources/static/js/sockjs.min.js`
- Create: `src/main/resources/static/js/stomp.min.js`
- Create: `src/main/java/com/wbsscaff/collab/WebSocketConfig.java`
- Create: `src/test/java/com/wbsscaff/collab/WebSocketConfigTest.java`

**Interfaces:**
- Produces: WebSocket 端點 `/ws`（SockJS），STOMP broker `/topic`，application prefix `/app`

- [ ] **Step 1: 下載 SockJS 與 STOMP 靜態檔案**

```bash
curl -L "https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js" \
  -o src/main/resources/static/js/sockjs.min.js
curl -L "https://cdn.jsdelivr.net/npm/@stomp/stompjs@7/bundles/stomp.umd.min.js" \
  -o src/main/resources/static/js/stomp.min.js
```

- [ ] **Step 2: 寫 failing test**

```java
package com.wbsscaff.collab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebSocketConfigTest {

    @Autowired MockMvc mockMvc;

    @Test
    void wsEndpoint_exists() throws Exception {
        mockMvc.perform(get("/ws/info"))
            .andExpect(status().isOk());
    }
}
```

- [ ] **Step 3: Run failing test**

```bash
mvn test -Dtest=WebSocketConfigTest -q
```
Expected: FAIL — `/ws/info` 回傳 404。

- [ ] **Step 4: 建立 WebSocketConfig.java**

```java
package com.wbsscaff.collab;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
}
```

- [ ] **Step 5: Run test — 確認通過**

```bash
mvn test -Dtest=WebSocketConfigTest -q
```
Expected: BUILD SUCCESS。

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/wbsscaff/collab/WebSocketConfig.java \
        src/main/resources/static/js/sockjs.min.js \
        src/main/resources/static/js/stomp.min.js \
        src/test/java/com/wbsscaff/collab/WebSocketConfigTest.java
git commit -m "feat: configure Spring WebSocket STOMP with SockJS endpoint"
```

---

### Task 2: Collab Messages + CollabService

**Files:**
- Create: `src/main/java/com/wbsscaff/collab/NodeChangeMessage.java`
- Create: `src/main/java/com/wbsscaff/collab/CursorMessage.java`
- Create: `src/main/java/com/wbsscaff/collab/PresenceMessage.java`
- Create: `src/main/java/com/wbsscaff/collab/CollabService.java`
- Create: `src/test/java/com/wbsscaff/collab/CollabServiceTest.java`

**Interfaces:**
- Produces:
  - `CollabService.userColor(Long userId): String` — 固定色盤依 userId 取色
  - `CollabService.join(Long projectId, Long userId, String displayName): void`
  - `CollabService.leave(Long projectId, Long userId): void`
  - `CollabService.getOnlineUsers(Long projectId): List<PresenceMessage>`

- [ ] **Step 1: 建立 NodeChangeMessage.java**

```java
package com.wbsscaff.collab;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NodeChangeMessage {
    public enum Type { NODE_UPDATE, NODE_CREATE, NODE_DELETE }

    private Type type;
    private Long nodeId;
    private Object payload;
    private UserInfo operator;
    private LocalDateTime timestamp = LocalDateTime.now();

    @Data
    public static class UserInfo {
        private Long userId;
        private String displayName;
        private String color;
    }
}
```

- [ ] **Step 2: 建立 CursorMessage.java**

```java
package com.wbsscaff.collab;

import lombok.Data;

@Data
public class CursorMessage {
    private Long userId;
    private String displayName;
    private String color;
    private Long hoveringNodeId;
    private Long editingNodeId;
}
```

- [ ] **Step 3: 建立 PresenceMessage.java**

```java
package com.wbsscaff.collab;

import lombok.Data;

@Data
public class PresenceMessage {
    public enum Type { JOIN, LEAVE }

    private Type type;
    private Long userId;
    private String displayName;
    private String color;
}
```

- [ ] **Step 4: 寫 failing CollabService test**

```java
package com.wbsscaff.collab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CollabServiceTest {

    @Autowired CollabService collabService;

    @Test
    void userColor_deterministicByUserId() {
        String c1 = collabService.userColor(1L);
        String c2 = collabService.userColor(1L);
        assertThat(c1).isEqualTo(c2);
        assertThat(c1).startsWith("#");
    }

    @Test
    void join_then_leave_updatesOnlineList() {
        Long projectId = 999L;
        collabService.join(projectId, 1L, "Alice");
        collabService.join(projectId, 2L, "Bob");
        assertThat(collabService.getOnlineUsers(projectId)).hasSize(2);

        collabService.leave(projectId, 1L);
        assertThat(collabService.getOnlineUsers(projectId)).hasSize(1);
    }
}
```

- [ ] **Step 5: Run failing test**

```bash
mvn test -Dtest=CollabServiceTest -q
```
Expected: FAIL — `CollabService` 不存在。

- [ ] **Step 6: 建立 CollabService.java**

```java
package com.wbsscaff.collab;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CollabService {

    private static final String[] COLORS = {
        "#4A90D9","#E74C3C","#27AE60","#F39C12",
        "#9B59B6","#1ABC9C","#E67E22","#2980B9"
    };

    private final Map<Long, Map<Long, PresenceMessage>> sessions = new ConcurrentHashMap<>();

    public String userColor(Long userId) {
        return COLORS[(int)(userId % COLORS.length)];
    }

    public void join(Long projectId, Long userId, String displayName) {
        sessions.computeIfAbsent(projectId, k -> new ConcurrentHashMap<>());
        PresenceMessage msg = new PresenceMessage();
        msg.setType(PresenceMessage.Type.JOIN);
        msg.setUserId(userId);
        msg.setDisplayName(displayName);
        msg.setColor(userColor(userId));
        sessions.get(projectId).put(userId, msg);
    }

    public void leave(Long projectId, Long userId) {
        if (sessions.containsKey(projectId)) {
            sessions.get(projectId).remove(userId);
        }
    }

    public List<PresenceMessage> getOnlineUsers(Long projectId) {
        return sessions.getOrDefault(projectId, Collections.emptyMap())
            .values().stream().toList();
    }
}
```

- [ ] **Step 7: Run test — 確認通過**

```bash
mvn test -Dtest=CollabServiceTest -q
```
Expected: BUILD SUCCESS。

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/wbsscaff/collab/ \
        src/test/java/com/wbsscaff/collab/CollabServiceTest.java
git commit -m "feat: add collab messages and CollabService for presence tracking"
```

---

### Task 3: CollabController（STOMP Handlers）

**Files:**
- Create: `src/main/java/com/wbsscaff/collab/CollabController.java`

**Interfaces:**
- Consumes: STOMP messages from client via `/app/project/{id}/**`
- Produces: Broadcast to `/topic/project/{id}/nodes`, `/cursors`, `/presence`

- [ ] **Step 1: 建立 CollabController.java**

```java
package com.wbsscaff.collab;

import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import com.wbsscaff.wbs.WbsDto;
import com.wbsscaff.wbs.WbsService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CollabController {

    private final SimpMessagingTemplate broker;
    private final CollabService collabService;
    private final WbsService wbsService;
    private final UserRepository userRepository;

    @SubscribeMapping("/project/{projectId}/presence")
    public List<PresenceMessage> onSubscribePresence(
            @DestinationVariable Long projectId, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        collabService.join(projectId, user.getId(), user.getDisplayName());

        PresenceMessage joinMsg = new PresenceMessage();
        joinMsg.setType(PresenceMessage.Type.JOIN);
        joinMsg.setUserId(user.getId());
        joinMsg.setDisplayName(user.getDisplayName());
        joinMsg.setColor(collabService.userColor(user.getId()));
        broker.convertAndSend("/topic/project/" + projectId + "/presence", joinMsg);

        return collabService.getOnlineUsers(projectId);
    }

    @MessageMapping("/project/{projectId}/node/update")
    public void onNodeUpdate(@DestinationVariable Long projectId,
            @Payload WbsDto.UpdateRequest req,
            @Header("nodeId") Long nodeId,
            Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        WbsDto.Response updated = WbsDto.Response.from(wbsService.updateNode(nodeId, req));

        NodeChangeMessage msg = new NodeChangeMessage();
        msg.setType(NodeChangeMessage.Type.NODE_UPDATE);
        msg.setNodeId(nodeId);
        msg.setPayload(updated);
        NodeChangeMessage.UserInfo ui = new NodeChangeMessage.UserInfo();
        ui.setUserId(user.getId());
        ui.setDisplayName(user.getDisplayName());
        ui.setColor(collabService.userColor(user.getId()));
        msg.setOperator(ui);

        broker.convertAndSend("/topic/project/" + projectId + "/nodes", msg);
    }

    @MessageMapping("/project/{projectId}/node/create")
    public void onNodeCreate(@DestinationVariable Long projectId,
            @Payload WbsDto.CreateRequest req, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        WbsDto.Response created = WbsDto.Response.from(wbsService.createNode(projectId, req));

        NodeChangeMessage msg = new NodeChangeMessage();
        msg.setType(NodeChangeMessage.Type.NODE_CREATE);
        msg.setNodeId(created.getId());
        msg.setPayload(created);
        NodeChangeMessage.UserInfo ui = new NodeChangeMessage.UserInfo();
        ui.setUserId(user.getId()); ui.setDisplayName(user.getDisplayName());
        ui.setColor(collabService.userColor(user.getId()));
        msg.setOperator(ui);

        broker.convertAndSend("/topic/project/" + projectId + "/nodes", msg);
    }

    @MessageMapping("/project/{projectId}/node/delete")
    public void onNodeDelete(@DestinationVariable Long projectId,
            @Header("nodeId") Long nodeId, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        wbsService.deleteNode(nodeId);

        NodeChangeMessage msg = new NodeChangeMessage();
        msg.setType(NodeChangeMessage.Type.NODE_DELETE);
        msg.setNodeId(nodeId);
        NodeChangeMessage.UserInfo ui = new NodeChangeMessage.UserInfo();
        ui.setUserId(user.getId()); ui.setDisplayName(user.getDisplayName());
        ui.setColor(collabService.userColor(user.getId()));
        msg.setOperator(ui);

        broker.convertAndSend("/topic/project/" + projectId + "/nodes", msg);
    }

    @MessageMapping("/project/{projectId}/cursor")
    public void onCursor(@DestinationVariable Long projectId,
            @Payload CursorMessage cursor, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        cursor.setUserId(user.getId());
        cursor.setDisplayName(user.getDisplayName());
        cursor.setColor(collabService.userColor(user.getId()));
        broker.convertAndSend("/topic/project/" + projectId + "/cursors", cursor);
    }
}
```

- [ ] **Step 2: 設定 WebSocket 在 disconnect 時廣播 LEAVE**

在 `WebSocketConfig.java` 新增 Event Listener：

```java
@Component
@RequiredArgsConstructor
class SessionDisconnectListener implements
        org.springframework.context.ApplicationListener<
        org.springframework.web.socket.messaging.SessionDisconnectEvent> {

    private final CollabService collabService;
    private final SimpMessagingTemplate broker;
    private final UserRepository userRepository;

    @Override
    public void onApplicationEvent(
            org.springframework.web.socket.messaging.SessionDisconnectEvent event) {
        var headerAccessor =
            org.springframework.messaging.simp.stomp.StompHeaderAccessor.wrap(event.getMessage());
        if (headerAccessor.getUser() == null) return;
        String email = headerAccessor.getUser().getName();
        userRepository.findByEmail(email).ifPresent(user -> {
            collabService.sessions.forEach((projectId, users) -> {
                if (users.containsKey(user.getId())) {
                    collabService.leave(projectId, user.getId());
                    PresenceMessage leave = new PresenceMessage();
                    leave.setType(PresenceMessage.Type.LEAVE);
                    leave.setUserId(user.getId());
                    leave.setDisplayName(user.getDisplayName());
                    leave.setColor(collabService.userColor(user.getId()));
                    broker.convertAndSend("/topic/project/" + projectId + "/presence", leave);
                }
            });
        });
    }
}
```

Note: `CollabService.sessions` 需改為 `public`（`public final Map<Long, Map<Long, PresenceMessage>> sessions`）。

- [ ] **Step 3: Run all tests**

```bash
mvn test -q
```
Expected: BUILD SUCCESS。

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/wbsscaff/collab/CollabController.java \
        src/main/java/com/wbsscaff/collab/WebSocketConfig.java
git commit -m "feat: add STOMP CollabController for node sync, cursor, and presence"
```

---

### Task 4: 前端 WebSocket 整合（project/detail.html）

**Files:**
- Modify: `src/main/resources/templates/project/detail.html`

**Interfaces:**
- Consumes: STOMP topics `/topic/project/{id}/nodes`, `/cursors`, `/presence`
- Produces: 即時節點同步、游標顯示、在線人員列表

- [ ] **Step 1: 在 detail.html head 引入 SockJS + STOMP**

在 `</head>` 前新增：

```html
<script th:src="@{/js/sockjs.min.js}"></script>
<script th:src="@{/js/stomp.min.js}"></script>
```

- [ ] **Step 2: 在 detail.html body 新增 presence bar HTML**

在 `.wbs-toolbar` div 內的 `.wbs-actions` 前新增：

```html
<div class="presence-bar">
  <div class="presence-avatar" v-for="u in onlineUsers" :key="u.userId"
       :style="{background: u.color}" :title="u.displayName">
    {{ u.displayName[0] }}
  </div>
</div>
```

- [ ] **Step 3: 在 WbsNodeComp template 新增游標指示器**

在 `.wbs-node` div 末尾新增：

```html
<div class="cursor-indicators">
  <span v-for="c in nodeCursors(node.id)" :key="c.userId"
        class="cursor-dot" :style="{background:c.color}"
        :title="c.displayName + (c.editingNodeId===node.id?' 正在編輯':' 正在查看')">
  </span>
</div>
```

並在 WbsNodeComp 的 emits 中增加游標事件，在 setup 中：

```js
function onMouseEnter(nodeId) {
  emit('cursor-move', { hoveringNodeId: nodeId, editingNodeId: null });
}
function onMouseLeave() {
  emit('cursor-move', { hoveringNodeId: null, editingNodeId: null });
}
```

在 template `.wbs-node` 上加 `@mouseenter="onMouseEnter(node.id)" @mouseleave="onMouseLeave()"`

- [ ] **Step 4: 在主 App setup() 加入 WebSocket 邏輯**

在 `createApp` setup 中加入（放在 `onMounted` 前）：

```js
const onlineUsers = ref([]);
const cursors = ref({});  // userId -> CursorMessage
const stompClient = ref(null);

function nodeCursors(nodeId) {
  return Object.values(cursors.value).filter(
    c => c.hoveringNodeId === nodeId || c.editingNodeId === nodeId
  );
}

function connectWs() {
  const sock = new SockJS('/ws');
  const client = new StompJs.Client({ webSocketFactory: () => sock });
  client.onConnect = () => {
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
}

function sendCursor(cursorData) {
  if (!stompClient.value?.connected) return;
  stompClient.value.publish({
    destination: `/app/project/${PROJECT_ID}/cursor`,
    body: JSON.stringify(cursorData)
  });
}
```

- [ ] **Step 5: 改寫 addRoot, addChild, updateNode, deleteNode 改為透過 STOMP 送出**

將原本直接呼叫 REST API 的 `addRoot`、`addChild`、`updateNode`、`deleteNode` 改為透過 STOMP 發送，讓 CollabController 廣播後所有人同步：

```js
async function addRoot() {
  const title = prompt('根節點名稱');
  if (!title) return;
  stompClient.value.publish({
    destination: `/app/project/${PROJECT_ID}/node/create`,
    body: JSON.stringify({ title, sortOrder: flatNodes.value.length })
  });
}

async function addChild(parent) {
  const title = prompt('子節點名稱');
  if (!title) return;
  const siblings = flatNodes.value.filter(n => n.parentId === parent.id);
  stompClient.value.publish({
    destination: `/app/project/${PROJECT_ID}/node/create`,
    body: JSON.stringify({ title, parentId: parent.id, sortOrder: siblings.length })
  });
}

async function updateNode({ node, changes }) {
  stompClient.value.publish({
    destination: `/app/project/${PROJECT_ID}/node/update`,
    headers: { nodeId: String(node.id) },
    body: JSON.stringify(changes)
  });
}

async function deleteNode(node) {
  if (!confirm(`確定刪除「${node.title}」及其所有子節點？`)) return;
  stompClient.value.publish({
    destination: `/app/project/${PROJECT_ID}/node/delete`,
    headers: { nodeId: String(node.id) }
  });
}
```

- [ ] **Step 6: 在 onMounted 中呼叫 connectWs**

```js
onMounted(() => {
  load();
  connectWs();
});
```

- [ ] **Step 7: 在 wbs-node component 中 emit cursor-move 並在主 App 接收後 sendCursor**

在主 App template 的 `wbs-node` 元件上加：

```html
<wbs-node v-for="n in roots" :key="n.id" :node="n"
          @add-child="addChild" @delete="deleteNode"
          @update="updateNode"
          @cursor-move="sendCursor"></wbs-node>
```

- [ ] **Step 8: 在 app.css 追加 presence 與 cursor 樣式**

```css
.presence-bar { display:flex; gap:6px; align-items:center; }
.presence-avatar { width:32px; height:32px; border-radius:50%; display:flex; align-items:center; justify-content:center; color:#fff; font-weight:700; font-size:0.85rem; cursor:default; }
.cursor-indicators { display:flex; gap:3px; margin-left:4px; }
.cursor-dot { width:10px; height:10px; border-radius:50%; display:inline-block; border:2px solid #fff; box-shadow:0 0 0 1px rgba(0,0,0,0.2); }
```

- [ ] **Step 9: 啟動並手動驗證即時協作**

```bash
mvn spring-boot:run
```

1. 開兩個瀏覽器視窗（不同帳號），進入同一個 `/projects/{id}`
2. 確認右上角 presence bar 顯示兩個用戶頭像
3. 在視窗 A 新增節點，確認視窗 B 即時出現
4. 在視窗 A hover 某節點，確認視窗 B 該節點出現小圓點游標指示
5. 在視窗 A 雙擊編輯節點，確認視窗 B 即時看到標題改變
6. 關閉視窗 A，確認視窗 B presence bar 移除視窗 A 的頭像

- [ ] **Step 10: Run all tests**

```bash
mvn test -q
```
Expected: BUILD SUCCESS。

- [ ] **Step 11: Commit**

```bash
git add src/main/resources/templates/project/detail.html \
        src/main/resources/static/css/app.css
git commit -m "feat: integrate WebSocket STOMP real-time collaboration in WBS editor"
```
