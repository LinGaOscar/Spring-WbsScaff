package com.wbsscaff.template;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    public TemplateDto.ListResponse listAll(Long userId) {
        TemplateDto.ListResponse res = new TemplateDto.ListResponse();
        res.setSystem(templateRepository.findByIsSystemTrue()
            .stream().map(TemplateDto.TemplateResponse::from).toList());
        res.setCustom(templateRepository.findByOwnerIdAndIsSystemFalse(userId)
            .stream().map(TemplateDto.TemplateResponse::from).toList());
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
        templateRepository.save(clone);

        // 複製節點並保留父子關係（舊 ID → 新 ID 對映）
        Map<Long, Long> idMap = new HashMap<>();
        List<WbsTemplateNode> srcNodes = nodeRepository.findByTemplateIdOrderBySortOrder(templateId);
        for (WbsTemplateNode src : srcNodes) {
            WbsTemplateNode copy = new WbsTemplateNode();
            copy.setTemplateId(clone.getId());
            copy.setTitle(src.getTitle());
            copy.setSortOrder(src.getSortOrder());
            if (src.getParentId() != null) copy.setParentId(idMap.get(src.getParentId()));
            nodeRepository.save(copy);
            idMap.put(src.getId(), copy.getId());
        }
        return clone;
    }

    @Transactional
    public void deleteCustom(Long templateId, Long userId) {
        WbsTemplate tpl = templateRepository.findById(templateId)
            .orElseThrow(() -> new EntityNotFoundException("模板不存在"));
        if (tpl.isSystem()) throw new IllegalArgumentException("系統模板不可刪除");
        if (!tpl.getOwner().getId().equals(userId)) throw new SecurityException("無權刪除此模板");
        nodeRepository.deleteByTemplateId(templateId);
        templateRepository.delete(tpl);
    }

    @Transactional
    public void setDefault(Long templateId, Long userId) {
        // 先清除該使用者所有自訂模板的預設標記
        templateRepository.findByOwnerIdAndIsSystemFalse(userId)
            .forEach(t -> { t.setDefault(false); templateRepository.save(t); });
        WbsTemplate tpl = templateRepository.findById(templateId).orElseThrow();
        tpl.setDefault(true);
        templateRepository.save(tpl);
    }

    @Transactional
    public void applyToProject(Long templateId, Long projectId) {
        var project = projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("專案不存在"));
        List<WbsTemplateNode> nodes = nodeRepository.findByTemplateIdOrderBySortOrder(templateId);
        Map<Long, Long> idMap = new HashMap<>();
        for (WbsTemplateNode tNode : nodes) {
            WbsNode wNode = new WbsNode();
            wNode.setProject(project);
            wNode.setTitle(tNode.getTitle());
            wNode.setSortOrder(tNode.getSortOrder());
            if (tNode.getParentId() != null) wNode.setParentId(idMap.get(tNode.getParentId()));
            wbsRepository.save(wNode);
            idMap.put(tNode.getId(), wNode.getId());
        }
    }

    @Transactional
    public WbsTemplate saveProjectAsTemplate(Long projectId, Long userId, String name) {
        User owner = userRepository.findById(userId).orElseThrow();
        WbsTemplate tpl = new WbsTemplate();
        tpl.setName(name); tpl.setOwner(owner); tpl.setSystem(false);
        templateRepository.save(tpl);

        List<WbsNode> wbsNodes = wbsRepository.findByProjectIdOrderBySortOrder(projectId);
        Map<Long, Long> idMap = new HashMap<>();
        for (var wNode : wbsNodes) {
            WbsTemplateNode tNode = new WbsTemplateNode();
            tNode.setTemplateId(tpl.getId());
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
        var project = projectRepository.findById(projectId)
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
            var src = nodes.get(i);
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
        List<WbsTemplateNode> nodes = nodeRepository.findByTemplateIdOrderBySortOrder(templateId);
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
