package cn.sztu.questioncloud.infrastructure.common.persistent.entity.dto;

import cn.sztu.questioncloud.infrastructure.common.file.model.InfraFileMetadata;
import cn.sztu.questioncloud.web.rest.v1.question.vo.AssetVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AssetSnapshotFactory {

    @Value("${file.storage.local.base-path}")
    private String basePath;

    public List<AssetSnapshot> from(List<AssetVO> assets, Map<String, InfraFileMetadata> uploadedAssets) {
        if (assets == null || assets.isEmpty()) return List.of();

        Map<String, AssetVO> bySlot = assets.stream()
                .collect(Collectors.toMap(AssetVO::slotId, a -> a, (a, b) -> a, LinkedHashMap::new));

        List<AssetSnapshot> snapshots = new ArrayList<>(bySlot.size());
        for (var e : bySlot.entrySet()) {
            String slotId = e.getKey();
            AssetVO vo = e.getValue();
            InfraFileMetadata data = uploadedAssets.get(slotId);
            if (data == null)
                throw new IllegalArgumentException("未找到已上传文件：" + slotId);

            snapshots.add(AssetSnapshot.builder()
                    .slotId(slotId)
                    .section(vo.section())
                    .ordinal(vo.ordinal())
                    .type(vo.assetType())
                    .latexPlaceholder(basePath + "/" + data.getFmStoragePath())
                    .fileId(data.getFmId())
                    .storagePath(data.getFmStoragePath())
                    .build());
        }
        return snapshots;
    }
}

