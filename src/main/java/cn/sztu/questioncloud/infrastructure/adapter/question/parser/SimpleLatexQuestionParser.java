package cn.sztu.questioncloud.infrastructure.adapter.question.parser;

import cn.sztu.questioncloud.application.question.port.LatexQuestionParser;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.enums.AssetSection;
import cn.sztu.questioncloud.infrastructure.common.persistent.enums.AssetType;
import cn.sztu.questioncloud.web.rest.v1.question.vo.AssetVO;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionDraftVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于简单正则的 LaTeX 解析器实现，仅覆盖 demo 场景。
 */
@Slf4j
@Component
public class SimpleLatexQuestionParser implements LatexQuestionParser {

    private static final Pattern PRO_PATTERN = Pattern.compile("\\\\begin\\{pro\\}(.*?)\\\\end\\{pro\\}", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern SOLU_PATTERN = Pattern.compile("\\\\begin\\{solu\\}(.*?)\\\\end\\{solu\\}", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern INCLUDE_GRAPHICS_PATTERN = Pattern.compile("\\\\includegraphics(?:\\[[^]]*])?\\{([^}]+)}", Pattern.CASE_INSENSITIVE);

    @Override
    public List<QuestionDraftVO> parse(InputStream source, Charset charset) {
        if (source == null) {
            throw new IllegalArgumentException("source input stream must not be null");
        }

        Charset useCharset = charset == null ? StandardCharsets.UTF_8 : charset;
        String latex = readAll(source, useCharset);

        List<String> stemBlocks = extractBlocks(latex, PRO_PATTERN);
        List<String> soluBlocks = extractBlocks(latex, SOLU_PATTERN);

        int questionCount = Math.max(stemBlocks.size(), soluBlocks.size());
        List<QuestionDraftVO> drafts = new ArrayList<>(questionCount);
        for (int i = 0; i < questionCount; i++) {
            String stemBlock = i < stemBlocks.size() ? stemBlocks.get(i) : "";
            String soluBlock = i < soluBlocks.size() ? soluBlocks.get(i) : "";

            SectionResult stem = replaceAssets(stemBlock, AssetSection.PRO, i + 1);
            SectionResult solu = replaceAssets(soluBlock, AssetSection.SOLU, i + 1);

            List<AssetVO> assets = new ArrayList<>();
            assets.addAll(stem.assets());
            assets.addAll(solu.assets());

            Long draftId = HutoolSnowflakeIdGenerator.generateLongId();

            drafts.add(QuestionDraftVO.of(draftId, null, stem.cleanedLatex(), solu.cleanedLatex(), assets));
        }

        return drafts;
    }

    private List<String> extractBlocks(String latex, Pattern pattern) {
        Matcher matcher = pattern.matcher(latex);
        List<String> blocks = new ArrayList<>();
        while (matcher.find()) {
            blocks.add(matcher.group(1).trim());
        }
        return blocks;
    }

    private SectionResult replaceAssets(String block, AssetSection section, int questionIndex) {
        if (block == null || block.isEmpty()) {
            return new SectionResult("", List.of());
        }
        Matcher matcher = INCLUDE_GRAPHICS_PATTERN.matcher(block);
        StringBuffer buffer = new StringBuffer();
        List<AssetVO> assets = new ArrayList<>();
        int ordinal = 1;
        while (matcher.find()) {
            String includeCommand = matcher.group(0);
            String path = matcher.group(1).trim();

            String slotId = buildSlotId(section, questionIndex, ordinal);
            String placeholder = "asset://" + slotId;

            assets.add(AssetVO.of(
                    slotId,
                    section,
                    ordinal,
                    AssetType.IMAGE,
                    includeCommand,
                    placeholder,
                    extractFileName(path)
            ));

            matcher.appendReplacement(buffer, Matcher.quoteReplacement(placeholder));
            ordinal++;
        }
        matcher.appendTail(buffer);
        return new SectionResult(buffer.toString().trim(), assets);
    }

    private static String buildSlotId(AssetSection section, int questionIndex, int ordinal) {
        return section.name().toLowerCase(Locale.ROOT) + "-" + questionIndex + "-" + ordinal;
    }

    private static String extractFileName(String path) {
        String normalized = path.replace("\\\\", "/");
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 && lastSlash < normalized.length() - 1
                ? normalized.substring(lastSlash + 1)
                : normalized;
    }

    private static String readAll(InputStream source, Charset charset) {
        try {
            return new String(source.readAllBytes(), charset);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read LaTeX stream", e);
        }
    }

    private static class SectionResult {
        private final String cleanedLatex;
        private final List<AssetVO> assets;

        private SectionResult(String cleanedLatex, List<AssetVO> assets) {
            this.cleanedLatex = cleanedLatex;
            this.assets = assets;
        }

        private String cleanedLatex() {
            return cleanedLatex;
        }

        private List<AssetVO> assets() {
            return assets;
        }
    }
}
