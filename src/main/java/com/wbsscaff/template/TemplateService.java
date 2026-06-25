package com.wbsscaff.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbsscaff.project.Project;
import com.wbsscaff.project.ProjectRepository;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import com.wbsscaff.wbs.WbsNode;
import com.wbsscaff.wbs.WbsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateNodeRepository nodeRepository;
    private final WbsRepository wbsRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // 科成員看系統模板+本科自訂模板；部長或無所屬科只看系統模板（避免跨科模板外洩）
    public TemplateDto.ListResponse listAll(User user) {
        TemplateDto.ListResponse res = new TemplateDto.ListResponse();
        // 判斷 user 是否屬於某個科（parent != null 代表是科）
        boolean inSection = user.getDepartment() != null && user.getDepartment().getParent() != null;
        if (inSection) {
            // 科成員：可見系統模板 + 本科自訂模板
            List<WbsTemplate> visible = templateRepository.findVisibleToSection(user.getDepartment().getId());
            res.setSystem(visible.stream().filter(WbsTemplate::isSystem).map(TemplateDto.TemplateResponse::from).toList());
            res.setCustom(visible.stream().filter(t -> !t.isSystem()).map(TemplateDto.TemplateResponse::from).toList());
        } else {
            // 部長或無所屬科：只看系統模板
            res.setSystem(templateRepository.findByIsSystemTrue().stream().map(TemplateDto.TemplateResponse::from).toList());
            res.setCustom(List.of());
        }
        return res;
    }

    // 複製系統模板為本科自訂版，節點 ID 全部更新為新 ID 以維持父子關係
    @Transactional
    public WbsTemplate cloneSystem(Long templateId, Long userId) {
        WbsTemplate source = templateRepository.findById(templateId)
            .orElseThrow(() -> new EntityNotFoundException("模板不存在"));
        User owner = userRepository.findById(userId).orElseThrow();

        WbsTemplate clone = new WbsTemplate();
        clone.setName(source.getName() + "（複製）");
        clone.setDescription(source.getDescription());
        clone.setOwner(owner);
        clone.setSystem(false);
        clone.setClonedFrom(templateId);
        // clone 後歸屬建立者的科
        clone.setSection(owner.getDepartment());
        templateRepository.save(clone);

        // 複製節點並保留父子關係（舊 ID → 新 ID 對映）
        Map<Long, Long> idMap = new HashMap<>();
        List<WbsTemplateNode> srcNodes = nodeRepository.findByTemplate_IdOrderBySortOrder(templateId);
        for (WbsTemplateNode src : srcNodes) {
            WbsTemplateNode copy = new WbsTemplateNode();
            copy.setTemplate(clone);
            copy.setTitle(src.getTitle());
            copy.setSortOrder(src.getSortOrder());
            if (src.getParentId() != null) copy.setParentId(idMap.get(src.getParentId()));
            nodeRepository.save(copy);
            idMap.put(src.getId(), copy.getId());
        }
        return clone;
    }

    // 系統模板不可刪除，防止影響所有科；只能刪本科自訂模板
    @Transactional
    public void deleteCustom(Long templateId, User user) {
        WbsTemplate tpl = templateRepository.findById(templateId)
            .orElseThrow(() -> new EntityNotFoundException("模板不存在"));
        if (tpl.isSystem()) throw new IllegalArgumentException("系統模板不可刪除");
        // 需要 canManageSection 且只能刪除本科的模板
        if (!user.canManageSection()) throw new SecurityException("無管理模板的權限");
        if (tpl.getSection() == null || !tpl.getSection().getId().equals(user.getDepartment().getId()))
            throw new SecurityException("無權刪除其他科的模板");
        nodeRepository.deleteByTemplate_Id(templateId);
        templateRepository.delete(tpl);
    }

    // 先清除本科所有預設標記，再設定新預設，確保每科只有一個預設模板
    @Transactional
    public void setDefault(Long templateId, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        if (!user.canManageSection()) throw new SecurityException("無設定預設模板的權限");
        // 批次清除本科所有模板的預設標記，避免 N+1 查詢
        List<WbsTemplate> sectionTemplates = templateRepository.findBySectionId(user.getDepartment().getId());
        sectionTemplates.forEach(t -> t.setDefault(false));
        templateRepository.saveAll(sectionTemplates);
        WbsTemplate tpl = templateRepository.findById(templateId).orElseThrow();
        tpl.setDefault(true);
        templateRepository.save(tpl);
    }

    // 同時回傳模板基本資訊與節點清單，前端一次請求取得完整模板內容
    @Transactional(readOnly = true)
    public TemplateDto.DetailResponse getWithNodes(Long id) {
        WbsTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("模板不存在"));
        List<WbsTemplateNode> nodes = nodeRepository.findByTemplate_IdOrderBySortOrder(id);
        return TemplateDto.DetailResponse.from(template, nodes);
    }

    // 系統模板不可修改，防止影響所有使用此模板的科
    @Transactional
    public TemplateDto.Response updateTemplate(Long id, TemplateDto.UpdateRequest req, User caller) {
        WbsTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("模板不存在"));
        if (template.isSystem()) throw new SecurityException("系統模板不可修改");
        // 只能修改本科的模板
        if (!caller.canManageSection()) throw new SecurityException("無管理模板的權限");
        if (template.getSection() == null || !template.getSection().getId().equals(caller.getDepartment().getId()))
            throw new SecurityException("無權修改其他科的模板");
        if (req.getName() != null) template.setName(req.getName());
        if (req.getDescription() != null) template.setDescription(req.getDescription());
        return TemplateDto.Response.from(templateRepository.save(template));
    }

    // 讓科長從既有專案 WBS 快速產生可重用模板，不需重新手動建立
    @Transactional
    public TemplateDto.Response saveFromProject(Long projectId, String name, User caller) {
        WbsTemplate tpl = saveProjectAsTemplate(projectId, caller.getId(), name);
        return TemplateDto.Response.from(tpl);
    }

    // 多輪處理解決父子節點建立順序問題：先建根，再建子，支援任意層深度
    @Transactional
    public void applyToProject(Long templateId, Long projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("專案不存在"));
        List<WbsTemplateNode> nodes = nodeRepository.findByTemplate_IdOrderBySortOrder(templateId);

        // 多輪處理：父節點建立後才處理子節點，支援任意層深度
        Map<Long, Long> tplIdToWbsId = new HashMap<>();
        List<WbsTemplateNode> remaining = new ArrayList<>(nodes);
        while (!remaining.isEmpty()) {
            List<WbsTemplateNode> deferred = new ArrayList<>();
            for (WbsTemplateNode tNode : remaining) {
                Long parentWbsId = null;
                if (tNode.getParentId() != null) {
                    if (!tplIdToWbsId.containsKey(tNode.getParentId())) {
                        deferred.add(tNode); // 父節點尚未建立，下輪再試
                        continue;
                    }
                    parentWbsId = tplIdToWbsId.get(tNode.getParentId());
                }
                WbsNode wNode = new WbsNode();
                wNode.setProject(project);
                wNode.setTitle(tNode.getTitle());
                wNode.setSortOrder(tNode.getSortOrder());
                wNode.setParentId(parentWbsId);
                wbsRepository.save(wNode);
                tplIdToWbsId.put(tNode.getId(), wNode.getId());
            }
            remaining = deferred;
        }
    }

    // 將現有專案的 WBS 節點（含層級）轉存為模板，模板歸屬建立者的科
    @Transactional
    public WbsTemplate saveProjectAsTemplate(Long projectId, Long userId, String name) {
        User owner = userRepository.findById(userId).orElseThrow();
        WbsTemplate tpl = new WbsTemplate();
        tpl.setName(name);
        tpl.setOwner(owner);
        tpl.setSystem(false);
        // 模板歸屬建立者的科
        tpl.setSection(owner.getDepartment());
        templateRepository.save(tpl);

        List<WbsNode> wbsNodes = wbsRepository.findByProjectIdOrderBySortOrder(projectId);
        Map<Long, Long> idMap = new HashMap<>();
        for (WbsNode wNode : wbsNodes) {
            WbsTemplateNode tNode = new WbsTemplateNode();
            tNode.setTemplate(tpl);
            tNode.setTitle(wNode.getTitle());
            tNode.setSortOrder(wNode.getSortOrder());
            if (wNode.getParentId() != null) tNode.setParentId(idMap.get(wNode.getParentId()));
            nodeRepository.save(tNode);
            idMap.put(wNode.getId(), tNode.getId());
        }
        return tpl;
    }

    // 從 JSON 匯入 WBS 結構，可從其他專案或外部備份快速初始化節點
    @Transactional
    public void importJson(String json, Long projectId) throws Exception {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("專案不存在"));
        List<TemplateDto.ExportNode> roots = objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructCollectionType(List.class,
                TemplateDto.ExportNode.class));
        importNodes(roots, null, project, 0);
    }

    // 遞迴建立節點，baseOrder 確保同層節點的 sortOrder 正確排序
    private void importNodes(List<TemplateDto.ExportNode> nodes,
                             Long parentId,
                             com.wbsscaff.project.Project project,
                             int baseOrder) {
        for (int i = 0; i < nodes.size(); i++) {
            TemplateDto.ExportNode src = nodes.get(i);
            WbsNode node = new WbsNode();
            node.setProject(project);
            node.setTitle(src.getTitle());
            node.setParentId(parentId);
            node.setSortOrder(baseOrder + i);
            wbsRepository.save(node);
            if (src.getChildren() != null && !src.getChildren().isEmpty()) {
                importNodes(src.getChildren(), node.getId(), project, 0);
            }
        }
    }

    // 匯出為 JSON 讓科長可備份模板或分享給其他科手動匯入
    public String exportJson(Long templateId) throws Exception {
        List<WbsTemplateNode> nodes = nodeRepository.findByTemplate_IdOrderBySortOrder(templateId);
        List<TemplateDto.ExportNode> roots = buildExportTree(nodes, null);
        return objectMapper.writeValueAsString(roots);
    }

    @Transactional
    public WbsTemplate createEmpty(String name, String description, Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        if (!user.canManageSection()) throw new SecurityException("無建立模板的權限");
        WbsTemplate tpl = new WbsTemplate();
        tpl.setName(name);
        tpl.setDescription(description);
        tpl.setOwner(user);
        tpl.setSection(user.getDepartment());
        return templateRepository.save(tpl);
    }

    @Transactional
    public WbsTemplateNode addNode(Long templateId, TemplateDto.NodeCreateRequest req, Long userId) {
        WbsTemplate tpl = requireEditAccess(templateId, userId);
        WbsTemplateNode node = new WbsTemplateNode();
        node.setTemplate(tpl);
        node.setTitle(req.getTitle());
        node.setParentId(req.getParentId());
        node.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        node.setNotes(req.getNotes());
        return nodeRepository.save(node);
    }

    @Transactional
    public WbsTemplateNode updateNode(Long templateId, Long nodeId, TemplateDto.NodeUpdateRequest req, Long userId) {
        requireEditAccess(templateId, userId);
        WbsTemplateNode node = nodeRepository.findById(nodeId)
            .orElseThrow(() -> new EntityNotFoundException("節點不存在"));
        if (!node.getTemplate().getId().equals(templateId))
            throw new SecurityException("節點不屬於此模板");
        if (req.getTitle() != null) node.setTitle(req.getTitle());
        node.setNotes(req.getNotes());
        return nodeRepository.save(node);
    }

    @Transactional
    public void deleteNode(Long templateId, Long nodeId, Long userId) {
        requireEditAccess(templateId, userId);
        WbsTemplateNode node = nodeRepository.findById(nodeId)
            .orElseThrow(() -> new EntityNotFoundException("節點不存在"));
        if (!node.getTemplate().getId().equals(templateId))
            throw new SecurityException("節點不屬於此模板");
        deleteNodeRecursive(nodeId);
    }

    @Transactional
    public void reorderNodes(Long templateId, List<TemplateDto.ReorderItem> items, Long userId) {
        requireEditAccess(templateId, userId);
        items.forEach(item -> nodeRepository.findById(item.getId()).ifPresent(n -> {
            n.setSortOrder(item.getSortOrder());
            nodeRepository.save(n);
        }));
    }

    private void deleteNodeRecursive(Long nodeId) {
        nodeRepository.findByParentId(nodeId).forEach(child -> deleteNodeRecursive(child.getId()));
        nodeRepository.deleteById(nodeId);
    }

    private WbsTemplate requireEditAccess(Long templateId, Long userId) {
        WbsTemplate tpl = templateRepository.findById(templateId)
            .orElseThrow(() -> new EntityNotFoundException("模板不存在"));
        if (tpl.isSystem()) throw new SecurityException("系統模板不可編輯");
        User user = userRepository.findById(userId).orElseThrow();
        if (!user.canManageSection()) throw new SecurityException("無管理模板的權限");
        if (tpl.getSection() == null || !tpl.getSection().getId().equals(user.getDepartment().getId()))
            throw new SecurityException("只能編輯本科模板");
        return tpl;
    }

    // 遞迴建立樹狀結構，以 parentId == null 作為根節點的起點
    private List<TemplateDto.ExportNode> buildExportTree(List<WbsTemplateNode> all, Long parentId) {
        List<TemplateDto.ExportNode> result = new ArrayList<>();
        all.stream().filter(n -> Objects.equals(n.getParentId(), parentId)).forEach(n -> {
            TemplateDto.ExportNode e = new TemplateDto.ExportNode();
            e.setTitle(n.getTitle());
            e.setChildren(buildExportTree(all, n.getId()));
            result.add(e);
        });
        return result;
    }
}
