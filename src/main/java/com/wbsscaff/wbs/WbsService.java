package com.wbsscaff.wbs;

import com.wbsscaff.project.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WbsService {

    private final WbsRepository wbsRepository;
    private final ProjectRepository projectRepository;

    public List<WbsDto.Response> getNodes(Long projectId) {
        return wbsRepository.findByProjectIdOrderBySortOrder(projectId)
            .stream().map(WbsDto.Response::from).toList();
    }

    @Transactional
    public WbsNode createNode(Long projectId, WbsDto.CreateRequest req) {
        var project = projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("專案不存在"));
        WbsNode node = new WbsNode();
        node.setProject(project);
        node.setTitle(req.getTitle());
        node.setParentId(req.getParentId());
        node.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        return wbsRepository.save(node);
    }

    @Transactional
    public WbsNode updateNode(Long nodeId, WbsDto.UpdateRequest req) {
        WbsNode node = wbsRepository.findById(nodeId)
            .orElseThrow(() -> new EntityNotFoundException("節點不存在"));
        if (req.getTitle()   != null) node.setTitle(req.getTitle());
        if (req.getOwner()   != null) node.setOwner(req.getOwner());
        if (req.getStartDate() != null) node.setStartDate(req.getStartDate());
        if (req.getEndDate()   != null) node.setEndDate(req.getEndDate());
        if (req.getStatus()  != null) node.setStatus(req.getStatus());
        if (req.getNotes()   != null) node.setNotes(req.getNotes());
        return wbsRepository.save(node);
    }

    @Transactional
    public void deleteNode(Long nodeId) {
        deleteRecursive(nodeId);
    }

    // 遞迴刪除子節點，確保 tree 完整清理
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
}
