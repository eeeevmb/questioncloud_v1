package cn.sztu.questioncloud.infrastructure.adapter.question.parser;

import cn.sztu.questioncloud.infrastructure.common.persistent.enums.AssetSection;
import cn.sztu.questioncloud.infrastructure.common.persistent.enums.AssetType;
import cn.sztu.questioncloud.web.rest.v1.question.vo.AssetVO;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionDraftVO;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleLatexQuestionParserTests {

    private final SimpleLatexQuestionParser parser = new SimpleLatexQuestionParser();

    @Test
    void parseMultipleBlocks() {
        String latex = """
                \\begin{pro}
                    计算 $I=\\iint_D|\\cos (x+y)| \\mathrm{d} x \\mathrm{~d} y$.
                \\end{pro}

                \\begin{solu}
                    第一题解析。
                    \\includegraphics[width = \\textwidth]{../Level1/fig/fig1.png}
                \\end{solu}

                \\begin{pro}
                    第二题题干。
                \\end{pro}

                \\begin{solu}
                    第二题解析。
                    \\includegraphics{../Level1/fig/fig2.png}
                \\end{solu}
                """;

        List<QuestionDraftVO> result = parser.parse(
                new ByteArrayInputStream(latex.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8
        );

        assertThat(result).hasSize(2);

        QuestionDraftVO first = result.getFirst();
        assertThat(first.stemLatex()).contains("\\iint");
        assertThat(first.solutionLatex()).contains("asset://solu-1-1");
        AssetVO firstAsset = first.assets().getFirst();
        assertThat(firstAsset.slotId()).isEqualTo("solu-1-1");
        assertThat(firstAsset.section()).isEqualTo(AssetSection.SOLU);
        assertThat(firstAsset.assetType()).isEqualTo(AssetType.IMAGE);
        assertThat(firstAsset.latexPlaceholder()).isEqualTo("asset://solu-1-1");
        assertThat(firstAsset.suggestedFilename()).isEqualTo("fig1.png");

        QuestionDraftVO second = result.get(1);
        assertThat(second.stemLatex()).contains("第二题题干");
        assertThat(second.solutionLatex()).contains("asset://solu-2-1");
        AssetVO secondAsset = second.assets().getFirst();
        assertThat(secondAsset.slotId()).isEqualTo("solu-2-1");
        assertThat(secondAsset.suggestedFilename()).isEqualTo("fig2.png");
    }
}
