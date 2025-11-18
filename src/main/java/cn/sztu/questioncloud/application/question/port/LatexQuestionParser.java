package cn.sztu.questioncloud.application.question.port;

import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionDraftVO;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

/**
 * LaTeX 题目解析端口：直接产出 Web 层可复用的题目草稿 VO。
 */
public interface LatexQuestionParser {

    /**
     * 解析 LaTeX 字符流，按约定输出题干、解析、附件占位集合。
     *
     * @param source  LaTeX 输入流
     * @param charset 字符集，null 默认 UTF-8
     * @return 题目草稿 VO 列表
     */
    List<QuestionDraftVO> parse(InputStream source, Charset charset);
}
