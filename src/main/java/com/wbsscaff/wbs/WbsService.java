package com.wbsscaff.wbs;

import com.wbsscaff.project.Project;
import com.wbsscaff.project.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WbsService {

    private final WbsRepository wbsRepository;
    private final ProjectRepository projectRepository;

    // 依 sortOrder 回傳，前端不需自行排序即可正確渲染樹狀結構
    public List<WbsDto.Response> getNodes(Long projectId) {
        return wbsRepository.findByProjectIdOrderBySortOrder(projectId)
            .stream().map(WbsDto.Response::from).toList();
    }

    @Transactional
    public WbsNode createNode(Long projectId, WbsDto.CreateRequest req) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("專案不存在"));
        WbsNode node = new WbsNode();
        node.setProject(project);
        node.setTitle(req.getTitle());
        if (req.getParentId() != null) {
            WbsNode parent = wbsRepository.findById(req.getParentId())
                .orElseThrow(() -> new EntityNotFoundException("父節點不存在"));
            if (!parent.getProject().getId().equals(projectId)) {
                throw new SecurityException("父節點不屬於此專案");
            }
            // WBS 最多兩層：父節點必須是根節點（無 parent），避免超過 L2 的深度
            if (parent.getParentId() != null) {
                throw new IllegalStateException("WBS 最多兩層，不允許在子節點下新增節點");
            }
            node.setParentId(req.getParentId());
        }
        node.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        return wbsRepository.save(node);
    }

    @Transactional
    public WbsNode updateNode(Long projectId, Long nodeId, WbsDto.UpdateRequest req) {
        WbsNode node = wbsRepository.findById(nodeId)
            .orElseThrow(() -> new EntityNotFoundException("節點不存在"));
        // 防止 IDOR：確認節點屬於請求路徑中的專案
        if (!node.getProject().getId().equals(projectId)) {
            throw new SecurityException("節點不屬於此專案");
        }
        if (req.getTitle()   != null) node.setTitle(req.getTitle());
        if (req.getOwner()   != null) node.setOwner(req.getOwner());
        if (req.getStartDate() != null) node.setStartDate(req.getStartDate());
        if (req.getEndDate()   != null) node.setEndDate(req.getEndDate());
        if (req.getStatus()  != null) node.setStatus(req.getStatus());
        if (req.getNotes()   != null) node.setNotes(req.getNotes());
        return wbsRepository.save(node);
    }

    @Transactional
    public void deleteNode(Long projectId, Long nodeId) {
        WbsNode node = wbsRepository.findById(nodeId)
            .orElseThrow(() -> new EntityNotFoundException("節點不存在"));
        // 防止 IDOR：確認節點屬於請求路徑中的專案
        if (!node.getProject().getId().equals(projectId)) {
            throw new SecurityException("節點不屬於此專案");
        }
        deleteRecursive(nodeId);
    }

    // 遞迴刪除子節點，確保整棵子樹完整清理，不留孤立節點
    private void deleteRecursive(Long nodeId) {
        wbsRepository.findByParentId(nodeId)
            .forEach(child -> deleteRecursive(child.getId()));
        wbsRepository.deleteById(nodeId);
    }

    @Transactional
    public void reorder(Long projectId, List<WbsDto.ReorderItem> items) {
        items.forEach(item -> {
            WbsNode node = wbsRepository.findById(item.getNodeId())
                .orElseThrow(() -> new EntityNotFoundException("節點不存在"));
            // 防止跨專案竄改：確認節點屬於當前專案
            if (!node.getProject().getId().equals(projectId)) {
                throw new SecurityException("節點不屬於此專案");
            }
            node.setSortOrder(item.getSortOrder());
            wbsRepository.save(node);
        });
    }

    // 支援拖曳跨父移動：同時更新 parentId 與 sortOrder，並驗證所有節點歸屬以防 IDOR
    @Transactional
    public void reorderWithParent(Long projectId, List<WbsDto.ReorderWithParentItem> items) {
        for (WbsDto.ReorderWithParentItem item : items) {
            WbsNode node = wbsRepository.findById(item.getNodeId())
                .orElseThrow(() -> new EntityNotFoundException("節點不存在"));
            if (!node.getProject().getId().equals(projectId))
                throw new SecurityException("節點不屬於此專案");
            node.setParentId(item.getParentId());
            node.setSortOrder(item.getSortOrder());
            wbsRepository.save(node);
        }
    }

    // JSON 匯入覆蓋：清除舊節點後以傳入的樹狀結構重建，支援兩層深度
    @Transactional
    public List<WbsNode> replaceAll(Long projectId, List<WbsDto.ImportNode> nodes) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("專案不存在"));
        wbsRepository.deleteByProjectId(projectId);
        List<WbsNode> created = new ArrayList<>();
        importNodes(project, nodes, null, 0, created);
        return created;
    }

    private void importNodes(Project project, List<WbsDto.ImportNode> nodes,
                             Long parentId, int baseOrder, List<WbsNode> created) {
        if (nodes == null) return;
        for (int i = 0; i < nodes.size(); i++) {
            WbsDto.ImportNode src = nodes.get(i);
            WbsNode node = new WbsNode();
            node.setProject(project);
            node.setParentId(parentId);
            node.setTitle(src.getTitle());
            node.setOwner(src.getOwner());
            node.setStartDate(src.getStartDate());
            node.setEndDate(src.getEndDate());
            node.setStatus(src.getStatus() != null ? src.getStatus() : WbsNode.Status.NOT_STARTED);
            node.setNotes(src.getNotes());
            node.setSortOrder(baseOrder + i);
            WbsNode saved = wbsRepository.save(node);
            created.add(saved);
            importNodes(project, src.getChildren(), saved.getId(), 0, created);
        }
    }
}
