# API 请求示例

## 创建简答题（带附件）

```json
{
  "typeCode": "short-answer",
  "title": "二重积分计算（带附图）",
  "stem": "计算二重积分 $\\displaystyle \\iint_D x^2 e^{-y^2}\\,dx\\,dy$，其中 $D$ 为三角形闭区域...",
  "answer": null,
  "solution": "\\begin{align*}...\\end{align*}",
  "difficulty": 0.45,
  "collectionId": 122000000000000001,
  "assets": [
    { "fileId": 1, "section": "PRO", "ordinal": 1 },
    { "fileId": 2, "section": "SOLU", "ordinal": 1 }
  ]
}
```

## 更新题目（PUT，简答题）

```json
{
  "title": "三重积分计算（更新版）",
  "stem": "计算三重积分 $\\displaystyle \\iiint\\limits_{\\substack{\\Omega}} ...",
  "solution": "\\begin{align*}...\\end{align*}",
  "assets": [
    { "fileId": 1, "section": "PRO", "ordinal": 1 }
  ]
}
```

## 更新判断题为 F

```json
{
  "stem": "设 A 为 3 阶方阵，|A|=2，则 |2A^{-1}|=4。",
  "judgeAnswer": "F",
  "answer": "命题错误",
  "solution": "利用 det(kA)=k^n det(A) 与 det(A^{-1})=1/det(A)",
  "assets": []
}
```
