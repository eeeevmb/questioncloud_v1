package cn.sztu.questioncloud.application.importer.dto;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
@ExcelIgnoreUnannotated
public class ImportExcelRow {
    @ExcelProperty("标题（选填）")
    private String title;

    @ExcelProperty("题干（必填）")
    private String stem;

    @ExcelProperty("题型（必填）")
    private String type;

    @ExcelProperty("选项A")
    private String optionA;
    @ExcelProperty("选项B")
    private String optionB;
    @ExcelProperty("选项C")
    private String optionC;
    @ExcelProperty("选项D")
    private String optionD;
    @ExcelProperty("选项E")
    private String optionE;
    @ExcelProperty("选项F")
    private String optionF;

    @ExcelProperty("选择题答案")
    private String choiceAnswer;

    @ExcelProperty("判断题答案")
    private String judgeAnswer;

    @ExcelProperty("正确答案")
    private String correctAnswer;

    @ExcelProperty("解析")
    private String solution;

    @ExcelProperty("难度系数（必填）")
    private String difficulty;

    @ExcelProperty("题库名 （必填）")
    private String collectionName;

    public boolean isEmptyRow() {
        return isBlank(title) && isBlank(stem) && isBlank(type)
                && isBlank(optionA) && isBlank(optionB) && isBlank(optionC)
                && isBlank(optionD) && isBlank(optionE) && isBlank(optionF)
                && isBlank(choiceAnswer)
                && isBlank(judgeAnswer) && isBlank(correctAnswer)
                && isBlank(solution) && isBlank(difficulty)
                && isBlank(collectionName);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
