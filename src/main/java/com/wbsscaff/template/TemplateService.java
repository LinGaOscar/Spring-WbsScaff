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

    @Transactional
    public void setDefault(Long templateId, Long userId) {
        // 先清除該使用者所屬科所有自訂模板的預設標記
        templateRepository.findBySectionId(userId)
            .forEach(t -> { t.setDefault(false); templateRepository.save(t); });
        WbsTemplate tpl = templateRepository.findById(templateId).orElseThrow();
        tpl.setDefault(true);
        templateRepository.save(tpl);
    }

    @Transactional(readOnly = true)
    public TemplateDto.DetailResponse getWithNodes(Long id) {
        WbsTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("模板不存在"));
        List<WbsTemplateNode> nodes = nodeRepository.findByTemplate_IdOrderBySortOrder(id);
        return TemplateDto.DetailResponse.from(template, nodes);
    }

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

    @Transactional
    public TemplateDto.Response saveFromProject(Long projectId, String name, User caller) {
        WbsTemplate tpl = saveProjectAsTemplate(projectId, caller.getId(), name);
        return TemplateDto.Response.from(tpl);
    }

    @Transactional
    public void applyToProject(Long templateId, Long projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("專案不存在"));
        List<WbsTemplateNode> nodes = nodeRepository.findByTemplate_IdOrderBySortOrder(templateId);

        // 建立父節點對照表，用於壓平超過兩層的結構
        Map<Long, Long> tplParentMap = new HashMap<>();
        for (WbsTemplateNode n : nodes) {
            if (n.getParentId() != null) tplParentMap.put(n.getId(), n.getParentId());
        }
        // 每個非根節點找到其最上層根節點（level 1）
        Map<Long, Long> rootAncestor = new HashMap<>();
        for (WbsTemplateNode n : nodes) {
            if (n.getParentId() == null) {
                rootAncestor.put(n.getId(), null);
            } else {
                Long cur = n.getParentId();
                while (tplParentMap.containsKey(cur)) cur = tplParentMap.get(cur);
                rootAncestor.put(n.getId(), cur);
            }
        }

        Map<Long, Long> tplIdToWbsId = new HashMap<>();
        // 第一輪：建立根節點
        for (WbsTemplateNode tNode : nodes) {
            if (tNode.getParentId() == null) {
                WbsNode wNode = new WbsNode();
                wNode.setProject(project);
                wNode.setTitle(tNode.getTitle());
                wNode.setSortOrder(tNode.getSortOrder());
                wbsRepository.save(wNode);
                tplIdToWbsId.put(tNode.getId(), wNode.getId());
            }
        }
        // 第二輪：所有非根節點一律壓平為 level 2（父節點 = 其根祖先）
        for (WbsTemplateNode tNode : nodes) {
            if (tNode.getParentId() != null) {
                Long rootTplId = rootAncestor.get(tNode.getId());
                Long parentWbsId = tplIdToWbsId.get(rootTplId);
                WbsNode wNode = new WbsNode();
                wNode.setProject(project);
                wNode.setTitle(tNode.getTitle());
                wNode.setSortOrder(tNode.getSortOrder());
                wNode.setParentId(parentWbsId);
                wbsRepository.save(wNode);
                tplIdToWbsId.put(tNode.getId(), wNode.getId());
            }
        }
    }

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

    @Transactional
    public void importJson(String json, Long projectId) throws Exception {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("專案不存在"));
        List<TemplateDto.ExportNode> roots = objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructCollectionType(List.class,
                TemplateDto.ExportNode.class));
        importNodes(roots, null, project, 0);
    }

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

    public String exportJson(Long templateId) throws Exception {
        List<WbsTemplateNode> nodes = nodeRepository.findByTemplate_IdOrderBySortOrder(templateId);
        List<TemplateDto.ExportNode> roots = buildExportTree(nodes, null);
        return objectMapper.writeValueAsString(roots);
    }

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
