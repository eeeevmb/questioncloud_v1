<template>
  <el-card>
    <h2>MathView 渲染示例</h2>
    <p>以下示例涵盖行内公式、块级公式和 align* 环境。</p>
    <div class="sample-block">
      <h3>行内公式</h3>
      <MathView :content="inlineContent" />
    </div>
    <div class="sample-block">
      <h3>块级公式</h3>
      <MathView :content="displayContent" />
    </div>
    <div class="sample-block">
      <h3>Align 环境</h3>
      <MathView :content="alignContent" />
    </div>
  </el-card>
</template>

<script setup lang="ts">
import MathView from '../components/MathView.vue';

const inlineContent = '设 $X$ 满足 $a^2 + b^2 = c^2$，则 $c = \\sqrt{a^2 + b^2}$。';
const displayContent = '\\int_0^1 x^2 \\, dx = \\frac{1}{3}';
const alignContent = `\\begin{align*}
  f(x) &= x^2 + 2x + 1 \\\\
       &= (x + 1)^2
\\end{align*}`;
</script>

<style scoped>
.sample-block {
  margin-top: 16px;
}

.sample-block h3 {
  margin: 0 0 8px;
}
</style>
